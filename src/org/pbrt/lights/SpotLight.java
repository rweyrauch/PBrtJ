
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

public class SpotLight extends Light {

    public static Light Create(Transform l2w, Medium outside, ParamSet paramSet) {
        Spectrum I = paramSet.FindOneSpectrum("I", new Spectrum(1));
        Spectrum sc = paramSet.FindOneSpectrum("scale", new Spectrum(1));
        float coneangle = paramSet.FindOneFloat("coneangle", 30);
        float conedelta = paramSet.FindOneFloat("conedeltaangle", 5);
        // Compute spotlight world to light transformation
        Point3f from = paramSet.FindOnePoint3f("from", new Point3f(0, 0, 0));
        Point3f to = paramSet.FindOnePoint3f("to", new Point3f(0, 0, 1));
        Vector3f dir = Vector3f.Normalize(to.subtract(from));
        Vector3f du, dv;
        Vector3f.CoordSystem cs = Vector3f.CoordinateSystem(dir);
        du = cs.v2;
        dv = cs.v3;
        Transform dirToZ = new Transform(new Matrix4x4(du.x, du.y, du.z, 0, dv.x, dv.y, dv.z, 0, dir.x,
                        dir.y, dir.z, 0, 0, 0, 0, 1));
        Transform light2world = l2w.concatenate(Transform.Translate(new Vector3f(from.x, from.y, from.z))).concatenate(Transform.Inverse(dirToZ));
        return new SpotLight(light2world, new MediumInterface(outside), I.multiply(sc), coneangle, coneangle - conedelta);
    }

    public SpotLight(Transform light2world, MediumInterface mi, Spectrum I, float totalWidth, float falloffStart) {
        super(FlagDeltaPosition, light2world, mi, 1);
        this.pLight = light2world.xform(new Point3f(0, 0, 0));
        this.I = new Spectrum(I);
        this.cosTotalWidth = (float)Math.cos(Math.toRadians(totalWidth));
        this.cosFalloffStart = (float)Math.cos(Math.toRadians(falloffStart));
    }

    @Override
    public LiResult Sample_Li(Interaction ref, Point2f u) {
        Stats.ProfilePhase pp = new Stats.ProfilePhase(Stats.Prof.LightSample);
        LiResult result = new LiResult();
        result.wi = Vector3f.Normalize(pLight.subtract(ref.p));
        result.pdf = 1.f;
        result.vis = new VisibilityTester(ref, new Interaction(pLight, ref.time, mediumInterface));
        result.spectrum = I.scale(falloff(result.wi.negate()) / Point3f.DistanceSquared(pLight, ref.p));
        return result;
    }

    @Override
    public Spectrum Power() {
        return I.scale(2 * (float)Math.PI * (1 - .5f * (cosFalloffStart + cosTotalWidth)));
    }

    @Override
    public float Pdf_Li(Interaction ref, Vector3f wi) {
        return 0;
    }

    @Override
    public LeResult Sample_Le(Point2f u1, Point2f u2, float time) {
        Stats.ProfilePhase pp = new Stats.ProfilePhase(Stats.Prof.LightSample);
        LeResult result = new LeResult();
        Vector3f w = Sampling.UniformSampleCone(u1, cosTotalWidth);
        result.ray = new Ray(pLight, LightToWorld.xform(w), Pbrt.Infinity, time, mediumInterface.inside);
        result.nLight = new Normal3f(result.ray.d);
        result.pdfPos = 1;
        result.pdfDir = Sampling.UniformConePdf(cosTotalWidth);
        result.spectrum = I.scale(falloff(result.ray.d));
        return result;
    }

    @Override
    public PdfResult Pdf_Le(Ray ray, Normal3f nLight) {
        Stats.ProfilePhase pp = new Stats.ProfilePhase(Stats.Prof.LightPdf);
        PdfResult result = new PdfResult();
        result.pdfPos = 0;
        result.pdfDir = (Reflection.CosTheta(WorldToLight.xform(ray.d)) >= cosTotalWidth)
                ? Sampling.UniformConePdf(cosTotalWidth) : 0;
        return result;
    }

    private float falloff(Vector3f w) {
        Vector3f wl = Vector3f.Normalize(WorldToLight.xform(w));
        float cosTheta = wl.z;
        if (cosTheta < cosTotalWidth) return 0;
        if (cosTheta > cosFalloffStart) return 1;
        // Compute falloff inside spotlight cone
        float delta = (cosTheta - cosTotalWidth) / (cosFalloffStart - cosTotalWidth);
        return (delta * delta) * (delta * delta);
    }

    private final Point3f pLight;
    private final Spectrum I;
    private final float cosTotalWidth, cosFalloffStart;
}