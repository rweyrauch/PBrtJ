
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

import static org.pbrt.core.BSSRDF.ComputeBeamDiffusionBSSRDF;
import static org.pbrt.core.BSSRDF.SubsurfaceFromDiffuse;

public class KdSubsurfaceMaterial extends Material {

    public static Material Create(TextureParams mp) {
        float[] Kd = {.5f, .5f, .5f};
        Texture<Spectrum> kd = mp.GetSpectrumTexture("Kd", Spectrum.FromRGB(Kd));
        Texture<Spectrum> mfp = mp.GetSpectrumTexture("mfp", new Spectrum(1));
        Texture<Spectrum> kr = mp.GetSpectrumTexture("Kr", new Spectrum(1));
        Texture<Spectrum> kt = mp.GetSpectrumTexture("Kt", new Spectrum(1));
        Texture<Float> roughu = mp.GetFloatTexture("uroughness", 0);
        Texture<Float> roughv = mp.GetFloatTexture("vroughness", 0);
        Texture<Float> bumpMap = mp.GetFloatTextureOrNull("bumpmap");
        float eta = mp.FindFloat("eta", 1.33f);
        float scale = mp.FindFloat("scale", 1);
        float g = mp.FindFloat("g", 0.0f);
        boolean remapRoughness = mp.FindBool("remaproughness", true);
        return new KdSubsurfaceMaterial(scale, kd, kr, kt, mfp, g, eta, roughu,
                roughv, bumpMap, remapRoughness);
    }

    public KdSubsurfaceMaterial(float scale, Texture<Spectrum> Kd, Texture<Spectrum> Kr, Texture<Spectrum> Kt,
                         Texture<Spectrum> mfp, float g, float eta,
                         Texture<Float> uRoughness, Texture<Float> vRoughness, Texture<Float> bumpMap, boolean remapRoughness) {
        this.scale =scale;
        this.Kd = Kd;
        this.Kr = Kr;
        this.Kt = Kt;
        this.mfp = mfp;
        this.uRoughness = uRoughness;
        this.vRoughness = vRoughness;
        this.bumpMap = bumpMap;
        this.eta = eta;
        this.remapRoughness = remapRoughness;
        this.table = new BSSRDF.BSSRDFTable(100, 64);

        this.table = ComputeBeamDiffusionBSSRDF(g, eta, this.table);
    }

    @Override
    public SurfaceInteraction ComputeScatteringFunctions(SurfaceInteraction si, TransportMode mode, boolean allowMultipleLobes) {
        // Perform bump mapping with _bumpMap_, if present
        if (bumpMap != null) Bump(bumpMap, si);
        Spectrum R = Kr.Evaluate(si).clamp(0, Pbrt.Infinity);
        Spectrum T = Kt.Evaluate(si).clamp(0, Pbrt.Infinity);
        float urough = uRoughness.Evaluate(si);
        float vrough = vRoughness.Evaluate(si);

        // Initialize _bsdf_ for smooth or rough dielectric
        si.bsdf = new BSDF(si, eta);

        if (R.isBlack() && T.isBlack()) return si;

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

        Spectrum mfree = (mfp.Evaluate(si).clamp(0, Pbrt.Infinity)).scale(scale);
        Spectrum kd = Kd.Evaluate(si).clamp(0, Pbrt.Infinity);
        BSSRDF.SubsurfaceSpectrum sss = SubsurfaceFromDiffuse(table, kd, mfree);
        si.bssrdf = new TabulatedBSSRDF(si, this, mode, eta, sss.sigma_a, sss.sigma_s, table);
        return si;
    }

    private float scale;
    private Texture<Spectrum> Kd, Kr, Kt, mfp;
    private Texture<Float> uRoughness, vRoughness;
    private Texture<Float> bumpMap;
    private float eta;
    private boolean remapRoughness;
    private BSSRDF.BSSRDFTable table;

}