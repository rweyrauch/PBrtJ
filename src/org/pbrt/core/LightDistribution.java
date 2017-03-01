
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

import java.util.Objects;

public abstract class LightDistribution {
    public abstract Distribution1D Lookup(Point3f p);

    public static LightDistribution CreateLightSampleDistribution(String name, Scene scene) {
        if (Objects.equals(name, "uniform") || scene.lights.size() == 1)
            return new UniformLightDistribution(scene);
        else if (Objects.equals(name, "power"))
            return new PowerLightDistribution(scene);
        else if (Objects.equals(name, "spatial"))
            return new SpatialLightDistribution(scene, 64);
        else {
            Error.Error("Light sample distribution type \"%s\" unknown. Using \"spatial\".", name);
            return new SpatialLightDistribution(scene, 64);
        }
    }
}