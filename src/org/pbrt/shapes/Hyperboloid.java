
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

public class Hyperboloid extends Shape {

    public Hyperboloid(Transform ObjectToWorld, Transform WorldToObject, boolean reverseOrientation) {
        super(ObjectToWorld, WorldToObject, reverseOrientation);
    }

    public static Shape Create(Transform object2world, Transform world2object, boolean reverseOrientation, ParamSet paramSet) {
        return null;
    }

    @Override
    public Bounds3f ObjectBound() {
        return null;
    }

    @Override
    public HitResult Intersect(Ray ray, boolean testAlphaTexture) {
        return null;
    }

    @Override
    public float Area() {
        return 0;
    }

    @Override
    public SampleResult Sample(Point2f u) {
        return null;
    }
}