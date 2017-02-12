
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
import org.pbrt.core.Error;

public class EnvironmentCamera extends Camera {

    public EnvironmentCamera(AnimatedTransform cameraToWorld, float shutterOpen, float shutterClose, Film film, Medium medium) {
        super(cameraToWorld, shutterOpen, shutterClose, film, medium);
    }

    @Override
    public CameraRay GenerateRay(CameraSample sample) {
        //ProfilePhase prof(Prof::GenerateCameraRay);
        // Compute environment camera ray direction
        float theta = (float)Math.PI * sample.pFilm.y / film.fullResolution.y;
        float phi = 2 * (float)Math.PI * sample.pFilm.x / film.fullResolution.x;
        Vector3f dir = new Vector3f((float)Math.sin(theta) * (float)Math.cos(phi), (float)Math.cos(theta), (float)Math.sin(theta) * (float)Math.sin(phi));
        CameraRay cr = new CameraRay();
        cr.ray = new Ray(new Point3f(0, 0, 0), dir, Pbrt.Infinity, Pbrt.Lerp(sample.time, shutterOpen, shutterClose), medium);
        cr.ray = CameraToWorld.xform(cr.ray);
        cr.weight = 1;
        return cr;
    }

    public static Camera Create(ParamSet paramSet, AnimatedTransform cam2world, Film film, Medium medium) {
        // Extract common camera parameters from _ParamSet_
        float shutteropen = paramSet.FindOneFloat("shutteropen", 0);
        float shutterclose = paramSet.FindOneFloat("shutterclose", 1);
        if (shutterclose < shutteropen) {
            Error.Warning("Shutter close time [%f] < shutter open [%f].  Swapping them.", shutterclose, shutteropen);
            float temp = shutterclose;
            shutterclose = shutteropen;
            shutteropen = temp;
        }
        float lensradius = paramSet.FindOneFloat("lensradius", 0);
        float focaldistance = paramSet.FindOneFloat("focaldistance", 1e30f);
        float frame = paramSet.FindOneFloat("frameaspectratio", film.fullResolution.x / film.fullResolution.y);
        Bounds2f screen = new Bounds2f();
        if (frame > 1.f) {
            screen.pMin.x = -frame;
            screen.pMax.x = frame;
            screen.pMin.y = -1.f;
            screen.pMax.y = 1.f;
        } else {
            screen.pMin.x = -1.f;
            screen.pMax.x = 1.f;
            screen.pMin.y = -1.f / frame;
            screen.pMax.y = 1.f / frame;
        }
        Float[] sw = paramSet.FindFloat("screenwindow");
        if (sw != null) {
            if (sw.length == 4) {
                screen.pMin.x = sw[0];
                screen.pMax.x = sw[1];
                screen.pMin.y = sw[2];
                screen.pMax.y = sw[3];
            } else
                Error.Error("\"screenwindow\" should have four values");
        }
        return new EnvironmentCamera(cam2world, shutteropen, shutterclose, film, medium);
    }
}