package com.lizardtech.djvu;

import java.util.AbstractList;
import java.util.List;

public class JsArrayList<T> extends AbstractList<T> {

	public JsArrayList() {
		init(null);
	}

	public JsArrayList(JsArrayList<T> toCopy) {
		init(toCopy);
	}

	public JsArrayList(List<T> toCopy) {
		init(null);
		for (T t : toCopy)
			add(t);
	}

	private native void init(JsArrayList<T> toCopy) /*-{
		this.array = toCopy ? toCopy.array : [];
	}-*/;

	@Override
	public native T get(int index) /*-{
		return this.array[index];
	}-*/;

	@Override
	public native int size() /*-{
		return this.array.length;
	}-*/;

	@Override
	public native boolean add(T e) /*-{
		this.array.push(e);
	}-*/;

	@Override
	public native T set(int index, T element) /*-{
		this.array[index] = element;
	}-*/;

	@Override
	public native void clear() /*-{
		this.array = [];
	}-*/;

	@Override
	public native T remove(int index) /*-{
		this.array.splice(index, 1);
	}-*/;
}
