package pl.djvuhtml5.client.ui;

import java.util.ArrayList;

import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.event.dom.client.TouchStartHandler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;

public class UIHider implements MouseMoveHandler, MouseOverHandler, MouseOutHandler, KeyDownHandler, TouchStartHandler,
		ScrollHandler {

	private static class UIElement {
		public final Widget widget;
		public final String hiddenStyleName;

		public UIElement(Widget widget, String hiddenStyleName) {
			this.widget = widget;
			this.hiddenStyleName = hiddenStyleName;
		}
	}

	private final int uiHideDelay;

	private final ArrayList<UIElement> uiElements = new ArrayList<>();

	private int previousX, previousY;
	private boolean isMouseOverUI = false;

	private final Timer timer = new Timer() {
		@Override
		public void run() {
			hideUI();
		}
	};

	public UIHider(int uiHideDelay, Widget textLayer) {
		this.uiHideDelay = uiHideDelay;
		textLayer.addDomHandler(this, MouseMoveEvent.getType());
		textLayer.addDomHandler(this, KeyDownEvent.getType());
		textLayer.addDomHandler(this, ScrollEvent.getType());
		textLayer.addDomHandler(this, TouchStartEvent.getType());
	}

	public void addUIElement(Widget widget, String hiddenStyleName) {
		uiElements.add(new UIElement(widget, hiddenStyleName));
		widget.addDomHandler(this, MouseOverEvent.getType());
		widget.addDomHandler(this, MouseOutEvent.getType());
	}

	private void showUI() {
		for (UIElement element : uiElements)
			element.widget.removeStyleName(element.hiddenStyleName);
		timer.cancel();
		timer.schedule(uiHideDelay);
	}

	private void hideUI() {
		if (isMouseOverUI || isUIFocused())
			return;
		for (UIElement element : uiElements)
			element.widget.addStyleName(element.hiddenStyleName);
	}

	private boolean isUIFocused() {
		Element focusedElement = getFocusedElement();
		while (focusedElement != null) {
			for (UIElement element : uiElements) {
				if (element.widget.getElement() == focusedElement)
					return true;
			}
			focusedElement = focusedElement.getParentElement();
		}
		return false;
	}

	private native final static Element getFocusedElement() /*-{
		return $doc.activeElement;
	}-*/;

	@Override
	public void onMouseMove(MouseMoveEvent event) {
		if (previousX == event.getX() && previousY == event.getY())
			return;
		previousX = event.getX();
		previousY = event.getY();
		showUI();
	}

	@Override
	public void onTouchStart(TouchStartEvent event) {
		isMouseOverUI = false;
		showUI();
	}


	@Override
	public void onMouseOver(MouseOverEvent event) {
		isMouseOverUI = true;
	}

	@Override
	public void onMouseOut(MouseOutEvent event) {
		isMouseOverUI = false;
	}

	@Override
	public void onKeyDown(KeyDownEvent event) {
		showUI();
	}

	@Override
	public void onScroll(ScrollEvent event) {
		showUI();
	}
}
