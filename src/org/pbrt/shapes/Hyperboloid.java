
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

public class Hyperboloid extends Shape {

    public static Shape Create(Transform o2w, Transform w2o, boolean reverseOrientation, ParamSet paramSet) {
        Point3f p1 = paramSet.FindOnePoint3f("p1", new Point3f(0, 0, 0));
        Point3f p2 = paramSet.FindOnePoint3f("p2", new Point3f(1, 1, 1));
        float phimax = paramSet.FindOneFloat("phimax", 360);
        return new Hyperboloid(o2w, w2o, reverseOrientation, p1, p2, phimax);
    }

    public Hyperboloid(Transform ObjectToWorld, Transform WorldToObject, boolean reverseOrientation, Point3f p1, Point3f p2, float phiMax) {
        super(ObjectToWorld, WorldToObject, reverseOrientation);
        this.p1 = new Point3f(p1);
        this.p2 = new Point3f(p2);
        this.phiMax = (float)Math.toRadians(Pbrt.Clamp(phiMax, 0, 360));
        float radius1 = (float)Math.sqrt(p1.x * p1.x + p1.y * p1.y);
        float radius2 = (float)Math.sqrt(p2.x * p2.x + p2.y * p2.y);
        rMax = Math.max(radius1, radius2);
        zMin = Math.min(p1.z, p2.z);
        zMax = Math.max(p1.z, p2.z);
        // Compute implicit function coefficients for hyperboloid
        if (p2.z == 0) {
            Point3f temp = p1;
            p1 = p2;
            p2 = temp;
        }
        Point3f pp = p1;
        float xy1, xy2;
        do {
            pp = pp.add((p2.subtract(p1)).scale(2));
            xy1 = pp.x * pp.x + pp.y * pp.y;
            xy2 = p2.x * p2.x + p2.y * p2.y;
            ah = (1.f / xy1 - (pp.z * pp.z) / (xy1 * p2.z * p2.z)) /
                    (1 - (xy2 * pp.z * pp.z) / (xy1 * p2.z * p2.z));
            ch = (ah * xy2 - 1) / (p2.z * p2.z);
        } while (Float.isInfinite(ah) || Float.isNaN(ah));
    }

    @Override
    public Bounds3f ObjectBound() {
        Point3f p1 = new Point3f(-rMax, -rMax, zMin);
        Point3f p2 = new Point3f(rMax, rMax, zMax);
        return new Bounds3f(p1, p2);
    }

    @Override
    public HitResult Intersect(Ray r, boolean testAlphaTexture) {
        float phi;
        Point3f pHit;
        // Transform _Ray_ to object space
        Vector3f oErr = WorldToObject.absError(r.o);
        Vector3f dErr = WorldToObject.absError(r.d);
        Ray ray = WorldToObject.xform(r);

        // Compute quadratic hyperboloid coefficients

        // Initialize _EFloat_ ray coordinate values
        EFloat ox = new EFloat(ray.o.x, oErr.x), oy = new EFloat(ray.o.y, oErr.y), oz = new EFloat(ray.o.z, oErr.z);
        EFloat dx = new EFloat(ray.d.x, dErr.x), dy = new EFloat(ray.d.y, dErr.y), dz = new EFloat(ray.d.z, dErr.z);
        EFloat a = (dx.multiply(dx.multiply(ah))).add(dy.multiply(dy.multiply(ah))).subtract(dz.multiply(dz.multiply(ch)));
        EFloat b = ((dx.multiply(ox.multiply(ah))).add(dy.multiply(oy.multiply(ah))).subtract(dz.multiply(oz.multiply(ch)))).multiply(2);
        EFloat c = (ox.multiply(ox.multiply(ah))).add(oy.multiply(oy.multiply(ah))).subtract(oz.multiply(oz.multiply(ch))).subtract(1);

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

        // Compute hyperboloid inverse mapping
        pHit = ray.at(tShapeHit.asFloat());
        float v = (pHit.z - p1.z) / (p2.z - p1.z);
        Point3f pr = p1.scale(1-v).add(p2.scale(v));
        phi = (float)Math.atan2(pr.x * pHit.y - pHit.x * pr.y, pHit.x * pr.x + pHit.y * pr.y);
        if (phi < 0) phi += 2 * Pbrt.Pi;

        // Test hyperboloid intersection against clipping parameters
        if (pHit.z < zMin || pHit.z > zMax || phi > phiMax) {
            if (tShapeHit == qr.t1) return null;
            tShapeHit = qr.t1;
            if (qr.t1.upperBound() > ray.tMax) return null;
            // Compute hyperboloid inverse mapping
            pHit = ray.at(tShapeHit.asFloat());
            v = (pHit.z - p1.z) / (p2.z - p1.z);
            pr = p1.scale(1-v).add(p2.scale(v));
            phi = (float)Math.atan2(pr.x * pHit.y - pHit.x * pr.y, pHit.x * pr.x + pHit.y * pr.y);
            if (phi < 0) phi += 2 * Pbrt.Pi;
            if (pHit.z < zMin || pHit.z > zMax || phi > phiMax) return null;
        }

        // Compute parametric representation of hyperboloid hit
        float u = phi / phiMax;

        // Compute hyperboloid $\dpdu$ and $\dpdv$
        float cosPhi = (float)Math.cos(phi), sinPhi = (float)Math.sin(phi);
        Vector3f dpdu = new Vector3f(-phiMax * pHit.y, phiMax * pHit.x, 0);
        Vector3f dpdv = new Vector3f((p2.x - p1.x) * cosPhi - (p2.y - p1.y) * sinPhi,
                (p2.x - p1.x) * sinPhi + (p2.y - p1.y) * cosPhi, p2.z - p1.z);

        // Compute hyperboloid $\dndu$ and $\dndv$
        Vector3f d2Pduu = (new Vector3f(pHit.x, pHit.y, 0)).scale(-phiMax * phiMax);
        Vector3f d2Pduv = (new Vector3f(-dpdv.y, dpdv.x, 0)).scale(phiMax);
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

        // Compute error bounds for hyperboloid intersection

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
        float phi;
        Point3f pHit;
        // Transform _Ray_ to object space
        Vector3f oErr = WorldToObject.absError(r.o);
        Vector3f dErr = WorldToObject.absError(r.d);
        Ray ray = WorldToObject.xform(r);

        // Compute quadratic hyperboloid coefficients

        // Initialize _EFloat_ ray coordinate values
        EFloat ox = new EFloat(ray.o.x, oErr.x), oy = new EFloat(ray.o.y, oErr.y), oz = new EFloat(ray.o.z, oErr.z);
        EFloat dx = new EFloat(ray.d.x, dErr.x), dy = new EFloat(ray.d.y, dErr.y), dz = new EFloat(ray.d.z, dErr.z);
        EFloat a = (dx.multiply(dx.multiply(ah))).add(dy.multiply(dy.multiply(ah))).subtract(dz.multiply(dz.multiply(ch)));
        EFloat b = ((dx.multiply(ox.multiply(ah))).add(dy.multiply(oy.multiply(ah))).subtract(dz.multiply(oz.multiply(ch)))).multiply(2);
        EFloat c = (ox.multiply(ox.multiply(ah))).add(oy.multiply(oy.multiply(ah))).subtract(oz.multiply(oz.multiply(ch))).subtract(1);

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

        // Compute hyperboloid inverse mapping
        pHit = ray.at(tShapeHit.asFloat());
        float v = (pHit.z - p1.z) / (p2.z - p1.z);
        Point3f pr = p1.scale(1-v).add(p2.scale(v));
        phi = (float)Math.atan2(pr.x * pHit.y - pHit.x * pr.y, pHit.x * pr.x + pHit.y * pr.y);
        if (phi < 0) phi += 2 * Pbrt.Pi;

        // Test hyperboloid intersection against clipping parameters
        if (pHit.z < zMin || pHit.z > zMax || phi > phiMax) {
            if (tShapeHit == qr.t1) return false;
            tShapeHit = qr.t1;
            if (qr.t1.upperBound() > ray.tMax) return false;
            // Compute hyperboloid inverse mapping
            pHit = ray.at(tShapeHit.asFloat());
            v = (pHit.z - p1.z) / (p2.z - p1.z);
            pr = p1.scale(1-v).add(p2.scale(v));
            phi = (float)Math.atan2(pr.x * pHit.y - pHit.x * pr.y, pHit.x * pr.x + pHit.y * pr.y);
            if (phi < 0) phi += 2 * Pbrt.Pi;
            if (pHit.z < zMin || pHit.z > zMax || phi > phiMax) return false;
        }
        return true;
    }

    private static float SQR(float a) { return a * a; }
    private static float QUAD(float a) { return SQR(a) * SQR(a); }
    @Override
    public float Area() {
        return phiMax / 6.f *
                (2 * QUAD(p1.x) - 2 * p1.x * p1.x * p1.x * p2.x + 2 * QUAD(p2.x) +
                        2 * (p1.y * p1.y + p1.y * p2.y + p2.y * p2.y) *
                                (SQR(p1.y - p2.y) + SQR(p1.z - p2.z)) +
                        p2.x * p2.x * (5 * p1.y * p1.y + 2 * p1.y * p2.y - 4 * p2.y * p2.y +
                                2 * SQR(p1.z - p2.z)) +
                        p1.x * p1.x * (-4 * p1.y * p1.y + 2 * p1.y * p2.y +
                                5 * p2.y * p2.y + 2 * SQR(p1.z - p2.z)) -
                        2 * p1.x * p2.x *
                                (p2.x * p2.x - p1.y * p1.y + 5 * p1.y * p2.y - p2.y * p2.y -
                                        p1.z * p1.z + 2 * p1.z * p2.z - p2.z * p2.z));
    }

    @Override
    public SampleResult Sample(Point2f u) {
        Error.Error("Hyperboloid.Sample not implemented.");
        return null;
    }

    private Point3f p1, p2;
    private float zMin, zMax;
    private float phiMax;
    private float rMax;
    private float ah, ch;

}