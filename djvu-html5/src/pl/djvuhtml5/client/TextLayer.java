package pl.djvuhtml5.client;

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

import pl.djvuhtml5.client.PageCache.DecodeListener;

public class TextLayer extends FlowPanel implements DecodeListener, ScrollHandler {

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

		TextLine(int minY, int maxY, TextPage parent, List<Zone> lineTokens) {
			int pageHeight = parent.height;
			Style style = getElement().getStyle();
			double top = (pageHeight - maxY) * 100.0 / pageHeight;
			style.setTop(round(top, PCT_ACCURACY), Unit.PCT);
			double height = (maxY - minY) * 100.0 / pageHeight;
			style.setHeight(round(height, PCT_ACCURACY), Unit.PCT);

			Token lastToken = null;
			for (Zone zone : lineTokens) {
				Token token = new Token(zone, parent);
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

		Token(Zone zone, TextPage page) {
			super(Document.get().createSpanElement());
			String text = page.getText(zone);
			setText(text);
			fullWidth = zone.xmax - zone.xmin;
			this.text = text.trim().replaceAll("\\s+", "");

			Style s = getElement().getStyle();
			double left = zone.xmin * 100.0 / page.width;
			s.setLeft(round(left, PCT_ACCURACY), Unit.PCT);
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

		app.getPageCache().addTextDecodeListener(this);

		fontMeasure = Canvas.createIfSupported().getContext2d();
	}

	@Override
	public void pageDecoded(int pageNum) {
		PageCache pageCache = app.getPageCache();
		DjVuInfo info = pageCache.getInfo(pageNum);
		DjVuText text = pageCache.getText(pageNum);
		int scrollPosition = currentPage >= pages.size() ? 0
				: getElement().getScrollTop() - pages.get(currentPage).getElement().getOffsetTop();

		TextPage page = getPage(pageNum);
		page.setData(info, text);
		if (text.length() > 0) {
			List<Zone> tokens = new ArrayList<>();
			text.page_zone.get_smallest(tokens);
			createTextLines(page, tokens);
		}

		if (currentPage < pages.size())
			getElement().setScrollTop(pages.get(currentPage).getElement().getOffsetTop() + scrollPosition);
	}

	private void createTextLines(TextPage page, List<Zone> tokens) {
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
				new TextLine(minY, maxY, page, lineTokens);
			} else {
				for (Zone lineToken : lineTokens) {
					new TextLine(lineToken.ymin, lineToken.ymax, page, Arrays.asList(lineToken));
				}
			}
		}
	}

	/**
	 * @param left position of page's left edge on the canvas
	 * @param top position of page's top edge on the canvas
	 */
	public void setViewPosition(int pageNum, int left, int top, double zoom) {
		currentPage = pageNum;
		TextPage page = getPage(pageNum);
		page.resize(zoom, false);

		Element layerElement = getElement();
		Element pageElement = page.getElement();
		pageElement.getStyle().setMarginLeft(Math.max(left, 0), Unit.PX);

		if (Math.abs(-left - layerElement.getScrollLeft()) > 0)
			layerElement.setScrollLeft(Math.max(-left, 0));

		int targetScrollTop = pageElement.getOffsetTop() - top;
		if (Math.abs(targetScrollTop - layerElement.getScrollTop()) > 0)
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
			add(page);
			pages.add(page);
		}
		return pages.get(pageNum);
	}


	private static double round(double value, double accuracy) {
		return Math.round((value + accuracy / 2) / accuracy) * accuracy;
	}
}
