
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
import org.pbrt.core.Error;

public class SubsurfaceMaterial extends Material {

    public static Material Create(TextureParams mp) {
        float[] sig_a_rgb = {.0011f, .0024f, .014f}, sig_s_rgb = {2.55f, 3.21f, 3.77f};
        Spectrum sig_a = Spectrum.FromRGB(sig_a_rgb),
                sig_s = Spectrum.FromRGB(sig_s_rgb);
        String name = mp.FindString("name","");
        Medium.ScatteringProps props = Medium.GetMediumScatteringProperties(name);
        if (props != null) {
            sig_a = props.sigma_a;
            sig_s = props.sigma_s;
        }
        float g = mp.FindFloat("g", 0.0f);
        if (!name.isEmpty()) {
            if (props == null)
                Error.Warning("Named material \"%s\" not found.  Using defaults.", name);
            else
                g = 0; /* Enforce g=0 (the database specifies reduced scattering
                      coefficients) */
        }
        float scale = mp.FindFloat("scale", 1);
        float eta = mp.FindFloat("eta", 1.33f);

        Texture<Spectrum> sigma_a, sigma_s;
        sigma_a = mp.GetSpectrumTexture("sigma_a", sig_a);
        sigma_s = mp.GetSpectrumTexture("sigma_s", sig_s);
        Texture<Spectrum> Kr = mp.GetSpectrumTexture("Kr", new Spectrum(1.f));
        Texture<Spectrum> Kt = mp.GetSpectrumTexture("Kt", new Spectrum(1.f));
        Texture<Float> roughu = mp.GetFloatTexture("uroughness", 0);
        Texture<Float> roughv = mp.GetFloatTexture("vroughness", 0);
        Texture<Float> bumpMap = mp.GetFloatTextureOrNull("bumpmap");
        boolean remapRoughness = mp.FindBool("remaproughness", true);
        return new SubsurfaceMaterial(scale, Kr, Kt, sigma_a, sigma_s, g, eta, roughu, roughv, bumpMap, remapRoughness);
    }

    public SubsurfaceMaterial(float scale,
                              Texture<Spectrum> Kr,
                              Texture<Spectrum> Kt,
                              Texture<Spectrum> sigma_a,
                              Texture<Spectrum> sigma_s,
                              float g, float eta,
                              Texture<Float> uRoughness,
                              Texture<Float> vRoughness,
                              Texture<Float> bumpMap,
                              boolean remapRoughness) {
        this.scale = scale;
        this.Kr = Kr;
        this.Kt = Kt;
        this.sigma_a = sigma_a;
        this.sigma_s = sigma_s;
        this.uRoughness = uRoughness;
        this.vRoughness = vRoughness;
        this.bumpMap = bumpMap;
        this.eta = eta;
        this.remapRoughness = remapRoughness;
        this.table = new BSSRDF.BSSRDFTable(100, 64);

        this.table = BSSRDF.ComputeBeamDiffusionBSSRDF(g, eta, this.table);
    }

    @Override
    public SurfaceInteraction ComputeScatteringFunctions(SurfaceInteraction si, TransportMode mode, boolean allowMultipleLobes) {
        // Perform bump mapping with _bumpMap_, if present
        if (bumpMap != null) Bump(bumpMap, si);

        // Initialize BSDF for _SubsurfaceMaterial_
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
        } else {
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
        Spectrum sig_a = (sigma_a.Evaluate(si).clamp(0, Pbrt.Infinity)).scale(scale);
        Spectrum sig_s = (sigma_s.Evaluate(si).clamp(0, Pbrt.Infinity)).scale(scale);
        si.bssrdf = new TabulatedBSSRDF(si, this, mode, eta, sig_a, sig_s, table);
        return si;
    }

    private final float scale;
    private Texture<Spectrum> Kr, Kt, sigma_a, sigma_s;
    private Texture<Float> uRoughness, vRoughness;
    private Texture<Float> bumpMap;
    private final float eta;
    private final boolean remapRoughness;
    private BSSRDF.BSSRDFTable table;

}