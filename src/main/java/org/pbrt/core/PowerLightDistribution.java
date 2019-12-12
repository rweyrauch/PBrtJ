
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class PowerLightDistribution extends LightDistribution {

    public PowerLightDistribution(Scene scene) {
        this.distrib = Integrator.ComputeLightPowerDistribution(scene);
    }

    @Override
    public Distribution1D Lookup(Point3f p) {
        return distrib;
    }

    private Distribution1D distrib;
}