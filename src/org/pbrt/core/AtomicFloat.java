/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

import java.util.concurrent.atomic.AtomicInteger;

public class AtomicFloat {
    private AtomicInteger bits;

    public AtomicFloat() {
        this.bits = new AtomicInteger();
    }

    public AtomicFloat(float value) {
        this.bits = new AtomicInteger(Float.floatToIntBits(value));
    }

    public float get() {
        return Float.intBitsToFloat(bits.get());
    }

    public void set(float value) {
        bits.set(Float.floatToIntBits(value));
    }

    public void add(float value) {
        int oldBits = bits.get();
        int newBits;
        do {
            newBits = Float.floatToIntBits(Float.intBitsToFloat(oldBits) + value);
        } while (!bits.weakCompareAndSet(oldBits, newBits));
    }
}