
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

public class PerspectiveCamera extends ProjectiveCamera {

    public PerspectiveCamera(AnimatedTransform CameraToWorld, Bounds2f screenWindow, float shutterOpen, float shutterClose, float lensr, float focald, float fov, Film film, Medium medium) {
        super(CameraToWorld, Transform.Perspective(fov, 1e-2f, 1000.0f), screenWindow, shutterOpen, shutterClose, lensr, focald, film, medium);
        // Compute differential changes in origin for perspective camera rays
        dxCamera = (RasterToCamera.xform(new Point3f(1, 0, 0)).subtract(RasterToCamera.xform(new Point3f(0, 0, 0))));
        dyCamera = (RasterToCamera.xform(new Point3f(0, 1, 0)).subtract(RasterToCamera.xform(new Point3f(0, 0, 0))));

        // Compute image plane bounds at $z=1$ for _PerspectiveCamera_
        Point2i res = film.fullResolution;
        Point3f pMin = RasterToCamera.xform(new Point3f(0, 0, 0));
        Point3f pMax = RasterToCamera.xform(new Point3f(res.x, res.y, 0));
        pMin.invScale(pMin.z);
        pMax.invScale(pMax.z);
        A = Math.abs((pMax.x - pMin.x) * (pMax.y - pMin.y));
    }

    @Override
    public CameraRay GenerateRay(CameraSample sample) {
        Stats.ProfilePhase prof = new Stats.ProfilePhase(Stats.Prof.GenerateCameraRay);
        // Compute raster and camera sample positions
        Point3f pFilm = new Point3f(sample.pFilm.x, sample.pFilm.y, 0);
        Point3f pCamera = RasterToCamera.xform(pFilm);
        CameraRay cr = new CameraRay();
        cr.ray = new Ray(new Point3f(0, 0, 0), Vector3f.Normalize(new Vector3f(pCamera)));
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
        // Compute raster and camera sample positions
        Point3f pFilm = new Point3f(sample.pFilm.x, sample.pFilm.y, 0);
        Point3f pCamera = RasterToCamera.xform(pFilm);
        Vector3f dir = Vector3f.Normalize(new Vector3f(pCamera.x, pCamera.y, pCamera.z));
        CameraRayDiff cr = new CameraRayDiff();
        cr.rd = new RayDifferential(new Point3f(0, 0, 0), dir);
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

        // Compute offset rays for _PerspectiveCamera_ ray differentials
        if (lensRadius > 0) {
            // Compute _PerspectiveCamera_ ray differentials accounting for lens

            // Sample point on lens
            Point2f pLens = Sampling.ConcentricSampleDisk(sample.pLens).scale(lensRadius);
            Vector3f dx = Vector3f.Normalize(new Vector3f(pCamera.add(dxCamera)));
            float ft = focalDistance / dx.z;
            Point3f pFocus = new Point3f(0, 0, 0).add(dx.scale(ft));
            cr.rd.rxOrigin = new Point3f(pLens.x, pLens.y, 0);
            cr.rd.rxDirection = Vector3f.Normalize(pFocus.subtract(cr.rd.rxOrigin));

            Vector3f dy = Vector3f.Normalize(new Vector3f(pCamera.add(dyCamera)));
            ft = focalDistance / dy.z;
            pFocus = new Point3f(0, 0, 0).add(dy.scale(ft));
            cr.rd.ryOrigin = new Point3f(pLens.x, pLens.y, 0);
            cr.rd.ryDirection = Vector3f.Normalize(pFocus.subtract(cr.rd.ryOrigin));
        }
        else {
            cr.rd.rxOrigin = cr.rd.ryOrigin = cr.rd.o;
            cr.rd.rxDirection = Vector3f.Normalize(new Vector3f(pCamera).add(dxCamera));
            cr.rd.ryDirection = Vector3f.Normalize(new Vector3f(pCamera).add(dyCamera));
        }
        cr.rd.time = Pbrt.Lerp(sample.time, shutterOpen, shutterClose);
        cr.rd.medium = medium;
        cr.rd = CameraToWorld.xform(cr.rd);
        cr.rd.hasDifferentials = true;
        cr.weight = 1;
        return cr;
    }

    @Override
    public CameraWe We(Ray ray) {
        CameraWe cwe = new CameraWe();
        cwe.we = new Spectrum(0);

        // Interpolate camera matrix and check if $\w{}$ is forward-facing
        Transform c2w = CameraToWorld.Interpolate(ray.time);
        float cosTheta = Vector3f.Dot(ray.d, c2w.xform(new Vector3f(0, 0, 1)));
        if (cosTheta <= 0) return cwe;

        // Map ray $(\p{}, \w{})$ onto the raster grid
        Point3f pFocus = ray.at((lensRadius > 0 ? focalDistance : 1) / cosTheta);
        Point3f pRaster = Transform.Inverse(RasterToCamera).xform(Transform.Inverse(c2w).xform(pFocus));

        // Return raster position if requested
        cwe.pRaster2 = new Point2f(pRaster.x, pRaster.y);

        // Return zero importance for out of bounds points
        Bounds2i sampleBounds = film.GetSampleBounds();
        if (pRaster.x < sampleBounds.pMin.x || pRaster.x >= sampleBounds.pMax.x ||
                pRaster.y < sampleBounds.pMin.y || pRaster.y >= sampleBounds.pMax.y)
            return cwe;

        // Compute lens area of perspective camera
        float lensArea = lensRadius != 0 ? ((float)Math.PI * lensRadius * lensRadius) : 1;

        // Return importance for point on image plane
        float cos2Theta = cosTheta * cosTheta;
        cwe.we = new Spectrum(1 / (A * lensArea * cos2Theta * cos2Theta));

        return cwe;
    }

    @Override
    public CameraPdf Pdf_We(Ray ray) {
        // Interpolate camera matrix and fail if $\w{}$ is not forward-facing
        Transform c2w = CameraToWorld.Interpolate(ray.time);
        float cosTheta = Vector3f.Dot(ray.d, c2w.xform(new Vector3f(0, 0, 1)));
        CameraPdf cd = new CameraPdf();
        if (cosTheta <= 0) {
            cd.pdfDir = 0;
            cd.pdfPos = 0;
            return cd;
        }

        // Map ray $(\p{}, \w{})$ onto the raster grid
        Point3f pFocus = ray.at((lensRadius > 0 ? focalDistance : 1) / cosTheta);
        Point3f pRaster = Transform.Inverse(RasterToCamera).xform(Transform.Inverse(c2w).xform(pFocus));

        // Return zero probability for out of bounds points
        Bounds2i sampleBounds = film.GetSampleBounds();
        if (pRaster.x < sampleBounds.pMin.x || pRaster.x >= sampleBounds.pMax.x ||
                pRaster.y < sampleBounds.pMin.y || pRaster.y >= sampleBounds.pMax.y) {
            cd.pdfPos = cd. pdfDir = 0;
            return cd;
        }

        // Compute lens area of perspective camera
        float lensArea = lensRadius != 0 ? ((float)Math.PI * lensRadius * lensRadius) : 1;
        cd.pdfPos = 1 / lensArea;
        cd.pdfDir = 1 / (A * cosTheta * cosTheta * cosTheta);

        return cd;
    }

    @Override
    public CameraWi Sample_Wi(Interaction ref, Point2f u) {
        // Uniformly sample a lens interaction _lensIntr_
        Point2f pLens = Sampling.ConcentricSampleDisk(u).scale(lensRadius);
        Point3f pLensWorld = CameraToWorld.xform(ref.time, new Point3f(pLens.x, pLens.y, 0));
        Interaction lensIntr = new Interaction(pLensWorld, ref.time, new MediumInterface(medium));
        lensIntr.n = new Normal3f(CameraToWorld.xform(ref.time, new Vector3f(0, 0, 1)));

        // Populate arguments and compute the importance value
        CameraWi cw = new CameraWi();
        cw.vis = new Light.VisibilityTester(ref, lensIntr);
        cw.wi = lensIntr.p.subtract(ref.p);
        float dist = cw.wi.Length();
        cw.wi.invScale(dist);

        // Compute PDF for importance arriving at _ref_

        // Compute lens area of perspective camera
        float lensArea = lensRadius != 0 ? ((float)Math.PI * lensRadius * lensRadius) : 1;
        cw.pdf = (dist * dist) / (Normal3f.AbsDot(lensIntr.n, cw.wi) * lensArea);
        CameraWe cwe = We(lensIntr.SpawnRay(cw.wi.negate()));
        cw.pRaster = cwe.pRaster2;
        cw.swe = cwe.we;
        return cw;
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
        float focaldistance = paramSet.FindOneFloat("focaldistance", 1e6f);
        float frame = paramSet.FindOneFloat("frameaspectratio", film.fullResolution.x / film.fullResolution.y);
        Bounds2f screen = new Bounds2f();
        if (frame > 1.f) {
            screen.pMin.x = -frame;
            screen.pMax.x = frame;
            screen.pMin.y = -1;
            screen.pMax.y = 1;
        } else {
            screen.pMin.x = -1;
            screen.pMax.x = 1;
            screen.pMin.y = -1 / frame;
            screen.pMax.y = 1 / frame;
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
        Float fov = paramSet.FindOneFloat("fov", 90);
        float halffov = paramSet.FindOneFloat("halffov", -1);
        if (halffov > 0) {
            // hack for structure synth, which exports half of the full fov
            fov = 2 * halffov;
        }
        return new PerspectiveCamera(cam2world, screen, shutteropen, shutterclose,
                lensradius, focaldistance, fov, film, medium);
    }

    // PerspectiveCamera Private Data
    private Vector3f dxCamera, dyCamera;
    private float A;
}