/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class FresnelConductor extends Fresnel {

    public FresnelConductor(Spectrum etaI, Spectrum etaT, Spectrum k) {
        this.etaI = etaI;
        this.etaT = etaT;
        this.k = k;
    }

    public Spectrum Evaluate(float cosI) {
        return FrConductor(Math.abs(cosI), etaI, etaT, k);
    }
    public String ToString() {
        return "";
    }

    private Spectrum etaI, etaT, k;

    // https://seblagarde.wordpress.com/2013/04/29/memo-on-fresnel-equations/
    private static Spectrum FrConductor(float cosThetaI, Spectrum etai, Spectrum etat, Spectrum k) {
        cosThetaI = Pbrt.Clamp(cosThetaI, -1, 1);
        Spectrum eta = etat.divide(etai);
        Spectrum etak = k.divide(etai);

        float cosThetaI2 = cosThetaI * cosThetaI;
        float sinThetaI2 = 1 - cosThetaI2;
        Spectrum eta2 = eta.multiply(eta);
        Spectrum etak2 = etak.multiply(etak);

        Spectrum t0 = eta2.subtract(etak2).subtract(new Spectrum(sinThetaI2));
        Spectrum a2plusb2 = Spectrum.Sqrt(t0.multiply(t0).add(eta2.multiply(etak2).scale(4)));
        Spectrum t1 = a2plusb2.add(new Spectrum(cosThetaI2));
        Spectrum a = Spectrum.Sqrt((a2plusb2.add(t0)).scale(0.5f));
        Spectrum t2 = a.scale(2*cosThetaI);
        Spectrum Rs = (t1.subtract(t2)).divide(t1.add(t2));

        Spectrum t3 = a2plusb2.scale(cosThetaI2).add(new Spectrum(sinThetaI2 * sinThetaI2));
        Spectrum t4 = t2.scale(sinThetaI2);
        Spectrum Rp = Rs.multiply((t3.subtract(t4)).divide(t3.add(t4)));

        return (Rp.add(Rs)).scale(0.5f);
    }

}