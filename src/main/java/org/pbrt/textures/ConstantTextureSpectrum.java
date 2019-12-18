
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

public class ConstantTextureSpectrum extends TextureSpectrum {

    public static ConstantTextureSpectrum CreateSpectrum(Transform tex2world, TextureParams tp) {
        return new ConstantTextureSpectrum(tp.FindSpectrum("value", new Spectrum(1.f)));
    }

    public ConstantTextureSpectrum(Spectrum value) {
        this.value = value;
    }

    public Spectrum Evaluate(SurfaceInteraction si) { return value; }

    private final Spectrum value;
}