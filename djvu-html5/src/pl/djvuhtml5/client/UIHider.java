package pl.djvuhtml5.client;

import java.util.ArrayList;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;

public class UIHider implements MouseMoveHandler, MouseOverHandler, MouseOutHandler {

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

	private boolean isMouseOverUI = false;

	private final Timer timer = new Timer() {

		@Override
		public void run() {
			if (!isMouseOverUI) {
				for (UIElement element : uiElements)
					element.widget.addStyleName(element.hiddenStyleName);
			}
		}
	};

	public void addUIElement(Widget widget, String hiddenStyleName) {
		uiElements.add(new UIElement(widget, hiddenStyleName));
		widget.addDomHandler(this, MouseOverEvent.getType());
		widget.addDomHandler(this, MouseOutEvent.getType());
	}

	public UIHider(Canvas canvas) {
		canvas.addMouseMoveHandler(this);
	}

	@Override
	public void onMouseMove(MouseMoveEvent event) {
		for (UIElement element : uiElements)
			element.widget.removeStyleName(element.hiddenStyleName);
		timer.cancel();
		timer.schedule(UI_HIDE_DELAY);
	}

	@Override
	public void onMouseOver(MouseOverEvent event) {
		isMouseOverUI = true;
	}

	@Override
	public void onMouseOut(MouseOutEvent event) {
		isMouseOverUI = false;
	}
}
