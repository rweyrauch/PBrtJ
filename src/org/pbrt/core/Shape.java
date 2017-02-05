
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public abstract class Shape {

    public class HitResult {
        public boolean hit;
        public float tDist;
        public Interaction interaction;
    }
    public class SampleResult {
        public Interaction interaction;
        public float pdf;
    }

    // Shape Public Data
    public Transform ObjectToWorld, WorldToObject;
    public boolean reverseOrientation;
    public boolean transformSwapsHandedness;

    public Shape(Transform ObjectToWorld, Transform WorldToObject,
          boolean reverseOrientation) {
        this.ObjectToWorld = ObjectToWorld;
        this.WorldToObject = WorldToObject;
        this.reverseOrientation = reverseOrientation;
        this.transformSwapsHandedness = ObjectToWorld.SwapsHandedness();
    }

    abstract Bounds3<Float> ObjectBound();
    public Bounds3<Float> WorldBound() { return ObjectToWorld.Transform(ObjectBound()); }
    abstract HitResult Intersect(Ray ray, boolean testAlphaTexture);
    public HitResult IntersectP(Ray ray, boolean testAlphaTexture) {
        return Intersect(ray, testAlphaTexture);
    }

    abstract float Area();
    // Sample a point on the surface of the shape and return the PDF with
    // respect to area on the surface.
    abstract SampleResult Sample(Point2f u);
    public float Pdf(Interaction ref) { return 1.0f / Area(); }

    // Sample a point on the shape given a reference point |ref| and
    // return the PDF with respect to solid angle from |ref|.
    public SampleResult Sample(Interaction ref, Point2f u) {
        SampleResult result = Sample(u);
        Vector3f wi = result.interaction.p - ref.p;
        if (wi.LengthSquared() == 0) {
            result.pdf = 0;
        }
        else {
            wi = Normalize(wi);
            // Convert from area measure, as returned by the Sample() call
            // above, to solid angle measure.
            result.pdf *= DistanceSquared(ref.p, result.interaction.p) / AbsDot(result.interaction.n, -wi);
            if (Float.isInfinite(result.pdf)) result.pdf = 0.0f;
        }
        return result;
    }
    public float Pdf(Interaction ref,  Vector3f wi) {
        // Intersect sample ray with area light geometry
        Ray ray = ref.SpawnRay(wi);
        // Ignore any alpha textures used for trimming the shape when performing
        // this intersection. Hack for the "San Miguel" scene, where this is used
        // to make an invisible area light.
        HitResult hit = Intersect(ray, false);
        if (!hit.hit)
            return 0.0f;

        // Convert light sample weight to solid angle measure
        float pdf = DistanceSquared(ref.p, hit.interaction.p) / (AbsDot(hit.interaction.n, -wi) * Area());
        if (Float.isInfinite(pdf)) pdf = 0.0f;
        return pdf;
    }

    // Returns the solid angle subtended by the shape w.r.t. the reference
    // point p, given in world space. Some shapes compute this value in
    // closed-form, while the default implementation uses Monte Carlo
    // integration; the nSamples parameter determines how many samples are
    // used in this case.
    public float SolidAngle(Point3f p, int nSamples) {
        Interaction ref(p, Normal3f(), Vector3f(), Vector3f(0, 0, 1), 0, MediumInterface{});
        double solidAngle = 0;
        for (int i = 0; i < nSamples; ++i) {
            Point2f u{RadicalInverse(0, i), RadicalInverse(1, i)};
            SampleResult res = Sample(ref, u);
            if (res.pdf > 0 && !IntersectP(Ray(p, res.interaction.p - p, .999f))) {
                solidAngle += 1 / res.pdf;
            }
        }
        return (float)solidAngle / nSamples;
    }

}