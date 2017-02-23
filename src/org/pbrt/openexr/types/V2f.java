package org.pbrt.openexr.types;

import java.util.Formatter;
import java.util.Locale;

import org.pbrt.openexr.header.Attribute;
import org.pbrt.openexr.util.DataReader;

/**
 * Copyright (c) Bartosz Zaczynski, 2010
 * http://syntaxcandy.blogspot.com
 */
public class V2f extends Attribute {
	
	public float x;
	public float y;
	
	public V2f(DataReader data) {
		x = data.readFloat();
		y = data.readFloat();
	}
	
	@Override
	public String toString() {
		Formatter formatter = new Formatter(Locale.US);
		formatter.format("%s = [%f, %f]", name, x, y);
		return formatter.toString();
	}
}