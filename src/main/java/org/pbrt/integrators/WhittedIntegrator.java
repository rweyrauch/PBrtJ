
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
import org.pbrt.core.PBrtTLogger;;

public class WhittedIntegrator extends SamplerIntegrator {

    public WhittedIntegrator(int maxDepth, Camera camera, Sampler sampler, Bounds2i pixelBounds) {
        super(camera, sampler, pixelBounds);
        this.maxDepth = maxDepth;
    }

    @Override
    public Spectrum Li(RayDifferential ray, Scene scene, Sampler sampler, int depth) {
        Spectrum L = new Spectrum(0);
        // Find closest ray intersection or return background radiance
        SurfaceInteraction isect = scene.Intersect(ray);
        if (isect == null) {
            for (Light light : scene.lights) L = L.add(light.Le(ray));
            return L;
        }

        // Compute emitted and reflected light at ray intersection point

        // Initialize common variables for Whitted integrator
        final Normal3f n = isect.shading.n;
        Vector3f wo = isect.wo;

        // Compute scattering functions for surface interaction
        isect.ComputeScatteringFunctions(ray);
        if (isect.bsdf == null)
            return Li(new RayDifferential(isect.SpawnRay(ray.d)), scene, sampler, depth);

        // Compute emitted light if ray hit an area light source
        L = L.add(isect.Le(wo));

        // Add contribution of each light source
        for (Light light : scene.lights) {
            Light.LiResult liResult = light.Sample_Li(isect, sampler.Get2D());
            Spectrum Li = liResult.spectrum;
            Vector3f wi = liResult.wi;
            float pdf = liResult.pdf;
            Light.VisibilityTester visibility = liResult.vis;

            if (Li.isBlack() || pdf == 0) continue;
            Spectrum f = isect.bsdf.f(wo, wi);
            if (!f.isBlack() && visibility.Unoccluded(scene))
                L = L.add(f.multiply(Li.scale(Normal3f.AbsDot(wi, n) / pdf)));
        }
        if (depth + 1 < maxDepth) {
            // Trace rays for specular reflection and refraction
            L = L.add(SpecularReflect(ray, isect, scene, sampler, depth));
            L = L.add(SpecularTransmit(ray, isect, scene, sampler, depth));
        }
        return L;
    }

    public static WhittedIntegrator Create(ParamSet params, Sampler sampler, Camera camera){
        int maxDepth = params.FindOneInt("maxdepth", 5);
        Integer[] pb = params.FindInt("pixelbounds");
        Bounds2i pixelBounds = camera.film.GetSampleBounds();
        if (pb != null) {
            if (pb.length != 4)
                PBrtTLogger.Error("Expected four values for \"pixelbounds\" parameter. Got %d.", pb.length);
            else {
                pixelBounds = Bounds2i.Intersect(pixelBounds, new Bounds2i(new Point2i(pb[0], pb[2]), new Point2i(pb[1], pb[3])));
                if (pixelBounds.Area() == 0)
                    PBrtTLogger.Error("Degenerate \"pixelbounds\" specified.");
            }
        }
        return new WhittedIntegrator(maxDepth, camera, sampler, pixelBounds);
    }

    private final int maxDepth;
}