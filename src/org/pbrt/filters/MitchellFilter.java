
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

public class MitchellFilter extends Filter {

    public static Filter Create(ParamSet paramSet) {
        return null;
    }

    @Override
    public float Evaluate(Point2f p) {
        return 0;
    }
}