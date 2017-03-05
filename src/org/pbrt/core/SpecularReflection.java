/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class SpecularReflection extends BxDF {

    public SpecularReflection(Spectrum R, Fresnel fresnel) {
        super(BSDF_REFLECTION | BSDF_SPECULAR);
        this.R = new Spectrum(R);
        this.fresnel = fresnel;
    }

    @Override
    public Spectrum f(Vector3f wo, Vector3f wi) {
        return new Spectrum(0);
    }

    @Override
    public BxDFSample Sample_f(Vector3f wo, Point2f u) {
        BxDFSample bs = new BxDFSample();
        bs.wiWorld = new Vector3f(-wo.x, -wo.y, wo.z);
        bs.pdf = 1;
        bs.f = fresnel.Evaluate(Reflection.CosTheta(bs.wiWorld)).multiply(R.scale(1 / Reflection.AbsCosTheta(bs.wiWorld)));
        return bs;
    }

    @Override
    public float Pdf(Vector3f wo, Vector3f wi) {
        return 0;
    }

    @Override
    public String ToString() {
        return null;
    }

    private final Spectrum R;
    private final Fresnel fresnel;
}