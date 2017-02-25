/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class MicrofacetTransmission extends BxDF {

    public MicrofacetTransmission(Spectrum T, MicrofacetDistribution distribution, float etaA, float etaB, Material.TransportMode mode) {
        super(BSDF_TRANSMISSION | BSDF_GLOSSY);
        this.T = T;
        this.distribution = distribution;
        this.etaA = etaA;
        this.etaB = etaB;
        this.fresnel = new FresnelDielectric(etaA, etaB);
        this.mode = mode;
    }

    @Override
    public Spectrum f(Vector3f wo, Vector3f wi) {
        if (Reflection.SameHemisphere(wo, wi)) return new Spectrum(0);  // transmission only

        float cosThetaO = Reflection.CosTheta(wo);
        float cosThetaI = Reflection.CosTheta(wi);
        if (cosThetaI == 0 || cosThetaO == 0) return new Spectrum(0);

        // Compute $\wh$ from $\wo$ and $\wi$ for microfacet transmission
        float eta = Reflection.CosTheta(wo) > 0 ? (etaB / etaA) : (etaA / etaB);
        Vector3f wh = Vector3f.Normalize(wo.add(wi.scale(eta)));
        if (wh.z < 0) wh = wh.negate();

        Spectrum F = fresnel.Evaluate(Vector3f.Dot(wo, wh));

        float sqrtDenom = Vector3f.Dot(wo, wh) + eta * Vector3f.Dot(wi, wh);
        float factor = (mode == Material.TransportMode.Radiance) ? (1 / eta) : 1;

        return (new Spectrum(1.f).subtract(F)).multiply(T.scale(Math.abs(distribution.D(wh) * distribution.G(wo, wi) * eta * eta *
                Vector3f.AbsDot(wi, wh) * Vector3f.AbsDot(wo, wh) * factor * factor /
                (cosThetaI * cosThetaO * sqrtDenom * sqrtDenom))));
    }

    @Override
    public BxDFSample Sample_f(Vector3f wo, Point2f u) {
        if (wo.z == 0) return null;
        Vector3f wh = distribution.Sample_wh(wo, u);
        float eta = Reflection.CosTheta(wo) > 0 ? (etaA / etaB) : (etaB / etaA);
        Vector3f wi = Reflection.Refract(wo, new Normal3f(wh), eta);
        if (wi == null) return null;
        BxDFSample bs = new BxDFSample();
        bs.pdf = Pdf(wo, wi);
        bs.wiWorld = wi;
        bs.f = f(wo, wi);
        return bs;
    }

    @Override
    public float Pdf(Vector3f wo, Vector3f wi) {
        if (Reflection.SameHemisphere(wo, wi)) return 0;
        // Compute $\wh$ from $\wo$ and $\wi$ for microfacet transmission
        float eta = Reflection.CosTheta(wo) > 0 ? (etaB / etaA) : (etaA / etaB);
        Vector3f wh = Vector3f.Normalize(wo.add(wi.scale(eta)));

        // Compute change of variables _dwh\_dwi_ for microfacet transmission
        float sqrtDenom = Vector3f.Dot(wo, wh) + eta * Vector3f.Dot(wi, wh);
        float dwh_dwi = Math.abs((eta * eta * Vector3f.Dot(wi, wh)) / (sqrtDenom * sqrtDenom));
        return distribution.Pdf(wo, wh) * dwh_dwi;
    }

    @Override
    public String ToString() {
        return null;
    }

    private final Spectrum T;
    private final MicrofacetDistribution distribution;
    private final float etaA, etaB;
    private final FresnelDielectric fresnel;
    private final Material.TransportMode mode;
}