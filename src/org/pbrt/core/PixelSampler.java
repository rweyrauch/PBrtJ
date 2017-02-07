
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

public class PixelSampler extends Sampler {

    // PixelSampler Protected Data
    protected ArrayList<Float[]> samples1D = new ArrayList<>();
    protected ArrayList<Point2f[]> samples2D = new ArrayList<>();
    protected int current1DDimension = 0, current2DDimension = 0;
    protected RNG rng;

    public PixelSampler(int samplesPerPixel, int nSampledDimensions) {
        super(samplesPerPixel);
        for (int i = 0; i < nSampledDimensions; ++i) {
            samples1D.add(new Float[samplesPerPixel]);
            samples2D.add(new Point2f[samplesPerPixel]);
        }

    }

    public boolean StartNextSample() {
        current1DDimension = current2DDimension = 0;
        return super.StartNextSample();
    }

    public boolean SetSampleNumber(int sampleNum) {
        current1DDimension = current2DDimension = 0;
        return super.SetSampleNumber(sampleNum);
    }

    @Override
    public float Get1D() {
        //ProfilePhase _(Prof::GetSample);
        assert (currentPixelSampleIndex < samplesPerPixel);
        if (current1DDimension < samples1D.size())
            return samples1D.get(current1DDimension++)[currentPixelSampleIndex];
        else
            return rng.UniformFloat();
    }

    @Override
    public Point2f Get2D() {
        //ProfilePhase _(Prof::GetSample);
        assert (currentPixelSampleIndex < samplesPerPixel);
        if (current2DDimension < samples2D.size())
            return samples2D.get(current2DDimension++)[currentPixelSampleIndex];
        else
            return new Point2f(rng.UniformFloat(), rng.UniformFloat());
    }

    @Override
    public Sampler Clone(int seed) {
        return null;
    }
}
