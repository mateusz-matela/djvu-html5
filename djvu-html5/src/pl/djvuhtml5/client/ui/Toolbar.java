package pl.djvuhtml5.client.ui;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;

import pl.djvuhtml5.client.DjvuContext;
import pl.djvuhtml5.client.ui.SinglePageLayout.ChangeListener;

public class Toolbar extends FlowPanel {

	private SinglePageLayout pageLayout;

	private List<Integer> zoomOptions = Arrays.asList(100);

	private final SelectionPanel zoomPanel;

	private int pagesCount;

	private final SelectionPanel pagePanel;

	public Toolbar() {
		setStyleName("toolbar");

		zoomPanel = new SelectionPanel("buttonZoomOut", "buttonZoomIn") {

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

			@Override
			protected boolean isButtonEnabled(int direction) {
				if (direction == -1) {
					return pageLayout != null && pageLayout.getZoom() > zoomOptions.get(zoomOptions.size() - 1);
				} else {
					return pageLayout != null && pageLayout.getZoom() < zoomOptions.get(0);
				}
			}
		};
		add(zoomPanel);
		setZoomOptions(zoomOptions);

		pagePanel = new SelectionPanel("buttonPagePrev", "buttonPageNext") {

			@Override
			protected void valueTypedIn() {
				pageTypedIn();
			}

			@Override
			protected void valueSelected() {
				pageSelectionChanged();
			}

			@Override
			protected void changeValueClicked(int direction) {
				pageChangeClicked(direction);
			}
		};
		add(pagePanel);

		addDomHandler(new KeyDownHandler() {
			@Override
			public void onKeyDown(KeyDownEvent event) {
				final int KEY_PLUS = 187, KEY_MINUS = 189;
				int key = event.getNativeKeyCode();
				if (event.isControlKeyDown() && (key == KEY_PLUS || key == KEY_MINUS)) {
					zoomChangeClicked(key == KEY_PLUS ? 1 : -1);
					event.preventDefault();
				}
			}
		}, KeyDownEvent.getType());
	}

	public void setPageLayout(SinglePageLayout pageLayout) {
		this.pageLayout = pageLayout;
		pageLayout.setChangeListener(new ChangeListener() {

			@Override
			public void zoomChanged(int currentZoom) {
				zoomPanel.textBox.setText(currentZoom + "%");
				zoomPanel.updateButtons();
			}

			@Override
			public void pageChanged(int currentPage) {
				pagePanel.textBox.setText((currentPage + 1) + "");
				pagePanel.selection.setSelectedIndex(currentPage);
				pagePanel.updateButtons();
			}
		});
		zoomPanel.updateButtons();
	}

	public void setZoomOptions(List<Integer> newZoomOptions) {
		ListBox zoomSelection = zoomPanel.selection;
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
			int zoom = pageLayout != null ? pageLayout.getZoom() : 100;
			int newSelected = Arrays.binarySearch(newZoomOptions.toArray(), zoom, Collections.reverseOrder());
			if (newSelected >= 0) {
				zoomSelection.setSelectedIndex(Math.min(newSelected, newZoomOptions.size() - 1));
				zoomOptions = newZoomOptions;
				zoomSelectionChanged();
			} else {
				zoomSelection.setSelectedIndex(-1);
				zoomOptions = newZoomOptions;
			}
		}
		zoomPanel.updateButtons();
	}

	public void setPageCount(int pagesCount) {
		this.pagesCount = pagesCount;
		ListBox pageSelection = pagePanel.selection;
		pageSelection.clear();
		for (int i = 1; i <= pagesCount; i++) {
			pageSelection.addItem(i + "");
		}
		pagePanel.updateButtons();
	}

	protected void zoomSelectionChanged() {
		if (pageLayout == null)
			return;
		ListBox zoomSelection = zoomPanel.selection;
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
		zoomPanel.textBox.setText(zoomSelection.getSelectedItemText());
		zoomSelection.setFocus(false);
	}

	protected void zoomTypedIn() {
		if (pageLayout == null)
			return;
		TextBox zoomTextBox = zoomPanel.textBox;
		String digits = zoomTextBox.getText().replaceAll("[^0-9]", "");
		if (digits.isEmpty() || digits.length() > 6) {
			zoomTextBox.setText(pageLayout.getZoom() + "%");
			zoomTextBox.selectAll();
			return;
		}
		int zoom = Math.min(Integer.valueOf(digits), DjvuContext.getMaxZoom());
		zoom = Math.max(zoom, zoomOptions.get(zoomOptions.size() - 1));
		zoomPanel.selection.setSelectedIndex(-1);
		pageLayout.setZoom(zoom);
		zoomTextBox.setText(zoom + "%");
		zoomTextBox.setFocus(false);
	}

	protected void zoomChangeClicked(int direction) {
		if (pageLayout == null)
			return;
		int index = Arrays.binarySearch(zoomOptions.toArray(), pageLayout.getZoom(), Collections.reverseOrder());
		if (index >= 0) {
			index -= direction;
		} else {
			index = -index - (direction == 1 ? 2 : 1); 
		}
		index = Math.min(index, zoomOptions.size() - 1);
		index = Math.max(index, 0);
		zoomPanel.selection.setSelectedIndex(index);
		zoomSelectionChanged();
	}

	protected void pageSelectionChanged() {
		int page = pagePanel.selection.getSelectedIndex();
		if (pageLayout != null)
			pageLayout.setPage(page);
		pagePanel.selection.setFocus(false);
	}

	protected void pageTypedIn() {
		if (pageLayout == null)
			return;
		TextBox pageTextBox = pagePanel.textBox;
		String digits = pageTextBox.getText().replaceAll("[^0-9]", "");
		if (digits.isEmpty() || digits.length() > 6) {
			pageTextBox.setText(pagePanel.selection.getSelectedItemText());
			pageTextBox.selectAll();
			return;
		}
		int page = Math.min(Integer.valueOf(digits), pagesCount) - 1;
		page = Math.max(page, 0);
		pagePanel.selection.setSelectedIndex(page);
		pageLayout.setPage(page);
		pageTextBox.setFocus(false);
	}

	protected void pageChangeClicked(int direction) {
		if (pageLayout == null)
			return;
		int page = pagePanel.selection.getSelectedIndex() + direction;
		page = Math.max(0, Math.min(pagesCount - 1, page));
		pagePanel.selection.setSelectedIndex(page);
		pageSelectionChanged();
	}

	private static abstract class SelectionPanel extends FlowPanel {
		public final ListBox selection;
		public final TextBox textBox;
		private final Button leftButton, rightButton;

		public SelectionPanel(String leftButtonStyle, String rightButtonStyle) {
			setStyleName("toolbarItem");

			leftButton = new Button();
			leftButton.setStyleName("toolbarSquareButton");
			leftButton.addStyleName(leftButtonStyle);
			leftButton.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					changeValueClicked(-1);
				}
			});
			add(leftButton);

			FlowPanel comboPanel = new FlowPanel();
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

			rightButton = new Button();
			rightButton.setStyleName("toolbarSquareButton");
			rightButton.addStyleName(rightButtonStyle);
			rightButton.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					changeValueClicked(1);
				}
			});
			add(rightButton);
		}

		public void updateButtons() {
			leftButton.setEnabled(isButtonEnabled(-1));
			rightButton.setEnabled(isButtonEnabled(1));
		}

		protected abstract void valueTypedIn();

		protected abstract void valueSelected();

		protected abstract void changeValueClicked(int direction);

		protected boolean isButtonEnabled(int direction) {
			int newIndex = selection.getSelectedIndex() + direction;
			return 0 <= newIndex && newIndex < selection.getItemCount();
		}
	}
}
