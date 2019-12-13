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
        return b.pMin.equal(pMin) && b.pMax.equal(pMax);
    }
    public boolean notEqual(Bounds3i b) {
        return b.pMin.notEqual(pMin) || b.pMax.notEqual(pMax);
    }
    public Point3i Corner(int corner) {
        assert (corner >= 0 && corner < 8);
        return new Point3i(at(corner & 1).x, at((corner & 2) != 0 ? 1 : 0).y, at((corner & 4) != 0 ? 1 : 0).z);
    }

    public String toString() {
        return "[ " + this.pMin.toString() + " - " + this.pMax.toString() + " ]";
    }

    public static Bounds3i Union(Bounds3i b, Point3i p) {
        var ret = new Bounds3i();
        ret.pMin = Point3i.Min(b.pMin, p);
        ret.pMax = Point3i.Max(b.pMax, p);
        return ret;
    }

    public static Bounds3i Union(Bounds3i b1, Bounds3i b2) {
        var ret = new Bounds3i();
        ret.pMin = Point3i.Min(b1.pMin, b2.pMin);
        ret.pMax = Point3i.Max(b1.pMax, b2.pMax);
        return ret;
    }

    public static Bounds3i Intersect(Bounds3i b1, Bounds3i b2) {
        // Important: assign to pMin/pMax directly and don't run the Bounds2()
        // constructor, since it takes min/max of the points passed to it.  In
        // turn, that breaks returning an invalid bound for the case where we
        // intersect non-overlapping bounds (as we'd like to happen).
        Bounds3i ret = new Bounds3i();
        ret.pMin = Point3i.Max(b1.pMin, b2.pMin);
        ret.pMax = Point3i.Min(b1.pMax, b2.pMax);
        return ret;
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