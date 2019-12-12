package org.pbrt.openexr.types;

import org.pbrt.openexr.header.Attribute;
import org.pbrt.openexr.util.DataReader;

/**
 * Copyright (c) Bartosz Zaczynski, 2010
 * http://syntaxcandy.blogspot.com
 */
public class Compression extends Attribute {
	
	public static final byte NONE = 0;
	public static final byte RLE = 1;
	public static final byte ZIPS = 2;
	public static final byte ZIP = 3;
	public static final byte PIZ = 4;
	public static final byte PXR24 = 5;
	public static final byte B44 = 6;
	public static final byte B44A = 7; 
	
	public byte method;
	public int linesPerBlock;
	public String methodName;
	
	public Compression(DataReader data) {
		method = data.readByte();
		switch (method) {
			case NONE:
				methodName = "none";
				linesPerBlock = 1;
				break;
			case RLE:
				methodName = "RLE";
				linesPerBlock = 1;
				break;
			case ZIPS:
				methodName = "ZIPS";
				linesPerBlock = 1;
				break;
			case ZIP:
				methodName = "ZIP";
				linesPerBlock = 16;
				break;
			case PIZ:
				methodName = "PIZ";
				linesPerBlock = 32;
				break;
			case PXR24:
				methodName = "PXR24";
				linesPerBlock = 16;
				break;
			case B44:
				methodName = "B44";
				linesPerBlock = 32;
				break;
			case B44A:
				methodName = "B44A";
				linesPerBlock = 32;
				break;
		}
	}
	
	@Override
	public String toString() {
		return name + " = " + methodName;
	}
}