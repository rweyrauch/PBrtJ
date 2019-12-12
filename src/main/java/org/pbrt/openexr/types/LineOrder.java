package org.pbrt.openexr.types;

import org.pbrt.openexr.header.Attribute;
import org.pbrt.openexr.util.DataReader;

/**
 * Copyright (c) Bartosz Zaczynski, 2010
 * http://syntaxcandy.blogspot.com
 */
public class LineOrder extends Attribute {
	
	public static final byte INCREASING_Y = 0;
	public static final byte DECREASING_Y = 1;
	public static final byte RANDOM_Y = 2;
	
	public byte value;
	
	public LineOrder(DataReader data) {
		value = data.readByte();
	}
	
	@Override
	public String toString() {
		return name + " = " + getType();
	}
	
	private String getType() {
		switch (value) {
			case INCREASING_Y: return "INCREASING_Y";
			case DECREASING_Y: return "DECREASING_Y";
			case RANDOM_Y: return "RANDOM_Y";
		}
		return "unknown";
	}
}