/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class Point3f {

    // Point3 Public Data
    public float x, y, z;

    public Point3f() { x = y = z = 0; }
    public Point3f(float xx, float yy, float zz){
        x = xx;
        y = yy;
        z = zz;
        assert (!HasNaNs()); }

    public Point3f(Point3f p) {
        x = p.x;
        y = p.y;
        z = p.z;
        assert (!HasNaNs());
    }
    public Point3f add(Vector3f v) {
        assert (!v.HasNaNs());
        return new Point3f(x + v.x, y + v.y, z + v.z);
    }
    public Point3f add(Point3f p) {
        assert (!p.HasNaNs());
        return new Point3f(x + p.x, y + p.y, z + p.z);
    }
    public Vector3f subtract(Point3f p) {
        assert (!p.HasNaNs());
        return new Vector3f(x - p.x, y - p.y, z - p.z);
    }
    public Point3f subtract(Vector3f v)  {
        assert (!v.HasNaNs());
        return new Point3f(x - v.x, y - v.y, z - v.z);
    }
    public Point3f scale(float f) {
        return new Point3f(f * x, f * y, f * z);
    }
    public Point3f invScale(float f) {
        float inv = 1.0f / f;
        return new Point3f(inv * x, inv * y, inv * z);
    }
    public float at(int i)  {
        assert (i >= 0 && i <= 2);
        if (i == 0) return x;
        if (i == 1) return y;
        return z;
    }
    public boolean equal(Point3f p) {
        return x == p.x && y == p.y && z == p.z;
    }
    public boolean notEqual(Point3f p) {
        return x != p.x || y != p.y || z != p.z;
    }
    public boolean HasNaNs() { return Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z); }
    public Point3f negate()  { return new Point3f(-x, -y, -z); }

}