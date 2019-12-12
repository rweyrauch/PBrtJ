
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public abstract class GlobalSampler extends Sampler {

    // GlobalSampler Private Data
    private int dimension;
    private int intervalSampleIndex;
    private static final int arrayStartDim = 5;
    private int arrayEndDim;

    public GlobalSampler(int samplesPerPixel) {
        super(samplesPerPixel);
    }

    // GlobalSampler Public Methods
    public boolean StartNextSample() {
        dimension = 0;
        intervalSampleIndex = GetIndexForSample(currentPixelSampleIndex + 1);
        return super.StartNextSample();
    }
    public void StartPixel(Point2i p) {
        super.StartPixel(p);
        dimension = 0;
        intervalSampleIndex = GetIndexForSample(0);
        // Compute _arrayEndDim_ for dimensions used for array samples
        arrayEndDim = arrayStartDim + sampleArray1D.size() + 2 * sampleArray2D.size();

        // Compute 1D array samples for _GlobalSampler_
        for (int i = 0; i < samples1DArraySizes.size(); ++i) {
            int nSamples = samples1DArraySizes.get(i) * samplesPerPixel;
            for (int j = 0; j < nSamples; ++j) {
                int index = GetIndexForSample(j);
                sampleArray1D.get(i)[j] = SampleDimension(index, arrayStartDim + i);
            }
        }

        // Compute 2D array samples for _GlobalSampler_
        int dim = arrayStartDim + samples1DArraySizes.size();
        for (int i = 0; i < samples2DArraySizes.size(); ++i) {
            int nSamples = samples2DArraySizes.get(i) * samplesPerPixel;
            for (int j = 0; j < nSamples; ++j) {
                int idx = GetIndexForSample(j);
                sampleArray2D.get(i)[j].x = SampleDimension(idx, dim);
                sampleArray2D.get(i)[j].y = SampleDimension(idx, dim + 1);
            }
            dim += 2;
        }
        assert (arrayEndDim == dim);
    }

    public boolean SetSampleNumber(int sampleNum) {
        dimension = 0;
        intervalSampleIndex = GetIndexForSample(sampleNum);
        return super.SetSampleNumber(sampleNum);
    }
    public float Get1D() {
        if (dimension >= arrayStartDim && dimension < arrayEndDim)
            dimension = arrayEndDim;
        return SampleDimension(intervalSampleIndex, dimension++);

    }
    public Point2f Get2D() {
        if (dimension + 1 >= arrayStartDim && dimension < arrayEndDim)
            dimension = arrayEndDim;
        Point2f p = new Point2f(SampleDimension(intervalSampleIndex, dimension), SampleDimension(intervalSampleIndex, dimension + 1));
        dimension += 2;
        return p;
    }

    public abstract int GetIndexForSample(int sampleNum);
    public abstract float SampleDimension(int index, int dimension);

}