package org.pbrt.openexr.types;

import org.pbrt.openexr.header.Attribute;
import org.pbrt.openexr.util.DataReader;

/**
 * Copyright (c) Bartosz Zaczynski, 2010
 * http://syntaxcandy.blogspot.com
 */
public class Preview extends Attribute {
	
	public Preview value;
	
	public int width;
	public int height;
	
	public Preview(DataReader data) {
		width = data.readInt();
		height = data.readInt();
		data.skip(4 * width * height);
	}
	
	@Override
	public String toString() {
		return String.format("%s = [width=%d, height=%d]", name, width, height);
	}
}