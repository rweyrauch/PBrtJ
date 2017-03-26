/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

public class EXROutput {

    public static final int SUCCESS = 0;
    public static final int INVALID_ARGUMENT = -1;

    public static final int PIXELTYPE_UINT = 0;
    public static final int PIXELTYPE_HALF = 1;
    public static final int PIXELTYPE_FLOAT = 2;

    public static final int COMPRESSIONTYPE_NONE = 0;

    private static CharsetEncoder enc = Charset.forName("ISO-8859-1").newEncoder();

    public static int SaveEXR(float[] data, int width, int height, int components, String filename) {

        if (components == 3 || components == 4) {
            // OK
        }
        else {
            return INVALID_ARGUMENT;
        }

        Header header = new Header();
        Image image = new Image();

        image.num_channels = components;

        float[] imageRed = new float[width * height];
        float[] imageGreen = new float[width * height];
        float[] imageBlue = new float[width * height];
        float[] imageAlpha = new float[width * height];

        for (int i = 0; i < width * height; i++) {
            imageRed[i] = data[components * i + 0];
            imageGreen[i] = data[components * i + 1];
            imageBlue[i] = data[components * i + 2];
            if (components == 4) {
                imageAlpha[i] = data[components * i + 3];
            }
        }

        image.images = new float[4][];
        if (components == 4) {
            image.images[0] = imageAlpha;
            image.images[1] = imageBlue;
            image.images[2] = imageGreen;
            image.images[3] = imageRed;
        }
        else {
            image.images[0] = imageBlue;
            image.images[1] = imageGreen;
            image.images[2] = imageRed;
        }

        image.width = width;
        image.height = height;

        header.channels = new ChannelInfo[components];
        for (int c = 0; c < components; c++) {
            header.channels[c] = new ChannelInfo();
        }
        if (components == 4) {
            header.channels[0].name = "A";
            header.channels[1].name = "B";
            header.channels[2].name = "G";
            header.channels[3].name = "R";
        }
        else {
            header.channels[0].name = "B";
            header.channels[1].name = "G";
            header.channels[2].name = "R";
        }

        header.data_window = new int[]{ 0, 0, width-1, height-1};
        header.display_window = new int[]{ 0, 0, width-1, height-1};

        header.line_order = 0;
        header.pixel_aspect_ratio = 1.0f;
        header.screen_window_width = width;
        header.screen_window_center[0] = 0;
        header.screen_window_center[1] = 0;

        return SaveEXRImageToFile(image, header, filename);
    }

    private static int SaveEXRImageToFile(Image image, Header header, String filename) {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

        int ret = SaveEXRImageToMemory(image, header, byteBuffer);
        if (ret == SUCCESS) {
            // write byte buffer to file
            FileOutputStream fos;
            try {
                fos = new FileOutputStream(new File(filename));
                fos.write(byteBuffer.toByteArray());
                fos.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    private static void writeString(ByteArrayOutputStream byteBuffer, String str) throws IOException {
        byte[] buf = new byte[str.length()+1];
        ByteBuffer bb = ByteBuffer.wrap(buf);
        enc.encode(CharBuffer.wrap(str), bb, true);
        buf[str.length()] = 0;
        byteBuffer.write(buf);
    }

    private static void writeAttribute(ByteArrayOutputStream byteBuffer, String name, String type, byte b[]) throws IOException {
        writeString(byteBuffer, name);
        writeString(byteBuffer, type);
        int outLen = b.length;
        byteBuffer.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(outLen).array());
        byteBuffer.write(b);
    }

    private static byte[] packChannelInfo(Header header) {
        ByteArrayOutputStream channelBytes = new ByteArrayOutputStream();
        try {
            for (int c = 0; c < header.channels.length; c++) {
                writeString(channelBytes, header.channels[c].name);
                channelBytes.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(header.channels[c].pixel_type).array());

                channelBytes.write(header.channels[c].p_linear);
                channelBytes.write(header.channels[c].pad);

                channelBytes.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(header.channels[c].x_sampling).array());
                channelBytes.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(header.channels[c].y_sampling).array());
            }
            channelBytes.write(0);
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
        return channelBytes.toByteArray();
    }

    private static int SaveEXRImageToMemory(Image image, Header header, ByteArrayOutputStream byteBuffer) {

        // Write header attributes
        try {
            final byte[] magicNumber = { 0x76, 0x2f, 0x31, 0x01};
            byteBuffer.write(magicNumber);

            final byte[] marker = {2, 0, 0, 0 };
            byteBuffer.write(marker);

            byte[] channelBytes = packChannelInfo(header);
            writeAttribute(byteBuffer, "channels", "chlist", channelBytes);

            writeAttribute(byteBuffer, "compression", "compression", ByteBuffer.allocate(1).put((byte)COMPRESSIONTYPE_NONE).array());

            ByteBuffer sizeBuff = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
            sizeBuff.putInt(header.data_window[0]);
            sizeBuff.putInt(header.data_window[1]);
            sizeBuff.putInt(header.data_window[2]);
            sizeBuff.putInt(header.data_window[3]);
            writeAttribute(byteBuffer, "dataWindow", "box2i", sizeBuff.array());

            sizeBuff.clear();
            sizeBuff.putInt(header.display_window[0]);
            sizeBuff.putInt(header.display_window[1]);
            sizeBuff.putInt(header.display_window[2]);
            sizeBuff.putInt(header.display_window[3]);

            writeAttribute(byteBuffer, "displayWindow", "box2i", sizeBuff.array());

            writeAttribute(byteBuffer, "lineOrder", "lineOrder", ByteBuffer.allocate(1).put((byte)header.line_order).array());
            writeAttribute(byteBuffer, "pixelAspectRatio", "float", ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(header.pixel_aspect_ratio).array());

            ByteBuffer centerBuff = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
            centerBuff.putFloat(header.screen_window_center[0]);
            centerBuff.putFloat(header.screen_window_center[1]);
            writeAttribute(byteBuffer, "screenWindowCenter", "v2f", centerBuff.array());

            writeAttribute(byteBuffer, "screenWindowWidth", "float", ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(header.screen_window_width).array());

            // End of header
            byteBuffer.write(0);

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        int numScanlines = 1;  // always 1 for uncompressed
        int numBlocks = image.height / numScanlines;
        int headerSize = byteBuffer.size();
        long offset = headerSize + numBlocks * 8;

        // Write pixels - half, uncompressed
        try {
            // Table of block (line) offsets
            final int lineSize = image.width * 2 * header.channels.length;

            for (int y = 0; y < image.height; y++) {
                byteBuffer.write(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(offset).array());
                offset += lineSize;
            }

            for (int y = 0; y < image.height; y++) {
                byteBuffer.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(y).array());
                byteBuffer.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(lineSize).array());
                for (int c = 0; c < header.channels.length; c++) {
                    for (int x = 0; x < image.width; x++) {
                        float pixel = image.images[c][y * image.width + x];
                        short hpixel = toHalf(pixel);
                        byteBuffer.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(hpixel).array());
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return SUCCESS;
    }

    private static class ChannelInfo {
        String name; // max 256 chars
        int pixel_type = PIXELTYPE_HALF;
        int x_sampling = 1;
        int y_sampling = 1;
        byte p_linear = 0;
        byte[] pad = new byte[3];
    }

    private static class Header {
        float pixel_aspect_ratio = 1;
        int line_order = 0;
        int[] data_window = new int[4];
        int[] display_window = new int[4];
        float[] screen_window_center = new float[2];
        float screen_window_width;

        ChannelInfo[] channels;
    }

    private static class Image {
        float[][] images;
        int width;
        int height;
        int num_channels;
    }

    @SuppressWarnings ( "FloatingPointEquality" )
    private static short toHalf(final float v)
    {
        if(Float.isNaN(v)) throw new UnsupportedOperationException("NaN to half conversion not supported!");
        if(v == Float.POSITIVE_INFINITY) return(short)0x7c00;
        if(v == Float.NEGATIVE_INFINITY) return(short)0xfc00;
        if(v == 0.0f) return(short)0x0000;
        if(v == -0.0f) return(short)0x8000;
        if(v > 65504.0f) return 0x7bff;  // max value supported by half float
        if(v < -65504.0f) return(short)( 0x7bff | 0x8000 );
        if(v > 0.0f && v < 5.96046E-8f) return 0x0001;
        if(v < 0.0f && v > -5.96046E-8f) return(short)0x8001;

        final int f = Float.floatToIntBits(v);

        return(short)((( f>>16 ) & 0x8000 ) | (((( f & 0x7f800000 ) - 0x38000000 )>>13 ) & 0x7c00 ) | (( f>>13 ) & 0x03ff ));
    }
}