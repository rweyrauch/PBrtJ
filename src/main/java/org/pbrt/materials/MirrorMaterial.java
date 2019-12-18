
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.materials;

import org.pbrt.core.*;

public class MirrorMaterial extends Material {

    public static Material Create(TextureParams mp) {
        TextureSpectrum Kr = mp.GetSpectrumTexture("Kr", new Spectrum(0.9f));
        TextureFloat bumpMap = mp.GetFloatTextureOrNull("bumpmap");
        return new MirrorMaterial(Kr, bumpMap);
    }

    public MirrorMaterial(TextureSpectrum r, TextureFloat bump) {
        this.Kr = r;
        this.bumpMap = bump;
    }

    @Override
    public void ComputeScatteringFunctions(SurfaceInteraction si, TransportMode mode, boolean allowMultipleLobes) {
        // Perform bump mapping with _bumpMap_, if present
        if (bumpMap != null) Bump(bumpMap, si);
        si.bsdf = new BSDF(si, 1);
        Spectrum R = Kr.Evaluate(si).clamp(0, Pbrt.Infinity);
        if (!R.isBlack())
            si.bsdf.Add(new SpecularReflection(R, new FresnelNoOp()));
    }

    private TextureSpectrum Kr;
    private TextureFloat bumpMap;
}