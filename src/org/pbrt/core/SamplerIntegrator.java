
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public abstract class SamplerIntegrator extends Integrator {

    public SamplerIntegrator(Camera camera, Sampler sampler, Bounds2i pixelBounds) {
        this.camera = camera;
        this.sampler = sampler;
        this.pixelBounds = pixelBounds;
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
        ProgressReporter reporter(nTiles.x * nTiles.y, "Rendering");
        {
            ParallelFor2D([&](Point2i tile) {
            // Render section of image corresponding to _tile_

            // Allocate _MemoryArena_ for tile
            //MemoryArena arena;

            // Get sampler instance for tile
            int seed = tile.y * nTiles.x + tile.x;
            Sampler tileSampler = sampler.Clone(seed);

            // Compute sample bounds for tile
            int x0 = sampleBounds.pMin.x + tile.x * tileSize;
            int x1 = Math.min(x0 + tileSize, sampleBounds.pMax.x);
            int y0 = sampleBounds.pMin.y + tile.y * tileSize;
            int y1 = Math.min(y0 + tileSize, sampleBounds.pMax.y);
            Bounds2i tileBounds = new Bounds2i(new Point2i(x0, y0), new Point2i(x1, y1));
            LOG(INFO) << "Starting image tile " << tileBounds;

            // Get _FilmTile_ for tile
            Film.FilmTile filmTile = camera.film.GetFilmTile(tileBounds);

            // Loop over pixels in tile to render them
            for (Point2i pixel : tileBounds) {
                {
                    ProfilePhase pp(Prof::StartPixel);
                    tileSampler.StartPixel(pixel);
                }

                // Do this check after the StartPixel() call; this keeps
                // the usage of RNG values from (most) Samplers that use
                // RNGs consistent, which improves reproducability /
                // debugging.
                if (!InsideExclusive(pixel, pixelBounds))
                    continue;

                do {
                    // Initialize _CameraSample_ for current sample
                    CameraSample cameraSample = tileSampler.GetCameraSample(pixel);

                    // Generate camera ray for current sample
                    RayDifferential ray;
                    float rayWeight = camera.GenerateRayDifferential(cameraSample, &ray);
                    ray.ScaleDifferentials(1 / (float)Math.sqrt((float)tileSampler.samplesPerPixel));
                    ++nCameraRays;

                    // Evaluate radiance along camera ray
                    Spectrum L = new Spectrum(0);
                    if (rayWeight > 0) L = Li(ray, scene, tileSampler);

                    // Issue warning if unexpected radiance value returned
                    if (L.HasNaNs()) {
                        LOG(ERROR) << StringPrintf(
                                "Not-a-number radiance value returned "
                                "for pixel (%d, %d), sample %d. Setting to black.",
                                pixel.x, pixel.y,
                                (int)tileSampler.CurrentSampleNumber());
                        L = new Spectrum(0);
                    } else if (L.y() < -1e-5f) {
                        LOG(ERROR) << StringPrintf(
                                "Negative luminance value, %f, returned "
                                "for pixel (%d, %d), sample %d. Setting to black.",
                                L.y(), pixel.x, pixel.y,
                                (int)tileSampler.CurrentSampleNumber());
                        L = new Spectrum(0);
                    } else if (Float::isInfinite(L.y())) {
                        LOG(ERROR) << StringPrintf(
                                "Infinite luminance value returned "
                                "for pixel (%d, %d), sample %d. Setting to black.",
                                pixel.x, pixel.y,
                                (int)tileSampler.CurrentSampleNumber());
                        L = new Spectrum(0);
                    }
                    VLOG(1) << "Camera sample: " << cameraSample << " -> ray: " <<
                            ray << " -> L = " << L;

                    // Add camera ray's contribution to image
                    filmTile.AddSample(cameraSample.pFilm, L, rayWeight);

                    // Free _MemoryArena_ memory from computing image sample
                    // value
                    //arena.Reset();
                } while (tileSampler.StartNextSample());
            }
            LOG(INFO) << "Finished image tile " << tileBounds;

            // Merge image tile into _Film_
            camera.film.MergeFilmTile(filmTile);
            reporter.Update();
        }, nTiles);
            reporter.Done();
        }
        LOG(INFO) << "Rendering finished";

        // Save final image after rendering
        camera.film.WriteImage();
    }

    public abstract Spectrum Li(RayDifferential ray, Scene scene, Sampler sampler, int depth);

    public Spectrum SpecularReflect(RayDifferential ray, SurfaceInteraction isect, Scene scene, Sampler sampler, int depth) {
        // Compute specular reflection direction _wi_ and BSDF value
        Vector3f wo = isect.wo;
        int type = BxDF.BSDF_REFLECTION | BxDF.BSDF_SPECULAR;
        BxDF.BxDFSample bxdfs = isect.bsdf.Sample_f(wo, sampler.Get2D(), type);
        Spectrum f = bxdfs.f;
        float pdf = bxdfs.pdf;
        Vector3f wi = bxdfs.wiWorld;

        // Return contribution of specular reflection
        Normal3f ns = isect.shading.n;
        if (pdf > 0 && !f.IsBlack() && Normal3f.AbsDot(wi, ns) != 0) {
            // Compute ray differential _rd_ for specular reflection
            RayDifferential rd = isect.SpawnRay(wi);
            if (ray.hasDifferentials) {
                rd.hasDifferentials = true;
                rd.rxOrigin = isect.p.add(isect.dpdx);
                rd.ryOrigin = isect.p.add(isect.dpdy);
                // Compute differential reflected directions
                Normal3f dndx = isect.shading.dndu.scale(isect.dudx).add(isect.shading.dndv.scale(isect.dvdx));
                Normal3f dndy = isect.shading.dndu.scale(isect.dudy).add(isect.shading.dndv.scale(isect.dvdy));
                Vector3f dwodx = ray.rxDirection.negate().subtract(wo), dwody = ray.ryDirection.negate().subtract(wo);
                float dDNdx = Normal3f.Dot(dwodx, ns) + Normal3f.Dot(wo, dndx);
                float dDNdy = Normal3f.Dot(dwody, ns) + Normal3f.Dot(wo, dndy);
                rd.rxDirection = wi.subtract(dwodx.add(new Vector3f(dndx.scale(Normal3f.Dot(wo, ns)).add(ns.scale(dDNdx))).scale(2));
                rd.ryDirection = wi.subtract(dwody.add(new Vector3f(dndy.scale(Normal3f.Dot(wo, ns)).add(ns.scale(dDNdy))).scale(2));
            }
            return f * Li(rd, scene, sampler, depth + 1) * Normal3f.AbsDot(wi, ns) / pdf;
        } else
            return new Spectrum(0);
    }

    public Spectrum SpecularTransmit(RayDifferential ray, SurfaceInteraction isect, Scene scene, Sampler sampler, int depth) {
        Vector3f wo = isect.wo;
        Point3f p = isect.p;
        Normal3f ns = isect.shading.n;
        BSDF bsdf = isect.bsdf;
        BxDF.BxDFSample bxdfs = bsdf.Sample_f(wo, sampler.Get2D(), BxDF.BSDF_TRANSMISSION | BxDF.BSDF_SPECULAR);
        Spectrum f = bxdfs.f;
        float pdf = bxdfs.pdf;
        Vector3f wi = bxdfs.wiWorld;
        Spectrum L = new Spectrum(0);
        if (pdf > 0 && !f.IsBlack() && Normal3f.AbsDot(wi, ns) != 0) {
            // Compute ray differential _rd_ for specular transmission
            RayDifferential rd = new RayDifferential(isect.SpawnRay(wi));
            if (ray.hasDifferentials) {
                rd.hasDifferentials = true;
                rd.rxOrigin = p.add(isect.dpdx);
                rd.ryOrigin = p.add(isect.dpdy);

                float eta = bsdf.eta;
                Vector3f w = wo.negate();
                if (Normal3f.Dot(wo, ns) < 0) eta = 1 / eta;

                Normal3f dndx = isect.shading.dndu * isect.dudx + isect.shading.dndv * isect.dvdx;
                Normal3f dndy = isect.shading.dndu * isect.dudy + isect.shading.dndv * isect.dvdy;

                Vector3f dwodx = ray.rxDirection.negate().subtract(wo),
                         dwody = ray.ryDirection.negate().subtract(wo);
                float dDNdx = Normal3f.Dot(dwodx, ns) + Normal3f.Dot(wo, dndx);
                float dDNdy = Normal3f.Dot(dwody, ns) + Normal3f.Dot(wo, dndy);

                float mu = eta * Normal3f.Dot(w, ns) - Normal3f.Dot(wi, ns);
                float dmudx = (eta - (eta * eta * Normal3f.Dot(w, ns)) / Normal3f.Dot(wi, ns)) * dDNdx;
                float dmudy = (eta - (eta * eta * Normal3f.Dot(w, ns)) / Normal3f.Dot(wi, ns)) * dDNdy;

                rd.rxDirection = wi + eta * dwodx - Vector3f(mu * dndx + dmudx * ns);
                rd.ryDirection = wi + eta * dwody - Vector3f(mu * dndy + dmudy * ns);
            }
            L = f * Li(rd, scene, sampler, depth + 1) * Normal3f.AbsDot(wi, ns) / pdf;
        }
        return L;
    }

    protected Camera camera;

    private Sampler sampler;
    private Bounds2i pixelBounds;
}