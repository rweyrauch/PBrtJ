/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.tests

import com.intellij.codeInspection.bytecodeAnalysis.Direction
import org.pbrt.core.ArithmeticOps
import org.pbrt.core.Distribution1D
import org.pbrt.core.LowDiscrepancy
import org.pbrt.core.Pbrt
import org.pbrt.core.Point2f
import org.pbrt.core.Point2i
import org.pbrt.core.RNG
import org.pbrt.core.Sampling
import org.pbrt.core.Vector2f
import org.pbrt.samplers.MaxMinDistSampler

class SamplingTest extends GroovyTestCase {

    void testRadicalInverse() {
        for (int a = 0; a < 1024; ++a) {
            assertEquals(LowDiscrepancy.ReverseBits32(a) * 2.3283064365386963e-10f, LowDiscrepancy.RadicalInverse(0, a))
        }
    }

    void testScrambledRadicalInverse() {
        for (int dim = 0; dim < 128; ++dim) {
            RNG rng = new RNG(dim)
            // Random permutation table
            final int base = LowDiscrepancy.Primes[dim]

            Short[] perm = new Short[base]
            for (int i = 0; i < base; ++i) perm[i] = (base - 1 - i)
            Sampling.Shuffle(perm, 0, perm.length, 1, rng)

            int[] indices = [0, 1, 2, 1151, 32351, 4363211, 681122]
            for (int index : indices) {
                // First, compare to the pbrt-v2 implementation.
                float val = 0
                float invBase = 1.0f / base
                float invBi = invBase
                int n = index
                while (n > 0) {
                    int d_i = perm[n % base]
                    val += d_i * invBi
                    n *= invBase
                    invBi *= invBase
                }
                // For the case where the permutation table permutes the digit 0
                // to
                // another digit, account for the infinite sequence of that
                // digit
                // trailing at the end of the radical inverse value.
                val += perm[0] * base / (base - 1.0f) * invBi

                assertTrue(Pbrt.AlmostEqual(val, LowDiscrepancy.ScrambledRadicalInverse(dim, index, perm), 1e-5f))


                // Now also check against a totally naive "loop over all the
                // bits in
                // the index" approach, regardless of hitting zero...
                val = 0.0f
                invBase = 1.0f / base
                invBi = invBase

                int a = index
                for (int i = 0; i < 32; ++i) {
                    int d_i = perm[a % base]
                    a /= base
                    val += d_i * invBi
                    invBi *= invBase

                    assertTrue(Pbrt.AlmostEqual(val, LowDiscrepancy.ScrambledRadicalInverse(dim, index, perm), 1e-5f))
                }
            }
        }
    }

    void testGeneratorMatrix() {
        int[] C = new int[32]
        int[] Crev = new int[32]
        // Identity matrix, column-wise
        for (int i = 0; i < 32; ++i) {
            C[i] = 1 << i
            Crev[i] = LowDiscrepancy.ReverseBits32(C[i])
        }

        for (int a = 0; a < 128; ++a) {
            // Make sure identity generator matrix matches van der Corput
            assertEquals(a, LowDiscrepancy.MultiplyGenerator(C, a))
            assertEquals(LowDiscrepancy.RadicalInverse(0, a),
                    LowDiscrepancy.ReverseBits32(LowDiscrepancy.MultiplyGenerator(C, a)) * 2.3283064365386963e-10f)
            assertEquals(LowDiscrepancy.RadicalInverse(0, a), LowDiscrepancy.SampleGeneratorMatrix(Crev, a, 0))
        }

        // Random / goofball generator matrix
        RNG rng = new RNG(0);
        for (int i = 0; i < 32; ++i) {
            C[i] = rng.UniformInt32()
            Crev[i] = LowDiscrepancy.ReverseBits32(C[i])
        }
        for (int a = 0; a < 1024; ++a) {
            assertEquals(LowDiscrepancy.ReverseBits32(LowDiscrepancy.MultiplyGenerator(C, a)),
                    LowDiscrepancy.MultiplyGenerator(Crev, a));
        }
    }

    void testGrayCodeSample() {
        int[] C = new int[32]
        // Identity matrix, column-wise
        for (int i = 0; i < 32; ++i) C[i] = 1 << i

        Float[] v = new Float[64]
        for (int i = 0; i < v.length; i++) v[i] = 0

        v = LowDiscrepancy.GrayCodeSample(C, v.length, 0, v)

        for (int a = 0; a < v.length; ++a) {
            float u = LowDiscrepancy.MultiplyGenerator(C, a) * 2.3283064365386963e-10f
            boolean found = false
            for (float vv : v) {
                if (vv == u) {
                    found = true
                    break
                }
            }
            assertTrue(found)
        }
    }

    void testSobol() {
        // Check that float and double variants match (as float values).
        for (int i = 0; i < 256; ++i) {
            for (int dim = 0; dim < 100; ++dim) {
                assertEquals(LowDiscrepancy.SobolSampleFloat(i, dim, 0), (float)LowDiscrepancy.SobolSampleDouble(i, dim, 0))
            }
        }

        // Make sure first dimension is the regular base 2 radical inverse
        for (int i = 0; i < 8192; ++i) {
            assertEquals(LowDiscrepancy.SobolSampleFloat(i, 0, 0), LowDiscrepancy.ReverseBits32(i) * 2.3283064365386963e-10f)
        }
    }
/*
    // Make sure samplers that are supposed to generate a single sample in
    // each of the elementary intervals actually do so.
    // TODO: check Halton (where the elementary intervals are (2^i, 3^j)).
    void testElementaryIntervals() {
        auto checkSampler = [](const char *name, std::unique_ptr<Sampler> sampler,
                int logSamples) {
                // Get all of the samples for a pixel.
            sampler->StartPixel(Point2i(0, 0));
                std::vector<Point2f> samples;
                do {
                samples.push_back(sampler->Get2D());
            } while (sampler->StartNextSample());

                for (int i = 0; i <= logSamples; ++i) {
                    // Check one set of elementary intervals: number of intervals
                    // in each dimension.
                    int nx = 1 << i, ny = 1 << (logSamples - i);

                    std::vector<int> count(1 << logSamples, 0);
                    for (const Point2f &s : samples) {
                        // Map the sample to an interval
                        Float x = nx * s.x, y = ny * s.y;
                        EXPECT_GE(x, 0);
                        EXPECT_LT(x, nx);
                        EXPECT_GE(y, 0);
                        EXPECT_LT(y, ny);
                        int index = (int)std::floor(y) * nx + (int)std::floor(x);
                        EXPECT_GE(index, 0);
                        EXPECT_LT(index, count.size());

                        // This should be the first time a sample has landed in its
                        // interval.
                        assertEquals(0, count[index]) << "Sampler " << name;
                        ++count[index];
                    }
                }
        };

        for (int logSamples = 2; logSamples <= 10; ++logSamples) {
            checkSampler(
                    "MaxMinDistSampler",
                    std::unique_ptr<Sampler>(new MaxMinDistSampler(1 << logSamples, 2)),
                    logSamples);
            checkSampler("ZeroTwoSequenceSampler",
                    std::unique_ptr<Sampler>(
                    new ZeroTwoSequenceSampler(1 << logSamples, 2)),
                    logSamples);
            checkSampler("Sobol", std::unique_ptr<Sampler>(new SobolSampler(
                    1 << logSamples,
                    Bounds2i(Point2i(0, 0), Point2i(10, 10)))),
                    logSamples);
        }
    }
*/

    // Distance with toroidal topology
    static float pointDist(Point2f p0, Point2f p1) {
        Vector2f d = Vector2f.Abs(p1.subtract(p0))
        if (d.x > 0.5f) d.x = 1.0f - d.x
        if (d.y > 0.5f) d.y = 1.0f - d.y
        return d.Length()
    }

    void testMaxMinDist() {
        // We use a silly O(n^2) distance check below, so don't go all the way up
        // to 2^16 samples.
        for (int logSamples = 2; logSamples <= 10; ++logSamples) {
            // Store a pixel's worth of samples in the vector s.
            MaxMinDistSampler mm = new MaxMinDistSampler(1 << logSamples, 2)
            mm.StartPixel(new Point2i(0, 0))
            ArrayList<Point2f> s = new ArrayList<>()
            s.add(mm.Get2D())
            while (mm.StartNextSample()) {
                s.add(mm.Get2D())
            }

            float minDist = Pbrt.Infinity
            for (int i = 0; i < s.size(); ++i) {
                for (int j = 0; j < s.size(); ++j) {
                    if (i == j) continue;
                    minDist = Math.min(minDist, pointDist(s[i], s[j]))
                }
            }

            // Expected minimum distances from Gruenschloss et al.'s paper.
            float[] expectedMinDist = [
                0.0f, /* not checked */
                0.0f, /* not checked */
                0.35355f, 0.35355f, 0.22534f, 0.16829f, 0.11267f,
                0.07812f, 0.05644f, 0.03906f, 0.02816f, 0.01953f,
                0.01408f, 0.00975f, 0.00704f, 0.00486f, 0.00352f,
            ]

            // Increase the tolerance by a small slop factor.
            assertTrue(minDist > 0.99f * expectedMinDist[logSamples])
        }
    }

    void testDistribution1DDiscreet() {
        // Carefully chosen distribution so that transitions line up with
        // (inverse) powers of 2.
        float[] func = [0, 1.0f, 0.0f, 3.0f]
        Distribution1D dist = new Distribution1D(func)
        assertEquals(4, dist.Count());

        assertEquals(0.0f, dist.DiscretePDF(0))
        assertEquals(0.25f, dist.DiscretePDF(1))
        assertEquals(0.0f, dist.DiscretePDF(2))
        assertEquals(0.75f, dist.DiscretePDF(3))

        Distribution1D.DiscreteSample ds = new Distribution1D.DiscreteSample()
        ds = dist.SampleDiscrete(0.0f)
        assertEquals(1, ds.offset)
        assertEquals(0.25f, ds.pdf)
        ds = dist.SampleDiscrete(0.125f)
        assertEquals(1, ds.offset)
        assertEquals(0.25f, ds.pdf)
        assertEquals(0.5f, ds.uRemapped)
        ds = dist.SampleDiscrete(0.24999f)
        assertEquals(1, ds.offset)
        assertEquals(0.25f, ds.pdf)
        ds = dist.SampleDiscrete(0.250001f)
        assertEquals(3, ds.offset)
        assertEquals(0.75f, ds.pdf)
        ds = dist.SampleDiscrete(0.625f)
        assertEquals(3, ds.offset)
        assertEquals(0.75f, ds.pdf)
        assertEquals(0.5f, ds.uRemapped)
        ds = dist.SampleDiscrete(Pbrt.OneMinusEpsilon)
        assertEquals(3, ds.offset)
        assertEquals(0.75f, ds.pdf)
        ds = dist.SampleDiscrete(1.0f)
        assertEquals(3, ds.offset)
        assertEquals(0.75f, ds.pdf)

        // Compute the interval to test over.
        float u = 0.25f, uMax = 0.25f
        for (int i = 0; i < 20; ++i) {
            u = Pbrt.NextFloatDown(u)
            uMax = Pbrt.NextFloatUp(uMax)
        }
        // We should get a stream of hits in the first interval, up until the
        // cross-over point at 0.25 (plus/minus fp slop).
        for (; u < uMax; u = Pbrt.NextFloatUp(u)) {
            ds = dist.SampleDiscrete(u)
            if (ds.offset == 3) break
            assertEquals(1, ds.offset)
        }
        assertTrue(u < uMax)
        // And then all the rest should be in the third interval
        for (; u <= uMax; u = Pbrt.NextFloatUp(u)) {
            ds = dist.SampleDiscrete(u)
            assertEquals(3, ds.offset)
        }
    }

    void testDistribution1DContinuous() {
        float[] func = [1, 1, 2, 4, 8]
        Distribution1D dist = new Distribution1D(func)
        assertEquals(5, dist.Count())

        Distribution1D.ContSample cs = dist.SampleContinuous(0.0f)
        assertEquals(0.0f, cs.sample)
        assertEquals(dist.Count() * 1.0f / 16.0f, cs.pdf)
        assertEquals(0, cs.offset)

        // Right at the boundary between the 4 and the 8 segments.
        cs = dist.SampleContinuous(0.5f)
        assertEquals(0.8f, cs.sample)

        // Middle of the 8 segment
        cs = dist.SampleContinuous(0.75f)
        assertEquals(0.9f, cs.sample)
        assertEquals(dist.Count() * 8.0f / 16.0f, cs.pdf)
        assertEquals(4, cs.offset)

        cs = dist.SampleContinuous(0.0f)
        assertEquals(0.0, cs.sample)
        cs = dist.SampleContinuous(1.0f)
        assertEquals(1.0, cs.sample)
    }
}
