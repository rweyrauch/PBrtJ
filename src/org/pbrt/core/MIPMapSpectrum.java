/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

import java.util.ArrayList;

public class MIPMapSpectrum {

    public static class ResampleWeight {
        public int firstTexel;
        public float[] weight = { 0, 0, 0, 0 };
    }

    public MIPMapSpectrum(Point2i resolution, Spectrum[] data, boolean doTri, float maxAniso, Texture.ImageWrap wrapMode, Spectrum black){
        this.doTrilinear = doTri;
        this.maxAnisotropy = maxAniso;
        this.wrapMode = wrapMode;
        this.resolution = new Point2i(resolution);
        this.black = black;

        Spectrum[] resampledImage = null;
        if (!Pbrt.IsPowerOf2(resolution.x) || !Pbrt.IsPowerOf2(resolution.y)) {
            // Resample image to power-of-two resolution
            Point2i resPow2 = new Point2i(Pbrt.RoundUpPow2(resolution.x), Pbrt.RoundUpPow2(resolution.y));
            Api.logger.info("Resampling MIPMap from %s to %s. Ratio = %f", resolution.toString(), resPow2.toString(), (float)(resPow2.x * resPow2.y) / (float)(resolution.x * resolution.y));
            // Resample image in $s$ direction
            ResampleWeight[] sWeights = resampleWeights(resolution.x, resPow2.x);
            resampledImage = new Spectrum[resPow2.x * resPow2.y];

            // Apply _sWeights_ to zoom in $s$ direction
            for (int t = 0; t < resolution.y; t++) {
                for (int s = 0; s < resPow2.x; ++s) {
                    // Compute texel $(s,t)$ in $s$-zoomed image
                    resampledImage[t * resPow2.x + s] = black;
                    for (int j = 0; j < 4; ++j) {
                        int origS = sWeights[s].firstTexel + j;
                        if (wrapMode == Texture.ImageWrap.Repeat)
                            origS = Pbrt.Mod(origS, resolution.x);
                        else if (wrapMode == Texture.ImageWrap.Clamp)
                            origS = Pbrt.Clamp(origS, 0, resolution.x - 1);
                        if (origS >= 0 && origS < resolution.x) {
                            Spectrum pixel = resampledImage[t * resPow2.x + s];
                            resampledImage[t * resPow2.x + s] = pixel.add(data[t * resolution.x + origS].scale(sWeights[s].weight[j]));
                        }
                    }
                }
            }

            // Resample image in $t$ direction
            ResampleWeight[] tWeights = resampleWeights(resolution.y, resPow2.y);
            Spectrum[] workData = new Spectrum[resolution.y * resolution.y];
            for (int s = 0; s < resPow2.x; s++) {
                for (int t = 0; t < resPow2.y; ++t) {
                    workData[t] = black;
                    for (int j = 0; j < 4; ++j) {
                        int offset = tWeights[t].firstTexel + j;
                        if (wrapMode == Texture.ImageWrap.Repeat)
                            offset = Pbrt.Mod(offset, resolution.y);
                        else if (wrapMode == Texture.ImageWrap.Clamp)
                            offset = Pbrt.Clamp(offset, 0, resolution.y - 1);
                        if (offset >= 0 && offset < resolution.y) {
                            Spectrum pixel = resampledImage[offset * resPow2.x + s];
                            Spectrum workPixel = workData[t];
                            workData[t] = workPixel.add(pixel.scale(tWeights[t].weight[j]));
                        }
                    }
                }
                for (int t = 0; t < resPow2.y; ++t) {
                    Spectrum workPixel = workData[t];
                    resampledImage[t * resPow2.x + s] = workPixel.clamp(0, Pbrt.Infinity);
                }
            }
            resolution = resPow2;
        }
        else {
            resampledImage = new Spectrum[resolution.x * resolution.y];
            for (int s = 0; s < resolution.x; s++) {
                for (int t = 0; t < resolution.y; t++) {
                    resampledImage[t * resolution.x + s] = data[t * resolution.x + s];
                }
            }
        }
        // Initialize levels of MIPMap from image
        int nLevels = 1 + Pbrt.Log2Int(Math.max(resolution.x, resolution.y));
        this.pyramid = new ArrayList<>(nLevels);

        // Initialize most detailed level of MIPMap
        int logBlockSize = 4;
        pyramid.add(new BlockedArray(resolution.x, resolution.y, logBlockSize, resampledImage));
        for (int i = 1; i < nLevels; ++i) {
            // Initialize $i$th MIPMap level from $i-1$st level
            int sRes = Math.max(1, pyramid.get(i - 1).uSize() / 2);
            int tRes = Math.max(1, pyramid.get(i - 1).vSize() / 2);
            pyramid.add(new BlockedArray(sRes, tRes, logBlockSize, null));

            // Filter four texels from finer level of pyramid
            for (int t = 0; t < tRes; t++) {
                for (int s = 0; s < sRes; ++s) {
                    pyramid.get(i).set(s, t, (Texel(i - 1, 2 * s, 2 * t).add(Texel(i - 1, 2 * s + 1, 2 * t)).add(Texel(i - 1, 2 * s, 2 * t + 1)).add(
                            Texel(i - 1, 2 * s + 1, 2 * t + 1))).scale(0.25f));
                }
            }
        }

        // Initialize EWA filter weights if needed
        if (weightLut[0] == 0) {
            for (int i = 0; i < WeightLUTSize; ++i) {
                float alpha = 2;
                float r2 = (float)i / (float)(WeightLUTSize - 1);
                weightLut[i] = (float)(Math.exp(-alpha * r2) - Math.exp(-alpha));
            }
        }
        mipMapMemory.increment((4 * resolution.x * resolution.y * 4*3) / 3);
    }

    public MIPMapSpectrum(Point2i resolution, Spectrum[] data, Spectrum black) {
        this(resolution, data, false, 8, Texture.ImageWrap.Repeat, black);
    }

    public int Width() { return resolution.x; }
    public int Height() { return resolution.y; }
    public int Levels(){ return pyramid.size(); }
    public Spectrum Texel(int level, int s, int t) {
        assert(level < pyramid.size());
        BlockedArray l = pyramid.get(level);
        // Compute texel $(s,t)$ accounting for boundary conditions
        switch (wrapMode) {
            case Repeat:
                s = Pbrt.Mod(s, l.uSize());
                t = Pbrt.Mod(t, l.vSize());
                break;
            case Clamp:
                s = Pbrt.Clamp(s, 0, l.uSize() - 1);
                t = Pbrt.Clamp(t, 0, l.vSize() - 1);
                break;
            case Black: {
                if (s < 0 || s >= l.uSize() || t < 0 || t >= l.vSize())
                    return black;
                break;
            }
        }
        return (Spectrum)l.at(s, t);
    }

    public Spectrum Lookup(Point2f st, float width) {

        nTrilerpLookups.increment();
        // Compute MIPMap level for trilinear filtering
        float level = Levels() - 1 + Pbrt.Log2((float)Math.max(width, 1e-8));

        // Perform trilinear interpolation at appropriate MIPMap level
        if (level < 0)
            return triangle(0, st);
        else if (level >= Levels() - 1)
            return Texel(Levels() - 1, 0, 0);
        else {
            int iLevel = (int)Math.floor(level);
            float delta = level - iLevel;
            return triangle(iLevel, st).lerp(delta, triangle(iLevel + 1, st));
        }
    }
    public Spectrum Lookup(Point2f st) {
        return this.Lookup(st, 0);
    }


    public Spectrum Lookup(Point2f st, Vector2f dst0, Vector2f dst1) {
        if (doTrilinear) {
            float width = Math.max(Math.max(Math.abs(dst0.x), Math.abs(dst0.y)),
                Math.max(Math.abs(dst1.x), Math.abs(dst1.y)));
            return Lookup(st, 2 * width);
        }

        nEWALookups.increment();
        // Compute ellipse minor and major axes
        if (dst0.LengthSquared() < dst1.LengthSquared()) {
            Vector2f temp = dst0;
            dst0 = dst1;
            dst1 = temp;
        }

        float majorLength = dst0.Length();
        float minorLength = dst1.Length();

        // Clamp ellipse eccentricity if too large
        if (minorLength * maxAnisotropy < majorLength && minorLength > 0) {
            float scale = majorLength / (minorLength * maxAnisotropy);
            dst1 = dst1.scale(scale);
            minorLength *= scale;
        }
        if (minorLength == 0) return triangle(0, st);

        // Choose level of detail for EWA lookup and perform EWA filtering
        float lod = Math.max(0, Levels() - 1 + Pbrt.Log2(minorLength));
        int ilod = (int)Math.floor(lod);
        return EWA(ilod, st, dst0, dst1).lerp(lod - ilod, EWA(ilod + 1, st, dst0, dst1));
    }

    private ResampleWeight[] resampleWeights(int oldRes, int newRes) {
        assert(newRes >= oldRes);

        ResampleWeight[] wt = new ResampleWeight[newRes];
        float filterwidth = 2;
        for (int i = 0; i < newRes; ++i) {
            wt[i] = new ResampleWeight();
            // Compute image resampling weights for _i_th texel
            float center = (i + .5f) * oldRes / newRes;
            wt[i].firstTexel = (int)Math.floor((center - filterwidth) + 0.5);
            for (int j = 0; j < 4; ++j) {
                float pos = wt[i].firstTexel + j + .5f;
                wt[i].weight[j] = Texture.Lanczos((pos - center) / filterwidth, 2);
            }

            // Normalize filter weights for texel resampling
            float invSumWts = 1 / (wt[i].weight[0] + wt[i].weight[1] + wt[i].weight[2] + wt[i].weight[3]);
            for (int j = 0; j < 4; ++j) wt[i].weight[j] *= invSumWts;
        }
        return wt;
    }

    private Spectrum triangle(int level, Point2f st) {
        level = Pbrt.Clamp(level, 0, Levels() - 1);
        float s = st.x * pyramid.get(level).uSize() - 0.5f;
        float t = st.y * pyramid.get(level).vSize() - 0.5f;
        int s0 = (int)Math.floor(s), t0 = (int)Math.floor(t);
        float ds = s - s0, dt = t - t0;

        return Texel(level, s0, t0).scale((1 - ds) * (1 - dt)).add(Texel(level, s0, t0 + 1).scale((1 - ds) * dt).add(Texel(level, s0 + 1, t0).scale(ds * (1 - dt)).add(Texel(level, s0 + 1, t0 + 1).scale(ds * dt))));
    }
    
    private Spectrum EWA(int level, Point2f st, Vector2f dst0, Vector2f dst1) {
        if (level >= Levels()) return Texel(Levels() - 1, 0, 0);
        // Convert EWA coordinates to appropriate scale for level
        st.x = st.x * pyramid.get(level).uSize() - 0.5f;
        st.y = st.y * pyramid.get(level).vSize() - 0.5f;
        dst0.x *= pyramid.get(level).uSize();
        dst0.y *= pyramid.get(level).vSize();
        dst1.x *= pyramid.get(level).uSize();
        dst1.y *= pyramid.get(level).vSize();

        // Compute ellipse coefficients to bound EWA filter region
        float A = dst0.y * dst0.y + dst1.y * dst1.y + 1;
        float B = -2 * (dst0.x * dst0.y + dst1.x * dst1.y);
        float C = dst0.x * dst0.x + dst1.x * dst1.x + 1;
        float invF = 1 / (A * C - B * B * 0.25f);
        A *= invF;
        B *= invF;
        C *= invF;

        // Compute the ellipse's $(s,t)$ bounding box in texture space
        float det = -B * B + 4 * A * C;
        float invDet = 1 / det;
        float uSqrt = (float)Math.sqrt(det * C), vSqrt = (float)Math.sqrt(A * det);
        int s0 = (int)Math.ceil(st.x - 2 * invDet * uSqrt);
        int s1 = (int)Math.floor(st.x + 2 * invDet * uSqrt);
        int t0 = (int)Math.ceil(st.y - 2 * invDet * vSqrt);
        int t1 = (int)Math.floor(st.y + 2 * invDet * vSqrt);


        // Scan over ellipse bound and compute quadratic equation
        Spectrum sum = black;
        float sumWts = 0;
        for (int it = t0; it <= t1; ++it) {
            float tt = it - st.y;
            for (int is = s0; is <= s1; ++is) {
                float ss = is - st.x;
                // Compute squared radius and filter texel if inside ellipse
                float r2 = A * ss * ss + B * ss * tt + C * tt * tt;
                if (r2 < 1) {
                    int index = Math.min((int)(r2 * WeightLUTSize), WeightLUTSize - 1);
                    float weight = weightLut[index];
                    sum = sum.add(Texel(level, is, it).scale(weight));
                    sumWts += weight;
                }
            }
        }
        return sum.scale(1 / sumWts);
    }

    private final boolean doTrilinear;
    private final float maxAnisotropy;
    private final Texture.ImageWrap wrapMode;
    private final Spectrum black;
    private Point2i resolution;
    ArrayList<BlockedArray> pyramid;
    private static final int WeightLUTSize = 128;
    private static float[] weightLut = new float[WeightLUTSize];

    public static Stats.Counter nEWALookups = new Stats.Counter("Texture/EWA lookups");
    public static Stats.Counter nTrilerpLookups = new Stats.Counter("Texture/Trilinear lookups");
    public static Stats.MemoryCounter mipMapMemory = new Stats.MemoryCounter("Memory/Texture MIP maps");
}