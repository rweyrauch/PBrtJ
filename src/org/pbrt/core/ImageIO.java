
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import org.pbrt.openexr.*;
import org.pbrt.openexr.exception.OpenExrException;

public class ImageIO {
    public static void Write(String filename, float[] rgb, Bounds2i outputBounds, Point2i totalResolution) {
        Vector2i resolution = outputBounds.Diagonal();
        if (FileUtil.HasExtension(filename, ".exr")) {
            WriteEXR(filename, rgb, resolution.x, resolution.y, totalResolution.x, totalResolution.y, outputBounds.pMin.x, outputBounds.pMin.y);
        }
        else if (FileUtil.HasExtension(filename, ".pfm")) {
            WritePFM(filename, rgb, resolution.x, resolution.y);
        }
        try {
            BufferedImage bimage = new BufferedImage(resolution.x, resolution.y, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < bimage.getHeight(); y++) {
                for (int x = 0; x < bimage.getWidth(); x++) {
                    int r = Pbrt.Clamp((int)(rgb[(y * totalResolution.x + x) * 3 + 0] * 255.0f), 0, 255);
                    int g = Pbrt.Clamp((int)(rgb[(y * totalResolution.x + x) * 3 + 1] * 255.0f), 0, 255);
                    int b = Pbrt.Clamp((int)(rgb[(y * totalResolution.x + x) * 3 + 2] * 255.0f), 0, 255);
                    int bgra = (int)(255 << 24) | (int)(r << 16) | (int)(g << 8) | b;
                    bimage.setRGB(x, y, bgra);
                }
            }
            boolean ok = javax.imageio.ImageIO.write(bimage, FileUtil.GetExtension(filename), new File(filename));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final static String[] knownFormats;
    static {
        knownFormats = javax.imageio.ImageIO.getReaderFormatNames();
    }

    public static class SpectrumImage {
        public Spectrum[] image;
        public Point2i resolution = new Point2i(0, 0);
    }

    public static SpectrumImage Read(String filename) {
        if (FileUtil.HasExtension(filename, ".exr")) {
            return ReadEXR(filename);
        }
        else if (FileUtil.HasExtension(filename, ".pfm")) {
            return ReadPFM(filename);
        }
        try {
            BufferedImage bimage = javax.imageio.ImageIO.read(new File(filename));
            if (bimage != null) {
                SpectrumImage simage = new SpectrumImage();
                simage.resolution = new Point2i(bimage.getWidth(), bimage.getHeight());
                simage.image = new Spectrum[simage.resolution.x * simage.resolution.y];
                for (int y = 0; y < bimage.getHeight(); y++) {
                    for (int x = 0; x < bimage.getWidth(); x++) {
                        int argb = bimage.getRGB(x, y);
                        simage.image[y * bimage.getWidth() + x] = Spectrum.FromRGB(argb);
                    }
                }
                return simage;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Error.Error("Unable to load image stored in format \"%s\" for filename \"%s\".",
                FileUtil.GetExtension(filename), filename);

        return null;
    }

    private static SpectrumImage ReadEXR(String filename) {
        SpectrumImage image = null;
        try {
            OpenExr oexr = new OpenExr(new File(filename));
            image = new SpectrumImage();
            image.resolution = new Point2i(oexr.getWidth(), oexr.getHeight());
            image.image = new Spectrum[oexr.getWidth()* oexr.getHeight()];
            final float[] pixels = oexr.getPixels();
            for (int pix = 0; pix < pixels.length / 3 ; pix++) {
                image.image[pix] = Spectrum.FromRGB(pixels[pix*3+0], pixels[pix*3+1], pixels[pix*3+2]);
            }
        } catch (OpenExrException e) {
            e.printStackTrace();
        }
        return image;
    }
    private static SpectrumImage ReadPFM(String filename) {
        Error.Warning("PFM image read not implemented.");
        return null;
    }

    private static void WritePFM(String filename, float[] rgb, int x, int y) {
    }

    private native static void WriteEXR(String filename, float[] rgb, int xRes, int yRes, int totalXRes, int totalYRes, int xOffset, int yOffset);

}