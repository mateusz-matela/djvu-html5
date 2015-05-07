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

	private final ComboBox zoomCombo;

	public Toolbar(Canvas canvas) {
		setStyleName("toolbar");

		new ToolBarHandler(this, canvas);

		zoomCombo = new ComboBox() {

			@Override
			protected void valueTypedIn() {
				zoomTypedIn();
			}

			@Override
			protected void valueSelected() {
				zoomSelectionChanged();
			}

			@Override
			protected void changeValueClicked(int direction) {
				zoomChangeClicked(direction);
			}
		};
		add(zoomCombo);
		setZoomOptions(zoomOptions);
	}

	public void setPageLayout(SinglePageLayout pageLayout) {
		this.pageLayout = pageLayout;
	}

	public void setZoomOptions(List<Integer> newZoomOptions) {
		ListBox zoomSelection = zoomCombo.selection;
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
		ListBox zoomSelection = zoomCombo.selection;
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
		zoomCombo.textBox.setText(zoomSelection.getSelectedItemText());
		zoomSelection.setFocus(false);
	}

	protected void zoomTypedIn() {
		TextBox zoomTextBox = zoomCombo.textBox;
		String digits = zoomTextBox.getText().replaceAll("[^0-9]", "");
		if (digits.isEmpty() || digits.length() > 6) {
			zoomTextBox.setText(pageLayout.getZoom() + "%");
			zoomTextBox.selectAll();
			return;
		}
		int zoom = Math.min(Integer.valueOf(digits), DjvuContext.getMaxZoom());
		zoomCombo.selection.setSelectedIndex(-1);
		pageLayout.setZoom(zoom);
		zoomTextBox.setText(zoom + "%");
		zoomTextBox.setFocus(false);
	}

	protected void zoomChangeClicked(int direction) {
		int index = zoomCombo.selection.getSelectedIndex() - direction;
		index = Math.min(index, zoomOptions.size() - 1);
		index = Math.max(index, 0);
		zoomCombo.selection.setSelectedIndex(index);
		zoomSelectionChanged();
	}

	private static abstract class ComboBox extends FlowPanel {
		public final ListBox selection;
		public final TextBox textBox;

		public ComboBox() {
			super(SpanElement.TAG);

			Button leftButton = new Button();
			leftButton.setStyleName("toolbarSquareButton");
			leftButton.addClickHandler(new ClickHandler() {
				
				@Override
				public void onClick(ClickEvent event) {
					changeValueClicked(-1);
				}
			});
			add(leftButton);

			FlowPanel comboPanel = new FlowPanel(SpanElement.TAG);
			add(comboPanel);
			comboPanel.setStyleName("comboBox");
			selection = new ListBox();
			selection.setStyleName("comboBoxSelection");
			selection.addChangeHandler(new ChangeHandler() {
				
				@Override
				public void onChange(ChangeEvent event) {
					valueSelected();
				}
			});
			comboPanel.add(selection);

			textBox = new TextBox();
			textBox.setStyleName("comboBoxText");
			textBox.addKeyPressHandler(new KeyPressHandler() {
				
				@Override
				public void onKeyPress(KeyPressEvent event) {
					if (event.getCharCode() == KeyCodes.KEY_ENTER) {
						valueTypedIn();
					}
				}
			});
			textBox.addBlurHandler(new BlurHandler() {
				
				@Override
				public void onBlur(BlurEvent event) {
					valueTypedIn();
				}
			});
			textBox.addClickHandler(new ClickHandler() {
				
				@Override
				public void onClick(ClickEvent event) {
					textBox.selectAll();
				}
			});
			comboPanel.add(textBox);

			Button rightButton = new Button();
			rightButton.setStyleName("toolbarSquareButton");
			rightButton.addClickHandler(new ClickHandler() {
				
				@Override
				public void onClick(ClickEvent event) {
					changeValueClicked(1);
				}
			});
			add(rightButton);
		}

		protected abstract void valueTypedIn();

		protected abstract void valueSelected();

		protected abstract void changeValueClicked(int direction);
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
