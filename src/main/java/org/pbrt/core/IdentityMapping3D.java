
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class IdentityMapping3D extends TextureMapping3D {

    public IdentityMapping3D(Transform worldToTexture) {
        this.worldToTexture = new Transform(worldToTexture);
    }

    public MapPoint Map(SurfaceInteraction si){
        MapPoint point = new MapPoint();
        point.p = worldToTexture.xform(si.p);
        point.dpdx = worldToTexture.xform(si.dpdx);
        point.dpdy = worldToTexture.xform(si.dpdy);
        return point;
    }

    private final Transform worldToTexture;
}