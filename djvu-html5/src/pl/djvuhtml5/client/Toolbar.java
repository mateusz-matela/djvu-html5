package pl.djvuhtml5.client;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
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

	private List<Integer> zoomOptions = Arrays.asList(100);

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
		zoomTextBox.addKeyPressHandler(new KeyPressHandler() {
			
			@Override
			public void onKeyPress(KeyPressEvent event) {
				if (event.getCharCode() == KeyCodes.KEY_ENTER) {
					zoomTypedIn();
				}
			}
		});
		zoomTextBox.addBlurHandler(new BlurHandler() {
			
			@Override
			public void onBlur(BlurEvent event) {
				zoomTypedIn();
			}
		});
		zoomTextBox.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				zoomTextBox.selectAll();
			}
		});
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
		int previousIndex = zoomSelection.getSelectedIndex();

		zoomSelection.clear();
		for (int i : newZoomOptions) {
			zoomSelection.addItem(i + "%");
		}
		zoomSelection.addItem(DjvuContext.getString("label_fitWidth", "Fit width"));
		zoomSelection.addItem(DjvuContext.getString("label_fitPage", "Fit page"));

		if (previousIndex >= zoomOptions.size()) {
			// either "fit with" or "fit page" was selected  
			zoomSelection.setSelectedIndex(newZoomOptions.size() + (zoomOptions.size() - previousIndex));
		} else {
			int zoom = previousIndex >= 0 ? zoomOptions.get(previousIndex) : 100;
			int newSelected = Arrays.binarySearch(newZoomOptions.toArray(), zoom, Collections.reverseOrder());
			if (newSelected < 0)
				newSelected = -newSelected;
			zoomSelection.setSelectedIndex(Math.min(newSelected, newZoomOptions.size() - 1));
		}
		zoomOptions = newZoomOptions;
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

	protected void zoomTypedIn() {
		String digits = zoomTextBox.getText().replaceAll("[^0-9]", "");
		if (digits.isEmpty() || digits.length() > 6) {
			zoomTextBox.setText(pageLayout.getZoom() + "%");
			zoomTextBox.selectAll();
			return;
		}
		int zoom = Math.min(Integer.valueOf(digits), DjvuContext.getMaxZoom());
		zoomSelection.setSelectedIndex(-1);
		pageLayout.setZoom(zoom);
		zoomTextBox.setText(zoom + "%");
		zoomTextBox.setFocus(false);
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
