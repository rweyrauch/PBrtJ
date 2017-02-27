
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

public class Paraboloid extends Shape {

    public static Shape Create(Transform o2w, Transform w2o, boolean reverseOrientation, ParamSet paramSet) {
        float radius = paramSet.FindOneFloat("radius", 1);
        float zmin = paramSet.FindOneFloat("zmin", 0);
        float zmax = paramSet.FindOneFloat("zmax", 1);
        float phimax = paramSet.FindOneFloat("phimax", 360);
        return new Paraboloid(o2w, w2o, reverseOrientation, radius, zmin, zmax, phimax);
    }

    public Paraboloid(Transform ObjectToWorld, Transform WorldToObject, boolean reverseOrientation, float radius, float z0, float z1, float phiMax) {
        super(ObjectToWorld, WorldToObject, reverseOrientation);
        this.radius = radius;
        this.zMin = Math.min(z0, z1);
        this.zMax = Math.max(z0, z1);
        this.phiMax = (float)Math.toRadians(Pbrt.Clamp(phiMax, 0, 360));
    }

    @Override
    public Bounds3f ObjectBound() {
        Point3f p1 = new Point3f(-radius, -radius, zMin);
        Point3f p2 = new Point3f(radius, radius, zMax);
        return new Bounds3f(p1, p2);
    }

    @Override
    public HitResult Intersect(Ray r, boolean testAlphaTexture) {
        Stats.ProfilePhase p = new Stats.ProfilePhase(Stats.Prof.ShapeIntersect);
        float phi;
        Point3f pHit;
        // Transform _Ray_ to object space
        Vector3f oErr = WorldToObject.absError(r.o);
        Vector3f dErr = WorldToObject.absError(r.d);
        Ray ray = WorldToObject.xform(r);

        // Compute quadratic paraboloid coefficients

        // Initialize _EFloat_ ray coordinate values
        EFloat ox = new EFloat(ray.o.x, oErr.x), oy = new EFloat(ray.o.y, oErr.y), oz = new EFloat(ray.o.z, oErr.z);
        EFloat dx = new EFloat(ray.d.x, dErr.x), dy = new EFloat(ray.d.y, dErr.y), dz = new EFloat(ray.d.z, dErr.z);
        EFloat eradius = new EFloat(radius);
        EFloat ezmax = new EFloat(zMax);
        EFloat k = ezmax.divide(eradius.multiply(eradius));
        EFloat a = k.multiply(dx.multiply(dx).add(dy.multiply(dy)));
        EFloat b = (k.multiply(2).multiply(ox.multiply(dx).add(oy.multiply(dy)))).subtract(dz);
        EFloat c = (k.multiply(ox.multiply(ox).add(oy.multiply(oy)))).subtract(oz);

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

        // Compute paraboloid inverse mapping
        pHit = ray.at(tShapeHit.asFloat());
        phi = (float)Math.atan2(pHit.y, pHit.x);
        if (phi < 0) phi += 2 * (float)Math.PI;

        // Test paraboloid intersection against clipping parameters
        if (pHit.z < zMin || pHit.z > zMax || phi > phiMax) {
            if (tShapeHit == qr.t1) return null;
            tShapeHit = qr.t1;
            if (qr.t1.upperBound() > ray.tMax) return null;
            // Compute paraboloid inverse mapping
            pHit = ray.at(tShapeHit.asFloat());
            phi = (float)Math.atan2(pHit.y, pHit.x);
            if (phi < 0) phi += 2 * (float)Math.PI;
            if (pHit.z < zMin || pHit.z > zMax || phi > phiMax) return null;
        }

        // Find parametric representation of paraboloid hit
        float u = phi / phiMax;
        float v = (pHit.z - zMin) / (zMax - zMin);

        // Compute paraboloid $\dpdu$ and $\dpdv$
        Vector3f dpdu = new Vector3f(-phiMax * pHit.y, phiMax * pHit.x, 0);
        Vector3f dpdv = (new Vector3f(pHit.x / (2 * pHit.z), pHit.y / (2 * pHit.z), 1)).scale(zMax - zMin);

        // Compute paraboloid $\dndu$ and $\dndv$
        Vector3f d2Pduu = (new Vector3f(pHit.x, pHit.y, 0)).scale(-phiMax * phiMax);
        Vector3f d2Pduv = (new Vector3f(-pHit.y / (2 * pHit.z), pHit.x / (2 * pHit.z), 0)).scale((zMax - zMin) * phiMax);
        Vector3f d2Pdvv = (new Vector3f(pHit.x / (4 * pHit.z * pHit.z), pHit.y / (4 * pHit.z * pHit.z), 0)).scale(-(zMax - zMin) * (zMax - zMin));

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

        // Compute error bounds for paraboloid intersection

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

        // Compute quadratic paraboloid coefficients

        // Initialize _EFloat_ ray coordinate values
        EFloat ox = new EFloat(ray.o.x, oErr.x), oy = new EFloat(ray.o.y, oErr.y), oz = new EFloat(ray.o.z, oErr.z);
        EFloat dx = new EFloat(ray.d.x, dErr.x), dy = new EFloat(ray.d.y, dErr.y), dz = new EFloat(ray.d.z, dErr.z);
        EFloat eradius = new EFloat(radius);
        EFloat ezmax = new EFloat(zMax);
        EFloat k = ezmax.divide(eradius.multiply(eradius));
        EFloat a = k.multiply(dx.multiply(dx).add(dy.multiply(dy)));
        EFloat b = (k.multiply(2).multiply(ox.multiply(dx).add(oy.multiply(dy)))).subtract(dz);
        EFloat c = (k.multiply(ox.multiply(ox).add(oy.multiply(oy)))).subtract(oz);

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

        // Compute paraboloid inverse mapping
        pHit = ray.at(tShapeHit.asFloat());
        phi = (float)Math.atan2(pHit.y, pHit.x);
        if (phi < 0) phi += 2 * (float)Math.PI;

        // Test paraboloid intersection against clipping parameters
        if (pHit.z < zMin || pHit.z > zMax || phi > phiMax) {
            if (tShapeHit == qr.t1) return false;
            tShapeHit = qr.t1;
            if (qr.t1.upperBound() > ray.tMax) return false;
            // Compute paraboloid inverse mapping
            pHit = ray.at(tShapeHit.asFloat());
            phi = (float)Math.atan2(pHit.y, pHit.x);
            if (phi < 0) phi += 2 * (float)Math.PI;
            if (pHit.z < zMin || pHit.z > zMax || phi > phiMax) return false;
        }
        return true;
    }

    @Override
    public float Area() {
        float radius2 = radius * radius;
        float k = 4 * zMax / radius2;
        return (radius2 * radius2 * phiMax / (12 * zMax * zMax)) *
                ((float)Math.pow(k * zMax + 1, 1.5f) - (float)Math.pow(k * zMin + 1, 1.5f));
    }

    @Override
    public SampleResult Sample(Point2f u) {
        Error.Error("Paraboloid.Sample not implemented.");
        return null;
    }

    private final float radius, zMin, zMax, phiMax;
}