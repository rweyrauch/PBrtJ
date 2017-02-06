
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public abstract class Light {

    // LightFlags Declarations
    public static final int FlagDeltaPosition = 1;
    public static final int FlagDeltaDirection = 2;
    public static final int FlagArea = 4;
    public static final int FlagInfinite = 8;

    public class VisibilityTester {
        private Interaction p0, p1;

        public VisibilityTester() {
            p0 = null;
            p1 = null;
        }
        // VisibilityTester Public Methods
        public VisibilityTester(Interaction p0, Interaction p1) {
            this.p0 = p0;
            this.p1 = p1;
        }
        public Interaction P0() { return p0; }
        public Interaction P1() { return p1; }
        boolean Unoccluded(Scene scene) {
            return !scene.IntersectP(p0.SpawnRayTo(p1));
        }
        Spectrum Tr(Scene scene, Sampler sampler) {
            Ray ray = new Ray(p0.SpawnRayTo(p1));
            Spectrum Tr = new Spectrum(1.0f);
            while (true) {
                SurfaceInteraction isect = scene.Intersect(ray);
                // Handle opaque surface along ray's path
                if (isect != null && isect.primitive.GetMaterial() != null)
                    return new Spectrum(0.0f);

                // Update transmittance for current ray segment
                if (ray.medium != null) Tr.multiply(ray.medium.Tr(ray, sampler));

                // Generate next ray segment or return final transmittance
                if (isect == null) break;
                ray = isect.SpawnRayTo(p1);
            }
            return Tr;
        }
    }

    public class LiResult {
        Spectrum spectrum;
        Vector3f wi;
        float pdf;
        VisibilityTester vis;
    }
    public class LeResult {
        Spectrum spectrum;
        Ray ray;
        Normal3f nLight;
        float pdfPos;
        float pdfDir;
    }
    public class PdfResult {
        float pdfPos;
        float pdfDir;
    }
    public Light() {
    }

    public Light(int flags,  Transform LightToWorld,
           MediumInterface mediumInterface, int nSamples) {
        this.flags = flags;
        this.nSamples = Math.max(1, nSamples);
        this.mediumInterface = mediumInterface;
        this.LightToWorld = LightToWorld;
        this.WorldToLight = Transform.Inverse(LightToWorld);
    }

    public abstract LiResult Sample_Li(Interaction ref, Point2f u);
    public abstract Spectrum Power();
    public void Preprocess(Scene scene) {}
    public Spectrum Le(RayDifferential r) {
        return new Spectrum(0.0f);
    }
    public abstract float Pdf_Li(Interaction ref, Vector3f wi);
    public abstract LeResult Sample_Le(Point2f u1, Point2f u2, float time);
    public abstract PdfResult Pdf_Le(Ray ray, Normal3f nLight);

    // Light Public Data
    public int flags;
    public int nSamples;
    public MediumInterface mediumInterface;

    // Light Protected Data
    protected Transform LightToWorld, WorldToLight;
}