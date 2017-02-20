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
        this.R = R;
        this.fresnel = fresnel;
    }

    @Override
    public Spectrum f(Vector3f wo, Vector3f wi) {
        return null;
    }

    @Override
    public String ToString() {
        return null;
    }

    private final Spectrum R;
    private final Fresnel fresnel;
}