
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

public class DistantLight extends Light {

    public static Light Create(Transform light2world, ParamSet paramSet) {
        Spectrum L = paramSet.FindOneSpectrum("L", new Spectrum(1));
        Spectrum sc = paramSet.FindOneSpectrum("scale", new Spectrum(1));
        Point3f from = paramSet.FindOnePoint3f("from", new Point3f(0, 0, 0));
        Point3f to = paramSet.FindOnePoint3f("to", new Point3f(0, 0, 1));
        Vector3f dir = from.subtract(to);
        return new DistantLight(light2world, L.multiply(sc), dir);
    }

    public DistantLight(Transform light2world, Spectrum L, Vector3f wLight) {
        super(FlagDeltaDirection, light2world, new MediumInterface(), 1);
        this.L = new Spectrum(L);
        this.wLight = Vector3f.Normalize(light2world.xform(wLight));
    }

    public void Preprocess(Scene scene) {
        Bounds3f.BoundSphere bs = scene.WorldBound().BoundingSphere();
        worldCenter = bs.center;
        worldRadius = bs.radius;
    }

    @Override
    public LiResult Sample_Li(Interaction ref, Point2f u) {
        //Stats.ProfilePhase pp = new Stats.ProfilePhase(Stats.Prof.LightSample);
        LiResult result = new LiResult();
        result.wi = wLight;
        result.pdf = 1;
        Point3f pOutside = ref.p.add(wLight.scale(2 * worldRadius));
        result.vis = new VisibilityTester(ref, new Interaction(pOutside, ref.time, mediumInterface));
        result.spectrum = L;
        return result;
    }

    @Override
    public Spectrum Power() {
        return L.scale(Pbrt.Pi * worldRadius * worldRadius);
    }

    @Override
    public float Pdf_Li(Interaction ref, Vector3f wi) {
        return 0;
    }

    @Override
    public LeResult Sample_Le(Point2f u1, Point2f u2, float time) {
        //Stats.ProfilePhase pp = new Stats.ProfilePhase(Stats.Prof.LightSample);
        LeResult result = new LeResult();
        // Choose point on disk oriented toward infinite light direction
        Vector3f.CoordSystem cs = Vector3f.CoordinateSystem(wLight);
        Point2f cd = Sampling.ConcentricSampleDisk(u1);
        Point3f pDisk = worldCenter.add((cs.v2.scale(cd.x).add(cs.v3.scale(cd.y))).scale(worldRadius));

        // Set ray origin and direction for infinite light ray
        result.ray = new Ray(pDisk.add(wLight.scale(worldRadius)), wLight.negate(), Pbrt.Infinity, time, null);
        result.nLight = new Normal3f(result.ray.d);
        result.pdfPos = 1 / (Pbrt.Pi * worldRadius * worldRadius);
        result.pdfDir = 1;
        result.spectrum = L;
        return result;
    }

    @Override
    public PdfResult Pdf_Le(Ray ray, Normal3f nLight) {
        PdfResult result = new PdfResult();
        result.pdfPos = 1 / (Pbrt.Pi * worldRadius * worldRadius);
        result.pdfDir = 0;
        return result;
    }

    private final Spectrum L;
    private final Vector3f wLight;
    private Point3f worldCenter;
    private float worldRadius;
}