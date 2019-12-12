package org.pbrt.openexr.types;

import org.pbrt.openexr.header.Attribute;
import org.pbrt.openexr.util.DataReader;

/**
 * Copyright (c) Bartosz Zaczynski, 2010
 * http://syntaxcandy.blogspot.com
 */
public class Rational extends Attribute {
	
	public int top;
	public int bottom;
	
	public Rational(DataReader data) {
		top = data.readInt();
		bottom = data.readInt();
	}
	
	@Override
	public String toString() {
		return String.format("%s = %d/%d", name, top, bottom);
	}
}