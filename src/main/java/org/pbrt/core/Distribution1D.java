/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class Distribution1D {

    // Distribution1D Public Data
    public float[] func, cdf;
    public float funcInt;

    public Distribution1D(float[] f) {
        func = f.clone();
        cdf = new float[f.length + 1];

        // Compute integral of step function at $x_i$
        cdf[0] = 0;
        for (int i = 1; i < cdf.length; ++i) cdf[i] = cdf[i - 1] + func[i - 1] / f.length;

        // Transform step function integral into CDF
        funcInt = cdf[f.length];
        if (funcInt == 0) {
            for (int i = 1; i < cdf.length; ++i) cdf[i] = (float)i / (float)f.length;
        }
        else {
            for (int i = 1; i < cdf.length; ++i) cdf[i] /= funcInt;
        }
    }

    public int Count() { return func.length; }

    public static class ContSample {
        public float sample;
        public float pdf;
        public int offset;
    }

    public ContSample SampleContinuous(float u) {
        ContSample dsamp = new ContSample();
        dsamp.offset = Pbrt.FindInterval(cdf.length, index -> cdf[index] < u);

        // Compute offset along CDF segment
        float du = u - cdf[dsamp.offset];
        if ((cdf[dsamp.offset + 1] - cdf[dsamp.offset]) > 0) {
            assert (cdf[dsamp.offset + 1] > cdf[dsamp.offset]);
            du /= (cdf[dsamp.offset + 1] - cdf[dsamp.offset]);
        }
        assert(!Float.isNaN(du));

        // Compute PDF for sampled offset
        dsamp.pdf = (funcInt > 0) ? func[dsamp.offset] / funcInt : 0;

        // Return $x\in{}[0,1)$ corresponding to sample
        dsamp.sample = (dsamp.offset + du) / Count();
        return dsamp;
    }

    public static class DiscreteSample {
        public float pdf;
        public float uRemapped;
        public int offset;
    }

    public DiscreteSample SampleDiscrete(float u) {
        DiscreteSample dsamp = new DiscreteSample();
        // Find surrounding CDF segments and _offset_
        dsamp.offset = Pbrt.FindInterval(cdf.length, index -> cdf[index] <= u);
        dsamp.pdf = (funcInt > 0) ? func[dsamp.offset] / (funcInt * Count()) : 0;
        dsamp.uRemapped = (u - cdf[dsamp.offset]) / (cdf[dsamp.offset + 1] - cdf[dsamp.offset]);
        assert(dsamp.uRemapped >= 0 && dsamp.uRemapped <= 1);
        return dsamp;
    }

    public float DiscretePDF(int index) {
        assert (index >= 0 && index < Count());
        return func[index] / (funcInt * Count());
    }

}