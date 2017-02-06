/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

import java.util.function.Predicate;

public class Pbrt
{
    public static Options options;

    public static float MachineEpsilon() {
        return Math.ulp(1.0f) * 0.5f;
    }
    public static final float Infinity = Float.MAX_VALUE;
    public static final float ShadowEpsilon = 0.0001f;
    public static final float OneMinusEpsilon = 0x1.fffffep-1f;
    public static float gamma(int n) {
        return (n * MachineEpsilon()) / (1 - n * MachineEpsilon());
    }

    public static float GammaCorrect(float value) {
        if (value <= 0.0031308f) return 12.92f * value;
        return 1.055f * (float)Math.pow(value, (1.0 / 2.4)) - 0.055f;
    }

    public static float InverseGammaCorrect(float value) {
        if (value <= 0.04045f) return value * 1.f / 12.92f;
        return (float)Math.pow((value + 0.055) * 1.0 / 1.055, 2.4);
    }

    public static float Clamp(float v, float low, float high) {
        if (v < low) return low;
        else if (v > high) return high;
        else return v;
    }
    public static int Clamp(int v, int low, int high) {
        if (v < low) return low;
        else if (v > high) return high;
        else return v;
    }

    public static int FindInterval(int size, Predicate<Integer> pred) {
        int first = 0, len = size;
        while (len > 0) {
            int half = len >> 1, middle = first + half;
            // Bisect range based on value of _pred_ at _middle_
            if (pred.test(middle)) {
                first = middle + 1;
                len -= half + 1;
            } else
                len = half;
        }
        return Clamp(first - 1, 0, size - 2);
    }

    public static float Lerp(float t, float v1, float v2) { return (1 - t) * v1 + t * v2; }
}