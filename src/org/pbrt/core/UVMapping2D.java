
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class UVMapping2D extends TextureMapping2D {

    public UVMapping2D() {
        this(1, 1, 0, 0);
    }
    public UVMapping2D(float su, float sv, float du, float dv) {
        super();
        this.su = su;
        this.sv = sv;
        this.du = du;
        this.dv = dv;
    }

    public MapPoint Map(SurfaceInteraction si) {
        MapPoint result = new MapPoint();
        // Compute texture differentials for 2D identity mapping
        result.dstdx = new Vector2f(su * si.dudx, sv * si.dvdx);
        result.dstdy = new Vector2f(su * si.dudy, sv * si.dvdy);
        result.point = new Point2f(su * si.uv.x + du, sv * si.uv.y + dv);
        return result;
    }

    private final float su, sv, du, dv;
}