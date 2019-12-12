package org.pbrt.openexr.types;

import org.pbrt.openexr.header.Attribute;
import org.pbrt.openexr.util.DataReader;

/**
 * Copyright (c) Bartosz Zaczynski, 2010
 * http://syntaxcandy.blogspot.com
 */
public class TimeCode extends Attribute {
	
	public int timeAndFlags;
	public int userData;
	
	public TimeCode(DataReader data) {
		timeAndFlags = data.readInt();
		userData = data.readInt();
	}
	
	@Override
	public String toString() {
		String fmt = "%s = [timeAndFlags=%d, userData=%d]";
		return String.format(fmt, name, timeAndFlags, userData);
	}
}