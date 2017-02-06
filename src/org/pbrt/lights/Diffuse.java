
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.lights;

import org.pbrt.core.*;

public class Diffuse extends AreaLight {

    public Diffuse(Transform LightToWorld, MediumInterface medium, int nSamples) {
        super(LightToWorld, medium, nSamples);
    }

    public static AreaLight Create(Transform light2world, Medium outside, ParamSet paramSet, Shape shape) {
        return null;
    }

    @Override
    public Spectrum L(Interaction intr, Vector3f w) {
        return null;
    }

    @Override
    public LiResult Sample_Li(Interaction ref, Point2f u) {
        return null;
    }

    @Override
    public Spectrum Power() {
        return null;
    }

    @Override
    public float Pdf_Li(Interaction ref, Vector3f wi) {
        return 0;
    }

    @Override
    public LeResult Sample_Le(Point2f u1, Point2f u2, float time) {
        return null;
    }

    @Override
    public PdfResult Pdf_Le(Ray ray, Normal3f nLight) {
        return null;
    }
}