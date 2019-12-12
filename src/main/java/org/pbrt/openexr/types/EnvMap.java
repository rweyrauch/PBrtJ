package org.pbrt.openexr.types;

import org.pbrt.openexr.header.Attribute;
import org.pbrt.openexr.util.DataReader;

/**
 * Copyright (c) Bartosz Zaczynski, 2010
 * http://syntaxcandy.blogspot.com
 */
public class EnvMap extends Attribute {
	
	public String value;
	
	public EnvMap(DataReader data) {
		switch(data.readByte()) {
			case 0: value = "ENVMAP_LATLONG"; break;
			case 1: value = "ENVMAP_CUBE"; break;
			default: value = "?";
		}
	}
	
	@Override
	public String toString() {
		return name + " = " + value;
	}
}