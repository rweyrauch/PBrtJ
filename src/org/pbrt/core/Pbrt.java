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