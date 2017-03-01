
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public abstract class Integrator {

    public abstract void Render(Scene scene);

    public static Distribution1D ComputeLightPowerDistribution(Scene scene) {
        if (scene.lights.isEmpty()) return null;
        float[] lightPower = new float[scene.lights.size()];
        int i = 0;
        for (Light light : scene.lights)
            lightPower[i++] = light.Power().y();
        return new Distribution1D(lightPower);
    }

}