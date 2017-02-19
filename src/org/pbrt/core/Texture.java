
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public abstract class Texture<T> {
    public abstract T Evaluate(SurfaceInteraction si);

    public static float Lanczos(float x, float tau) {
        x = Math.abs(x);
        if (x < 1e-5f) return 1;
        if (x > 1.f) return 0;
        x *= (float)Math.PI;
        float s = (float)Math.sin(x * tau) / (x * tau);
        float lanczos = (float)Math.sin(x) / x;
        return s * lanczos;
    }
}