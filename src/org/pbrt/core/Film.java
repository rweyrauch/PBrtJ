
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
        //AtomicFloat splatXYZ[];
        float pad;
    }
    Pixel[] pixels;
    private static final int filterTableWidth = 16;
    private float filterTable[] = new float[filterTableWidth * filterTableWidth];
    private float scale;
    private float maxSampleLuminance;

    // FilmTilePixel Declarations
    public class FilmTilePixel {
        Spectrum contribSum = new Spectrum(0);
        float filterWeightSum = 0;
    };

    public class FilmTile {
        // FilmTile Public Methods
        public FilmTile(Bounds2i pixelBounds, Vector2f filterRadius,
             float[] filterTable, int filterTableSize,
                 float maxSampleLuminance) {
            this.pixelBounds = pixelBounds;
            this.filterRadius = filterRadius;
            this.invFilterRadius = new Vector2f(1 / filterRadius.x, 1 / filterRadius.y);
            this.filterTable = filterTable;
            this.filterTableSize = filterTableSize;
            this.maxSampleLuminance = maxSampleLuminance;
            this.pixels = new FilmTilePixel[Math.max(0, pixelBounds.Area())];
        }
        public void AddSample(Point2f pFilm, Spectrum L, float sampleWeight) {
            //ProfilePhase _(Prof::AddFilmSample);
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
                    pixel.contribSum = (Spectrum)CoefficientSpectrum.add(pixel.contribSum, CoefficientSpectrum.scale(L, sampleWeight * filterWeight));
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
    };

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
        //LOG(INFO) << "Created film with full resolution " << resolution <<
        //        ". Crop window of " << cropWindow << " -> croppedPixelBounds " <<
        //        croppedPixelBounds;

        // Allocate film image storage
        this.pixels = new Pixel[croppedPixelBounds.Area()];

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
        Point2i p0 = new Point2i(Point2f.Ceil(floatBounds.pMin.subtract(halfPixel.subtract(filter.radius))));
        Point2i p1 = new Point2i(Point2f.Floor(floatBounds.pMax.subtract(halfPixel.add(filter.radius)))).add(new Point2i(1, 1));
        Bounds2i tilePixelBounds = Bounds2i.Intersect(new Bounds2i(p0, p1), croppedPixelBounds);
        return new FilmTile(tilePixelBounds, filter.radius, filterTable, filterTableWidth, maxSampleLuminance);
    }
    public void MergeFilmTile(FilmTile tile) {

    }
    public void SetImage(Spectrum[] img) {

    }
    public void AddSplat(Point2f p, Spectrum v) {

    }
    public void WriteImage(float splatScale) {

    }
    public void Clear() {

    }

    // Film Public Data
    public Point2i fullResolution;
    public float diagonal;
    Filter filter;
    String filename;
    Bounds2i croppedPixelBounds;

    public static Film Create(ParamSet paramSet, Filter filter) {
        return null;
    }

    // Film Private Methods
    private Pixel GetPixel(Point2i p) {
        assert (Bounds2i.InsideExclusive(p, croppedPixelBounds));
        int width = croppedPixelBounds.pMax.x - croppedPixelBounds.pMin.x;
        int offset = (p.x - croppedPixelBounds.pMin.x) + (p.y - croppedPixelBounds.pMin.y) * width;
        return pixels[offset];
    }

}