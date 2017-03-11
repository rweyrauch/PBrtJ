
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

public class Sphere extends Shape {

    public Sphere(Transform ObjectToWorld, Transform WorldToObject, boolean reverseOrientation, float radius, float zMin, float zMax, float phiMax) {
        super(ObjectToWorld, WorldToObject, reverseOrientation);
        this.radius = radius;
        this.zMin = Pbrt.Clamp(Math.min(zMin, zMax), -radius, radius);
        this.zMax = Pbrt.Clamp(Math.max(zMin, zMax), -radius, radius);
        this.thetaMin = (float)Math.acos(Pbrt.Clamp(Math.min(zMin, zMax) / radius, -1, 1));
        this.thetaMax = (float)Math.acos(Pbrt.Clamp(Math.max(zMin, zMax) / radius, -1, 1));
        this.phiMax = (float)Math.toRadians(Pbrt.Clamp(phiMax, 0, 360));
    }

    public static Shape Create(Transform object2world, Transform world2object, boolean reverseOrientation, ParamSet paramSet) {
        float radius = paramSet.FindOneFloat("radius", 1);
        float zmin = paramSet.FindOneFloat("zmin", -radius);
        float zmax = paramSet.FindOneFloat("zmax", radius);
        float phimax = paramSet.FindOneFloat("phimax", 360);
        return new Sphere(object2world, world2object, reverseOrientation, radius, zmin, zmax, phimax);
    }

    @Override
    public Bounds3f ObjectBound() {
        return new Bounds3f(new Point3f(-radius, -radius, zMin), new Point3f(radius, radius, zMax));
    }

    @Override
    public HitResult Intersect(Ray r, boolean testAlphaTexture) {
        Stats.ProfilePhase p = new Stats.ProfilePhase(Stats.Prof.ShapeIntersect);

        // Transform _Ray_ to object space
        Vector3f oErr = WorldToObject.absError(r.o);
        Vector3f dErr = WorldToObject.absError(r.d);
        Ray ray = WorldToObject.xform(r);

        // Compute quadratic sphere coefficients

        //float fox = ray.o.x, foy = ray.o.y, foz = ray.o.z;
        //float fdx = ray.d.x, fdy = ray.d.y, fdz = ray.d.z;
        //float fa = fdx*fdx + fdy*fdy + fdz*fdz;
        //float fb = 2 * (fdx*fox + fdy * foy + fdz*foz);
        //float fc = fox *fox + foy*foy + foz*foz - radius * radius;

        // Initialize _EFloat_ ray coordinate values
        EFloat ox = new EFloat(ray.o.x, oErr.x), oy = new EFloat(ray.o.y, oErr.y), oz = new EFloat(ray.o.z, oErr.z);
        EFloat dx = new EFloat(ray.d.x, dErr.x), dy = new EFloat(ray.d.y, dErr.y), dz = new EFloat(ray.d.z, dErr.z);
        EFloat a = dx.multiply(dx).add(dy.multiply(dy)).add(dz.multiply(dz));
        EFloat b = (new EFloat(2)).multiply(dx.multiply(ox).add(dy.multiply(oy).add(dz.multiply(oz))));
        EFloat c = (ox.multiply(ox)).add(oy.multiply(oy)).add(oz.multiply(oz)).subtract(new EFloat(radius).multiply(new EFloat(radius)));

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

        // Compute sphere hit position and $\phi$
        Point3f pHit = ray.at(tShapeHit.asFloat());

        // Refine sphere intersection point
        pHit = pHit.scale(radius / Point3f.Distance(pHit, new Point3f(0, 0, 0)));
        if (pHit.x == 0 && pHit.y == 0) pHit.x = 1e-5f * radius;
        float phi = (float)Math.atan2(pHit.y, pHit.x);
        if (phi < 0) phi += 2 * Pbrt.Pi;

        // Test sphere intersection against clipping parameters
        if ((zMin > -radius && pHit.z < zMin) || (zMax < radius && pHit.z > zMax) || phi > phiMax) {
            if (tShapeHit == qr.t1) return null;
            if (qr.t1.upperBound() > ray.tMax) return null;
            tShapeHit = qr.t1;
            // Compute sphere hit position and $\phi$
            pHit = ray.at(tShapeHit.asFloat());

            // Refine sphere intersection point
            pHit = pHit.scale(radius / Point3f.Distance(pHit, new Point3f(0, 0, 0)));
            if (pHit.x == 0 && pHit.y == 0) pHit.x = 1e-5f * radius;
            phi = (float)Math.atan2(pHit.y, pHit.x);
            if (phi < 0) phi += 2 * Pbrt.Pi;
            if ((zMin > -radius && pHit.z < zMin) || (zMax < radius && pHit.z > zMax) || phi > phiMax)
                return null;
        }

        // Find parametric representation of sphere hit
        float u = phi / phiMax;
        float theta = (float)Math.acos(Pbrt.Clamp(pHit.z / radius, -1, 1));
        float v = (theta - thetaMin) / (thetaMax - thetaMin);

        // Compute sphere $\dpdu$ and $\dpdv$
        float zRadius = (float)Math.sqrt(pHit.x * pHit.x + pHit.y * pHit.y);
        Float invZRadius = 1 / zRadius;
        Float cosPhi = pHit.x * invZRadius;
        Float sinPhi = pHit.y * invZRadius;
        Vector3f dpdu = new Vector3f(-phiMax * pHit.y, phiMax * pHit.x, 0);
        Vector3f dpdv = (new Vector3f(pHit.z * cosPhi, pHit.z * sinPhi, -radius * (float)Math.sin(theta))).scale(thetaMax - thetaMin);

        // Compute sphere $\dndu$ and $\dndv$
        Vector3f d2Pduu = (new Vector3f(pHit.x, pHit.y, 0)).scale(-phiMax * phiMax);
        Vector3f d2Pduv = (new Vector3f(-sinPhi, cosPhi, 0)).scale((thetaMax - thetaMin) * pHit.z * phiMax);
        Vector3f d2Pdvv = (new Vector3f(pHit.x, pHit.y, pHit.z)).scale(-(thetaMax - thetaMin) * (thetaMax - thetaMin));

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

        // Compute error bounds for sphere intersection
        Vector3f pError = Vector3f.Abs(new Vector3f(pHit)).scale(Pbrt.gamma(5));

        // Initialize _SurfaceInteraction_ from parametric information
        HitResult hr = new HitResult();
        hr.isect = ObjectToWorld.xform(new SurfaceInteraction(pHit, pError, new Point2f(u, v),
                ray.d.negate(), dpdu, dpdv, dndu, dndv, ray.time, this));

        // Update _tHit_ for quadric intersection
        hr.tHit = tShapeHit.asFloat();
        return hr;
    }

    @Override
    public boolean IntersectP(Ray r, boolean testAlphaTexture) {
        Stats.ProfilePhase p = new Stats.ProfilePhase(Stats.Prof.ShapeIntersectP);

        // Transform _Ray_ to object space
        Vector3f oErr = WorldToObject.absError(r.o);
        Vector3f dErr = WorldToObject.absError(r.d);
        Ray ray = WorldToObject.xform(r);

        // Compute quadratic sphere coefficients

        //float fox = ray.o.x, foy = ray.o.y, foz = ray.o.z;
        //float fdx = ray.d.x, fdy = ray.d.y, fdz = ray.d.z;
        //float fa = fdx*fdx + fdy*fdy + fdz*fdz;
        //float fb = 2 * (fdx*fox + fdy * foy + fdz*foz);
        //float fc = fox *fox + foy*foy + foz*foz - radius * radius;

        // Initialize _EFloat_ ray coordinate values
        EFloat ox = new EFloat(ray.o.x, oErr.x), oy = new EFloat(ray.o.y, oErr.y), oz = new EFloat(ray.o.z, oErr.z);
        EFloat dx = new EFloat(ray.d.x, dErr.x), dy = new EFloat(ray.d.y, dErr.y), dz = new EFloat(ray.d.z, dErr.z);
        EFloat a = dx.multiply(dx).add(dy.multiply(dy)).add(dz.multiply(dz));
        EFloat b = (new EFloat(2)).multiply(dx.multiply(ox).add(dy.multiply(oy).add(dz.multiply(oz))));
        EFloat c = (ox.multiply(ox)).add(oy.multiply(oy)).add(oz.multiply(oz)).subtract(new EFloat(radius).multiply(new EFloat(radius)));

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

        // Compute sphere hit position and $\phi$
        Point3f pHit = ray.at(tShapeHit.asFloat());

        // Refine sphere intersection point
        pHit = pHit.scale(radius / Point3f.Distance(pHit, new Point3f(0, 0, 0)));
        if (pHit.x == 0 && pHit.y == 0) pHit.x = 1e-5f * radius;
        float phi = (float)Math.atan2(pHit.y, pHit.x);
        if (phi < 0) phi += 2 * Pbrt.Pi;

        // Test sphere intersection against clipping parameters
        if ((zMin > -radius && pHit.z < zMin) || (zMax < radius && pHit.z > zMax) || phi > phiMax) {
            if (tShapeHit == qr.t1) return false;
            if (qr.t1.upperBound() > ray.tMax) return false;
            tShapeHit = qr.t1;
            // Compute sphere hit position and $\phi$
            pHit = ray.at(tShapeHit.asFloat());

            // Refine sphere intersection point
            pHit = pHit.scale(radius / Point3f.Distance(pHit, new Point3f(0, 0, 0)));
            if (pHit.x == 0 && pHit.y == 0) pHit.x = 1e-5f * radius;
            phi = (float)Math.atan2(pHit.y, pHit.x);
            if (phi < 0) phi += 2 * Pbrt.Pi;
            if ((zMin > -radius && pHit.z < zMin) || (zMax < radius && pHit.z > zMax) || phi > phiMax)
                return false;
        }
        return true;
    }

    @Override
    public float Area() {
        return phiMax * radius * (zMax - zMin);
    }

    @Override
    public float Pdf(Interaction ref,  Vector3f wi) {
        Point3f pCenter = ObjectToWorld.xform(new Point3f(0, 0, 0));
        // Return uniform PDF if point is inside sphere
        Point3f pOrigin = Point3f.OffsetRayOrigin(ref.p, ref.pError, ref.n, pCenter.subtract(ref.p));
        if (Point3f.DistanceSquared(pOrigin, pCenter) <= radius * radius)
            return super.Pdf(ref, wi);

        // Compute general sphere PDF
        float sinThetaMax2 = radius * radius / Point3f.DistanceSquared(ref.p, pCenter);
        float cosThetaMax = (float)Math.sqrt(Math.max(0, 1 - sinThetaMax2));
        return Sampling.UniformConePdf(cosThetaMax);
    }

    @Override
    public SampleResult Sample(Point2f u) {
        Point3f pObj = new Point3f(0, 0, 0).add(Sampling.UniformSampleSphere(u).scale(radius));
        Interaction it;
        SampleResult sr = new SampleResult();
        sr.isect = new SurfaceInteraction();
        sr.isect.n = Normal3f.Normalize(ObjectToWorld.xform(new Normal3f(pObj.x, pObj.y, pObj.z)));
        if (reverseOrientation) sr.isect.n = sr.isect.n.negate();
        // Reproject _pObj_ to sphere surface and compute _pObjError_
        pObj = pObj.scale(radius / Point3f.Distance(pObj, new Point3f(0, 0, 0)));
        Vector3f pObjError = (Vector3f.Abs(new Vector3f(pObj))).scale(Pbrt.gamma(5));
        sr.isect.pError = ObjectToWorld.absError(pObjError);
        sr.isect.p = ObjectToWorld.xform(pObj);
        sr.pdf = 1 / Area();
        return sr;
    }

    @Override
    public SampleResult Sample(Interaction ref, Point2f u) {
        Point3f pCenter = ObjectToWorld.xform(new Point3f(0, 0, 0));

        // Sample uniformly on sphere if $\pt{}$ is inside it
        Point3f pOrigin = Point3f.OffsetRayOrigin(ref.p, ref.pError, ref.n, pCenter.subtract(ref.p));
        if (Point3f.DistanceSquared(pOrigin, pCenter) <= radius * radius) {
            SampleResult intr = Sample(u);
            Vector3f wi = intr.isect.p.subtract(ref.p);
            if (wi.LengthSquared() == 0)
                intr.pdf = 0;
            else {
                // Convert from area measure returned by Sample() call above to
                // solid angle measure.
                wi = Vector3f.Normalize(wi);
                intr.pdf *= Point3f.DistanceSquared(ref.p, intr.isect.p) / Normal3f.AbsDot(intr.isect.n, wi.negate());
            }
            if (Float.isInfinite(intr.pdf)) intr.pdf = 0;
            return intr;
        }

        // Compute coordinate system for sphere sampling
        Vector3f wc = Vector3f.Normalize(pCenter.subtract(ref.p));
        Vector3f wcX, wcY;
        Vector3f.CoordSystem cs = Vector3f.CoordinateSystem(wc);
        wcX = cs.v2;
        wcY = cs.v3;

        // Sample sphere uniformly inside subtended cone

        // Compute $\theta$ and $\phi$ values for sample in cone
        float sinThetaMax2 = radius * radius / Point3f.DistanceSquared(ref.p, pCenter);
        float cosThetaMax = (float)Math.sqrt(Math.max(0, 1 - sinThetaMax2));
        float cosTheta = (1 - u.x) + u.x * cosThetaMax;
        float sinTheta = (float)Math.sqrt(Math.max(0, 1 - cosTheta * cosTheta));
        float phi = u.y * 2 * Pbrt.Pi;

        // Compute angle $\alpha$ from center of sphere to sampled point on surface
        float dc = Point3f.Distance(ref.p, pCenter);
        float ds = dc * cosTheta - (float)Math.sqrt(Math.max(0, radius * radius - dc * dc * sinTheta * sinTheta));
        float cosAlpha = (dc * dc + radius * radius - ds * ds) / (2 * dc * radius);
        float sinAlpha = (float)Math.sqrt(Math.max(0, 1 - cosAlpha * cosAlpha));

        // Compute surface normal and sampled point on sphere
        Vector3f nWorld = Vector3f.SphericalDirection(sinAlpha, cosAlpha, phi, wcX.negate(), wcY.negate(), wc.negate());
        Point3f pWorld = pCenter.add((new Point3f(nWorld.x, nWorld.y, nWorld.z)).scale(radius));

        // Return _Interaction_ for sampled point on sphere
        SurfaceInteraction it = new SurfaceInteraction();
        it.p = pWorld;
        it.pError = Vector3f.Abs(new Vector3f(pWorld)).scale(Pbrt.gamma(5));
        it.n = new Normal3f(nWorld);
        if (reverseOrientation) it.n = it.n.negate();

        // Uniform cone PDF.
        SampleResult sr = new SampleResult();
        sr.pdf = 1 / (2 * Pbrt.Pi * (1 - cosThetaMax));
        sr.isect = it;
        return sr;
    }

    public float SolidAngle(Point3f p, int nSamples) {
        Point3f pCenter = ObjectToWorld.xform(new Point3f(0, 0, 0));
        if (Point3f.DistanceSquared(p, pCenter) <= radius * radius)
            return 4 * Pbrt.Pi;
        float sinTheta2 = radius * radius / Point3f.DistanceSquared(p, pCenter);
        float cosTheta = (float)Math.sqrt(Math.max(0, 1 - sinTheta2));
        return (2 * Pbrt.Pi * (1 - cosTheta));
    }

    private final float radius;
    private final float zMin, zMax;
    private final float thetaMin, thetaMax, phiMax;
}