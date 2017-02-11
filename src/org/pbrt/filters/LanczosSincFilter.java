
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

public class LanczosSincFilter extends Filter {

    private float tau;

    public LanczosSincFilter(Vector2f radius, float tau) {
        super(radius);
        this.tau = tau;
    }

    @Override
    public float Evaluate(Point2f p) {
        return WindowedSinc(p.x, radius.x) * WindowedSinc(p.y, radius.y);
    }

    public float Sinc(float x) {
        x = Math.abs(x);
        if (x < 1e-5) return 1;
        return (float)(Math.sin(Math.PI * x) / (Math.PI * x));
    }
    public float WindowedSinc(float x, float radius) {
        x = Math.abs(x);
        if (x > radius) return 0;
        float lanczos = Sinc(x / tau);
        return Sinc(x) * lanczos;
    }

    public static Filter Create(ParamSet paramSet) {
        float xw = paramSet.FindOneFloat("xwidth", 4);
        float yw = paramSet.FindOneFloat("ywidth", 4);
        float tau = paramSet.FindOneFloat("tau", 3);
        return new LanczosSincFilter(new Vector2f(xw, yw), tau);
    }

}