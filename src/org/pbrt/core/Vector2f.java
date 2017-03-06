/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class Vector2f {

    // Vector2f Public Data
    public float x, y;

    public Vector2f() {
        x = 0.0f;
        y = 0.0f;
    }

    public Vector2f(float xx, float yy) {
        x = xx;
        y = yy;
        assert !HasNaNs();
    }

    public boolean HasNaNs() {
        return Float.isNaN(x) || Float.isNaN(y);
    }

    public Vector2f(Point2f p) {
        x = p.x;
        y = p.y;
    }

    public Vector2f(Point3f p) {
        x = p.x;
        y = p.y;
    }

    // default versions of these are fine for release builds; for debug
    // we define them so that we can add the Assert checks.
    public Vector2f(Vector2f v) {
        x = v.x;
        y = v.y;
        assert !HasNaNs();
    }

    public Vector2f add(Vector2f v) {
        return new Vector2f(x + v.x, y + v.y);
    }

    public Vector2f subtract(Vector2f v) {
        return new Vector2f(x - v.x, y - v.y);
    }

    public boolean equals(Vector2f v) {
        return x == v.x && y == v.y;
    }

    public boolean notEquals(Vector2f v) {
        return x != v.x || y != v.y;
    }

    public Vector2f scale(float f) {
        return new Vector2f(f * x, f * y);
    }

    public Vector2f invScale(float f) {
        float inv = 1.0f / f;
        return new Vector2f(x * inv, y * inv);
    }

    public Vector2f negate() {
        return new Vector2f(-x, -y);
    }

    public float at(int i) {
        if (i == 0) return x;
        return y;
    }

    @Override
    public String toString() {
        return "[ " + this.x + ", " + this.y + " ]";
    }

    public static float Dot(Vector2f v1, Vector2f v2) {
        assert (!v1.HasNaNs() && !v2.HasNaNs());
        return v1.x * v2.x + v1.y * v2.y;
    }

    public float LengthSquared() {
        return x * x + y * y;
    }

    public float Length() {
        return (float) Math.sqrt((double) LengthSquared());
    }
}