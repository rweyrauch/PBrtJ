
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

public class UberMaterial extends Material {

    public static UberMaterial Create(TextureParams mp) {
        Texture<Spectrum> Kd = mp.GetSpectrumTexture("Kd", new Spectrum(0.25f));
        Texture<Spectrum> Ks = mp.GetSpectrumTexture("Ks", new Spectrum(0.25f));
        Texture<Spectrum> Kr = mp.GetSpectrumTexture("Kr", new Spectrum(0));
        Texture<Spectrum> Kt = mp.GetSpectrumTexture("Kt", new Spectrum(0));
        Texture<Float> roughness = mp.GetFloatTexture("roughness", .1f);
        Texture<Float> uroughness = mp.GetFloatTextureOrNull("uroughness");
        Texture<Float> vroughness = mp.GetFloatTextureOrNull("vroughness");
        Texture<Float> eta = mp.GetFloatTextureOrNull("eta");
        if (eta == null) eta = mp.GetFloatTexture("index", 1.5f);
        Texture<Spectrum> opacity = mp.GetSpectrumTexture("opacity", new Spectrum(1));
        Texture<Float> bumpMap = mp.GetFloatTextureOrNull("bumpmap");
        boolean remapRoughness = mp.FindBool("remaproughness", true);
        return new UberMaterial(Kd, Ks, Kr, Kt, roughness, uroughness, vroughness,
                opacity, eta, bumpMap, remapRoughness);
    }

    public UberMaterial(Texture<Spectrum> Kd, Texture<Spectrum> Ks, Texture<Spectrum> Kr, Texture<Spectrum> Kt,
                        Texture<Float> roughness, Texture<Float> roughnessu, Texture<Float> roughnessv,
                        Texture<Spectrum> opacity, Texture<Float> eta, Texture<Float> bumMap, boolean remapRoughness) {
        this.Kd = Kd;
        this.Ks = Ks;
        this.Kr = Kr;
        this.Kt = Kt;
        this.opacity = opacity;
        this.roughness = roughness;
        this.roughnessu = roughnessu;
        this.roughnessv = roughnessv;
        this.eta = eta;
        this.bumpMap = bumMap;
        this.remapRoughness = remapRoughness;
    }

    @Override
    public void ComputeScatteringFunctions(SurfaceInteraction si, TransportMode mode, boolean allowMultipleLobes) {
        // Perform bump mapping with _bumpMap_, if present
        if (bumpMap != null) Bump(bumpMap, si);
        float e = eta.Evaluate(si);

        Spectrum op = opacity.Evaluate(si).clamp(0, Pbrt.Infinity);
        Spectrum t = Spectrum.Add(op.negate(), new Spectrum(1)).clamp(0, Pbrt.Infinity);
        if (!t.isBlack()) {
            si.bsdf = new BSDF(si, 1);
            BxDF tr = new SpecularTransmission(t, 1, 1, mode);
            si.bsdf.Add(tr);
        } else
            si.bsdf = new BSDF(si, e);

        Spectrum kd = Spectrum.Multiply(op, Kd.Evaluate(si).clamp(0, Pbrt.Infinity));
        if (!kd.isBlack()) {
            BxDF diff = new LambertianReflection(kd);
            si.bsdf.Add(diff);
        }

        Spectrum ks = Spectrum.Multiply(op, Ks.Evaluate(si).clamp(0, Pbrt.Infinity));
        if (!ks.isBlack()) {
            Fresnel fresnel = new FresnelDielectric(1, e);
            float roughu, roughv;
            if (roughnessu != null)
                roughu = roughnessu.Evaluate(si);
        else
            roughu = roughness.Evaluate(si);
            if (roughnessv != null)
                roughv = roughnessv.Evaluate(si);
        else
            roughv = roughu;
            if (remapRoughness) {
                roughu = TrowbridgeReitzDistribution.RoughnessToAlpha(roughu);
                roughv = TrowbridgeReitzDistribution.RoughnessToAlpha(roughv);
            }
            MicrofacetDistribution distrib = new TrowbridgeReitzDistribution(roughu, roughv, true);
            BxDF spec = new MicrofacetReflection(ks, distrib, fresnel);
            si.bsdf.Add(spec);
        }

        Spectrum kr = Spectrum.Multiply(op, Kr.Evaluate(si).clamp(0, Pbrt.Infinity));
        if (!kr.isBlack()) {
            Fresnel fresnel = new FresnelDielectric(1, e);
            si.bsdf.Add(new SpecularReflection(kr, fresnel));
        }

        Spectrum kt = Spectrum.Multiply(op, Kt.Evaluate(si).clamp(0, Pbrt.Infinity));
        if (!kt.isBlack())
            si.bsdf.Add(new SpecularTransmission(kt, 1, e, mode));
    }

    private Texture<Spectrum> Kd, Ks, Kr, Kt, opacity;
    private Texture<Float> roughness, roughnessu, roughnessv, eta, bumpMap;
    private boolean remapRoughness;
}