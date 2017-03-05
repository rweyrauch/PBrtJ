/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class Point2f {

    // Point2 Public Data
    public float x, y;

    public Point2f(Point3f p) {
        this.x = p.x;
        this.y = p.y;
        assert (!HasNaNs());
    }
    public Point2f() { x = y = 0; }
    public Point2f(float x, float y) {
        this.x = x;
        this.y = y;
        assert (!HasNaNs());
    }
    public Point2f(Point2i p) {
        this.x = p.x;
        this.y = p.y;
    }
    public Point2f(Vector2f v) {
        this.x = v.x;
        this.y = v.y;
    }
    public Point2f(Point2f p) {
        x = p.x;
        y = p.y;
    }

    public Point2f add(Vector2f v) {
        assert (!v.HasNaNs());
        return new Point2f(x + v.x, y + v.y);
    }

    public Vector2f subtract(Point2f p) {
        assert (!p.HasNaNs());
        return new Vector2f(x - p.x, y - p.y);
    }
    public Point2f subtract(Vector2f v) {
        assert (!v.HasNaNs());
        return new Point2f(x - v.x, y - v.y);
    }
    public Point2f negate() { return new Point2f(-x, -y); }

    public Point2f add(Point2f p) {
        assert (!p.HasNaNs());
        return new Point2f(x + p.x, y + p.y);
    }
    public Point2f scale(float f) {
        return new Point2f(f * x, f * y);
    }
    public Point2f invScale(float f) {
        float inv = 1.0f / f;
        return new Point2f(inv * x, inv * y);
    }
    public float at(int i) {
        assert (i >= 0 && i <= 1);
        if (i == 0) return x;
        return y;
    }

    public boolean equal(Point2f p) { return x == p.x && y == p.y; }
    public boolean notEqual(Point2f p) { return x != p.x || y != p.y; }
    public boolean HasNaNs() { return Float.isNaN(x) || Float.isNaN(y); }

    public static Point2f Floor(Point2f p) {
        return new Point2f((float) Math.floor(p.x), (float) Math.floor(p.y));
    }

    public static Point2f Ceil(Point2f p) {
        return new Point2f((float) Math.ceil(p.x), (float) Math.ceil(p.y));
    }

}