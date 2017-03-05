
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class LambertianTransmission extends BxDF {

    public LambertianTransmission(Spectrum T) {
        super(BSDF_TRANSMISSION | BSDF_DIFFUSE);
        this.T = new Spectrum(T);
    }

    @Override
    public Spectrum f(Vector3f wo, Vector3f wi) {
        return Spectrum.Scale(T, (float)(1/Math.PI));
    }

    @Override
    public BxDFSample Sample_f(Vector3f wo, Point2f u) {
        BxDFSample bs = new BxDFSample();
        bs.wiWorld = Sampling.CosineSampleHemisphere(u);
        if (wo.z > 0) bs.wiWorld.z *= -1;
        bs.pdf = Pdf(wo, bs.wiWorld);
        bs.f = f(wo, bs.wiWorld);
        return bs;
    }

    @Override
    public Spectrum rho(Vector3f w, int nSamples, Point2f[] u) { return T; }

    @Override
    public Spectrum rho(int nSamples, Point2f[] u1, Point2f[] u2) { return T; }

    @Override
    public float Pdf(Vector3f wo, Vector3f wi) {
        return !Reflection.SameHemisphere(wo, wi) ? Reflection.AbsCosTheta(wi) / (float)Math.PI : 0;
    }

    @Override
    public String ToString() {
        return null;
    }

    private Spectrum T;
}