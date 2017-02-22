
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

public class InfiniteAreaLight extends Light {

    public InfiniteAreaLight(Transform lightToWorld, Spectrum L, int nSamples, String texMap) {
        super(FlagInfinite, lightToWorld, new MediumInterface(), nSamples);

        // Read texel data from _texmap_ and initialize _Lmap_
        Point2i resolution = new Point2i(1, 1);
        Spectrum[] texels = null;
        if (!texMap.isEmpty()) {
            ImageIO.SpectrumImage image = ImageIO.Read(texMap);
            if (image != null) {
                texels = image.image;
                resolution = image.resolution;
                for (int i = 0; i < resolution.x * resolution.y; ++i)
                    texels[i].multiply(L);
            }
        }
        if (texels == null) {
            resolution.x = resolution.y = 1;
            texels = new Spectrum[1];
            texels[0] = L;
        }
        Lmap = new MIPMapSpectrum(resolution, texels, new Spectrum(0));

        // Initialize sampling PDFs for infinite area light
/*
        // Compute scalar-valued image _img_ from environment map
        int width = 2 * Lmap.Width(), height = 2 * Lmap.Height();
        float[] img = new float[width * height];
        float fwidth = 0.5f / Math.min(width, height);
        for (int v = 0; v < height; v++) {
            float vp = (v + .5f) / height;
            float sinTheta = (float)Math.sin(Math.PI * (v + .5f) / height);
            for (int u = 0; u < width; ++u) {
                float up = (u + .5f) / width;
                img[u + v * width] = Lmap.Lookup(new Point2f(up, vp), fwidth).y();
                img[u + v * width] *= sinTheta;
            }
        }

        // Compute sampling distributions for rows and columns of image
        distribution = new Distribution2D(img, width, height);
*/
    }

    public void Preprocess(Scene scene) {
        Bounds3f.BoundSphere bsphere = scene.WorldBound().BoundingSphere();
        worldCenter = bsphere.center;
        worldRadius = bsphere.radius;
    }

    @Override
    public LiResult Sample_Li(Interaction ref, Point2f u) {
        //ProfilePhase _(Prof::LightSample);
        LiResult result = new LiResult();
        // Find $(u,v)$ sample coordinates in infinite light texture
        Distribution2D.ContSample samp = distribution.SampleContinuous(u);
        float mapPdf = samp.pdf;
        Point2f uv = samp.sample;
        if (mapPdf == 0) {
            result.spectrum = new Spectrum(0);
            return result;
        }

        // Convert infinite light sample point to direction
        float theta = uv.y * (float)Math.PI, phi = uv.x * 2 * (float)Math.PI;
        float cosTheta = (float)Math.cos(theta), sinTheta = (float)Math.sin(theta);
        float sinPhi = (float)Math.sin(phi), cosPhi = (float)Math.cos(phi);
        result.wi = LightToWorld.xform(new Vector3f(sinTheta * cosPhi, sinTheta * sinPhi, cosTheta));

        // Compute PDF for sampled infinite light direction
        result.pdf = mapPdf / (2 * (float)Math.PI * (float)Math.PI * sinTheta);
        if (sinTheta == 0) result.pdf = 0;

        // Return radiance value for infinite light direction
        result.vis = new VisibilityTester(ref, new Interaction(ref.p.add(result.wi.scale(2 * worldRadius)),
                ref.time, mediumInterface));
        result.spectrum = Lmap.Lookup(uv);
        return result;
    }

    @Override
    public Spectrum Power() {
        Spectrum L = Lmap.Lookup(new Point2f(.5f, .5f), .5f);
        L.scale((float)Math.PI * worldRadius * worldRadius);
        return L;
    }

    @Override
    public Spectrum Le(RayDifferential ray) {
        Vector3f w = Vector3f.Normalize(WorldToLight.xform(ray.d));
        Point2f st = new Point2f(Vector3f.SphericalPhi(w) / (float)Math.PI, Vector3f.SphericalTheta(w) / (float)Math.PI);
        return Lmap.Lookup(st);
    }

    @Override
    public float Pdf_Li(Interaction ref, Vector3f w) {
        //ProfilePhase _(Prof::LightPdf);
        Vector3f wi = WorldToLight.xform(w);
        float theta = Vector3f.SphericalTheta(wi), phi = Vector3f.SphericalPhi(wi);
        float sinTheta = (float)Math.sin(theta);
        if (sinTheta == 0) return 0;
        return distribution.Pdf(new Point2f(phi / (float)Math.PI, theta / (float)Math.PI)) / (2 * (float)Math.PI * (float)Math.PI * sinTheta);
    }

    @Override
    public LeResult Sample_Le(Point2f u1, Point2f u2, float time) {
        LeResult result = new LeResult();
        //ProfilePhase _(Prof::LightSample);
        // Compute direction for infinite light sample ray
        Point2f u = u1;

        // Find $(u,v)$ sample coordinates in infinite light texture
        Distribution2D.ContSample samp = distribution.SampleContinuous(u);
        float mapPdf = samp.pdf;
        Point2f uv = samp.sample;
        if (mapPdf == 0) {
            result.spectrum = new Spectrum(0);
            return result;
        }
        float theta = uv.y * (float)Math.PI, phi = uv.x * 2.f * (float)Math.PI;
        float cosTheta = (float)Math.cos(theta), sinTheta = (float)Math.sin(theta);
        float sinPhi = (float)Math.sin(phi), cosPhi = (float)Math.cos(phi);
        Vector3f d = LightToWorld.xform(new Vector3f(sinTheta * cosPhi, sinTheta * sinPhi, cosTheta)).negate();
        result.nLight = new Normal3f(d);

        // Compute origin for infinite light sample ray
        Vector3f.CoordSystem cs = Vector3f.CoordinateSystem(d.negate());
        Vector3f v1 = cs.v2;
        Vector3f v2 = cs.v3;
        Point2f cd = Sampling.ConcentricSampleDisk(u2);
        Point3f pDisk = worldCenter.add((v1.scale(cd.x).add(v2.scale(cd.y))).scale(worldRadius));
        result.ray = new Ray(pDisk.add(d.negate().scale(worldRadius)), d, Pbrt.Infinity, time, null);

        // Compute _InfiniteAreaLight_ ray PDFs
        result.pdfDir = sinTheta == 0 ? 0 : mapPdf / (2 * (float)Math.PI * (float)Math.PI * sinTheta);
        result.pdfPos = 1 / ((float)Math.PI * worldRadius * worldRadius);
        result.spectrum = Lmap.Lookup(uv);
        return result;
    }

    @Override
    public PdfResult Pdf_Le(Ray ray, Normal3f nLight) {
        //ProfilePhase _(Prof::LightPdf);
        Vector3f d = WorldToLight.xform(ray.d).negate();
        float theta = Vector3f.SphericalTheta(d), phi = Vector3f.SphericalPhi(d);
        Point2f uv = new Point2f(phi / (float)Math.PI, theta / (float)Math.PI);
        float mapPdf = distribution.Pdf(uv);
        PdfResult result = new PdfResult();
        result.pdfDir = mapPdf / (2 * (float)Math.PI * (float)Math.PI * (float)Math.sin(theta));
        result.pdfPos = 1 / ((float)Math.PI * worldRadius * worldRadius);
        return result;
    }

    public static InfiniteAreaLight Create(Transform light2world, ParamSet paramSet) {
        Spectrum L = paramSet.FindOneSpectrum("L", new Spectrum(1));
        Spectrum sc = paramSet.FindOneSpectrum("scale", new Spectrum(1));
        String texmap = paramSet.FindOneFilename("mapname", "");
        int nSamples = paramSet.FindOneInt("samples", paramSet.FindOneInt("nsamples", 1));
        if (Pbrt.options.QuickRender) nSamples = Math.max(1, nSamples / 4);
        Spectrum Lsc = Spectrum.Multiply(L, sc);
        return new InfiniteAreaLight(light2world, Lsc, nSamples, texmap);
    }

    private MIPMapSpectrum Lmap;
    private Point3f worldCenter;
    private float worldRadius;
    private Distribution2D distribution;

}