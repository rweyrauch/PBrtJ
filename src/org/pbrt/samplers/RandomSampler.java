
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
        //Stats.ProfilePhase pp = new Stats.ProfilePhase(Stats.Prof.StartPixel);
        for (int i = 0; i < sampleArray1D.size(); ++i)
            for (int j = 0; j < sampleArray1D.get(i).length; ++j)
                sampleArray1D.get(i)[j] = rng.UniformFloat();

        for (int i = 0; i < sampleArray2D.size(); ++i)
            for (int j = 0; j < sampleArray2D.get(i).length; ++j)
                sampleArray2D.get(i)[j] = new Point2f(rng.UniformFloat(), rng.UniformFloat());
        super.StartPixel(p);
    }

    @Override
    public float Get1D() {
        //Stats.ProfilePhase pp = new Stats.ProfilePhase(Stats.Prof.GetSample);
        assert (currentPixelSampleIndex < samplesPerPixel);
        return rng.UniformFloat();
    }

    @Override
    public Point2f Get2D() {
        //Stats.ProfilePhase pp = new Stats.ProfilePhase(Stats.Prof.GetSample);
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