
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class LambertianReflection extends BxDF {

    public LambertianReflection(Spectrum R) {
        super(BSDF_REFLECTION | BSDF_DIFFUSE);
        this.R = new Spectrum(R);
    }

    @Override
    public Spectrum f(Vector3f wo, Vector3f wi) {
        return Spectrum.Scale(R, Pbrt.InvPi);
    }

    @Override
    public Spectrum rho(Vector3f w, int nSamples, Point2f[] u) { return R; }

    @Override
    public Spectrum rho(int nSamples, Point2f[] u1, Point2f[] u2) { return R; }

    @Override
    public String toString() {
        return "[ LambertianReflection R: " + R.toString() + " ]";
    }

    private Spectrum R;
}