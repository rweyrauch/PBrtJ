/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class Bounds3i {
    // Bounds3 Public Data
    public Point3i pMin, pMax;

    public Bounds3i() {
        int minNum = Integer.MIN_VALUE;
        int maxNum = Integer.MAX_VALUE;
        pMin = new Point3i(maxNum, maxNum, maxNum);
        pMax = new Point3i(minNum, minNum, minNum);
    }
    public Bounds3i(Point3i p) {
        pMin = new Point3i(p);
        pMax = new Point3i(p);
    }
    public Bounds3i(Point3i p1, Point3i p2) {
        pMin = new Point3i(Math.min(p1.x, p2.x), Math.min(p1.y, p2.y), Math.min(p1.z, p2.z));
        pMax = new Point3i(Math.max(p1.x, p2.x), Math.max(p1.y, p2.y), Math.max(p1.z, p2.z));
    }

    public Bounds3i(Bounds3i b) {
        this(b.pMax, b.pMin);
    }

    public Point3i at(int i) {
        assert (i == 0 || i == 1);
        return (i == 0) ? pMin : pMax;
    }
    public boolean equal(Bounds3i b) {
        return b.pMin == pMin && b.pMax == pMax;
    }
    public boolean notEqual(Bounds3i b) {
        return b.pMin != pMin || b.pMax != pMax;
    }
    public Point3i Corner(int corner) {
        assert (corner >= 0 && corner < 8);
        return new Point3i(at(corner & 1).x, at((corner & 2) != 0 ? 1 : 0).y, at((corner & 4) != 0 ? 1 : 0).z);
    }

    public String toString() {
        return "[ " + this.pMin.toString() + " - " + this.pMax.toString() + " ]";
    }

    public static Bounds3i Union(Bounds3i b, Point3i p) {
        return new Bounds3i(new Point3i(Math.min(b.pMin.x, p.x), Math.min(b.pMin.y, p.y), Math.min(b.pMin.z, p.z)),
            new Point3i(Math.max(b.pMax.x, p.x), Math.max(b.pMax.y, p.y), Math.max(b.pMax.z, p.z)));
    }

    public static Bounds3i Union(Bounds3i b1, Bounds3i b2) {
        return new Bounds3i(new Point3i(Math.min(b1.pMin.x, b2.pMin.x), Math.min(b1.pMin.y, b2.pMin.y), Math.min(b1.pMin.z, b2.pMin.z)),
            new Point3i(Math.max(b1.pMax.x, b2.pMax.x), Math.max(b1.pMax.y, b2.pMax.y), Math.max(b1.pMax.z, b2.pMax.z)));
    }

    public static Bounds3i Intersect(Bounds3i b1, Bounds3i b2) {
        return new Bounds3i(new Point3i(Math.max(b1.pMin.x, b2.pMin.x), Math.max(b1.pMin.y, b2.pMin.y), Math.max(b1.pMin.z, b2.pMin.z)),
            new Point3i(Math.min(b1.pMax.x, b2.pMax.x), Math.min(b1.pMax.y, b2.pMax.y), Math.min(b1.pMax.z, b2.pMax.z)));
    }

    public static boolean Overlaps(Bounds3i b1, Bounds3i b2) {
        boolean x = (b1.pMax.x >= b2.pMin.x) && (b1.pMin.x <= b2.pMax.x);
        boolean y = (b1.pMax.y >= b2.pMin.y) && (b1.pMin.y <= b2.pMax.y);
        boolean z = (b1.pMax.z >= b2.pMin.z) && (b1.pMin.z <= b2.pMax.z);
        return (x && y && z);
    }

    public static boolean Inside(Point3i p, Bounds3i b) {
        return (p.x >= b.pMin.x && p.x <= b.pMax.x && p.y >= b.pMin.y &&
                p.y <= b.pMax.y && p.z >= b.pMin.z && p.z <= b.pMax.z);
    }

    public static boolean InsideExclusive(Point3i p, Bounds3i b) {
        return (p.x >= b.pMin.x && p.x < b.pMax.x && p.y >= b.pMin.y &&
                p.y < b.pMax.y && p.z >= b.pMin.z && p.z < b.pMax.z);
    }

}