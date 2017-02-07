/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class Point3i {

    // Point3 Public Data
    public int x, y, z;

    public Point3i() {
        x = y = z = 0;
    }

    public Point3i(int xx, int yy, int zz) {
        x = xx;
        y = yy;
        z = zz;
    }

    public Point3i(Point3i p) {
        x = p.x;
        y = p.y;
        z = p.z;
    }

    public Point3i(Point3f p) {
        x = (int)p.x;
        y = (int)p.y;
        z = (int)p.z;
    }

    public Point3i add(Vector3i v) {
        return new Point3i(x + v.x, y + v.y, z + v.z);
    }

    public Point3i add(Point3i p) {
        return new Point3i(x + p.x, y + p.y, z + p.z);
    }

    public Vector3i subtract(Point3i p) {
        return new Vector3i(x - p.x, y - p.y, z - p.z);
    }

    public Point3i subtract(Vector3i v) {
        return new Point3i(x - v.x, y - v.y, z - v.z);
    }

    public Point3i scale(int f) {
        return new Point3i(f * x, f * y, f * z);
    }

    public int at(int i) {
        assert (i >= 0 && i <= 2);
        if (i == 0) return x;
        if (i == 1) return y;
        return z;
    }
    public void set(int i, int v) {
        assert (i >= 0 && i <= 2);
        if (i == 0) x = v;
        if (i == 1) y = v;
        else z = v;
    }
    public boolean equal(Point3i p) {
        return x == p.x && y == p.y && z == p.z;
    }

    public boolean notEqual(Point3i p) {
        return x != p.x || y != p.y || z != p.z;
    }

    public Point3i negate() {
        return new Point3i(-x, -y, -z);
    }

    public static Point3i Min(Point3i p1, Point3i p2) {
        return new Point3i(Math.min(p1.x, p2.x), Math.min(p1.y, p2.y), Math.min(p1.z, p2.z));
    }

    public static Point3i Max(Point3i p1, Point3i p2) {
        return new Point3i(Math.max(p1.x, p2.x), Math.max(p1.y, p2.y), Math.max(p1.z, p2.z));
    }
}