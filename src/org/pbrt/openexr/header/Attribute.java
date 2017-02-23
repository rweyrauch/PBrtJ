package org.pbrt.openexr.header;

import org.pbrt.openexr.types.Binary32;
import org.pbrt.openexr.types.Binary64;
import org.pbrt.openexr.types.Box2f;
import org.pbrt.openexr.types.Box2i;
import org.pbrt.openexr.types.Channels;
import org.pbrt.openexr.types.Chromaticities;
import org.pbrt.openexr.types.Compression;
import org.pbrt.openexr.types.EnvMap;
import org.pbrt.openexr.types.Int;
import org.pbrt.openexr.types.KeyCode;
import org.pbrt.openexr.types.LineOrder;
import org.pbrt.openexr.types.ShortString;
import org.pbrt.openexr.types.Preview;
import org.pbrt.openexr.types.Rational;
import org.pbrt.openexr.types.TimeCode;
import org.pbrt.openexr.types.V2f;
import org.pbrt.openexr.types.V2i;
import org.pbrt.openexr.types.V3f;
import org.pbrt.openexr.types.V3i;
import org.pbrt.openexr.util.DataReader;

/**
 * Copyright (c) Bartosz Zaczynski, 2010
 * http://syntaxcandy.blogspot.com
 */
public class Attribute {
	
	protected String name;
	protected String type;
	protected int size;
	
	public static Attribute newInstance(DataReader data,
										String name,
										String type,
										int size) {
		
		Attribute attribute = null;
		
		switch (Types.fromString(type)) {
			
			case STRING: attribute = new ShortString(data, size); break;
			
			case INT: attribute = new Int(data); break;
			case FLOAT: attribute = new Binary32(data); break;
			case DOUBLE: attribute = new Binary64(data); break;
			case RATIONAL: attribute = new Rational(data); break;
			
			case BOX2I: attribute = new Box2i(data); break;
			case BOX2F: attribute = new Box2f(data); break;
			
			case V2I: attribute = new V2i(data); break;
			case V2F: attribute = new V2f(data); break;
			
			case V3I: attribute = new V3i(data); break;
			case V3F: attribute = new V3f(data); break;
			
			case CHLIST: attribute = new Channels(data); break;
			case CHROMATICITIES: attribute = new Chromaticities(data); break;
			case LINEORDER: attribute = new LineOrder(data); break;
			case COMPRESSION: attribute = new Compression(data); break;
			case ENVMAP: attribute = new EnvMap(data); break;
			case KEYCODE: attribute = new KeyCode(data); break;
			case TIMECODE: attribute = new TimeCode(data); break;
			case PREVIEW: attribute = new Preview(data); break;
			
			default:
				attribute = new Attribute();
				data.skip(size);
		}
		
		if (attribute != null) {
			attribute.name = name;
			attribute.type = type;
			attribute.size = size;
		}
		
		return attribute;
	}
	
	@Override
	public String toString() {
		String numeral = (size > 1) ? "bytes" : "byte";
		return String.format("%s : %s (%d %s)", name, type, size, numeral);
	}
	
	protected Attribute() {
	}
}