package pl.djvuhtml5.client;

import java.util.ArrayList;

import com.lizardtech.djvu.DjVuInfo;
import com.lizardtech.djvu.GMap;
import com.lizardtech.djvu.text.DjVuText;

import pl.djvuhtml5.client.TileRenderer.TileInfo;

public interface ProcessingContext {

	final String STATUS_LOADING = "loading";
	final String STATUS_ERROR = "error";

	void setStatus(String status);

	void startProcessing();

	void interruptProcessing();

	void setPageCount(int pageCount);

	void setPageInfo(int pageNum, DjVuInfo info);

	void setText(int pageNum, DjVuText text);

	void setTile(TileInfo tileInfo, GMap bufferGMap);

	void releaseTileImages(ArrayList<TileInfo> tilesToRemove);
}
