
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public abstract class TextureMapping3D {

    public static class MapPoint {
        public Point3f p;
        public Vector3f dpdx, dpdy;
    }

    public abstract MapPoint Map(SurfaceInteraction si);

}