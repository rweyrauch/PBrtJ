/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class FresnelDielectric extends Fresnel {

    public FresnelDielectric(float etaI, float etaT) {
        this.etaI = etaI;
        this.etaT = etaT;
    }

    public Spectrum Evaluate(float cosI) {
        return null;
    }
    public String ToString() {
        return "";
    }

    private float etaI, etaT;
}