
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

public class ScaleTextureFloat extends Texture<Float> {

    ScaleTextureFloat(Texture<Float> tex1, Texture<Float> tex2) {
        super();
        this.tex1 = tex1;
        this.tex2 = tex2;
    }

    @Override
    public Float Evaluate(SurfaceInteraction si) {
        return tex1.Evaluate(si) * tex2.Evaluate(si);
    }

    public static Texture<Float> CreateFloat(Transform tex2world, TextureParams tp) {
        return new ScaleTextureFloat(tp.GetFloatTexture("tex1", 1),
                tp.GetFloatTexture("tex2", 1));
    }

    private Texture<Float> tex1, tex2;
}