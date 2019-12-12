package org.pbrt.openexr.compressor;

import java.util.zip.DataFormatException;

/**
 * Copyright (c) Bartosz Zaczynski, 2010
 * http://syntaxcandy.blogspot.com
 */
public class NullCompressor extends Compressor {

	@Override
	public byte[] uncompress(byte[] data) throws DataFormatException {
		return data;
	}
}