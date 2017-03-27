
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

import java.util.HashMap;
import java.util.function.Consumer;

public class MLTIntegrator extends Integrator {

    private static final int cameraStreamIndex = 0;
    private static final int lightStreamIndex = 1;
    private static final int connectionStreamIndex = 2;
    private static final int nSampleStreams = 3;

    public MLTIntegrator(Camera camera, int maxDepth, int nBootstrap, int nChains, int mutationsPerPixel, float sigma, float largeStepProbability) {
        super();
        this.camera = camera;
        this.maxDepth = maxDepth;
        this.nBootstrap = nBootstrap;
        this.nChains = nChains;
        this.mutationsPerPixel = mutationsPerPixel;
        this.sigma = sigma;
        this.largeStepProbability = largeStepProbability;
    }

    @Override
    public void Render(Scene scene) {
        Distribution1D lightDistr = ComputeLightPowerDistribution(scene);

        // Compute a reverse mapping from light pointers to offsets into the
        // scene lights vector (and, equivalently, offsets into
        // lightDistr). Added after book text was finalized; this is critical
        // to reasonable performance with 100s+ of light sources.
        HashMap<Light, Integer> lightToIndex = new HashMap<>();
        for (int i = 0; i < scene.lights.size(); ++i)
            lightToIndex.put(scene.lights.get(i), i);

        // Generate bootstrap samples and compute normalization constant $b$
        int nBootstrapSamples = nBootstrap * (maxDepth + 1);
        float[] bootstrapWeights = new float[nBootstrapSamples];
        if (scene.lights.size() > 0) {
            ProgressReporter progress = new ProgressReporter(nBootstrap / 256, "Generating bootstrap paths");
            int chunkSize = Pbrt.Clamp(nBootstrap / 128, 1, 8192);

            Consumer<Long> bootFunc = (Long li) -> {
                int i = Math.toIntExact(li);
                // Generate _i_th bootstrap sample
                for (int depth = 0; depth <= maxDepth; ++depth) {
                    int rngIndex = i * (maxDepth + 1) + depth;
                    MLTSampler sampler = new MLTSampler(mutationsPerPixel, rngIndex, sigma, largeStepProbability, nSampleStreams);
                    Point2f[] pRaster = new Point2f[1];
                    bootstrapWeights[rngIndex] = L(scene, lightDistr, lightToIndex, sampler, depth, pRaster).y();
                }
                if ((i + 1 % 256) == 0) progress.Update(1);
            };
            Parallel.ParallelFor(bootFunc, nBootstrap, chunkSize);
            progress.Done();
        }
        Distribution1D bootstrap = new Distribution1D(bootstrapWeights);
        float b = bootstrap.funcInt * (maxDepth + 1);

        // Run _nChains_ Markov chains in parallel
        Film film = camera.film;
        long nTotalMutations = (long)mutationsPerPixel * (long)film.GetSampleBounds().Area();
        if (scene.lights.size() > 0) {
            final int progressFrequency = 32768;
            ProgressReporter progress = new ProgressReporter(nTotalMutations / progressFrequency, "Rendering");

            Consumer<Long> renderFunc = (Long li) -> {
                int i = Math.toIntExact(li);

                long nChainMutations = Math.min((i + 1) * nTotalMutations / nChains, nTotalMutations) - i * nTotalMutations / nChains;
                // Follow {i}th Markov chain for _nChainMutations_

                // Select initial state from the set of bootstrap samples
                RNG rng = new RNG(i);
                Distribution1D.DiscreteSample ds = bootstrap.SampleDiscrete(rng.UniformFloat());
                int bootstrapIndex = ds.offset;
                int depth = bootstrapIndex % (maxDepth + 1);

                // Initialize local variables for selected state
                MLTSampler sampler = new MLTSampler(mutationsPerPixel, bootstrapIndex, sigma, largeStepProbability, nSampleStreams);
                Point2f[] pCurrent = new Point2f[1];
                pCurrent[0] = new Point2f();
                Spectrum LCurrent = L(scene, lightDistr, lightToIndex, sampler, depth, pCurrent);

                // Run the Markov chain for _nChainMutations_ steps
                for (long j = 0; j < nChainMutations; ++j) {
                    sampler.StartIteration();
                    Point2f[] pProposed = new Point2f[1];
                    pProposed[0] = new Point2f();
                    Spectrum LProposed = L(scene, lightDistr, lightToIndex, sampler, depth, pProposed);
                    // Compute acceptance probability for proposed sample
                    float accept = Math.min(1, LProposed.y() / LCurrent.y());

                    // Splat both current and proposed samples to _film_
                    if (accept > 0)
                        film.AddSplat(pProposed[0], (LProposed.scale(accept)).scale(1 / LProposed.y()));
                    film.AddSplat(pCurrent[0], (LCurrent.scale(1 - accept)).scale(1 / LCurrent.y()));

                    // Accept or reject the proposal
                    if (rng.UniformFloat() < accept) {
                        pCurrent = pProposed;
                        LCurrent = LProposed;
                        sampler.Accept();
                        acceptedMutationsPerc.incrementNumer(1); // ++acceptedMutations;
                    } else
                        sampler.Reject();
                    acceptedMutationsPerc.incrementDenom(1); // ++totalMutations;
                    if ((i * nTotalMutations / nChains + j) % progressFrequency == 0)
                        progress.Update(1);
                }
            };
            Parallel.ParallelFor(renderFunc, nChains, 1);
            progress.Done();
        }

        // Store final image computed with MLT
        camera.film.WriteImage(b / mutationsPerPixel);
    }

    public Spectrum L(Scene scene, Distribution1D lightDistr, HashMap<Light, Integer> lightToIndex, MLTSampler sampler, int depth, Point2f[] pRaster) {
        sampler.StartStream(cameraStreamIndex);
        // Determine the number of available strategies and pick a specific one
        int s, t, nStrategies;
        if (depth == 0) {
            nStrategies = 1;
            s = 0;
            t = 2;
        } else {
            nStrategies = depth + 2;
            s = Math.min((int)(sampler.Get1D() * nStrategies), nStrategies - 1);
            t = nStrategies - s;
        }

        // Generate a camera subpath with exactly _t_ vertices
        Vertex[] cameraVertices = new Vertex[t];
        Bounds2f sampleBounds = new Bounds2f(camera.film.GetSampleBounds());
        pRaster[0] = sampleBounds.Lerp(sampler.Get2D());
        if (Vertex.GenerateCameraSubpath(scene, sampler, t, camera, pRaster[0], cameraVertices) != t)
        return new Spectrum(0);

        // Generate a light subpath with exactly _s_ vertices
        sampler.StartStream(lightStreamIndex);
        Vertex[] lightVertices = new Vertex[s];
        if (Vertex.GenerateLightSubpath(scene, sampler, s, cameraVertices[0].time(), lightDistr, lightToIndex, lightVertices) != s)
        return new Spectrum(0);

        // Execute connection strategy and return the radiance estimate
        sampler.StartStream(connectionStreamIndex);
        return Vertex.ConnectBDPT(scene, lightVertices, cameraVertices, s, t, lightDistr, lightToIndex, camera, sampler, pRaster, null).scale(nStrategies);
    }

    public static MLTIntegrator Create(ParamSet params, Camera camera) {
        int maxDepth = params.FindOneInt("maxdepth", 5);
        int nBootstrap = params.FindOneInt("bootstrapsamples", 100000);
        int nChains = params.FindOneInt("chains", 1000);
        int mutationsPerPixel = params.FindOneInt("mutationsperpixel", 100);
        float largeStepProbability = params.FindOneFloat("largestepprobability", 0.3f);
        float sigma = params.FindOneFloat("sigma", .01f);
        if (Pbrt.options.QuickRender) {
            mutationsPerPixel = Math.max(1, mutationsPerPixel / 16);
            nBootstrap = Math.max(1, nBootstrap / 16);
        }
        return new MLTIntegrator(camera, maxDepth, nBootstrap, nChains, mutationsPerPixel, sigma, largeStepProbability);
    }

    private Camera camera;
    private final int maxDepth;
    private final int nBootstrap;
    private final int nChains;
    private final int mutationsPerPixel;
    private final float sigma, largeStepProbability;

    private static Stats.Percent acceptedMutationsPerc = new Stats.Percent("Integrator/Acceptance rate"); // acceptedMutations, totalMutations

}