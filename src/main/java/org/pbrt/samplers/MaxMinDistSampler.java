
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

public class MaxMinDistSampler extends PixelSampler {

    private static int calcSPP(int spp) {
        int Cindex = Pbrt.Log2Int(spp);
        if (Cindex >= LowDiscrepancy.CMaxMinDist.length) {
            PBrtTLogger.Warning("No more than %d samples per pixel are supported with MaxMinDistSampler. Rounding down.",
                    (1 << LowDiscrepancy.CMaxMinDist.length) -1);
            spp = (1 << LowDiscrepancy.CMaxMinDist.length) - 1;
        }
        if (!Pbrt.IsPowerOf2(spp)) {
            spp = Pbrt.RoundUpPow2(spp);
            PBrtTLogger.Warning("Non power-of-two sample count rounded up to %d for MaxMinDistSampler.", spp);
        }
        return spp;
    }
    public MaxMinDistSampler(int samplesPerPixel, int nSampledDimensions) {
        super(calcSPP(samplesPerPixel), nSampledDimensions);

        int Cindex = Pbrt.Log2Int(samplesPerPixel);
        assert (Cindex >= 0 && Cindex < LowDiscrepancy.CMaxMinDist.length);
        CPixel = LowDiscrepancy.CMaxMinDist[Cindex];
    }
    public MaxMinDistSampler(MaxMinDistSampler mms) {
        this(mms.samplesPerPixel, mms.samples1D.length);
    }

    @Override
    public Sampler Clone(int seed) {
        MaxMinDistSampler mmds = new MaxMinDistSampler(this);
        mmds.rng.SetSequence(seed);
        return mmds;
    }

    @Override
    public void StartPixel(Point2i p) {
        float invSPP = 1.0f / samplesPerPixel;
        for (int i = 0; i < samplesPerPixel; ++i)
            samples2D[0][i] = new Point2f(i * invSPP, LowDiscrepancy.SampleGeneratorMatrix(CPixel, i, 0));
        Sampling.Shuffle(samples2D[0], 0, samplesPerPixel, 1, rng);
        // Generate remaining samples for _MaxMinDistSampler_
        for (float[] aSamples1D : samples1D) LowDiscrepancy.VanDerCorput(1, samplesPerPixel, aSamples1D, rng);

        for (int i = 1; i < samples2D.length; ++i)
            LowDiscrepancy.Sobol2D(1, samplesPerPixel, samples2D[i], rng);

        for (int i = 0; i < samples1DArraySizes.size(); ++i) {
            int count = samples1DArraySizes.get(i);
            LowDiscrepancy.VanDerCorput(count, samplesPerPixel, sampleArray1D.get(i), rng);
        }

        for (int i = 0; i < samples2DArraySizes.size(); ++i) {
            int count = samples2DArraySizes.get(i);
            LowDiscrepancy.Sobol2D(count, samplesPerPixel, sampleArray2D.get(i), rng);
        }
        super.StartPixel(p);
    }

    public static Sampler Create(ParamSet paramSet) {
        int nsamp = paramSet.FindOneInt("pixelsamples", 16);
        int sd = paramSet.FindOneInt("dimensions", 4);
        if (Pbrt.options.QuickRender) nsamp = 1;
        return new MaxMinDistSampler(nsamp, sd);
    }

    private int[] CPixel;
}