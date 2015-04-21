package pl.djvuhtml5.client;

import java.io.IOException;

import pl.djvuhtml5.shared.FieldVerifier;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.CanvasPixelArray;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.ImageData;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.CanvasElement;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.lizardtech.djvu.CachedInputStream;
import com.lizardtech.djvu.DjVmDir;
import com.lizardtech.djvu.DjVuInfo;
import com.lizardtech.djvu.DjVuPage;
import com.lizardtech.djvu.Document;
import com.lizardtech.djvu.GMap;
import com.lizardtech.djvu.GRect;
import com.lizardtech.djvu.InputStateListener;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Djvu_html5 implements EntryPoint {

	/**
	 * The message displayed to the user when the server cannot be reached or
	 * returns an error.
	 */
	private static final String SERVER_ERROR = "An error occurred while "
			+ "attempting to contact the server. Please check your network " + "connection and try again.";
	private String url;
	private Document document;
	private Label errorLabel;
	private Context2d drawingContext;

	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad() {
		final Button sendButton = new Button("Send");
		final TextBox nameField = new TextBox();
		nameField.setText("GWT User");
		errorLabel = new Label();

		// We can add style names to widgets
		sendButton.addStyleName("sendButton");

		// Add the nameField and sendButton to the RootPanel
		// Use RootPanel.get() to get the entire body element
		RootPanel.get("nameFieldContainer").add(nameField);
		RootPanel.get("sendButtonContainer").add(sendButton);
		RootPanel.get("errorLabelContainer").add(errorLabel);

		// Focus the cursor on the name field when the app loads
		nameField.setFocus(true);
		nameField.selectAll();

		// Create the popup dialog box
		final DialogBox dialogBox = new DialogBox();
		dialogBox.setText("Remote Procedure Call");
		dialogBox.setAnimationEnabled(true);
		final Button closeButton = new Button("Close");
		// We can set the id of a widget by accessing its Element
		closeButton.getElement().setId("closeButton");
		final Label textToServerLabel = new Label();
		final HTML serverResponseLabel = new HTML();
		VerticalPanel dialogVPanel = new VerticalPanel();
		dialogVPanel.addStyleName("dialogVPanel");
		dialogVPanel.add(new HTML("<b>Sending name to the server:</b>"));
		dialogVPanel.add(textToServerLabel);
		dialogVPanel.add(new HTML("<br><b>Server replies:</b>"));
		dialogVPanel.add(serverResponseLabel);
		dialogVPanel.setHorizontalAlignment(VerticalPanel.ALIGN_RIGHT);
		dialogVPanel.add(closeButton);
		dialogBox.setWidget(dialogVPanel);

		// Add a handler to close the DialogBox
		closeButton.addClickHandler(new ClickHandler() {

			public void onClick(ClickEvent event) {
				dialogBox.hide();
				sendButton.setEnabled(true);
				sendButton.setFocus(true);
			}
		});

		// Create a handler for the sendButton and nameField
		class MyHandler implements ClickHandler, KeyUpHandler {

			/**
			 * Fired when the user clicks on the sendButton.
			 */
			public void onClick(ClickEvent event) {
				sendNameToServer();
			}

			/**
			 * Fired when the user types in the nameField.
			 */
			public void onKeyUp(KeyUpEvent event) {
				if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
					sendNameToServer();
				}
			}

			/**
			 * Send the name from the nameField to the server and wait for a
			 * response.
			 */
			private void sendNameToServer() {
				// First, we validate the input.
				errorLabel.setText("");
				String textToServer = nameField.getText();
				if (!FieldVerifier.isValidName(textToServer)) {
					errorLabel.setText("Please enter at least four characters");
					return;
				}

				// Then, we send the input to the server.
				sendButton.setEnabled(false);
				textToServerLabel.setText(textToServer);
				serverResponseLabel.setText("");
				System.out.println("not sending " + textToServer);
			}
		}

		// Add a handler to send the name to the server
		MyHandler handler = new MyHandler();
		sendButton.addClickHandler(handler);
		nameField.addKeyUpHandler(handler);

		Canvas canvas = Canvas.createIfSupported();
		if (canvas == null) {
			// TODO
			throw new RuntimeException("Canvas not supported!");
		}
		canvas.setSize("1000px", "1000px");
		canvas.setCoordinateSpaceWidth(1000);
		canvas.setCoordinateSpaceHeight(1000);
		RootPanel.get("djvuContainer").add(canvas);

		drawingContext = canvas.getContext2d();
		drawingContext.fillRect(10, 10, 200, 200);

		url = "http://127.0.0.1:8888/sample/index.djvu";
		new CachedInputStream().init(url, new InputStateListener() {

			@Override
			public void inputReady() {
				parseDocument();
			}
		});
	}

	private void parseDocument() {
		try {
			document = new Document();
			document.read(url);
			DjVmDir djVmDir = document.getDjVmDir();
			int filesCount = djVmDir.get_files_num();
			filesCount = 1;
			System.out.println("document read, found " + filesCount + " files.");
			final int[] countDown = { filesCount };
			InputStateListener listener = new InputStateListener() {

				@Override
				public void inputReady() {
					countDown[0]--;
					if (countDown[0] <= 0) {
						parsePages();
					}
				}
			};
			for (int i = 0; i < filesCount; i++) {
				document.get_data(i, listener);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void parsePages() {
		System.out.println("all pages downloaded!");
//		try {
//			for (int i = 0; i < document.size(); i++) {
//				DjVuPage page = document.getPage(i);
//				DjVuInfo info = page.getInfo();
//				System.out.println("page " + i + ": " + info.width + " x " + info.height);
//			}
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		try {
			DjVuPage page = document.getPage(0);
			int w = 1000, h = 1000;
			GRect segment = new GRect(0, page.getInfo().height - h, w, h);
			long start = System.currentTimeMillis();
			GMap map = page.getMap(segment, 1, null);
			byte[] data = map.getData();
			System.out.println("bitmap generation: " + (System.currentTimeMillis() - start));
			start = System.currentTimeMillis();
			ImageData imageData = drawingContext.createImageData(1000, 1000);
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					int offset = 3 * ((h - y - 1) * w + x);
					imageData.setRedAt(data[offset + map.getRedOffset()] & 0xFF, x, y);
					imageData.setGreenAt(data[offset + map.getGreenOffset()] & 0xFF, x, y);
					imageData.setBlueAt(data[offset + map.getBlueOffset()] & 0xFF, x, y);
					imageData.setAlphaAt(255, x, y);
				}
			}
			System.out.println("bitmap copying: " + (System.currentTimeMillis() - start));
			drawingContext.putImageData(imageData, 0, 0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
