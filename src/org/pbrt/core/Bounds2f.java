/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class Bounds2f {
    // Bounds2 Public Data
    public Point2f pMin, pMax;

    public Bounds2f() {
        float minNum = Float.MIN_VALUE;
        float maxNum = Float.MAX_VALUE;
        pMin = new Point2f(maxNum, maxNum);
        pMax = new Point2f(minNum, minNum);
    }
    public Bounds2f(Point2f p) {
        pMin = new Point2f(p);
        pMax = new Point2f(p);
    }
    public Bounds2f(Point2f p1, Point2f p2) {
        pMin = new Point2f(Math.min(p1.x, p2.x), Math.min(p1.y, p2.y));
        pMax = new Point2f(Math.max(p1.x, p2.x), Math.max(p1.y, p2.y));
    }

    public Bounds2f(Bounds2i b) {
        pMin = new Point2f(b.pMin.x, b.pMin.y);
        pMax = new Point2f(b.pMax.x, b.pMax.y);
    }
    public Bounds2f(Bounds2f b) {
        this(b.pMax, b.pMin);
    }

    public Point2f at(int i) {
        assert (i == 0 || i == 1);
        return (i == 0) ? pMin : pMax;
    }
    public boolean equal(Bounds2f b) {
        return b.pMin == pMin && b.pMax == pMax;
    }
    public boolean notEqual(Bounds2f b) {
        return b.pMin != pMin || b.pMax != pMax;
    }

    public Vector2f Diagonal() {
        return pMax.subtract(pMin);
    }

    public float Area() {
        Vector2f d = pMax.subtract(pMin);
        return (d.x * d.y);
    }

    public int MaximumExtent() {
        Vector2f diag = Diagonal();
        if (diag.x > diag.y)
            return 0;
        else
            return 1;
    }

    public Point2f Lerp(Point2f t) {
        return new Point2f(Pbrt.Lerp(t.x, pMin.x, pMax.x), Pbrt.Lerp(t.y, pMin.y, pMax.y));
    }
    public Vector2f Offset(Point2f p) {
        Vector2f o = p.subtract(pMin);
        if (pMax.x > pMin.x) o.x /= pMax.x - pMin.x;
        if (pMax.y > pMin.y) o.y /= pMax.y - pMin.y;
        return o;
    }

    public static Bounds2f Union(Bounds2f b, Point2f p) {
        Bounds2f ret = new Bounds2f(
                new Point2f(Math.min(b.pMin.x, p.x), Math.min(b.pMin.y, p.y)),
                new Point2f(Math.max(b.pMax.x, p.x), Math.max(b.pMax.y, p.y)));
        return ret;
    }

    public static Bounds2f Union(Bounds2f b, Bounds2f b2) {
        Bounds2f ret = new Bounds2f(
                new Point2f(Math.min(b.pMin.x, b2.pMin.x), Math.min(b.pMin.y, b2.pMin.y)),
                new Point2f(Math.max(b.pMax.x, b2.pMax.x), Math.max(b.pMax.y, b2.pMax.y)));
        return ret;
    }

    public static Bounds2f Intersect(Bounds2f b, Bounds2f b2) {
        Bounds2f ret = new Bounds2f(
                new Point2f(Math.max(b.pMin.x, b2.pMin.x), Math.max(b.pMin.y, b2.pMin.y)),
                new Point2f(Math.min(b.pMax.x, b2.pMax.x), Math.min(b.pMax.y, b2.pMax.y)));
        return ret;
    }

    public static boolean Overlaps(Bounds2f ba, Bounds2f bb) {
        boolean x = (ba.pMax.x >= bb.pMin.x) && (ba.pMin.x <= bb.pMax.x);
        boolean y = (ba.pMax.y >= bb.pMin.y) && (ba.pMin.y <= bb.pMax.y);
        return (x && y);
    }

    public static boolean Inside(Point2f pt, Bounds2f b) {
        return (pt.x >= b.pMin.x && pt.x <= b.pMax.x && pt.y >= b.pMin.y &&
                pt.y <= b.pMax.y);
    }

    public static boolean InsideExclusive(Point2f pt, Bounds2f b) {
        return (pt.x >= b.pMin.x && pt.x < b.pMax.x && pt.y >= b.pMin.y &&
                pt.y < b.pMax.y);
    }

    public static Bounds2f Expand(Bounds2f b, float delta) {
        Vector2f dv = new Vector2f(delta, delta);
        return new Bounds2f(b.pMin.subtract(dv), b.pMax.add(dv));
    }

}