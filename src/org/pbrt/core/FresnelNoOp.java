/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class FresnelNoOp extends Fresnel {

    @Override
    public Spectrum Evaluate(float cosI) {
        return new Spectrum(1);
    }

    @Override
    public String ToString() {
        return "[ FresnelNoOp ]";
    }
}