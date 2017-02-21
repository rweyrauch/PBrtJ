/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public interface ArithmeticOps<T> {

    T lerp(float t, T v1);
    T add(T addend);
    T subtract(T subtrahend);
    void accum(T addend);
    T multiply(T multiplicand);
    T divide(T divisor);
    T scale(float scalar);
    T clamp(float low, float high);
}