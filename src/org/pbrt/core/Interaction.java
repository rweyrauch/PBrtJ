/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

import org.pbrt.openexr.types.Int;

public class Interaction {

    // Interaction Public Data
    public Point3f p = new Point3f();
    public float time = 0.0f;
    public Vector3f pError = new Vector3f();
    public Vector3f wo = new Vector3f();
    public Normal3f n = new Normal3f();
    public MediumInterface mediumInterface = new MediumInterface();

    public Interaction() {
        time = 0.0f;
    }
    public Interaction(Point3f p, Normal3f n, Vector3f pError,
                 Vector3f wo, float time,
                 MediumInterface mediumInterface) {
        this.p = new Point3f(p);
        this.time = time;
        this.pError = new Vector3f(pError);
        this.wo = Vector3f.Normalize(wo);
        this.n = new Normal3f(n);
        this.mediumInterface = (mediumInterface != null) ? new MediumInterface(mediumInterface) : new MediumInterface();
    }
    public Interaction(Point3f p, Vector3f wo, float time, MediumInterface mediumInterface) {
        this.p = new Point3f(p);
        this.time = time;
        this.wo = new Vector3f(wo);
        this.mediumInterface = (mediumInterface != null) ? new MediumInterface(mediumInterface) : new MediumInterface();
    }
    public Interaction(Point3f p, float time, MediumInterface mediumInterface) {
        this.p = new Point3f(p);
        this.time = time;
        this.mediumInterface = (mediumInterface != null) ? new MediumInterface(mediumInterface) : new MediumInterface();
    }

    public Interaction(Interaction it) {
        this(it.p, it.n, it.pError, it.wo, it.time, it.mediumInterface);
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
    public boolean IsMediumInteraction() {
        return !IsSurfaceInteraction();
    }
    public Medium GetMedium(Vector3f w) {
        assert mediumInterface != null;
        assert w != null;
        return Normal3f.Dot(w, n) > 0 ? mediumInterface.outside : mediumInterface.inside;
    }
    public Medium GetMedium() {
        assert (mediumInterface.inside == mediumInterface.outside);
        return mediumInterface.inside;
    }

}