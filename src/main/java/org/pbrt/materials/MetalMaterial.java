
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

public class MetalMaterial extends Material {

    private static final Spectrum copperN, copperK;

    public static Material Create(TextureParams mp) {
        Texture<Spectrum> eta = mp.GetSpectrumTexture("eta", copperN);
        Texture<Spectrum> k = mp.GetSpectrumTexture("k", copperK);
        Texture<Float> roughness = mp.GetFloatTexture("roughness", .01f);
        Texture<Float> uRoughness = mp.GetFloatTextureOrNull("uroughness");
        Texture<Float> vRoughness = mp.GetFloatTextureOrNull("vroughness");
        Texture<Float> bumpMap = mp.GetFloatTextureOrNull("bumpmap");
        boolean remapRoughness = mp.FindBool("remaproughness", true);
        return new MetalMaterial(eta, k, roughness, uRoughness, vRoughness, bumpMap, remapRoughness);
    }

    public MetalMaterial(Texture<Spectrum> eta,
                  Texture<Spectrum> k,
                  Texture<Float> rough,
                  Texture<Float> urough,
                  Texture<Float> vrough,
                  Texture<Float> bump,
                  boolean remapRoughness) {
        this.eta = eta;
        this.k = k;
        this.roughness = rough;
        this.uRoughness = urough;
        this.vRoughness = vrough;
        this.bumpMap = bump;
        this.remapRoughness = remapRoughness;
    }
    
    @Override
    public void ComputeScatteringFunctions(SurfaceInteraction si, TransportMode mode, boolean allowMultipleLobes) {
        // Perform bump mapping with _bumpMap_, if present
        if (bumpMap != null) Bump(bumpMap, si);
        si.bsdf = new BSDF(si, 1);

        float uRough = (uRoughness != null) ? uRoughness.Evaluate(si) : roughness.Evaluate(si);
        float vRough = (vRoughness != null) ? vRoughness.Evaluate(si) : roughness.Evaluate(si);
        if (remapRoughness) {
            uRough = TrowbridgeReitzDistribution.RoughnessToAlpha(uRough);
            vRough = TrowbridgeReitzDistribution.RoughnessToAlpha(vRough);
        }
        Fresnel frMf = new FresnelConductor(new Spectrum(1), eta.Evaluate(si), k.Evaluate(si));
        MicrofacetDistribution distrib = new TrowbridgeReitzDistribution(uRough, vRough, true);
        si.bsdf.Add(new MicrofacetReflection(new Spectrum(1), distrib, frMf));
    }

    Texture<Spectrum> eta, k;
    Texture<Float> roughness, uRoughness, vRoughness;
    Texture<Float> bumpMap;
    boolean remapRoughness;

    private final static int CopperSamples = 56;
    private final static float[] CopperWavelengths = {
        298.7570554f, 302.4004341f, 306.1337728f, 309.960445f,  313.8839949f,
                317.9081487f, 322.036826f,  326.2741526f, 330.6244747f, 335.092373f,
                339.6826795f, 344.4004944f, 349.2512056f, 354.2405086f, 359.374429f,
                364.6593471f, 370.1020239f, 375.7096303f, 381.4897785f, 387.4505563f,
                393.6005651f, 399.9489613f, 406.5055016f, 413.2805933f, 420.2853492f,
                427.5316483f, 435.0322035f, 442.8006357f, 450.8515564f, 459.2006593f,
                467.8648226f, 476.8622231f, 486.2124627f, 495.936712f,  506.0578694f,
                516.6007417f, 527.5922468f, 539.0616435f, 551.0407911f, 563.5644455f,
                576.6705953f, 590.4008476f, 604.8008683f, 619.92089f,   635.8162974f,
                652.5483053f, 670.1847459f, 688.8009889f, 708.4810171f, 729.3186941f,
                751.4192606f, 774.9011125f, 799.8979226f, 826.5611867f, 855.0632966f,
                885.6012714f};

    private final static float[] CopperN = {
        1.400313f, 1.38f,  1.358438f, 1.34f,  1.329063f, 1.325f, 1.3325f,   1.34f,
                1.334375f, 1.325f, 1.317812f, 1.31f,  1.300313f, 1.29f,  1.281563f, 1.27f,
                1.249062f, 1.225f, 1.2f,      1.18f,  1.174375f, 1.175f, 1.1775f,   1.18f,
                1.178125f, 1.175f, 1.172812f, 1.17f,  1.165312f, 1.16f,  1.155312f, 1.15f,
                1.142812f, 1.135f, 1.131562f, 1.12f,  1.092437f, 1.04f,  0.950375f, 0.826f,
                0.645875f, 0.468f, 0.35125f,  0.272f, 0.230813f, 0.214f, 0.20925f,  0.213f,
                0.21625f,  0.223f, 0.2365f,   0.25f,  0.254188f, 0.26f,  0.28f,     0.3f};

    private final static float[] CopperK = {
        1.662125f, 1.687f, 1.703313f, 1.72f,  1.744563f, 1.77f,  1.791625f, 1.81f,
                1.822125f, 1.834f, 1.85175f,  1.872f, 1.89425f,  1.916f, 1.931688f, 1.95f,
                1.972438f, 2.015f, 2.121562f, 2.21f,  2.177188f, 2.13f,  2.160063f, 2.21f,
                2.249938f, 2.289f, 2.326f,    2.362f, 2.397625f, 2.433f, 2.469187f, 2.504f,
                2.535875f, 2.564f, 2.589625f, 2.605f, 2.595562f, 2.583f, 2.5765f,   2.599f,
                2.678062f, 2.809f, 3.01075f,  3.24f,  3.458187f, 3.67f,  3.863125f, 4.05f,
                4.239563f, 4.43f,  4.619563f, 4.817f, 5.034125f, 5.26f,  5.485625f, 5.717f};

    static {
        copperN = Spectrum.FromSampled(CopperWavelengths, CopperN);
        copperK = Spectrum.FromSampled(CopperWavelengths, CopperK);
    }
}