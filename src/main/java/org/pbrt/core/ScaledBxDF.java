/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class ScaledBxDF extends BxDF {

    public ScaledBxDF(BxDF bxdf, Spectrum scale) {
        super(bxdf.type);
        this.bxdf = bxdf;
        this.scale = new Spectrum(scale);
    }

    @Override
    public BxDFSample Sample_f(Vector3f wo, Point2f u) {
        BxDFSample bs = bxdf.Sample_f(wo, u);
        bs.f = bs.f.multiply(scale);
        return bs;
    }

    @Override
    public Spectrum f(Vector3f wo, Vector3f wi) {
        return bxdf.f(wo, wi).multiply(scale);
    }

    @Override
    public Spectrum rho(Vector3f w, int nSamples, Point2f[] samples) {
        return bxdf.rho(w, nSamples, samples).multiply(scale);
    }

    @Override
    public Spectrum rho(int nSamples, Point2f[] samples1, Point2f[] samples2) {
        return bxdf.rho(nSamples, samples1, samples2).multiply(scale);
    }

    @Override
    public float Pdf(Vector3f wo, Vector3f wi) {
        return bxdf.Pdf(wo, wi);
    }

    @Override
    public String toString() {
        return "[ ScaledBxDF bxdf: " + bxdf.toString() + " scale: " + scale.toString() + " ]";
    }

    private BxDF bxdf;
    private Spectrum scale;
}