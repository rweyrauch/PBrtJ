package org.pbrt.openexr.types;

import org.pbrt.openexr.header.Attribute;
import org.pbrt.openexr.util.DataReader;

/**
 * Copyright (c) Bartosz Zaczynski, 2010
 * http://syntaxcandy.blogspot.com
 */
public class ShortString extends Attribute {
	
	public String value;
	
	public ShortString(DataReader data, int size) {
		byte[] chars = data.readBytes(size);
		value = new String(chars);
	}
	
	@Override
	public String toString() {
		return String.format("%s = \"%s\"", name, value);
	}
}