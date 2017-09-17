package pl.djvuhtml5.client;

import static pl.djvuhtml5.client.Djvu_html5.log;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.JavaScriptObject;
import com.lizardtech.djvu.GRect;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public class BackgroundWorker implements EntryPoint {

	public static class WorkerMessage {
		@JsProperty
		public final String order;
		@JsProperty
		public Object data;

		public WorkerMessage(String order, Object data) {
			this.order = order;
			this.data = data;
		}
	}

	@JsType
	public static class ViewState {
		public final String url;
		public final int page;
		public final GRect tileRange;
		public final int subsample;

		public ViewState() {
			url = DjvuContext.getUrl();
			page = DjvuContext.getPage();
			tileRange = new GRect();
			DjvuContext.getTileRange(tileRange);
			subsample = DjvuContext.getSubsample();
		}

		public void push() {
			DjvuContext.setUrl(url);
			DjvuContext.setPage(page);
			DjvuContext.setTileRange(tileRange, subsample);
		}

		public static native ViewState cast(Object data) /*-{
			return data;
		}-*/;
	}

	/** Sends messages to worker slave and handles returned messages. */
	static class Master {

		private final JavaScriptObject workerInstance;

		public Master() {
			workerInstance = createWorker();
			postMessage(new WorkerMessage("context-init", DjvuContext.exportConfig()));
			DjvuContext.addViewChangeListener(() -> postMessage(new WorkerMessage("view-change", new ViewState())));
		}
		
		private native JavaScriptObject createWorker() /*-{
			var that = this;
			var djvuWorker = new Worker('djvu_worker/djvu_worker.nocache.js');
			djvuWorker.addEventListener('message', function(e) {
				console.log('Worker said: ', e.data);
				that.@pl.djvuhtml5.client.BackgroundWorker$Master::onMessage(Lpl/djvuhtml5/client/BackgroundWorker$WorkerMessage;)(e.data);
			}, false);
			return djvuWorker;
		}-*/;

		void onMessage(WorkerMessage message) {
			log("message from worker: " + message.order);
		}

		public void postMessage(WorkerMessage message) {
			postMessage(workerInstance, message);
		}

		private native void postMessage(JavaScriptObject djvuWorker, WorkerMessage message) /*-{
			djvuWorker.postMessage(message);
		}-*/;
	}

	/** The web worker side */
	static class Slave {
		public Slave() {
			initialize();
			DjvuContext.addViewChangeListener(() -> log("view changed in slave " + DjvuContext.getUrl()));
		}
		
		native void initialize() /*-{
			var that = this;
			addEventListener('message', function(e) {
				console.log("Received message in web worker: " + e.data.order);
				that.@pl.djvuhtml5.client.BackgroundWorker$Slave::onMessage(Lpl/djvuhtml5/client/BackgroundWorker$WorkerMessage;)(e.data);
			}, false);
		}-*/;

		void onMessage(WorkerMessage message) {
			switch (message.order) {
			case "context-init":
				DjvuContext.importConfig(message.data);
				break;
			case "view-change":
				ViewState.cast(message.data).push();
				break;
			}
			WorkerMessage response = new WorkerMessage("hello", null);
			postMessage(response);
		}

		public native void postMessage(WorkerMessage message) /*-{
			postMessage(message);
		}-*/;
	}

	public static void test() {
		Master wm = new Master();
		WorkerMessage message = new WorkerMessage("what?", null);
		wm.postMessage(message);
	}

	@Override
	public void onModuleLoad() {
		new Slave();
	}
}
