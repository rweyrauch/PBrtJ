
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

public class OrthographicCamera extends ProjectiveCamera {

    // OrthographicCamera Private Data
    private Vector3f dxCamera, dyCamera;

    public OrthographicCamera(AnimatedTransform CameraToWorld, Bounds2f screenWindow, float shutterOpen, float shutterClose, float lensRadius, float focalDistance, Film film, Medium medium) {
        super(CameraToWorld, Transform.Orthographic(0, 1), screenWindow, shutterOpen, shutterClose, lensRadius, focalDistance, film, medium);
        // Compute differential changes in origin for orthographic camera rays
        dxCamera = RasterToCamera.xform(new Vector3f(1, 0, 0));
        dyCamera = RasterToCamera.xform(new Vector3f(0, 1, 0));
    }

    @Override
    public CameraRay GenerateRay(CameraSample sample) {
        Stats.ProfilePhase prof = new Stats.ProfilePhase(Stats.Prof.GenerateCameraRay);
        // Compute raster and camera sample positions
        Point3f pFilm = new Point3f(sample.pFilm.x, sample.pFilm.y, 0);
        Point3f pCamera = RasterToCamera.xform(pFilm);
        CameraRay cr = new CameraRay();
        cr.ray = new Ray(pCamera, new Vector3f(0, 0, 1));
        // Modify ray for depth of field
        if (lensRadius > 0) {
            // Sample point on lens
            Point2f pLens = Sampling.ConcentricSampleDisk(sample.pLens).scale(lensRadius);

            // Compute point on plane of focus
            float ft = focalDistance / cr.ray.d.z;
            Point3f pFocus = cr.ray.at(ft);

            // Update ray for effect of lens
            cr.ray.o = new Point3f(pLens.x, pLens.y, 0);
            cr.ray.d = Vector3f.Normalize(pFocus.subtract(cr.ray.o));
        }
        cr.ray.time = Pbrt.Lerp(sample.time, shutterOpen, shutterClose);
        cr.ray.medium = medium;
        cr.ray = CameraToWorld.xform(cr.ray);
        cr.weight = 1;
        return cr;
    }

    @Override
    public CameraRayDiff GenerateRayDifferential(CameraSample sample) {
        Stats.ProfilePhase prof = new Stats.ProfilePhase(Stats.Prof.GenerateCameraRay);
        // Compute main orthographic viewing ray

        // Compute raster and camera sample positions
        Point3f pFilm = new Point3f(sample.pFilm.x, sample.pFilm.y, 0);
        Point3f pCamera = RasterToCamera.xform(pFilm);
        CameraRayDiff cr = new CameraRayDiff();
        cr.rd = new RayDifferential(pCamera, new Vector3f(0, 0, 1));

        // Modify ray for depth of field
        if (lensRadius > 0) {
            // Sample point on lens
            Point2f pLens = Sampling.ConcentricSampleDisk(sample.pLens).scale(lensRadius);

            // Compute point on plane of focus
            float ft = focalDistance / cr.rd.d.z;
            Point3f pFocus = cr.rd.at(ft);

            // Update ray for effect of lens
            cr.rd.o = new Point3f(pLens.x, pLens.y, 0);
            cr.rd.d = Vector3f.Normalize(pFocus.subtract(cr.rd.o));
        }

        // Compute ray differentials for _OrthographicCamera_
        if (lensRadius > 0) {
            // Compute _OrthographicCamera_ ray differentials accounting for lens

            // Sample point on lens
            Point2f pLens = Sampling.ConcentricSampleDisk(sample.pLens).scale(lensRadius);
            float ft = focalDistance / cr.rd.d.z;

            Point3f pFocus = pCamera.add(dxCamera.add(new Vector3f(0, 0, ft)));
            cr.rd.rxOrigin = new Point3f(pLens.x, pLens.y, 0);
            cr.rd.rxDirection = Vector3f.Normalize(pFocus.subtract(cr.rd.rxOrigin));

            pFocus = pCamera.add(dyCamera.add(new Vector3f(0, 0, ft)));
            cr.rd.ryOrigin = new Point3f(pLens.x, pLens.y, 0);
            cr.rd.ryDirection = Vector3f.Normalize(pFocus.subtract(cr.rd.ryOrigin));
        }
        else {
            cr.rd.rxOrigin = cr.rd.o.add(dxCamera);
            cr.rd.ryOrigin = cr.rd.o.add(dyCamera);
            cr.rd.rxDirection = cr.rd.ryDirection = cr.rd.d;
        }
        cr.rd.time = Pbrt.Lerp(sample.time, shutterOpen, shutterClose);
        cr.rd.hasDifferentials = true;
        cr.rd.medium = medium;
        cr.rd = CameraToWorld.xform(cr.rd);
        cr.weight = 1;
        return cr;
    }

    public static Camera Create(ParamSet paramSet, AnimatedTransform cam2world, Film film, Medium medium) {
        // Extract common camera parameters from _ParamSet_
        float shutteropen = paramSet.FindOneFloat("shutteropen", 0);
        float shutterclose = paramSet.FindOneFloat("shutterclose", 1);
        if (shutterclose < shutteropen) {
            Error.Warning("Shutter close time [%f] < shutter open [%f].  Swapping them.",
                    shutterclose, shutteropen);
            float temp = shutterclose;
            shutterclose = shutteropen;
            shutteropen = temp;
        }
        float lensradius = paramSet.FindOneFloat("lensradius", 0);
        float focaldistance = paramSet.FindOneFloat("focaldistance", 1e6f);
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
        return new OrthographicCamera(cam2world, screen, shutteropen, shutterclose,
                lensradius, focaldistance, film, medium);
    }
}