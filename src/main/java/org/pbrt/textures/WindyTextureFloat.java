
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

public class WindyTextureFloat extends Texture<Float> {

    public WindyTextureFloat(TextureMapping3D mapping) {
        this.mapping = mapping;
    }

    @Override
    public Float Evaluate(SurfaceInteraction si) {
        TextureMapping3D.MapPoint point = mapping.Map(si);
        float windStrength = FBm(point.p.scale(0.1f), point.dpdx.scale(0.1f), point.dpdy.scale(0.1f), 0.5f, 3);
        float waveHeight = FBm(point.p, point.dpdx, point.dpdy, 0.5f, 6);
        return Math.abs(windStrength) * waveHeight;
    }

    public static Texture<Float> CreateFloat(Transform tex2world, TextureParams tp) {
        // Initialize 3D texture mapping _map_ from _tp_
        TextureMapping3D map = new IdentityMapping3D(tex2world);
        return new WindyTextureFloat(map);
    }

    private TextureMapping3D mapping;
}