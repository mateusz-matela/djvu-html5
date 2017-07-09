package pl.djvuhtml5.client;

import static pl.djvuhtml5.client.Djvu_html5.log;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.JavaScriptObject;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public class BackgroundWorker implements EntryPoint {

	@JsType
	public static class WorkerMessage {
		@JsProperty public int id;
	}

	/** Sends messages to worker slave and handles returned messages. */
	static class Master {

		private final JavaScriptObject workerInstance;

		public Master() {
			workerInstance = createWorker();
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
			log("message from worker: " + message.id);
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
		}
		
		native void initialize() /*-{
			var that = this;
			addEventListener('message', function(e) {
				console.log("Received message in web worker");
				that.@pl.djvuhtml5.client.BackgroundWorker$Slave::onMessage(Lpl/djvuhtml5/client/BackgroundWorker$WorkerMessage;)(e.data);
			}, false);
		}-*/;

		void onMessage(WorkerMessage message) {
			log("message to worker: " + message.id);
			WorkerMessage response = new WorkerMessage();
			response.id = 33;
			postMessage(response);
		}

		public native void postMessage(WorkerMessage message) /*-{
			postMessage(message);
		}-*/;
	}

	public static void test() {
		Master wm = new Master();
		WorkerMessage message = new WorkerMessage();
		message.id = 22;
		wm.postMessage(message);
	}

	@Override
	public void onModuleLoad() {
		new Slave();
	}
}
