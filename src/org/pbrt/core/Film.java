
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class Film {

    // Film Private Data
    private class Pixel {
        Pixel() {}
        float xyz[] = {0, 0, 0};
        float filterWeightSum = 0;
        AtomicFloat splatXYZ[] = {new AtomicFloat(0), new AtomicFloat(0), new AtomicFloat(0)};
    }
    private Pixel[] pixels;
    private static final int filterTableWidth = 16;
    private float filterTable[] = new float[filterTableWidth * filterTableWidth];
    private final float scale;
    private final float maxSampleLuminance;

    // FilmTilePixel Declarations
    public class FilmTilePixel {
        Spectrum contribSum = new Spectrum(0);
        float filterWeightSum = 0;
    }

    public class FilmTile {
        // FilmTile Public Methods
        public FilmTile(Bounds2i pixelBounds, Vector2f filterRadius,
             float[] filterTable, int filterTableSize, float maxSampleLuminance) {
            this.pixelBounds = pixelBounds;
            this.filterRadius = filterRadius;
            this.invFilterRadius = new Vector2f(1 / filterRadius.x, 1 / filterRadius.y);
            this.filterTable = filterTable;
            this.filterTableSize = filterTableSize;
            this.maxSampleLuminance = maxSampleLuminance;
            this.pixels = new FilmTilePixel[Math.max(0, pixelBounds.Area())];
            for (int i = 0; i < this.pixels.length; i++) {
                this.pixels[i] = new FilmTilePixel();
            }
        }
        public void AddSample(Point2f pFilm, Spectrum L, float sampleWeight) {
            if (L.y() > maxSampleLuminance)
                L.scale(maxSampleLuminance / L.y());

            // Compute sample's raster bounds
            Point2f pFilmDiscrete = pFilm.subtract(new Vector2f(0.5f, 0.5f));
            Point2i p0 = new Point2i(Point2f.Ceil(pFilmDiscrete.subtract(filterRadius)));
            Point2i p1 = new Point2i(Point2f.Floor(pFilmDiscrete.add(filterRadius))).add(new Point2i(1, 1));
            p0 = Point2i.Max(p0, pixelBounds.pMin);
            p1 = Point2i.Min(p1, pixelBounds.pMax);

            // Loop over filter support and add sample to pixel arrays

            // Precompute $x$ and $y$ filter table offsets
            int[] ifx = new int[p1.x - p0.x];
            for (int x = p0.x; x < p1.x; ++x) {
                float fx = Math.abs((x - pFilmDiscrete.x) * invFilterRadius.x *
                        filterTableSize);
                ifx[x - p0.x] = Math.min((int)Math.floor(fx), filterTableSize - 1);
            }
            int[] ify = new int[p1.y - p0.y];
            for (int y = p0.y; y < p1.y; ++y) {
                float fy = Math.abs((y - pFilmDiscrete.y) * invFilterRadius.y *
                        filterTableSize);
                ify[y - p0.y] = Math.min((int)Math.floor(fy), filterTableSize - 1);
            }
            for (int y = p0.y; y < p1.y; ++y) {
                for (int x = p0.x; x < p1.x; ++x) {
                    // Evaluate filter value at $(x,y)$ pixel
                    int offset = ify[y - p0.y] * filterTableSize + ifx[x - p0.x];
                    float filterWeight = filterTable[offset];

                    // Update pixel values with filtered sample contribution
                    FilmTilePixel pixel = GetPixel(new Point2i(x, y));
                    pixel.contribSum = Spectrum.Add(pixel.contribSum, Spectrum.Scale(L, sampleWeight * filterWeight));
                    pixel.filterWeightSum += filterWeight;
                }
            }
        }
        public FilmTilePixel GetPixel(Point2i p) {
            assert (Bounds2i.InsideExclusive(p, pixelBounds));
            int width = pixelBounds.pMax.x - pixelBounds.pMin.x;
            int offset = (p.x - pixelBounds.pMin.x) + (p.y - pixelBounds.pMin.y) * width;
            return pixels[offset];
        }
        public Bounds2i GetPixelBounds() { return pixelBounds; }

        // FilmTile Private Data
        private Bounds2i pixelBounds;
        private Vector2f filterRadius, invFilterRadius;
        private float[] filterTable;
        private int filterTableSize;
        private FilmTilePixel[] pixels;
        private float maxSampleLuminance;
    }

    // Film Public Methods
    public Film(Point2i resolution, Bounds2f cropWindow, Filter filter, float diagonal, String filename, float scale, float maxSampleLuminance) {
        this.fullResolution = resolution;
        this.diagonal = diagonal * 0.001f;
        this.filter = filter;
        this.filename = filename;
        this.scale = scale;
        this.maxSampleLuminance = maxSampleLuminance;

        // Compute film image bounds
        this.croppedPixelBounds =
                new Bounds2i(new Point2i((int)Math.ceil(fullResolution.x * cropWindow.pMin.x),
                        (int)Math.ceil(fullResolution.y * cropWindow.pMin.y)),
                new Point2i((int)Math.ceil(fullResolution.x * cropWindow.pMax.x),
                        (int)Math.ceil(fullResolution.y * cropWindow.pMax.y)));
        Api.logger.info("Created film with full resolution %s. Crop window of %s -> croppedPixelBounds %s", resolution.toString(), cropWindow, croppedPixelBounds);

        // Allocate film image storage
        this.pixels = new Pixel[croppedPixelBounds.Area()];
        for (int i = 0; i < this.pixels.length; i++) this.pixels[i] = new Pixel();
        filmPixelMemory.increment(croppedPixelBounds.Area() * 8*4);

        // Precompute filter weight table
        int offset = 0;
        for (int y = 0; y < filterTableWidth; ++y) {
            for (int x = 0; x < filterTableWidth; ++x, ++offset) {
                Point2f p = new Point2f();
                p.x = (x + 0.5f) * filter.radius.x / filterTableWidth;
                p.y = (y + 0.5f) * filter.radius.y / filterTableWidth;
                filterTable[offset] = filter.Evaluate(p);
            }
        }
    }

    public Bounds2i GetSampleBounds() {
        Vector2f halfPixel = new Vector2f(0.5f, 0.5f);
        Bounds2f floatBounds = new Bounds2f(Point2f.Floor(new Point2f(croppedPixelBounds.pMin).add(halfPixel.subtract(filter.radius))),
                Point2f.Ceil(new Point2f(croppedPixelBounds.pMax).subtract(halfPixel.add(filter.radius))));
        return new Bounds2i(floatBounds);
    }

    public Bounds2f GetPhysicalExtent() {
        float aspect = (float)fullResolution.y / (float)fullResolution.x;
        float x = (float)Math.sqrt(diagonal * diagonal / (1 + aspect * aspect));
        float y = aspect * x;
        return new Bounds2f(new Point2f(-x / 2, -y / 2), new Point2f(x / 2, y / 2));
    }

    public FilmTile GetFilmTile(Bounds2i sampleBounds) {
        // Bound image pixels that samples in _sampleBounds_ contribute to
        Vector2f halfPixel = new Vector2f(0.5f, 0.5f);
        Bounds2f floatBounds = new Bounds2f(sampleBounds);
        Point2i p0 = new Point2i(Point2f.Ceil(floatBounds.pMin.subtract(halfPixel).subtract(filter.radius)));
        Point2i p1 = new Point2i(Point2f.Floor(floatBounds.pMax.subtract(halfPixel).add(filter.radius))).add(new Point2i(1, 1));
        Bounds2i tilePixelBounds = Bounds2i.Intersect(new Bounds2i(p0, p1), croppedPixelBounds);
        return new FilmTile(tilePixelBounds, filter.radius, filterTable, filterTableWidth, maxSampleLuminance);
    }

    public void MergeFilmTile(FilmTile tile) {
        //Api.logger.trace("Merging film tile %s", tile.pixelBounds.toString());
        //std::lock_guard<std::mutex> lock(mutex);
        for (int y = tile.GetPixelBounds().pMin.y; y < tile.GetPixelBounds().pMax.y; y++) {
            for (int x = tile.GetPixelBounds().pMin.x; x < tile.GetPixelBounds().pMax.x; x++) {
                Point2i pixel = new Point2i(x, y);
                // Merge _pixel_ into _Film::pixels_
                final FilmTilePixel tilePixel = tile.GetPixel(pixel);
                Pixel mergePixel = GetPixel(pixel);
                float[] xyz = tilePixel.contribSum.toXYZ();
                for (int i = 0; i < 3; ++i) mergePixel.xyz[i] += xyz[i];
                mergePixel.filterWeightSum += tilePixel.filterWeightSum;
                SetPixel(pixel, mergePixel);
            }
        }
    }

    public void SetImage(Spectrum[] img) {
        int nPixels = croppedPixelBounds.Area();
        for (int i = 0; i < nPixels; ++i) {
            Pixel p = pixels[i];
            p.xyz = img[i].toXYZ();
            p.filterWeightSum = 1;
            p.splatXYZ[0].set(0);
            p.splatXYZ[1].set(0);
            p.splatXYZ[2].set(0);
        }
    }

    public void AddSplat(Point2f p, Spectrum v) {
        if (v.hasNaNs()) {
            Api.logger.error("Ignoring splatted spectrum with NaN values at (%f, %f)", p.x, p.y);
            return;
        } else if (v.y() < 0) {
            Api.logger.error("Ignoring splatted spectrum with negative luminance %f at (%f, %f)", v.y(), p.x, p.y);
            return;
        } else if (Float.isInfinite(v.y())) {
            Api.logger.error("Ignoring splatted spectrum with infinite luminance at (%f, %f)", p.x, p.y);
            return;
        }

        if (!Bounds2i.InsideExclusive(new Point2i(p), croppedPixelBounds)) return;
        if (v.y() > maxSampleLuminance)
            v = v.scale(maxSampleLuminance / v.y());
        float[] xyz = v.toXYZ();
        Pixel pixel = GetPixel(new Point2i(p));
        for (int i = 0; i < 3; ++i) pixel.splatXYZ[i].add(xyz[i]);

    }

    public void WriteImage(float splatScale) {

        // Convert image to RGB and compute final pixel values
        //LOG(INFO) << "Converting image to RGB and computing final weighted pixel values";
        float[] pixRGB = new float[3];
        float[] splatRGB = new float[3];
        float[] rgb = new float[3 * croppedPixelBounds.Area()];
        int offset = 0;
        for (int py = croppedPixelBounds.pMin.y; py < croppedPixelBounds.pMax.y; py++) {
            for (int px = croppedPixelBounds.pMin.x; px < croppedPixelBounds.pMax.x; px++) {
                Point2i p = new Point2i(px, py);

                // Convert pixel XYZ color to RGB
                Pixel pixel = GetPixel(p);
                pixRGB = Spectrum.XYZToRGB(pixel.xyz, pixRGB);
                rgb[3 * offset] = pixRGB[0];
                rgb[3 * offset+1] = pixRGB[1];
                rgb[3 * offset+2] = pixRGB[2];

                // Normalize pixel with weight sum
                float filterWeightSum = pixel.filterWeightSum;
                if (filterWeightSum != 0) {
                    float invWt = 1 / filterWeightSum;
                    rgb[3 * offset] = Math.max(0, rgb[3 * offset] * invWt);
                    rgb[3 * offset + 1] = Math.max(0, rgb[3 * offset + 1] * invWt);
                    rgb[3 * offset + 2] = Math.max(0, rgb[3 * offset + 2] * invWt);
                }

                // Add splat value at pixel
                float[] splatXYZ = {pixel.splatXYZ[0].get(), pixel.splatXYZ[1].get(), pixel.splatXYZ[2].get()};
                splatRGB = Spectrum.XYZToRGB(splatXYZ, splatRGB);
                rgb[3 * offset] += splatScale * splatRGB[0];
                rgb[3 * offset + 1] += splatScale * splatRGB[1];
                rgb[3 * offset + 2] += splatScale * splatRGB[2];

                // Scale pixel value by _scale_
                rgb[3 * offset] *= scale;
                rgb[3 * offset + 1] *= scale;
                rgb[3 * offset + 2] *= scale;
                ++offset;
            }
        }

        // Write RGB image
        Api.logger.info("Writing image %s with bounds %s", filename, croppedPixelBounds.toString());
        ImageIO.Write(filename, rgb, croppedPixelBounds, fullResolution);
    }

    public void Clear() {
        for (int py = croppedPixelBounds.pMin.y; py < croppedPixelBounds.pMax.y; py++) {
            for (int px = croppedPixelBounds.pMin.x; px < croppedPixelBounds.pMax.x; px++) {
                Point2i p = new Point2i(px, py);
                Pixel pixel = GetPixel(p);
                for (int c = 0; c < 3; ++c) {
                    pixel.splatXYZ[c].set(0);
                    pixel.xyz[c] = 0;
                }
                pixel.filterWeightSum = 0;
            }
        }
    }

    // Film Public Data
    public final Point2i fullResolution;
    public final float diagonal;
    public Filter filter;
    public final String filename;
    public Bounds2i croppedPixelBounds;

    public static Film Create(ParamSet paramSet, Filter filter) {
        // Intentionally use FindOneString() rather than FindOneFilename() here
        // so that the rendered image is left in the working directory, rather
        // than the directory the scene file lives in.
        String filename = paramSet.FindOneString("filename", "");
        if (!Pbrt.options.ImageFile.isEmpty()) {
            if (!filename.isEmpty()) {
                Error.Warning("Output filename supplied on command line, \"%s\", ignored " +
                        "due to filename provided in scene description file, \"%s\".",
                        Pbrt.options.ImageFile, filename);
            } else {
                filename = Pbrt.options.ImageFile;
            }
        }
        if (filename.isEmpty()) filename = "pbrt.exr";

        int xres = paramSet.FindOneInt("xresolution", 1280);
        int yres = paramSet.FindOneInt("yresolution", 720);
        if (Pbrt.options.QuickRender) xres = Math.max(1, xres / 4);
        if (Pbrt.options.QuickRender) yres = Math.max(1, yres / 4);
        Bounds2f crop = new Bounds2f(new Point2f(0, 0), new Point2f(1, 1));
        Float[] cr = paramSet.FindFloat("cropwindow");
        if (cr != null && cr.length == 4) {
            crop.pMin.x = Pbrt.Clamp(Math.min(cr[0], cr[1]), 0, 1);
            crop.pMax.x = Pbrt.Clamp(Math.max(cr[0], cr[1]), 0, 1);
            crop.pMin.y = Pbrt.Clamp(Math.min(cr[2], cr[3]), 0, 1);
            crop.pMax.y = Pbrt.Clamp(Math.max(cr[2], cr[3]), 0, 1);
        } else if (cr != null) {
            Error.Error("%d values supplied for \"cropwindow\". Expected 4.", cr.length);
        }

        float scale = paramSet.FindOneFloat("scale", 1);
        float diagonal = paramSet.FindOneFloat("diagonal", 35);
        float maxSampleLuminance = paramSet.FindOneFloat("maxsampleluminance", Pbrt.Infinity);
        return new Film(new Point2i(xres, yres), crop, filter, diagonal, filename, scale, maxSampleLuminance);
    }

    // Film Private Methods
    private Pixel GetPixel(Point2i p) {
        assert (Bounds2i.InsideExclusive(p, croppedPixelBounds));
        int width = croppedPixelBounds.pMax.x - croppedPixelBounds.pMin.x;
        int offset = (p.x - croppedPixelBounds.pMin.x) + (p.y - croppedPixelBounds.pMin.y) * width;
        return pixels[offset];
    }
    private void SetPixel(Point2i p, Pixel pix) {
        assert (Bounds2i.InsideExclusive(p, croppedPixelBounds));
        int width = croppedPixelBounds.pMax.x - croppedPixelBounds.pMin.x;
        int offset = (p.x - croppedPixelBounds.pMin.x) + (p.y - croppedPixelBounds.pMin.y) * width;
        pixels[offset] = pix;
    }

    private static Stats.MemoryCounter filmPixelMemory = new Stats.MemoryCounter("Memory/Film pixels");

}