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

    public Point3f() {
        x = y = z = 0;
    }

    public Point3f(float xx, float yy, float zz) {
        x = xx;
        y = yy;
        z = zz;
        assert (!HasNaNs());
    }

    public Point3f(Point3f p) {
        x = p.x;
        y = p.y;
        z = p.z;
        assert (!HasNaNs());
    }

    public Point3f(Point3i pi) {
        x = pi.x;
        y = pi.y;
        z = pi.z;
    }

    public Point3f add(Vector3f v) {
        assert (!v.HasNaNs());
        return new Point3f(x + v.x, y + v.y, z + v.z);
    }

    public Point3f add(Point3f p) {
        assert (!p.HasNaNs());
        return new Point3f(x + p.x, y + p.y, z + p.z);
    }
    public void increment(Vector3f v) {
        assert !v.HasNaNs();
        x += v.x;
        y += v.y;
        z += v.z;
    }
    public void increment(Point3f p) {
        assert !p.HasNaNs();
        x += p.x;
        y += p.y;
        z += p.z;
    }

    public Vector3f subtract(Point3f p) {
        assert (!p.HasNaNs());
        return new Vector3f(x - p.x, y - p.y, z - p.z);
    }

    public Point3f subtract(Vector3f v) {
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

    public float at(int i) {
        assert (i >= 0 && i <= 2);
        if (i == 0) return x;
        if (i == 1) return y;
        return z;
    }
    public void set(int i, float v) {
        assert (i >= 0 && i <= 2);
        if (i == 0) x = v;
        if (i == 1) y = v;
        else z = v;
    }
    public boolean equal(Point3f p) {
        return x == p.x && y == p.y && z == p.z;
    }

    public boolean notEqual(Point3f p) {
        return x != p.x || y != p.y || z != p.z;
    }

    public boolean HasNaNs() {
        return Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z);
    }

    public Point3f negate() {
        return new Point3f(-x, -y, -z);
    }

    public static float Distance(Point3f p1, Point3f p2) {
        return (p1.subtract(p2)).Length();
    }

    public static float DistanceSquared(Point3f p1, Point3f p2) {
        return (p1.subtract(p2)).LengthSquared();
    }

    public static Point3f Lerp(float t, Point3f p0, Point3f p1) {
        return p0.scale(1.0f - t).add(p1.scale(t));
    }

    public static Point3f Min(Point3f p1, Point3f p2) {
        return new Point3f(Math.min(p1.x, p2.x), Math.min(p1.y, p2.y), Math.min(p1.z, p2.z));
    }

    public static Point3f Max(Point3f p1, Point3f p2) {
        return new Point3f(Math.max(p1.x, p2.x), Math.max(p1.y, p2.y), Math.max(p1.z, p2.z));
    }

    public static Point3f Floor(Point3f p) {
        return new Point3f((float) Math.floor(p.x), (float) Math.floor(p.y), (float) Math.floor(p.z));
    }

    public static Point3f Ceil(Point3f p) {
        return new Point3f((float) Math.ceil(p.x), (float) Math.ceil(p.y), (float) Math.ceil(p.z));
    }

    public static Point3f Abs(Point3f p) {
        return new Point3f(Math.abs(p.x), Math.abs(p.y), Math.abs(p.z));
    }

    public static Point3f Permute(Point3f p, int x, int y, int z) {
        return new Point3f(p.at(x), p.at(y), p.at(z));
    }

    public static Point3f OffsetRayOrigin(Point3f p,  Vector3f pError,
                                Normal3f n,  Vector3f w) {
        float d = Normal3f.Dot(Normal3f.Abs(n), pError);
        Vector3f offset = new Vector3f(n.scale(d));
        if (Normal3f.Dot(w, n) < 0) offset = offset.negate();
        Point3f po = p.add(offset);
        // Round offset point _po_ away from _p_
        for (int i = 0; i < 3; ++i) {
            if (offset.at(i) > 0)
                po.set(i, Math.nextUp(po.at(i)));
            else if (offset.at(i) < 0)
                po.set(i, Math.nextDown(po.at(i)));
        }
        return po;
    }

}