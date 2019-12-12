
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

public class MatteMaterial extends Material {

    public MatteMaterial(Texture<Spectrum> Kd, Texture<Float> sigma, Texture<Float> bumpMap) {
        this.Kd = Kd;
        this.sigma = sigma;
        this.bumpMap = bumpMap;
    }

    @Override
    public void ComputeScatteringFunctions(SurfaceInteraction si, TransportMode mode, boolean allowMultipleLobes) {
        // Perform bump mapping with _bumpMap_, if present
        if (bumpMap != null) Bump(bumpMap, si);

        // Evaluate textures for _MatteMaterial_ material and allocate BRDF
        si.bsdf = new BSDF(si, 1);
        Spectrum r = (Kd.Evaluate(si)).clamp(0, Pbrt.Infinity);
        float sig = Pbrt.Clamp(sigma.Evaluate(si), 0.0f, 90.0f);
        if (!r.isBlack()) {
            if (sig == 0)
                si.bsdf.Add(new LambertianReflection(r));
        else
            si.bsdf.Add(new OrenNayar(r, sig));
        }
    }

    public static Material Create(TextureParams mp) {
        Texture<Spectrum> Kd = mp.GetSpectrumTexture("Kd", new Spectrum(0.5f));
        Texture<Float> sigma = mp.GetFloatTexture("sigma", 0.f);
        Texture<Float> bumpMap = mp.GetFloatTextureOrNull("bumpmap");
        return new MatteMaterial(Kd, sigma, bumpMap);
    }

    private Texture<Spectrum> Kd;
    private Texture<Float> sigma, bumpMap;
}