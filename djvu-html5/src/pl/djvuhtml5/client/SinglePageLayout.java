package pl.djvuhtml5.client;

import static pl.djvuhtml5.client.TileCache.MAX_SUBSAMPLE;
import static pl.djvuhtml5.client.TileCache.toSubsample;
import static pl.djvuhtml5.client.TileCache.toZoom;

import java.util.ArrayList;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.dom.client.CanvasElement;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.dom.client.MouseWheelEvent;
import com.google.gwt.event.dom.client.MouseWheelHandler;
import com.google.gwt.user.client.Event;
import com.lizardtech.djvu.DjVuInfo;
import com.lizardtech.djvu.DjVuPage;
import com.lizardtech.djvu.GRect;

import pl.djvuhtml5.client.PageCache.PageDownloadListener;
import pl.djvuhtml5.client.Scrollbar.ScrollPanListener;
import pl.djvuhtml5.client.TileCache.TileCacheListener;
import pl.djvuhtml5.client.TileCache.TileInfo;

public class SinglePageLayout implements PageDownloadListener, TileCacheListener {

	interface ChangeListener {
		void pageChanged(int currentPage);
		void zoomChanged(int currentZoom);
	}

	private final Djvu_html5 app;

	private double zoom = 0;

	private double zoom100;

	private int page = 0;

	private final TileCache tileCache;

	private DjVuInfo pageInfo;

	/**
	 * Location of the center of the screen in the coordinates of the scaled
	 * page (one point is one pixel on the screen).
	 */
	private int centerX, centerY;

	private final Canvas canvas;

	private ChangeListener changeListener;

	private String background;

	private final int pageMargin;

	private CanvasElement[][] imagesArray;

	private GRect range = new GRect();

	public SinglePageLayout(Djvu_html5 app) {
		this.app = app;
		this.tileCache = app.getTileCache();

		app.getPageCache().addPageDownloadListener(this);
		tileCache.addTileCacheListener(this);

		this.canvas = app.getCanvas();

		this.background = app.getBackground();
		this.pageMargin = app.getPageMargin();

		new PanController(canvas);
	}

	public void setPage(int pageNum) {
		page = pageNum;
		DjVuPage newPage = app.getPageCache().fetchPage(pageNum);
		if (newPage != null) {
			pageInfo = newPage.getInfo();
			app.getToolbar().setZoomOptions(findZoomOptions());
			viewChanged();
		} else {
			pageInfo = null;
		}
		redraw();
		if (changeListener != null)
			changeListener.pageChanged(pageNum);
	}

	public int getPage() {
		return page;
	}

	public void canvasResized() {
		viewChanged();
		redraw();
	}

	public void zoomToFitPage() {
		if (pageInfo == null)
			return;
		doSetZoom(Math.min((1.0f * canvas.getCoordinateSpaceWidth() - pageMargin * 2) / pageInfo.width,
				(1.0f * canvas.getCoordinateSpaceHeight() - pageMargin * 2) / pageInfo.height));
	}

	public void zoomToFitWidth() {
		if (pageInfo == null)
			return;
		doSetZoom((1.0f * canvas.getCoordinateSpaceWidth() - pageMargin * 2) / pageInfo.width);
	}

	public void setZoom(int percent) {
		if (pageInfo == null)
			return;
		doSetZoom(percent * zoom100 / 100);
	}

	public int getZoom() {
		return (int) (zoom / zoom100 * 100 + 0.5);
	}

	private void doSetZoom(double zoom) {
		centerX *= zoom / this.zoom;
		centerY *= zoom / this.zoom;
		this.zoom = zoom;
		viewChanged();
		redraw();
		if (changeListener != null)
			changeListener.zoomChanged(getZoom());
	}

	private ArrayList<Integer> findZoomOptions() {
		ArrayList<Integer> result = new ArrayList<>();
		result.add(100);
		final int screenDPI = app.getScreenDPI();
		zoom100 = 1.0 * screenDPI / pageInfo.dpi;
		int subsample = toSubsample(zoom100);
		if (toZoom(subsample) / zoom100 > zoom100 / toZoom(subsample + 1))
			subsample++;
		zoom100 = toZoom(subsample);

		if (zoom == 0)
			zoom = zoom100;

		double z = zoom100;
		for (int i = subsample + 1; i <= MAX_SUBSAMPLE; i++) {
			double z2 = toZoom(i);
			if (z / z2 > 1.2) {
				z = z2;
				result.add((int) (z / zoom100 * 100 + 0.5));
			}
		}

		z = zoom100;
		for (int i = subsample - 1; i >= 1; i--) {
			double z2 = toZoom(i);
			if (z2 / z > 1.2) {
				z = z2;
				result.add(0, (int) (z / zoom100 * 100 + 0.5));
			}
		}
		return result;
	}

	private void viewChanged() {
		if (pageInfo == null)
			return;
		int w = canvas.getCoordinateSpaceWidth(), h = canvas.getCoordinateSpaceHeight();
		int pw = (int) (pageInfo.width * zoom), ph = (int) (pageInfo.height * zoom);
		if (pw < w) {
			centerX = pw / 2;
		} else {
			centerX = Math.max(centerX, w / 2 - pageMargin);
			centerX = Math.min(centerX, pw - w / 2 + pageMargin);
		}
	
		if (ph < h) {
			centerY = ph / 2;
		} else {
			centerY = Math.max(centerY, h / 2 - pageMargin);
			centerY = Math.min(centerY, ph - h / 2 + pageMargin);
		}

		double pw2 = pw + 2 * pageMargin, ph2 = ph + 2 * pageMargin;
		app.getHorizontalScrollbar().setThumb((centerX + pageMargin) / pw2, w / pw2);
		app.getVerticalScrollbar().setThumb((centerY + pageMargin) / ph2, h / ph2);
	}

	public void setChangeListener(ChangeListener changeListener) {
		this.changeListener = changeListener;
	}

	public void redraw() {
		Context2d graphics2d = canvas.getContext2d();
		int w = canvas.getCoordinateSpaceWidth(), h = canvas.getCoordinateSpaceHeight();
		graphics2d.setFillStyle(background);
		graphics2d.fillRect(0, 0, w, h);
		if (pageInfo == null)
			return;

		int subsample = toSubsample(zoom);
		double scale = zoom / toZoom(subsample);
		graphics2d.save();
		int startX = w / 2 - centerX, startY = h / 2 - centerY;
		graphics2d.translate(startX, startY);
		graphics2d.scale(scale, scale);
		graphics2d.translate(-startX, -startY);
		graphics2d.scale(1, -1); // DjVu images have y-axis inverted 

		int tileSize = tileCache.tileSize;
		int pw = (int) (pageInfo.width * zoom), ph = (int) (pageInfo.height * zoom);
		range.xmin = Math.max(0, centerX - w / 2) / tileSize;
		range.xmax = Math.min((int) Math.ceil(pw / scale), centerX + w / 2) / tileSize;
		range.ymin = Math.max(0, centerY - h / 2) / tileSize;
		range.ymax = Math.min((int) Math.ceil(ph / scale), centerY + h / 2) / tileSize;
		imagesArray = tileCache.getTileImages(page, subsample, range , imagesArray);
		for (int y = range.ymin; y <= range.ymax; y++)
			for (int x = range.xmin; x <= range.xmax; x++) {
				CanvasElement canvasElement = imagesArray[y - range.ymin][x - range.xmin];
				graphics2d.drawImage(canvasElement, startX + x * tileSize,
						-startY - y * tileSize - canvasElement.getHeight());
			}
		graphics2d.restore();
		// missing tile graphics may exceed the page boundary
		graphics2d.fillRect(startX + pw, 0, w, h);
		graphics2d.fillRect(0, startY + ph, w, h);
	}

	@Override
	public void pageAvailable(int pageNum) {
		if (pageNum == page) {
			setPage(pageNum);
		}
	}

	@Override
	public void tileAvailable(TileInfo tileInfo) {
		if (tileInfo.page == page)
			redraw();
	}

	private class PanController implements MouseDownHandler, MouseUpHandler, MouseMoveHandler, MouseWheelHandler,
			KeyDownHandler, ScrollPanListener {

		private static final int KEY_PLUS = 187;
		private static final int KEY_MINUS = 189;

		private static final int PAN_STEP = 100;

		private boolean isDown = false;
		private int x, y;

		public PanController(Canvas canvas) {
			canvas.addMouseDownHandler(this);
			canvas.addMouseUpHandler(this);
			canvas.addMouseMoveHandler(this);
			canvas.addMouseWheelHandler(this);
			canvas.addKeyDownHandler(this);
			canvas.setFocus(true);

			app.getHorizontalScrollbar().addScrollPanListener(this);
			app.getVerticalScrollbar().addScrollPanListener(this);
		}

		@Override
		public void onMouseDown(MouseDownEvent event) {
			canvas.setFocus(true);
			int button = event.getNativeButton();
			if (button == NativeEvent.BUTTON_LEFT || button == NativeEvent.BUTTON_MIDDLE) {
				isDown = true;
				x = event.getX();
				y = event.getY();
				event.preventDefault();
				Event.setCapture(canvas.getElement());
			}
		}

		@Override
		public void onMouseUp(MouseUpEvent event) {
			isDown = false;
			Event.releaseCapture(canvas.getElement());
		}

		@Override
		public void onMouseMove(MouseMoveEvent event) {
			if (isDown) {
				pan(x - event.getX(), y - event.getY());
				x = event.getX();
				y = event.getY();
			}
		}

		@Override
		public void onKeyDown(KeyDownEvent event) {
			int key = event.getNativeKeyCode();
			if (event.isControlKeyDown()) {
				if (key == KEY_PLUS) {
					// TODO zoom
					event.preventDefault();
				} else if (key == KEY_MINUS) {
					// TODO zoom
					event.preventDefault();
				}
			} else {
				boolean handled = true;
				switch (key) {
				case KeyCodes.KEY_UP:
					if (!pan(0, -PAN_STEP))
						changePage(page - 1, 0, 1);
					break;
				case KeyCodes.KEY_DOWN:
					if (!pan(0, PAN_STEP))
						changePage(page + 1, 0, -1);
					break;
				case KeyCodes.KEY_LEFT:
					if (!pan(-PAN_STEP, 0))
						changePage(page - 1, 1, 0);
					break;
				case KeyCodes.KEY_RIGHT:
					if (!pan(PAN_STEP, 0))
						changePage(page + 1, -1, 0);
					break;
				case KeyCodes.KEY_PAGEUP:
					if (!pan(0, -canvas.getCoordinateSpaceHeight() + PAN_STEP))
						changePage(page - 1, 0, 1);
					break;
				case KeyCodes.KEY_PAGEDOWN:
				case KeyCodes.KEY_SPACE:
					if (!pan(0, canvas.getCoordinateSpaceHeight() - PAN_STEP))
						changePage(page + 1, 0, -1);
					break;
				case KeyCodes.KEY_HOME:
					changePage(0, -1, -1);
					break;
				case KeyCodes.KEY_END:
					changePage(app.getPageCache().getPageCount() - 1, 1, 1);
					break;
				default:
					handled = false;
				}
				if (handled)
					event.preventDefault();
			}
		}

		@Override
		public void onMouseWheel(MouseWheelEvent event) {
			int delta = event.getDeltaY();
			if (event.isControlKeyDown()) {
				// TODO zoom
				
			} else {
				if (!pan(0, delta * PAN_STEP / 2))
					changePage(page + Integer.signum(delta), 0, -delta);
			}
			event.preventDefault();
		}

		@Override
		public void thumbDragged(double newCenter, boolean isHorizontal) {
			if (isHorizontal) {
				double pw2 = pageInfo.width * zoom + 2 * pageMargin;
				pan((int) (newCenter * pw2 - pageMargin + 0.5) - centerX, 0);
			} else {
				double ph2 = pageInfo.height * zoom + 2 * pageMargin;
				pan(0, (int) (newCenter * ph2 - pageMargin + 0.5) - centerY);
			}
		}

		private boolean pan(int x, int y) {
			int oldX = centerX;
			int oldY = centerY;
			centerX += x;
			centerY += y;
			viewChanged();
			if (centerX != oldX || centerY != oldY) {
				redraw();
				return true;
			}
			return false;
		}

		private void changePage(int targetPage, int horizontalPosition, int verticalPosition) {
			if (targetPage >= 0 && targetPage < app.getPageCache().getPageCount()) {
				if (horizontalPosition < 0)
					centerX = 0;
				else if (horizontalPosition > 0)
					centerX = Integer.MAX_VALUE;
				if (verticalPosition < 0)
					centerY = 0;
				else if (verticalPosition > 0)
					centerY = Integer.MAX_VALUE;
				setPage(targetPage);
			}
		}
	}
}
