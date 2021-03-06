
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
import org.pbrt.core.PBrtTLogger;
import org.pbrt.textures.ConstantTextureSpectrum;

public class HairMaterial extends Material {

    public static Material Create(TextureParams mp) {
        TextureSpectrum sigma_a = mp.GetSpectrumTextureOrNull("sigma_a");
        TextureSpectrum color = mp.GetSpectrumTextureOrNull("color");
        TextureFloat eumelanin = mp.GetFloatTextureOrNull("eumelanin");
        TextureFloat pheomelanin = mp.GetFloatTextureOrNull("pheomelanin");
        if (sigma_a != null) {
            if (color != null)
                PBrtTLogger.Warning("Ignoring \"color\" parameter since \"sigma_a\" was provided.");
            if (eumelanin != null)
                PBrtTLogger.Warning("Ignoring \"eumelanin\" parameter since \"sigma_a\" was provided.");
            if (pheomelanin != null)
                PBrtTLogger.Warning("Ignoring \"pheomelanin\" parameter since \"sigma_a\" was provided.");
        }
        else if (color != null) {
            if (sigma_a != null)
                PBrtTLogger.Warning("Ignoring \"sigma_a\" parameter since \"color\" was provided.");
            if (eumelanin != null)
                PBrtTLogger.Warning("Ignoring \"eumelanin\" parameter since \"color\" was provided.");
            if (pheomelanin != null)
                PBrtTLogger.Warning("Ignoring \"pheomelanin\" parameter since \"color\" was provided.");
        }
        else if (eumelanin != null || pheomelanin != null) {
            if (sigma_a != null)
                PBrtTLogger.Warning("Ignoring \"sigma_a\" parameter since \"eumelanin\"/\"pheomelanin\" was provided.");
            if (color != null)
                PBrtTLogger.Warning("Ignoring \"color\" parameter since \"eumelanin\"/\"pheomelanin\" was provided.");
        }
        else {
            // Default: brown-ish hair.
            sigma_a = new ConstantTextureSpectrum(HairBSDF.SigmaAFromConcentration(1.3f, 0));
        }

        TextureFloat eta = mp.GetFloatTexture("eta", 1.55f);
        TextureFloat beta_m = mp.GetFloatTexture("beta_m", 0.3f);
        TextureFloat beta_n = mp.GetFloatTexture("beta_n", 0.3f);
        TextureFloat alpha = mp.GetFloatTexture("alpha", 2.f);

        return new HairMaterial(sigma_a, color, eumelanin, pheomelanin, eta, beta_m, beta_n, alpha);
    }

    public HairMaterial(TextureSpectrum sigma_a, TextureSpectrum color, TextureFloat eumelanin, TextureFloat pheomelanin, TextureFloat eta,
                        TextureFloat beta_m, TextureFloat beta_n, TextureFloat alpha) {
        this.sigma_a = sigma_a;
        this.color = color;
        this.eumelanin = eumelanin;
        this.pheomelanin = pheomelanin;
        this.eta = eta;
        this.beta_m = beta_m;
        this.beta_n = beta_n;
        this.alpha = alpha;
    }

    @Override
    public void ComputeScatteringFunctions(SurfaceInteraction si, TransportMode mode, boolean allowMultipleLobes) {
        float bm = beta_m.Evaluate(si);
        float bn = beta_n.Evaluate(si);
        float a = (float)Math.toRadians(alpha.Evaluate(si));
        float e = eta.Evaluate(si);

        si.bsdf = new BSDF(si, e);

        Spectrum sig_a;
        if (sigma_a != null)
            sig_a = sigma_a.Evaluate(si).clamp(0, Pbrt.Infinity);
        else if (color != null) {
            Spectrum c = color.Evaluate(si).clamp(0, Pbrt.Infinity);
            sig_a = HairBSDF.SigmaAFromReflectance(c, bn);
        }
        else {
            assert (eumelanin != null || pheomelanin != null);
            sig_a = HairBSDF.SigmaAFromConcentration(Math.max(0, (eumelanin != null) ? eumelanin.Evaluate(si) : 0),
                Math.max(0, (pheomelanin != null) ? pheomelanin.Evaluate(si) : 0));
        }

        // Offset along width
        float h = -1 + 2 * si.uv.y;
        si.bsdf.Add(new HairBSDF(h, e, sig_a, bm, bn, a));
    }

    private TextureSpectrum sigma_a, color;
    private TextureFloat eumelanin, pheomelanin, eta;
    private TextureFloat beta_m, beta_n, alpha;

}