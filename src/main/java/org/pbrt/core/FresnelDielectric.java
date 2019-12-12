/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class FresnelDielectric extends Fresnel {

    public FresnelDielectric(float etaI, float etaT) {
        this.etaI = etaI;
        this.etaT = etaT;
    }

    @Override
    public Spectrum Evaluate(float cosI) {
        return new Spectrum(FrDielectric(cosI, etaI, etaT));
    }

    @Override
    public String toString() {
        return String.format("[ FrenselDielectric etaI: %f etaT: %f ]", etaI, etaT);
    }

    private float etaI, etaT;

    private static float FrDielectric(float cosThetaI, float etaI, float etaT) {
        cosThetaI = Pbrt.Clamp(cosThetaI, -1, 1);
        // Potentially swap indices of refraction
        boolean entering = cosThetaI > 0;
        if (!entering) {
            float temp = etaI;
            etaI = etaT;
            etaT = temp;
            cosThetaI = Math.abs(cosThetaI);
        }

        // Compute _cosThetaT_ using Snell's law
        float sinThetaI = (float)Math.sqrt(Math.max(0, 1 - cosThetaI * cosThetaI));
        float sinThetaT = etaI / etaT * sinThetaI;

        // Handle total internal reflection
        if (sinThetaT >= 1) return 1;
        float cosThetaT = (float)Math.sqrt(Math.max(0, 1 - sinThetaT * sinThetaT));
        float Rparl = ((etaT * cosThetaI) - (etaI * cosThetaT)) /
                ((etaT * cosThetaI) + (etaI * cosThetaT));
        float Rperp = ((etaI * cosThetaI) - (etaT * cosThetaT)) /
                ((etaI * cosThetaI) + (etaT * cosThetaT));
        return (Rparl * Rparl + Rperp * Rperp) / 2;
    }

}