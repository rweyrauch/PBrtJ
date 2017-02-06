
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public abstract class Primitive {

    public abstract Bounds3f WorldBound();
    public abstract SurfaceInteraction Intersect(Ray r);
    public abstract boolean IntersectP(Ray r);
    public abstract AreaLight GetAreaLight();
    public abstract Material GetMaterial();
    public abstract void ComputeScatteringFunctions(SurfaceInteraction isect, Material.TransportMode mode, boolean allowMultipleLobes);
}