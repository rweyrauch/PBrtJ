/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class SpecularTransmission extends BxDF {

    public SpecularTransmission(Spectrum T, float etaA, float etaB, Material.TransportMode mode) {
        super(BSDF_TRANSMISSION | BSDF_SPECULAR);
        this.T = T;
        this.etaA = etaA;
        this.etaB = etaB;
        this.fresnel = new FresnelDielectric(etaA, etaB);
        this.mode = mode;
    }

    @Override
    public Spectrum f(Vector3f wo, Vector3f wi) {
        return null;
    }

    @Override
    public String ToString() {
        return null;
    }

    private final Spectrum T;
    private final float etaA, etaB;
    private final FresnelDielectric fresnel;
    private final Material.TransportMode mode;
}