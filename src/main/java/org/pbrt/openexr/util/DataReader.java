package org.pbrt.openexr.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

/**
 * Copyright (c) Bartosz Zaczynski, 2010
 * http://syntaxcandy.blogspot.com
 */
public class DataReader {
	
	private MappedByteBuffer buffer;
	
	public DataReader(File file, ByteOrder endian) throws IOException {
		RandomAccessFile raf = new RandomAccessFile(file, "r");
		FileChannel fileChannel = raf.getChannel();
		buffer = fileChannel.map(MapMode.READ_ONLY, 0, file.length());
		buffer.order(endian);
	}
	
	public int offset() {
		return buffer.position();
	}
	
	public void seek(int offset) {
		buffer.position(offset);
	}
	
	public void skip(int numBytes) {
		seek(offset() + numBytes);
	}
	
	public byte peekByte() {
		byte value = readByte();
		seek(offset() - 1);
		return value;
	}
	
	public byte readByte() {
		return buffer.get();
	}
	
	public byte[] readBytes(int length) {
		byte[] bytes = new byte[length];
		buffer.get(bytes);
		return bytes;
	}
	
	public short readShort() {
		return buffer.getShort();
	}
	
	public int readInt() {
		return buffer.getInt();
	}
	
	public float readFloat() {
		return buffer.getFloat();
	}
	
	public double readDouble() {
		return buffer.getDouble();
	}
	
	public String readString() {
		
		StringBuilder sb = new StringBuilder();
		
		for (;;) {
			byte b = readByte();
			if (b == 0) {
				break;
			} else {
				sb.append((char) b);
			}
		}
		
		return sb.toString();
	}
}