package org.pbrt.openexr.compressor;

import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Copyright (c) Bartosz Zaczynski, 2010
 * http://syntaxcandy.blogspot.com
 */
public class ZipCompressor extends Compressor {
	
	private final byte[] buffer;
	
	public ZipCompressor(int blockSize) {
		buffer = new byte[blockSize];
	}
	
	@Override
	public byte[] uncompress(byte[] data) throws DataFormatException {
		
		Inflater inflater = new Inflater();
		inflater.setInput(data);
		
		int size = inflater.inflate(buffer);
		inflater.end();
		
		return postProcess(Arrays.copyOfRange(buffer, 0, size));
	}
	
	private byte[] postProcess(byte[] data) {
		
		int length = data.length;
		
		// predictor
		for (int i = 1; i < length; i++) {
			int value = data[i-1] + data[i] - 128;
			data[i] = (byte) value;
		}
		
		// reorder the pixel data
		byte[] result = new byte[length];
		int half = (length + 1) / 2;
		for (int i = 0; i < length; i++) {
			int j = (i < half) ? 2*i : 2*(i - half) + 1;
			result[j] = data[i];
		}
		
		return result;
	}
}