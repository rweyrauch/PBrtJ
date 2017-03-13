
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

import java.util.ArrayList;
import java.util.Objects;

public class RealisticCamera extends Camera {

    public RealisticCamera(AnimatedTransform CameraToWorld, float shutterOpen, float shutterClose, float apertureDiameter,
                           float focusDistance, boolean simpleWeighting,
                           float[] lensData, Film film, Medium medium) {
        super(CameraToWorld, shutterOpen, shutterClose, film, medium);
        this.simpleWeighting = simpleWeighting;
        for (int i = 0; i < lensData.length; i += 4) {
            if (lensData[i] == 0) {
                if (apertureDiameter > lensData[i + 3]) {
                    Error.Warning("Specified aperture diameter %f is greater than maximum " +
                            "possible %f.  Clamping it.", apertureDiameter, lensData[i + 3]);
                }
                else {
                    lensData[i + 3] = apertureDiameter;
                }
            }
            elementInterfaces.add(new LensElementInterface(lensData[i] * 0.001f, lensData[i + 1] * 0.001f,
                    lensData[i + 2], lensData[i + 3] * 0.001f / 2));
        }

        // Compute lens--film distance for given focus distance
        float fb = FocusBinarySearch(focusDistance);
        Api.logger.info("Binary search focus: %f -> %f\n", fb, FocusDistance(fb));
        elementInterfaces.get(elementInterfaces.size()-1).thickness = FocusThickLens(focusDistance);
        Api.logger.info("Thick lens focus: %f -> %f\n", elementInterfaces.get(elementInterfaces.size()-1).thickness,
                FocusDistance(elementInterfaces.get(elementInterfaces.size()-1).thickness));

        // Compute exit pupil bounds at sampled points on the film
        int nSamples = 64;
        exitPupilBounds = new Bounds2f[nSamples];
        for (int i = 0; i < nSamples; i++) {
            float r0 = (float)i / nSamples * film.diagonal / 2;
            float r1 = (float)(i + 1) / nSamples * film.diagonal / 2;
            exitPupilBounds[i] = BoundExitPupil(r0, r1);
        }
    }

    @Override
    public CameraRay GenerateRay(CameraSample sample) {
        //Stats.ProfilePhase prof = new Stats.ProfilePhase(Stats.Prof.GenerateCameraRay);
        //++totalRays;
        // Find point on film, _pFilm_, corresponding to _sample.pFilm_
        Point2f s = new Point2f(sample.pFilm.x / film.fullResolution.x, sample.pFilm.y / film.fullResolution.y);
        Point2f pFilm2 = film.GetPhysicalExtent().Lerp(s);
        Point3f pFilm = new Point3f(-pFilm2.x, pFilm2.y, 0);

        // Trace ray from _pFilm_ through lens system
        PupilSample pupilSample = SampleExitPupil(new Point2f(pFilm.x, pFilm.y), sample.pLens);
        Point3f pRear = pupilSample.p;
        float exitPupilBoundsArea = pupilSample.area;
        Ray rFilm = new Ray(pFilm, pRear.subtract(pFilm), Pbrt.Infinity, Pbrt.Lerp(sample.time, shutterOpen, shutterClose), null);
        Ray ray = TraceLensesFromFilm(rFilm);
        if (ray == null) {
            //++vignettedRays;
            return null;
        }

        // Finish initialization of _RealisticCamera_ ray
        CameraRay cray = new CameraRay();
        cray.ray = CameraToWorld.xform(ray);
        cray.ray.d = Vector3f.Normalize(cray.ray.d);
        cray.ray.medium = medium;

        // Return weighting for _RealisticCamera_ ray
        float cosTheta = Vector3f.Normalize(rFilm.d).z;
        float cos4Theta = (cosTheta * cosTheta) * (cosTheta * cosTheta);
        if (simpleWeighting)
            cray.weight = cos4Theta;
        else
            cray.weight = (shutterClose - shutterOpen) * (cos4Theta * exitPupilBoundsArea) / (LensRearZ() * LensRearZ());
        return cray;
    }

    public static Camera Create(ParamSet paramSet, AnimatedTransform cam2world, Film film, Medium medium) {
        float shutteropen = paramSet.FindOneFloat("shutteropen", 0);
        float shutterclose = paramSet.FindOneFloat("shutterclose", 1);
        if (shutterclose < shutteropen) {
            Error.Warning("Shutter close time [%f] < shutter open [%f].  Swapping them.", shutterclose, shutteropen);
            float temp = shutterclose;
            shutterclose = shutteropen;
            shutteropen = temp;
        }

        // Realistic camera-specific parameters
        String lensFile = paramSet.FindOneFilename("lensfile", "");
        float apertureDiameter = paramSet.FindOneFloat("aperturediameter", 1);
        float focusDistance = paramSet.FindOneFloat("focusdistance", 10);
        boolean simpleWeighting = paramSet.FindOneBoolean("simpleweighting", true);
        if (Objects.equals(lensFile, "")) {
            Error.Error("No lens description file supplied!");
            return null;
        }
        // Load element data from lens description file
        float[] lensData = FloatFile.Read(lensFile);
        if (lensData == null) {
            Error.Error("Error reading lens specification file \"%s\".", lensFile);
            return null;
        }
        if (lensData.length % 4 != 0) {
            Error.Error("Excess values in lens specification file \"%s\"; must be multiple-of-four values, read %d.", lensFile, lensData.length);
            return null;
        }

        return new RealisticCamera(cam2world, shutteropen, shutterclose, apertureDiameter, focusDistance, simpleWeighting, lensData, film, medium);
    }

    // RealisticCamera Private Declarations
    private class LensElementInterface {
        public LensElementInterface(float curvatureRadius, float thickness, float eta, float apertureRadius) {
            this.curvatureRadius = curvatureRadius;
            this.thickness = thickness;
            this.eta = eta;
            this.apertureRadius = apertureRadius;
        }
        public float curvatureRadius;
        public float thickness;
        public float eta;
        public float apertureRadius;
    }

    // RealisticCamera Private Data
    private boolean simpleWeighting;
    private ArrayList<LensElementInterface> elementInterfaces;
    private Bounds2f[] exitPupilBounds;

    private static final Transform CameraToLens = Transform.Scale(1, 1, -1);

    // RealisticCamera Private Methods
    private float LensRearZ() { return elementInterfaces.get(elementInterfaces.size()-1).thickness; }
    private float LensFrontZ() {
        float zSum = 0;
        for (LensElementInterface element : elementInterfaces)
            zSum += element.thickness;
        return zSum;
    }
    private float RearElementRadius() {
        return elementInterfaces.get(elementInterfaces.size()-1).apertureRadius;
    }

    private Ray TraceLensesFromFilm(Ray rCamera) {
        float elementZ = 0;
        // Transform _rCamera_ from camera to lens system space
        Ray rLens = CameraToLens.xform(rCamera);
        for (int i = elementInterfaces.size() - 1; i >= 0; --i) {
            LensElementInterface element = elementInterfaces.get(i);
            // Update ray from film accounting for interaction with _element_
            elementZ -= element.thickness;

            // Compute intersection of ray with lens element
            float t;
            Normal3f n = new Normal3f();
            boolean isStop = (element.curvatureRadius == 0);
            if (isStop)
                t = (elementZ - rLens.o.z) / rLens.d.z;
            else {
                float radius = element.curvatureRadius;
                float zCenter = elementZ + element.curvatureRadius;
                InterResult ires = IntersectSphericalElement(radius, zCenter, rLens);
                if (ires == null)
                    return null;
                t = ires.t;
                n = ires.n;
            }
            assert (t >= 0);

            // Test intersection point against element aperture
            Point3f pHit = rLens.at(t);
            Float r2 = pHit.x * pHit.x + pHit.y * pHit.y;
            if (r2 > element.apertureRadius * element.apertureRadius) return null;
            rLens.o = pHit;

            // Update ray path for element interface interaction
            if (!isStop) {
                float etaI = element.eta;
                float etaT = (i > 0 && elementInterfaces.get(i - 1).eta != 0) ? elementInterfaces.get(i - 1).eta : 1;
                Vector3f w = Reflection.Refract(Vector3f.Normalize(rLens.d.negate()), n, etaI / etaT);
                if (w == null) return null;
                rLens.d = w;
            }
        }
        // Transform _rLens_ from lens system space back to camera space
        Transform LensToCamera = Transform.Scale(1, 1, -1);

        return LensToCamera.xform(rLens);
    }

    private static class InterResult {
        public float t;
        public Normal3f n;
    }
    private static InterResult IntersectSphericalElement(float radius, float zCenter, Ray ray) {
        // Compute _t0_ and _t1_ for ray--element intersection
        Point3f o = ray.o.subtract(new Vector3f(0, 0, zCenter));
        Float A = ray.d.x * ray.d.x + ray.d.y * ray.d.y + ray.d.z * ray.d.z;
        Float B = 2 * (ray.d.x * o.x + ray.d.y * o.y + ray.d.z * o.z);
        Float C = o.x * o.x + o.y * o.y + o.z * o.z - radius * radius;
        Pbrt.QuadRes res = Pbrt.Quadratic(A, B, C);
        if (res == null) return null;

        // Select intersection $t$ based on ray direction and element curvature
        boolean useCloserT = (ray.d.z > 0) ^ (radius < 0);
        float t = useCloserT ? Math.min(res.t0, res.t1) : Math.max(res.t0, res.t1);
        if (t < 0) return null;

        // Compute surface normal of element at ray intersection point
        InterResult ires = new InterResult();
        ires.t = t;
        ires.n = new Normal3f(new Vector3f(o.add(ray.d.scale(t))));
        ires.n = Normal3f.Faceforward(Normal3f.Normalize(ires.n), ray.d.negate());
        return ires;
    }

    private Ray TraceLensesFromScene(Ray rCamera) {
        float elementZ = -LensFrontZ();
        // Transform _rCamera_ from camera to lens system space
        Ray rLens = CameraToLens.xform(rCamera);
        for (int i = 0; i < elementInterfaces.size(); ++i) {
            LensElementInterface element = elementInterfaces.get(i);
            // Compute intersection of ray with lens element
            float t;
            Normal3f n = new Normal3f();
            boolean isStop = (element.curvatureRadius == 0);
            if (isStop)
                t = (elementZ - rLens.o.z) / rLens.d.z;
            else {
                float radius = element.curvatureRadius;
                float zCenter = elementZ + element.curvatureRadius;
                InterResult ires = IntersectSphericalElement(radius, zCenter, rLens);
                if (ires == null)
                    return null;
                t = ires.t;
                n = ires.n;
            }
            assert (t >= 0);

            // Test intersection point against element aperture
            Point3f pHit = rLens.at(t);
            float r2 = pHit.x * pHit.x + pHit.y * pHit.y;
            if (r2 > element.apertureRadius * element.apertureRadius) return null;
            rLens.o = pHit;

            // Update ray path for from-scene element interface interaction
            if (!isStop) {
                float etaI = (i == 0 || elementInterfaces.get(i - 1).eta == 0) ? 1 : elementInterfaces.get(i - 1).eta;
                float etaT = (elementInterfaces.get(i).eta != 0) ? elementInterfaces.get(i).eta : 1;
                Vector3f wt = Reflection.Refract(Vector3f.Normalize(rLens.d.negate()), n, etaI / etaT);
                if (wt == null) return null;
                rLens.d = wt;
            }
            elementZ += element.thickness;
        }
        // Transform _rLens_ from lens system space back to camera space
        Transform LensToCamera = Transform.Scale(1, 1, -1);
        return LensToCamera.xform(rLens);
    }

    private void DrawLensSystem() {
        float sumz = -LensFrontZ();
        float z = sumz;
        for (int i = 0; i < elementInterfaces.size(); ++i) {
            LensElementInterface element = elementInterfaces.get(i);
            float r = element.curvatureRadius;
            if (r == 0) {
                // stop
                System.out.printf("{Thick, Line[{{%f, %f}, {%f, %f}}], ", z, element.apertureRadius, z, 2 * element.apertureRadius);
                System.out.printf("Line[{{%f, %f}, {%f, %f}}]}, ", z, -element.apertureRadius, z, -2 * element.apertureRadius);
            } else {
                float theta = Math.abs((float)Math.asin(element.apertureRadius / r));
                if (r > 0) {
                    // convex as seen from front of lens
                    float t0 = Pbrt.Pi - theta;
                    float t1 = Pbrt.Pi + theta;
                    System.out.printf("Circle[{%f, 0}, %f, {%f, %f}], ", z + r, r, t0, t1);
                } else {
                    // concave as seen from front of lens
                    float t0 = -theta;
                    float t1 = theta;
                    System.out.printf("Circle[{%f, 0}, %f, {%f, %f}], ", z + r, -r, t0, t1);
                }
                if (element.eta != 0 && element.eta != 1) {
                    // connect top/bottom to next element
                    assert (i + 1 < elementInterfaces.size());
                    float nextApertureRadius = elementInterfaces.get(i + 1).apertureRadius;
                    float h = Math.max(element.apertureRadius, nextApertureRadius);
                    float hlow = Math.min(element.apertureRadius, nextApertureRadius);

                    float zp0, zp1;
                    if (r > 0) {
                        zp0 = z + element.curvatureRadius - element.apertureRadius / (float)Math.tan(theta);
                    } else {
                        zp0 = z + element.curvatureRadius + element.apertureRadius / (float)Math.tan(theta);
                    }

                    float nextCurvatureRadius = elementInterfaces.get(i + 1).curvatureRadius;
                    float nextTheta = Math.abs((float)Math.asin(nextApertureRadius / nextCurvatureRadius));
                    if (nextCurvatureRadius > 0) {
                        zp1 = z + element.thickness + nextCurvatureRadius - nextApertureRadius / (float)Math.tan(nextTheta);
                    } else {
                        zp1 = z + element.thickness + nextCurvatureRadius + nextApertureRadius / (float)Math.tan(nextTheta);
                    }

                    // Connect tops
                    System.out.printf("Line[{{%f, %f}, {%f, %f}}], ", zp0, h, zp1, h);
                    System.out.printf("Line[{{%f, %f}, {%f, %f}}], ", zp0, -h, zp1, -h);

                    // vertical lines when needed to close up the element profile
                    if (element.apertureRadius < nextApertureRadius) {
                        System.out.printf("Line[{{%f, %f}, {%f, %f}}], ", zp0, h, zp0, hlow);
                        System.out.printf("Line[{{%f, %f}, {%f, %f}}], ", zp0, -h, zp0, -hlow);
                    }
                    else if (element.apertureRadius > nextApertureRadius) {
                        System.out.printf("Line[{{%f, %f}, {%f, %f}}], ", zp1, h, zp1, hlow);
                        System.out.printf("Line[{{%f, %f}, {%f, %f}}], ", zp1, -h, zp1, -hlow);
                    }
                }
            }
            z += element.thickness;
        }

        // 24mm height for 35mm film
        System.out.printf("Line[{{0, -.012}, {0, .012}}], ");
        // optical axis
        System.out.printf("Line[{{0, 0}, {%f, 0}}] ", 1.2f * sumz);
    }

    private void DrawRayPathFromFilm(Ray r, boolean arrow, boolean toOpticalIntercept) {
        float elementZ = 0;
        // Transform _ray_ from camera to lens system space
        Ray ray = CameraToLens.xform(r);
        System.out.printf("{ ");
        if (TraceLensesFromFilm(r) == null) System.out.printf("Dashed, ");
        for (int i = elementInterfaces.size() - 1; i >= 0; --i) {
            LensElementInterface element = elementInterfaces.get(i);
            elementZ -= element.thickness;
            boolean isStop = (element.curvatureRadius == 0);
            // Compute intersection of ray with lens element
            float t;
            Normal3f n = new Normal3f();
            if (isStop)
                t = -(ray.o.z - elementZ) / ray.d.z;
            else {
                float radius = element.curvatureRadius;
                float zCenter = elementZ + element.curvatureRadius;
                InterResult ires = IntersectSphericalElement(radius, zCenter, ray);
                if (ires == null) {
                    System.out.printf("}");
                    return;
                }
                t = ires.t;
                n = ires.n;
            }
            assert (t >= 0);

            System.out.printf("Line[{{%f, %f}, {%f, %f}}],", ray.o.z, ray.o.x, ray.at(t).z, ray.at(t).x);

            // Test intersection point against element aperture
            Point3f pHit = ray.at(t);
            float r2 = pHit.x * pHit.x + pHit.y * pHit.y;
            float apertureRadius2 = element.apertureRadius * element.apertureRadius;
            if (r2 > apertureRadius2) {
                System.out.printf("}");
                return;
            }
            ray.o = pHit;

            // Update ray path for element interface interaction
            if (!isStop) {
                float etaI = element.eta;
                float etaT = (i > 0 && elementInterfaces.get(i - 1).eta != 0) ? elementInterfaces.get(i - 1).eta : 1;
                Vector3f wt = Reflection.Refract(Vector3f.Normalize(ray.d.negate()), n, etaI / etaT);
                if (wt == null) {
                    System.out.printf("}");
                    return;
                }
                ray.d = wt;
            }
        }

        ray.d = Vector3f.Normalize(ray.d);
        {
            float ta = Math.abs(elementZ / 4);
            if (toOpticalIntercept) {
                ta = -ray.o.x / ray.d.x;
                System.out.printf("Point[{%f, %f}], ", ray.at(ta).z, ray.at(ta).x);
            }
            System.out.printf("%s[{{%f, %f}, {%f, %f}}]", arrow ? "Arrow" : "Line", ray.o.z, ray.o.x, ray.at(ta).z, ray.at(ta).x);

            // overdraw the optical axis if needed...
            if (toOpticalIntercept)
                System.out.printf(", Line[{{%f, 0}, {%f, 0}}]", ray.o.z, ray.at(ta).z * 1.05f);
        }

        System.out.printf("}");
    }

    private void DrawRayPathFromScene(Ray r, boolean arrow, boolean toOpticalIntercept) {
        float elementZ = LensFrontZ() * -1;

        // Transform _ray_ from camera to lens system space
        Ray ray = CameraToLens.xform(r);
        for (int i = 0; i < elementInterfaces.size(); ++i) {
            LensElementInterface element = elementInterfaces.get(i);
            boolean isStop = (element.curvatureRadius == 0);
            // Compute intersection of ray with lens element
            float t;
            Normal3f n = new Normal3f();
            if (isStop)
                t = -(ray.o.z - elementZ) / ray.d.z;
            else {
                float radius = element.curvatureRadius;
                float zCenter = elementZ + element.curvatureRadius;
                InterResult ires = IntersectSphericalElement(radius, zCenter, ray);
                if (ires == null)
                    return;
                t = ires.t;
                n = ires.n;
            }
            assert (t >= 0.f);

            System.out.printf("Line[{{%f, %f}, {%f, %f}}],", ray.o.z, ray.o.x, ray.at(t).z, ray.at(t).x);

            // Test intersection point against element aperture
            Point3f pHit = ray.at(t);
            float r2 = pHit.x * pHit.x + pHit.y * pHit.y;
            float apertureRadius2 = element.apertureRadius * element.apertureRadius;
            if (r2 > apertureRadius2) return;
            ray.o = pHit;

            // Update ray path for from-scene element interface interaction
            if (!isStop) {
                float etaI = (i == 0 || elementInterfaces.get(i - 1).eta == 0.f) ? 1.f : elementInterfaces.get(i - 1).eta;
                float etaT = (elementInterfaces.get(i).eta != 0.f) ? elementInterfaces.get(i).eta : 1.f;
                Vector3f wt = Reflection.Refract(Vector3f.Normalize(ray.d.negate()), n, etaI / etaT);
                if (wt == null) return;
                ray.d = wt;
            }
            elementZ += element.thickness;
        }

        // go to the film plane by default
        {
            float ta = -ray.o.z / ray.d.z;
            if (toOpticalIntercept) {
                ta = -ray.o.x / ray.d.x;
                System.out.printf("Point[{%f, %f}], ", ray.at(ta).z, ray.at(ta).x);
            }
            System.out.printf("%s[{{%f, %f}, {%f, %f}}]", arrow ? "Arrow" : "Line", ray.o.z, ray.o.x, ray.at(ta).z, ray.at(ta).x);
        }
    }

    private static class CardPnts {
        public CardPnts(float p, float f) {
            this.p = p;
            this.f = f;
        }
        public float p, f;
    }
    private static CardPnts ComputeCardinalPoints(Ray rIn, Ray rOut) {
        float tf = -rOut.o.x / rOut.d.x;
        float fz = -rOut.at(tf).z;
        float tp = (rIn.o.x - rOut.o.x) / rOut.d.x;
        float pz = -rOut.at(tp).z;
        return new CardPnts(pz, fz);
    }

    private CardPnts[] ComputeThickLensApproximation() {
        // Find height $x$ from optical axis for parallel rays
        float x = 0.001f * film.diagonal;

        // Compute cardinal points for film side of lens system
        Ray rScene = new Ray(new Point3f(x, 0, LensFrontZ() + 1), new Vector3f(0, 0, -1));
        Ray rFilm = TraceLensesFromScene(rScene);
        if (rFilm == null) {
            Error.Error("Unable to trace ray from scene to film for thick lens " +
                    "approximation. Is aperture stop extremely small?");
        }
        CardPnts[] result = new CardPnts[2];
        result[0] = ComputeCardinalPoints(rScene, rFilm);

        // Compute cardinal points for scene side of lens system
        rFilm = new Ray(new Point3f(x, 0, LensRearZ() - 1), new Vector3f(0, 0, 1));
        rScene = TraceLensesFromFilm(rFilm);
        if (rScene == null) {
            Error.Error("Unable to trace ray from film to scene for thick lens " +
                "approximation. Is aperture stop extremely small?");
        }
        result[1] = ComputeCardinalPoints(rFilm, rScene);
        return result;
    }

    private float FocusThickLens(float focusDistance) {
        CardPnts[] cpnts = ComputeThickLensApproximation();
        Api.logger.info("Cardinal points: p' = %f f' = %f, p = %f f = %f.\n", cpnts[0].p, cpnts[0].f, cpnts[1].p, cpnts[1].f);
        Api.logger.info("Effective focal length %f\n", cpnts[0].f - cpnts[0].p);
        // Compute translation of lens, _delta_, to focus at _focusDistance_
        float f = cpnts[0].f - cpnts[0].p;
        float z = -focusDistance;
        float delta = 0.5f * (cpnts[1].p - z + cpnts[0].p - (float)Math.sqrt((cpnts[1].p - z - cpnts[0].p) * (cpnts[1].p - z - 4 * f - cpnts[0].p)));
        return elementInterfaces.get(elementInterfaces.size()-1).thickness + delta;
    }

    private float FocusBinarySearch(float focusDistance) {
        float filmDistanceLower, filmDistanceUpper;
        // Find _filmDistanceLower_, _filmDistanceUpper_ that bound focus distance
        filmDistanceLower = filmDistanceUpper = FocusThickLens(focusDistance);
        while (FocusDistance(filmDistanceLower) > focusDistance)
            filmDistanceLower *= 1.005f;
        while (FocusDistance(filmDistanceUpper) < focusDistance)
            filmDistanceUpper /= 1.005f;

        // Do binary search on film distances to focus
        for (int i = 0; i < 20; ++i) {
            float fmid = 0.5f * (filmDistanceLower + filmDistanceUpper);
            float midFocus = FocusDistance(fmid);
            if (midFocus < focusDistance)
                filmDistanceLower = fmid;
            else
                filmDistanceUpper = fmid;
        }
        return 0.5f * (filmDistanceLower + filmDistanceUpper);
    }

    private float FocusDistance(float filmDist) {
        // Find offset ray from film center through lens
        Bounds2f bounds = BoundExitPupil(0, 0.001f * film.diagonal);
        float lu = 0.1f * bounds.pMax.x;
        Ray ray = TraceLensesFromFilm(new Ray(new Point3f(0, 0, LensRearZ() - filmDist), new Vector3f(lu, 0, filmDist)));
        if (ray == null) {
            Error.Error("Focus ray at lens pos(%f,0) didn't make it through the lenses with film distance %f?!??\n", lu, filmDist);
            return Pbrt.Infinity;
        }

        // Compute distance _zFocus_ where ray intersects the principal axis
        float tFocus = -ray.o.x / ray.d.x;
        float zFocus = ray.at(tFocus).z;
        if (zFocus < 0) zFocus = Pbrt.Infinity;
        return zFocus;
    }

    private Bounds2f BoundExitPupil(float pFilmX0, float pFilmX1) {
        Bounds2f pupilBounds = new Bounds2f();
        // Sample a collection of points on the rear lens to find exit pupil
        int nSamples = 1024 * 1024;
        int nExitingRays = 0;

        // Compute bounding box of projection of rear element on sampling plane
        float rearRadius = RearElementRadius();
        Bounds2f projRearBounds = new Bounds2f(new Point2f(-1.5f * rearRadius, -1.5f * rearRadius),
                new Point2f(1.5f * rearRadius, 1.5f * rearRadius));
        for (int i = 0; i < nSamples; ++i) {
            // Find location of sample points on $x$ segment and rear lens element
            Point3f pFilm = new Point3f(Pbrt.Lerp((i + 0.5f) / nSamples, pFilmX0, pFilmX1), 0, 0);
            float[] u = {LowDiscrepancy.RadicalInverse(0, i), LowDiscrepancy.RadicalInverse(1, i)};
            Point3f pRear = new Point3f(Pbrt.Lerp(u[0], projRearBounds.pMin.x, projRearBounds.pMax.x),
                    Pbrt.Lerp(u[1], projRearBounds.pMin.y, projRearBounds.pMax.y), LensRearZ());

            // Expand pupil bounds if ray makes it through the lens system
            Ray ray = TraceLensesFromFilm(new Ray(pFilm, pRear.subtract(pFilm)));
            if (Bounds2f.Inside(new Point2f(pRear.x, pRear.y), pupilBounds) || (ray != null)) {
                pupilBounds = Bounds2f.Union(pupilBounds, new Point2f(pRear.x, pRear.y));
                ++nExitingRays;
            }
        }

        // Return entire element bounds if no rays made it through the lens system
        if (nExitingRays == 0) {
            Api.logger.info("Unable to find exit pupil in x = [%f,%f] on film.", pFilmX0, pFilmX1);
            return projRearBounds;
        }

        // Expand bounds to account for sample spacing
        pupilBounds = Bounds2f.Expand(pupilBounds, 2 * projRearBounds.Diagonal().Length() / (float)Math.sqrt(nSamples));
        return pupilBounds;
    }
    private void RenderExitPupil(float sx, float sy, String filename) {
        Point3f pFilm = new Point3f(sx, sy, 0);

        int nSamples = 2048;
        float[] image = new float[3 * nSamples * nSamples];

        int i = 0;
        for (int y = 0; y < nSamples; ++y) {
            float fy = (float)y / (float)(nSamples - 1);
            float ly = Pbrt.Lerp(fy, -RearElementRadius(), RearElementRadius());
            for (int x = 0; x < nSamples; ++x) {
                float fx = (float)x / (float)(nSamples - 1);
                float lx = Pbrt.Lerp(fx, -RearElementRadius(), RearElementRadius());

                Point3f pRear = new Point3f(lx, ly, LensRearZ());

                if (lx * lx + ly * ly > RearElementRadius() * RearElementRadius()) {
                    image[i++] = 1;
                    image[i++] = 1;
                    image[i++] = 1;
                }
                else if (TraceLensesFromFilm(new Ray(pFilm, pRear.subtract(pFilm))) != null) {
                    image[i++] = 0.5f;
                    image[i++] = 0.5f;
                    image[i++] = 0.5f;
                }
                else {
                    image[i++] = 0.f;
                    image[i++] = 0.f;
                    image[i++] = 0.f;
                }
            }
        }

        ImageIO.Write(filename, image, new Bounds2i(new Point2i(0, 0), new Point2i(nSamples, nSamples)), new Point2i(nSamples, nSamples));
    }
    private class PupilSample {
        Point3f p;
        float area;
    }
    private PupilSample SampleExitPupil(Point2f pFilm, Point2f lensSample) {
        // Find exit pupil bound for sample distance from film center
        float rFilm = (float)Math.sqrt(pFilm.x * pFilm.x + pFilm.y * pFilm.y);
        int rIndex = (int)(rFilm / (film.diagonal / 2) * exitPupilBounds.length);
        rIndex = Math.min(exitPupilBounds.length - 1, rIndex);
        Bounds2f pupilBounds = exitPupilBounds[rIndex];

        PupilSample psamp = new PupilSample();
        psamp.area = pupilBounds.Area();

        // Generate sample point inside exit pupil bound
        Point2f pLens = pupilBounds.Lerp(lensSample);

        // Return sample point rotated by angle of _pFilm_ with $+x$ axis
        float sinTheta = (rFilm != 0) ? pFilm.y / rFilm : 0;
        float cosTheta = (rFilm != 0) ? pFilm.x / rFilm : 1;
        psamp.p =  new Point3f(cosTheta * pLens.x - sinTheta * pLens.y,
                sinTheta * pLens.x + cosTheta * pLens.y, LensRearZ());
        return psamp;
    }

    private void TestExitPupilBounds() {
        Float filmDiagonal = film.diagonal;

        RNG rng = new RNG();

        float u = rng.UniformFloat();
        Point3f pFilm = new Point3f(u * filmDiagonal / 2, 0, 0);

        float r = pFilm.x / (filmDiagonal / 2);
        int pupilIndex = Math.min(exitPupilBounds.length - 1, (int)Math.floor(r * (exitPupilBounds.length - 1)));
        Bounds2f pupilBounds = exitPupilBounds[pupilIndex];
        if (pupilIndex + 1 < exitPupilBounds.length)
            pupilBounds = Bounds2f.Union(pupilBounds, exitPupilBounds[pupilIndex + 1]);

        // Now, randomly pick points on the aperture and see if any are outside
        // of pupil bounds...
        for (int i = 0; i < 1000; ++i) {
            Point2f u2 = new Point2f(rng.UniformFloat(), rng.UniformFloat());
            Point2f pd = Sampling.ConcentricSampleDisk(u2);
            pd.scale(RearElementRadius());

            Ray testRay = new Ray(pFilm, new Point3f(pd.x, pd.y, 0.f).subtract(pFilm));
            Ray testOut = TraceLensesFromFilm(testRay);
            if (testOut == null) continue;

            if (!Bounds2f.Inside(pd, pupilBounds)) {
                System.err.printf("Aha! (%f,%f) went through, but outside bounds (%f,%f) - (%f,%f)\n",
                        pd.x, pd.y, pupilBounds.pMin.x, pupilBounds.pMin.y, pupilBounds.pMax.x, pupilBounds.pMax.y);
                RenderExitPupil((float)pupilIndex / exitPupilBounds.length * filmDiagonal / 2.f, 0.f, "low.exr");
                RenderExitPupil((float)(pupilIndex + 1) / exitPupilBounds.length * filmDiagonal / 2.f, 0.f, "high.exr");
                RenderExitPupil(pFilm.x, 0.f, "mid.exr");
                System.exit(0);
            }
        }
        System.err.printf(".");
    }

}