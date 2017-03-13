
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.shapes;

import org.pbrt.core.*;

public class Disk extends Shape {

    public Disk(Transform ObjectToWorld, Transform WorldToObject, boolean reverseOrientation, float height, float radius, float innerRadius, float phiMax) {
        super(ObjectToWorld, WorldToObject, reverseOrientation);
        this.height = height;
        this.radius = radius;
        this.innerRadius = innerRadius;
        this.phiMax = (float)Math.toRadians(Pbrt.Clamp(phiMax, 0, 360));
    }

    @Override
    public Bounds3f ObjectBound() {
        return new Bounds3f(new Point3f(-radius, -radius, height),
                new Point3f(radius, radius, height));
    }

    @Override
    public HitResult Intersect(Ray r, boolean testAlphaTexture) {
        //Stats.ProfilePhase p = new Stats.ProfilePhase(Stats.Prof.ShapeIntersect);
        // Transform Ray to object space
        Ray ray = WorldToObject.xform(r);

        // Compute plane intersection for disk

        // Reject disk intersections for rays parallel to the disk's plane
        if (ray.d.z == 0) return null;
        float tShapeHit = (height - ray.o.z) / ray.d.z;
        if (tShapeHit <= 0 || tShapeHit >= ray.tMax) return null;

        // See if hit point is inside disk radii and phimax
        Point3f pHit = ray.at(tShapeHit);
        float dist2 = pHit.x * pHit.x + pHit.y * pHit.y;
        if (dist2 > radius * radius || dist2 < innerRadius * innerRadius)
            return null;

        // Test disk phi value against phimax
        float phi = (float)Math.atan2(pHit.y, pHit.x);
        if (phi < 0) phi += 2 * Pbrt.Pi;
        if (phi > phiMax) return null;

        // Find parametric representation of disk hit
        float u = phi / phiMax;
        float rHit = (float)Math.sqrt(dist2);
        float oneMinusV = ((rHit - innerRadius) / (radius - innerRadius));
        float v = 1 - oneMinusV;
        Vector3f dpdu = new Vector3f(-phiMax * pHit.y, phiMax * pHit.x, 0);
        Vector3f dpdv = (new Vector3f(pHit.x, pHit.y, 0)).scale((innerRadius - radius) / rHit);
        Normal3f dndu = new Normal3f(0, 0, 0);
        Normal3f dndv = new Normal3f(0, 0, 0);

        // Refine disk intersection point
        pHit.z = height;

        // Compute error bounds for disk intersection
        Vector3f pError = new Vector3f(0, 0, 0);

        HitResult hr = new HitResult();
        // Initialize _SurfaceInteraction_ from parametric information
        hr.isect = ObjectToWorld.xform(new SurfaceInteraction(pHit, pError, new Point2f(u, v),
                ray.d.negate(), dpdu, dpdv, dndu, dndv, ray.time, this));

        // Update tHit for quadric intersection
        hr.tHit = tShapeHit;
        return hr;
    }

    @Override
    public boolean IntersectP(Ray r, boolean testAlphaTexture) {
        //Stats.ProfilePhase p = new Stats.ProfilePhase(Stats.Prof.ShapeIntersectP);
        // Transform _Ray_ to object space
        Ray ray = WorldToObject.xform(r);

        // Compute plane intersection for disk

        // Reject disk intersections for rays parallel to the disk's plane
        if (ray.d.z == 0) return false;
        float tShapeHit = (height - ray.o.z) / ray.d.z;
        if (tShapeHit <= 0 || tShapeHit >= ray.tMax) return false;

        // See if hit point is inside disk radii and $\phimax$
        Point3f pHit = ray.at(tShapeHit);
        float dist2 = pHit.x * pHit.x + pHit.y * pHit.y;
        if (dist2 > radius * radius || dist2 < innerRadius * innerRadius)
            return false;

        // Test disk $\phi$ value against $\phimax$
        float phi = (float)Math.atan2(pHit.y, pHit.x);
        if (phi < 0) phi += 2 * Pbrt.Pi;
        return !(phi > phiMax);
    }

    @Override
    public float Area() {
        return phiMax * 0.5f * (radius * radius - innerRadius * innerRadius);
    }

    @Override
    public SampleResult Sample(Point2f u) {
        Point2f pd = Sampling.ConcentricSampleDisk(u);
        Point3f pObj = new Point3f(pd.x * radius, pd.y * radius, height);

        SurfaceInteraction it = new SurfaceInteraction();
        it.n = Normal3f.Normalize(ObjectToWorld.xform(new Normal3f(0, 0, 1)));
        if (reverseOrientation) it.n = it.n.negate();
        it.p = ObjectToWorld.xform(pObj);
        it.pError = ObjectToWorld.absError(new Vector3f(0, 0, 0));
        SampleResult sr = new SampleResult();
        sr.isect = it;
        sr.pdf = 1 / Area();
        return sr;
    }

    public static Shape Create(Transform o2w, Transform w2o, boolean reverseOrientation, ParamSet paramSet) {
        float height = paramSet.FindOneFloat("height", 0);
        float radius = paramSet.FindOneFloat("radius", 1);
        float inner_radius = paramSet.FindOneFloat("innerradius", 0);
        float phimax = paramSet.FindOneFloat("phimax", 360);
        return new Disk(o2w, w2o, reverseOrientation, height, radius, inner_radius, phimax);
    }

    private final float height;
    private final float radius;
    private final float innerRadius;
    private final float phiMax;
}