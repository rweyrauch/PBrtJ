/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class FloatTexel implements ArithmeticOps<FloatTexel> {

    public FloatTexel(Float value) {
        this.value = value;
    }
    public Float asFloat() { return value; }

    @Override
    public FloatTexel lerp(float t, FloatTexel v1) {
        return new FloatTexel(Pbrt.Lerp(t, this.value, v1.value));
    }

    @Override
    public FloatTexel add(FloatTexel addend) {
        return new FloatTexel(this.value + addend.value);
    }

    @Override
    public FloatTexel subtract(FloatTexel subtrahend) {
        return new FloatTexel(this.value - subtrahend.value);
    }

    @Override
    public void accum(FloatTexel addend) {
        this.value += addend.value;
    }

    @Override
    public FloatTexel multiply(FloatTexel multiplicand) {
        return new FloatTexel(this.value * multiplicand.value);
    }

    @Override
    public FloatTexel divide(FloatTexel divisor) {
        assert(divisor.value != 0);
        return new FloatTexel(this.value / divisor.value);
    }

    @Override
    public FloatTexel scale(float scalar) {
        return new FloatTexel(this.value * scalar);
    }

    @Override
    public FloatTexel clamp(float low, float high) {
        return new FloatTexel(Pbrt.Clamp(this.value, low, high));
    }

    private Float value;
}