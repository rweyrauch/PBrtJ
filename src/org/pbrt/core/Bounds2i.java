/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class Bounds2i {
    // Bounds2 Public Data
    public Point2i pMin, pMax;

    public Bounds2i() {
        int minNum = Integer.MIN_VALUE;
        int maxNum = Integer.MAX_VALUE;
        pMin = new Point2i(maxNum, maxNum);
        pMax = new Point2i(minNum, minNum);
    }
    public Bounds2i(Point2i p) {
        pMin = new Point2i(p);
        pMax = new Point2i(p);
    }
    public Bounds2i(Point2i p1, Point2i p2) {
        pMin = new Point2i(Math.min(p1.x, p2.x), Math.min(p1.y, p2.y));
        pMax = new Point2i(Math.max(p1.x, p2.x), Math.max(p1.y, p2.y));
    }

    public Bounds2i(Bounds2f b) {
        pMin = new Point2i((int)b.pMin.x, (int)b.pMin.y);
        pMax = new Point2i((int)b.pMax.x, (int)b.pMax.y);
    }

    public Bounds2i(Bounds2i b) {
        this(b.pMax, b.pMin);
    }

    public Point2i at(int i) {
        assert (i == 0 || i == 1);
        return (i == 0) ? pMin : pMax;
    }
    public boolean equal(Bounds2i b) {
        return b.pMin == pMin && b.pMax == pMax;
    }
    public boolean notEqual(Bounds2i b) {
        return b.pMin != pMin || b.pMax != pMax;
    }

    public Vector2i Diagonal() {
        return pMax.subtract(pMin);
    }

    public int Area() {
        Vector2i d = pMax.subtract(pMin);
        return (d.x * d.y);
    }

    public int MaximumExtent() {
        Vector2i diag = Diagonal();
        if (diag.x > diag.y)
            return 0;
        else
            return 1;
    }

    public Vector2i Offset(Point2i p) {
        Vector2i o = p.subtract(pMin);
        if (pMax.x > pMin.x) o.x /= pMax.x - pMin.x;
        if (pMax.y > pMin.y) o.y /= pMax.y - pMin.y;
        return o;
    }

    public static Bounds2i Union(Bounds2i b, Point2i p) {
        Bounds2i ret = new Bounds2i(
                new Point2i(Math.min(b.pMin.x, p.x), Math.min(b.pMin.y, p.y)),
                new Point2i(Math.max(b.pMax.x, p.x), Math.max(b.pMax.y, p.y)));
        return ret;
    }

    public static Bounds2i Union(Bounds2i b, Bounds2i b2) {
        Bounds2i ret = new Bounds2i(
                new Point2i(Math.min(b.pMin.x, b2.pMin.x), Math.min(b.pMin.y, b2.pMin.y)),
                new Point2i(Math.max(b.pMax.x, b2.pMax.x), Math.max(b.pMax.y, b2.pMax.y)));
        return ret;
    }

    public static Bounds2i Intersect(Bounds2i b, Bounds2i b2) {
        Bounds2i ret = new Bounds2i(
                new Point2i(Math.max(b.pMin.x, b2.pMin.x), Math.max(b.pMin.y, b2.pMin.y)),
                new Point2i(Math.min(b.pMax.x, b2.pMax.x), Math.min(b.pMax.y, b2.pMax.y)));
        return ret;
    }

    public static boolean Overlaps(Bounds2i ba, Bounds2i bb) {
        boolean x = (ba.pMax.x >= bb.pMin.x) && (ba.pMin.x <= bb.pMax.x);
        boolean y = (ba.pMax.y >= bb.pMin.y) && (ba.pMin.y <= bb.pMax.y);
        return (x && y);
    }

    public static boolean Inside(Point2i pt, Bounds2i b) {
        return (pt.x >= b.pMin.x && pt.x <= b.pMax.x && pt.y >= b.pMin.y &&
                pt.y <= b.pMax.y);
    }

    public static boolean InsideExclusive(Point2i pt, Bounds2i b) {
        return (pt.x >= b.pMin.x && pt.x < b.pMax.x && pt.y >= b.pMin.y &&
                pt.y < b.pMax.y);
    }

    public static Bounds2i Expand(Bounds2i b, int delta) {
        Vector2i dv = new Vector2i(delta, delta);
        return new Bounds2i(b.pMin.subtract(dv), b.pMax.add(dv));
    }

}