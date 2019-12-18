
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.textures;

import org.pbrt.core.*;

public class FBmTextureFloat extends TextureFloat {

    public FBmTextureFloat(TextureMapping3D mapping, int octaves, float omega) {
        this.mapping = mapping;
        this.omega = omega;
        this.octaves = octaves;
    }

    @Override
    public float Evaluate(SurfaceInteraction si) {
        TextureMapping3D.MapPoint point = mapping.Map(si);
        return Texture.FBm(point.p, point.dpdx, point.dpdy, omega, octaves);
    }

    public static TextureFloat CreateFloat(Transform tex2world, TextureParams tp) {
        // Initialize 3D texture mapping _map_ from _tp_
        TextureMapping3D map = new IdentityMapping3D(tex2world);
        return new FBmTextureFloat(map, tp.FindInt("octaves", 8),
                tp.FindFloat("roughness", .5f));
    }

    private TextureMapping3D mapping;
    private int octaves;
    private float omega;
}