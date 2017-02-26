
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
import org.pbrt.samplers.StratifiedSampler;

public class GonioPhotometricLight extends Light {

    public static Light Create(Transform light2world, Medium outside, ParamSet paramSet) {
        Spectrum I = paramSet.FindOneSpectrum("I", new Spectrum(1));
        Spectrum sc = paramSet.FindOneSpectrum("scale", new Spectrum(0));
        String texname = paramSet.FindOneFilename("mapname", "");
        return new GonioPhotometricLight(light2world, new MediumInterface(outside), I.multiply(sc), texname);
    }

    public GonioPhotometricLight(Transform light2world, MediumInterface medium, Spectrum I, String texname) {
        super(FlagDeltaPosition, light2world, medium, 1);
        this.pLight = light2world.xform(new Point3f(0, 0, 0));
        this.I = I;

        // Create _mipmap_ for _GonioPhotometricLight_
        ImageIO.SpectrumImage texels = ImageIO.Read(texname);
        if (texels != null)
            mipmap = new MIPMapSpectrum(texels.resolution, texels.image, new Spectrum(0));
    }

    public Spectrum Scale(Vector3f w) {
        Vector3f wp = Vector3f.Normalize(WorldToLight.xform(w));
        float temp = wp.y;
        wp.y = wp.z;
        wp.z = temp;
        float theta = Vector3f.SphericalTheta(wp);
        float phi = Vector3f.SphericalPhi(wp);
        Point2f st = new Point2f(phi / (2 * (float)Math.PI), theta / (float)Math.PI);
        return (mipmap == null) ? new Spectrum(1) : mipmap.Lookup(st);
    }

    @Override
    public LiResult Sample_Li(Interaction ref, Point2f u) {
        //ProfilePhase _(Prof::LightSample);
        LiResult result = new LiResult();
        result.wi = Vector3f.Normalize(pLight.subtract(ref.p));
        result.pdf = 1;
        result.vis = new VisibilityTester(ref, new Interaction(pLight, ref.time, mediumInterface));
        result.spectrum = I.multiply(Scale(result.wi.negate()).scale(1 / Point3f.DistanceSquared(pLight, ref.p)));
        return result;
    }

    @Override
    public Spectrum Power() {
        Spectrum s = new Spectrum(1);
        if (mipmap != null) {
            s = mipmap.Lookup(new Point2f(0.5f, 0.5f), 0.5f);
        }
        return I.multiply(s.scale(4 * (float)Math.PI ));
    }

    @Override
    public float Pdf_Li(Interaction ref, Vector3f wi) {
        return 0;
    }

    @Override
    public LeResult Sample_Le(Point2f u1, Point2f u2, float time) {
        //ProfilePhase _(Prof::LightSample);
        LeResult result = new LeResult();
        result.ray = new Ray(pLight, Sampling.UniformSampleSphere(u1), Pbrt.Infinity, time, mediumInterface.inside);
        result.nLight = new Normal3f(result.ray.d);
        result.pdfPos = 1;
        result.pdfDir = Sampling.UniformSpherePdf();
        result.spectrum = I.multiply(Scale(result.ray.d));
        return result;
    }

    @Override
    public PdfResult Pdf_Le(Ray ray, Normal3f nLight) {
        //ProfilePhase _(Prof::LightPdf);
        PdfResult result = new PdfResult();
        result.pdfPos = 0;
        result.pdfDir = Sampling.UniformSpherePdf();
        return result;
    }

    private final Point3f pLight;
    private final Spectrum I;
    private MIPMapSpectrum mipmap;
}