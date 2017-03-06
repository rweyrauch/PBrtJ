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
        this.T = new Spectrum(T);
        this.etaA = etaA;
        this.etaB = etaB;
        this.fresnel = new FresnelDielectric(etaA, etaB);
        this.mode = mode;
    }

    @Override
    public Spectrum f(Vector3f wo, Vector3f wi) {
        return new Spectrum(0);
    }

    @Override
    public BxDFSample Sample_f(Vector3f wo, Point2f u) {
        BxDFSample bs = new BxDFSample();
        bs.pdf = 0;
        bs.f = new Spectrum(0);
        // Figure out which $\eta$ is incident and which is transmitted
        boolean entering = Reflection.CosTheta(wo) > 0;
        float etaI = entering ? etaA : etaB;
        float etaT = entering ? etaB : etaA;

        // Compute ray direction for specular transmission
        Vector3f refract = Reflection.Refract(wo, Normal3f.Faceforward(new Normal3f(0, 0, 1), wo), etaI / etaT);
        if (refract == null) {
            return bs;
        }
        bs.wiWorld = refract;
        bs.pdf = 1;
        Spectrum ft = (T.multiply(new Spectrum(1)).subtract(fresnel.Evaluate(Reflection.CosTheta(bs.wiWorld))));
        // Account for non-symmetry with transmission to different medium
        if (mode == Material.TransportMode.Radiance) ft = ft.scale((etaI * etaI) / (etaT * etaT));
        bs.f = ft.scale(1 / Reflection.AbsCosTheta(bs.wiWorld));

        return bs;
    }

    @Override
    public float Pdf(Vector3f wo, Vector3f wi) {
        return 0;
    }

    @Override
    public String toString() {
        return "[ SpecularTransmission: T: " + T.toString() + String.format(" etaA: %f etaB: %f ", etaA, etaB) +
                " fresnel: " + fresnel.toString() + " mode : " + mode.toString() + " ]";
    }

    private final Spectrum T;
    private final float etaA, etaB;
    private final FresnelDielectric fresnel;
    private final Material.TransportMode mode;
}