
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
import org.pbrt.core.Error;
import org.pbrt.shapes.Triangle;

public class DiffuseAreaLight extends AreaLight {

    public static DiffuseAreaLight Create(Transform light2world, Medium outside, ParamSet paramSet, Shape shape) {
        Spectrum L = paramSet.FindOneSpectrum("L", new Spectrum(1));
        Spectrum sc = paramSet.FindOneSpectrum("scale", new Spectrum(1));
        int nSamples = paramSet.FindOneInt("samples", paramSet.FindOneInt("nsamples", 1));
        boolean twoSided = paramSet.FindOneBoolean("twosided", false);
        if (Pbrt.options.QuickRender) nSamples = Math.max(1, nSamples / 4);
        return new DiffuseAreaLight(light2world, new MediumInterface(outside), L.multiply(sc), nSamples, shape, twoSided);
    }

    public DiffuseAreaLight(Transform LightToWorld, MediumInterface medium, Spectrum Le, int nSamples, Shape shape, boolean twoSided) {
        super(LightToWorld, medium, nSamples);
        assert shape != null;
        this.Lemit = new Spectrum(Le);
        this.shape = shape;
        this.twoSided = twoSided;
        this.area = shape.Area();

        // Warn if light has transformation with non-uniform scale, though not
        // for Triangles, since this doesn't matter for them.
        if (WorldToLight.HasScale() && shape.getClass() != Triangle.class) {
            Error.Warning("Scaling detected in world to light transformation! " +
                    "The system has numerous assumptions, implicit and explicit, " +
                    "that this transform will have no scale factors in it. " +
                    "Proceed at your own risk; your image may have errors.");
        }
    }

    public DiffuseAreaLight(Transform LightToWorld, MediumInterface medium, Spectrum Le, int nSamples, Shape shape) {
        this(LightToWorld, medium, Le, nSamples, shape, false);
    }

    @Override
    public Spectrum L(Interaction intr, Vector3f w) {
        return (twoSided || Normal3f.Dot(intr.n, w) > 0) ? Lemit : new Spectrum(0);
    }

    @Override
    public LiResult Sample_Li(Interaction ref, Point2f u) {
        LiResult result = new LiResult();
        Stats.ProfilePhase pp = new Stats.ProfilePhase(Stats.Prof.LightSample);
        Shape.SampleResult sampResult = shape.Sample(ref, u);
        Interaction pShape = sampResult.isect;
        result.pdf = sampResult.pdf;
        pShape.mediumInterface = mediumInterface;
        if (result.pdf == 0 || (pShape.p.subtract(ref.p)).LengthSquared() == 0) {
            result.pdf = 0;
            result.spectrum = new Spectrum(0);
            return result;
        }
        result.wi = Vector3f.Normalize(pShape.p.subtract(ref.p));
        result.vis = new VisibilityTester(ref, pShape);
        result.spectrum = L(pShape, result.wi.negate());
        return result;
    }

    @Override
    public Spectrum Power() {
        return Lemit.scale(area * Pbrt.Pi * (twoSided ? 2 : 1));
    }

    @Override
    public float Pdf_Li(Interaction ref, Vector3f wi) {
        Stats.ProfilePhase pp = new Stats.ProfilePhase(Stats.Prof.LightPdf);
        return shape.Pdf(ref, wi);
    }

    @Override
    public LeResult Sample_Le(Point2f u1, Point2f u2, float time) {
        LeResult result = new LeResult();
        Stats.ProfilePhase pp = new Stats.ProfilePhase(Stats.Prof.LightSample);
        // Sample a point on the area light's _Shape_, _pShape_
        Shape.SampleResult sampRes = shape.Sample(u1);
        Interaction pShape = sampRes.isect;
        pShape.mediumInterface = mediumInterface;
        result.nLight = pShape.n;

        // Sample a cosine-weighted outgoing direction _w_ for area light
        Vector3f w;
        if (twoSided) {
            Point2f u = new Point2f(u2);
            // Choose a side to sample and then remap u[0] to [0,1] before
            // applying cosine-weighted hemisphere sampling for the chosen side.
            if (u.x < .5) {
                u.x = Math.min(u.x * 2, Pbrt.OneMinusEpsilon);
                w = Sampling.CosineSampleHemisphere(u);
            } else {
                u.x = Math.min((u.x - .5f) * 2, Pbrt.OneMinusEpsilon);
                w = Sampling.CosineSampleHemisphere(u);
                w.z *= -1;
            }
            result.pdfDir = 0.5f * Sampling.CosineHemispherePdf(Math.abs(w.z));
        } else {
            w = Sampling.CosineSampleHemisphere(u2);
            result.pdfDir = Sampling.CosineHemispherePdf(w.z);
        }

        Vector3f n = new Vector3f(pShape.n);
        Vector3f.CoordSystem cs = Vector3f.CoordinateSystem(n);
        w = cs.v2.scale(w.x).add(cs.v3.scale(w.y).add(n.scale(w.z)));
        result.ray = pShape.SpawnRay(w);
        result.spectrum = L(pShape, w);
        return result;
    }

    @Override
    public PdfResult Pdf_Le(Ray ray, Normal3f nLight) {
        PdfResult result = new PdfResult();
        Stats.ProfilePhase pp = new Stats.ProfilePhase(Stats.Prof.LightPdf);
        Interaction it = new Interaction(ray.o, nLight, new Vector3f(), new Vector3f(nLight), ray.time, mediumInterface);
        result.pdfPos = shape.Pdf(it);
        result.pdfDir = twoSided ? (0.5f * Sampling.CosineHemispherePdf(Normal3f.AbsDot(nLight, ray.d)))
                : Sampling.CosineHemispherePdf(Normal3f.Dot(nLight, ray.d));
        return result;
    }

    private final Spectrum Lemit;
    private Shape shape;
    private final boolean twoSided;
    private final float area;
}