
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class TransformedPrimitive extends Primitive {

    // TransformedPrimitive Private Data
    private Primitive primitive;
    private AnimatedTransform PrimitiveToWorld;

    public TransformedPrimitive(Primitive primitive, AnimatedTransform PrimitiveToWorld) {
        this.primitive = primitive;
        this.PrimitiveToWorld = PrimitiveToWorld.clone();
    }

    @Override
    public Bounds3f WorldBound() {
        return PrimitiveToWorld.MotionBounds(primitive.WorldBound());
    }

    @Override
    public SurfaceInteraction Intersect(Ray r) {
        // Compute _ray_ after transformation by _PrimitiveToWorld_
        Transform InterpolatedPrimToWorld = PrimitiveToWorld.Interpolate(r.time);
        Ray ray = Transform.Inverse(InterpolatedPrimToWorld).xform(r);
        SurfaceInteraction isect = primitive.Intersect(ray);
        if (isect == null) return null;
        r.tMax = ray.tMax;
        // Transform instance's intersection data to world space
        if (!InterpolatedPrimToWorld.IsIdentity())
            isect = InterpolatedPrimToWorld.xform(isect);
        assert (Normal3f.Dot(isect.n, isect.shading.n) >= 0);
        return isect;
    }

    @Override
    public boolean IntersectP(Ray r) {
        Transform InterpolatedPrimToWorld = PrimitiveToWorld.Interpolate(r.time);
        Transform InterpolatedWorldToPrim = Transform.Inverse(InterpolatedPrimToWorld);
        return primitive.IntersectP(InterpolatedWorldToPrim.xform(r));
    }

    @Override
    public AreaLight GetAreaLight() {
        return null;
    }

    @Override
    public Material GetMaterial() {
        return null;
    }

    @Override
    public void ComputeScatteringFunctions(SurfaceInteraction isect, Material.TransportMode mode, boolean allowMultipleLobes) {
        Error.Error("TransformedPrimitive::ComputeScatteringFunctions() shouldn't be called");
    }
}