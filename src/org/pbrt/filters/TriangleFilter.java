
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.filters;

import org.pbrt.core.Filter;
import org.pbrt.core.ParamSet;
import org.pbrt.core.Point2f;
import org.pbrt.core.Vector2f;

public class TriangleFilter extends Filter {

    public TriangleFilter(Vector2f radius) {
        super(radius);
    }

    @Override
    public float Evaluate(Point2f p) {
        return Math.max(0, radius.x - Math.abs(p.x)) * Math.max(0, radius.y - Math.abs(p.y));
    }

    public static Filter Create(ParamSet paramSet) {
        // Find common filter parameters
        float xw = paramSet.FindOneFloat("xwidth", 2);
        float yw = paramSet.FindOneFloat("ywidth", 2);
        return new TriangleFilter(new Vector2f(xw, yw));
    }

}