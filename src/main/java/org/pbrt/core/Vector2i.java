/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class Vector2i {

    // Vector2i Public Data
    public int x, y;

    public Vector2i() {
        x = 0;
        y = 0;
    }

    public Vector2i(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Vector2i(Point2i p) {
        x = p.x;
        y = p.y;
    }

    public Vector2i(Vector2i v) {
        x = v.x;
        y = v.y;
    }

    public Vector2i add(Vector2i v) {
        return new Vector2i(x + v.x, y + v.y);
    }

    public Vector2i subtract(Vector2i v) {
        return new Vector2i(x - v.x, y - v.y);
    }

    public boolean equals(Vector2i v) {
        return x == v.x && y == v.y;
    }

    public boolean notEquals(Vector2i v) {
        return x != v.x || y != v.y;
    }

    public Vector2i scale(int f) {
        return new Vector2i(f * x, f * y);
    }

    public Vector2i negate() {
        return new Vector2i(-x, -y);
    }

    public int at(int i) {
        if (i == 0) return x;
        return y;
    }

    @Override
    public String toString() {
        return "[ " + this.x + ", " + this.y + " ]";
    }
}