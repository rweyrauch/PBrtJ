
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

public class PathIntegrator extends SamplerIntegrator{

    public PathIntegrator(int maxDepth, Camera camera, Sampler sampler, Bounds2i pixelBounds, float rrThreshold, String lightSampleStrategy) {
        super(camera, sampler, pixelBounds);
        this.maxDepth = maxDepth;
        this.rrThreshold = rrThreshold;
        this.lightSampleStrategy = lightSampleStrategy;
    }

    @Override
    public Spectrum Li(RayDifferential r, Scene scene, Sampler sampler, int depth) {
        Stats.ProfilePhase p = new Stats.ProfilePhase(Stats.Prof.SamplerIntegratorLi);
        Spectrum L = new Spectrum(0), beta = new Spectrum(1);
        RayDifferential ray = new RayDifferential(r);
        boolean specularBounce = false;
        int bounces;
        // Added after book publication: etaScale tracks the accumulated effect
        // of radiance scaling due to rays passing through refractive
        // boundaries (see the derivation on p. 527 of the third edition). We
        // track this value in order to remove it from beta when we apply
        // Russian roulette; this is worthwhile, since it lets us sometimes
        // avoid terminating refracted rays that are about to be refracted back
        // out of a medium and thus have their beta value increased.
        float etaScale = 1;

        for (bounces = 0;; ++bounces) {
            // Find next path vertex and accumulate contribution
            //VLOG(2) << "Path tracer bounce " << bounces << ", current L = " << L << ", beta = " << beta;

            // Intersect _ray_ with scene and store intersection in _isect_
            SurfaceInteraction isect = scene.Intersect(ray);

            // Possibly add emitted light at intersection
            if ((bounces == 0) || specularBounce) {
                // Add emitted light at path vertex or from the environment
                if (isect != null) {
                    L = L.add(beta.multiply(isect.Le(ray.d.negate())));
                    //VLOG(2) << "Added Le -> L = " << L;
                } else {
                    for (Light light : scene.infiniteLights)
                    L = L.add(beta.multiply(light.Le(ray)));
                    //VLOG(2) << "Added infinite area lights -> L = " << L;
                }
            }

            // Terminate path if ray escaped or _maxDepth_ was reached
            if (isect == null || bounces >= maxDepth) break;

            // Compute scattering functions and skip over medium boundaries
            isect.ComputeScatteringFunctions(ray, true, null);
            if (isect.bsdf == null) {
                //VLOG(2) << "Skipping intersection due to null bsdf";
                ray = new RayDifferential(isect.SpawnRay(ray.d));
                bounces--;
                continue;
            }

            final Distribution1D distrib = lightDistribution.Lookup(isect.p);

            // Sample illumination from lights to find path contribution.
            // (But skip this for perfectly specular BSDFs.)
            if (isect.bsdf.NumComponents(BxDF.BSDF_ALL & ~BxDF.BSDF_SPECULAR) > 0) {
                pathStats.incrementDenom(1); // totalPaths
                Spectrum Ld = beta.multiply(UniformSampleOneLight(isect, scene, sampler, false, distrib));
                //VLOG(2) << "Sampled direct lighting Ld = " << Ld;
                if (Ld.isBlack()) pathStats.incrementNumer(1); // zeroRadiancePaths
                assert (Ld.y() >= 0);
                L = L.add(Ld);
            }

            // Sample BSDF to get new path direction
            Vector3f wo = ray.d.negate(), wi;
            BxDF.BxDFSample bxDFSample = isect.bsdf.Sample_f(wo, sampler.Get2D(), BxDF.BSDF_ALL);
            float pdf = bxDFSample.pdf;
            int flags = bxDFSample.sampledType;
            Spectrum f = bxDFSample.f;
            wi = bxDFSample.wiWorld;

            //VLOG(2) << "Sampled BSDF, f = " << f << ", pdf = " << pdf;
            if (f.isBlack() || pdf == 0) break;
            beta = beta.multiply(f.scale(Normal3f.AbsDot(wi, isect.shading.n) / pdf));
            //VLOG(2) << "Updated beta = " << beta;
            assert (beta.y() >= 0);
            assert (!Float.isInfinite(beta.y()));
            specularBounce = (flags & BxDF.BSDF_SPECULAR) != 0;
            if (((flags & BxDF.BSDF_SPECULAR) != 0) && ((flags & BxDF.BSDF_TRANSMISSION) != 0)) {
                float eta = isect.bsdf.eta;
                // Update the term that tracks radiance scaling for refraction
                // depending on whether the ray is entering or leaving the
                // medium.
                etaScale *= (Normal3f.Dot(wo, isect.n) > 0) ? (eta * eta) : 1 / (eta * eta);
            }
            ray = new RayDifferential(isect.SpawnRay(wi));

            // Account for subsurface scattering, if applicable
            if ((isect.bssrdf != null) && ((flags & BxDF.BSDF_TRANSMISSION) != 0)) {
                // Importance sample the BSSRDF
                BSSRDF.BSSRDFSample sample = isect.bssrdf.Sample_S(scene, sampler.Get1D(), sampler.Get2D());
                SurfaceInteraction pi = sample.si;
                Spectrum S = sample.s;
                pdf = sample.pdf;

                assert(!Float.isInfinite(beta.y()));
                if (S.isBlack() || pdf == 0) break;
                beta = beta.multiply(S.scale(1/ pdf));

                // Account for the direct subsurface scattering component
                L = L.add(beta.multiply(UniformSampleOneLight(pi, scene, sampler, false, lightDistribution.Lookup(pi.p))));

                // Account for the indirect subsurface scattering component
                BxDF.BxDFSample bxsample = pi.bsdf.Sample_f(pi.wo, sampler.Get2D(), BxDF.BSDF_ALL);
                Spectrum ff = bxsample.f;
                pdf = bxsample.pdf;
                wi = bxsample.wiWorld;
                flags = bxsample.sampledType;

                if (ff.isBlack() || pdf == 0) break;
                beta = beta.multiply(ff.scale(Normal3f.AbsDot(wi, pi.shading.n) / pdf));
                assert (!Float.isInfinite(beta.y()));
                specularBounce = (flags & BxDF.BSDF_SPECULAR) != 0;
                ray = new RayDifferential(pi.SpawnRay(wi));
            }

            // Possibly terminate the path with Russian roulette.
            // Factor out radiance scaling due to refraction in rrBeta.
            Spectrum rrBeta = beta.scale(etaScale);
            if (rrBeta.maxComponentValue() < rrThreshold && bounces > 3) {
                float q = Math.max(.05f, 1 - rrBeta.maxComponentValue());
                if (sampler.Get1D() < q) break;
                beta = beta.scale(1 /(1 - q));
                assert (!Float.isInfinite(beta.y()));
            }
        }
        //ReportValue(pathLength, bounces);
        return L;
    }

    public void Preprocess(Scene scene, Sampler sampler) {
        lightDistribution = LightDistribution.CreateLightSampleDistribution(lightSampleStrategy, scene);

    }

    public static PathIntegrator Create(ParamSet params, Sampler sampler, Camera camera) {
        int maxDepth = params.FindOneInt("maxdepth", 5);
        final Integer[] pb = params.FindInt("pixelbounds");
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
        float rrThreshold = params.FindOneFloat("rrthreshold", 1);
        String lightStrategy = params.FindOneString("lightsamplestrategy", "spatial");
        return new PathIntegrator(maxDepth, camera, sampler, pixelBounds, rrThreshold, lightStrategy);
    }

    private final int maxDepth;
    private final float rrThreshold;
    private final String lightSampleStrategy;
    private LightDistribution lightDistribution;

    private static Stats.STAT_PERCENT pathStats = new Stats.STAT_PERCENT("Integrator/Zero-radiance paths");
    private static Stats.STAT_INT_DISTRIBUTION pathLength = new Stats.STAT_INT_DISTRIBUTION("Integrator/Path length");

}