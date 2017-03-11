
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
import org.pbrt.core.Error;

public class Cone extends Shape {

    public Cone(Transform ObjectToWorld, Transform WorldToObject, boolean reverseOrientation, float height, float radius, float phiMax) {
        super(ObjectToWorld, WorldToObject, reverseOrientation);
        this.radius = radius;
        this.height = height;
        this.phiMax = (float)Math.toRadians(Pbrt.Clamp(phiMax, 0, 360));
    }

    @Override
    public Bounds3f ObjectBound() {
        Point3f p1 = new Point3f(-radius, -radius, 0);
        Point3f p2 = new Point3f(radius, radius, height);
        return new Bounds3f(p1, p2);
    }

    @Override
    public HitResult Intersect(Ray r, boolean testAlphaTexture) {
        Stats.ProfilePhase p = new Stats.ProfilePhase(Stats.Prof.ShapeIntersect);
        Float phi;
        Point3f pHit;
        // Transform _Ray_ to object space
        Vector3f oErr = WorldToObject.absError(r.o);
        Vector3f dErr = WorldToObject.absError(r.d);
        Ray ray = WorldToObject.xform(r);

        // Compute quadratic cone coefficients

        // Initialize _EFloat_ ray coordinate values
        EFloat ox = new EFloat(ray.o.x, oErr.x), oy = new EFloat(ray.o.y, oErr.y), oz = new EFloat(ray.o.z, oErr.z);
        EFloat dx = new EFloat(ray.d.x, dErr.x), dy = new EFloat(ray.d.y, dErr.y), dz = new EFloat(ray.d.z, dErr.z);
        EFloat k = new EFloat(radius).divide(new EFloat(height));
        k = k.multiply(k);
        EFloat a = dx.multiply(dx).add(dy.multiply(dy)).subtract(k.multiply(dz.multiply(dz)));
        EFloat b = ((dx.multiply(ox)).add(dy.multiply(oy)).subtract(k.multiply(dz.multiply(oz.subtract(height))))).multiply(2);
        EFloat c = ox.multiply(ox).add(oy.multiply(oy)).subtract(k.multiply(oz.subtract(height)).multiply(oz.subtract(height)));

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

        // Compute cone inverse mapping
        pHit = ray.at(tShapeHit.asFloat());
        phi = (float)Math.atan2(pHit.y, pHit.x);
        if (phi < 0) phi += 2 * Pbrt.Pi;

        // Test cone intersection against clipping parameters
        if (pHit.z < 0 || pHit.z > height || phi > phiMax) {
            if (tShapeHit == qr.t1) return null;
            tShapeHit = qr.t1;
            if (qr.t1.upperBound() > ray.tMax) return null;
            // Compute cone inverse mapping
            pHit = ray.at(tShapeHit.asFloat());
            phi = (float)Math.atan2(pHit.y, pHit.x);
            if (phi < 0.) phi += 2 * Pbrt.Pi;
            if (pHit.z < 0 || pHit.z > height || phi > phiMax) return null;
        }

        // Find parametric representation of cone hit
        float u = phi / phiMax;
        float v = pHit.z / height;

        // Compute cone $\dpdu$ and $\dpdv$
        Vector3f dpdu = new Vector3f(-phiMax * pHit.y, phiMax * pHit.x, 0);
        Vector3f dpdv = new Vector3f(-pHit.x / (1 - v), -pHit.y / (1 - v), height);

        // Compute cone $\dndu$ and $\dndv$
        Vector3f d2Pduu = (new Vector3f(pHit.x, pHit.y, 0)).scale(-phiMax * phiMax);
        Vector3f d2Pduv = (new Vector3f(pHit.y, -pHit.x, 0)).scale(phiMax / (1 - v));
        Vector3f d2Pdvv = new Vector3f(0, 0, 0);

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

        // Compute error bounds for cone intersection

        // Compute error bounds for intersection computed with ray equation
        EFloat px = ox.add(tShapeHit.multiply(dx));
        EFloat py = oy.add(tShapeHit.multiply(dy));
        EFloat pz = oz.add(tShapeHit.multiply(dz));
        Vector3f pError = new Vector3f(px.getAbsoluteError(), py.getAbsoluteError(), pz.getAbsoluteError());

        // Initialize _SurfaceInteraction_ from parametric information
        HitResult hr = new HitResult();
        hr.isect = ObjectToWorld.xform(new SurfaceInteraction(pHit, pError, new Point2f(u, v),
                ray.d.negate(), dpdu, dpdv, dndu, dndv, ray.time, this));
        hr.tHit = tShapeHit.asFloat();
        return hr;
    }

    @Override
    public boolean IntersectP(Ray r, boolean testAlphaTexture) {
        Stats.ProfilePhase p = new Stats.ProfilePhase(Stats.Prof.ShapeIntersectP);
        float phi;
        Point3f pHit;
        // Transform _Ray_ to object space
        Vector3f oErr = WorldToObject.absError(r.o);
        Vector3f dErr = WorldToObject.absError(r.d);
        Ray ray = WorldToObject.xform(r);

        // Compute quadratic cone coefficients

        // Initialize _EFloat_ ray coordinate values
        EFloat ox = new EFloat(ray.o.x, oErr.x), oy = new EFloat(ray.o.y, oErr.y), oz = new EFloat(ray.o.z, oErr.z);
        EFloat dx = new EFloat(ray.d.x, dErr.x), dy = new EFloat(ray.d.y, dErr.y), dz = new EFloat(ray.d.z, dErr.z);
        EFloat k = new EFloat(radius).divide(new EFloat(height));
        k = k.multiply(k);
        EFloat a = dx.multiply(dx).add(dy.multiply(dy)).subtract(k.multiply(dz.multiply(dz)));
        EFloat b = ((dx.multiply(ox)).add(dy.multiply(oy)).subtract(k.multiply(dz.multiply(oz.subtract(height))))).multiply(2);
        EFloat c = ox.multiply(ox).add(oy.multiply(oy)).subtract(k.multiply(oz.subtract(height)).multiply(oz.subtract(height)));

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

        // Compute cone inverse mapping
        pHit = ray.at(tShapeHit.asFloat());
        phi = (float)Math.atan2(pHit.y, pHit.x);
        if (phi < 0) phi += 2 * Pbrt.Pi;

        // Test cone intersection against clipping parameters
        if (pHit.z < 0 || pHit.z > height || phi > phiMax) {
            if (tShapeHit == qr.t1) return false;
            tShapeHit = qr.t1;
            if (qr.t1.upperBound() > ray.tMax) return false;
            // Compute cone inverse mapping
            pHit = ray.at(tShapeHit.asFloat());
            phi = (float)Math.atan2(pHit.y, pHit.x);
            if (phi < 0.) phi += 2 * Pbrt.Pi;
            if (pHit.z < 0 || pHit.z > height || phi > phiMax) return false;
        }
        return true;
    }

    @Override
    public float Area() {
        return radius * (float)Math.sqrt((height * height) + (radius * radius)) * phiMax / 2;
    }

    @Override
    public SampleResult Sample(Point2f u) {
        Error.Error("Cone.Sample not implemented.");
        return null;
    }

    public static Shape Create(Transform o2w, Transform w2o, boolean reverseOrientation, ParamSet paramSet) {
        float radius = paramSet.FindOneFloat("radius", 1);
        float height = paramSet.FindOneFloat("height", 1);
        float phimax = paramSet.FindOneFloat("phimax", 360);
        return new Cone(o2w, w2o, reverseOrientation, height, radius, phimax);
    }

    private final float radius;
    private final float height;
    private final float phiMax;
}