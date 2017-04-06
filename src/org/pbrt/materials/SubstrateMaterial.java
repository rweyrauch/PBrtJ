
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

public class SubstrateMaterial extends Material {

    public static Material Create(TextureParams mp) {
        Texture<Spectrum> Kd = mp.GetSpectrumTexture("Kd", new Spectrum(.5f));
        Texture<Spectrum> Ks = mp.GetSpectrumTexture("Ks", new Spectrum(.5f));
        Texture<Float> uroughness = mp.GetFloatTexture("uroughness", .1f);
        Texture<Float> vroughness = mp.GetFloatTexture("vroughness", .1f);
        Texture<Float> bumpMap = mp.GetFloatTextureOrNull("bumpmap");
        boolean remapRoughness = mp.FindBool("remaproughness", true);
        return new SubstrateMaterial(Kd, Ks, uroughness, vroughness, bumpMap, remapRoughness);
    }

    public SubstrateMaterial(Texture<Spectrum> Kd, Texture<Spectrum> Ks,
                      Texture<Float> nu, Texture<Float> nv,
                      Texture<Float> bumpMap, boolean remapRoughness) {
        this.Kd = Kd;
        this.Ks = Ks;
        this.nu = nu;
        this.nv = nv;
        this.bumpMap = bumpMap;
        this.remapRoughness = remapRoughness;
    }

    @Override
    public void ComputeScatteringFunctions(SurfaceInteraction si, TransportMode mode, boolean allowMultipleLobes) {
        // Perform bump mapping with _bumpMap_, if present
        if (bumpMap != null) Bump(bumpMap, si);
        si.bsdf = new BSDF(si, 1);
        Spectrum d = Kd.Evaluate(si).clamp(0, Pbrt.Infinity);
        Spectrum s = Ks.Evaluate(si).clamp(0, Pbrt.Infinity);
        float roughu = nu.Evaluate(si);
        float roughv = nv.Evaluate(si);

        if (!d.isBlack() || !s.isBlack()) {
            if (remapRoughness) {
                roughu = TrowbridgeReitzDistribution.RoughnessToAlpha(roughu);
                roughv = TrowbridgeReitzDistribution.RoughnessToAlpha(roughv);
            }
            MicrofacetDistribution distrib = new TrowbridgeReitzDistribution(roughu, roughv, true);
            si.bsdf.Add(new FresnelBlend(d, s, distrib));
        }
    }

    private Texture<Spectrum> Kd, Ks;
    private Texture<Float> nu, nv;
    private Texture<Float> bumpMap;
    private boolean remapRoughness;

}