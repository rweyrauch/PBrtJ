/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class Interaction {

    // Interaction Public Data
    public Point3f p = new Point3f();
    public float time = 0.0f;
    public Vector3f pError = new Vector3f();
    public Vector3f wo = new Vector3f();
    public Normal3f n = new Normal3f();
    public MediumInterface mediumInterface = null;

    public Interaction() {
        time = 0.0f;
    }
    public Interaction(Point3f p, Normal3f n, Vector3f pError,
                 Vector3f wo, float time,
                 MediumInterface mediumInterface) {
        this.p = p;
        this.time = time;
        this.pError = pError;
        this.wo = Vector3f.Normalize(wo);
        this.n = n;
        this.mediumInterface = mediumInterface;
    }

    public boolean IsSurfaceInteraction() { return n.x != 0.0f || n.y != 0.0f || n.z != 0.0f; }
    public Ray SpawnRay(Vector3f d) {
        Point3f o = Point3f.OffsetRayOrigin(p, pError, n, d);
        return new Ray(o, d, Pbrt.Infinity, time, GetMedium(d));
    }
    public Ray SpawnRayTo(Point3f p2) {
        Point3f origin = Point3f.OffsetRayOrigin(p, pError, n, p2.subtract(p));
        Vector3f d = p2.subtract(p);
        return new Ray(origin, d, 1 - Pbrt.ShadowEpsilon, time, GetMedium(d));
    }
    public Ray SpawnRayTo(Interaction it) {
        Point3f origin = Point3f.OffsetRayOrigin(p, pError, n, it.p.subtract(p));
        Point3f target = Point3f.OffsetRayOrigin(it.p, it.pError, it.n, origin.subtract(it.p));
        Vector3f d = target.subtract(origin);
        return new Ray(origin, d, 1 - Pbrt.ShadowEpsilon, time, GetMedium(d));
    }
    public Interaction(Point3f p, Vector3f wo, float time, MediumInterface mediumInterface) {
        this.p = p;
        this.time = time;
        this.wo = wo;
        this.mediumInterface = mediumInterface;
    }
    public Interaction(Point3f p, float time, MediumInterface mediumInterface) {
        this.p = p;
        this.time = time;
        this.mediumInterface = mediumInterface;
    }
    public boolean IsMediumInteraction() {
        return !IsSurfaceInteraction();
    }
    public Medium GetMedium(Vector3f w) {
        return Normal3f.Dot(w, n) > 0 ? mediumInterface.outside : mediumInterface.inside;
    }
    public Medium GetMedium() {
        assert (mediumInterface.inside == mediumInterface.outside);
        return mediumInterface.inside;
    }

}