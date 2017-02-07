
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public abstract class Aggregate extends Primitive {

    @Override
    public Bounds3f WorldBound() {
        return null;
    }

    @Override
    public AreaLight GetAreaLight() {
        Error.Error("Aggregate::GetAreaLight() method called; should have gone to GeometricPrimitive");
        return null;
    }

    @Override
    public Material GetMaterial() {
        Error.Error("Aggregate::GetMaterial() method called; should have gone to GeometricPrimitive");
        return null;
    }

    @Override
    public void ComputeScatteringFunctions(SurfaceInteraction isect, Material.TransportMode mode, boolean allowMultipleLobes) {
        Error.Error("Aggregate::ComputeScatteringFunctions() method called; should have gone to GeometricPrimitive");

    }
}

