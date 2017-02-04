/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class Normal3f {

    // Normal3 Public Data
    public float x, y, z;

    public Normal3f() {
        x = y = z = 0;
    }

    public Normal3f(float xx, float yy, float zz) {
        x = xx;
        y = yy;
        z = zz;
        assert (!HasNaNs());
    }

    public Normal3f(Vector3f v) {
        x = v.x;
        y = v.y;
        z = v.z;
    }

    public Normal3f negate() {
        return new Normal3f(-x, -y, -z);
    }

    public Normal3f add(Normal3f n) {
        assert (!n.HasNaNs());
        return new Normal3f(x + n.x, y + n.y, z + n.z);
    }

    public boolean HasNaNs() {
        return Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z);
    }

    public Normal3f scale(float f) {
        return new Normal3f(f * x, f * y, f * z);
    }

    public Normal3f invScale(float f) {
        float inv = 1.0f / f;
        return new Normal3f(x * inv, y * inv, z * inv);
    }

    public float LengthSquared() {
        return x * x + y * y + z * z;
    }

    public float Length() {
        return (float) Math.sqrt(LengthSquared());
    }

    public boolean equal(Normal3f n) {
        return x == n.x && y == n.y && z == n.z;
    }

    public boolean notEqual(Normal3f n) {
        return x != n.x || y != n.y || z != n.z;
    }

    public float at(int i) {
        assert (i >= 0 && i <= 2);
        if (i == 0) return x;
        if (i == 1) return y;
        return z;
    }
}