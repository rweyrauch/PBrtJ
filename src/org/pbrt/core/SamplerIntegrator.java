
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
        /*
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
        */
        // Save final image after rendering
        camera.film.WriteImage(1);
    }

    public abstract Spectrum Li(RayDifferential ray, Scene scene, Sampler sampler, int depth);

    public Spectrum SpecularReflect(RayDifferential ray, SurfaceInteraction isect, Scene scene, Sampler sampler, int depth) {
        return null;
    }

    public Spectrum SpecularTransmit(RayDifferential ray, SurfaceInteraction isect, Scene scene, Sampler sampler, int depth) {
        return null;
    }

    protected Camera camera;

    private Sampler sampler;
    private Bounds2i pixelBounds;
}