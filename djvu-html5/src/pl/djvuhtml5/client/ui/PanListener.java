package pl.djvuhtml5.client.ui;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Touch;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.dom.client.TouchEndEvent;
import com.google.gwt.event.dom.client.TouchEndHandler;
import com.google.gwt.event.dom.client.TouchMoveEvent;
import com.google.gwt.event.dom.client.TouchMoveHandler;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.event.dom.client.TouchStartHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;

public abstract class PanListener implements MouseDownHandler, MouseUpHandler, MouseMoveHandler,
TouchStartHandler, TouchMoveHandler, TouchEndHandler {

	protected Widget widget;

	protected boolean isMouseDown = false;
	private Integer touchId = null;
	protected int x, y;

	public PanListener(Widget widget) {
		this.widget = widget;
		widget.addDomHandler(this, MouseDownEvent.getType());
		widget.addDomHandler(this, MouseUpEvent.getType());
		widget.addDomHandler(this, MouseMoveEvent.getType());
		widget.addDomHandler(this, TouchStartEvent.getType());
		widget.addDomHandler(this, TouchEndEvent.getType());
		widget.addDomHandler(this, TouchMoveEvent.getType());
	}

	@Override
	public void onMouseDown(MouseDownEvent event) {
		int button = event.getNativeButton();
		if ((button == NativeEvent.BUTTON_LEFT || button == NativeEvent.BUTTON_MIDDLE) && touchId == null) {
			isMouseDown = true;
			x = event.getX();
			y = event.getY();
			event.preventDefault();
			Event.setCapture(widget.getElement());
		}
	}

	@Override
	public void onMouseUp(MouseUpEvent event) {
		isMouseDown = false;
		Event.releaseCapture(widget.getElement());
	}

	@Override
	public void onMouseMove(MouseMoveEvent event) {
		if (isMouseDown) {
			pan(event.getX() - x, event.getY() - y);
			x = event.getX();
			y = event.getY();
		}
	}

	@Override
	public void onTouchStart(TouchStartEvent event) {
		if (touchId != null || isMouseDown)
			return;
		Touch touch = event.getTouches().get(0);
		touchId = touch.getIdentifier();
		x = touch.getClientX();
		y = touch.getClientY();
		event.preventDefault();
	}

	@Override
	public void onTouchEnd(TouchEndEvent event) {
		if (touchId == null)
			return;
		JsArray<Touch> touches = event.getTouches();
		for (int i = 0; i < touches.length(); i++) {
			Touch touch = touches.get(i);
			if (touch.getIdentifier() == touchId)
				return;
		}
		touchId = null;
		event.preventDefault();
	}

	@Override
	public void onTouchMove(TouchMoveEvent event) {
		if (touchId == null)
			return;
		JsArray<Touch> touches = event.getTouches();
		for (int i = 0; i < touches.length(); i++) {
			Touch touch = touches.get(i);
			if (touch.getIdentifier() != touchId)
				continue;
			pan(touch.getClientX() - x, touch.getClientY() - y);
			x = touch.getClientX();
			y = touch.getClientY();
			event.preventDefault();
		}
	}

	protected abstract void pan(int dx, int dy);
}
