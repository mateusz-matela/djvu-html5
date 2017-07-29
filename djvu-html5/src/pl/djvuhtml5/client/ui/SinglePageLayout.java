package pl.djvuhtml5.client.ui;

import static pl.djvuhtml5.client.TileRenderer.MAX_SUBSAMPLE;
import static pl.djvuhtml5.client.TileRenderer.toSubsample;
import static pl.djvuhtml5.client.TileRenderer.toZoom;

import java.util.ArrayList;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.dom.client.CanvasElement;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseWheelEvent;
import com.google.gwt.event.dom.client.MouseWheelHandler;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.http.client.UrlBuilder;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.lizardtech.djvu.DjVuInfo;
import com.lizardtech.djvu.GRect;

import pl.djvuhtml5.client.DataStore;
import pl.djvuhtml5.client.DjvuContext;
import pl.djvuhtml5.client.Djvu_html5;
import pl.djvuhtml5.client.ui.Scrollbar.ScrollPanListener;

public class SinglePageLayout {

	interface ChangeListener {
		void pageChanged(int currentPage);
		void zoomChanged(int currentZoom);
	}

	private final Djvu_html5 app;

	private double zoom = 0;

	private double zoom100;

	private int page;

	private final DataStore dataStore;

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

	private final boolean locationUpdateEnabled;
	private Timer locationUpdater;

	public SinglePageLayout(Djvu_html5 app) {
		this.app = app;
		this.dataStore = app.getDataStore();

		this.canvas = app.getCanvas();

		this.background = DjvuContext.getBackground();
		this.pageMargin = DjvuContext.getPageMargin();

		new PanController();

		boolean pageParam = false;
		try {
			page = Integer.parseInt(Window.Location.getParameter("p")) - 1;
			pageParam = true;
		} catch (Exception e) {
			page = 0;
		}
		locationUpdateEnabled = pageParam || DjvuContext.getLocationUpdateEnabled();
		DjvuContext.setPage(page);

		dataStore.addPageCountListener(pageCount -> {
			app.getToolbar().setPageCount(pageCount);
			int newPage = Math.max(0, Math.min(pageCount - 1, page));
			setPage(newPage);
		});
		dataStore.addInfoListener(pageNum -> {
			if (pageNum == page)
				setPage(pageNum);
		});
		dataStore.addTileListener(pageNum -> {
			if (pageNum == page)
				redraw();
		});
	}

	public void setPage(int pageNum) {
		page = pageNum;
		DjVuInfo newInfo = app.getDataStore().getPageInfo(pageNum);
		if (newInfo != null) {
			pageInfo = newInfo;
			app.getToolbar().setZoomOptions(findZoomOptions());
		} else {
			pageInfo = null;
		}
		viewChanged();
		DjvuContext.setPage(pageNum);
		if (changeListener != null)
			changeListener.pageChanged(pageNum);
		scheduleURLUpdate();
	}

	private void scheduleURLUpdate() {
		if (!locationUpdateEnabled)
			return;
		if (locationUpdater == null) {
			locationUpdater = new Timer() {
				@Override
				public void run() {
					UrlBuilder url = Window.Location.createUrlBuilder();
					if (page > 0) {
						url.setParameter("p", Integer.toString(page + 1));
					} else {
						url.removeParameter("p");
					}
					updateURLWithoutReloading(url.buildString());
				}

				private native void updateURLWithoutReloading(String newUrl) /*-{
					$wnd.history.replaceState(newUrl, "", newUrl);
				}-*/;
			};
		}
		locationUpdater.cancel();
		locationUpdater.schedule(500);
	}

	public int getPage() {
		return page;
	}

	public void canvasResized() {
		viewChanged();
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

	public double getPreciseZoom() {
		return zoom;
	}

	private void doSetZoom(double zoom) {
		if (this.zoom == zoom)
			return;
		centerX *= zoom / this.zoom;
		centerY *= zoom / this.zoom;
		this.zoom = zoom;
		viewChanged();
		if (changeListener != null)
			changeListener.zoomChanged(getZoom());
	}

	private ArrayList<Integer> findZoomOptions() {
		ArrayList<Integer> result = new ArrayList<>();
		result.add(100);
		final int screenDPI = DjvuContext.getScreenDPI();
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
		if (pageInfo == null) {
			redraw();
			return;
		}
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
		if (app.getTextLayer() != null)
			app.getTextLayer().setViewPosition(page, w / 2 - centerX, h / 2 - centerY, zoom);

		redraw();
	}

	/**
	 * @param left position of page's left edge on the canvas
	 * @param top position of page's top edge on the canvas
	 */
	public void externalScroll(int page, int left, int top) {
		int w = canvas.getCoordinateSpaceWidth(), h = canvas.getCoordinateSpaceHeight();
		if (page == this.page && centerX == w / 2 - left && centerY == h / 2 - top)
			return;
		centerX = w / 2 - left;
		centerY = h / 2 - top;
		if (page != this.page) {
			setPage(page);
		} else {
			viewChanged();
		}
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

		int tileSize = DjvuContext.getTileSize();
		int pw = (int) (pageInfo.width * zoom), ph = (int) (pageInfo.height * zoom);
		range.xmin = (int) (Math.max(0, centerX - w * 0.5) / tileSize / scale);
		range.xmax = (int) Math.ceil(Math.min(pw, centerX + w * 0.5) / tileSize / scale);
		range.ymin = (int) (Math.max(0, centerY - h * 0.5) / tileSize / scale);
		range.ymax = (int) Math.ceil(Math.min(ph, centerY + h * 0.5) / tileSize / scale);
		imagesArray = dataStore.getTileImages(page, subsample, range , imagesArray);
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

		DjvuContext.setTileRange(range, subsample);
	}

	private class PanController extends PanListener implements MouseWheelHandler, KeyDownHandler, ScrollPanListener {

		private static final int KEY_PLUS = 187;
		private static final int KEY_MINUS = 189;

		private static final int PAN_STEP = 100;

		public PanController() {
			super(canvas);
			canvas.addMouseWheelHandler(this);
			canvas.addKeyDownHandler(this);
			canvas.setFocus(true);

			TextLayer textLayer = app.getTextLayer();
			if (textLayer != null) {
				textLayer.addDomHandler(this, MouseWheelEvent.getType());
				textLayer.addDomHandler(this, KeyDownEvent.getType());
			}

			app.getHorizontalScrollbar().addScrollPanListener(this);
			app.getVerticalScrollbar().addScrollPanListener(this);
		}

		@Override
		public void onMouseDown(MouseDownEvent event) {
			canvas.setFocus(true);
			super.onMouseDown(event);
		}

		@Override
		public void onTouchStart(TouchStartEvent event) {
			canvas.setFocus(true);
			super.onTouchStart(event);
		}

		@Override
		public void onKeyDown(KeyDownEvent event) {
			int key = event.getNativeKeyCode();
			if (event.isControlKeyDown()) {
				if (key == KEY_PLUS || key == KEY_MINUS) {
					app.getToolbar().zoomChangeClicked(key == KEY_PLUS ? 1 : -1);
					event.preventDefault();
				}
			} else if (!event.isShiftKeyDown()) {
				boolean handled = true;
				switch (key) {
				case KeyCodes.KEY_UP:
					if (!tryPan(0, -PAN_STEP))
						changePage(page - 1, 0, 1);
					break;
				case KeyCodes.KEY_DOWN:
					if (!tryPan(0, PAN_STEP))
						changePage(page + 1, 0, -1);
					break;
				case KeyCodes.KEY_LEFT:
					if (!tryPan(-PAN_STEP, 0))
						changePage(page - 1, 1, 0);
					break;
				case KeyCodes.KEY_RIGHT:
					if (!tryPan(PAN_STEP, 0))
						changePage(page + 1, -1, 0);
					break;
				case KeyCodes.KEY_PAGEUP:
					if (!tryPan(0, -canvas.getCoordinateSpaceHeight() + PAN_STEP))
						changePage(page - 1, 0, 1);
					break;
				case KeyCodes.KEY_PAGEDOWN:
				case KeyCodes.KEY_SPACE:
					if (!tryPan(0, canvas.getCoordinateSpaceHeight() - PAN_STEP))
						changePage(page + 1, 0, -1);
					break;
				case KeyCodes.KEY_HOME:
					changePage(0, -1, -1);
					break;
				case KeyCodes.KEY_END:
					changePage(dataStore.getPageCount() - 1, 1, 1);
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
				app.getToolbar().zoomChangeClicked(Integer.signum(-delta));
			} else {
				if (!tryPan(0, delta * PAN_STEP / 2))
					changePage(page + Integer.signum(delta), 0, -delta);
			}
			event.preventDefault();
		}

		@Override
		public void thumbDragged(double newCenter, boolean isHorizontal) {
			if (pageInfo == null)
				return;
			if (isHorizontal) {
				double pw2 = pageInfo.width * zoom + 2 * pageMargin;
				tryPan((int) (newCenter * pw2 - pageMargin + 0.5) - centerX, 0);
			} else {
				double ph2 = pageInfo.height * zoom + 2 * pageMargin;
				tryPan(0, (int) (newCenter * ph2 - pageMargin + 0.5) - centerY);
			}
		}

		@Override
		protected void pan(int dx, int dy) {
			tryPan(-dx, -dy);
		}

		private boolean tryPan(int dx, int dy) {
			int oldX = centerX;
			int oldY = centerY;
			centerX += dx;
			centerY += dy;
			viewChanged(); // applies constraints to x,y
			if (centerX != oldX || centerY != oldY) {
				redraw();
				return true;
			}
			return false;
		}

		private void changePage(int targetPage, int horizontalPosition, int verticalPosition) {
			if (targetPage >= 0 && targetPage < dataStore.getPageCount()) {
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
