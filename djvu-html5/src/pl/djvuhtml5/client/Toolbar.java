package pl.djvuhtml5.client;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class Toolbar extends FlowPanel {

	private SinglePageLayout pageLayout;

	private List<Integer> zoomOptions = Collections.emptyList();

	private final ListBox zoomSelection;

	private TextBox zoomTextBox;

	public Toolbar(Canvas canvas) {
		setStyleName("toolbar");

		new ToolBarHandler(this, canvas);

		Button zoomOutButton = new Button();
		zoomOutButton.setStyleName("toolbarSquareButton");
		zoomOutButton.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
			}
		});
		add(zoomOutButton);

		FlowPanel zoomComboBox = new FlowPanel(SpanElement.TAG);
		zoomTextBox = new TextBox();
		zoomComboBox.setStyleName("zoomComboBox");
		zoomSelection = new ListBox();
		zoomSelection.setStyleName("zoomSelection");
		zoomSelection.addChangeHandler(new ChangeHandler() {
			
			@Override
			public void onChange(ChangeEvent event) {
				zoomSelectionChanged();
			}
		});
		zoomComboBox.add(zoomSelection);
		zoomTextBox.setStyleName("zoomTextBox");
		zoomComboBox.add(zoomTextBox);
		add(zoomComboBox);

		Button zoomInbutton = new Button();
		zoomInbutton.setStyleName("toolbarSquareButton");
		zoomInbutton.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
			}
		});
		add(zoomInbutton);

		setZoomOptions(zoomOptions);
	}

	public void setPageLayout(SinglePageLayout pageLayout) {
		this.pageLayout = pageLayout;
	}

	public void setZoomOptions(List<Integer> newZoomOptions) {
		int selected = zoomSelection.getSelectedIndex();
		boolean fitPage = selected == -1 || selected == zoomSelection.getItemCount() - 1;
		boolean fitWidth = selected == zoomSelection.getItemCount() - 2;
		if (!fitPage && !fitWidth)
			selected = this.zoomOptions.get(selected);

		zoomOptions = newZoomOptions;

		zoomSelection.clear();
		for (int i : zoomOptions) {
			zoomSelection.addItem(i + "%");
		}
		zoomSelection.addItem(DjvuContext.getString("label_fitWidth", "Fit width"));
		zoomSelection.addItem(DjvuContext.getString("label_fitPage", "Fit page"));

		if (fitPage) {
			zoomSelection.setSelectedIndex(zoomOptions.size() + 1);
		} else if (fitWidth) {
			zoomSelection.setSelectedIndex(zoomOptions.size());
		} else {
			int newSelected = Arrays.binarySearch(zoomOptions.toArray(), selected);
			zoomSelection.setSelectedIndex(Math.min(newSelected, zoomOptions.size() - 1));
		}
		zoomSelectionChanged();
	}

	protected void zoomSelectionChanged() {
		if (pageLayout == null)
			return;
		int index = zoomSelection.getSelectedIndex();
		if (index < zoomOptions.size()) {
			pageLayout.setZoom(zoomOptions.get(index));
		} else {
			switch (index - zoomOptions.size()) {
			case 0:
				pageLayout.zoomToFitWidth();
				break;
			case 1:
				pageLayout.zoomToFitPage();
				break;
			default:
				throw new RuntimeException();
			}
		}
		zoomTextBox.setText(zoomSelection.getSelectedItemText());
		zoomSelection.setFocus(false);
	}

	private class ToolBarHandler implements MouseMoveHandler, MouseOverHandler, MouseOutHandler {

		private static final int TOOLBAR_HIDE_DELAY = 1500;

		private boolean isMouseOverToolbar = false;

		private final Widget toolBar;

		private final Timer timer = new Timer() {
			
			@Override
			public void run() {
				if (!isMouseOverToolbar)
					toolBar.addStyleName("toolbarHidden");
			}
		};

		public ToolBarHandler(Widget toolbar, Canvas canvas) {
			this.toolBar = toolbar;
			canvas.addMouseMoveHandler(this);
			toolBar.addDomHandler(this, MouseOverEvent.getType());
			toolBar.addDomHandler(this, MouseOutEvent.getType());
		}

		@Override
		public void onMouseMove(MouseMoveEvent event) {
			toolBar.removeStyleName("toolbarHidden");
			timer.cancel();
			//TODO timer.schedule(TOOLBAR_HIDE_DELAY);
		}

		@Override
		public void onMouseOver(MouseOverEvent event) {
			isMouseOverToolbar = true;
		}

		@Override
		public void onMouseOut(MouseOutEvent event) {
			isMouseOverToolbar = false;
		}
		
	}
}
