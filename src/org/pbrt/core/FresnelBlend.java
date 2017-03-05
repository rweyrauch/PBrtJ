/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class FresnelBlend extends BxDF {

    public FresnelBlend(Spectrum Rd, Spectrum Rs, MicrofacetDistribution distrib) {
        super(BSDF_REFLECTION | BSDF_GLOSSY);
        this.Rd = new Spectrum(Rd);
        this.Rs = new Spectrum(Rs);
        this.distribution = distrib;
    }

    @Override
    public Spectrum f(Vector3f wo, Vector3f wi) {
        Spectrum diffuse = Rd.scale((28.f / (23.f * (float)Math.PI))).multiply(new Spectrum(1).subtract(Rs)).scale((1 - pow5(1 - .5f * Reflection.AbsCosTheta(wi))) * (1 - pow5(1 - .5f * Reflection.AbsCosTheta(wo))));
        Vector3f wh = wi.add(wo);
        if (wh.x == 0 && wh.y == 0 && wh.z == 0) return new Spectrum(0);
        wh = Vector3f.Normalize(wh);
        Spectrum specular = SchlickFresnel(Vector3f.Dot(wi, wh)).scale(distribution.D(wh) / (4 * Vector3f.AbsDot(wi, wh) * Math.max(Reflection.AbsCosTheta(wi), Reflection.AbsCosTheta(wo))));
        return diffuse.add(specular);
    }

    @Override
    public BxDFSample Sample_f(Vector3f wo, Point2f uOrig) {
        BxDFSample bs = new BxDFSample();
        Point2f u = uOrig;
        if (u.x < .5f) {
            u.x = Math.min(2 * u.x, Pbrt.OneMinusEpsilon);
            // Cosine-sample the hemisphere, flipping the direction if necessary
            bs.wiWorld = Sampling.CosineSampleHemisphere(u);
            if (wo.z < 0) bs.wiWorld.z *= -1;
        }
        else {
            u.x = Math.min(2 * (u.x - .5f), Pbrt.OneMinusEpsilon);
            // Sample microfacet orientation $\wh$ and reflected direction $\wi$
            Vector3f wh = distribution.Sample_wh(wo, u);
            bs.wiWorld = Reflection.Reflect(wo, wh);
            if (!Reflection.SameHemisphere(wo, bs.wiWorld)) {
                bs.f = new Spectrum(0.f);
                return bs;
            }
        }
        bs.pdf = Pdf(wo, bs.wiWorld);
        bs.f = f(wo, bs.wiWorld);
        return bs;
    }

    @Override
    public float Pdf(Vector3f wo, Vector3f wi) {
        if (!Reflection.SameHemisphere(wo, wi)) return 0;
        Vector3f wh = Vector3f.Normalize(wo.add(wi));
        float pdf_wh = distribution.Pdf(wo, wh);
        return .5f * (Reflection.AbsCosTheta(wi) / (float)Math.PI + pdf_wh / (4 * Vector3f.Dot(wo, wh)));
    }

    @Override
    public String ToString() {
        return null;
    }

    private static float pow5(float v) { return (v * v) * (v * v) * v; };

    public Spectrum SchlickFresnel(float cosTheta) {
        return Rs.add((new Spectrum(1).subtract(Rs)).scale(pow5(1 - cosTheta)));
    }

    private final Spectrum Rd, Rs;
    private MicrofacetDistribution distribution;
}