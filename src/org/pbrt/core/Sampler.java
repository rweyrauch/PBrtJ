
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

public abstract class Sampler {

    // Sampler Public Data
    public int samplesPerPixel;

    // Sampler Protected Data
    protected Point2i currentPixel;
    protected int currentPixelSampleIndex;
    protected ArrayList<Integer> samples1DArraySizes, samples2DArraySizes;
    protected ArrayList<Float[]> sampleArray1D;
    protected ArrayList<Point2f[]> sampleArray2D;

    // Sampler Private Data
    private int array1DOffset, array2DOffset;

    // Sampler Interface
    public Sampler(int samplesPerPixel) {
        this.samplesPerPixel = samplesPerPixel;
    }

    public void StartPixel(Point2i p) {
        currentPixel = p;
        currentPixelSampleIndex = 0;
        // Reset array offsets for next pixel sample
        array1DOffset = array2DOffset = 0;
    }
    public abstract float Get1D();
    public abstract Point2f Get2D();
    public Camera.CameraSample GetCameraSample(Point2i pRaster) {
        Camera.CameraSample cs = new Camera.CameraSample();
        Point2f pRasterf = new Point2f(pRaster);
        cs.pFilm = pRasterf.add(Get2D());
        cs.time = Get1D();
        cs.pLens = Get2D();
        return cs;
    }
    public void Request1DArray(int n) {
        assert (RoundCount(n) == n);
        samples1DArraySizes.add(n);
        sampleArray1D.add(new Float[n * samplesPerPixel]);
    }
    public void Request2DArray(int n) {
        assert (RoundCount(n) == n);
        samples2DArraySizes.add(n);
        sampleArray2D.add(new Point2f[n * samplesPerPixel]);
    }
    public int RoundCount(int n) { return n; }

    float[] Get1DArray(int n) {
        if (array1DOffset == sampleArray1D.size()) return null;
        assert (samples1DArraySizes.get(array1DOffset) == n);
        assert (currentPixelSampleIndex < samplesPerPixel);
        return null; //sampleArray1D.get(array1DOffset++)[currentPixelSampleIndex * n];
    }
    public Point2f[] Get2DArray(int n) {
        if (array2DOffset == sampleArray2D.size()) return null;
        assert (samples2DArraySizes.get(array2DOffset) == n);
        assert (currentPixelSampleIndex < samplesPerPixel);
        return null; //sampleArray2D.get(array2DOffset++)[currentPixelSampleIndex * n];
    }
    public boolean StartNextSample() {
        // Reset array offsets for next pixel sample
        array1DOffset = array2DOffset = 0;
        return ++currentPixelSampleIndex < samplesPerPixel;
    }
    public abstract Sampler Clone(int seed);
    public boolean SetSampleNumber(int sampleNum) {
        // Reset array offsets for next pixel sample
        array1DOffset = array2DOffset = 0;
        currentPixelSampleIndex = sampleNum;
        return currentPixelSampleIndex < samplesPerPixel;
    }
    public int CurrentSampleNumber() { return currentPixelSampleIndex; }

}