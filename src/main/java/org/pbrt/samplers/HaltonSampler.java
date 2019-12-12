
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.samplers;

import org.pbrt.core.*;
import org.pbrt.core.Error;

public class HaltonSampler extends GlobalSampler {

    private static final int kMaxResolution = 128;

    public HaltonSampler(int samplesPerPixel, Bounds2i sampleBounds, boolean sampleAtCenter) {
        super(samplesPerPixel);
        this.sampleAtPixelCenter = sampleAtCenter;

        // Generate random digit permutations for Halton sampler
        if (radicalInversePermutations.length == 0) {
            RNG rng = new RNG();
            radicalInversePermutations = LowDiscrepancy.ComputeRadicalInversePermutations(rng);
        }

        // Find radical inverse base scales and exponents that cover sampling area
        Vector2i res = sampleBounds.pMax.subtract(sampleBounds.pMin);
        for (int i = 0; i < 2; ++i) {
            int base = (i == 0) ? 2 : 3;
            int scale = 1, exp = 0;
            while (scale < Math.min(res.at(i), kMaxResolution)) {
                scale *= base;
                ++exp;
            }
            if (i == 0) {
                baseScales.x = scale;
                baseExponents.x = exp;
            }
            else {
                baseScales.y = scale;
                baseExponents.y = exp;
            }
        }

        // Compute stride in samples for visiting each pixel area
        sampleStride = baseScales.x * baseScales.y;

        // Compute multiplicative inverses for _baseScales_
        multInverse[0] = (int)multiplicativeInverse(baseScales.y, baseScales.x);
        multInverse[1] = (int)multiplicativeInverse(baseScales.x, baseScales.y);
    }
    public HaltonSampler(HaltonSampler sampler) {
        super(sampler.samplesPerPixel);
        this.sampleAtPixelCenter = sampler.sampleAtPixelCenter;
        this.baseScales = sampler.baseScales;
        this.baseExponents = sampler.baseExponents;
        this.sampleStride = sampler.sampleStride;
        this.multInverse[0] = sampler.multInverse[0];
        this.multInverse[1] = sampler.multInverse[1];
    }
    public HaltonSampler(int samplesPerPixel, Bounds2i sampleBounds) {
        this(samplesPerPixel, sampleBounds, false);
    }

    @Override
    public int GetIndexForSample(int sampleNum) {
        if (currentPixel.notEqual(pixelForOffset)) {
            // Compute Halton sample offset for _currentPixel_
            offsetForCurrentPixel = 0;
            if (sampleStride > 1) {
                Point2i pm = new Point2i(Pbrt.Mod(currentPixel.x, kMaxResolution),
                                        Pbrt.Mod(currentPixel.y, kMaxResolution));
                for (int i = 0; i < 2; ++i) {
                    long dimOffset = (i == 0)
                        ? LowDiscrepancy.InverseRadicalInverse(2, (long)pm.at(i), baseExponents.at(i))
                        : LowDiscrepancy.InverseRadicalInverse(3, (long)pm.at(i), baseExponents.at(i));
                    offsetForCurrentPixel +=
                            dimOffset * (sampleStride / baseScales.at(i)) * multInverse[i];
                }
                offsetForCurrentPixel %= sampleStride;
            }
            pixelForOffset = currentPixel;
        }
        return offsetForCurrentPixel + sampleNum * sampleStride;
    }

    @Override
    public float SampleDimension(int index, int dim) {
        if (sampleAtPixelCenter && (dim == 0 || dim == 1)) return 0.5f;
        if (dim == 0)
            return LowDiscrepancy.RadicalInverse(dim, index >> baseExponents.x);
        else if (dim == 1)
            return LowDiscrepancy.RadicalInverse(dim, index / baseScales.y);
        else
            return LowDiscrepancy.ScrambledRadicalInverse(dim, index, radicalInversePermutations, PermutationForDimension(dim));
    }

    @Override
    public Sampler Clone(int seed) {
        return new HaltonSampler(this);
    }

    public static Sampler Create(ParamSet paramSet, Bounds2i sampleBounds) {
        int nsamp = paramSet.FindOneInt("pixelsamples", 16);
        if (Pbrt.options.QuickRender) nsamp = 1;
        boolean sampleAtCenter = paramSet.FindOneBoolean("samplepixelcenter", false);
        return new HaltonSampler(nsamp, sampleBounds, sampleAtCenter);
    }


    private static Short[] radicalInversePermutations = new Short[0];
    private Point2i baseScales = new Point2i(), baseExponents = new Point2i();
    private int sampleStride;
    private int[] multInverse = { 0, 0 };
    private Point2i pixelForOffset = new Point2i(Integer.MAX_VALUE, Integer.MAX_VALUE);
    private int offsetForCurrentPixel;
    // Added after book publication: force all image samples to be at the
    // center of the pixel area.
    private boolean sampleAtPixelCenter;

    // HaltonSampler Private Methods
    private int PermutationForDimension(int dim) {
        if (dim >= LowDiscrepancy.PrimeTableSize)
            Error.Error("HaltonSampler can only sample %d dimensions.", LowDiscrepancy.PrimeTableSize);
        return LowDiscrepancy.PrimeSums[dim];
    }

    private static class ExtGCD {
        long x, y;
    }

    private static long multiplicativeInverse(long a, long n) {
        ExtGCD xy = extendedGCD(a, n);
        return Pbrt.Mod(xy.x, n);
    }

    private static ExtGCD extendedGCD(long a, long b) {
        ExtGCD xy = new ExtGCD();
        if (b == 0) {
            xy.x = 1;
            xy.y = 0;
            return xy;
        }
        long d = a / b;
        ExtGCD xyp = extendedGCD(b, a % b);
        xy.x = xyp.y;
        xy.y = xyp.x - (d * xyp.y);
        return xy;
    }

}