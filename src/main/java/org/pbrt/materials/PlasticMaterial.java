
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

public class PlasticMaterial extends Material {

    public static Material Create(TextureParams mp) {
        TextureSpectrum Kd = mp.GetSpectrumTexture("Kd", new Spectrum(0.25f));
        TextureSpectrum Ks = mp.GetSpectrumTexture("Ks", new Spectrum(0.25f));
        TextureFloat roughness = mp.GetFloatTexture("roughness", .1f);
        TextureFloat bumpMap = mp.GetFloatTextureOrNull("bumpmap");
        boolean remapRoughness = mp.FindBool("remaproughness", true);
        return new PlasticMaterial(Kd, Ks, roughness, bumpMap, remapRoughness);
    }

    PlasticMaterial(TextureSpectrum Kd,
                    TextureSpectrum Ks,
                    TextureFloat roughness,
                    TextureFloat bumpMap,
                    boolean remapRoughness) {
        this.Kd = Kd;
        this.Ks = Ks;
        this.roughness = roughness;
        this.bumpMap = bumpMap;
        this.remapRoughness = remapRoughness;
    }

    @Override
    public void ComputeScatteringFunctions(SurfaceInteraction si, TransportMode mode, boolean allowMultipleLobes) {
        // Perform bump mapping with _bumpMap_, if present
        if (bumpMap != null) Bump(bumpMap, si);
        si.bsdf = new BSDF(si, 1);
        // Initialize diffuse component of plastic material
        Spectrum kd = Kd.Evaluate(si).clamp(0, Pbrt.Infinity);
        if (!kd.isBlack())
            si.bsdf.Add(new LambertianReflection(kd));

        // Initialize specular component of plastic material
        Spectrum ks = Ks.Evaluate(si).clamp(0, Pbrt.Infinity);
        if (!ks.isBlack()) {
            Fresnel fresnel = new FresnelDielectric(1.5f, 1.f);
            // Create microfacet distribution _distrib_ for plastic material
            float rough = roughness.Evaluate(si);
            if (remapRoughness)
                rough = TrowbridgeReitzDistribution.RoughnessToAlpha(rough);
            MicrofacetDistribution distrib = new TrowbridgeReitzDistribution(rough, rough, true);
            BxDF spec = new MicrofacetReflection(ks, distrib, fresnel);
            si.bsdf.Add(spec);
        }
    }

    private TextureSpectrum Kd, Ks;
    private TextureFloat roughness, bumpMap;
    private final boolean remapRoughness;

}