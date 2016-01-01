package pl.djvuhtml5.client;

import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.user.client.ui.FlowPanel;

public class Scrollbar extends FlowPanel {

	private final boolean isHorizontal;

	public Scrollbar(boolean horizontal) {
		isHorizontal = horizontal;
		setStyleName("scrollbar");
		addStyleName(horizontal ? "scrollbarHorizontal" : "scrollbarVertical");
	}

	public void setRange(int start, int end, int fullRange) {
		Style style = getElement().getStyle();
		if (start < 0 || end > fullRange) {
			style.setVisibility(Visibility.HIDDEN);
			return;
		} else {
			style.setVisibility(Visibility.VISIBLE);
		}
		if (isHorizontal) {
			style.setLeft(100.0 * start / fullRange, Unit.PCT);
			style.setRight(100 - 100.0 * end / fullRange, Unit.PCT);
		} else {
			style.setTop(100.0 * start / fullRange, Unit.PCT);
			style.setBottom(100 - 100.0 * end / fullRange, Unit.PCT);
		}
	}
}
