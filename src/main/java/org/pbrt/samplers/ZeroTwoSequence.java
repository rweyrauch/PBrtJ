
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

public class ZeroTwoSequence extends PixelSampler {

    public ZeroTwoSequence(int samplesPerPixel, int nSampledDimensions) {
        super(Pbrt.RoundUpPow2(samplesPerPixel), nSampledDimensions);
        if (!Pbrt.IsPowerOf2(samplesPerPixel))
            PBrtTLogger.Warning("Pixel samples being rounded up to power of 2 (from %d to %d).", samplesPerPixel, Pbrt.RoundUpPow2(samplesPerPixel));
    }

    public ZeroTwoSequence(ZeroTwoSequence sampler) {
        this(sampler.samplesPerPixel, sampler.samples1D.length);
    }

    public void StartPixel(Point2i p) {
        // Generate 1D and 2D pixel sample components using $(0,2)$-sequence
        for (int i = 0; i < samples1D.length; ++i)
            samples1D[i] = LowDiscrepancy.VanDerCorput(1, samplesPerPixel, samples1D[i], rng);
        for (int i = 0; i < samples2D.length; ++i)
            samples2D[i] = LowDiscrepancy.Sobol2D(1, samplesPerPixel, samples2D[i], rng);

        // Generate 1D and 2D array samples using $(0,2)$-sequence
        for (int i = 0; i < samples1DArraySizes.size(); ++i)
            sampleArray1D.set(i, LowDiscrepancy.VanDerCorput(samples1DArraySizes.get(i), samplesPerPixel, sampleArray1D.get(i), rng));
        for (int i = 0; i < samples2DArraySizes.size(); ++i)
            sampleArray2D.set(i, LowDiscrepancy.Sobol2D(samples2DArraySizes.get(i), samplesPerPixel, sampleArray2D.get(i), rng));
        super.StartPixel(p);
    }

    @Override
    public Sampler Clone(int seed) {
        ZeroTwoSequence lds = new ZeroTwoSequence(this);
        lds.rng.SetSequence(seed);
        return lds;
    }

    public int RoundCount(int count) {
        return Pbrt.RoundUpPow2(count);
    }

    public static Sampler Create(ParamSet paramSet) {
        int nsamp = paramSet.FindOneInt("pixelsamples", 16);
        int sd = paramSet.FindOneInt("dimensions", 4);
        if (Pbrt.options.QuickRender) nsamp = 1;
        return new ZeroTwoSequence(nsamp, sd);
    }
}