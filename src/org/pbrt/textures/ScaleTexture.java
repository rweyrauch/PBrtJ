
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

public class ScaleTexture<T> extends Texture {

    @Override
    public Object Evaluate(SurfaceInteraction si) {
        return null;
    }

    public static Texture<Float> CreateFloat(Transform tex2world, TextureParams tp) {
        return null;
    }

    public static Texture<Spectrum> CreateSpectrum(Transform tex2world, TextureParams tp) {
        return null;
    }
}