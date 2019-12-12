package org.pbrt.openexr.types;

import org.pbrt.openexr.header.Attribute;
import org.pbrt.openexr.util.DataReader;

/**
 * Copyright (c) Bartosz Zaczynski, 2010
 * http://syntaxcandy.blogspot.com
 */
public class Int extends Attribute {
	
	public int value;
	
	public Int(DataReader data) {
		value = data.readInt();
	}
	
	@Override
	public String toString() {
		return name + " = " + value;
	}
}