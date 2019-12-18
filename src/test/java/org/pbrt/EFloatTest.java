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

import org.junit.Test;

import org.pbrt.core.EFloat;
import org.pbrt.core.Pbrt;
import org.pbrt.core.RNG;

public class EFloatTest {

    // Return an exponentially-distributed floating-point value.
    static EFloat getFloat(RNG rng) {
        float minExp = -6.0f;
        float maxExp = 6.0f;
        float logu = Pbrt.Lerp(rng.UniformFloat(), minExp, maxExp);
        float val = (float)Math.pow(10, logu);

        // Choose a random error bound.
        float err = 0;
        switch (rng.UniformUInt32(4)) {
            case 0:
                // no error
                break;
            case 1:
                {
                    // small typical/reasonable error
                    int ulpError = rng.UniformUInt32(1024);
                    float offset = Float.intBitsToFloat(Float.floatToIntBits(val) + ulpError);
                    err = Math.abs(offset - val);
                }
                    break;
            case 2:
                {
                    // bigger ~reasonable error
                    int ulpError = rng.UniformUInt32(1024 * 1024);
                    float offset = Float.intBitsToFloat(Float.floatToIntBits(val) + ulpError);
                    err = Math.abs(offset - val);
                }
                    break;
            case 3:
                err = (4 * rng.UniformFloat()) * Math.abs(val);
        }
        float sign = rng.UniformFloat() < 0.5f ? -1.0f : 1.0f;
        return new EFloat((float)(sign * val), err);
    }

    // Given an EFloat covering some range, choose a double-precision "precise"
    // value that is in the EFloat's range.
    static double getPrecise(EFloat ef, RNG rng) {
        switch (rng.UniformUInt32(3)) {
            // 2/3 of the time, pick a value that is right at the end of the range;
            // this is a maximally difficult / adversarial choice, so should help
            // ferret out any bugs.
            case 0:
                return ef.lowerBound();
            case 1:
                return ef.upperBound();
            case 2:
                // Otherwise choose a value uniformly inside the EFloat's range.
                float t = rng.UniformFloat();
                double p = (1 - t) * ef.lowerBound() + t * ef.upperBound();
                if (p > ef.upperBound()) p = ef.upperBound();
                if (p < ef.lowerBound()) p = ef.lowerBound();
                return p;
        }
        return (double)ef.asFloat();  // NOTREACHED
    }

    static final int kEFloatIters = 1000000;

    @Test
    public void testAbs() {
        for (int trial = 0; trial < kEFloatIters; ++trial) {
            RNG rng = new RNG(trial);

            EFloat ef = getFloat(rng);
            double precise = getPrecise(ef, rng);

            EFloat efResult = EFloat.abs(ef);
            double preciseResult = Math.abs(precise);

            assertTrue(preciseResult >= efResult.lowerBound());
            assertTrue(preciseResult <= efResult.upperBound());
        }
    }

    @Test
    public void testSqrt() {
        for (int trial = 0; trial < kEFloatIters; ++trial) {
            RNG rng = new RNG(trial);

            EFloat ef = getFloat(rng);
            double precise = getPrecise(ef, rng);

            // If the error starts to get too big such that the interval is
            // relatively close to zero w.r.t. the center value, we can't
            // compute error bounds for sqrt; skip these.
            if (ef.getAbsoluteError() > 0.25f * Math.abs(ef.lowerBound())) continue;

            EFloat efResult = EFloat.sqrt(EFloat.abs(ef));
            double preciseResult = Math.sqrt(Math.abs(precise));

            assertTrue(preciseResult >= efResult.lowerBound());
            assertTrue(preciseResult <= efResult.upperBound());
        }
    }

    @Test
    public void testAdd() {
        for (int trial = 0; trial < kEFloatIters; ++trial) {
            RNG rng = new RNG(trial);

            EFloat[] ef = {getFloat(rng), getFloat(rng)};
            double[] precise = {getPrecise(ef[0], rng), getPrecise(ef[1], rng)};

            EFloat efResult = ef[0].add(ef[1]);
            float preciseResult = (float)(precise[0] + precise[1]);

            assertTrue(preciseResult >= efResult.lowerBound());
            assertTrue(preciseResult <= efResult.upperBound());
        }
    }

    @Test
    public void testSub() {
        for (int trial = 0; trial < kEFloatIters; ++trial) {
            RNG rng = new RNG(trial);

            EFloat[] ef = {getFloat(rng), getFloat(rng)};
            double[] precise = {getPrecise(ef[0], rng), getPrecise(ef[1], rng)};

            EFloat efResult = ef[0].subtract(ef[1]);
            float preciseResult = (float)(precise[0] - precise[1]);

            assertTrue(preciseResult >= efResult.lowerBound());
            assertTrue(preciseResult <= efResult.upperBound());
        }
    }

    @Test
    public void testMul() {
        for (int trial = 0; trial < kEFloatIters; ++trial) {
            RNG rng = new RNG(trial);

            EFloat[] ef = {getFloat(rng), getFloat(rng)};
            double[] precise = {getPrecise(ef[0], rng), getPrecise(ef[1], rng)};

            EFloat efResult = ef[0].multiply(ef[1]);
            float preciseResult = (float)(precise[0] * precise[1]);

            assertTrue(preciseResult >= efResult.lowerBound());
            assertTrue(preciseResult <= efResult.upperBound());
        }
    }

    @Test
    public void testDiv() {
        for (int trial = 0; trial < kEFloatIters; ++trial) {
            RNG rng = new RNG(trial);

            EFloat[] ef = {getFloat(rng), getFloat(rng)};
            double[] precise = {getPrecise(ef[0], rng), getPrecise(ef[1], rng)};

            // As with sqrt, things get messy if the denominator's interval
            // straddles zero or is too close to zero w.r.t. its center value.
            if (ef[1].lowerBound() * ef[1].upperBound() < 0.0f || ef[1].getAbsoluteError() > 0.25f * Math.abs(ef[1].lowerBound()))
                continue;

            EFloat efResult = ef[0].divide(ef[1]);
            float preciseResult = (float)(precise[0] / precise[1]);

            assertTrue(preciseResult >= efResult.lowerBound());
            assertTrue(preciseResult <= efResult.upperBound());
        }
    }
    
}
