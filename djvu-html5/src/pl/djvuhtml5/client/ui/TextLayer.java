package pl.djvuhtml5.client.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.lizardtech.djvu.DjVuInfo;
import com.lizardtech.djvu.text.DjVuText;
import com.lizardtech.djvu.text.DjVuText.Zone;

import pl.djvuhtml5.client.DataStore;
import pl.djvuhtml5.client.Djvu_html5;

public class TextLayer extends FlowPanel implements ScrollHandler {

	private static final String PAGE_STYLE_VISIBLE = "visibleTextPage";

	private class TextPage extends FlowPanel {

		final List<TextLine> textLines = new ArrayList<>();
		DjVuText text;
		int width, height;

		TextPage() {
			setStyleName("textPage");
			setPixelSize(1, 1);
		}

		void setData(DjVuInfo info, DjVuText text) {
			this.text = text;
			width = info.width;
			height = info.height;
			setPixelSize(width, height);
		}

		void resize(double zoom, boolean force) {
			int w = (int) (width * zoom + 0.5);
			int h = (int) (height * zoom + 0.5);
			if (force || w != getOffsetWidth() || h != getOffsetHeight()) {
				setPixelSize(w, h);
				for (TextLine textLine : textLines)
					textLine.resize(zoom);
			}
		}

		String getText(Zone zone) {
			return text.getString(zone.text_start, zone.text_start + zone.text_length);
		}
	}

	private class TextLine extends FlowPanel {
		final List<Token> tokens = new ArrayList<>();

		TextLine(int minY, int maxY, int prevY, TextPage parent, List<Zone> lineTokens) {
			Style style = getElement().getStyle();
			double marginTop = round((prevY - maxY) * 100.0 / parent.width, PCT_ACCURACY);
			if (parent.getWidgetCount() == 0) {
				style.setPaddingTop(marginTop, Unit.PCT);
			} else {
				style.setMarginTop(marginTop, Unit.PCT);
			}
			style.setHeight(round((maxY - minY) * 100.0 / parent.height, PCT_ACCURACY), Unit.PCT);
			
			Token lastToken = null;
			int prevXmax = 0;
			for (Zone zone : lineTokens) {
				Token token = new Token(zone, prevXmax, parent);
				if (token.text.isEmpty()) {
					if (lastToken != null)
						lastToken.setText(lastToken.getText() + token.getText());
					continue;
				}
				add(token);
				lastToken = token;
				if (token.text.length() > 1 || lineTokens.size() == 1) {
					tokens.add(token);
				}
				prevXmax = zone.xmax;
			}

			parent.textLines.add(this);
			parent.add(this);
		}

		void resize(double zoom) {
			double fontSize = 0;
			for (Token token : tokens)
				fontSize += token.getFontSize(zoom);
			fontSize = round(fontSize / tokens.size(), FONT_ACCURACY);
			getElement().getStyle().setFontSize(fontSize, Unit.PX);
			for (Token token : tokens)
				token.adjustSpacing(fontSize, zoom);
		}
	}

	private class Token extends Label {
		/** Token's text stripped of excessive whitespace, in contrast to {@link #getText()} */
		final String text;
		final double fullWidth;

		Token(Zone zone, int prevXmax, TextPage page) {
			super(Document.get().createSpanElement());
			String text = page.getText(zone);
			setText(text);
			fullWidth = zone.xmax - zone.xmin;
			this.text = text.trim().replaceAll("\\s+", "");

			Style s = getElement().getStyle();
			double marginLeft = (zone.xmin - prevXmax) * 100.0 / page.width;
			s.setMarginLeft(round(marginLeft, PCT_ACCURACY), Unit.PCT);
			double width = fullWidth * 100.0 / page.width;
			s.setWidth(round(width, PCT_ACCURACY), Unit.PCT);
		}

		double getFontSize(double zoom) {
			final double width = fullWidth * zoom;
			double fontSize = previousFontSize;
			double diff = Double.MAX_VALUE, prevDiff;
			double ratio = 1;
			int count = 0;
			do {
				fontSize *= ratio;
				ratio = width / getWidth(fontSize);
				prevDiff = diff;
				diff = Math.abs(1 - ratio);
			} while (count++ < 6 && diff > 0.05 && diff < prevDiff);
			previousFontSize = fontSize;
			return fontSize;
		}

		void adjustSpacing(double fontSize, double zoom) {
			int textLength = text.length();
			if (textLength < 2)
				return;
			double width = getWidth(fontSize);
			double spacing = (fullWidth * zoom - width) / (textLength - 1);
			getElement().getStyle().setProperty("letterSpacing", round(spacing, 0.1), Unit.PX);
		}

		private double getWidth(double fontSize) {
			if (fontFamily == null)
				fontFamily = getComputedFontFamily(getElement());
			fontMeasure.setFont(fontSize + "px " + fontFamily);
			return fontMeasure.measureText(text).getWidth();
		}

		private native String getComputedFontFamily(JavaScriptObject element) /*-{
		    return document.defaultView.getComputedStyle(element, null)["fontFamily"];
		}-*/;
	}

	private static final double PCT_ACCURACY = 0.0001;
	private static final double FONT_ACCURACY = 0.5;

	private final Djvu_html5 app;

	private final List<TextPage> pages = new ArrayList<>();

	private final Context2d fontMeasure;
	private String fontFamily;
	private double previousFontSize = 20;

	private int currentPage = 0;

	public TextLayer(Djvu_html5 app) {
		this.app = app;
		setStyleName("textLayer");
		getElement().setAttribute("tabindex", "-1");

		addDomHandler(this, ScrollEvent.getType());

		app.getDataStore().addTextListener(this::textAvailable);

		fontMeasure = Canvas.createIfSupported().getContext2d();
	}

	private void textAvailable(int pageNum) {
		DataStore dataStore = app.getDataStore();
		DjVuInfo info = dataStore.getPageInfo(pageNum);
		DjVuText text = dataStore.getText(pageNum);

		TextPage page = getPage(pageNum);
		page.setData(info, text);
		if (text.length() > 0) {
			List<Zone> tokens = new ArrayList<>();
			text.page_zone.get_smallest(tokens);
			createTextLines(page, tokens);
			if (pageNum == currentPage)
				app.getPageLayout().setPage(pageNum);
		}
	}

	private void createTextLines(TextPage page, List<Zone> tokens) {
		int prevY = page.height;
		while (!tokens.isEmpty()) {
			List<Zone> lineTokens = new ArrayList<>();
			Zone lastToken = null;
			int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;

			while (!tokens.isEmpty()) {
				Zone token = tokens.get(0);
				boolean hasMultipleChars = page.getText(token).trim().length() > 1;
				if (lastToken != null) { //make sure it's the same line
					if (token.xmin < lastToken.xmax)
						break;
					int commonPart = Math.min(maxY, token.ymax) - Math.max(minY, token.ymin);
					int total = Math.max(maxY, token.ymax) - Math.min(minY, token.ymin);
					if (hasMultipleChars) {
						if (commonPart <= 0)
							break;
					} else {
						if (commonPart * 2 <= total)
							break;
					}
				}
				tokens.remove(0);
				lineTokens.add(token);
				if (hasMultipleChars) {
					lastToken = token;
					minY = Math.min(minY, token.ymin);
					maxY = Math.max(maxY, token.ymax);
				}
			}
			if (lastToken != null) {
				new TextLine(minY, maxY, prevY, page, lineTokens);
				prevY = minY;
			} else {
				for (Zone lineToken : lineTokens) {
					new TextLine(lineToken.ymin, lineToken.ymax, prevY, page, Arrays.asList(lineToken));
					prevY = lineToken.ymin;
				}
			}
		}
	}

	/**
	 * @param left position of page's left edge on the canvas
	 * @param top position of page's top edge on the canvas
	 */
	public void setViewPosition(int pageNum, int left, int top, double zoom) {
		boolean pageChanged = currentPage != pageNum;
		TextPage page = getPage(pageNum);
		if (pageChanged) {
			getPage(currentPage).removeStyleName(PAGE_STYLE_VISIBLE);
			page.addStyleName(PAGE_STYLE_VISIBLE);
		}
		page.resize(zoom, pageChanged);
		currentPage = pageNum;

		Element layerElement = getElement();
		Element pageElement = page.getElement();
		pageElement.getStyle().setMarginLeft(Math.max(left, 0), Unit.PX);

		if (layerElement.getScrollLeft() != -left)
			layerElement.setScrollLeft(Math.max(-left, 0));

		int targetScrollTop = pageElement.getOffsetTop() - top;
		if (layerElement.getScrollTop() != targetScrollTop)
			layerElement.setScrollTop(targetScrollTop);
	}

	@Override
	public void onScroll(ScrollEvent event) {
		int scrollTop = getElement().getScrollTop();
		int page = currentPage;
		Element pageElement = pages.get(page).getElement();
		while (page > 0 && pageElement.getOffsetTop() > scrollTop) {
			pageElement = pages.get(--page).getElement();
		}
		while (page + 1 < pages.size() && pageElement.getOffsetTop() + pageElement.getOffsetHeight() < scrollTop) {
			pageElement = pages.get(++page).getElement();
		}
		int left = pageElement.getOffsetLeft() - getElement().getScrollLeft();
		int top = pageElement.getOffsetTop() - scrollTop;
		app.getPageLayout().externalScroll(page, left, top);
	}

	private TextPage getPage(int pageNum) {
		while (pageNum >= pages.size()) {
			TextPage page = new TextPage();
			if (pages.size() == currentPage)
				page.addStyleName(PAGE_STYLE_VISIBLE);
			add(page);
			pages.add(page);
		}
		return pages.get(pageNum);
	}


	private static double round(double value, double accuracy) {
		return Math.round((value + accuracy / 2) / accuracy) * accuracy;
	}
}
