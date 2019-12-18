
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

public class ScaleTextureSpectrum extends TextureSpectrum {

    ScaleTextureSpectrum(TextureSpectrum tex1, TextureSpectrum tex2) {
        super();
        this.tex1 = tex1;
        this.tex2 = tex2;
    }

    @Override
    public Spectrum Evaluate(SurfaceInteraction si) {
        return tex1.Evaluate(si).multiply(tex2.Evaluate(si));
    }

    public static TextureSpectrum CreateSpectrum(Transform tex2world, TextureParams tp) {
        return new ScaleTextureSpectrum(tp.GetSpectrumTexture("tex1", new Spectrum(1)),
                tp.GetSpectrumTexture("tex2", new Spectrum(1)));
    }

    private TextureSpectrum tex1, tex2;
}