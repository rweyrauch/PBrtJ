package org.pbrt.openexr.types;

import org.pbrt.openexr.header.Attribute;
import org.pbrt.openexr.util.DataReader;

/**
 * Copyright (c) Bartosz Zaczynski, 2010
 * http://syntaxcandy.blogspot.com
 */
public class V3i extends Attribute {
	
	public int x;
	public int y;
	public int z;
	
	public V3i(DataReader data) {
		x = data.readInt();
		y = data.readInt();
		z = data.readInt();
	}
	
	@Override
	public String toString() {
		return String.format("%s = [%d, %d, %d]", name, x, y, z);
	}
}