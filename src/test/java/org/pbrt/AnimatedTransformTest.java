/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */
package org.pbrt;

import static org.junit.Assert.assertTrue;
import static org.pbrt.core.Sampling.UniformSampleSphere;

import org.junit.Test;

import org.pbrt.core.*;

public class AnimatedTransformTest {

    interface GenRandom {
        float generate();
    }

    static Transform RandomTransform(RNG rng) {
        Transform t = new Transform();
        GenRandom r = () -> { return -10 + 20 * rng.UniformFloat(); };

        for (int i = 0; i < 10; ++i) {
            switch ((int)rng.UniformUInt32(3)) {
                case 0:
                    t = t.concatenate(Transform.Scale(Math.abs(r.generate()), Math.abs(r.generate()), Math.abs(r.generate())));
                    break;
                case 1:
                    t = t.concatenate(Transform.Translate(new Vector3f(r.generate(), r.generate(), r.generate())));
                    break;
                case 2:
                    t = t.concatenate(Transform.Rotate(r.generate() * 20,
                            UniformSampleSphere(new Point2f(rng.UniformFloat(), rng.UniformFloat()))));
                    break;
            }
        }
        return t;
    }

    @Test
    public void testRandoms() {
        final RNG rng = new RNG();
        GenRandom r = () -> { return -10 + 20 * rng.UniformFloat(); };

        for (int i = 0; i < 200; ++i) {
            // Generate a pair of random transformation matrices.
            Transform t0 = RandomTransform(rng);
            Transform t1 = RandomTransform(rng);
            AnimatedTransform at = new AnimatedTransform(t0, 0, t1, 1);

            for (int j = 0; j < 5; ++j) {
                // Generate a random bounding box and find the bounds of its motion.
                Bounds3f bounds = new Bounds3f(new Point3f(r.generate(), r.generate(), r.generate()),
                        new Point3f(r.generate(), r.generate(), r.generate()));
                Bounds3f motionBounds = at.MotionBounds(bounds);

                for (float t = 0; t <= 1f; t += 1e-3f * rng.UniformFloat()) {
                    // Now, interpolate the transformations at a bunch of times
                    // along the time range and then transform the bounding box
                    // with the result.
                    Transform tr = at.Interpolate(t);
                    Bounds3f tb = tr.xform(bounds);

                    // Add a little slop to allow for floating-point round-off
                    // error in computing the motion extrema times.
                    tb.pMin.increment(tb.Diagonal().scale(1e-4f));
                    tb.pMax.increment(tb.Diagonal().scale(-1e-4f));

                    // Now, the transformed bounds should be inside the motion
                    // bounds.
                    assertTrue(tb.pMin.x >= motionBounds.pMin.x);
                    assertTrue(tb.pMax.x <= motionBounds.pMax.x);
                    assertTrue(tb.pMin.y >= motionBounds.pMin.y);
                    assertTrue(tb.pMax.y <= motionBounds.pMax.y);
                    assertTrue(tb.pMin.z >= motionBounds.pMin.z);
                    assertTrue(tb.pMax.z <= motionBounds.pMax.z);
                }
            }
        }
    }

}
