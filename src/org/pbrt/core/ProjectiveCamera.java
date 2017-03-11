
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public abstract class ProjectiveCamera extends Camera {
    // ProjectiveCamera Protected Data
    protected Transform CameraToScreen, RasterToCamera;
    protected Transform ScreenToRaster, RasterToScreen;
    protected float lensRadius, focalDistance;

    public ProjectiveCamera(AnimatedTransform CameraToWorld, Transform CameraToScreen, Bounds2f screenWindow, float shutterOpen, float shutterClose, float lensr, float focald, Film film, Medium medium) {
        super(CameraToWorld, shutterOpen, shutterClose, film, medium);
        this.CameraToScreen = new Transform(CameraToScreen);
        // Initialize depth of field parameters
        this.lensRadius = lensr;
        this.focalDistance = focald;

        // Compute projective camera transformations

        // Compute projective camera screen transformations
        ScreenToRaster = Transform.Scale(film.fullResolution.x, film.fullResolution.y, 1).concatenate(Transform.Scale(1 / (screenWindow.pMax.x - screenWindow.pMin.x), 1 / (screenWindow.pMin.y - screenWindow.pMax.y), 1)).concatenate(Transform.Translate(new Vector3f(-screenWindow.pMin.x, -screenWindow.pMax.y, 0)));
        RasterToScreen = Transform.Inverse(ScreenToRaster);
        RasterToCamera = Transform.Inverse(CameraToScreen).concatenate(RasterToScreen);
    }
}