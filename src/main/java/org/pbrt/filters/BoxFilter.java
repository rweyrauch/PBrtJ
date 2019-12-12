
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

public class BoxFilter extends Filter {

    public BoxFilter(Vector2f radius) {
        super(radius);
    }

    @Override
    public float Evaluate(Point2f p) {
        return 1;
    }

    public static Filter Create(ParamSet paramSet) {
        float xw = paramSet.FindOneFloat("xwidth", 0.5f);
        float yw = paramSet.FindOneFloat("ywidth", 0.5f);
        return new BoxFilter(new Vector2f(xw, yw));
    }
}