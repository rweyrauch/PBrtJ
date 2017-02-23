package org.pbrt.openexr.types;

import org.pbrt.openexr.header.Attribute;
import org.pbrt.openexr.util.DataReader;

/**
 * Copyright (c) Bartosz Zaczynski, 2010
 * http://syntaxcandy.blogspot.com
 */
public class KeyCode extends Attribute {
	
	public int filmMfcCode;
	public int filmType;
	public int prefix;
	public int count;
	public int perfOffset;
	public int perfsPerFrame;
	public int perfsPerCount;
	
	public KeyCode(DataReader data) {
		filmMfcCode = data.readInt();
		filmType = data.readInt();
		prefix = data.readInt();
		count = data.readInt();
		perfOffset = data.readInt();
		perfsPerFrame = data.readInt();
		perfsPerCount = data.readInt();
	}
	
	@Override
	public String toString() {
		
		StringBuilder sb = new StringBuilder();
		sb.append("%s = ");
		sb.append("(filmMfcCode=%d, ");
		sb.append("filmType=%d, ");
		sb.append("prefix=%d, ");
		sb.append("count=%d, ");
		sb.append("perfOffset=%d, ");
		sb.append("perfsPerFrame=%d, ");
		sb.append("perfsPerCount=%d)");
		
		return String.format(
			sb.toString(),
			name,
			filmMfcCode,
			filmType,
			prefix,
			count,
			perfOffset,
			perfsPerFrame,
			perfsPerCount
		);
	}
}