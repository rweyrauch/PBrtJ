
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class SeparableBSSRDFAdapter extends BxDF {

    public SeparableBSSRDFAdapter(SeparableBSSRDF bssrdf) {
        super(BSDF_REFLECTION | BSDF_DIFFUSE);
        this.bssrdf = bssrdf;
    }

    @Override
    public Spectrum f(Vector3f wo, Vector3f wi) {
        Spectrum f = bssrdf.Sw(wi);
        // Update BSSRDF transmission term to account for adjoint light
        // transport
        if (bssrdf.mode == Material.TransportMode.Radiance)
            f = f.scale(bssrdf.eta * bssrdf.eta);
        return f;
    }

    @Override
    public String toString() {
        return "[ SeparableBSSRDFAdapter ]";
    }

    private final SeparableBSSRDF bssrdf;
}
