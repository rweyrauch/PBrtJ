/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class HenyeyGreenstein extends PhaseFunction {

    private float g;

    public HenyeyGreenstein(float g) {
        this.g = g;
    }

    @Override
    public float p(Vector3f wo, Vector3f wi) {
        //ProfilePhase _(Prof::PhaseFuncEvaluation);
        return Medium.PhaseHG(Vector3f.Dot(wo, wi), g);
    }

    @Override
    public PhaseSample Sample_p(Vector3f wo, Point2f u) {
        //ProfilePhase _(Prof::PhaseFuncSampling);
        // Compute $\cos \theta$ for Henyey--Greenstein sample
        float cosTheta;
        if (Math.abs(g) < 1e-3)
            cosTheta = 1 - 2 * u.at(0);
        else {
            float sqrTerm = (1 - g * g) / (1 - g + 2 * g * u.at(0));
            cosTheta = (1 + g * g - sqrTerm * sqrTerm) / (2 * g);
        }

        // Compute direction _wi_ for Henyey--Greenstein sample
        float sinTheta = (float)Math.sqrt(Math.max(0, 1 - cosTheta * cosTheta));
        float phi = 2 * (float)Math.PI * u.at(1);
        Vector3f.CoordSystem cs = Vector3f.CoordinateSystem(wo);
        PhaseSample ps = new PhaseSample();
        ps.wi = Vector3f.SphericalDirection(sinTheta, cosTheta, phi, cs.v1, cs.v2, wo.negate());
        ps.phase = Medium.PhaseHG(-cosTheta, g);
        return ps;
    }
}
