
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

import org.apache.log4j.LogManager;

import java.util.ArrayList;

public abstract class SamplerIntegrator extends Integrator {

    public SamplerIntegrator(Camera camera, Sampler sampler, Bounds2i pixelBounds) {
        this.camera = camera;
        this.sampler = sampler;
        this.pixelBounds = new Bounds2i(pixelBounds);
    }

    public void Preprocess(Scene scene, Sampler sampler) {}

    @Override
    public void Render(Scene scene) {
        Preprocess(scene, sampler);
        // Render image tiles in parallel

        // Compute number of tiles, _nTiles_, to use for parallel rendering
        Bounds2i sampleBounds = camera.film.GetSampleBounds();
        Vector2i sampleExtent = sampleBounds.Diagonal();
        int tileSize = 16;
        Point2i nTiles = new Point2i((sampleExtent.x + tileSize - 1) / tileSize, (sampleExtent.y + tileSize - 1) / tileSize);

        ProgressReporter reporter = new ProgressReporter(nTiles.x * nTiles.y, "Rendering");

        {
            for (int y = 0; y < nTiles.y; y++) {
                for (int x = 0; x < nTiles.x; x++) {
                    Point2i tile = new Point2i(x, y);

                    // Render section of image corresponding to _tile_

                    // Get sampler instance for tile
                    int seed = tile.y * nTiles.x + tile.x;
                    Sampler tileSampler = sampler.Clone(seed);

                    // Compute sample bounds for tile
                    int x0 = sampleBounds.pMin.x + tile.x * tileSize;
                    int x1 = Math.min(x0 + tileSize, sampleBounds.pMax.x);
                    int y0 = sampleBounds.pMin.y + tile.y * tileSize;
                    int y1 = Math.min(y0 + tileSize, sampleBounds.pMax.y);
                    Bounds2i tileBounds = new Bounds2i(new Point2i(x0, y0), new Point2i(x1, y1));
                    Api.logger.info("Starting image tile %s\n", tileBounds.toString());

                    // Get _FilmTile_ for tile
                    Film.FilmTile filmTile = camera.film.GetFilmTile(tileBounds);

                    // Loop over pixels in tile to render them
                    for (int py = tileBounds.pMin.y; py < tileBounds.pMax.y; py++) {
                        for (int px = tileBounds.pMin.x; px < tileBounds.pMax.x; px++) {
                            Point2i pixel = new Point2i(px, py);
                            {
                                Stats.ProfilePhase pp = new Stats.ProfilePhase(Stats.Prof.StartPixel);
                                tileSampler.StartPixel(pixel);
                            }

                            // Do this check after the StartPixel() call; this keeps
                            // the usage of RNG values from (most) Samplers that use
                            // RNGs consistent, which improves reproducability /
                            // debugging.
                            if (!Bounds2i.InsideExclusive(pixel, pixelBounds))
                                continue;

                            do {
                                // Initialize _CameraSample_ for current sample
                                Camera.CameraSample cameraSample = tileSampler.GetCameraSample(pixel);

                                // Generate camera ray for current sample
                                Camera.CameraRayDiff camRay = camera.GenerateRayDifferential(cameraSample);
                                RayDifferential ray = camRay.rd;
                                float rayWeight = camRay.weight;

                                ray.ScaleDifferentials(1 / (float) Math.sqrt((float) tileSampler.samplesPerPixel));
                                nCameraRays.increment();

                                // Evaluate radiance along camera ray
                                Spectrum L = new Spectrum(0);
                                if (rayWeight > 0) L = Li(ray, scene, tileSampler, 0);

                                // Issue warning if unexpected radiance value returned
                                if (L.hasNaNs()) {
                                    Api.logger.error("Not-a-number radiance value returned for pixel (%d, %d), sample %d. Setting to black.",
                                            pixel.x, pixel.y, tileSampler.CurrentSampleNumber());
                                    L = new Spectrum(0);
                                } else if (L.y() < -1e-5f) {
                                    Api.logger.error("Negative luminance value, %f, returned for pixel (%d, %d), sample %d. Setting to black.",
                                            L.y(), pixel.x, pixel.y, tileSampler.CurrentSampleNumber());
                                    L = new Spectrum(0);
                                } else if (Float.isInfinite(L.y())) {
                                    Api.logger.error("Infinite luminance value returned for pixel (%d, %d), sample %d. Setting to black.",
                                            pixel.x, pixel.y, tileSampler.CurrentSampleNumber());
                                    L = new Spectrum(0);
                                }
                                //System.out.format("Camera sample: (%f,%f) L: (%f,%f,%f)\n", cameraSample.pFilm.x, cameraSample.pFilm.y, L.at(0), L.at(1), L.at(2));

                                // Add camera ray's contribution to image
                                filmTile.AddSample(cameraSample.pFilm, L, rayWeight);

                            } while (tileSampler.StartNextSample());
                        }
                    }
                    Api.logger.info("Finished image tile, %s", tileBounds.toString());

                    // Merge image tile into _Film_
                    camera.film.MergeFilmTile(filmTile);
                    reporter.Update(1);
                }
            }
            reporter.Done();
        }
        Api.logger.info("Rendering finished");
        reporter.Exit();

        // Save final image after rendering
        camera.film.WriteImage(1);
    }

    public abstract Spectrum Li(RayDifferential ray, Scene scene, Sampler sampler, int depth);

    public Spectrum SpecularReflect(RayDifferential ray, SurfaceInteraction isect, Scene scene, Sampler sampler, int depth) {
        // Compute specular reflection direction _wi_ and BSDF value
        Vector3f wo = isect.wo, wi;
        float pdf;
        int type = BxDF.BSDF_REFLECTION | BxDF.BSDF_SPECULAR;
        Spectrum f;
        final BxDF.BxDFSample bxDFSample = isect.bsdf.Sample_f(wo, sampler.Get2D(), type);
        wi = bxDFSample.wiWorld;
        f = bxDFSample.f;
        pdf = bxDFSample.pdf;

        // Return contribution of specular reflection
        final Normal3f ns = isect.shading.n;
        if (pdf > 0.f && !f.isBlack() && Normal3f.AbsDot(wi, ns) != 0.f) {
            // Compute ray differential _rd_ for specular reflection
            RayDifferential rd = new RayDifferential(isect.SpawnRay(wi));
            if (ray.hasDifferentials) {
                rd.hasDifferentials = true;
                rd.rxOrigin = isect.p.add(isect.dpdx);
                rd.ryOrigin = isect.p.add(isect.dpdy);
                // Compute differential reflected directions
                Normal3f dndx = isect.shading.dndu.scale(isect.dudx).add(isect.shading.dndv.scale(isect.dvdx));
                Normal3f dndy = isect.shading.dndu.scale(isect.dudy).add(isect.shading.dndv.scale(isect.dvdy));
                Vector3f dwodx = (ray.rxDirection.negate()).subtract(wo),
                         dwody = (ray.ryDirection.negate()).subtract(wo);
                float dDNdx = Normal3f.Dot(dwodx, ns) + Normal3f.Dot(wo, dndx);
                float dDNdy = Normal3f.Dot(dwody, ns) + Normal3f.Dot(wo, dndy);
                rd.rxDirection = wi.subtract(dwodx).add((new Vector3f(dndx.scale(Normal3f.Dot(wo, ns)).add(ns.scale(dDNdx)))).scale(2));
                rd.ryDirection = wi.subtract(dwody).add((new Vector3f(dndy.scale(Normal3f.Dot(wo, ns)).add(ns.scale(dDNdy)))).scale(2));
            }
            return f.multiply((Li(rd, scene, sampler, depth + 1)).scale(Normal3f.AbsDot(wi, ns) / pdf));
        } else
            return new Spectrum(0);
    }

    public Spectrum SpecularTransmit(RayDifferential ray, SurfaceInteraction isect, Scene scene, Sampler sampler, int depth) {
        Vector3f wo = isect.wo, wi;
        float pdf;
        final Point3f p = isect.p;
        final Normal3f ns = isect.shading.n;
        final BSDF bsdf = isect.bsdf;
        Spectrum f;
        final BxDF.BxDFSample bxDFSample = bsdf.Sample_f(wo, sampler.Get2D(), BxDF.BSDF_TRANSMISSION | BxDF.BSDF_SPECULAR);
        f = bxDFSample.f;
        wi = bxDFSample.wiWorld;
        pdf = bxDFSample.pdf;
        Spectrum L = new Spectrum(0.f);
        if (pdf > 0.f && !f.isBlack() && Normal3f.AbsDot(wi, ns) != 0.f) {
            // Compute ray differential _rd_ for specular transmission
            RayDifferential rd = new RayDifferential(isect.SpawnRay(wi));
            if (ray.hasDifferentials) {
                rd.hasDifferentials = true;
                rd.rxOrigin = p.add(isect.dpdx);
                rd.ryOrigin = p.add(isect.dpdy);

                float eta = bsdf.eta;
                Vector3f w = wo.negate();
                if (Normal3f.Dot(wo, ns) < 0) eta = 1.f / eta;

                Normal3f dndx = isect.shading.dndu.scale(isect.dudx).add(isect.shading.dndv.scale(isect.dvdx));
                Normal3f dndy = isect.shading.dndu.scale(isect.dudy).add(isect.shading.dndv.scale(isect.dvdy));
                Vector3f dwodx = (ray.rxDirection.negate()).subtract(wo),
                        dwody = (ray.ryDirection.negate()).subtract(wo);
                float dDNdx = Normal3f.Dot(dwodx, ns) + Normal3f.Dot(wo, dndx);
                float dDNdy = Normal3f.Dot(dwody, ns) + Normal3f.Dot(wo, dndy);

                float mu = eta * Normal3f.Dot(w, ns) - Normal3f.Dot(wi, ns);
                float dmudx = (eta - (eta * eta * Normal3f.Dot(w, ns)) / Normal3f.Dot(wi, ns)) * dDNdx;
                float dmudy = (eta - (eta * eta * Normal3f.Dot(w, ns)) / Normal3f.Dot(wi, ns)) * dDNdy;

                rd.rxDirection = wi.add(dwodx.scale(eta).subtract(new Vector3f(dndx.scale(mu).add(ns.scale(dmudx)))));
                rd.ryDirection = wi.add(dwody.scale(eta).subtract(new Vector3f(dndy.scale(mu).add(ns.scale(dmudy)))));
            }
            L = f.multiply(Li(rd, scene, sampler, depth + 1).scale(Normal3f.AbsDot(wi, ns) / pdf));
        }
        return L;
    }

    public static Spectrum UniformSampleAllLights(Interaction it, Scene scene, Sampler sampler, int[] nLightSamples, boolean handleMedia) {
        Stats.ProfilePhase p = new Stats.ProfilePhase(Stats.Prof.DirectLighting);
        Spectrum L = new Spectrum(0);
        for (int j = 0; j < scene.lights.size(); ++j) {
            // Accumulate contribution of _j_th light to _L_
            Light light = scene.lights.get(j);
            int nSamples = nLightSamples[j];
            Point2f[] uLightArray = sampler.Get2DArray(nSamples);
            Point2f[] uScatteringArray = sampler.Get2DArray(nSamples);
            if (uLightArray == null || uScatteringArray == null) {
                // Use a single sample for illumination from _light_
                Point2f uLight = sampler.Get2D();
                Point2f uScattering = sampler.Get2D();
                L = L.add(EstimateDirect(it, uScattering, light, uLight, scene, sampler, handleMedia, false));
            } else {
                // Estimate direct lighting using sample arrays
                Spectrum Ld = new Spectrum(0);
                for (int k = 0; k < nSamples; ++k)
                    Ld = Ld.add(EstimateDirect(it, uScatteringArray[k], light, uLightArray[k], scene, sampler, handleMedia, false));
                L = L.add(Ld.scale(1.0f/ nSamples));
            }
        }
        return L;
    }

    public static Spectrum UniformSampleOneLight(Interaction it, Scene scene, Sampler sampler, boolean handleMedia, Distribution1D lightDistrib) {
        Stats.ProfilePhase p = new Stats.ProfilePhase(Stats.Prof.DirectLighting);
        // Randomly choose a single light to sample, _light_
        int nLights = scene.lights.size();
        if (nLights == 0) return new Spectrum(0);
        int lightNum;
        float lightPdf;
        if (lightDistrib != null) {
            Distribution1D.DiscreteSample ds = lightDistrib.SampleDiscrete(sampler.Get1D());
            lightPdf = ds.pdf;
            lightNum = ds.offset;
            if (lightPdf == 0) return new Spectrum(0);
        } else {
            lightNum = Math.min((int)(sampler.Get1D() * nLights), nLights - 1);
            lightPdf = 1.0f / nLights;
        }
        final Light light = scene.lights.get(lightNum);
        Point2f uLight = sampler.Get2D();
        Point2f uScattering = sampler.Get2D();
        return EstimateDirect(it, uScattering, light, uLight,
                scene, sampler, handleMedia, false).scale(1.0f / lightPdf);
    }

    public static Spectrum EstimateDirect(Interaction it, Point2f uScattering, Light light, Point2f uLight,
                        Scene scene, Sampler sampler, boolean handleMedia, boolean specular) {
        int bsdfFlags =
                specular ? BxDF.BSDF_ALL : BxDF.BSDF_ALL & ~BxDF.BSDF_SPECULAR;
        Spectrum Ld = new Spectrum(0);
        // Sample light source with multiple importance sampling
        Vector3f wi;
        float lightPdf = 0, scatteringPdf = 0;
        Light.VisibilityTester visibility;
        Spectrum Li;
        Light.LiResult lis = light.Sample_Li(it, uLight);
        Li = lis.spectrum;
        visibility = lis.vis;
        wi = lis.wi;
        lightPdf = lis.pdf;

        Api.logger.trace("EstimateDirect uLight: %s -> Li: %s, wi: %s, pdf: %f", uLight.toString(), Li.toString(), wi.toString(), lightPdf);
        if (lightPdf > 0 && !Li.isBlack()) {
            // Compute BSDF or phase function's value for light sample
            Spectrum f;
            if (it.IsSurfaceInteraction()) {
                // Evaluate BSDF for light sampling strategy
                final SurfaceInteraction isect = (SurfaceInteraction)it;
                f = isect.bsdf.f(isect.wo, wi, bsdfFlags).scale(Normal3f.AbsDot(wi, isect.shading.n));
                scatteringPdf = isect.bsdf.Pdf(isect.wo, wi, bsdfFlags);
                Api.logger.trace("  surf f*dot: %s, scatteringPdf: %f", f, scatteringPdf);
            } else {
                // Evaluate phase function for light sampling strategy
                final MediumInteraction mi = (MediumInteraction)it;
                float p = mi.phase.p(mi.wo, wi);
                f = new Spectrum(p);
                scatteringPdf = p;
                Api.logger.trace("  medium p: %f", p);
            }
            if (!f.isBlack()) {
                // Compute effect of visibility for light source sample
                if (handleMedia) {
                    Li = Li.multiply(visibility.Tr(scene, sampler));
                    Api.logger.trace("  after Tr, Li: %s", Li);
                } else {
                    if (!visibility.Unoccluded(scene)) {
                        Api.logger.trace("  shadow ray blocked");
                        Li = new Spectrum(0);
                    } else {
                        Api.logger.trace("  shadow ray unoccluded");
                    }
                }

                // Add light's contribution to reflected radiance
                if (!Li.isBlack()) {
                    if (Light.IsDeltaLight(light.flags))
                        Ld = Ld.add(f.multiply(Li.scale(1 / lightPdf)));
                    else {
                        float weight = Sampling.PowerHeuristic(1, lightPdf, 1, scatteringPdf);
                        Ld = Ld.add(f.multiply(Li.scale(weight / lightPdf)));
                    }
                }
            }
        }

        // Sample BSDF with multiple importance sampling
        if (!Light.IsDeltaLight(light.flags)) {
            Spectrum f;
            boolean sampledSpecular = false;
            if (it.IsSurfaceInteraction()) {
                // Sample scattered direction for surface interactions
                final SurfaceInteraction isect = (SurfaceInteraction)it;

                assert isect.wo != null;
                assert uScattering != null;
                
                BxDF.BxDFSample bs = isect.bsdf.Sample_f(isect.wo, uScattering, bsdfFlags);
                int sampledType = bs.sampledType;
                f = bs.f;
                wi = bs.wiWorld;
                scatteringPdf = bs.pdf;
                f = f.scale(Normal3f.AbsDot(wi, isect.shading.n));
                sampledSpecular = (sampledType & BxDF.BSDF_SPECULAR) != 0;
            } else {
                // Sample scattered direction for medium interactions
                final MediumInteraction mi = (MediumInteraction)it;
                PhaseFunction.PhaseSample ps = mi.phase.Sample_p(mi.wo, uScattering);
                float p = ps.phase;
                wi = ps.wi;
                f = new Spectrum(p);
                scatteringPdf = p;
            }
            Api.logger.trace("  BSDF / phase sampling f: %s, scatteringPdf: %f", f, scatteringPdf);
            if (!f.isBlack() && scatteringPdf > 0) {
                // Account for light contributions along sampled direction _wi_
                float weight = 1;
                if (!sampledSpecular) {
                    lightPdf = light.Pdf_Li(it, wi);
                    if (lightPdf == 0) return Ld;
                    weight = Sampling.PowerHeuristic(1, scatteringPdf, 1, lightPdf);
                }

                // Find intersection and compute transmittance
                SurfaceInteraction lightIsect = null;
                Ray ray = it.SpawnRay(wi);
                Spectrum Tr = new Spectrum(1);
                if (handleMedia) {
                    Scene.TrIntersection trIntersection = scene.IntersectTr(ray, sampler);
                    if (trIntersection != null) {
                         lightIsect = trIntersection.isect;
                        Tr = trIntersection.Tr;
                    }
                }
                else {
                    lightIsect = scene.Intersect(ray);
                }
                // Add light contribution from material sampling
                Li = new Spectrum(0);
                if (lightIsect != null) {
                    if (lightIsect.primitive.GetAreaLight() == light)
                    Li = lightIsect.Le(wi.negate());
                } else
                    Li = light.Le(new RayDifferential(ray));
                if (!Li.isBlack()) Ld = Ld.add(f.multiply(Li.multiply(Tr.scale(weight / scatteringPdf))));
            }
        }
        return Ld;
    }

    public static Distribution1D ComputeLightPowerDistribution(Scene scene) {
        if (scene.lights.isEmpty()) return null;
        float[] lightPower = new float[scene.lights.size()];
        int i = 0;
        for (Light light : scene.lights) {
            lightPower[i] =light.Power().y();
            i++;
        }
        return new Distribution1D(lightPower);
    }

    protected Camera camera;

    private Sampler sampler;
    private final Bounds2i pixelBounds;

    private static Stats.STAT_COUNTER nCameraRays = new Stats.STAT_COUNTER("Integrator/Camera rays traced");

}