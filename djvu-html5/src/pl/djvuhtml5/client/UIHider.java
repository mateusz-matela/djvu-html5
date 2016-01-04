package pl.djvuhtml5.client;

import java.util.ArrayList;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.dom.client.TouchEndEvent;
import com.google.gwt.event.dom.client.TouchEndHandler;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.event.dom.client.TouchStartHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;

public class UIHider implements MouseMoveHandler, TouchStartHandler, TouchEndHandler, MouseOverHandler, MouseOutHandler,
		MouseDownHandler, MouseUpHandler, KeyDownHandler, FocusHandler, BlurHandler {

	private static class UIElement {
		public final Widget widget;
		public final String hiddenStyleName;

		public UIElement(Widget widget, String hiddenStyleName) {
			this.widget = widget;
			this.hiddenStyleName = hiddenStyleName;
		}
	}

	private static final int UI_HIDE_DELAY = 1500;

	private final ArrayList<UIElement> uiElements = new ArrayList<>();

	private int previousX, previousY;
	private boolean isMouseOverUI = false;
	private boolean isMouseDown = false;
	private boolean isTouchDown = false;
	private boolean hasCanvasFocus = false;

	private final Timer timer = new Timer() {

		@Override
		public void run() {
			if (!isMouseOverUI && !isMouseDown && !isTouchDown && hasCanvasFocus) {
				for (UIElement element : uiElements)
					element.widget.addStyleName(element.hiddenStyleName);
			}
		}
	};

	public void addUIElement(Widget widget, String hiddenStyleName) {
		uiElements.add(new UIElement(widget, hiddenStyleName));
		widget.addDomHandler(this, MouseOverEvent.getType());
		widget.addDomHandler(this, MouseOutEvent.getType());
		widget.addDomHandler(this, MouseDownEvent.getType());
		widget.addDomHandler(this, MouseUpEvent.getType());
		widget.addDomHandler(this, TouchStartEvent.getType());
		widget.addDomHandler(this, TouchEndEvent.getType());
	}

	public UIHider(Canvas canvas) {
		canvas.addMouseMoveHandler(this);
		canvas.addTouchStartHandler(this);
		canvas.addTouchEndHandler(this);
		canvas.addKeyDownHandler(this);
		canvas.addFocusHandler(this);
		canvas.addBlurHandler(this);
	}

	private void showUI() {
		for (UIElement element : uiElements)
			element.widget.removeStyleName(element.hiddenStyleName);
		timer.cancel();
		timer.schedule(UI_HIDE_DELAY);
	}

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
		isTouchDown = true;
		showUI();
	}

	@Override
	public void onTouchEnd(TouchEndEvent event) {
		if (event.getTouches().length() == 0)
			isTouchDown = false;
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
	public void onMouseDown(MouseDownEvent event) {
		isMouseDown = true;
	}

	@Override
	public void onMouseUp(MouseUpEvent event) {
		isMouseDown = false;
	}

	@Override
	public void onKeyDown(KeyDownEvent event) {
		showUI();
	}

	@Override
	public void onFocus(FocusEvent event) {
		hasCanvasFocus = true;
	}

	@Override
	public void onBlur(BlurEvent event) {
		hasCanvasFocus = false;
	}
}
