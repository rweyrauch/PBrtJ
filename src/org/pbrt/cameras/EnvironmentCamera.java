
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */
package org.pbrt.cameras;

import org.pbrt.core.*;

public class EnvironmentCamera extends Camera {

    public EnvironmentCamera(AnimatedTransform CameraToWorld, float shutterOpen, float shutterClose, Film film, Medium medium) {
        super(CameraToWorld, shutterOpen, shutterClose, film, medium);
    }

    @Override
    public CameraRay GenerateRay(CameraSample sample) {
        return null;
    }

    public static Camera Create(ParamSet paramSet, AnimatedTransform animatedCam2World, Film film, Medium outside) {
        return null;
    }
}