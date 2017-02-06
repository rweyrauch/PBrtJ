/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public abstract class PhaseFunction {
    public class PhaseSample {
        float phase;
        Vector3f wi;
    }

    // PhaseFunction Interface
    public abstract float p(Vector3f wo, Vector3f wi);
    public abstract PhaseSample Sample_p(Vector3f wo, Point2f u);
};

