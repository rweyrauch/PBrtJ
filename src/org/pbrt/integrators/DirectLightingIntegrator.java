
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.integrators;

import org.pbrt.core.*;
import org.pbrt.core.Error;

import java.util.ArrayList;
import java.util.Objects;

public class DirectLightingIntegrator extends SamplerIntegrator {

    enum LightStrategy { UniformSampleAll, UniformSampleOne }

    public DirectLightingIntegrator(LightStrategy strategy, int maxDepth, Camera camera, Sampler sampler, Bounds2i pixelBounds) {
        super(camera, sampler, pixelBounds);
        this.strategy = strategy;
        this.maxDepth = maxDepth;
    }

    @Override
    public Spectrum Li(RayDifferential ray, Scene scene, Sampler sampler, int depth) {
        Stats.ProfilePhase p = new Stats.ProfilePhase(Stats.Prof.SamplerIntegratorLi);
        Spectrum L = new Spectrum(0);
        // Find closest ray intersection or return background radiance
        SurfaceInteraction isect = scene.Intersect(ray);
        if (isect == null) {
            for (Light light : scene.lights) L = L.add(light.Le(ray));
            return L;
        }

        // Compute scattering functions for surface interaction
        isect.ComputeScatteringFunctions(ray);
        if (isect.bsdf == null)
            return Li(new RayDifferential(isect.SpawnRay(ray.d)), scene, sampler, depth);
        Vector3f wo = isect.wo;
        // Compute emitted light if ray hit an area light source
        L = L.add(isect.Le(wo));
        if (scene.lights.size() > 0) {
            // Compute direct lighting for _DirectLightingIntegrator_ integrator
            if (strategy == LightStrategy.UniformSampleAll)
                L = L.add(UniformSampleAllLights(isect, scene, sampler, nLightSamples, false));
            else
                L = L.add(UniformSampleOneLight(isect, scene, sampler, false, null));
        }
        if (depth + 1 < maxDepth) {
            Vector3f wi;
            // Trace rays for specular reflection and refraction
            L = L.add(SpecularReflect(ray, isect, scene, sampler, depth));
            L = L.add(SpecularTransmit(ray, isect, scene, sampler, depth));
        }
        return L;
    }

    public void Preprocess(Scene scene, Sampler sampler) {
        if (strategy == LightStrategy.UniformSampleAll) {
            // Compute number of samples to use for each light
            nLightSamples = new int[scene.lights.size()];
            int ii = 0;
            for (Light light : scene.lights) {
                nLightSamples[ii++] = sampler.RoundCount(light.nSamples);
            }
            // Request samples for sampling all lights
            for (int i = 0; i < maxDepth; ++i) {
                for (int j = 0; j < scene.lights.size(); ++j) {
                    sampler.Request2DArray(nLightSamples[j]);
                    sampler.Request2DArray(nLightSamples[j]);
                }
            }
        }
    }

    public static DirectLightingIntegrator Create(ParamSet params, Sampler sampler, Camera camera) {
        int maxDepth = params.FindOneInt("maxdepth", 5);
        LightStrategy strategy;
        String st = params.FindOneString("strategy", "all");
        if (Objects.equals(st, "one"))
            strategy = LightStrategy.UniformSampleOne;
        else if (Objects.equals(st, "all"))
            strategy = LightStrategy.UniformSampleAll;
        else {
            Error.Warning("Strategy \"%s\" for direct lighting unknown. Using \"all\".", st);
            strategy = LightStrategy.UniformSampleAll;
        }
        Integer[] pb = params.FindInt("pixelbounds");
        Bounds2i pixelBounds = camera.film.GetSampleBounds();
        if (pb != null) {
            if (pb.length != 4)
                Error.Error("Expected four values for \"pixelbounds\" parameter. Got %d.", pb.length);
            else {
                pixelBounds = Bounds2i.Intersect(pixelBounds, new Bounds2i(new Point2i(pb[0], pb[2]), new Point2i(pb[1], pb[3])));
                if (pixelBounds.Area() == 0)
                    Error.Error("Degenerate \"pixelbounds\" specified.");
            }
        }
        return new DirectLightingIntegrator(strategy, maxDepth, camera, sampler, pixelBounds);
    }

    private final LightStrategy strategy;
    private final int maxDepth;
    private int[] nLightSamples;
}