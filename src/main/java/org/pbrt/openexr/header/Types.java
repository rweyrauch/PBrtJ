package org.pbrt.openexr.header;

/**
 * Copyright (c) Bartosz Zaczynski, 2010
 * http://syntaxcandy.blogspot.com
 */
public enum Types {
	
	UNKNOWN("unknown"),
	BOX2I("box2i"),
	BOX2F("box2f"),
	CHLIST("chlist"),
	CHROMATICITIES("chromaticities"),
	COMPRESSION("compression"),
	DOUBLE("double"),
	ENVMAP("envmap"),
	FLOAT("float"),
	INT("int"),
	KEYCODE("keycode"),
	LINEORDER("lineOrder"),
	PREVIEW("preview"),
	RATIONAL("rational"),
	STRING("string"),
	TIMECODE("timecode"),
	V2I("v2i"),
	V2F("v2f"),
	V3I("v3i"),
	V3F("v3f");
	
	public String name;
	
	Types(String name) {
		this.name = name;
	}
	
	public static Types fromString(String name) {
		for (Types element : values()) {
			if (element.name.equals(name)) {
				return element;
			}
		}
		return UNKNOWN;
	}
}