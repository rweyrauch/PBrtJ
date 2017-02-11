
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

public class MitchellFilter extends Filter {

    private float B, C;

    public MitchellFilter(Vector2f radius, float B, float C) {
        super(radius);
        this.B = B;
        this.C = C;
    }

    @Override
    public float Evaluate(Point2f p) {
        return Mitchell1D(p.x * invRadius.x) * Mitchell1D(p.y * invRadius.y);
    }

    public float Mitchell1D(float x) {
        x = Math.abs(2 * x);
        if (x > 1)
            return ((-B - 6 * C) * x * x * x + (6 * B + 30 * C) * x * x +
                    (-12 * B - 48 * C) * x + (8 * B + 24 * C)) *
                    (1.f / 6.f);
        else
            return ((12 - 9 * B - 6 * C) * x * x * x +
                    (-18 + 12 * B + 6 * C) * x * x + (6 - 2 * B)) *
                    (1.f / 6.f);
    }

    public static Filter Create(ParamSet paramSet) {
        // Find common filter parameters
        float xw = paramSet.FindOneFloat("xwidth", 2);
        float yw = paramSet.FindOneFloat("ywidth", 2);
        float B = paramSet.FindOneFloat("B", 1.f / 3.f);
        float C = paramSet.FindOneFloat("C", 1.f / 3.f);
        return new MitchellFilter(new Vector2f(xw, yw), B, C);
    }

}