
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
import org.pbrt.filters.BoxFilter;

import java.util.HashMap;

public class BDPTIntegrator extends Integrator {

    public BDPTIntegrator(Sampler sampler, Camera camera, int maxDepth, boolean visualizeStrategies, boolean visualizeWeights, Bounds2i pixelBounds, String lightSampleStrategy) {
        super();
        this.sampler = sampler;
        this.camera = camera;
        this.maxDepth = maxDepth;
        this.visualizeStrategies = visualizeStrategies;
        this.visualizeWeights =visualizeWeights;
        this.pixelBounds = new Bounds2i(pixelBounds);
        this.lightSampleStrategy = lightSampleStrategy;
    }

    @Override
    public void Render(Scene scene) {
        LightDistribution lightDistribution = LightDistribution.CreateLightSampleDistribution(lightSampleStrategy, scene);

        // Compute a reverse mapping from light pointers to offsets into the
        // scene lights vector (and, equivalently, offsets into
        // lightDistr). Added after book text was finalized; this is critical
        // to reasonable performance with 100s+ of light sources.
        HashMap<Light, Integer> lightToIndex = new HashMap<>();
        for (int i = 0; i < scene.lights.size(); ++i)
            lightToIndex.put(scene.lights.get(i), i);

        // Partition the image into tiles
        Film film = camera.film;
        final Bounds2i sampleBounds = film.GetSampleBounds();
        final Vector2i sampleExtent = sampleBounds.Diagonal();
        final int tileSize = 16;
        final int nXTiles = (sampleExtent.x + tileSize - 1) / tileSize;
        final int nYTiles = (sampleExtent.y + tileSize - 1) / tileSize;

        ProgressReporter reporter = new ProgressReporter(nXTiles * nYTiles, "Rendering");

        // Allocate buffers for debug visualization
        final int bufferCount = (1 + maxDepth) * (6 + maxDepth) / 2;
        Film[] weightFilms = new Film[bufferCount];
        if (visualizeStrategies || visualizeWeights) {
            for (int depth = 0; depth <= maxDepth; ++depth) {
                for (int s = 0; s <= depth + 2; ++s) {
                    int t = depth + 2 - s;
                    if (t == 0 || (s == 1 && t == 1)) continue;

                    String filename = String.format("bdpt_d%02i_s%02i_t%02i.exr", depth, s, t);

                    weightFilms[BufferIndex(s, t)] = new Film(film.fullResolution, new Bounds2f(new Point2f(0, 0), new Point2f(1, 1)),
                            BoxFilter.Create(new ParamSet()), film.diagonal * 1000, filename, 1, Pbrt.Infinity);
                }
            }
        }

        // Render and write the output image to disk
        if (scene.lights.size() > 0) {
            for (int y = 0; y < nYTiles; y++) {
                for (int x = 0; x < nXTiles; x++) {
                    Point2i tile = new Point2i(x, y);

                    // Render a single tile using BDPT
                    int seed = tile.y * nXTiles + tile.x;
                    Sampler tileSampler = sampler.Clone(seed);
                    int x0 = sampleBounds.pMin.x + tile.x * tileSize;
                    int x1 = Math.min(x0 + tileSize, sampleBounds.pMax.x);
                    int y0 = sampleBounds.pMin.y + tile.y * tileSize;
                    int y1 = Math.min(y0 + tileSize, sampleBounds.pMax.y);
                    Bounds2i tileBounds = new Bounds2i(new Point2i(x0, y0), new Point2i(x1, y1));
                    Film.FilmTile filmTile = camera.film.GetFilmTile(tileBounds);
                    for (int py = tileBounds.pMin.y; py < tileBounds.pMax.y; py++) {
                        for (int px = tileBounds.pMin.x; px < tileBounds.pMax.x; px++) {
                            Point2i pPixel = new Point2i(px, py);
                            tileSampler.StartPixel(pPixel);
                            if (!Bounds2i.InsideExclusive(pPixel, pixelBounds))
                                continue;
                            do {
                                // Generate a single sample using BDPT
                                Point2f pFilm = (new Point2f(pPixel)).add(tileSampler.Get2D());

                                // Trace the camera subpath
                                Vertex[] cameraVertices = new Vertex[maxDepth + 2];
                                Vertex[] lightVertices = new Vertex[maxDepth + 1];
                                int nCamera = Vertex.GenerateCameraSubpath(scene, tileSampler, maxDepth + 2, camera,
                                        pFilm, cameraVertices);
                                // Get a distribution for sampling the light at the
                                // start of the light subpath. Because the light path
                                // follows multiple bounces, basing the sampling
                                // distribution on any of the vertices of the camera
                                // path is unlikely to be a good strategy. We use the
                                // PowerLightDistribution by default here, which
                                // doesn't use the point passed to it.
                                final Distribution1D lightDistr = lightDistribution.Lookup(cameraVertices[0].p());
                                // Now trace the light subpath
                                int nLight = Vertex.GenerateLightSubpath(scene, tileSampler, maxDepth + 1,
                                        cameraVertices[0].time(), lightDistr, lightToIndex, lightVertices);

                                // Execute all BDPT connection strategies
                                Spectrum L = new Spectrum(0);
                                for (int t = 1; t <= nCamera; ++t) {
                                    for (int s = 0; s <= nLight; ++s) {
                                        int depth = t + s - 2;
                                        if ((s == 1 && t == 1) || depth < 0 ||
                                                depth > maxDepth)
                                            continue;
                                        // Execute the $(s, t)$ connection strategy and
                                        // update _L_
                                        Point2f[] pFilmNew = new Point2f[1];
                                        pFilmNew[0] = new Point2f(pFilm);
                                        float[] misWeight = new float[1];
                                        Spectrum Lpath = Vertex.ConnectBDPT(scene, lightVertices, cameraVertices, s, t,
                                                lightDistr, lightToIndex, camera, tileSampler, pFilmNew, misWeight);
                                        //VLOG(2) << "Connect bdpt s: " << s <<", t: " << t << ", Lpath: " << Lpath << ", misWeight: " << misWeight;
                                        if (visualizeStrategies || visualizeWeights) {
                                            Spectrum value = new Spectrum(0);
                                            if (visualizeStrategies)
                                                value = (misWeight[0] == 0) ? new Spectrum(0) : Lpath.scale(1 / misWeight[0]);
                                            if (visualizeWeights) value = Lpath;
                                            weightFilms[BufferIndex(s, t)].AddSplat(pFilmNew[0], value);
                                        }
                                        if (t != 1)
                                            L = L.add(Lpath);
                                        else
                                            film.AddSplat(pFilmNew[0], Lpath);
                                    }
                                }
                                //VLOG(2) << "Add film sample pFilm: " << pFilm << ", L: " << L << ", (y: " << L.y() << ")";
                                filmTile.AddSample(pFilm, L, 1);

                            } while (tileSampler.StartNextSample());
                        }
                    }
                    film.MergeFilmTile(filmTile);
                    reporter.Update(1);
                }
            }
            reporter.Done();
        }
        film.WriteImage(1.0f / sampler.samplesPerPixel);

        // Write buffers for debug visualization
        if (visualizeStrategies || visualizeWeights) {
            final float invSampleCount = 1.0f / sampler.samplesPerPixel;
            for (int i = 0; i < weightFilms.length; ++i)
                if (weightFilms[i] != null) weightFilms[i].WriteImage(invSampleCount);
        }
    }

    public static BDPTIntegrator Create(ParamSet params, Sampler sampler, Camera camera) {
        int maxDepth = params.FindOneInt("maxdepth", 5);
        boolean visualizeStrategies = params.FindOneBoolean("visualizestrategies", false);
        boolean visualizeWeights = params.FindOneBoolean("visualizeweights", false);

        if ((visualizeStrategies || visualizeWeights) && maxDepth > 5) {
            Error.Warning("visualizestrategies/visualizeweights was enabled, limiting maxdepth to 5");
            maxDepth = 5;
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

        String lightStrategy = params.FindOneString("lightsamplestrategy", "power");
        return new BDPTIntegrator(sampler, camera, maxDepth, visualizeStrategies,
                visualizeWeights, pixelBounds, lightStrategy);
    }

    private Sampler sampler;
    private final Camera camera;
    private final int maxDepth;
    private final boolean visualizeStrategies;
    private final boolean visualizeWeights;
    private final Bounds2i pixelBounds;
    private final String lightSampleStrategy;

    private static int BufferIndex(int s, int t) {
        int above = s + t - 2;
        return s + above * (5 + above) / 2;
    }

}