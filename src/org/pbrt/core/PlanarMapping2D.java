
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class PlanarMapping2D extends TextureMapping2D {

    public PlanarMapping2D(Vector3f vs, Vector3f vt) {
        this(vs, vt, 0, 0);
    }
    public PlanarMapping2D(Vector3f vs, Vector3f vt, float ds, float dt) {
        super();
        this.vs = vs;
        this.vt = vt;
        this.ds = ds;
        this.dt = dt;
    }

    public MapPoint Map(SurfaceInteraction si) {
        MapPoint result = new MapPoint();
        Vector3f vec = new Vector3f(si.p);
        result.dstdx = new Vector2f(Vector3f.Dot(si.dpdx, vs), Vector3f.Dot(si.dpdx, vt));
        result.dstdy = new Vector2f(Vector3f.Dot(si.dpdy, vs), Vector3f.Dot(si.dpdy, vt));
        result.st = new Point2f(ds + Vector3f.Dot(vec, vs), dt + Vector3f.Dot(vec, vt));
        return result;
    }

    private final Vector3f vs, vt;
    private final float ds, dt;
}