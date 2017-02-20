/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public abstract class MicrofacetDistribution {

    public MicrofacetDistribution(boolean samplevis) {
        this.sampleVisibleArea = samplevis;
    }

    public abstract float D(Vector3f wh);
    public abstract float Lambda(Vector3f w);

    public float G1(Vector3f w) {
        return 1 / (1 + Lambda(w));
    }
    public float G(Vector3f wo, Vector3f wi) {
        return 1 / (1 + Lambda(wo) + Lambda(wi));
    }

    public abstract Vector3f Sample_wh(Vector3f wo, Point2f u);

    public float Pdf(Vector3f wo, Vector3f wh) {
        if (sampleVisibleArea)
            return D(wh) * G1(wo) * Vector3f.AbsDot(wo, wh) / Reflection.AbsCosTheta(wo);
        else
            return D(wh) * Reflection.AbsCosTheta(wh);
    }

    public abstract String ToString();

    protected final boolean sampleVisibleArea;
}