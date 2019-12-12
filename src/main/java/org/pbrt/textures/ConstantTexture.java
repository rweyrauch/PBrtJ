
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

public class ConstantTexture<T> extends Texture<T> {

    public static ConstantTexture<Float> CreateFloat(Transform tex2world, TextureParams tp) {
        return new ConstantTexture<>(tp.FindFloat("value", 1.0f));
    }
    public static ConstantTexture<Spectrum> CreateSpectrum(Transform tex2world, TextureParams tp) {
        return new ConstantTexture<>(tp.FindSpectrum("value", new Spectrum(1.f)));
    }

    public ConstantTexture(T value) {
        this.value = value;
    }

    public T Evaluate(SurfaceInteraction si) { return value; }

    private final T value;
}