
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public abstract class TextureMapping2D {

    public static class MapPoint {
        public Point2f st;
        public Vector2f dstdx, dstdy;
    }

    public abstract MapPoint Map(SurfaceInteraction si);

}