
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
    public float shutterOpen, shutterClose;
    public Film film;
    public Medium medium;

    public static class CameraSample {
        public Point2f pFilm;
        public Point2f pLens;
        public float time;
    }

    // Camera Interface
    public Camera(AnimatedTransform CameraToWorld, float shutterOpen, float shutterClose, Film film, Medium medium) {
        this.CameraToWorld = CameraToWorld;
        this.shutterOpen = shutterOpen;
        this.shutterClose = shutterClose;
        this.film = film;
        this.medium = medium;
    }

    public abstract float GenerateRay(CameraSample sample, Ray ray);

    public float GenerateRayDifferential(CameraSample sample, RayDifferential rd) {
        return 0;
    }
    public Spectrum We(Ray ray, Point2f pRaster2) {
        return null;
    }
    public void Pdf_We(Ray ray, float pdfPos, float pdfDir) {

    }
    public Spectrum Sample_Wi(Interaction ref, Point2f u, Vector3f wi, float pdf, Point2f pRaster, Light.VisibilityTester vis) {
        return null;
    }

}