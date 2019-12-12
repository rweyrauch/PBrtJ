package org.pbrt.openexr.types;

import org.pbrt.openexr.header.Attribute;
import org.pbrt.openexr.util.DataReader;

/**
 * Copyright (c) Bartosz Zaczynski, 2010
 * http://syntaxcandy.blogspot.com
 */
public class Binary64 extends Attribute {
	
	public double value;
	
	public Binary64(DataReader data) {
		value = data.readDouble();
	}
	
	@Override
	public String toString() {
		return name + " = " + value;
	}
}