package org.pbrt.openexr.types;

import org.pbrt.openexr.header.Attribute;
import org.pbrt.openexr.util.DataReader;

/**
 * Copyright (c) Bartosz Zaczynski, 2010
 * http://syntaxcandy.blogspot.com
 */
public class V2i extends Attribute {
	
	public int x;
	public int y;
	
	public V2i(DataReader data) {
		x = data.readInt();
		y = data.readInt();
	}
	
	@Override
	public String toString() {
		return String.format("%s = [%d, %d]", name, x, y);
	}
}