package pl.djvuhtml5.client;

import java.util.ArrayList;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.typedarrays.shared.Uint8Array;
import com.lizardtech.djvu.DjVuInfo;
import com.lizardtech.djvu.GMap;
import com.lizardtech.djvu.GRect;
import com.lizardtech.djvu.text.DjVuText;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import pl.djvuhtml5.client.TileRenderer.TileInfo;

public class BackgroundWorker implements EntryPoint {

	private static class WorkerMessage {
		@JsProperty
		public final String order;
		@JsProperty
		public Object data;
		@JsProperty
		public Object data2;

		public WorkerMessage(String order, Object data) {
			this.order = order;
			this.data = data;
		}

		public WorkerMessage(String order, Object data, Object data2) {
			this(order, data);
			this.data2 = data2;
		}
	}

	@JsType
	private static class ViewState {
		public final int page;
		public final GRect tileRange;
		public final int subsample;

		public ViewState() {
			page = DjvuContext.getPage();
			tileRange = new GRect();
			DjvuContext.getTileRange(tileRange);
			subsample = DjvuContext.getSubsample();
		}

		public void push() {
			DjvuContext.setPage(page);
			DjvuContext.setTileRange(tileRange, subsample);
		}

		public static native ViewState cast(Object data) /*-{
			return data;
		}-*/;
	}

	/** Sends messages to worker slave and handles returned messages. */
	private static class Master {

		final ProcessingContext context;

		public Master(ProcessingContext context) {
			this.context = context;
			initWorker();
			postMessage(new WorkerMessage("context-init", DjvuContext.exportConfig(), DjvuContext.getUrl()));
			DjvuContext.addViewChangeListener(() -> postMessage(new WorkerMessage("view-change", new ViewState())));
		}

		native void initWorker() /*-{
			var that = this;
			this.djvuWorker = new Worker('djvu_worker/djvu_worker.nocache.js');
			this.djvuWorker.addEventListener('message', function(e) {
				that.@pl.djvuhtml5.client.BackgroundWorker$Master::onMessage(Lpl/djvuhtml5/client/BackgroundWorker$WorkerMessage;)(e.data);
			}, false);
		}-*/;

		void onMessage(WorkerMessage message) {
			switch (message.order) {
			case "status-set":
				context.setStatus((String) message.data);
				break;
			case "page-count":
				context.setPageCount(Integer.valueOf((String) message.data));
				break;
			case "page-info":
				context.setPageInfo(Integer.valueOf((String) message.data), new DjVuInfo(toDjVuInfo(message.data2)));
				break;
			case "page-text":
				context.setText(Integer.valueOf((String) message.data), new DjVuText(toDjVuText(message.data2)));
				break;
			case "tile-data":
				context.setTile(new TileInfo(toTileInfo(message.data)), new GMap(toGMap(message.data2)));
				break;
			}
		}

		native void postMessage(WorkerMessage message) /*-{
			this.djvuWorker.postMessage(message);
		}-*/;

		native DjVuInfo toDjVuInfo(Object o) /*-{
			return o;
		}-*/;

		native DjVuText toDjVuText(Object o) /*-{
			return o;
		}-*/;

		native TileInfo toTileInfo(Object o) /*-{
			return o;
		}-*/;

		native GMap toGMap(Object o) /*-{
			if (!o.border) o.border = 0;
			return o;
		}-*/;
	}

	/** The web worker side */
	private static class Slave implements ProcessingContext {
		BackgroundProcessor backgroundProcessor;
		String status = null;

		public Slave() {
			initialize();
		}
		
		native void initialize() /*-{
			var that = this;
			addEventListener('message', function(e) {
				that.@pl.djvuhtml5.client.BackgroundWorker$Slave::onMessage(Lpl/djvuhtml5/client/BackgroundWorker$WorkerMessage;)(e.data);
			}, false);
		}-*/;

		void onMessage(WorkerMessage message) {
			switch (message.order) {
			case "context-init":
				DjvuContext.importConfig(message.data);
				DjvuContext.setUrl((String) message.data2);
				backgroundProcessor = new BackgroundProcessor(this);
				break;
			case "view-change":
				ViewState.cast(message.data).push();
				break;
			}
		}

		native void postMessage(WorkerMessage message) /*-{
			postMessage(message);
		}-*/;

		native void postMessage(WorkerMessage message, Object transfer) /*-{
			postMessage(message, [transfer]);
		}-*/;

		@Override
		public void setStatus(String status) {
			if (status == this.status || (status != null && status.equals(this.status)))
				return;
			this.status = status;
			postMessage(new WorkerMessage("status-set", status));
		}

		@Override
		public void startProcessing() {
			backgroundProcessor.start();
		}

		@Override
		public void interruptProcessing() {
			backgroundProcessor.interrupt();
		}

		@Override
		public void setPageCount(int pageCount) {
			postMessage(new WorkerMessage("page-count", Integer.toString(pageCount)));
		}

		@Override
		public void setPageInfo(int pageNum, DjVuInfo info) {
			postMessage(new WorkerMessage("page-info", Integer.toString(pageNum), info));
		}

		@Override
		public void setText(int pageNum, DjVuText text) {
			postMessage(new WorkerMessage("page-text", Integer.toString(pageNum), text), text.getTransferable());
		}

		@Override
		public void setTile(TileInfo tileInfo, GMap bufferGMap) {
			postMessage(new WorkerMessage("tile-data", tileInfo, bufferGMap), bufferGMap.getTransferable());
		}

		@Override
		public void releaseTileImages(ArrayList<TileInfo> tilesToRemove) {
			// TODO Auto-generated method stub
			
		}
	}

	public static void init(ProcessingContext context) {
		new Master(context);
	}

	@Override
	public void onModuleLoad() {
		new Slave();
	}
}
