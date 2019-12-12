
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

public class MixTextureSpectrum extends Texture<Spectrum> {

    public MixTextureSpectrum(Texture<Spectrum> tex1, Texture<Spectrum> tex2, Texture<Float> amount) {
        super();
        this.tex1 = tex1;
        this.tex2 = tex2;
        this.amount = amount;
    }

    @Override
    public Spectrum Evaluate(SurfaceInteraction si) {
        Spectrum t1 = tex1.Evaluate(si), t2 = tex2.Evaluate(si);
        float amt = amount.Evaluate(si);
        return t1.scale(1-amt).add(t2.scale(amt));
    }

    public static MixTextureSpectrum CreateSpectrum(Transform tex2world, TextureParams tp) {
        return new MixTextureSpectrum(tp.GetSpectrumTexture("tex1", new Spectrum(0)),
                tp.GetSpectrumTexture("tex2", new Spectrum(1)),
                tp.GetFloatTexture("amount", 0.5f));
    }

    private Texture<Spectrum> tex1, tex2;
    private Texture<Float> amount;
}