package pl.djvuhtml5.client;

import java.util.ArrayList;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.FlowPanel;

public class Scrollbar extends FlowPanel {

	public static interface ScrollPanListener {
		void thumbDragged(double newCenter, boolean isHorizontal);
	}

	private final boolean isHorizontal;

	private final ArrayList<ScrollPanListener> panListeners = new ArrayList<>();

	public Scrollbar(boolean horizontal) {
		isHorizontal = horizontal;
		setStyleName("scrollbar");
		addStyleName(horizontal ? "scrollbarHorizontal" : "scrollbarVertical");

		new PanHandler();
	}

	public void setThumb(double center, double width) {
		Style style = getElement().getStyle();
		if (width >= 1) {
			style.setVisibility(Visibility.HIDDEN);
			return;
		} else {
			style.setVisibility(Visibility.VISIBLE);
		}
		if (isHorizontal) {
			style.setLeft(100 * (center - width / 2), Unit.PCT);
			style.setRight(100 * (1 - center - width / 2), Unit.PCT);
		} else {
			style.setTop(100.0 * (center - width / 2), Unit.PCT);
			style.setBottom(100 * (1 - center - width / 2), Unit.PCT);
		}
	}

	public void addScrollPanListener(ScrollPanListener listener) {
		panListeners.add(listener);
	}

	private class PanHandler implements MouseDownHandler, MouseUpHandler, MouseMoveHandler {

		private boolean isDown = false;
		private int dragPosition;

		public PanHandler() {
			addDomHandler(this, MouseDownEvent.getType());
			addDomHandler(this, MouseUpEvent.getType());
			addDomHandler(this, MouseMoveEvent.getType());
		}

		@Override
		public void onMouseDown(MouseDownEvent event) {
			int button = event.getNativeButton();
			if (button == NativeEvent.BUTTON_LEFT || button == NativeEvent.BUTTON_MIDDLE) {
				isDown = true;
				dragPosition = isHorizontal ? event.getClientX() : event.getClientY();
				event.preventDefault();
				Event.setCapture(getElement());
			}
		}

		@Override
		public void onMouseUp(MouseUpEvent event) {
			isDown = false;
			Event.releaseCapture(getElement());
		}

		@Override
		public void onMouseMove(MouseMoveEvent event) {
			if (isDown) {
				int position =  isHorizontal ? event.getClientX() : event.getClientY();
				int thumbStart, thumbWidth, fullRange;
				if (isHorizontal) {
					thumbStart = getElement().getOffsetLeft();
					thumbWidth = getOffsetWidth();
					fullRange = getParent().getOffsetWidth();
				} else {
					thumbStart = getElement().getOffsetTop();
					thumbWidth = getOffsetHeight();
					fullRange = getParent().getOffsetHeight();
				}
				double newCenter = ((position - dragPosition) + (thumbStart + thumbWidth * 0.5)) / fullRange;
				for (ScrollPanListener listener : panListeners)
					listener.thumbDragged(newCenter, isHorizontal);
				dragPosition = position;
			}
		}
	}
}
