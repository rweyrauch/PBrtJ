/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

import java.util.Arrays;

public class FresnelSpecular extends BxDF {

    public FresnelSpecular(Spectrum R, Spectrum T, float etaA, float etaB, Material.TransportMode mode) {
        super(BSDF_REFLECTION | BSDF_TRANSMISSION | BSDF_SPECULAR);
        this.R = new Spectrum(R);
        this.T = new Spectrum(T);
        this.etaA = etaA;
        this.etaB = etaB;
        this.mode = mode;
    }

    @Override
    public Spectrum f(Vector3f wo, Vector3f wi) {
        return new Spectrum(0);
    }

    @Override
    public BxDFSample Sample_f(Vector3f wo, Point2f u) {
        float F = Reflection.FrDielectric(Reflection.CosTheta(wo), etaA, etaB);
        BxDFSample sf = new BxDFSample();
        if (u.x < F) {
            // Compute specular reflection for _FresnelSpecular_

            // Compute perfect specular reflection direction
            sf.wiWorld = new Vector3f(-wo.x, -wo.y, wo.z);
            sf.sampledType = BSDF_SPECULAR | BSDF_REFLECTION;
            sf.pdf = F;
            sf.f = R.scale(F / Reflection.AbsCosTheta(sf.wiWorld));
            return sf;
        } else {
            // Compute specular transmission for _FresnelSpecular_

            // Figure out which $\eta$ is incident and which is transmitted
            boolean entering = Reflection.CosTheta(wo) > 0;
            float etaI = entering ? etaA : etaB;
            float etaT = entering ? etaB : etaA;

            // Compute ray direction for specular transmission
            final Vector3f refract = Reflection.Refract(wo, Normal3f.Faceforward(new Normal3f(0, 0, 1), wo), etaI / etaT);
            if (refract == null)
                return null;

            Spectrum ft = T.scale(1 - F);

            // Account for non-symmetry with transmission to different medium
            if (mode == Material.TransportMode.Radiance)
                ft = ft.scale((etaI * etaI) / (etaT * etaT));

            sf.wiWorld = refract;
            sf.sampledType = BSDF_SPECULAR | BSDF_TRANSMISSION;
            sf.pdf = 1 - F;
            sf.f = ft.scale(1 / Reflection.AbsCosTheta(sf.wiWorld));
            return sf;
        }
    }

    @Override
    public float Pdf(Vector3f wo, Vector3f wi) {
        return 0;
    }

    @Override
    public String ToString() {
        return null;
    }

    private final Spectrum R, T;
    private final float etaA, etaB;
    private final Material.TransportMode mode;
}