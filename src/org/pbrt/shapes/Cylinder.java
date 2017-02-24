
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

public class Cylinder extends Shape {

    public Cylinder(Transform ObjectToWorld, Transform WorldToObject, boolean reverseOrientation, float radius, float zMin, float zMax, float phiMax) {
        super(ObjectToWorld, WorldToObject, reverseOrientation);
        this.radius = radius;
        this.zMin = Math.min(zMin, zMax);
        this.zMax = Math.max(zMin, zMax);
        this.phiMax = (float)Math.toRadians(Pbrt.Clamp(phiMax, 0, 360));
    }

    public static Shape Create(Transform object2world, Transform world2object, boolean reverseOrientation, ParamSet paramSet) {
        float radius = paramSet.FindOneFloat("radius", 1);
        float zmin = paramSet.FindOneFloat("zmin", -1);
        float zmax = paramSet.FindOneFloat("zmax", 1);
        float phimax = paramSet.FindOneFloat("phimax", 360);
        return new Cylinder(object2world, world2object, reverseOrientation, radius, zmin, zmax, phimax);
    }

    @Override
    public Bounds3f ObjectBound() {
        return new Bounds3f(new Point3f(-radius, -radius, zMin), new Point3f(radius, radius, zMax));
    }

    @Override
    public HitResult Intersect(Ray r, boolean testAlphaTexture) {
        //ProfilePhase p(Prof::ShapeIntersect);
        float phi;
        Point3f pHit;
        // Transform _Ray_ to object space
        Vector3f oErr = WorldToObject.absError(r.o);
        Vector3f dErr = WorldToObject.absError(r.d);
        Ray ray = WorldToObject.xform(r);

        // Compute quadratic cylinder coefficients

        // Initialize _EFloat_ ray coordinate values
        EFloat ox = new EFloat(ray.o.x, oErr.x), oy = new EFloat(ray.o.y, oErr.y), oz = new EFloat(ray.o.z, oErr.z);
        EFloat dx = new EFloat(ray.d.x, dErr.x), dy = new EFloat(ray.d.y, dErr.y), dz = new EFloat(ray.d.z, dErr.z);
        EFloat a = dx.multiply(dx).add(dy.multiply(dy));
        EFloat b = (dx.multiply(ox).add(dy.multiply(oy))).multiply(2);
        EFloat c = ox.multiply(ox).add(oy.multiply(oy)).subtract(new EFloat(radius).multiply(new EFloat(radius)));

        // Solve quadratic equation for _t_ values
        EFloat.QuadRes qr = EFloat.Quadratic(a, b, c);
        if (qr == null) return null;

        // Check quadric shape _t0_ and _t1_ for nearest intersection
        if (qr.t0.upperBound() > ray.tMax || qr.t1.lowerBound() <= 0) return null;
        EFloat tShapeHit = qr.t0;
        if (tShapeHit.lowerBound() <= 0) {
            tShapeHit = qr.t1;
            if (tShapeHit.upperBound() > ray.tMax) return null;
        }

        // Compute cylinder hit point and $\phi$
        pHit = ray.at(tShapeHit.asFloat());

        // Refine cylinder intersection point
        float hitRad = (float)Math.sqrt(pHit.x * pHit.x + pHit.y * pHit.y);
        pHit.x *= radius / hitRad;
        pHit.y *= radius / hitRad;
        phi = (float)Math.atan2(pHit.y, pHit.x);
        if (phi < 0) phi += 2 * (float)Math.PI;

        // Test cylinder intersection against clipping parameters
        if (pHit.z < zMin || pHit.z > zMax || phi > phiMax) {
            if (tShapeHit == qr.t1) return null;
            tShapeHit = qr.t1;
            if (qr.t1.upperBound() > ray.tMax) return null;
            // Compute cylinder hit point and $\phi$
            pHit = ray.at(tShapeHit.asFloat());

            // Refine cylinder intersection point
            hitRad = (float)Math.sqrt(pHit.x * pHit.x + pHit.y * pHit.y);
            pHit.x *= radius / hitRad;
            pHit.y *= radius / hitRad;
            phi = (float)Math.atan2(pHit.y, pHit.x);
            if (phi < 0) phi += 2 * (float)Math.PI;
            if (pHit.z < zMin || pHit.z > zMax || phi > phiMax) return null;
        }

        // Find parametric representation of cylinder hit
        float u = phi / phiMax;
        float v = (pHit.z - zMin) / (zMax - zMin);

        // Compute cylinder $\dpdu$ and $\dpdv$
        Vector3f dpdu = new Vector3f(-phiMax * pHit.y, phiMax * pHit.x, 0);
        Vector3f dpdv = new Vector3f(0, 0, zMax - zMin);

        // Compute cylinder $\dndu$ and $\dndv$
        Vector3f d2Pduu = (new Vector3f(pHit.x, pHit.y, 0)).scale(-phiMax * phiMax);
        Vector3f d2Pduv = new Vector3f(0, 0, 0), d2Pdvv = new Vector3f(0, 0, 0);

        // Compute coefficients for fundamental forms
        float E = Vector3f.Dot(dpdu, dpdu);
        float F = Vector3f.Dot(dpdu, dpdv);
        float G = Vector3f.Dot(dpdv, dpdv);
        Vector3f N = Vector3f.Normalize(Vector3f.Cross(dpdu, dpdv));
        float e = Vector3f.Dot(N, d2Pduu);
        float f = Vector3f.Dot(N, d2Pduv);
        float g = Vector3f.Dot(N, d2Pdvv);

        // Compute $\dndu$ and $\dndv$ from fundamental form coefficients
        float invEGF2 = 1 / (E * G - F * F);
        Normal3f dndu = new Normal3f(dpdu.scale((f * F - e * G) * invEGF2).add(dpdv.scale((e * F - f * E) * invEGF2)));
        Normal3f dndv = new Normal3f(dpdu.scale((g * F - f * G) * invEGF2).add(dpdv.scale((f * F - g * E) * invEGF2)));

        // Compute error bounds for cylinder intersection
        Vector3f pError = Vector3f.Abs(new Vector3f(pHit.x, pHit.y, 0)).scale(Pbrt.gamma(3));

        // Initialize _SurfaceInteraction_ from parametric information
        HitResult hr = new HitResult();
        hr.isect = ObjectToWorld.xform(new SurfaceInteraction(pHit, pError, new Point2f(u, v), ray.d.negate(), dpdu, dpdv, dndu, dndv,
                ray.time, this));

        // Update _tHit_ for quadric intersection
        hr.tHit = tShapeHit.asFloat();
        return hr;
    }

    @Override
    public boolean IntersectP(Ray r, boolean testAlphaTexture) {
        //ProfilePhase p(Prof::ShapeIntersectP);
        Float phi;
        Point3f pHit;
        // Transform _Ray_ to object space
        Vector3f oErr = WorldToObject.absError(r.o);
        Vector3f dErr = WorldToObject.absError(r.d);
        Ray ray = WorldToObject.xform(r);

        // Compute quadratic cylinder coefficients

        // Initialize _EFloat_ ray coordinate values
        EFloat ox = new EFloat(ray.o.x, oErr.x), oy = new EFloat(ray.o.y, oErr.y), oz = new EFloat(ray.o.z, oErr.z);
        EFloat dx = new EFloat(ray.d.x, dErr.x), dy = new EFloat(ray.d.y, dErr.y), dz = new EFloat(ray.d.z, dErr.z);
        EFloat a = dx.multiply(dx).add(dy.multiply(dy));
        EFloat b = (dx.multiply(ox).add(dy.multiply(oy))).multiply(2);
        EFloat c = ox.multiply(ox).add(oy.multiply(oy)).subtract(new EFloat(radius).multiply(new EFloat(radius)));

        // Solve quadratic equation for _t_ values
        EFloat.QuadRes qr = EFloat.Quadratic(a, b, c);
        if (qr == null) return false;

        // Check quadric shape _t0_ and _t1_ for nearest intersection
        if (qr.t0.upperBound() > ray.tMax || qr.t1.lowerBound() <= 0) return false;
        EFloat tShapeHit = qr.t0;
        if (tShapeHit.lowerBound() <= 0) {
            tShapeHit = qr.t1;
            if (tShapeHit.upperBound() > ray.tMax) return false;
        }

        // Compute cylinder hit point and $\phi$
        pHit = ray.at(tShapeHit.asFloat());

        // Refine cylinder intersection point
        float hitRad = (float)Math.sqrt(pHit.x * pHit.x + pHit.y * pHit.y);
        pHit.x *= radius / hitRad;
        pHit.y *= radius / hitRad;
        phi = (float)Math.atan2(pHit.y, pHit.x);
        if (phi < 0) phi += 2 * (float)Math.PI;

        // Test cylinder intersection against clipping parameters
        if (pHit.z < zMin || pHit.z > zMax || phi > phiMax) {
            if (tShapeHit == qr.t1) return false;
            tShapeHit = qr.t1;
            if (qr.t1.upperBound() > ray.tMax) return false;
            // Compute cylinder hit point and $\phi$
            pHit = ray.at(tShapeHit.asFloat());

            // Refine cylinder intersection point
            hitRad = (float)Math.sqrt(pHit.x * pHit.x + pHit.y * pHit.y);
            pHit.x *= radius / hitRad;
            pHit.y *= radius / hitRad;
            phi = (float)Math.atan2(pHit.y, pHit.x);
            if (phi < 0) phi += 2 * (float)Math.PI;
            if (pHit.z < zMin || pHit.z > zMax || phi > phiMax) return false;
        }
        return true;
    }

    @Override
    public float Area() {
        return (zMax - zMin) * radius * phiMax;
    }

    @Override
    public SampleResult Sample(Point2f u) {
        SampleResult sr = new SampleResult();
        float z = Pbrt.Lerp(u.x, zMin, zMax);
        float phi = u.y * phiMax;
        Point3f pObj = new Point3f(radius * (float)Math.cos(phi), radius * (float)Math.sin(phi), z);
        sr.isect = new SurfaceInteraction();
        sr.isect.n = Normal3f.Normalize(ObjectToWorld.xform(new Normal3f(pObj.x, pObj.y, 0)));
        if (reverseOrientation) sr.isect.n = sr.isect.n.negate();
        // Reproject _pObj_ to cylinder surface and compute _pObjError_
        float hitRad = (float)Math.sqrt(pObj.x * pObj.x + pObj.y * pObj.y);
        pObj.x *= radius / hitRad;
        pObj.y *= radius / hitRad;
        Vector3f pObjError = Vector3f.Abs(new Vector3f(pObj.x, pObj.y, 0)).scale(Pbrt.gamma(3));
        sr.isect.pError = ObjectToWorld.absError(pObjError);
        sr.isect.p = ObjectToWorld.xform(pObj);
        sr.pdf = 1 / Area();
        return sr;
    }

    private final float radius, zMin, zMax, phiMax;
}