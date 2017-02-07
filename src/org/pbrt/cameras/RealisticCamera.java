
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

public class RealisticCamera extends Camera {

    public RealisticCamera(AnimatedTransform CameraToWorld, float shutterOpen, float shutterClose, Film film, Medium medium) {
        super(CameraToWorld, shutterOpen, shutterClose, film, medium);
    }

    public static Camera Create(ParamSet paramSet, AnimatedTransform animatedCam2World, Film film, Medium outside) {
        return null;
    }

    @Override
    public float GenerateRay(CameraSample sample, Ray ray) {
        return 0;
    }
}