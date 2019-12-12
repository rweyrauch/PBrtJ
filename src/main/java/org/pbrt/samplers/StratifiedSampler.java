
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

public class StratifiedSampler extends PixelSampler {

    public StratifiedSampler(int xPixelSamples, int yPixelSamples, boolean jitterSamples, int nSampledDimensions) {
        super(xPixelSamples*yPixelSamples, nSampledDimensions);
        this.xPixelSamples = xPixelSamples;
        this.yPixelSamples = yPixelSamples;
        this.jitterSamples = jitterSamples;
    }

    public StratifiedSampler(StratifiedSampler sampler) {
        this(sampler.xPixelSamples, sampler.yPixelSamples, sampler.jitterSamples, sampler.samples1D.size());
    }

    public void StartPixel(Point2i p) {
        // Generate single stratified samples for the pixel
        for (int i = 0; i < samples1D.size(); ++i) {
            samples1D.set(i, Sampling.StratifiedSample1D(samples1D.get(i), xPixelSamples * yPixelSamples, rng, jitterSamples));
            samples1D.set(i, Sampling.Shuffle(samples1D.get(i), 0, xPixelSamples * yPixelSamples, 1, rng));
        }
        for (int i = 0; i < samples2D.size(); ++i) {
            samples2D.set(i, Sampling.StratifiedSample2D(samples2D.get(i), xPixelSamples, yPixelSamples, rng, jitterSamples));
            samples2D.set(i, Sampling.Shuffle(samples2D.get(i), 0, xPixelSamples * yPixelSamples, 1, rng));
        }

        // Generate arrays of stratified samples for the pixel
        for (int i = 0; i < samples1DArraySizes.size(); ++i) {
            for (int j = 0; j < samplesPerPixel; ++j) {
                int count = samples1DArraySizes.get(i);
                sampleArray1D.set(i, Sampling.StratifiedSample1D(sampleArray1D.get(i), j * count, (j+1)*count, rng, jitterSamples));
                sampleArray1D.set(i, Sampling.Shuffle(sampleArray1D.get(i), j * count, (j+1)*count, 1, rng));
            }
        }
        for (int i = 0; i < samples2DArraySizes.size(); ++i) {
            for (int j = 0; j < samplesPerPixel; ++j) {
                int count = samples2DArraySizes.get(i);
                sampleArray2D.set(i, Sampling.LatinHypercube(sampleArray2D.get(i), j * count, (j + 1) * count, rng));
            }
        }
        super.StartPixel(p);
    }

    @Override
    public Sampler Clone(int seed) {
        StratifiedSampler ss = new StratifiedSampler(this);
        ss.rng.SetSequence(seed);
        return ss;
    }

    public static Sampler Create(ParamSet paramSet) {
        boolean jitter = paramSet.FindOneBoolean("jitter", true);
        int xsamp = paramSet.FindOneInt("xsamples", 4);
        int ysamp = paramSet.FindOneInt("ysamples", 4);
        int sd = paramSet.FindOneInt("dimensions", 4);
        if (Pbrt.options.QuickRender) xsamp = ysamp = 1;
        return new StratifiedSampler(xsamp, ysamp, jitter, sd);
    }

    private final int xPixelSamples, yPixelSamples;
    private final boolean jitterSamples;
}