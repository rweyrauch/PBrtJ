/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class Vector3i {

    // Vector3 Public Data
    public int x, y, z;

    public Vector3i() {
        x = y = z = 0;
    }

    public Vector3i(int xx, int yy, int zz) {
        x = xx;
        y = yy;
        z = zz;
    }

    public Vector3i(Point3i p) {
        x = p.x;
        y = p.y;
        z = p.z;
    }

    public Vector3i(Vector3i v) {
        x = v.x;
        y = v.y;
        z = v.z;
    }

    public Vector3i add(Vector3i v) {
         return new Vector3i(x + v.x, y + v.y, z + v.z);
    }

    public Vector3i subtract(Vector3i v) {
        return new Vector3i(x - v.x, y - v.y, z - v.z);
    }

    public boolean equal(Vector3i v) {
        return x == v.x && y == v.y && z == v.z;
    }

    public boolean newEqual(Vector3i v) {
        return x != v.x || y != v.y || z != v.z;
    }

    public Vector3i scale(int s) {
        return new Vector3i(s * x, s * y, s * z);
    }

    public Vector3i negate() {
        return new Vector3i(-x, -y, -z);
    }

    public int at(int i) {
        assert (i >= 0 && i <= 2);
        if (i == 0) return x;
        if (i == 1) return y;
        return z;
    }

    public static Vector3i Min(Vector3i p1, Vector3i p2) {
        return new Vector3i(Math.min(p1.x, p2.x), Math.min(p1.y, p2.y),
                Math.min(p1.z, p2.z));
    }
    public static Vector3i Max(Vector3i p1, Vector3i p2) {
        return new Vector3i(Math.max(p1.x, p2.x), Math.max(p1.y, p2.y),
                Math.max(p1.z, p2.z));
    }
}