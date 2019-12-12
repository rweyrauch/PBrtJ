package org.pbrt.openexr.types;

import java.util.Formatter;
import java.util.Locale;

import org.pbrt.openexr.header.Attribute;
import org.pbrt.openexr.util.DataReader;

public class Box2f extends Attribute {
	
	public float xMin;
	public float yMin;
	public float xMax;
	public float yMax;
	
	public Box2f(DataReader data) {
		xMin = data.readFloat();
		yMin = data.readFloat();
		xMax = data.readFloat();
		yMax = data.readFloat();
	}
	
	@Override
	public String toString() {
		Formatter formatter = new Formatter(Locale.US);
		formatter.format("%s = (%f, %f; %f; %f)", name, xMin, yMin, xMax, yMax);
		return formatter.toString();
	}
}