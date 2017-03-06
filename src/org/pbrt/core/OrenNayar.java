
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class OrenNayar extends BxDF {

    public OrenNayar(Spectrum R, float sigma) {
        super(BSDF_REFLECTION | BSDF_DIFFUSE);
        this.R = new Spectrum(R);
        sigma = (float)Math.toRadians(sigma);
        float sigma2 = sigma * sigma;
        this.A = 1 - (sigma2 / (2 * (sigma2 + 0.33f)));
        this.B = 0.45f * sigma2 / (sigma2 + 0.09f);
    }

    @Override
    public Spectrum f(Vector3f wo, Vector3f wi) {
        float sinThetaI = Reflection.SinTheta(wi);
        float sinThetaO = Reflection.SinTheta(wo);
        // Compute cosine term of Oren-Nayar model
        float maxCos = 0;
        if (sinThetaI > 1e-4 && sinThetaO > 1e-4) {
            float sinPhiI = Reflection.SinPhi(wi), cosPhiI = Reflection.CosPhi(wi);
            float sinPhiO = Reflection.SinPhi(wo), cosPhiO = Reflection.CosPhi(wo);
            float dCos = cosPhiI * cosPhiO + sinPhiI * sinPhiO;
            maxCos = Math.max(0, dCos);
        }

        // Compute sine and tangent terms of Oren-Nayar model
        float sinAlpha, tanBeta;
        if (Reflection.AbsCosTheta(wi) > Reflection.AbsCosTheta(wo)) {
            sinAlpha = sinThetaO;
            tanBeta = sinThetaI / Reflection.AbsCosTheta(wi);
        } else {
            sinAlpha = sinThetaI;
            tanBeta = sinThetaO / Reflection.AbsCosTheta(wo);
        }
        return Spectrum.Scale(R, (A + B * maxCos * sinAlpha * tanBeta)/ (float)Math.PI);
    }

    @Override
    public String toString() {
        return "[ OrenNayar R: " + R.toString() + String.format(" A: %f B: %f ]", A, B);
    }

    private Spectrum R;
    private float A, B;
}