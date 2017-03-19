
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.shapes;

import org.pbrt.core.*;
import org.pbrt.core.Error;

import java.util.ArrayList;
import java.util.Objects;

public class Curve extends Shape {

    public enum CurveType { Flat, Cylinder, Ribbon }

    public static class CurveCommon {
        public CurveCommon(Point3f[] c, float w0, float w1, CurveType type, Normal3f[] norm) {
            this.type = type;
            this.width[0] = w0;
            this.width[1] = w1;
            assert c.length == 4;
            for (int i = 0; i < 4; i++) {
                this.cpObj[i] = c[i];
            }
            if (norm != null) {
                assert norm.length == 2;
                this.n[0] = Normal3f.Normalize(norm[0]);
                this.n[1] = Normal3f.Normalize(norm[1]);
                this.normalAngle = (float)Math.acos(Pbrt.Clamp(Normal3f.Dot(n[0], n[1]), 0, 1));
                invSinNormalAngle = 1 / (float)Math.sin(this.normalAngle);
            }
        }

        public final CurveType type;
        public Point3f[] cpObj = { null, null, null, null };
        public float[] width = { 0, 0 };
        public Normal3f[] n = { null, null };
        public float normalAngle, invSinNormalAngle;

        public static long sizeof() {
            return 24 * 4;
        }
    }

    public static ArrayList<Shape> Create(Transform object2world, Transform world2object, boolean reverseOrientation, ParamSet paramSet) {
        float width = paramSet.FindOneFloat("width", 1);
        float width0 = paramSet.FindOneFloat("width0", width);
        float width1 = paramSet.FindOneFloat("width1", width);

        Point3f[] cp = paramSet.FindPoint3f("P");
        if (cp.length != 4) {
            Error.Error("Must provide 4 control points for \"curve\" primitive. (Provided %d).", cp.length);
            return null;
        }

        CurveType type;
        String curveType = paramSet.FindOneString("type", "flat");
        if (Objects.equals(curveType, "flat"))
            type = CurveType.Flat;
        else if (Objects.equals(curveType, "ribbon"))
            type = CurveType.Ribbon;
        else if (Objects.equals(curveType, "cylinder"))
            type = CurveType.Cylinder;
        else {
            Error.Error("Unknown curve type \"%s\".  Using \"flat\".", curveType);
            type = CurveType.Cylinder;
        }
        Normal3f[] n = paramSet.FindNormal3f("N");
        if (n != null) {
            if (type != CurveType.Ribbon) {
                Error.Warning("Curve normals are only used with \"ribbon\" type curves.");
                n = null;
            } else if (n.length != 2) {
                Error.Error("Must provide two normals with \"N\" parameter for ribbon curves. (Provided %d).", n.length);
                return null;
            }
        }

        int sd = (int)paramSet.FindOneFloat("splitdepth", 3);

        if (type == CurveType.Ribbon && n == null) {
            Error.Error("Must provide normals \"N\" at curve endpoints with ribbon curves.");
            return null;
        } else
            return CreateCurve(object2world, world2object, reverseOrientation, cp, width0, width1, type, n, sd);
    }

    public Curve(Transform ObjectToWorld, Transform WorldToObject, boolean reverseOrientation, CurveCommon common, float uMin, float uMax) {
        super(ObjectToWorld, WorldToObject, reverseOrientation);
        this.common = common;
        this.uMin = uMin;
        this.uMax = uMax;
    }

    @Override
    public Bounds3f ObjectBound() {
        // Compute object-space control points for curve segment, _cpObj_
        Point3f[] cpObj = {BlossomBezier(common.cpObj, uMin, uMin, uMin), BlossomBezier(common.cpObj, uMin, uMin, uMax),
                BlossomBezier(common.cpObj, uMin, uMax, uMax), BlossomBezier(common.cpObj, uMax, uMax, uMax) };
        Bounds3f b = Bounds3f.Union(new Bounds3f(cpObj[0], cpObj[1]), new Bounds3f(cpObj[2], cpObj[3]));
        float[] width = {Pbrt.Lerp(uMin, common.width[0], common.width[1]), Pbrt.Lerp(uMax, common.width[0], common.width[1])};
        return Bounds3f.Expand(b, Math.max(width[0], width[1]) * 0.5f);
    }

    @Override
    public HitResult Intersect(Ray r, boolean testAlphaTexture) {
        return Intersect(r, testAlphaTexture, false);
    }

    private HitResult Intersect(Ray r, boolean testAlphaTexture, boolean isShadow) {
        hitsPerTest.incrementDenom(1); // ++nTests
        // Transform _Ray_ to object space
         Ray ray = WorldToObject.xform(r);

        // Compute object-space control points for curve segment, _cpObj_
        Point3f[] cpObj = {BlossomBezier(common.cpObj, uMin, uMin, uMin), BlossomBezier(common.cpObj, uMin, uMin, uMax),
            BlossomBezier(common.cpObj, uMin, uMax, uMax), BlossomBezier(common.cpObj, uMax, uMax, uMax) };

        // Project curve control points to plane perpendicular to ray

        // Be careful to set the "up" direction passed to LookAt() to equal the
        // vector from the first to the last control points.  In turn, this
        // helps orient the curve to be roughly parallel to the x axis in the
        // ray coordinate system.
        //
        // In turn (especially for curves that are approaching stright lines),
        // we get curve bounds with minimal extent in y, which in turn lets us
        // early out more quickly in recursiveIntersect().
        Vector3f dx = Vector3f.Cross(ray.d, cpObj[3].subtract(cpObj[0]));
        if (dx.LengthSquared() == 0) {
            // If the ray and the vector between the first and last control
            // points are parallel, dx will be zero.  Generate an arbitrary xy
            // orientation for the ray coordinate system so that intersection
            // tests can proceeed in this unusual case.
            Vector3f.CoordSystem cs = Vector3f.CoordinateSystem(ray.d);
            dx = cs.v2;
        }

        Transform objectToRay = Transform.LookAt(ray.o, ray.o.add(ray.d), dx);
        Point3f[] cp = {objectToRay.xform(cpObj[0]), objectToRay.xform(cpObj[1]),
                objectToRay.xform(cpObj[2]), objectToRay.xform(cpObj[3])};

        // Before going any further, see if the ray's bounding box intersects
        // the curve's bounding box. We start with the y dimension, since the y
        // extent is generally the smallest (and is often tiny) due to our
        // careful orientation of the ray coordinate ysstem above.
        float maxWidth = Math.max(Pbrt.Lerp(uMin, common.width[0], common.width[1]),
                Pbrt.Lerp(uMax, common.width[0], common.width[1]));
        if (Math.max(Math.max(cp[0].y, cp[1].y), Math.max(cp[2].y, cp[3].y)) + 0.5f * maxWidth < 0 || 
                Math.min(Math.min(cp[0].y, cp[1].y), Math.min(cp[2].y, cp[3].y)) - 0.5f * maxWidth > 0)
            return null;

        // Check for non-overlap in x.
        if (Math.max(Math.max(cp[0].x, cp[1].x), Math.max(cp[2].x, cp[3].x)) + 0.5f * maxWidth < 0 ||
                Math.min(Math.min(cp[0].x, cp[1].x), Math.min(cp[2].x, cp[3].x)) - 0.5f * maxWidth > 0)
            return null;

        // Check for non-overlap in z.
        float rayLength = ray.d.Length();
        float zMax = rayLength * ray.tMax;
        if (Math.max(Math.max(cp[0].z, cp[1].z), Math.max(cp[2].z, cp[3].z)) + 0.5f * maxWidth < 0 ||
                Math.min(Math.min(cp[0].z, cp[1].z), Math.min(cp[2].z, cp[3].z)) - 0.5f * maxWidth > zMax)
            return null;

        // Compute refinement depth for curve, _maxDepth_
        float L0 = 0;
        for (int i = 0; i < 2; ++i)
            L0 = Math.max(L0, Math.max(Math.max(Math.abs(cp[i].x - 2 * cp[i + 1].x + cp[i + 2].x),
                Math.abs(cp[i].y - 2 * cp[i + 1].y + cp[i + 2].y)), Math.abs(cp[i].z - 2 * cp[i + 1].z + cp[i + 2].z)));

        float eps = Math.max(common.width[0], common.width[1]) * .05f;  // width / 20
        // Compute log base 4 by dividing log2 in half.
        int r0 = Log2(1.41421356237f * 6.f * L0 / (8.f * eps)) / 2;
        int maxDepth = Pbrt.Clamp(r0, 0, 10);
        refinementLevel.ReportValue(maxDepth);

        return recursiveIntersect(ray, cp, 0, Transform.Inverse(objectToRay), uMin, uMax, maxDepth, isShadow);
    }

    public boolean IntersectP(Ray ray, boolean testAlphaTexture) {
        return (Intersect(ray, testAlphaTexture, true) != null);
    }

    @Override
    public float Area() {
        // Compute object-space control points for curve segment, _cpObj_
        Point3f[] cpObj = {BlossomBezier(common.cpObj, uMin, uMin, uMin), BlossomBezier(common.cpObj, uMin, uMin, uMax),
                BlossomBezier(common.cpObj, uMin, uMax, uMax), BlossomBezier(common.cpObj, uMax, uMax, uMax) };
        float width0 = Pbrt.Lerp(uMin, common.width[0], common.width[1]);
        float width1 = Pbrt.Lerp(uMax, common.width[0], common.width[1]);
        float avgWidth = (width0 + width1) * 0.5f;
        float approxLength = 0;
        for (int i = 0; i < 3; ++i)
            approxLength += Point3f.Distance(cpObj[i], cpObj[i + 1]);
        return approxLength * avgWidth;
    }

    @Override
    public SampleResult Sample(Point2f u) {
        Error.Error("Curve::Sample not implemented.");
        return null;

    }

    private HitResult recursiveIntersect(Ray ray, Point3f[] cp, int cpi, Transform rayToObject, float u0, float u1, int depth, boolean isShadow) {
        HitResult hr = new HitResult();

        float rayLength = ray.d.Length();

        if (depth > 0) {
            // Split curve segment into sub-segments and test for intersection
            Point3f[] cpSplit = new Point3f[7];
            cpSplit = SubdivideBezier(cp, cpSplit);

            // For each of the two segments, see if the ray's bounding box
            // overlaps the segment before recursively checking for
            // intersection with it.
            boolean hit = false;
            float[] u = {u0, (u0 + u1) / 2, u1};
            // Pointer to the 4 control points for the current segment.
            Point3f[] cps = cpSplit;
            int cpsi = cpi;
            for (int seg = 0; seg < 2; ++seg, cpsi += 3) {
                float maxWidth = Math.max(Pbrt.Lerp(u[seg], common.width[0], common.width[1]), Pbrt.Lerp(u[seg + 1], common.width[0], common.width[1]));

                // As above, check y first, since it most commonly lets us exit
                // out early.
                if (Math.max(Math.max(cps[cpsi+0].y, cps[cpsi+1].y), Math.max(cps[cpsi+2].y, cps[cpsi+3].y)) + 0.5 * maxWidth < 0 ||
                        Math.min(Math.min(cps[cpsi+0].y, cps[cpsi+1].y), Math.min(cps[cpsi+2].y, cps[cpsi+3].y)) - 0.5 * maxWidth > 0)
                    continue;

                if (Math.max(Math.max(cps[cpsi+0].x, cps[cpsi+1].x), Math.max(cps[cpsi+2].x, cps[cpsi+3].x)) + 0.5 * maxWidth < 0 ||
                        Math.min(Math.min(cps[cpsi+0].x, cps[cpsi+1].x), Math.min(cps[cpsi+2].x, cps[cpsi+3].x)) - 0.5 * maxWidth > 0)
                    continue;

                float zMax = rayLength * ray.tMax;
                if (Math.max(Math.max(cps[cpsi+0].z, cps[cpsi+1].z), Math.max(cps[cpsi+2].z, cps[cpsi+3].z)) + 0.5 * maxWidth < 0 ||
                        Math.min(Math.min(cps[cpsi+0].z, cps[cpsi+1].z), Math.min(cps[cpsi+2].z, cps[cpsi+3].z)) - 0.5 * maxWidth > zMax)
                    continue;

                hr = recursiveIntersect(ray, cps, cpsi, rayToObject, u[seg], u[seg + 1], depth - 1, isShadow);
                // If we found an intersection and this is a shadow ray,
                // we can exit out immediately.
                if (hr.isect != null && isShadow) return hr;
            }
            return hr;
        }
        else {
            // Intersect ray with curve segment

            // Test ray against segment endpoint boundaries

            // Test sample point against tangent perpendicular at curve start
            float edge = (cp[cpi+1].y - cp[cpi+0].y) * -cp[cpi+0].y + cp[cpi+0].x * (cp[cpi+0].x - cp[cpi+1].x);
            if (edge < 0) return null;

            // Test sample point against tangent perpendicular at curve end
            edge = (cp[cpi+2].y - cp[cpi+3].y) * -cp[cpi+3].y + cp[cpi+3].x * (cp[cpi+3].x - cp[cpi+2].x);
            if (edge < 0) return null;

            // Compute line $w$ that gives minimum distance to sample point
            Vector2f segmentDirection = (new Point2f(cp[cpi+3])).subtract(new Point2f(cp[cpi+0]));
            float denom = segmentDirection.LengthSquared();
            if (denom == 0) return null;
            float w = Vector2f.Dot((new Vector2f(cp[cpi+0])).negate(), segmentDirection) / denom;

            // Compute $u$ coordinate of curve intersection point and _hitWidth_
            float u = Pbrt.Clamp(Pbrt.Lerp(w, u0, u1), u0, u1);
            float hitWidth = Pbrt.Lerp(u, common.width[0], common.width[1]);
            Normal3f nHit = new Normal3f();
            if (common.type == CurveType.Ribbon) {
                // Scale _hitWidth_ based on ribbon orientation
                float sin0 = (float)Math.sin((1 - u) * common.normalAngle) * common.invSinNormalAngle;
                float sin1 = (float)Math.sin(u * common.normalAngle) * common.invSinNormalAngle;
                nHit = common.n[0].scale(sin0).add(common.n[1].scale(sin1));
                hitWidth *= Normal3f.AbsDot(nHit, ray.d) / rayLength;
            }

            // Test intersection point against curve width
            BezierPoint bp = EvalBezier(cp, Pbrt.Clamp(w, 0, 1));
            Vector3f dpcdw = bp.deriv;
            Point3f pc = bp.point;
            float ptCurveDist2 = pc.x * pc.x + pc.y * pc.y;
            if (ptCurveDist2 > hitWidth * hitWidth * .25f) return null;
            float zMax = rayLength * ray.tMax;
            if (pc.z < 0 || pc.z > zMax) return null;

            // Compute $v$ coordinate of curve intersection point
            float ptCurveDist = (float)Math.sqrt(ptCurveDist2);
            float edgeFunc = dpcdw.x * -pc.y + pc.x * dpcdw.y;
            float v = (edgeFunc > 0) ? 0.5f + ptCurveDist / hitWidth : 0.5f - ptCurveDist / hitWidth;

            // Compute hit _t_ and partial derivatives for curve intersection
            if (!isShadow) {
                // FIXME: this tHit isn't quite right for ribbons...
                hr.tHit = pc.z / rayLength;
                // Compute error bounds for curve intersection
                Vector3f pError = new Vector3f(2 * hitWidth, 2 * hitWidth, 2 * hitWidth);

                // Compute $\dpdu$ and $\dpdv$ for curve intersection
                Vector3f dpdu, dpdv;
                bp = EvalBezier(common.cpObj, u);
                dpdu = bp.deriv;
                if (common.type == CurveType.Ribbon)
                    dpdv = Vector3f.Normalize(Vector3f.Cross(nHit, dpdu)).scale(hitWidth);
                else {
                    // Compute curve $\dpdv$ for flat and cylinder curves
                    Vector3f dpduPlane = Transform.Inverse(rayToObject).xform(dpdu);
                    Vector3f dpdvPlane = Vector3f.Normalize(new Vector3f(-dpduPlane.y, dpduPlane.x, 0)).scale(hitWidth);
                    if (common.type == CurveType.Cylinder) {
                        // Rotate _dpdvPlane_ to give cylindrical appearance
                        float theta = Pbrt.Lerp(v, -90, 90);
                        Transform rot = Transform.Rotate(-theta, dpduPlane);
                        dpdvPlane = rot.xform(dpdvPlane);
                    }
                    dpdv = rayToObject.xform(dpdvPlane);
                }
                hr.isect = ObjectToWorld.xform(new SurfaceInteraction(ray.at(pc.z), pError, new Point2f(u, v), ray.d.negate(), dpdu, dpdv,
                        new Normal3f(0, 0, 0), new Normal3f(0, 0, 0), ray.time, this));
            }
            hitsPerTest.incrementNumer(1); //++nHits;
            return hr;
        }
    }

    // Curve Utility Functions
    private static Point3f BlossomBezier(Point3f[] p, float u0, float u1, float u2) {
        Point3f[] a = {Point3f.Lerp(u0, p[0], p[1]), Point3f.Lerp(u0, p[1], p[2]), Point3f.Lerp(u0, p[2], p[3])};
        Point3f[] b = {Point3f.Lerp(u1, a[0], a[1]), Point3f.Lerp(u1, a[1], a[2])};
        return Point3f.Lerp(u2, b[0], b[1]);
    }

    private static Point3f[] SubdivideBezier(Point3f[] cp, Point3f[] cpSplit) {
        cpSplit[0] = cp[0];
        cpSplit[1] = (cp[0].add(cp[1])).invScale(2);
        cpSplit[2] = (cp[0].add(cp[1].scale(2)).add(cp[2])).invScale(4);
        cpSplit[3] = (cp[0].add(cp[1].scale(3)).add(cp[2].scale(3)).add(cp[3])).invScale(8);
        cpSplit[4] = (cp[1].add(cp[2].scale(2)).add(cp[3])).invScale(4);
        cpSplit[5] = (cp[2].add(cp[3])).invScale(2);
        cpSplit[6] = cp[3];
        return cpSplit;
    }

    private static class BezierPoint {
        Point3f point;
        Vector3f deriv;
    }
    private static BezierPoint EvalBezier(Point3f[] cp, float u) {
        BezierPoint bp = new BezierPoint();
        Point3f[] cp1 = {Point3f.Lerp(u, cp[0], cp[1]), Point3f.Lerp(u, cp[1], cp[2]), Point3f.Lerp(u, cp[2], cp[3])};
        Point3f[] cp2 = {Point3f.Lerp(u, cp1[0], cp1[1]), Point3f.Lerp(u, cp1[1], cp1[2])};
        bp.deriv = (cp2[1].subtract(cp2[0])).scale(3);
        bp.point = Point3f.Lerp(u, cp2[0], cp2[1]);
        return bp;
    }

    private static ArrayList<Shape> CreateCurve(Transform o2w,  Transform w2o, boolean reverseOrientation,
        Point3f[] c, float w0, float w1, CurveType type, Normal3f[] norm, int splitDepth) {
        CurveCommon common = new CurveCommon(c, w0, w1, type, norm);
        int nSegments = 1 << splitDepth;
        ArrayList<Shape> segments = new ArrayList<>(nSegments);
        for (int i = 0; i < nSegments; ++i) {
            float uMin = i / (float)nSegments;
            float uMax = (i + 1) / (float)nSegments;
            segments.add(new Curve(o2w, w2o, reverseOrientation, common, uMin, uMax));
            nSplitCurves.increment();
        }
        curveBytes.increment(CurveCommon.sizeof() + nSegments * Curve.sizeof());
        return segments;
    }

    private static int sizeof() {
        return 1;
    }

    private int Log2(float v) {
        if (v < 1) return 0;
        int bits = Float.floatToRawIntBits(v);
        // https://graphics.stanford.edu/~seander/bithacks.html#IntegerLog
        // (With an additional add so get round-to-nearest rather than
        // round down.)
        return (bits >> 23) - 127 + ((bits & (1 << 22)) != 0 ? 1 : 0);
    }

    private final CurveCommon common;
    private final float uMin, uMax;

    private static Stats.MemoryCounter curveBytes = new Stats.MemoryCounter("Memory/Curves");
    private static Stats.Percent hitsPerTest = new Stats.Percent("Intersections/Ray-curve intersection tests"); // nHits, nTests
    private static Stats.IntegerDistribution refinementLevel = new Stats.IntegerDistribution("Intersections/Curve refinement level");
    private static Stats.Counter nCurves = new Stats.Counter("Scene/Curves");
    private static Stats.Counter nSplitCurves = new Stats.Counter("Scene/Split curves");
}