
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public abstract class Camera {
    // Camera Public Data
    public AnimatedTransform CameraToWorld;
    public final float shutterOpen, shutterClose;
    public Film film;
    public final Medium medium;

    public static class CameraSample {
        public Point2f pFilm;
        public Point2f pLens;
        public float time;

        @Override
        public String toString() {
            return "[ pFilm: " + this.pFilm.toString() + " , pLens: " + this.pLens.toString() +
                    String.format(", time %f ]", this.time);
        }
    }

    // Camera Interface
    public Camera(AnimatedTransform CameraToWorld, float shutterOpen, float shutterClose, Film film, Medium medium) {
        this.CameraToWorld = CameraToWorld.clone();
        this.shutterOpen = shutterOpen;
        this.shutterClose = shutterClose;
        this.film = film;
        this.medium = medium;

        if (CameraToWorld.HasScale()) {
            Error.Warning("Scaling detected in world-to-camera transformation!\n" +
                    "The system has numerous assumptions, implicit and explicit,\n" +
                    "that this transform will have no scale factors in it.\n" +
                    "Proceed at your own risk; your image may have errors or\n" +
                    "the system may crash as a result of this.");
        }
    }

    public class CameraRay {
        public Ray ray;
        public float weight;
    }

    public abstract CameraRay GenerateRay(CameraSample sample);

    public class CameraRayDiff {
        public RayDifferential rd;
        public float weight;
    }

    public CameraRayDiff GenerateRayDifferential(CameraSample sample) {
        CameraRay cray = GenerateRay(sample);
        // Find camera ray after shifting one pixel in the $x$ direction
        CameraSample sshift = sample;
        sshift.pFilm.x++;
        CameraRay rx = GenerateRay(sshift);
        if (rx.weight == 0) return null;
        CameraRayDiff crd = new CameraRayDiff();
        crd.rd = new RayDifferential(cray.ray);
        crd.rd.rxOrigin = rx.ray.o;
        crd.rd.rxDirection = rx.ray.d;

        // Find camera ray after shifting one pixel in the $y$ direction
        sshift.pFilm.x--;
        sshift.pFilm.y++;
        CameraRay ry = GenerateRay(sshift);
        if (ry.weight == 0) return null;
        crd.rd.ryOrigin = ry.ray.o;
        crd.rd.ryDirection = ry.ray.d;
        crd.rd.hasDifferentials = true;
        crd.weight = cray.weight;
        return crd;
    }

    public class CameraWe {
        public Point2f pRaster2;
        public Spectrum we;
    }
    public class CameraPdf {
        public float pdfPos;
        public float pdfDir;
    }
    public class CameraWi {
        public Vector3f wi;
        public float pdf;
        public Point2f pRaster;
        public Light.VisibilityTester vis;
        public Spectrum swe;
    }
    public CameraWe We(Ray ray) {
        Error.Error("Camera::We() is not implemented!");
        return null;
    }
    public CameraPdf Pdf_We(Ray ray) {
        Error.Error("Camera::Pdf_We() is not implemented!");
        return null;
    }

    public CameraWi Sample_Wi(Interaction ref, Point2f u) {
        Error.Error("Camera::Sample_Wi() is not implemented!");
        return null;
    }

}