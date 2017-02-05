
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
    public Transform() {}
    public Transform(float mat[][]) {
        m = Matrix4x4(mat[0][0], mat[0][1], mat[0][2], mat[0][3], mat[1][0],
                mat[1][1], mat[1][2], mat[1][3], mat[2][0], mat[2][1],
                mat[2][2], mat[2][3], mat[3][0], mat[3][1], mat[3][2],
                mat[3][3]);
        mInv = Inverse(m);
    }
    Transform(Matrix4x4 m) : m(m), mInv(Inverse(m)) {}
    Transform(Matrix4x4 m, Matrix4x4 mInv) : m(m), mInv(mInv) {}
    void Print(FILE *f) const;
    public static Transform Inverse(Transform t) {
        return Transform(t.mInv, t.m);
    }
    public static Transform Transpose(Transform t) {
        return Transform(Transpose(t.m), Transpose(t.mInv));
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
    boolean HasScale() {
        float la2 = (*this)(Vector3f(1, 0, 0)).LengthSquared();
        float lb2 = (*this)(Vector3f(0, 1, 0)).LengthSquared();
        float lc2 = (*this)(Vector3f(0, 0, 1)).LengthSquared();
#define NOT_ONE(x) ((x) < .999f || (x) > 1.001f)
        return (NOT_ONE(la2) || NOT_ONE(lb2) || NOT_ONE(lc2));
#undef NOT_ONE
    }

    public Point3f xform(Point3f p) {

    }

    public Vector3f xform(Vector3f v) {

    }

    public Normal3f xform(Normal3f n) {

    }
    public Ray xform(Ray r) {

    }
    public RayDifferential xform(RayDifferential r) {

    }
    public Bounds3<Float> xform(Bounds3<Float> b) {

    }
    public Transform concatenate(Transform t2) {

    }

    boolean SwapsHandedness() {

    }

    public SurfaceInteraction xform(SurfaceInteraction si) {

    }

    public Point3<T> operator()(Point3<T> pt, Vector3<T> *absError) {

    }

    public Point3<T> operator()(Point3<T> p, Vector3<T> pError, Vector3<T> *pTransError) {

    }

    public Vector3<T> operator()(Vector3<T> v, Vector3<T> *vTransError) {

    }

    public Vector3<T> operator()(Vector3<T> v, Vector3<T> vError, Vector3<T> *vTransError) {

    }
    public Ray operator()(Ray r, Vector3f *oError, Vector3f *dError) {

    }
    public Ray operator()(Ray r, Vector3f oErrorIn, Vector3f dErrorIn, Vector3f *oErrorOut, Vector3f *dErrorOut) {

    }

    public static Transform Translate(Vector3f delta) {

    }
    public static Transform Scale(float x, float y, float z) {

    }
    public static Transform RotateX(float theta) {

    }
    public static Transform RotateY(float theta) {

    }
    public static Transform RotateZ(float theta) {

    }
    public static Transform Rotate(float theta, Vector3f axis) {

    }
    public static Transform LookAt(Point3f pos, Point3f look, Vector3f up) {

    }
    public static Transform Orthographic(float znear, float zfar) {

    }
    public static Transform Perspective(float fov, float znear, float zfar) {

    }

}