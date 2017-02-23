package org.pbrt.openexr.util;

/**
 * Copyright (c) Bartosz Zaczynski, 2010
 * http://syntaxcandy.blogspot.com
 * 
 * IEEE-754 half-precision floating point
 */
public class Half {
	
	private static Half instance = null;
	private static float[] lookupTable = new float[1 << 16];
	
	public static Half getInstance() {
		if (instance == null) {
			instance = new Half();
		}
		return instance;
	}
	
	public float toFloat(short bitPattern) {
		int index = bitPattern - Short.MIN_VALUE;
		return lookupTable[index];
	}
	
	private Half() {
		for (int i = 0; i < lookupTable.length; i++) {
			short bitPattern = (short) (Short.MIN_VALUE + i);
			lookupTable[i] = makeFloat(bitPattern);
		}
	}
	
	private float makeFloat(short bitPattern) {
		
		// sign, exponent, mantissa bits
		int s = (bitPattern >> 15) & 1;
		int e = (bitPattern >> 10) & 31;
		int m = bitPattern & 1023;
		
		if (e == 0) {
			if (m == 0) {
				return 0.0f;
			} else {
				return getDenormalized(s, m);
			}
		} else if (e == 31) {
			if (m == 0) {
				return (s == 1) ? Float.NEGATIVE_INFINITY :
								  Float.POSITIVE_INFINITY;
			} else {
				return Float.NaN;
			}
		} else {
			return getNormalized(s, e, m);
		}
	}
	
	private float getDenormalized(int s, int m) {
		float mantissa = getMantissa(m);
		float value = (float) (Math.pow(2.0, -14) * mantissa);
		return (s == 1) ? -value : value;
	}
	
	private float getNormalized(int s, int e, int m) {
		float mantissa = 1.0f + getMantissa(m);
		float value = (float) (Math.pow(2.0, e - 15) * mantissa);
		return (s == 1) ? -value : value;
	}
	
	public float getMantissa(int mantissaBits) {
		double mantissa = 0.0;
		for (int i = 9; i >= 0; i--) {
			int bitMask = 1 << i;
			if (((mantissaBits & bitMask) >> i) == 1) {
				mantissa += Math.pow(2.0, i - 10);
			}
		}
		return (float) mantissa;
	}
}