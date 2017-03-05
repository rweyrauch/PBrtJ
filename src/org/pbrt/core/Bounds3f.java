/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class Bounds3f {
    // Bounds3 Public Data
    public Point3f pMin, pMax;

    public Bounds3f() {
        float minNum = Float.MIN_VALUE;
        float maxNum = Float.MAX_VALUE;
        pMin = new Point3f(maxNum, maxNum, maxNum);
        pMax = new Point3f(minNum, minNum, minNum);
    }
    public Bounds3f(Point3f p) {
        pMin = new Point3f(p);
        pMax = new Point3f(p);
    }
    public Bounds3f(Point3f p1, Point3f p2) {
        pMin = new Point3f(Math.min(p1.x, p2.x), Math.min(p1.y, p2.y), Math.min(p1.z, p2.z));
        pMax = new Point3f(Math.max(p1.x, p2.x), Math.max(p1.y, p2.y), Math.max(p1.z, p2.z));
    }

    public Bounds3f(Bounds3f b) {
        this(b.pMax, b.pMin);
    }

    public Point3f at(int i) {
        assert (i == 0 || i == 1);
        return (i == 0) ? pMin : pMax;
    }
    public boolean equal(Bounds3f b) {
        return b.pMin == pMin && b.pMax == pMax;
    }
    public boolean notEqual(Bounds3f b) {
        return b.pMin != pMin || b.pMax != pMax;
    }
    public Point3f Corner(int corner) {
        assert (corner >= 0 && corner < 8);
        return new Point3f(at(corner & 1).x, at((corner & 2) != 0 ? 1 : 0).y, at((corner & 4) != 0 ? 1 : 0).z);
    }
    public Vector3f Diagonal() {
        return pMax.subtract(pMin);
    }
    public float SurfaceArea() {
        Vector3f d = Diagonal();
        return 2 * (d.x * d.y + d.x * d.z + d.y * d.z);
    }
    public float Volume()  {
        Vector3f d = Diagonal();
        return d.x * d.y * d.z;
    }
    public int MaximumExtent() {
        Vector3f d = Diagonal();
        if (d.x > d.y && d.x > d.z)
            return 0;
        else if (d.y > d.z)
            return 1;
        else
            return 2;
    }
    public Point3f Lerp(Point3f t) {
        return new Point3f(Pbrt.Lerp(t.x, pMin.x, pMax.x), Pbrt.Lerp(t.y, pMin.y, pMax.y), Pbrt.Lerp(t.z, pMin.z, pMax.z));
    }
    public Vector3f Offset(Point3f p) {
        Vector3f o = p.subtract(pMin);
        if (pMax.x > pMin.x) o.x /= pMax.x - pMin.x;
        if (pMax.y > pMin.y) o.y /= pMax.y - pMin.y;
        if (pMax.z > pMin.z) o.z /= pMax.z - pMin.z;
        return o;
    }

    public class BoundSphere {
        public Point3f center;
        public float radius;
    }

    public BoundSphere BoundingSphere() {
        BoundSphere bsphere = new BoundSphere();
        bsphere.center = (pMin.add(pMax)).invScale(2.0f);
        bsphere.radius = Inside(bsphere.center, this) ? Point3f.Distance(bsphere.center, pMax) : 0;
        return bsphere;
    }

    public class BoundIntersect {
        public float hit0;
        public float hit1;
    }
    public BoundIntersect IntersectP(Ray ray) {
        float t0 = 0, t1 = ray.tMax;
        for (int i = 0; i < 3; ++i) {
            // Update interval for _i_th bounding box slab
            float invRayDir = 1 / ray.d.at(i);
            float tNear = (pMin.at(i) - ray.o.at(i)) * invRayDir;
            float tFar = (pMax.at(i) - ray.o.at(i)) * invRayDir;

            // Update parametric interval from slab intersection $t$ values
            if (tNear > tFar) {
                float temp = tNear;
                tNear = tFar;
                tFar = temp;
            }

            // Update _tFar_ to ensure robust ray--bounds intersection
            tFar *= 1 + 2 * Pbrt.gamma(3);
            t0 = tNear > t0 ? tNear : t0;
            t1 = tFar < t1 ? tFar : t1;
            if (t0 > t1) return null;
        }

        BoundIntersect bi = new BoundIntersect();
        bi.hit0 = t0;
        bi.hit1 = t1;

        return bi;
    }

    public boolean IntersectP(Ray ray, Vector3f invDir, int dirIsNeg[]) {
        // Check for ray intersection against $x$ and $y$ slabs
        float tMin = (at(dirIsNeg[0]).x - ray.o.x) * invDir.x;
        float tMax = (at(1 - dirIsNeg[0]).x - ray.o.x) * invDir.x;
        float tyMin = (at(dirIsNeg[1]).y - ray.o.y) * invDir.y;
        float tyMax = (at(1 - dirIsNeg[1]).y - ray.o.y) * invDir.y;

        // Update _tMax_ and _tyMax_ to ensure robust bounds intersection
        tMax *= 1 + 2 * Pbrt.gamma(3);
        tyMax *= 1 + 2 * Pbrt.gamma(3);
        if (tMin > tyMax || tyMin > tMax) return false;
        if (tyMin > tMin) tMin = tyMin;
        if (tyMax < tMax) tMax = tyMax;

        // Check for ray intersection against $z$ slab
        float tzMin = (at(dirIsNeg[2]).z - ray.o.z) * invDir.z;
        float tzMax = (at(1 - dirIsNeg[2]).z - ray.o.z) * invDir.z;

        // Update _tzMax_ to ensure robust bounds intersection
        tzMax *= 1 + 2 * Pbrt.gamma(3);
        if (tMin > tzMax || tzMin > tMax) return false;
        if (tzMin > tMin) tMin = tzMin;
        if (tzMax < tMax) tMax = tzMax;
        return (tMin < ray.tMax) && (tMax > 0);
    }

    public static Bounds3f Union(Bounds3f b, Point3f p) {
        return new Bounds3f(new Point3f(Math.min(b.pMin.x, p.x), Math.min(b.pMin.y, p.y), Math.min(b.pMin.z, p.z)),
            new Point3f(Math.max(b.pMax.x, p.x), Math.max(b.pMax.y, p.y), Math.max(b.pMax.z, p.z)));
    }

    public static Bounds3f Union(Bounds3f b1, Bounds3f b2) {
        return new Bounds3f(new Point3f(Math.min(b1.pMin.x, b2.pMin.x), Math.min(b1.pMin.y, b2.pMin.y), Math.min(b1.pMin.z, b2.pMin.z)),
            new Point3f(Math.max(b1.pMax.x, b2.pMax.x), Math.max(b1.pMax.y, b2.pMax.y), Math.max(b1.pMax.z, b2.pMax.z)));
    }

    public static Bounds3f Intersect(Bounds3f b1, Bounds3f b2) {
        return new Bounds3f(new Point3f(Math.max(b1.pMin.x, b2.pMin.x), Math.max(b1.pMin.y, b2.pMin.y), Math.max(b1.pMin.z, b2.pMin.z)),
            new Point3f(Math.min(b1.pMax.x, b2.pMax.x), Math.min(b1.pMax.y, b2.pMax.y), Math.min(b1.pMax.z, b2.pMax.z)));
    }

    public static boolean Overlaps(Bounds3f b1, Bounds3f b2) {
        boolean x = (b1.pMax.x >= b2.pMin.x) && (b1.pMin.x <= b2.pMax.x);
        boolean y = (b1.pMax.y >= b2.pMin.y) && (b1.pMin.y <= b2.pMax.y);
        boolean z = (b1.pMax.z >= b2.pMin.z) && (b1.pMin.z <= b2.pMax.z);
        return (x && y && z);
    }

    public static boolean Inside(Point3f p, Bounds3f b) {
        return (p.x >= b.pMin.x && p.x <= b.pMax.x && p.y >= b.pMin.y &&
                p.y <= b.pMax.y && p.z >= b.pMin.z && p.z <= b.pMax.z);
    }

    public static boolean InsideExclusive(Point3f p, Bounds3f b) {
        return (p.x >= b.pMin.x && p.x < b.pMax.x && p.y >= b.pMin.y &&
                p.y < b.pMax.y && p.z >= b.pMin.z && p.z < b.pMax.z);
    }

    public static Bounds3f Expand(Bounds3f b, float delta) {
        Vector3f dv = new Vector3f(delta, delta, delta);
        return new Bounds3f(b.pMin.subtract(dv), b.pMax.add(dv));
    }

    public static float DistanceSquared(Point3f p, Bounds3f b) {
        float dx = Math.max(0.0f, Math.max(b.pMin.x - p.x, p.x - b.pMax.x));
        float dy = Math.max(0.0f, Math.max(b.pMin.y - p.y, p.y - b.pMax.y));
        float dz = Math.max(0.0f, Math.max(b.pMin.z - p.z, p.z - b.pMax.z));
        return dx * dx + dy * dy + dz * dz;
    }

    public static float Distance(Point3f p, Bounds3f b) {
        return (float)Math.sqrt(DistanceSquared(p, b));
    }

}