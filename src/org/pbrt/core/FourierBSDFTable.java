
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class FourierBSDFTable {

    public float eta;
    public int mMax;
    public int nChannels;
    public int nMu;
    public float[] mu;
    public int[] m;
    public int[] aOffset;
    public float[] a;
    public float[] a0;
    public float[] cdf;
    public float[] recip;

    public static FourierBSDFTable Read(String filename) {
        return null;
    }

    public float[] GetAk(int offsetI, int offsetO) {
        return null;
    }
    public int GetM(int offsetI, int offsetO) {
        return m[offsetO * nMu + offsetI];
    }

    public Interpolation.Weights GetWeights(float cosTheta) {
        return Interpolation.CatmullRomWeights(nMu, mu, cosTheta);
    }
}
