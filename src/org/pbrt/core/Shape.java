
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
        public float tHit;
        public SurfaceInteraction isect;
    }
    public class SampleResult {
        public SurfaceInteraction isect;
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

    public abstract Bounds3f ObjectBound();
    public Bounds3f WorldBound() { return ObjectToWorld.xform(ObjectBound()); }
    public abstract HitResult Intersect(Ray ray, boolean testAlphaTexture);
    public boolean IntersectP(Ray ray, boolean testAlphaTexture) {
        return (Intersect(ray, testAlphaTexture) != null);
    }

    public abstract float Area();
    // Sample a point on the surface of the shape and return the PDF with
    // respect to area on the surface.
    public abstract SampleResult Sample(Point2f u);
    public float Pdf(Interaction ref) { return 1 / Area(); }

    // Sample a point on the shape given a reference point |ref| and
    // return the PDF with respect to solid angle from |ref|.
    public SampleResult Sample(Interaction ref, Point2f u) {
        SampleResult result = Sample(u);
        Vector3f wi = result.isect.p.subtract(ref.p);
        if (wi.LengthSquared() == 0) {
            result.pdf = 0;
        }
        else {
            wi = Vector3f.Normalize(wi);
            // Convert from area measure, as returned by the Sample() call
            // above, to solid angle measure.
            result.pdf *= Point3f.DistanceSquared(ref.p, result.isect.p) / Normal3f.AbsDot(result.isect.n, wi.negate());
            if (Float.isInfinite(result.pdf)) result.pdf = 0;
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
        if (hit == null)
            return 0;

        // Convert light sample weight to solid angle measure
        float pdf = Point3f.DistanceSquared(ref.p, hit.isect.p) / (Normal3f.AbsDot(hit.isect.n, wi.negate()) * Area());
        if (Float.isInfinite(pdf)) pdf = 0;
        return pdf;
    }

    // Returns the solid angle subtended by the shape w.r.t. the reference
    // point p, given in world space. Some shapes compute this value in
    // closed-form, while the default implementation uses Monte Carlo
    // integration; the nSamples parameter determines how many samples are
    // used in this case.
    public float SolidAngle(Point3f p, int nSamples) {
        Interaction ref = new Interaction(p, new Normal3f(), new Vector3f(), new Vector3f(0, 0, 1), 0, new MediumInterface());
        double solidAngle = 0;
        for (int i = 0; i < nSamples; ++i) {
            Point2f u = new Point2f(LowDiscrepancy.RadicalInverse(0, i), LowDiscrepancy.RadicalInverse(1, i));
            SampleResult res = Sample(ref, u);
            if (res.pdf > 0 && !IntersectP(new Ray(p, res.isect.p.subtract(p), 0.999f, 0.0f, null), true)) {
                solidAngle += 1 / res.pdf;
            }
        }
        return (float)solidAngle / nSamples;
    }

}