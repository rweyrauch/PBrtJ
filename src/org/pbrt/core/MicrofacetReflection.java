/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class MicrofacetReflection extends BxDF {

    public MicrofacetReflection(Spectrum R, MicrofacetDistribution distribution, Fresnel fresnel) {
        super(BSDF_REFLECTION | BSDF_GLOSSY);
        this.R = new Spectrum(R);
        this.distribution = distribution;
        this.fresnel = fresnel;
    }

    @Override
    public Spectrum f(Vector3f wo, Vector3f wi) {
        float cosThetaO = Reflection.AbsCosTheta(wo), cosThetaI = Reflection.AbsCosTheta(wi);
        Vector3f wh = wi.add(wo);
        // Handle degenerate cases for microfacet reflection
        if (cosThetaI == 0 || cosThetaO == 0) return new Spectrum(0);
        if (wh.x == 0 && wh.y == 0 && wh.z == 0) return new Spectrum(0);
        wh = Vector3f.Normalize(wh);
        Spectrum F = fresnel.Evaluate(Vector3f.Dot(wi, wh));
        return R.scale(distribution.D(wh) * distribution.G(wo, wi)).multiply(F.scale(1 / (4 * cosThetaI * cosThetaO)));
    }

    @Override
    public String toString() {
        return "[ MicrofacetReflection R: " + R.toString() + " distribution: " + distribution.toString() +
                " fresnel: " + fresnel.toString() + " ]";
    }

    private final Spectrum R;
    private final MicrofacetDistribution distribution;
    private final Fresnel fresnel;
}