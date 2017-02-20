/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class FresnelConductor extends Fresnel {

    public FresnelConductor(Spectrum etaI, Spectrum etaT, Spectrum k) {
        this.etaI = etaI;
        this.etaT = etaT;
        this.k = k;
    }

    public Spectrum Evaluate(float cosI) {
        return null;
    }
    public String ToString() {
        return "";
    }

    private Spectrum etaI, etaT, k;
}