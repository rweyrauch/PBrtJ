
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

public class ProjectionLight extends Light {

    public static Light Create(Transform light2world, Medium outside, ParamSet paramSet) {
        Spectrum I = paramSet.FindOneSpectrum("I", new Spectrum(1));
        Spectrum sc = paramSet.FindOneSpectrum("scale", new Spectrum(1));
        float fov = paramSet.FindOneFloat("fov", 45);
        String texname = paramSet.FindOneFilename("mapname", "");
        return new ProjectionLight(light2world, new MediumInterface(outside), I.multiply(sc), texname, fov);
    }

    public ProjectionLight(Transform light2world, MediumInterface medium, Spectrum I, String texname, float fov) {
        super(FlagDeltaPosition, light2world, medium, 1);
        this.pLight = light2world.xform(new Point3f(0, 0, 0));
        this.I = I;

        // Create _ProjectionLight_ MIP map
        ImageIO.SpectrumImage texels = ImageIO.Read(texname);
        if (texels != null)
            projectionMap = new MIPMapSpectrum(texels.resolution, texels.image, new Spectrum(0));

        // Initialize _ProjectionLight_ projection matrix
        float aspect = (projectionMap != null) ? ((float)texels.resolution.x / (float)texels.resolution.y) : 1;
        if (aspect > 1)
            screenBounds = new Bounds2f(new Point2f(-aspect, -1), new Point2f(aspect, 1));
        else
            screenBounds = new Bounds2f(new Point2f(-1, -1 / aspect), new Point2f(1, 1 / aspect));
        hither = 1e-3f;
        yon = 1e30f;
        lightProjection = Transform.Perspective(fov, hither, yon);

        // Compute cosine of cone surrounding projection directions
        float opposite = (float)Math.tan(Math.toRadians(fov) / 2);
        float tanDiag = opposite * (float)Math.sqrt(1 + 1 / (aspect * aspect));
        cosTotalWidth = (float)Math.cos(Math.atan(tanDiag));
    }

    public Spectrum Projection(Vector3f w) {
        Vector3f wl = WorldToLight.xform(w);
        // Discard directions behind projection light
        if (wl.z < hither) return new Spectrum(0);

        // Project point onto projection plane and compute light
        Point3f p = lightProjection.xform(new Point3f(wl.x, wl.y, wl.z));
        if (!Bounds2f.Inside(new Point2f(p.x, p.y), screenBounds)) return new Spectrum(0);
        if (projectionMap == null) return new Spectrum(1);
        Point2f st = new Point2f(screenBounds.Offset(new Point2f(p.x, p.y)));
        return projectionMap.Lookup(st);
    }

    @Override
    public LiResult Sample_Li(Interaction ref, Point2f u) {
        Stats.ProfilePhase pp = new Stats.ProfilePhase(Stats.Prof.LightSample);
        LiResult result = new LiResult();
        result.wi = Vector3f.Normalize(pLight.subtract(ref.p));
        result.pdf = 1;
        result.vis = new VisibilityTester(ref, new Interaction(pLight, ref.time, mediumInterface));
        result.spectrum = I.multiply(Projection(result.wi.negate()).scale(1 / Point3f.DistanceSquared(pLight, ref.p)));
        return result;
    }

    @Override
    public Spectrum Power() {
        Spectrum s = new Spectrum(1);
        if (projectionMap != null) {
            s = projectionMap.Lookup(new Point2f(.5f, .5f), .5f);
        }
        return s.multiply(I.scale(2 * (float)Math.PI * (1 - cosTotalWidth)));
    }

    @Override
    public float Pdf_Li(Interaction ref, Vector3f wi) {
        return 0;
    }

    @Override
    public LeResult Sample_Le(Point2f u1, Point2f u2, float time) {
        Stats.ProfilePhase pp = new Stats.ProfilePhase(Stats.Prof.LightSample);
        LeResult result = new LeResult();
        Vector3f v = Sampling.UniformSampleCone(u1, cosTotalWidth);
        result.ray = new Ray(pLight, LightToWorld.xform(v), Pbrt.Infinity, time, mediumInterface.inside);
        result.nLight = new Normal3f(result.ray.d);  /// same here
        result.pdfPos = 1;
        result.pdfDir = Sampling.UniformConePdf(cosTotalWidth);
        result.spectrum = I.multiply(Projection(result.ray.d));
        return result;
    }

    @Override
    public PdfResult Pdf_Le(Ray ray, Normal3f nLight) {
        Stats.ProfilePhase pp = new Stats.ProfilePhase(Stats.Prof.LightPdf);
        PdfResult result = new PdfResult();
        result.pdfPos = 0;
        result.pdfDir = (Reflection.CosTheta(WorldToLight.xform(ray.d)) >= cosTotalWidth)
                ? Sampling.UniformConePdf(cosTotalWidth)
                : 0;
        return result;
    }

    private MIPMapSpectrum projectionMap;
    private final Point3f pLight;
    private final Spectrum I;
    private Transform lightProjection;
    private float hither, yon;
    private Bounds2f screenBounds;
    private float cosTotalWidth;
}