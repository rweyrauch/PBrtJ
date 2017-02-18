
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class Transform {

    // Transform Private Data
    private Matrix4x4 m, mInv;

    // Transform Public Methods
    public Transform() {
        m = new Matrix4x4();
        mInv = new Matrix4x4();
    }

    public Transform(float mat[][]) {
        m = new Matrix4x4(mat[0][0], mat[0][1], mat[0][2], mat[0][3], mat[1][0],
                mat[1][1], mat[1][2], mat[1][3], mat[2][0], mat[2][1],
                mat[2][2], mat[2][3], mat[3][0], mat[3][1], mat[3][2],
                mat[3][3]);
        mInv = Matrix4x4.Inverse(m);
    }
    public Transform(Matrix4x4 m) {
        this.m = m;
        this.mInv = Matrix4x4.Inverse(m);
    }
    public Transform(Matrix4x4 m, Matrix4x4 mInv) {
        this.m = m;
        this.mInv = mInv;
    }

    public static Transform Inverse(Transform t) {
        return new Transform(t.mInv, t.m);
    }
    public static Transform Transpose(Transform t) {
        return new Transform(Matrix4x4.Transpose(t.m), Matrix4x4.Transpose(t.mInv));
    }
    public boolean equal(Transform t) {
        return t.m == m && t.mInv == mInv;
    }
    public boolean notEqual(Transform t) {
        return t.m != m || t.mInv != mInv;
    }
    public boolean less(Transform t2) {
        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 4; ++j) {
                if (m.m[i][j] < t2.m.m[i][j]) return true;
                if (m.m[i][j] > t2.m.m[i][j]) return false;
            }
        }
        return false;
    }

    public boolean IsIdentity() {
        return (m.m[0][0] == 1.f && m.m[0][1] == 0.f && m.m[0][2] == 0.f &&
                m.m[0][3] == 0.f && m.m[1][0] == 0.f && m.m[1][1] == 1.f &&
                m.m[1][2] == 0.f && m.m[1][3] == 0.f && m.m[2][0] == 0.f &&
                m.m[2][1] == 0.f && m.m[2][2] == 1.f && m.m[2][3] == 0.f &&
                m.m[3][0] == 0.f && m.m[3][1] == 0.f && m.m[3][2] == 0.f &&
                m.m[3][3] == 1.f);
    }
    Matrix4x4 GetMatrix() { return m; }
    Matrix4x4 GetInverseMatrix() { return mInv; }

    private static boolean NOT_ONE(float x) {
        return (x < 0.999f || x > 1.001f);
    }
    boolean HasScale() {
        float la2 = xform(new Vector3f(1, 0, 0)).LengthSquared();
        float lb2 = xform(new Vector3f(0, 1, 0)).LengthSquared();
        float lc2 = xform(new Vector3f(0, 0, 1)).LengthSquared();
        return (NOT_ONE(la2) || NOT_ONE(lb2) || NOT_ONE(lc2));
    }

    public Point3f xform(Point3f p) {
        float x = p.x, y = p.y, z = p.z;
        float xp = m.m[0][0] * x + m.m[0][1] * y + m.m[0][2] * z + m.m[0][3];
        float yp = m.m[1][0] * x + m.m[1][1] * y + m.m[1][2] * z + m.m[1][3];
        float zp = m.m[2][0] * x + m.m[2][1] * y + m.m[2][2] * z + m.m[2][3];
        float wp = m.m[3][0] * x + m.m[3][1] * y + m.m[3][2] * z + m.m[3][3];
        assert (wp != 0);
        if (wp == 1)
            return new Point3f(xp, yp, zp);
        else
            return new Point3f(xp, yp, zp).invScale(wp);
    }

    public Vector3f xform(Vector3f v) {
        float x = v.x, y = v.y, z = v.z;
        return new Vector3f(m.m[0][0] * x + m.m[0][1] * y + m.m[0][2] * z,
                m.m[1][0] * x + m.m[1][1] * y + m.m[1][2] * z,
                m.m[2][0] * x + m.m[2][1] * y + m.m[2][2] * z);
    }

    public Normal3f xform(Normal3f n) {
        float x = n.x, y = n.y, z = n.z;
        return new Normal3f(mInv.m[0][0] * x + mInv.m[1][0] * y + mInv.m[2][0] * z,
        mInv.m[0][1] * x + mInv.m[1][1] * y + mInv.m[2][1] * z,
                mInv.m[0][2] * x + mInv.m[1][2] * y + mInv.m[2][2] * z);

    }
    public Ray xform(Ray r) {
        Vector3f oError = absError(r.o);
        Point3f o = xform(r.o);
        Vector3f d = xform(r.d);
        // Offset ray origin to edge of error bounds and compute _tMax_
        Float lengthSquared = d.LengthSquared();
        Float tMax = r.tMax;
        if (lengthSquared > 0) {
            float dt = Vector3f.Dot(Vector3f.Abs(d), oError) / lengthSquared;
            o.increment(d.scale(dt));
            tMax -= dt;
        }
        return new Ray(o, d, tMax, r.time, r.medium);
    }
    public RayDifferential xform(RayDifferential r) {
        Ray tr = xform(new Ray(r.o, r.d, r.tMax, r.time, r.medium));
        RayDifferential ret = new RayDifferential(tr.o, tr.d, tr.tMax, tr.time, tr.medium);
        ret.hasDifferentials = r.hasDifferentials;
        ret.rxOrigin = xform(r.rxOrigin);
        ret.ryOrigin = xform(r.ryOrigin);
        ret.rxDirection = xform(r.rxDirection);
        ret.ryDirection = xform(r.ryDirection);
        return ret;

    }
    public Bounds3f xform(Bounds3f b) {
        Bounds3f ret = new Bounds3f(xform(new Point3f(b.pMin.x, b.pMin.y, b.pMin.z)));
        ret = Bounds3f.Union(ret, xform(new Point3f(b.pMax.x, b.pMin.y, b.pMin.z)));
        ret = Bounds3f.Union(ret, xform(new Point3f(b.pMin.x, b.pMax.y, b.pMin.z)));
        ret = Bounds3f.Union(ret, xform(new Point3f(b.pMin.x, b.pMin.y, b.pMax.z)));
        ret = Bounds3f.Union(ret, xform(new Point3f(b.pMin.x, b.pMax.y, b.pMax.z)));
        ret = Bounds3f.Union(ret, xform(new Point3f(b.pMax.x, b.pMax.y, b.pMin.z)));
        ret = Bounds3f.Union(ret, xform(new Point3f(b.pMax.x, b.pMin.y, b.pMax.z)));
        ret = Bounds3f.Union(ret, xform(new Point3f(b.pMax.x, b.pMax.y, b.pMax.z)));
        return ret;
    }
    public Transform concatenate(Transform t2) {
        return new Transform(Matrix4x4.Mul(m, t2.m), Matrix4x4.Mul(t2.mInv, mInv));
    }

    public boolean SwapsHandedness() {
        float det = m.m[0][0] * (m.m[1][1] * m.m[2][2] - m.m[1][2] * m.m[2][1]) -
                m.m[0][1] * (m.m[1][0] * m.m[2][2] - m.m[1][2] * m.m[2][0]) +
                m.m[0][2] * (m.m[1][0] * m.m[2][1] - m.m[1][1] * m.m[2][0]);
        return det < 0;
    }

    public SurfaceInteraction xform(SurfaceInteraction si) {
        SurfaceInteraction ret = new SurfaceInteraction();
        // Transform _p_ and _pError_ in _SurfaceInteraction_
        ret.p = xform(si.p);
        ret.pError = absError(si.p, si.pError);
        // Transform remaining members of _SurfaceInteraction_
        ret.n = Normal3f.Normalize(xform(si.n));
        ret.wo = Vector3f.Normalize(xform(si.wo));
        ret.time = si.time;
        ret.mediumInterface = si.mediumInterface;
        ret.uv = si.uv;
        ret.shape = si.shape;
        ret.dpdu = xform(si.dpdu);
        ret.dpdv = xform(si.dpdv);
        ret.dndu = xform(si.dndu);
        ret.dndv = xform(si.dndv);
        ret.shading.n = Normal3f.Normalize(xform(si.shading.n));
        ret.shading.dpdu = xform(si.shading.dpdu);
        ret.shading.dpdv = xform(si.shading.dpdv);
        ret.shading.dndu = xform(si.shading.dndu);
        ret.shading.dndv = xform(si.shading.dndv);
        ret.dudx = si.dudx;
        ret.dvdx = si.dvdx;
        ret.dudy = si.dudy;
        ret.dvdy = si.dvdy;
        ret.dpdx = xform(si.dpdx);
        ret.dpdy = xform(si.dpdy);
        ret.bsdf = si.bsdf;
        ret.bssrdf = si.bssrdf;
        ret.primitive = si.primitive;
        ret.shading.n = Normal3f.Faceforward(ret.shading.n, ret.n);
        return ret;
    }

    public Vector3f absError(Point3f p) {
        float x = p.x, y = p.y, z = p.z;
        // Compute absolute error for transformed point
        float xAbsSum = (Math.abs(m.m[0][0] * x) + Math.abs(m.m[0][1] * y) +
                Math.abs(m.m[0][2] * z) + Math.abs(m.m[0][3]));
        float yAbsSum = (Math.abs(m.m[1][0] * x) + Math.abs(m.m[1][1] * y) +
                Math.abs(m.m[1][2] * z) + Math.abs(m.m[1][3]));
        float zAbsSum = (Math.abs(m.m[2][0] * x) + Math.abs(m.m[2][1] * y) +
                Math.abs(m.m[2][2] * z) + Math.abs(m.m[2][3]));
        float g3 = Pbrt.gamma(3);
        return new Vector3f(xAbsSum * g3, yAbsSum * g3, zAbsSum * g3);
    }

    public Vector3f absError(Point3f pt, Vector3f ptError) {
        float x = pt.x, y = pt.y, z = pt.z;
        float g3 = Pbrt.gamma(3);
        float ex = (g3 + 1) * (Math.abs(m.m[0][0]) * ptError.x + Math.abs(m.m[0][1]) * ptError.y +
                Math.abs(m.m[0][2]) * ptError.z) +
                g3 * (Math.abs(m.m[0][0] * x) + Math.abs(m.m[0][1] * y) +
                Math.abs(m.m[0][2] * z) + Math.abs(m.m[0][3]));
        float ey = (g3 + 1) * (Math.abs(m.m[1][0]) * ptError.x + Math.abs(m.m[1][1]) * ptError.y +
                Math.abs(m.m[1][2]) * ptError.z) +
                g3 * (Math.abs(m.m[1][0] * x) + Math.abs(m.m[1][1] * y) +
                Math.abs(m.m[1][2] * z) + Math.abs(m.m[1][3]));
        float ez = (g3 + 1) * (Math.abs(m.m[2][0]) * ptError.x + Math.abs(m.m[2][1]) * ptError.y +
                Math.abs(m.m[2][2]) * ptError.z) +
                g3 * (Math.abs(m.m[2][0] * x) + Math.abs(m.m[2][1] * y) +
                Math.abs(m.m[2][2] * z) + Math.abs(m.m[2][3]));
        return new Vector3f(ex, ey, ez);
    }

    public Vector3f absError(Vector3f v) {
        float x = v.x, y = v.y, z = v.z;
        float g3 = Pbrt.gamma(3);
        float ex =
                g3 * (Math.abs(m.m[0][0] * v.x) + Math.abs(m.m[0][1] * v.y) +
                Math.abs(m.m[0][2] * v.z));
        float ey =
                g3 * (Math.abs(m.m[1][0] * v.x) + Math.abs(m.m[1][1] * v.y) +
                Math.abs(m.m[1][2] * v.z));
        float ez =
                g3 * (Math.abs(m.m[2][0] * v.x) + Math.abs(m.m[2][1] * v.y) +
                Math.abs(m.m[2][2] * v.z));
        return new Vector3f(ex, ey, ez);
    }

    public Vector3f absError(Vector3f v, Vector3f vError) {
        float x = v.x, y = v.y, z = v.z;
        float g3 = Pbrt.gamma(3);
        float ex = (g3 + 1) * (Math.abs(m.m[0][0]) * vError.x + Math.abs(m.m[0][1]) * vError.y +
                Math.abs(m.m[0][2]) * vError.z) +
                g3 * (Math.abs(m.m[0][0] * v.x) + Math.abs(m.m[0][1] * v.y) +
                Math.abs(m.m[0][2] * v.z));
        float ey = (g3 + 1) * (Math.abs(m.m[1][0]) * vError.x + Math.abs(m.m[1][1]) * vError.y +
                Math.abs(m.m[1][2]) * vError.z) +
                g3 * (Math.abs(m.m[1][0] * v.x) + Math.abs(m.m[1][1] * v.y) +
                Math.abs(m.m[1][2] * v.z));
        float ez = (g3 + 1) * (Math.abs(m.m[2][0]) * vError.x + Math.abs(m.m[2][1]) * vError.y +
                Math.abs(m.m[2][2]) * vError.z) +
                g3 * (Math.abs(m.m[2][0] * v.x) + Math.abs(m.m[2][1] * v.y) +
                Math.abs(m.m[2][2] * v.z));
        return new Vector3f(ex, ey, ez);
    }

    public static Transform Translate(Vector3f delta) {
        Matrix4x4 m = new Matrix4x4(1, 0, 0, delta.x, 0, 1, 0, delta.y, 0, 0, 1, delta.z, 0, 0, 0,
                1);
        Matrix4x4 minv = new Matrix4x4(1, 0, 0, -delta.x, 0, 1, 0, -delta.y, 0, 0, 1, -delta.z, 0,
                0, 0, 1);
        return new Transform(m, minv);
    }
    public static Transform Scale(float x, float y, float z) {
        Matrix4x4 m = new Matrix4x4(x, 0, 0, 0, 0, y, 0, 0, 0, 0, z, 0, 0, 0, 0, 1);
        Matrix4x4 minv = new Matrix4x4(1 / x, 0, 0, 0, 0, 1 / y, 0, 0, 0, 0, 1 / z, 0, 0, 0, 0, 1);
        return new Transform(m, minv);
    }
    public static Transform RotateX(float theta) {
        float sinTheta = (float)Math.sin(Math.toRadians(theta));
        float cosTheta = (float)Math.cos(Math.toRadians(theta));
        Matrix4x4 m = new Matrix4x4(1, 0, 0, 0, 0, cosTheta, -sinTheta, 0, 0, sinTheta, cosTheta, 0,
                0, 0, 0, 1);
        return new Transform(m, Matrix4x4.Transpose(m));
    }
    public static Transform RotateY(float theta) {
        float sinTheta = (float)Math.sin(Math.toRadians(theta));
        float cosTheta = (float)Math.cos(Math.toRadians(theta));
        Matrix4x4 m = new Matrix4x4(cosTheta, 0, sinTheta, 0, 0, 1, 0, 0, -sinTheta, 0, cosTheta, 0,
                0, 0, 0, 1);
        return new Transform(m, Matrix4x4.Transpose(m));
    }
    public static Transform RotateZ(float theta) {
        float sinTheta = (float)Math.sin(Math.toRadians(theta));
        float cosTheta = (float)Math.cos(Math.toRadians(theta));
        Matrix4x4 m = new Matrix4x4(cosTheta, -sinTheta, 0, 0, sinTheta, cosTheta, 0, 0, 0, 0, 1, 0,
                0, 0, 0, 1);
        return new Transform(m, Matrix4x4.Transpose(m));
    }
    public static Transform Rotate(float theta, Vector3f axis) {
        Vector3f a = Vector3f.Normalize(axis);
        float sinTheta = (float)Math.sin(Math.toRadians(theta));
        float cosTheta = (float)Math.cos(Math.toRadians(theta));
        Matrix4x4 m = new Matrix4x4();
        // Compute rotation of first basis vector
        m.m[0][0] = a.x * a.x + (1 - a.x * a.x) * cosTheta;
        m.m[0][1] = a.x * a.y * (1 - cosTheta) - a.z * sinTheta;
        m.m[0][2] = a.x * a.z * (1 - cosTheta) + a.y * sinTheta;
        m.m[0][3] = 0;

        // Compute rotations of second and third basis vectors
        m.m[1][0] = a.x * a.y * (1 - cosTheta) + a.z * sinTheta;
        m.m[1][1] = a.y * a.y + (1 - a.y * a.y) * cosTheta;
        m.m[1][2] = a.y * a.z * (1 - cosTheta) - a.x * sinTheta;
        m.m[1][3] = 0;

        m.m[2][0] = a.x * a.z * (1 - cosTheta) - a.y * sinTheta;
        m.m[2][1] = a.y * a.z * (1 - cosTheta) + a.x * sinTheta;
        m.m[2][2] = a.z * a.z + (1 - a.z * a.z) * cosTheta;
        m.m[2][3] = 0;
        return new Transform(m, Matrix4x4.Transpose(m));
    }
    public static Transform LookAt(Point3f pos, Point3f look, Vector3f up) {
        Matrix4x4 cameraToWorld = new Matrix4x4();
        // Initialize fourth column of viewing matrix
        cameraToWorld.m[0][3] = pos.x;
        cameraToWorld.m[1][3] = pos.y;
        cameraToWorld.m[2][3] = pos.z;
        cameraToWorld.m[3][3] = 1;

        // Initialize first three columns of viewing matrix
        Vector3f dir = Vector3f.Normalize(look.subtract(pos));
        if (Vector3f.Cross(Vector3f.Normalize(up), dir).Length() == 0) {
            Error.Error("\"up\" vector (%f, %f, %f) and viewing direction (%f, %f, %f) passed to LookAt are pointing in the same direction.  Using the identity transformation.",
                    up.x, up.y, up.z, dir.x, dir.y, dir.z);
            return new Transform();
        }
        Vector3f left = Vector3f.Normalize(Vector3f.Cross(Vector3f.Normalize(up), dir));
        Vector3f newUp = Vector3f.Cross(dir, left);
        cameraToWorld.m[0][0] = left.x;
        cameraToWorld.m[1][0] = left.y;
        cameraToWorld.m[2][0] = left.z;
        cameraToWorld.m[3][0] = 0;
        cameraToWorld.m[0][1] = newUp.x;
        cameraToWorld.m[1][1] = newUp.y;
        cameraToWorld.m[2][1] = newUp.z;
        cameraToWorld.m[3][1] = 0;
        cameraToWorld.m[0][2] = dir.x;
        cameraToWorld.m[1][2] = dir.y;
        cameraToWorld.m[2][2] = dir.z;
        cameraToWorld.m[3][2] = 0;
        return new Transform(Matrix4x4.Inverse(cameraToWorld), cameraToWorld);
    }
    public static Transform Orthographic(float znear, float zfar) {
        return Scale(1, 1, 1 / (zfar - znear)).concatenate(Translate(new Vector3f(0, 0, -znear)));
    }
    public static Transform Perspective(float fov, float znear, float zfar) {
        // Perform projective divide for perspective projection
        Matrix4x4 persp = new Matrix4x4(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, zfar / (zfar - znear), -zfar * znear / (zfar - znear),
                0, 0, 1, 0);

        // Scale canonical perspective view to specified field of view
        float invTanAng = 1.0f / (float)Math.tan(Math.toRadians(fov) / 2);
        return Scale(invTanAng, invTanAng, 1).concatenate(new Transform(persp));
    }

}