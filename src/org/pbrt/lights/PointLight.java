
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

public class PointLight extends Light {

    public static Light Create(Transform light2world, Medium outside, ParamSet paramSet) {
        Spectrum I = paramSet.FindOneSpectrum("I", new Spectrum(1));
        Spectrum sc = paramSet.FindOneSpectrum("scale", new Spectrum(1));
        Point3f P = paramSet.FindOnePoint3f("from", new Point3f(0, 0, 0));
        Transform l2w = Transform.Translate(new Vector3f(P.x, P.y, P.z)).concatenate(light2world);
        return new PointLight(light2world, new MediumInterface(outside), I.multiply(sc));
    }

    public PointLight(Transform light2world, MediumInterface mediumInterface, Spectrum I) {
        super(FlagDeltaPosition, light2world, mediumInterface, 1);
        this.pLight = light2world.xform(new Point3f(0, 0, 0));
        this.I = new Spectrum(I);
    }

    @Override
    public LiResult Sample_Li(Interaction ref, Point2f u) {
        Stats.ProfilePhase pp = new Stats.ProfilePhase(Stats.Prof.LightSample);
        LiResult result = new LiResult();
        result.wi = Vector3f.Normalize(pLight.subtract(ref.p));
        result.pdf = 1;
        result.vis = new VisibilityTester(ref, new Interaction(pLight, ref.time, mediumInterface));
        result.spectrum = I.scale(1 / Point3f.DistanceSquared(pLight, ref.p));
        return result;
    }

    @Override
    public Spectrum Power() {
        return I.scale(4 * Pbrt.Pi);
    }

    @Override
    public float Pdf_Li(Interaction ref, Vector3f wi) {
        return 0;
    }

    @Override
    public LeResult Sample_Le(Point2f u1, Point2f u2, float time) {
        Stats.ProfilePhase pp = new Stats.ProfilePhase(Stats.Prof.LightSample);
        LeResult result = new LeResult();
        result.ray = new Ray(pLight, Sampling.UniformSampleSphere(u1), Pbrt.Infinity, time, mediumInterface.inside);
        result.nLight = new Normal3f(result.ray.d);
        result.pdfPos = 1;
        result.pdfDir = Sampling.UniformSpherePdf();
        result.spectrum = I;
        return result;
    }

    @Override
    public PdfResult Pdf_Le(Ray ray, Normal3f nLight) {
        Stats.ProfilePhase pp = new Stats.ProfilePhase(Stats.Prof.LightPdf);
        PdfResult result = new PdfResult();
        result.pdfPos = 0;
        result.pdfDir = Sampling.UniformSpherePdf();
        return result;
    }

    private final Point3f pLight;
    private final Spectrum I;
}