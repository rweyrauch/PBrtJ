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
        x = y = z = 0.0f;
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
    public Vector3f add(Normal3f n) {
        assert !n.HasNaNs();
        return new Vector3f(x + n.x, y + n.y, z + n.z);
    }

    public void increment(Vector3f v) {
        assert !v.HasNaNs();
        x += v.x;
        y += v.y;
        z += v.z;
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
        return (float)Math.sqrt(LengthSquared());
    }

    public float at(int i) {
        assert (i >= 0 && i <= 2);
        if (i == 0) return x;
        if (i == 1) return y;
        return z;
    }

    public static Vector3f Abs(Vector3f v) {
        return new Vector3f(Math.abs(v.x), Math.abs(v.y), Math.abs(v.z));
    }
    public static float Dot(Vector3f v1, Vector3f v2) {
        assert (!v1.HasNaNs() && !v2.HasNaNs());
        return v1.x * v2.x + v1.y * v2.y + v1.z * v2.z;
    }
    public static float AbsDot(Vector3f v1, Vector3f v2) {
        assert (!v1.HasNaNs() && !v2.HasNaNs());
        return Math.abs(Dot(v1, v2));
    }
    public static Vector3f Cross(Vector3f v1, Vector3f v2) {
        assert (!v1.HasNaNs() && !v2.HasNaNs());
        double v1x = v1.x, v1y = v1.y, v1z = v1.z;
        double v2x = v2.x, v2y = v2.y, v2z = v2.z;
        return new Vector3f((float)((v1y * v2z) - (v1z * v2y)), (float)((v1z * v2x) - (v1x * v2z)), (float)((v1x * v2y) - (v1y * v2x)));
    }
    public static Vector3f Cross(Vector3f v1, Normal3f v2) {
        assert (!v1.HasNaNs() && !v2.HasNaNs());
        double v1x = v1.x, v1y = v1.y, v1z = v1.z;
        double v2x = v2.x, v2y = v2.y, v2z = v2.z;
        return new Vector3f((float)((v1y * v2z) - (v1z * v2y)), (float)((v1z * v2x) - (v1x * v2z)), (float)((v1x * v2y) - (v1y * v2x)));
    }
    public static Vector3f Cross(Normal3f v1, Vector3f v2) {
        assert (!v1.HasNaNs() && !v2.HasNaNs());
        double v1x = v1.x, v1y = v1.y, v1z = v1.z;
        double v2x = v2.x, v2y = v2.y, v2z = v2.z;
        return new Vector3f((float)((v1y * v2z) - (v1z * v2y)), (float)((v1z * v2x) - (v1x * v2z)), (float)((v1x * v2y) - (v1y * v2x)));
    }
    public static Vector3f Normalize(Vector3f v) {
        return v.invScale(v.Length());
    }
    public static float MinComponent(Vector3f v) {
        return Math.min(v.x, Math.min(v.y, v.z));
    }
    public static float MaxComponent(Vector3f v) {
        return Math.max(v.x, Math.max(v.y, v.z));
    }
    public static int MaxDimension(Vector3f v) {
        return (v.x > v.y) ? ((v.x > v.z) ? 0 : 2) : ((v.y > v.z) ? 1 : 2);
    }
    public static Vector3f Min(Vector3f p1, Vector3f p2) {
        return new Vector3f(Math.min(p1.x, p2.x), Math.min(p1.y, p2.y),
                Math.min(p1.z, p2.z));
    }
    public static Vector3f Max(Vector3f p1, Vector3f p2) {
        return new Vector3f(Math.max(p1.x, p2.x), Math.max(p1.y, p2.y),
                Math.max(p1.z, p2.z));
    }
    public static Vector3f Permute(Vector3f v, int x, int y, int z) {
        return new Vector3f(v.at(x), v.at(y), v.at(z));
    }

    public static class CoordSystem {
        public Vector3f v1, v2, v3;
    }
    public static CoordSystem CoordinateSystem(Vector3f v1) {
        CoordSystem coord = new CoordSystem();
        coord.v1 = v1;
        if (Math.abs(v1.x) > Math.abs(v1.y))
            coord.v2 = new Vector3f(-v1.z, 0, v1.x).invScale((float)Math.sqrt(v1.x * v1.x + v1.z * v1.z));
        else
            coord.v2 = new Vector3f(0, v1.z, -v1.y).invScale((float)Math.sqrt(v1.y * v1.y + v1.z * v1.z));

        coord.v3 = Cross(v1, coord.v2);
        return coord;
    }

    public static Vector3f Faceforward(Vector3f v, Vector3f v2) {
        return (Dot(v, v2) < 0.f) ? v.negate() : v;
    }

}