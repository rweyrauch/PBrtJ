
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

public class MixTextureFloat extends Texture<Float> {

    public MixTextureFloat(Texture<Float> tex1, Texture<Float> tex2, Texture<Float> amount) {
        super();
        this.tex1 = tex1;
        this.tex2 = tex2;
        this.amount = amount;
    }

    @Override
    public Float Evaluate(SurfaceInteraction si) {
        float t1 = tex1.Evaluate(si), t2 = tex2.Evaluate(si);
        float amt = amount.Evaluate(si);
        return (1-amt) * t1 + amt * t2;
    }

    public static MixTextureFloat CreateFloat(Transform tex2world, TextureParams tp) {
        return new MixTextureFloat(tp.GetFloatTexture("tex1", 0),
                tp.GetFloatTexture("tex2", 1),
                tp.GetFloatTexture("amount", 0.5f));
    }

    private Texture<Float> tex1, tex2;
    private Texture<Float> amount;
}