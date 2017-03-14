/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.tests

import org.pbrt.core.AtomicFloat
import org.pbrt.core.Pbrt
import org.pbrt.core.RNG

import java.util.function.Predicate

class PbrtTest extends GroovyTestCase {

    void testLog2() {
        for (int i = 0; i < 32; ++i) {
            int ui = 1 << i
            assertEquals(i, Pbrt.Log2Int(ui))
            assertEquals(i, Pbrt.Log2Int((long)ui))
        }

        for (int i = 1; i < 31; ++i) {
            int ui = 1 << i
            assertEquals(i, Pbrt.Log2Int(ui + 1))
            assertEquals(i, Pbrt.Log2Int((long)(ui + 1)))
        }

        for (int i = 0; i < 64; ++i) {
            long ui = 1L << i
            assertEquals(i, Pbrt.Log2Int(ui))
        }

        for (int i = 1; i < 64; ++i) {
            long ui = 1L << i
            assertEquals(i, Pbrt.Log2Int(ui + 1))
        }
    }

    void testPow2() {
        for (int i = 0; i < 32; ++i) {
            int ui = 1 << i
            assertEquals(true, Pbrt.IsPowerOf2(ui))
            if (ui > 1) {
                assertEquals(false, Pbrt.IsPowerOf2(ui + 1))
            }
            if (ui > 2) {
                assertEquals(false, Pbrt.IsPowerOf2(ui - 1))
            }
        }
    }

    void testCountTrailing() {
        for (int i = 0; i < 32; ++i) {
            int ui = 1 << i
            assertEquals(i, Integer.numberOfTrailingZeros(ui))
        }
    }

    void testRoundUpPow2() {
        assertEquals(Pbrt.RoundUpPow2(7), 8)
        for (int i = 1; i < (1 << 24); ++i)
            if (Pbrt.IsPowerOf2(i))
                assertEquals(Pbrt.RoundUpPow2(i), i)
            else
                assertEquals(Pbrt.RoundUpPow2(i), 1 << (Pbrt.Log2Int(i) + 1))

        for (long i = 1; i < (1 << 24); ++i)
            if (Pbrt.IsPowerOf2(i))
                assertEquals(Pbrt.RoundUpPow2(i), i)
            else
                assertEquals(Pbrt.RoundUpPow2(i), 1 << (Pbrt.Log2Int(i) + 1))

        for (int i = 0; i < 30; ++i) {
            int v = 1 << i
            assertEquals(Pbrt.RoundUpPow2(v), v)
            if (v > 2) assertEquals(Pbrt.RoundUpPow2(v - 1), v)
            assertEquals(Pbrt.RoundUpPow2(v + 1), 2 * v)
        }

        for (int i = 0; i < 62; ++i) {
            long v = 1L << i
            assertEquals(Pbrt.RoundUpPow2(v), v)
            if (v > 2) assertEquals(Pbrt.RoundUpPow2(v - 1L), v)
            assertEquals(Pbrt.RoundUpPow2(v + 1L), 2L * v)
        }
    }

    void testFindInterval() {
        float[] a = [0.0f, 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f]

        // Check clamping for out of range
        Predicate<Integer> p1 = [test:{int index -> a[index] <= -1}] as Predicate
        Predicate<Integer> p2 = [test:{int index -> a[index] <= 100}] as Predicate
        assertEquals(0, Pbrt.FindInterval(a.length, p1))
        assertEquals(a.size() - 2, Pbrt.FindInterval(a.length, p2))

        Predicate<Integer> p3 = [test:{int index, int i -> a[index] <= i}] as Predicate
        Predicate<Integer> p4 = [test:{int index, int i -> a[index] <= i + 0.5f}] as Predicate
        Predicate<Integer> p5 = [test:{int index, int i -> a[index] <= i - 0.5f}] as Predicate
        for (int i = 0; i < a.length - 1; ++i) {
            assertEquals(i, Pbrt.FindInterval(a.length, p3))
            assertEquals(i, Pbrt.FindInterval(a.length, p4))
            if (i > 0)
                assertEquals(i - 1, Pbrt.FindInterval(a.length, p5))
        }
    }

    float GetFloat(RNG rng) {
        float f = Float.intBitsToFloat(rng.UniformInt32())
        while (Float.isNaN(f)) {
            f = Float.intBitsToFloat(rng.UniformInt32())
        };
        return f
    }

    double GetDouble(RNG rng) {
        double d = Double.longBitsToDouble((long)(rng.UniformInt32()) | ((long)(rng.UniformInt32()) << 32))
        while (Double.isNaN(d)) {
            d = Double.longBitsToDouble((long)(rng.UniformInt32()) | ((long)(rng.UniformInt32()) << 32))
        };
        return d
    }

    void testNextUpDownFloat() {
        assertTrue(Pbrt.NextFloatUp(-0.0f) > 0.0f)
        assertTrue(Pbrt.NextFloatDown(0.0f) < 0.0f)

        assertEquals(Pbrt.NextFloatUp(Pbrt.Infinity), Pbrt.Infinity)
        assertTrue(Pbrt.NextFloatDown(Pbrt.Infinity) < Pbrt.Infinity)

        assertEquals(Pbrt.NextFloatDown(-Pbrt.Infinity), -Pbrt.Infinity)
        assertTrue(Pbrt.NextFloatUp(-Pbrt.Infinity) > -Pbrt.Infinity)

        RNG rng = new RNG(0)
        for (int i = 0; i < 100000; ++i) {
            float f = GetFloat(rng)
            if (Float.isInfinite(f)) continue;

            assertEquals((float)Math.nextAfter(f, Pbrt.Infinity), Pbrt.NextFloatUp(f))
            assertEquals((float)Math.nextAfter(f, -Pbrt.Infinity), Pbrt.NextFloatDown(f))
        }
    }

    void testNextUpDownDouble() {
        assertTrue(Pbrt.NextFloatUp(-0.0, 1) > 0.0)
        assertTrue(Pbrt.NextFloatDown(0.0, 1) < 0.0)

        assertEquals(Pbrt.NextFloatUp((double)Pbrt.Infinity, 1), (double)Pbrt.Infinity)
        assertTrue(Pbrt.NextFloatDown((double)Pbrt.Infinity, 1) < (double)Pbrt.Infinity)

        assertEquals(Pbrt.NextFloatDown(-(double)Pbrt.Infinity, 1), -(double)Pbrt.Infinity)
        assertTrue(Pbrt.NextFloatUp(-(double)Pbrt.Infinity, 1) > -(double)Pbrt.Infinity)

        RNG rng = new RNG(3)
        for (int i = 0; i < 100000; ++i) {
            double d = GetDouble(rng)
            if (Double.isInfinite(d)) continue

            assertEquals(Math.nextAfter(d, (double)Pbrt.Infinity), Pbrt.NextFloatUp(d, 1))
            assertEquals(Math.nextAfter(d, -(double)Pbrt.Infinity), Pbrt.NextFloatDown(d, 1))
        }
    }

    void testFloatBits() {
        RNG rng = new RNG(1)
        for (int i = 0; i < 100000; ++i) {
            int ui = rng.UniformInt32()
            float f = Float.intBitsToFloat(ui)
            if (Float.isNaN(f)) continue

            assertEquals(ui, Float.floatToIntBits(f))
        }
    }

    void testDoubleBits() {
        RNG rng = new RNG(2)
        for (int i = 0; i < 100000; ++i) {
            long ui = ((long)(rng.UniformInt32()) | ((long)(rng.UniformInt32()) << 32))
            double f = Double.longBitsToDouble(ui)

            if (Double.isNaN(f)) continue

            assertEquals(ui, Double.doubleToLongBits(f))
        }
    }

    void testAtomicFloat() {
        AtomicFloat af = new AtomicFloat(0)
        float f = 0
        assertEquals(f, af.get())
        af.add(1.0251f)
        f += 1.0251f
        assertEquals(f, af.get())
        af.add(2.0f)
        f += 2.0f
        assertEquals(f, af.get())
    }
}
