package org.pbrt.openexr;

import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.List;
import java.util.zip.DataFormatException;

import org.pbrt.openexr.compressor.Compressor;
import org.pbrt.openexr.exception.OpenExrException;
import org.pbrt.openexr.header.Attribute;
import org.pbrt.openexr.header.Header;
import org.pbrt.openexr.types.Box2i;
import org.pbrt.openexr.types.Channel;
import org.pbrt.openexr.types.Channels;
import org.pbrt.openexr.types.Compression;
import org.pbrt.openexr.util.DataReader;
import org.pbrt.openexr.util.Half;

/**
 * Copyright (c) Bartosz Zaczynski, 2010
 * http://syntaxcandy.blogspot.com
 */
public class OpenExr {
	
	private static final int MAGIC_NUMBER = 0x01312F76;
	private static final int VERSION = 2;
	
	private int width;
	private int height;
	
	private float[] pixels;
	private Header header;
	
	public OpenExr(File file) throws OpenExrException {
		load(file);
	}
	
	public Header getHeader() {
		return header;
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
	
	public float[] getPixels() {
		return pixels.clone();
	}
	
	public byte[] getPixels(double ev, double gamma) {
		
		final double evCorrection = pow(2.0, ev);
		final double gammaCorrection = 1.0 / gamma;
		
		final int length = width * height * 3;
		final byte[] rgb = new byte[length];
		
		Thread[] threads = new Thread[3];
		for (int j = 0; j < 3; j++) {
			final int offset = j;
			threads[j] = new Thread(() -> {
                for (int i = offset; i < length; i += 3) {
                    float value = (float) ((pow(pixels[i] * evCorrection, gammaCorrection)) * 255.0);
                    if (value < 0.0f) value = 0.0f;
                    if (value > 255.0f) value = 255.0f;
                    rgb[i] = (byte) value;
                }
            });
			threads[j].start();
		}
		
		for (int j = 0; j < 3; j++) {
			try {
				threads[j].join();
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
		}
		
		return rgb;
	}
	
	private void load(File file) throws OpenExrException {
		try {
			DataReader data = new DataReader(file, ByteOrder.LITTLE_ENDIAN);
			validateFile(data);
			header = new Header(data);
			readBody(data);
		} catch (Exception ex) {
			throw new OpenExrException(ex);
		}
	}
	
	private void validateFile(DataReader data) throws IOException, OpenExrException {
		
		if (MAGIC_NUMBER != data.readInt()) {
			throw new OpenExrException("Not a valid OpenEXR file format");
		}
		
		byte version = data.readByte();
		if (version != VERSION) {
			throw new OpenExrException("Not supported file version: " + version);
		}
		
		byte flags = data.readByte();
		if ((flags & 0x2) != 0) {
			throw new OpenExrException("Tiles not supported");
		}
		
		data.skip(2);
	}
	
	private void readBody(DataReader data) throws DataFormatException, OpenExrException {
		
		Box2i dataWindow = (Box2i) getAttribute("dataWindow");
		setDimensions(dataWindow);
		
		Channels channels = (Channels) getAttribute("channels");
		verify(channels);
		
		int bytesPerRow = getBytesPerRow(channels.list);
		
		Compression compression = (Compression) getAttribute("compression");
		Compressor compressor = Compressor.newInstance(compression, bytesPerRow);
		
		pixels = new float[width * height * 3];
		
		int numBlocks = (int) Math.ceil(height / compression.linesPerBlock);
		
		// line offset table
		data.skip(numBlocks * 8);
		
		Half half = Half.getInstance();
		for (int i = 0; i < numBlocks; i++) {
			
			int yStart = data.readInt() - dataWindow.yMin;
			int size = data.readInt();
			byte[] block = data.readBytes(size);
			
			block = compressor.uncompress(block);
			
			int yMax = compression.linesPerBlock;
			if (i == numBlocks - 1) {
				int tmp = height % compression.linesPerBlock;
				if (tmp > 0) yMax = tmp;
			}
			
			int pos = 0;
			for (int yBlock = 0; yBlock < yMax; yBlock++) {
				LOOP:
				for (Channel channel : channels.list) {
					int channelOffset = getChannelOffset(channel);
					int yOffset = (yStart + yBlock) * width;
					for (int x = 0; x < width; x++) {
						
						if (channelOffset == -1) {
							pos += channel.getNumBytes() * width;
							continue LOOP;
						}
						
						short bitPattern = (short) (((block[pos + 1] & 0xff) << 8) | (block[pos] & 0xff));
						pos += 2;
						
						int index = (x + yOffset) * 3 + channelOffset;
						pixels[index] = half.toFloat(bitPattern);
					}
				}
			}
		}
	}
	
	private Attribute getAttribute(String name) throws OpenExrException {
		Attribute attribute = header.get(name);
		if (attribute == null) {
			throw new OpenExrException("Required attribute \"" + name + "\" not found");
		}
		return attribute;
	}
	
	private void setDimensions(Box2i dataWindow) {
		width = dataWindow.xMax - dataWindow.xMin + 1;
		height = dataWindow.yMax - dataWindow.yMin + 1;
	}
	
	private void verify(Channels channels) throws OpenExrException {
		
		boolean r = false;
		boolean g = false;
		boolean b = false;
		
		List<Channel> list = channels.list;
		for (Channel channel : list) {
			String name = channel.name.toLowerCase();
			switch (name) {
				case "r":
					verify(channel);
					r = true;
					break;
				case "g":
					verify(channel);
					g = true;
					break;
				case "b":
					verify(channel);
					b = true;
					break;
			}
		}
		
		if (!r || !g || !b) {
			throw new OpenExrException("No RGB colour channels were found");
		}
	}
	
	private void verify(Channel channel) throws OpenExrException {
		
		if (channel.pixelType != Channel.HALF) {
			throw new OpenExrException("Pixels must be stored in half precision floating-point format");
		}
		
		if (channel.xSampling != 1 || channel.ySampling != 1) {
			throw new OpenExrException("Pixel subsampling is not supported");
		}
	}
	
	private int getBytesPerRow(List<Channel> list) {
		int pixelBytes = 0;
		for (Channel channel : list) {
			pixelBytes += channel.getNumBytes();
		}
		return pixelBytes * width;
	}
	
	private int getChannelOffset(Channel channel) {
		char c = channel.name.charAt(0);
		if (c == 82) return 0;
		if (c == 71) return 1;
		if (c == 66) return 2;
		return -1;
	}
	
	/**
	 * Martin Ankerl's fast approximation of pow(x, y):
	 * x^y = exp(ln(x^y)) = exp(y*ln(x))
	 * 
	 * http://martin.ankerl.com
	 */
	private double pow(double a, double b) {
		int x = (int) (Double.doubleToLongBits(a) >> 32);
		int y = (int) (b * (x - 1072632447) + 1072632447);
		return Double.longBitsToDouble(((long) y) << 32);
	}
}