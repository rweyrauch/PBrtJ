
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class LowDiscrepancy {

    public static int ReverseBits32(int n) {
        n = (n << 16) | (n >> 16);
        n = ((n & 0x00ff00ff) << 8) | ((n & 0xff00ff00) >> 8);
        n = ((n & 0x0f0f0f0f) << 4) | ((n & 0xf0f0f0f0) >> 4);
        n = ((n & 0x33333333) << 2) | ((n & 0xcccccccc) >> 2);
        n = ((n & 0x55555555) << 1) | ((n & 0xaaaaaaaa) >> 1);
        return n;
    }

    public static long ReverseBits64(long n) {
        long n0 = ReverseBits32((int)n);
        long n1 = ReverseBits32((int)(n >> 32));
        return (n0 << 32) | n1;
    }

    private static float RadicalInverseSpecialized(int base, long a) {
        float invBase = 1 / (float)base;
        long reversedDigits = 0;
        float invBaseN = 1;
        while (a != 0) {
            long next = a / base;
            long digit = a - next * base;
            reversedDigits = reversedDigits * base + digit;
            invBaseN *= invBase;
            a = next;
        }
        assert (reversedDigits * invBaseN < 1.00001);
        return Math.min(reversedDigits * invBaseN, Pbrt.OneMinusEpsilon);
    }

    public static float RadicalInverse(int baseIndex, long a) {
        switch (baseIndex) {
            case 0:
                return (float)ReverseBits64(a) * 0x1p-64f;
            default:
                return RadicalInverseSpecialized(baseIndex, a);
        }
    }
}