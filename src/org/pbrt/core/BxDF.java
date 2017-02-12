
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public abstract class BxDF {

    public static final int BSDF_NONE = 0;
    public static final int BSDF_REFLECTION = (1 << 0);
    public static final int BSDF_TRANSMISSION = (1 << 1);
    public static final int BSDF_DIFFUSE = (1 << 2);
    public static final int BSDF_GLOSSY = (1 << 3);
    public static final int BSDF_SPECULAR = (1 << 4);
    public static final int BSDF_ALL = (BSDF_REFLECTION | BSDF_TRANSMISSION | BSDF_DIFFUSE | BSDF_GLOSSY | BSDF_SPECULAR);
    
    // BxDF Public Data
    public int type;

    // BxDF Interface
    public BxDF(int type) {
        this.type = type;
    }
    public boolean MatchesFlags(int t) { return (type & t) == type; }
    public abstract Spectrum f(Vector3f wo, Vector3f wi);

    public static class BxDFSample {
        public Spectrum f;
        public Vector3f wiWorld;
        public float pdf;
        public int sampledType;
    }

    public BxDFSample Sample_f(Vector3f wo, Point2f u) {
        // Cosine-sample the hemisphere, flipping the direction if necessary
        BxDFSample sample = new BxDFSample();
        sample.wiWorld = Sampling.CosineSampleHemisphere(u);
        if (wo.z < 0) sample.wiWorld.z *= -1;
        sample.pdf = Pdf(wo, sample.wiWorld);
        sample.f = f(wo, sample.wiWorld);
        return sample;
    }
    public Spectrum rho(Vector3f w, int nSamples, Point2f[] u) {
        Spectrum r = new Spectrum(0);
        for (int i = 0; i < nSamples; ++i) {
            // Estimate one term of $\rho_\roman{hd}$
            BxDFSample sample = Sample_f(w, u[i]);
            if (sample.pdf > 0) {
                r = (Spectrum)CoefficientSpectrum.add(r, CoefficientSpectrum.scale(sample.f, Reflection.AbsCosTheta(sample.wiWorld) / sample.pdf));
            }
        }
        r.invScale(nSamples);
        return r;
    }

    public Spectrum rho(int nSamples, Point2f[] u1, Point2f[] u2) {
        Spectrum r = new Spectrum(0);
        for (int i = 0; i < nSamples; ++i) {
            // Estimate one term of $\rho_\roman{hh}$
            Vector3f wo = Sampling.UniformSampleHemisphere(u1[i]);
            float pdfo = Sampling.UniformHemispherePdf();
            BxDFSample sample = Sample_f(wo, u2[i]);
            if (sample.pdf > 0) {
                r = (Spectrum)CoefficientSpectrum.add(r, CoefficientSpectrum.scale(sample.f, Reflection.AbsCosTheta(sample.wiWorld) * Reflection.AbsCosTheta(wo) / (pdfo * sample.pdf)));
            }
        }
        r.invScale((float)Math.PI * nSamples);
        return r;
    }

    public float Pdf(Vector3f wo, Vector3f wi) {
        return Reflection.SameHemisphere(wo, wi) ? Reflection.AbsCosTheta(wi) / (float)Math.PI : 0;
    }
    public abstract String ToString();

}