package org.pbrt.openexr.compressor;

import java.util.zip.DataFormatException;

import org.pbrt.openexr.exception.OpenExrException;
import org.pbrt.openexr.types.Compression;

/**
 * Copyright (c) Bartosz Zaczynski, 2010
 * http://syntaxcandy.blogspot.com
 */
public abstract class Compressor {
	
	public static Compressor newInstance(Compression compression, int bytesPerRow) throws OpenExrException {
		
		int blockSize = compression.linesPerBlock * bytesPerRow;
		
		switch (compression.method) {
			case Compression.NONE:
				return new NullCompressor();
			case Compression.ZIPS:
			case Compression.ZIP:
				return new ZipCompressor(blockSize);
		}
		
		throw new OpenExrException("Compression method not supported: " + compression.methodName);
	}
	
	public abstract byte[] uncompress(byte[] data) throws DataFormatException;
	
	protected Compressor() {
	}
}