
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

public class GlassMaterial extends Material {

    public static Material Create(TextureParams mp) {
        TextureSpectrum Kr = mp.GetSpectrumTexture("Kr", new Spectrum(1));
        TextureSpectrum Kt = mp.GetSpectrumTexture("Kt", new Spectrum(1));
        TextureFloat eta = mp.GetFloatTextureOrNull("eta");
        if (eta == null) eta = mp.GetFloatTexture("index", 1.5f);
        TextureFloat roughu = mp.GetFloatTexture("uroughness", 0);
        TextureFloat roughv = mp.GetFloatTexture("vroughness", 0);
        TextureFloat bumpMap = mp.GetFloatTextureOrNull("bumpmap");
        boolean remapRoughness = mp.FindBool("remaproughness", true);
        return new GlassMaterial(Kr, Kt, roughu, roughv, eta, bumpMap, remapRoughness);
    }

    public GlassMaterial(TextureSpectrum Kr,
                  TextureSpectrum Kt,
                  TextureFloat uRoughness,
                  TextureFloat vRoughness,
                  TextureFloat index,
                  TextureFloat bumpMap,
                  boolean remapRoughness) {
        this.Kr = Kr;
        this.Kt = Kt;
        this.uRoughness = uRoughness;
        this.vRoughness = vRoughness;
        this.index = index;
        this.bumpMap = bumpMap;
        this.remapRoughness = remapRoughness;
    }

    @Override
    public void ComputeScatteringFunctions(SurfaceInteraction si, TransportMode mode, boolean allowMultipleLobes) {
        // Perform bump mapping with _bumpMap_, if present
        if (bumpMap != null) Bump(bumpMap, si);
        float eta = index.Evaluate(si);
        float urough = uRoughness.Evaluate(si);
        float vrough = vRoughness.Evaluate(si);
        Spectrum R = Kr.Evaluate(si).clamp(0, Pbrt.Infinity);
        Spectrum T = Kt.Evaluate(si).clamp(0, Pbrt.Infinity);
        // Initialize _bsdf_ for smooth or rough dielectric
        si.bsdf = new BSDF(si, eta);

        if (R.isBlack() && T.isBlack()) return;

        boolean isSpecular = urough == 0 && vrough == 0;
        if (isSpecular && allowMultipleLobes) {
            si.bsdf.Add(new FresnelSpecular(R, T, 1.f, eta, mode));
        }
        else {
            if (remapRoughness) {
                urough = TrowbridgeReitzDistribution.RoughnessToAlpha(urough);
                vrough = TrowbridgeReitzDistribution.RoughnessToAlpha(vrough);
            }
            MicrofacetDistribution distrib = isSpecular ? null : new TrowbridgeReitzDistribution(urough, vrough, true);
            if (!R.isBlack()) {
                Fresnel fresnel = new FresnelDielectric(1.f, eta);
                if (isSpecular)
                    si.bsdf.Add(new SpecularReflection(R, fresnel));
            else
                si.bsdf.Add(new MicrofacetReflection(R, distrib, fresnel));
            }
            if (!T.isBlack()) {
                if (isSpecular)
                    si.bsdf.Add(new SpecularTransmission(T, 1.f, eta, mode));
            else
                si.bsdf.Add(new MicrofacetTransmission(T, distrib, 1.f, eta, mode));
            }
        }
    }

    private TextureSpectrum Kr, Kt;
    private TextureFloat uRoughness, vRoughness;
    private TextureFloat index;
    private TextureFloat bumpMap;
    boolean remapRoughness;

}