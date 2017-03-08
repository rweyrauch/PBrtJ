/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class RNG {
    // RNG Private Data
    private long state, inc;

    public static final double DoubleOneMinusEpsilon = 0x1.fffffffffffffp-1;
    public static final float FloatOneMinusEpsilon = 0x1.fffffep-1f;

    private static final long PCG32_DEFAULT_STATE = 0x853c49e6748fea9bL;
    private static final long PCG32_DEFAULT_STREAM = 0xda3e39cb94b95bdbL;
    private static final long PCG32_MULT = 0x5851f42d4c957f2dL;

    // RNG Public Methods
    public RNG() {
        this.state = PCG32_DEFAULT_STATE;
        this.inc = PCG32_DEFAULT_STREAM;
    }
    public RNG(long sequenceIndex) {
        SetSequence(sequenceIndex);
    }
    public void SetSequence(long sequenceIndex) {
        state = 0;
        inc = (sequenceIndex << 1) | 1;
        UniformInt32();
        state += PCG32_DEFAULT_STATE;
        UniformInt32();
    }
    public int UniformInt32() {
        long oldstate = state;
        state = oldstate * PCG32_MULT + inc;
        long xorshifted = ((oldstate >> 18) ^ oldstate) >> 27;
        int rot = (int)(oldstate >> 59);
        return (int)(xorshifted >> rot) | (int)(xorshifted << ((~rot + 1) & 31));
    }
    public int UniformInt32(int b) {
        int threshold = (~b + 1) % b;
        while (true) {
            int r = UniformInt32();
            if (r >= threshold) return r % b;
        }
    }
    public float UniformFloat() {
        return Math.min(Pbrt.OneMinusEpsilon, ((float)UniformInt32() * 0x1p-32f));
    }

    public void Advance(long idelta) {
        long cur_mult = PCG32_MULT, cur_plus = inc, acc_mult = 1,
                acc_plus = 0, delta = idelta;
        while (delta > 0) {
            if ((delta & 1) != 0) {
                acc_mult *= cur_mult;
                acc_plus = acc_plus * cur_mult + cur_plus;
            }
            cur_plus = (cur_mult + 1) * cur_plus;
            cur_mult *= cur_mult;
            delta /= 2;
        }
        state = acc_mult * state + acc_plus;
    }

    public long subtract(RNG other) {
        assert (inc == other.inc);
        long cur_mult = PCG32_MULT, cur_plus = inc, cur_state = other.state,
                the_bit = 1, distance = 0;
        while (state != cur_state) {
            if ((state & the_bit) != (cur_state & the_bit)) {
                cur_state = cur_state * cur_mult + cur_plus;
                distance |= the_bit;
            }
            assert ((state & the_bit) == (cur_state & the_bit));
            the_bit <<= 1;
            cur_plus = (cur_mult + 1) * cur_plus;
            cur_mult *= cur_mult;
        }
        return distance;
    }

}