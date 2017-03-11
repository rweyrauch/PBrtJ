/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class TabulatedBSSRDF extends SeparableBSSRDF {

    private final BSSRDFTable table;
    private Spectrum sigma_t, rho;

    public TabulatedBSSRDF(SurfaceInteraction po, Material material,
                    Material.TransportMode mode, float eta, Spectrum sigma_a,
                    Spectrum sigma_s, BSSRDFTable table) {
        super(po, eta, material, mode);
        this.table = table;
        this.sigma_t = Spectrum.Add(sigma_a, sigma_s);
        for (int c = 0; c < Spectrum.numSamples(); ++c) {
            if (this.sigma_t.at(c) != 0) {
                this.rho.set(c, sigma_s.at(c) / this.sigma_t.at(c));
            }
            else {
                this.rho.set(c, 0);
            }
        }
    }

    @Override
    public Spectrum Sr(float r) {
        Spectrum Sr = new Spectrum(0);
        for (int ch = 0; ch < Spectrum.numSamples(); ++ch) {
            // Convert $r$ into unitless optical radius $r_{\roman{optical}}$
            float rOptical = r * sigma_t.at(ch);

            // Compute spline weights to interpolate BSSRDF on channel _ch_
            Interpolation.Weights wRho = Interpolation.CatmullRomWeights(table.nRhoSamples, table.rhoSamples, rho.at(ch));
            Interpolation.Weights wRadius = Interpolation.CatmullRomWeights(table.nRadiusSamples, table.radiusSamples, rOptical);
            if (wRho == null || wRadius == null)
                continue;
            int rhoOffset = wRho.offset;
            float[] rhoWeights = wRho.weights;
            int radiusOffset = wRadius.offset;
            float[] radiusWeights = wRadius.weights;

            // Set BSSRDF value _Sr[ch]_ using tensor spline interpolation
            float sr = 0;
            for (int i = 0; i < 4; ++i) {
                for (int j = 0; j < 4; ++j) {
                    float weight = rhoWeights[i] * radiusWeights[j];
                    if (weight != 0)
                        sr += weight * table.EvalProfile(rhoOffset + i, radiusOffset + j);
                }
            }

            // Cancel marginal PDF factor from tabulated BSSRDF profile
            if (rOptical != 0) sr /= 2 * Pbrt.Pi * rOptical;
            Sr.set(ch, sr);
        }
        // Transform BSSRDF value into world space units
        Sr.multiply(sigma_t); // * sigma_t ^ 2
        Sr.multiply(sigma_t);
        return Sr.clamp(0, Pbrt.Infinity);
    }

    @Override
    public float Sample_Sr(int ch, float u) {
        if (sigma_t.at(ch) == 0) return -1;
        Interpolation.SampleCR sampCR = Interpolation.SampleCatmullRom2D(table.nRhoSamples, table.nRadiusSamples,
                table.rhoSamples, table.radiusSamples,
                table.profile, table.profileCDF,
                rho.at(ch), u);
         return sampCR.sample / sigma_t.at(ch);
    }

    @Override
    public float Pdf_Sr(int ch, float r) {
        // Convert $r$ into unitless optical radius $r_{\roman{optical}}$
        float rOptical = r * sigma_t.at(ch);

        // Compute spline weights to interpolate BSSRDF density on channel _ch_
        Interpolation.Weights wRho = Interpolation.CatmullRomWeights(table.nRhoSamples, table.rhoSamples, rho.at(ch));
        Interpolation.Weights wRadius = Interpolation.CatmullRomWeights(table.nRadiusSamples, table.radiusSamples, rOptical);
        if (wRho == null || wRadius == null)
            return 0;
        int rhoOffset = wRho.offset;
        float[] rhoWeights = wRho.weights;
        int radiusOffset = wRadius.offset;
        float[] radiusWeights = wRadius.weights;

        // Return BSSRDF profile density for channel _ch_
        float sr = 0, rhoEff = 0;
        for (int i = 0; i < 4; ++i) {
            if (rhoWeights[i] == 0) continue;
            rhoEff += table.rhoEff[rhoOffset + i] * rhoWeights[i];
            for (int j = 0; j < 4; ++j) {
                if (radiusWeights[j] == 0) continue;
                sr += table.EvalProfile(rhoOffset + i, radiusOffset + j) *
                        rhoWeights[i] * radiusWeights[j];
            }
        }

        // Cancel marginal PDF factor from tabulated BSSRDF profile
        if (rOptical != 0) sr /= 2 * Pbrt.Pi * rOptical;
        return Math.max(0, sr * sigma_t.at(ch) * sigma_t.at(ch) / rhoEff);
    }
}