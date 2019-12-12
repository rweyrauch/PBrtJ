package org.pbrt.openexr.types;

import org.pbrt.openexr.header.Attribute;
import org.pbrt.openexr.util.DataReader;

/**
 * Copyright (c) Bartosz Zaczynski, 2010
 * http://syntaxcandy.blogspot.com
 */
public class Binary32 extends Attribute {
	
	public float value;
	
	public Binary32(DataReader data) {
		value = data.readFloat();
	}
	
	@Override
	public String toString() {
		return name + " = " + value;
	}
}