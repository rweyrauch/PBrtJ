
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

public class BVHAccel extends Aggregate {

    public enum SplitMethod {
        SAH,
        HLBVH,
        Middle,
        EqualCounts
    }

    public BVHAccel(Primitive[] prims, int maxPrimsInNode, SplitMethod splitMethod) {

    }
    public BVHAccel(Primitive[] prims) {
        this(prims, 1, SplitMethod.SAH);
    }

    @Override
    public Bounds3f WorldBound() {
        return null;
    }

    @Override
    public SurfaceInteraction Intersect(Ray r) {
        return null;
    }

    @Override
    public boolean IntersectP(Ray r) {
        return false;
    }

    public static Primitive Create(Primitive[] prims, ParamSet paramSet) {
        return null;
    }
}