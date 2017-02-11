
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

    public enum BxDFType {
        BSDF_NONE(0),
        BSDF_REFLECTION(1 << 0),
        BSDF_TRANSMISSION(1 << 1),
        BSDF_DIFFUSE(1 << 2),
        BSDF_GLOSSY(1 << 3),
        BSDF_SPECULAR(1 << 4),
        BSDF_ALL(0x1f);

        public int value;
        BxDFType(int value) {
            this.value = value;
        }
    }

    // BxDF Public Data
    public BxDFType type;

    // BxDF Interface
    public BxDF(BxDFType type) {
        this.type = type;
    }
    public boolean MatchesFlags(BxDFType t) { return (type.value & t.value) == type.value; }
    public abstract Spectrum f(Vector3f wo, Vector3f wi);

    public static class BxDFSample {
        public Spectrum f;
        public Vector3f wiWorld;
        public float pdf;
        public BxDF.BxDFType sampledType;
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