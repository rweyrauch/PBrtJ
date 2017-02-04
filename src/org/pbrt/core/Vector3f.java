/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class Vector3f {

    // Vector3 Public Data
    public float x, y, z;

    public Vector3f() {
        x = y = z = 0;
    }

    public Vector3f(float xx, float yy, float zz) {
        x = xx;
        y = yy;
        z = zz;
        assert !HasNaNs();
    }

    public boolean HasNaNs() {
        return Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z);
    }

    public Vector3f(Point3f p) {
        x = p.x;
        y = p.y;
        z = p.z;
    }

    public Vector3f(Normal3f n) {
        x = n.x;
        y = n.y;
        z = n.z;
    }

    public Vector3f add(Vector3f v) {
        assert !v.HasNaNs();
        return new Vector3f(x + v.x, y + v.y, z + v.z);
    }

    public Vector3f subtract(Vector3f v) {
        assert !v.HasNaNs();
        return new Vector3f(x - v.x, y - v.y, z - v.z);
    }

    public boolean equal(Vector3f v) {
        return x == v.x && y == v.y && z == v.z;
    }

    public boolean newEqual(Vector3f v) {
        return x != v.x || y != v.y || z != v.z;
    }

    public Vector3f scale(float s) {
        return new Vector3f(s * x, s * y, s * z);
    }

    public Vector3f invScale(float f) {
        assert (f != 0.0f);
        float inv = 1.0f / f;
        return new Vector3f(x * inv, y * inv, z * inv);
    }

    public Vector3f negate() {
        return new Vector3f(-x, -y, -z);
    }

    public float LengthSquared() {
        return x * x + y * y + z * z;
    }

    public float Length() {
        return (float) Math.sqrt(LengthSquared());
    }

}