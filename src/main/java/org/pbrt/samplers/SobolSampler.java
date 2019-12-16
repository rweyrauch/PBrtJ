
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
import org.pbrt.core.PBrtTLogger;

public class SobolSampler extends GlobalSampler {

    public SobolSampler(int samplesPerPixel, Bounds2i sampleBounds) {
        super(Pbrt.RoundUpPow2(samplesPerPixel));
        this.sampleBounds = sampleBounds;

        if (!Pbrt.IsPowerOf2(samplesPerPixel))
            PBrtTLogger.Warning("Non power-of-two sample count rounded up to % for SobolSampler.", samplesPerPixel);
        resolution = Pbrt.RoundUpPow2(Math.max(sampleBounds.Diagonal().x, sampleBounds.Diagonal().y));
        log2Resolution = Pbrt.Log2Int(resolution);
        if (resolution > 0) assert((1 << log2Resolution) == resolution);
    }

    public SobolSampler(SobolSampler sampler) {
        super(Pbrt.RoundUpPow2(sampler.samplesPerPixel));
        this.sampleBounds = sampler.sampleBounds;
        this.resolution = sampler.resolution;
        this.log2Resolution = sampler.log2Resolution;
    }

    @Override
    public int GetIndexForSample(int sampleNum) {
        return (int)LowDiscrepancy.SobolIntervalToIndex(log2Resolution, sampleNum, new Point2i(currentPixel.subtract(sampleBounds.pMin)));
    }

    @Override
    public float SampleDimension(int index, int dim) {
        if (dim >= SobolMatrices.NumSobolDimensions)
            PBrtTLogger.Error("SobolSampler can only sample up to %d dimensions! Exiting.", SobolMatrices.NumSobolDimensions);
        float s = LowDiscrepancy.SobolSampleFloat(index, dim, 0);
        // Remap Sobol$'$ dimensions used for pixel samples
        if (dim == 0 || dim == 1) {
            s = s * resolution + sampleBounds.pMin.at(dim);
            s = Pbrt.Clamp(s - currentPixel.at(dim), 0, Pbrt.OneMinusEpsilon);
        }
        return s;
    }

    @Override
    public Sampler Clone(int seed) {
        return new SobolSampler(this);
    }

    public static Sampler Create(ParamSet paramSet, Bounds2i sampleBounds) {
        int nsamp = paramSet.FindOneInt("pixelsamples", 16);
        if (Pbrt.options.QuickRender) nsamp = 1;
        return new SobolSampler(nsamp, sampleBounds);
    }

    private final Bounds2i sampleBounds;
    private int resolution, log2Resolution;
}