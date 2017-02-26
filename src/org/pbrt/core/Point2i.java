/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class Point2i {

    // Point2 Public Data
    public int x, y;

    public Point2i() { x = y = 0; }
    public Point2i(int x, int y) {
        this.x = x;
        this.y = y;
    }
    public Point2i(Point2f p) {
        x = (int)p.x;
        y = (int)p.y;
    }
    public Point2i(Vector2i v) {
        this.x = v.x;
        this.y = v.y;
    }

    public Point2i add(Vector2i v) {
        return new Point2i(x + v.x, y + v.y);
    }

    public Vector2i subtract(Point2i p) {
        return new Vector2i(x - p.x, y - p.y);
    }
    public Point2i subtract(Vector2i v) {
        return new Point2i(x - v.x, y - v.y);
    }
    public Point2i negate() { return new Point2i(-x, -y); }

    public Point2i add(Point2i p) {
        return new Point2i(x + p.x, y + p.y);
    }
    public Point2i scale(int f) {
        return new Point2i(f * x, f * y);
    }
    public int at(int i) {
        assert (i >= 0 && i <= 1);
        if (i == 0) return x;
        return y;
    }

    public boolean equal(Point2i p) { return x == p.x && y == p.y; }
    public boolean notEqual(Point2i p) { return x != p.x || y != p.y; }

    public static Point2i Max(Point2i p1, Point2i p2) {
        return new Point2i(Math.max(p1.x, p2.x), Math.max(p1.y, p2.y));
    }

    public static Point2i Min(Point2i p1, Point2i p2) {
        return new Point2i(Math.min(p1.x, p2.x), Math.min(p1.y, p2.y));
    }
}