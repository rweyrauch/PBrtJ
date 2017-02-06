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

    public void flip() {
        x = -x;
        y = -y;
        z = -z;
    }
    public Normal3f negate() {
        return new Normal3f(-x, -y, -z);
    }


    public Normal3f add(Normal3f n) {
        assert (!n.HasNaNs());
        return new Normal3f(x + n.x, y + n.y, z + n.z);
    }
    public Normal3f add(Vector3f v) {
        assert (!v.HasNaNs());
        return new Normal3f(x + v.x, y + v.y, z + v.z);
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

    public static Normal3f Normalize(Normal3f n) {
        return n.invScale(n.Length());
    }

    public static float Dot(Normal3f n1, Vector3f v2) {
        assert(!n1.HasNaNs() && !v2.HasNaNs());
        return n1.x * v2.x + n1.y * v2.y + n1.z * v2.z;
    }

    public static float Dot(Vector3f v1, Normal3f n2) {
        assert(!v1.HasNaNs() && !n2.HasNaNs());
        return v1.x * n2.x + v1.y * n2.y + v1.z * n2.z;
    }

    public static float Dot(Normal3f n1, Normal3f n2) {
        assert(!n1.HasNaNs() && !n2.HasNaNs());
        return n1.x * n2.x + n1.y * n2.y + n1.z * n2.z;
    }

    public static float AbsDot(Normal3f n1, Vector3f v2) {
        assert(!n1.HasNaNs() && !v2.HasNaNs());
        return Math.abs(n1.x * v2.x + n1.y * v2.y + n1.z * v2.z);
    }

    public static float AbsDot(Vector3f v1, Normal3f n2) {
        assert(!v1.HasNaNs() && !n2.HasNaNs());
        return Math.abs(v1.x * n2.x + v1.y * n2.y + v1.z * n2.z);
    }

    public static float AbsDot(Normal3f n1, Normal3f n2) {
        assert(!n1.HasNaNs() && !n2.HasNaNs());
        return Math.abs(n1.x * n2.x + n1.y * n2.y + n1.z * n2.z);
    }

    public static Normal3f Faceforward(Normal3f n, Vector3f v) {
        return (Dot(n, v) < 0.f) ? n.negate() : n;
    }

    public static Normal3f Faceforward(Normal3f n, Normal3f n2) {
        return (Dot(n, n2) < 0.f) ? n.negate() : n;
    }

    public static Vector3f Faceforward(Vector3f v, Normal3f n2) {
        return (Dot(v, n2) < 0.f) ? v.negate() : v;
    }

    public static Normal3f Abs(Normal3f v) {
        return new Normal3f(Math.abs(v.x), Math.abs(v.y), Math.abs(v.z));
    }

}