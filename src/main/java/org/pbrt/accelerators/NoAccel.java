
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.accelerators;

import org.pbrt.core.*;

public class NoAccel extends Aggregate {

    public NoAccel(Primitive[] prims) {
        this.primitives = prims;

        this.bounds = new Bounds3f();
        for (Primitive prim : this.primitives) {
            this.bounds = Bounds3f.Union(this.bounds, prim.WorldBound());
        }
    }

    public static Primitive Create(Primitive[] prims, ParamSet paramSet) {
        return new NoAccel(prims);
    }

    @Override
    public Bounds3f WorldBound() {
        return bounds;
    }

    @Override
    public SurfaceInteraction Intersect(Ray r) {
        SurfaceInteraction isect = null;
        for (Primitive prim : primitives) {
            isect = prim.Intersect(r);
            if (isect != null) return isect;
        }
        return null;
    }

    @Override
    public boolean IntersectP(Ray r) {
        boolean hit = false;
        for (Primitive prim : primitives) {
            hit = prim.IntersectP(r);
            if (hit) break;
        }
        return hit;
    }

    private Primitive[] primitives;
    private Bounds3f bounds;
}