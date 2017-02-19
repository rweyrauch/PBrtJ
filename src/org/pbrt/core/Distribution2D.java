/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class Distribution2D {

    private Distribution1D[] pConditionalV;
    private Distribution1D pMarginal;

    public Distribution2D(float[] data, int nu, int nv) {
        this.pConditionalV = new Distribution1D[nv];
        for (int v = 0; v < nv; ++v) {
            // Compute conditional sampling distribution for $\tilde{v}$
            float[] func = new float[nu];
            for (int i = 0; i < nu; i++) func[i] = data[v * nu + i];
            pConditionalV[v] = new Distribution1D(func);
        }
        // Compute marginal sampling distribution $p[\tilde{v}]$
        float[] marginalFunc = new float[nv];
        for (int v = 0; v < nv; ++v)
            marginalFunc[v] = pConditionalV[v].funcInt;
        pMarginal = new Distribution1D(marginalFunc);
    }

    public static class ContSample {
        public Point2f sample;
        public float pdf;
    }
    public ContSample SampleContinuous(Point2f u) {
        Distribution1D.ContSample d1 = pMarginal.SampleContinuous(u.y);
        Distribution1D.ContSample d0 = pConditionalV[d1.offset].SampleContinuous(u.x);
        ContSample csamp = new ContSample();
        csamp.pdf = d0.pdf * d1.pdf;
        csamp.sample = new Point2f(d0.sample, d1.sample);
        return csamp;
    }
    public float Pdf(Point2f p) {
        int iu = Pbrt.Clamp((int)(p.x * pConditionalV[0].Count()), 0, pConditionalV[0].Count() - 1);
        int iv = Pbrt.Clamp((int)(p.y * pMarginal.Count()), 0, pMarginal.Count() - 1);
        return pConditionalV[iv].func[iu] / pMarginal.funcInt;
    }

}