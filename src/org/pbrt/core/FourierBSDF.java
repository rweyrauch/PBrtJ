/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

import java.util.Arrays;

public class FourierBSDF extends BxDF {

    public FourierBSDF(FourierBSDFTable table, Material.TransportMode mode) {
        super(BSDF_REFLECTION | BSDF_TRANSMISSION | BSDF_GLOSSY);
        this.bsdfTable = table;
        this.mode = mode;
    }

    @Override
    public Spectrum f(Vector3f wo, Vector3f wi) {
        // Find the zenith angle cosines and azimuth difference angle
        float muI = Reflection.CosTheta(wi.negate()), muO = Reflection.CosTheta(wo);
        float cosPhi = Reflection.CosDPhi(wi.negate(), wo);

        // Compute Fourier coefficients $a_k$ for $(\mui, \muo)$

        // Determine offsets and weights for $\mui$ and $\muo$
        Interpolation.Weights weightsI = bsdfTable.GetWeights(muI);
        Interpolation.Weights weightsO = bsdfTable.GetWeights(muO);
        if ((weightsI == null) || (weightsO == null))
            return new Spectrum(0);

        // Allocate storage to accumulate _ak_ coefficients
        float[] ak = new float[bsdfTable.mMax * bsdfTable.nChannels];

        // Accumulate weighted sums of nearby $a_k$ coefficients
        int mMax = 0;
        for (int b = 0; b < 4; ++b) {
            for (int a = 0; a < 4; ++a) {
                // Add contribution of _(a, b)_ to $a_k$ values
                float weight = weightsI.weights[a] * weightsO.weights[b];
                if (weight != 0) {
                    int m = bsdfTable.GetM(weightsI.offset + a, weightsO.offset + b);
                    float[] ap = bsdfTable.GetAk(weightsI.offset + a, weightsO.offset + b);
                    mMax = Math.max(mMax, m);
                    for (int c = 0; c < bsdfTable.nChannels; ++c)
                        for (int k = 0; k < m; ++k)
                            ak[c * bsdfTable.mMax + k] += weight * ap[c * m + k];
                }
            }
        }

        // Evaluate Fourier expansion for angle $\phi$
        float Y = Math.max(0, Interpolation.Fourier(ak, mMax, cosPhi));
        float scale = muI != 0 ? (1 / Math.abs(muI)) : 0;

        // Update _scale_ to account for adjoint light transport
        if (mode == Material.TransportMode.Radiance && muI * muO > 0) {
            float eta = muI > 0 ? 1 / bsdfTable.eta : bsdfTable.eta;
            scale *= eta * eta;
        }
        if (bsdfTable.nChannels == 1)
            return new Spectrum(Y * scale);
        else {
            // Compute and return RGB colors for tabulated BSDF
            float R = Interpolation.Fourier(Arrays.copyOfRange(ak, 1 * bsdfTable.mMax, mMax), mMax, cosPhi);
            float B = Interpolation.Fourier(Arrays.copyOfRange(ak, 2 * bsdfTable.mMax, mMax), mMax, cosPhi);
            float G = 1.39829f * Y - 0.100913f * B - 0.297375f * R;
            float[] rgb = {R * scale, G * scale, B * scale};
            return Spectrum.FromRGB(rgb).clamp(0, Pbrt.Infinity);
        }
    }

    @Override
    public BxDFSample Sample_f(Vector3f wo, Point2f u) {
        BxDFSample result = new BxDFSample();
        result.f = new Spectrum(0);
        result.pdf = 0;

        // Sample zenith angle component for _FourierBSDF_
        float muO = Reflection.CosTheta(wo);
        Interpolation.SampleCR sampCR = Interpolation.SampleCatmullRom2D(bsdfTable.nMu, bsdfTable.nMu, bsdfTable.mu,
                bsdfTable.mu, bsdfTable.a0, bsdfTable.cdf, muO, u.y);
        float pdfMu = sampCR.pdf;
        float muI = sampCR.sample;

        // Compute Fourier coefficients $a_k$ for $(\mui, \muo)$

        // Determine offsets and weights for $\mui$ and $\muo$
        Interpolation.Weights weightsI = bsdfTable.GetWeights(muI);
        Interpolation.Weights weightsO = bsdfTable.GetWeights(muO);

        if ((weightsI == null) || (weightsO == null))
            return result;

        // Allocate storage to accumulate _ak_ coefficients
        float[] ak = new float[bsdfTable.mMax * bsdfTable.nChannels];

        // Accumulate weighted sums of nearby $a_k$ coefficients
        int mMax = 0;
        for (int b = 0; b < 4; ++b) {
            for (int a = 0; a < 4; ++a) {
                // Add contribution of _(a, b)_ to $a_k$ values
                float weight = weightsI.weights[a] * weightsO.weights[b];
                if (weight != 0) {
                    int m = bsdfTable.GetM(weightsI.offset + a, weightsO.offset + b);
                    float[] ap = bsdfTable.GetAk(weightsI.offset + a, weightsO.offset + b);
                    mMax = Math.max(mMax, m);
                    for (int c = 0; c < bsdfTable.nChannels; ++c)
                        for (int k = 0; k < m; ++k)
                            ak[c * bsdfTable.mMax + k] += weight * ap[c * m + k];
                }
            }
        }

        // Importance sample the luminance Fourier expansion
        Interpolation.SampleF sf = Interpolation.SampleFourier(ak, bsdfTable.recip, mMax, u.x);
        float phi = sf.phiPtr;
        float pdfPhi = sf.pdf;
        float Y = sf.sample;
        result.pdf = Math.max(0, pdfPhi * pdfMu);

        // Compute the scattered direction for _FourierBSDF_
        float sin2ThetaI = Math.max(0, 1 - muI * muI);
        float norm = (float)Math.sqrt(sin2ThetaI / Reflection.Sin2Theta(wo));
        if (Float.isInfinite(norm)) norm = 0;
        float sinPhi = (float)Math.sin(phi), cosPhi = (float)Math.cos(phi);
        result.wiWorld = new Vector3f(-norm * (cosPhi * wo.x - sinPhi * wo.y),
                -norm * (sinPhi * wo.x + cosPhi * wo.y), -muI);

        // Mathematically, wi will be normalized (if wo was). However, in
        // practice, floating-point rounding error can cause some error to
        // accumulate in the computed value of wi here. This can be
        // catastrophic: if the ray intersects an object with the FourierBSDF
        // again and the wo (based on such a wi) is nearly perpendicular to the
        // surface, then the wi computed at the next intersection can end up
        // being substantially (like 4x) longer than normalized, which leads to
        // all sorts of errors, including negative spectral values. Therefore,
        // we normalize again here.
        result.wiWorld = Vector3f.Normalize(result.wiWorld);

        // Evaluate remaining Fourier expansions for angle $\phi$
        float scale = muI != 0 ? (1 / Math.abs(muI)) : 0;
        if (mode == Material.TransportMode.Radiance && muI * muO > 0) {
            float eta = muI > 0 ? 1 / bsdfTable.eta : bsdfTable.eta;
            scale *= eta * eta;
        }

        if (bsdfTable.nChannels == 1) {
            result.f = new Spectrum(Y * scale);
            return result;
        }
        float R = Interpolation.Fourier(Arrays.copyOfRange(ak, 1 * bsdfTable.mMax, mMax), mMax, cosPhi);
        float B = Interpolation.Fourier(Arrays.copyOfRange(ak, 2 * bsdfTable.mMax, mMax), mMax, cosPhi);
        float G = 1.39829f * Y - 0.100913f * B - 0.297375f * R;
        float[] rgb = {R * scale, G * scale, B * scale};
        result.f = Spectrum.FromRGB(rgb).clamp(0, Pbrt.Infinity);
        return result;
    }

    @Override
    public float Pdf(Vector3f wo, Vector3f wi) {
        // Find the zenith angle cosines and azimuth difference angle
        float muI = Reflection.CosTheta(wi.negate()), muO = Reflection.CosTheta(wo);
        float cosPhi = Reflection.CosDPhi(wi.negate(), wo);

        // Compute luminance Fourier coefficients $a_k$ for $(\mui, \muo)$
        Interpolation.Weights weightsI = bsdfTable.GetWeights(muI);
        Interpolation.Weights weightsO = bsdfTable.GetWeights(muO);
        if ((weightsI == null) || (weightsO == null))
            return 0;
        float[] ak = new float[bsdfTable.mMax * bsdfTable.nChannels];
        int mMax = 0;
        for (int o = 0; o < 4; ++o) {
            for (int i = 0; i < 4; ++i) {
                float weight = weightsI.weights[i] * weightsO.weights[o];
                if (weight == 0) continue;

                int order = bsdfTable.GetM(weightsI.offset + i, weightsO.offset + o);
                float[] coeffs = bsdfTable.GetAk(weightsI.offset + i, weightsO.offset + o);
                mMax = Math.max(mMax, order);

                for (int k = 0; k < order; ++k) ak[k] += coeffs[k] * weight;
            }
        }

        // Evaluate probability of sampling _wi_
        float rho = 0;
        for (int o = 0; o < 4; ++o) {
            if (weightsO.weights[o] == 0) continue;
            rho += weightsO.weights[o] * bsdfTable.cdf[(weightsO.offset + o) * bsdfTable.nMu + bsdfTable.nMu - 1] * (2 * (float)Math.PI);
        }
        float Y = Interpolation.Fourier(ak, mMax, cosPhi);
        return (rho > 0 && Y > 0) ? (Y / rho) : 0;
    }

    @Override
    public String ToString() {
        return null;
    }

    private FourierBSDFTable bsdfTable;
    private Material.TransportMode mode;
}