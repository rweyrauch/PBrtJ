/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.tests

import org.pbrt.core.HenyeyGreenstein
import org.pbrt.core.Pbrt
import org.pbrt.core.Point2f
import org.pbrt.core.RNG
import org.pbrt.core.Sampling
import org.pbrt.core.Vector3f

class HgTest  extends GroovyTestCase {

    void testHenyeyGreensteinSamplingMatch() {
        RNG rng = new RNG(0)
        for (float g = -0.75f; g <= 0.75f; g += 0.25f) {
            HenyeyGreenstein hg = new HenyeyGreenstein(g)
            for (int i = 0; i < 100; ++i) {
                Vector3f wo = Sampling.UniformSampleSphere(new Point2f(rng.UniformFloat(), rng.UniformFloat()))
                Point2f u = new Point2f(rng.UniformFloat(), rng.UniformFloat())
                def sampleP = hg.Sample_p(wo, u)
                Vector3f wi = sampleP.wi
                float p0 = sampleP.phase

                // Phase function is normalized, and the sampling method should be
                // exact.
                assertTrue(Pbrt.AlmostEqual(p0, hg.p(wo, wi), 1e-4f))
            }
        }
    }

    void testHenyeyGreensteinSamplingOrientationForward() {
        RNG rng = new RNG(0)

        HenyeyGreenstein hg = new HenyeyGreenstein(0.95f)
        Vector3f wo = new Vector3f(-1, 0, 0)
        int nForward = 0, nBackward = 0
        for (int i = 0; i < 100; ++i) {
            Point2f u = new Point2f(rng.UniformFloat(), rng.UniformFloat())
            def sampleP = hg.Sample_p(wo, u)
            Vector3f wi = sampleP.wi
            if (wi.x > 0)
                ++nForward
            else
                ++nBackward
        }
        // With g = 0.95, almost all of the samples should have wi.x > 0.
        assertTrue(nForward >= 10 * nBackward)
    }

    void testHenyeyGreensteinSamplingOrientationBackward() {
        RNG rng = new RNG(0)

        HenyeyGreenstein hg = new HenyeyGreenstein(-0.95f)
        Vector3f wo = new Vector3f(-1, 0, 0)
        int nForward = 0, nBackward = 0
        for (int i = 0; i < 100; ++i) {
            Point2f u = new Point2f(rng.UniformFloat(), rng.UniformFloat())
            def sampleP = hg.Sample_p(wo, u)
            Vector3f wi = sampleP.wi
            if (wi.x > 0)
                ++nForward
            else
                ++nBackward
        }
        // With g = -0.95, almost all of the samples should have wi.x < 0.
        assertTrue(nBackward >= 10 * nForward)
    }

    void testHenyeyGreensteinNormalized() {
        RNG rng = new RNG(0)
        for (float g = -0.75f; g <= 0.75f; g += 0.25f) {
            HenyeyGreenstein hg = new HenyeyGreenstein(g)
            Vector3f wo = Sampling.UniformSampleSphere(new Point2f(rng.UniformFloat(), rng.UniformFloat()))
            float sum = 0
            int nSamples = 100000
            for (int i = 0; i < nSamples; ++i) {
                Vector3f wi = Sampling.UniformSampleSphere(new Point2f(rng.UniformFloat(), rng.UniformFloat()))
                sum += hg.p(wo, wi)
            }
            // Phase function should integrate to 1/4pi.
            assertTrue(Pbrt.AlmostEqual(sum / nSamples, 1.0f / (4.0 * Pbrt.Pi), 1e-3f))
        }
    }
}
