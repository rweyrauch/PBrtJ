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
    public static final float MaxFloat = Float.MAX_VALUE;
    public static final float Infinity = Float.POSITIVE_INFINITY;
    public static final float ShadowEpsilon = 0.0001f;
    public static final float OneMinusEpsilon = 0x1.fffffep-1f;
    public static final float Pi = (float)Math.PI;
    public static final float InvPi = (float)(1/Math.PI);
    public static final float Inv2Pi = (float)(1/(2*Math.PI));
    public static final float Inv4Pi = (float)(1/(4*Math.PI));
    public static final float PiOver2 = (float)(Math.PI/2);
    public static final float PiOver4 = (float)(Math.PI/4);
    public static final float Sqrt2 = (float)Math.sqrt(2);

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

    public static float Log2(float x) {
        float invLog2 = 1.442695040888963387004650940071f;
        return (float)Math.log(x) * invLog2;
    }

    public static boolean AlmostEqual(float v0, float v1, float tol) {
        return (Math.abs(v0-v1) < tol);
    }
    public static boolean AlmostEqual(double v0, double v1, double tol) {
        return (Math.abs(v0-v1) < tol);
    }

    public static int Log2Int(int v) {
        return (Integer.SIZE - 1) - Integer.numberOfLeadingZeros(v);
    }
    public static long Log2Int(long v) {
        return (Long.SIZE - 1) - Long.numberOfLeadingZeros(v);
    }

    public static int Mod(int a, int b) {
        int result = a - (a / b) * b;
        return (result < 0) ? result + b : result;
    }
    public static long Mod(long a, long b) {
        long result = a - (a / b) * b;
        return (result < 0) ? result + b : result;
    }

    public static float Mod(float a, float b) {
        return a % b;
    }

    public static boolean IsPowerOf2(int v) {
        return (v > 0) && ((v & (v - 1)) == 0);
    }
    public static boolean IsPowerOf2(long v) {
        return (v > 0) && ((v & (v - 1)) == 0);
    }

    public static int RoundUpPow2(int v) {
        v--;
        v |= v >> 1;
        v |= v >> 2;
        v |= v >> 4;
        v |= v >> 8;
        v |= v >> 16;
        return v + 1;
    }
    public static long RoundUpPow2(long v) {
        v--;
        v |= v >> 1;
        v |= v >> 2;
        v |= v >> 4;
        v |= v >> 8;
        v |= v >> 16;
        v |= v >> 32;
        return v + 1;
    }

    public static int CountTrailingZeros(int x) {
        return Integer.numberOfTrailingZeros(x);
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

    public static class QuadRes {
        public float t0, t1;
    }
    public static QuadRes Quadratic(float a, float b, float c) {
        // Find quadratic discriminant
        double discrim = (double)b * (double)b - 4 * (double)a * (double)c;
        if (discrim < 0) return null;
        double rootDiscrim = Math.sqrt(discrim);

        // Compute quadratic _t_ values
        double q;
        if (b < 0)
            q = -.5 * (b - rootDiscrim);
        else
            q = -.5 * (b + rootDiscrim);
        QuadRes res = new QuadRes();
        res.t0 = (float)q / a;
        res.t1 = c / (float)q;
        if (res.t0 > res.t1) {
            float temp = res.t0;
            res.t0 = res.t1;
            res.t1 = temp;
        }
        return res;
    }

    public static float NextFloatUp(float v) {
        // Handle infinity and negative zero for _NextFloatUp()_
        if (Float.isInfinite(v) && v > 0) return v;
        if (v == -0.f) v = 0.f;

        // Advance _v_ to next higher float
        int ui = Float.floatToIntBits(v);
        if (v >= 0) {
            ++ui;
        } else {
            --ui;
        }
        return Float.intBitsToFloat(ui);
    }

    public static float NextFloatDown(float v) {
    // Handle infinity and positive zero for _NextFloatDown()_
        if (Float.isInfinite(v) && v > 0) return v;
        if (v == 0.f) v = -0.f;
        int ui = Float.floatToIntBits(v);
        if (v > 0)
            --ui;
        else
            ++ui;
        return Float.intBitsToFloat(ui);
    }

    public static double NextFloatUp(double v, int delta) {
        if (Double.isInfinite(v) && v > 0.0) return v;
        if (v == -0.0) v = 0.0;
        long ui = Double.doubleToLongBits(v);
        if (v >= 0.0)
            ui += delta;
        else
            ui -= delta;
        return Double.longBitsToDouble(ui);
    }

    public static double NextFloatDown(double v, int delta) {
        if (Double.isInfinite(v) && v < 0.0) return v;
        if (v == 0.0) v = -0.0;
        long ui = Double.doubleToLongBits(v);
        if (v > 0.0)
            ui -= delta;
        else
            ui += delta;
        return Double.longBitsToDouble(ui);
    }

    public static float ErfInv(float x) {
        float w, p;
        x = Clamp(x, -.99999f, .99999f);
        w = -(float)Math.log((1 - x) * (1 + x));
        if (w < 5) {
            w = w - 2.5f;
            p = 2.81022636e-08f;
            p = 3.43273939e-07f + p * w;
            p = -3.5233877e-06f + p * w;
            p = -4.39150654e-06f + p * w;
            p = 0.00021858087f + p * w;
            p = -0.00125372503f + p * w;
            p = -0.00417768164f + p * w;
            p = 0.246640727f + p * w;
            p = 1.50140941f + p * w;
        } else {
            w = (float)Math.sqrt(w) - 3;
            p = -0.000200214257f;
            p = 0.000100950558f + p * w;
            p = 0.00134934322f + p * w;
            p = -0.00367342844f + p * w;
            p = 0.00573950773f + p * w;
            p = -0.0076224613f + p * w;
            p = 0.00943887047f + p * w;
            p = 1.00167406f + p * w;
            p = 2.83297682f + p * w;
        }
        return p * x;
    }

    public static float Erf(float x) {
        // constants
        final float a1 = 0.254829592f;
        final float a2 = -0.284496736f;
        final float a3 = 1.421413741f;
        final float a4 = -1.453152027f;
        final float a5 = 1.061405429f;
        final float p = 0.3275911f;

        // Save the sign of x
        int sign = 1;
        if (x < 0) sign = -1;
        x = Math.abs(x);

        // A&S formula 7.1.26
        float t = 1 / (1 + p * x);
        float y = 1 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * (float)Math.exp(-x * x);

        return sign * y;
    }

}