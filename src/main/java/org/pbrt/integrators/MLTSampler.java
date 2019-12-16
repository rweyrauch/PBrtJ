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
import org.pbrt.core.PBrtTLogger;

import java.util.ArrayList;

public class MLTSampler extends Sampler {

    public MLTSampler(int mutationsPerPixel, int rngSequenceIndex, float sigma, float largeStepProbability, int streamCount) {
        super(mutationsPerPixel);
        this.rng = new RNG(rngSequenceIndex);
        this.sigma = sigma;
        this.largeStepProbability = largeStepProbability;
        this.streamCount = streamCount;
    }

    @Override
    public float Get1D() {
        int index = GetNextIndex();
        EnsureReady(index);
        return X.get(index).value;
    }

    @Override
    public Point2f Get2D() {
        return new Point2f(Get1D(), Get1D());
    }

    @Override
    public Sampler Clone(int seed) {
        PBrtTLogger.Error("MLTSampler::Clone() is not implemented.");
        return null;
    }

    public void StartIteration() {
        currentIteration++;
        largeStep = rng.UniformFloat() < largeStepProbability;
    }

    public void Accept() {
        if (largeStep) lastLargeStepIteration = currentIteration;
    }
    public void Reject() {
        for (PrimarySample Xi : X) {
            if (Xi.lastModificationIteration == currentIteration) Xi.Restore();
        }
        --currentIteration;
    }
    public void StartStream(int index) {
        assert (index < streamCount);
        streamIndex = index;
        sampleIndex = 0;
    }

    public int GetNextIndex() { return streamIndex + streamCount * sampleIndex++; }

    private static class PrimarySample {
        public float value = 0;
        // PrimarySample Public Methods
        public void Backup() {
            valueBackup = value;
            modifyBackup = lastModificationIteration;
        }
        public void Restore() {
            value = valueBackup;
            lastModificationIteration = modifyBackup;
        }

        // PrimarySample Public Data
        public long lastModificationIteration = 0;
        public float valueBackup = 0;
        public long modifyBackup = 0;
    }

    private void EnsureReady(int index) {    // Enlarge _MLTSampler::X_ if necessary and get current $\VEC{X}_i$
        while (index >= X.size()) {
            X.add(new PrimarySample());
        }
        PrimarySample Xi = X.get(index);

        // Reset $\VEC{X}_i$ if a large step took place in the meantime
        if (Xi.lastModificationIteration < lastLargeStepIteration) {
            Xi.value = rng.UniformFloat();
            Xi.lastModificationIteration = lastLargeStepIteration;
        }

        // Apply remaining sequence of mutations to _sample_
        Xi.Backup();
        if (largeStep) {
            Xi.value = rng.UniformFloat();
        } else {
            long nSmall = currentIteration - Xi.lastModificationIteration;
            // Apply _nSmall_ small step mutations

            // Sample the standard normal distribution $N(0, 1)$
            float normalSample = Pbrt.Sqrt2 * Pbrt.ErfInv(2 * rng.UniformFloat() - 1);

            // Compute the effective standard deviation and apply perturbation to
            // $\VEC{X}_i$
            float effSigma = sigma * (float)Math.sqrt((float)nSmall);
            Xi.value += normalSample * effSigma;
            Xi.value -= Math.floor(Xi.value);
        }
        Xi.lastModificationIteration = currentIteration;
    }

    private RNG rng;
    private final float sigma, largeStepProbability;
    private final int streamCount;
    private ArrayList<PrimarySample> X;
    private long currentIteration = 0;
    private boolean largeStep = true;
    private long lastLargeStepIteration = 0;
    private int streamIndex, sampleIndex;
}