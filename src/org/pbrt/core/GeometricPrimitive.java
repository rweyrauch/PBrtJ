
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class GeometricPrimitive extends Primitive {

    // GeometricPrimitive Private Data
    private Shape shape;
    private Material material;
    private AreaLight areaLight;
    private MediumInterface mediumInterface;

    public GeometricPrimitive(Shape shape, Material material, AreaLight areaLight, MediumInterface mediumInterface) {
        super();
        this.shape = shape;
        this.material = material;
        this.areaLight = areaLight;
        this.mediumInterface = mediumInterface;
    }

    @Override
    public Bounds3f WorldBound() {
        return shape.WorldBound();
    }

    @Override
    public SurfaceInteraction Intersect(Ray r) {
        Shape.HitResult hres = shape.Intersect(r, true);
        if (hres == null) return null;
        r.tMax = hres.tHit;
        hres.isect.primitive = this;
        assert (Normal3f.Dot(hres.isect.n, hres.isect.shading.n) >= 0);
        // Initialize _SurfaceInteraction::mediumInterface_ after _Shape_
        // intersection
        if (mediumInterface.IsMediumTransition())
            hres.isect.mediumInterface = mediumInterface;
        else
            hres.isect.mediumInterface = new MediumInterface(r.medium);
        return hres.isect;
    }

    @Override
    public boolean IntersectP(Ray r) {
        return shape.IntersectP(r, true);
    }

    @Override
    public AreaLight GetAreaLight() {
        return areaLight;
    }

    @Override
    public Material GetMaterial() {
        return material;
    }

    @Override
    public void ComputeScatteringFunctions(SurfaceInteraction isect, Material.TransportMode mode, boolean allowMultipleLobes) {
        if (material != null)
            material.ComputeScatteringFunctions(isect, mode, allowMultipleLobes);
        assert(Normal3f.Dot(isect.n, isect.shading.n) >= 0);
    }
}
