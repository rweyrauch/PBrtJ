package org.pbrt.openexr.types;

import java.util.Formatter;
import java.util.Locale;

import org.pbrt.openexr.header.Attribute;
import org.pbrt.openexr.util.DataReader;

/**
 * Copyright (c) Bartosz Zaczynski, 2010
 * http://syntaxcandy.blogspot.com
 */
public class Chromaticities extends Attribute {
	
	public float redX;
	public float redY;
	
	public float greenX;
	public float greenY;
	
	public float blueX;
	public float blueY;
	
	public float whiteX;
	public float whiteY;
	
	public Chromaticities(DataReader data) {
		redX = data.readFloat();
		redY = data.readFloat();
		greenX = data.readFloat();
		greenY = data.readFloat();
		blueX = data.readFloat();
		blueY = data.readFloat();
		whiteX = data.readFloat();
		whiteY = data.readFloat();
	}
	
	@Override
	public String toString() {
		Formatter formatter = new Formatter(Locale.US);
		String fmt = "%s = [red(%f, %f), green(%f, %f), blue(%f, %f), white(%f, %f)]";
		formatter.format(fmt, name, redX, redY, greenX, greenY, blueX, blueY, whiteX, whiteY);
		return formatter.toString();
	}
}