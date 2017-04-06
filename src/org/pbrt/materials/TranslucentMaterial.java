
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

public class TranslucentMaterial extends Material {

    public static Material Create(TextureParams mp) {
        Texture<Spectrum> Kd = mp.GetSpectrumTexture("Kd", new Spectrum(0.25f));
        Texture<Spectrum> Ks = mp.GetSpectrumTexture("Ks", new Spectrum(0.25f));
        Texture<Spectrum> reflect = mp.GetSpectrumTexture("reflect", new Spectrum(0.5f));
        Texture<Spectrum> transmit = mp.GetSpectrumTexture("transmit", new Spectrum(0.5f));
        Texture<Float> roughness = mp.GetFloatTexture("roughness", .1f);
        Texture<Float> bumpMap = mp.GetFloatTextureOrNull("bumpmap");
        boolean remapRoughness = mp.FindBool("remaproughness", true);
        return new TranslucentMaterial(Kd, Ks, roughness, reflect, transmit, bumpMap, remapRoughness);
    }

    public TranslucentMaterial(Texture<Spectrum> kd,
                        Texture<Spectrum> ks,
                        Texture<Float> rough,
                        Texture<Spectrum> refl,
                        Texture<Spectrum> trans,
                        Texture<Float> bump,
                        boolean remap) {
        this.Kd = kd;
        this.Ks = ks;
        this.roughness = rough;
        this.reflect = refl;
        this.transmit = trans;
        this.bumpMap = bump;
        this.remapRoughness = remap;
    }

    @Override
    public void ComputeScatteringFunctions(SurfaceInteraction si, TransportMode mode, boolean allowMultipleLobes) {
        // Perform bump mapping with _bumpMap_, if present
        if (bumpMap != null) Bump(bumpMap, si);
        float eta = 1.5f;
        si.bsdf = new BSDF(si, eta);

        Spectrum r = reflect.Evaluate(si).clamp(0, Pbrt.Infinity);
        Spectrum t = transmit.Evaluate(si).clamp(0, Pbrt.Infinity);
        if (r.isBlack() && t.isBlack()) return;

        Spectrum kd = Kd.Evaluate(si).clamp(0, Pbrt.Infinity);
        if (!kd.isBlack()) {
            if (!r.isBlack())
                si.bsdf.Add(new LambertianReflection(r.multiply(kd)));
            if (!t.isBlack())
                si.bsdf.Add(new LambertianTransmission(t.multiply(kd)));
        }
        Spectrum ks = Ks.Evaluate(si).clamp(0, Pbrt.Infinity);
        if (!ks.isBlack() && (!r.isBlack() || !t.isBlack())) {
            float rough = roughness.Evaluate(si);
            if (remapRoughness)
                rough = TrowbridgeReitzDistribution.RoughnessToAlpha(rough);
            MicrofacetDistribution distrib = new TrowbridgeReitzDistribution(rough, rough, true);
            if (!r.isBlack()) {
                Fresnel fresnel = new FresnelDielectric(1.f, eta);
                si.bsdf.Add(new MicrofacetReflection(r.multiply(ks), distrib, fresnel));
            }
            if (!t.isBlack())
                si.bsdf.Add(new MicrofacetTransmission(t.multiply(ks), distrib, 1.f, eta, mode));
        }
    }

    private Texture<Spectrum> Kd, Ks;
    private Texture<Float> roughness;
    private Texture<Spectrum> reflect, transmit;
    private Texture<Float> bumpMap;
    private boolean remapRoughness;
}