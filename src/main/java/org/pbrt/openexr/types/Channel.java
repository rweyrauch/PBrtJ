package org.pbrt.openexr.types;

import org.pbrt.openexr.util.DataReader;

/**
 * Copyright (c) Bartosz Zaczynski, 2010
 * http://syntaxcandy.blogspot.com
 */
public class Channel {
	
	public static final int UINT = 0;
	public static final int HALF = 1;
	public static final int FLOAT = 2;
	
	public String name;
	
	public int pixelType;
	public byte pLinear;
	
	public int xSampling;
	public int ySampling;
	
	public Channel(DataReader data) {
		name = data.readString();
		pixelType = data.readInt();
		pLinear = data.readByte();
		data.skip(3);
		xSampling = data.readInt();
		ySampling = data.readInt();
	}
	
	public int getNumBytes() {
		switch (pixelType) {
			case UINT: return 4;
			case HALF: return 2;
			case FLOAT: return 4;
			default: return 0;
		}
	}
	
	@Override
	public String toString() {
		String fmt = "%s(type=%s, pLinear=%d, xSampling=%d, ySampling=%d)";
		return String.format(fmt, name, getPixelType(), pLinear, xSampling, ySampling);
	}
	
	private String getPixelType() {
		switch (pixelType) {
			case UINT: return "uint";
			case HALF: return "half";
			case FLOAT: return "float";
			default: return "unknown";
		}
	}
}