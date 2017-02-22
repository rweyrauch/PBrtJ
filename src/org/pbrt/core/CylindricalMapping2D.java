
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class CylindricalMapping2D extends TextureMapping2D {

    public CylindricalMapping2D(Transform worldToTexture) {
        super();
        this.worldToTexture = worldToTexture;
    }

    public MapPoint Map(SurfaceInteraction si) {
        MapPoint result = new MapPoint();
        Point2f st = cylinder(si.p);
        // Compute texture coordinate differentials for cylinder $(u,v)$ mapping
        float delta = .01f;
        Point2f stDeltaX = cylinder(si.p.add(si.dpdx.scale(delta)));
        result.dstdx = (stDeltaX.subtract(st)).invScale(delta);
        if (result.dstdx.y > .5f)
            result.dstdx.y = 1 - result.dstdx.y;
        else if (result.dstdx.y < -.5f)
            result.dstdx.y = -(result.dstdx.y + 1);
        Point2f stDeltaY = cylinder(si.p.add(si.dpdy.scale(delta)));
        result.dstdy = (stDeltaY.subtract(st)).invScale(delta);
        if (result.dstdy.y > .5f)
            result.dstdy.y = 1 - result.dstdy.y;
        else if (result.dstdy.y < -.5f)
            result.dstdy.y = -(result.dstdy.y + 1);
        result.st = st;
        return result;
    }

    private Point2f cylinder(Point3f p) {
        Vector3f vec = Vector3f.Normalize(worldToTexture.xform(p).subtract(new Point3f(0, 0, 0)));
        return new Point2f(((float)Math.PI + (float)Math.atan2(vec.y, vec.x)) / (float)(Math.PI*2), vec.z);
    }

    private final Transform worldToTexture;
}