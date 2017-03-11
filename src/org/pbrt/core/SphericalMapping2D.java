
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class SphericalMapping2D extends TextureMapping2D {

    public SphericalMapping2D(Transform worldToTexture) {
        super();
        this.worldToTexture = new Transform(worldToTexture);
    }

    public MapPoint Map(SurfaceInteraction si) {
        MapPoint result = new MapPoint();
        Point2f st = sphere(si.p);
        // Compute texture coordinate differentials for sphere $(u,v)$ mapping
        float delta = .1f;
        Point2f stDeltaX = sphere(si.p.add(si.dpdx.scale(delta)));
        result.dstdx = (stDeltaX.subtract(st)).invScale(delta);
        Point2f stDeltaY = sphere(si.p.add(si.dpdy.scale(delta)));
        result.dstdy = (stDeltaY.subtract(st)).invScale(delta);

        // Handle sphere mapping discontinuity for coordinate differentials
        if (result.dstdx.y > .5)
            result.dstdx.y = 1 - result.dstdx.y;
        else if (result.dstdx.y < -.5f)
            result.dstdx.y = -(result.dstdx.y + 1);
        if (result.dstdx.y > .5)
            result.dstdx.y = 1 - result.dstdx.y;
        else if (result.dstdx.y < -.5f)
            result.dstdx.y = -(result.dstdx.y + 1);
        result.st = st;

        return result;
    }

    private Point2f sphere(Point3f p) {
        Vector3f vec = Vector3f.Normalize(worldToTexture.xform(p).subtract(new Point3f(0, 0, 0)));
        float theta = Vector3f.SphericalTheta(vec), phi = Vector3f.SphericalPhi(vec);
        return new Point2f(theta * Pbrt.InvPi, phi * Pbrt.Inv2Pi );
    }
    private final Transform worldToTexture;
}