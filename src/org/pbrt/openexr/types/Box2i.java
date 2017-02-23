package org.pbrt.openexr.types;

import org.pbrt.openexr.header.Attribute;
import org.pbrt.openexr.util.DataReader;

public class Box2i extends Attribute {
	
	public int xMin;
	public int yMin;
	public int xMax;
	public int yMax;
	
	public Box2i(DataReader data) {
		xMin = data.readInt();
		yMin = data.readInt();
		xMax = data.readInt();
		yMax = data.readInt();
	}
	
	@Override
	public String toString() {
		return String.format("%s = (%d, %d; %d, %d)", name, xMin, yMin, xMax, yMax);
	}
}