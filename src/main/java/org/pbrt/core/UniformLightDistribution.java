
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class UniformLightDistribution extends LightDistribution {

    public UniformLightDistribution(Scene scene) {
        float[] prob = new float[scene.lights.size()];
        for (int i = 0; i < prob.length; i++) prob[i] = 1;
        distrib = new Distribution1D(prob);
    }

    @Override
    public Distribution1D Lookup(Point3f p) {
        return distrib;
    }

    private Distribution1D distrib;
}