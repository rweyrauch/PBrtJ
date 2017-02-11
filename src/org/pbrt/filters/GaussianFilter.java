
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

public class GaussianFilter extends Filter {

    private float alpha;
    private float expX, expY;

    public GaussianFilter(Vector2f radius, float alpha) {
        super(radius);
        this.alpha = alpha;
        this.expX = (float)Math.exp(-alpha * radius.x * radius.x);
        this.expY = (float)Math.exp(-alpha * radius.y * radius.y);
    }

    @Override
    public float Evaluate(Point2f p) {
        return Gaussian(p.x, expX) * Gaussian(p.y, expY);
    }

    public static Filter Create(ParamSet paramSet) {
        // Find common filter parameters
        float xw = paramSet.FindOneFloat("xwidth", 2);
        float yw = paramSet.FindOneFloat("ywidth", 2);
        float alpha = paramSet.FindOneFloat("alpha", 2);
        return new GaussianFilter(new Vector2f(xw, yw), alpha);
    }

    private float Gaussian(float d, float expv) {
        return Math.max(0, (float)Math.exp(-alpha * d * d) - expv);
    }
}