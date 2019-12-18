/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

import java.util.Random;

public class RNG {

    private Random random;

    public static final double DoubleOneMinusEpsilon = 0x1.fffffffffffffp-1;
    public static final float FloatOneMinusEpsilon = 0x1.fffffep-1f;

    private static final long PCG32_DEFAULT_STATE = 0x853c49e6748fea9bL;

    // RNG Public Methods
    public RNG() {
        this.random = new Random(PCG32_DEFAULT_STATE);
    }
    public RNG(long sequenceIndex) {
        this.random = new Random(sequenceIndex);
    }
    public void SetSequence(long sequenceIndex) {
        random.setSeed(sequenceIndex);
    }
    public long UniformUInt32() {
        return Integer.toUnsignedLong(random.nextInt());
    }
    public long UniformUInt32(int b) {
        return Integer.toUnsignedLong(random.nextInt(b));
    }
    public float UniformFloat() {
        return Math.min(Pbrt.OneMinusEpsilon, random.nextFloat());
    }
}