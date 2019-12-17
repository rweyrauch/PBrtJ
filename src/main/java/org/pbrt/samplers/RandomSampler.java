
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

public class RandomSampler extends Sampler {

    public RandomSampler(int samplesPerPixel, int seed) {
        super(samplesPerPixel);
        this.rng = new RNG(seed);
    }
    public RandomSampler(int samplesPerPixel) {
        this(samplesPerPixel, 0);
    }

    public void StartPixel(Point2i p) {
        for (float[] aSampleArray1D : sampleArray1D)
            for (int j = 0; j < aSampleArray1D.length; ++j)
                aSampleArray1D[j] = rng.UniformFloat();

        for (Point2f[] aSampleArray2D : sampleArray2D)
            for (int j = 0; j < aSampleArray2D.length; ++j)
                aSampleArray2D[j] = new Point2f(rng.UniformFloat(), rng.UniformFloat());
        super.StartPixel(p);
    }

    @Override
    public float Get1D() {
        assert (currentPixelSampleIndex < samplesPerPixel);
        return rng.UniformFloat();
    }

    @Override
    public Point2f Get2D() {
        assert (currentPixelSampleIndex < samplesPerPixel);
        return new Point2f(rng.UniformFloat(), rng.UniformFloat());
    }

    @Override
    public Sampler Clone(int seed) {
        RandomSampler rs = new RandomSampler(this.samplesPerPixel);
        rs.rng.SetSequence(seed);
        return rs;
    }

    public static Sampler Create(ParamSet paramSet) {
        int ns = paramSet.FindOneInt("pixelsamples", 4);
        return new RandomSampler(ns);
    }

    private RNG rng;
}