
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.samplers;

import org.pbrt.core.Bounds2i;
import org.pbrt.core.ParamSet;
import org.pbrt.core.Point2f;
import org.pbrt.core.Sampler;

public class SobolSampler extends Sampler {

    public SobolSampler(int samplesPerPixel) {
        super(samplesPerPixel);
    }

    @Override
    public float Get1D() {
        return 0;
    }

    @Override
    public Point2f Get2D() {
        return null;
    }

    @Override
    public Sampler Clone(int seed) {
        return null;
    }

    public static Sampler Create(ParamSet paramSet, Bounds2i bounds2i) {
        return null;
    }
}