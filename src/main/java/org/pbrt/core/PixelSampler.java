
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class PixelSampler extends Sampler {

    // PixelSampler Protected Data
    protected float[][] samples1D;
    protected Point2f[][] samples2D;
    protected int current1DDimension = 0, current2DDimension = 0;
    protected RNG rng = new RNG();

    public PixelSampler(int samplesPerPixel, int nSampledDimensions) {
        super(samplesPerPixel);
        samples1D = new float[nSampledDimensions][samplesPerPixel];
        samples2D = new Point2f[nSampledDimensions][samplesPerPixel];
        for (int i = 0; i < nSampledDimensions; ++i) {
            for (int j = 0; j < samplesPerPixel; j++) {
                samples1D[i][j] = 0.0f;
                samples2D[i][j] = new Point2f();
            }
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
        assert (currentPixelSampleIndex < samplesPerPixel);
        if (current1DDimension < samples1D.length)
            return samples1D[current1DDimension++][currentPixelSampleIndex];
        else
            return rng.UniformFloat();
    }

    @Override
    public Point2f Get2D() {
        assert (currentPixelSampleIndex < samplesPerPixel);
        if (current2DDimension < samples2D.length)
            return samples2D[current2DDimension++][currentPixelSampleIndex];
        else
            return new Point2f(rng.UniformFloat(), rng.UniformFloat());
    }

    @Override
    public Sampler Clone(int seed) {
        return new PixelSampler(this.samples1D[0].length, this.samples1D.length);
    }
}
