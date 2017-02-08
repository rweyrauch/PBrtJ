
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

import static org.pbrt.core.Medium.PhaseHG;

public abstract class BSSRDF {

    protected SurfaceInteraction po;
    protected float eta;

    public BSSRDF(SurfaceInteraction po, float eta) {
        this.po = po;
        this.eta = eta;
    }

    public class BSSRDFSample {
        public Spectrum s;
        public SurfaceInteraction si;
        public float pdf;
    }
    public abstract Spectrum S(SurfaceInteraction si, Vector3f wi);
    public abstract BSSRDFSample Sample_S(Scene scene, float u1, Point2f u2);

    public static class BSSRDFTable {
        // BSSRDFTable Public Data
        public int nRhoSamples, nRadiusSamples;
        public float[] rhoSamples, radiusSamples;
        public float[] profile;
        public float[] rhoEff;
        public float[] profileCDF;

        // BSSRDFTable Public Methods
        public BSSRDFTable(int nRhoSamples, int nRadiusSamples) {
            this.nRhoSamples = nRhoSamples;
            this.nRadiusSamples = nRadiusSamples;
            this.rhoSamples = new float[nRhoSamples];
            this.radiusSamples = new float[nRadiusSamples];
            this.profile = new float[nRadiusSamples * nRhoSamples];
            this.rhoEff = new float[nRhoSamples];
            this.profileCDF = new float[nRadiusSamples * nRhoSamples];

        }
        public float EvalProfile(int rhoIndex, int radiusIndex) {
            return profile[rhoIndex * nRadiusSamples + radiusIndex];
        }
    }

    public static float FresnelMoment1(float eta) {
        float eta2 = eta * eta, eta3 = eta2 * eta, eta4 = eta3 * eta,
                eta5 = eta4 * eta;
        if (eta < 1)
            return 0.45966f - 1.73965f * eta + 3.37668f * eta2 - 3.904945f * eta3 +
                    2.49277f * eta4 - 0.68441f * eta5;
        else
            return -4.61686f + 11.1136f * eta - 10.4646f * eta2 + 5.11455f * eta3 -
                    1.27198f * eta4 + 0.12746f * eta5;
    }
    public static float FresnelMoment2(float eta) {
        float eta2 = eta * eta, eta3 = eta2 * eta, eta4 = eta3 * eta,
                eta5 = eta4 * eta;
        if (eta < 1) {
            return 0.27614f - 0.87350f * eta + 1.12077f * eta2 - 0.65095f * eta3 +
                    0.07883f * eta4 + 0.04860f * eta5;
        } else {
            float r_eta = 1 / eta, r_eta2 = r_eta * r_eta, r_eta3 = r_eta2 * r_eta;
            return -547.033f + 45.3087f * r_eta3 - 218.725f * r_eta2 +
                    458.843f * r_eta + 404.557f * eta - 189.519f * eta2 +
                    54.9327f * eta3 - 9.00603f * eta4 + 0.63942f * eta5;
        }
    }

    public static float BeamDiffusionSS(float sigma_s, float sigma_a, float g, float eta, float r) {
        // Compute material parameters and minimum $t$ below the critical angle
        float sigma_t = sigma_a + sigma_s, rho = sigma_s / sigma_t;
        float tCrit = r * (float)Math.sqrt(eta * eta - 1);
        float Ess = 0;
        int nSamples = 100;
        for (int i = 0; i < nSamples; ++i) {
            // Evaluate single scattering integrand and add to _Ess_
            float ti = tCrit - (float)Math.log(1 - (i + .5f) / nSamples) / sigma_t;

            // Determine length $d$ of connecting segment and $\cos\theta_\roman{o}$
            float d = (float)Math.sqrt(r * r + ti * ti);
            float cosThetaO = ti / d;

            // Add contribution of single scattering at depth $t$
            Ess += rho * (float)Math.exp(-sigma_t * (d + tCrit)) / (d * d) *
                    PhaseHG(cosThetaO, g) * (1 - Reflection.FrDielectric(-cosThetaO, 1, eta)) *
                    Math.abs(cosThetaO);
        }
        return Ess / nSamples;
    }

    public static float BeamDiffusionMS(float sigma_s, float sigma_a, float g, float eta, float r) {
        int nSamples = 100;
        float Inv4Pi = (float)(1.0 / 4.0 * Math.PI);
        float Ed = 0;
        // Precompute information for dipole integrand

        // Compute reduced scattering coefficients $\sigmaps, \sigmapt$ and albedo
        // $\rhop$
        float sigmap_s = sigma_s * (1 - g);
        float sigmap_t = sigma_a + sigmap_s;
        float rhop = sigmap_s / sigmap_t;

        // Compute non-classical diffusion coefficient $D_\roman{G}$ using
        // Equation (15.24)
        float D_g = (2 * sigma_a + sigmap_s) / (3 * sigmap_t * sigmap_t);

        // Compute effective transport coefficient $\sigmatr$ based on $D_\roman{G}$
        float sigma_tr = (float)Math.sqrt(sigma_a / D_g);

        // Determine linear extrapolation distance $\depthextrapolation$ using
        // Equation (15.28)
        float fm1 = FresnelMoment1(eta), fm2 = FresnelMoment2(eta);
        float ze = -2 * D_g * (1 + 3 * fm2) / (1 - 2 * fm1);

        // Determine exitance scale factors using Equations (15.31) and (15.32)
        float cPhi = .25f * (1 - 2 * fm1), cE = .5f * (1 - 3 * fm2);
        for (int i = 0; i < nSamples; ++i) {
            // Sample real point source depth $\depthreal$
            float zr = -(float)Math.log(1 - (i + .5f) / nSamples) / sigmap_t;

            // Evaluate dipole integrand $E_{\roman{d}}$ at $\depthreal$ and add to
            // _Ed_
            float zv = -zr + 2 * ze;
            float dr = (float)Math.sqrt(r * r + zr * zr), dv = (float)Math.sqrt(r * r + zv * zv);

            // Compute dipole fluence rate $\dipole(r)$ using Equation (15.27)
            float phiD = Inv4Pi / D_g * ((float)Math.exp(-sigma_tr * dr) / dr -
                    (float)Math.exp(-sigma_tr * dv) / dv);

            // Compute dipole vector irradiance $-\N{}\cdot\dipoleE(r)$ using
            // Equation (15.27)
            float EDn = Inv4Pi * (zr * (1 + sigma_tr * dr) *
                    (float)Math.exp(-sigma_tr * dr) / (dr * dr * dr) -
                    zv * (1 + sigma_tr * dv) *
                            (float)Math.exp(-sigma_tr * dv) / (dv * dv * dv));

            // Add contribution from dipole for depth $\depthreal$ to _Ed_
            float E = phiD * cPhi + EDn * cE;
            float kappa = 1 - (float)Math.exp(-2 * sigmap_t * (dr + zr));
            Ed += kappa * rhop * rhop * E;
        }
        return Ed / nSamples;
    }
    public static void ComputeBeamDiffusionBSSRDF(float g, float eta, BSSRDFTable t) {

    }

    public static class SubsurfaceSpectrum {
        public Spectrum sigma_a;
        public Spectrum sigma_s;
    }
    public static SubsurfaceSpectrum SubsurfaceFromDiffuse(BSSRDFTable table, Spectrum rhoEff,
                            Spectrum mfp) {
        return null;
    }

}