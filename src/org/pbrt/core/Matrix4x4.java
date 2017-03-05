/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class Matrix4x4 implements Cloneable {
    public float m[][] = new float[4][4];

    public Matrix4x4() {
        m[0][0] = m[1][1] = m[2][2] = m[3][3] = 1;
        m[0][1] = m[0][2] = m[0][3] = m[1][0] = m[1][2] = m[1][3] = m[2][0] = m[2][1] = m[2][3] = m[3][0] = m[3][1] = m[3][2] = 0;
    }

    public Matrix4x4(float mat[][]) {
        assert mat.length == 4;
        assert mat[0].length == 4;
        m = mat.clone();
    }

    public Matrix4x4(float t00, float t01, float t02, float t03, float t10, float t11,
                     float t12, float t13, float t20, float t21, float t22, float t23,
                     float t30, float t31, float t32, float t33) {
        m[0][0] = t00;
        m[0][1] = t01;
        m[0][2] = t02;
        m[0][3] = t03;
        m[1][0] = t10;
        m[1][1] = t11;
        m[1][2] = t12;
        m[1][3] = t13;
        m[2][0] = t20;
        m[2][1] = t21;
        m[2][2] = t22;
        m[2][3] = t23;
        m[3][0] = t30;
        m[3][1] = t31;
        m[3][2] = t32;
        m[3][3] = t33;
    }

    @Override
    public Matrix4x4 clone() {
        return new Matrix4x4(this.m);
    }

    public boolean equal(Matrix4x4 m2) {
        for (int i = 0; i < 4; ++i)
            for (int j = 0; j < 4; ++j)
                if (m[i][j] != m2.m[i][j]) return false;
        return true;
    }

    public boolean notEqual(Matrix4x4 m2) {
        for (int i = 0; i < 4; ++i)
            for (int j = 0; j < 4; ++j)
                if (m[i][j] != m2.m[i][j]) return true;
        return false;
    }

    public static Matrix4x4 Transpose(Matrix4x4 m) {
        return new Matrix4x4(m.m[0][0], m.m[1][0], m.m[2][0], m.m[3][0], m.m[0][1],
                m.m[1][1], m.m[2][1], m.m[3][1], m.m[0][2], m.m[1][2],
                m.m[2][2], m.m[3][2], m.m[0][3], m.m[1][3], m.m[2][3],
                m.m[3][3]);
    }

    public static Matrix4x4 Mul(Matrix4x4 m1, Matrix4x4 m2) {
        Matrix4x4 r = new Matrix4x4();
        for (int i = 0; i < 4; ++i)
            for (int j = 0; j < 4; ++j)
                r.m[i][j] = m1.m[i][0] * m2.m[0][j] + m1.m[i][1] * m2.m[1][j] +
                        m1.m[i][2] * m2.m[2][j] + m1.m[i][3] * m2.m[3][j];
        return r;
    }

    public static Matrix4x4 Inverse(Matrix4x4 m) {
        int indxc[] = {0, 0, 0, 0};
        int indxr[] = {0, 0, 0, 0};
        int ipiv[] = {0, 0, 0, 0};
        float minv[][] = m.m.clone();
        for (int i = 0; i < 4; i++) {
            int irow = 0, icol = 0;
            float big = 0.0f;
            // Choose pivot
            for (int j = 0; j < 4; j++) {
                if (ipiv[j] != 1) {
                    for (int k = 0; k < 4; k++) {
                        if (ipiv[k] == 0) {
                            if (Math.abs(minv[j][k]) >= big) {
                                big = Math.abs(minv[j][k]);
                                irow = j;
                                icol = k;
                            }
                        } else if (ipiv[k] > 1)
                            Error.Error("Singular matrix in MatrixInvert");
                    }
                }
            }
            ++ipiv[icol];
            // Swap rows _irow_ and _icol_ for pivot
            if (irow != icol) {
                for (int k = 0; k < 4; ++k) {
                    float temp = minv[irow][k];
                    minv[irow][k] = minv[icol][k];
                    minv[icol][k] = temp;
                }
            }
            indxr[i] = irow;
            indxc[i] = icol;
            if (minv[icol][icol] == 0.f) Error.Error("Singular matrix in MatrixInvert");

            // Set $m[icol][icol]$ to one by scaling row _icol_ appropriately
            float pivinv = 1.0f / minv[icol][icol];
            minv[icol][icol] = 1.0f;
            for (int j = 0; j < 4; j++) minv[icol][j] *= pivinv;

            // Subtract this row from others to zero out their columns
            for (int j = 0; j < 4; j++) {
                if (j != icol) {
                    float save = minv[j][icol];
                    minv[j][icol] = 0;
                    for (int k = 0; k < 4; k++) minv[j][k] -= minv[icol][k] * save;
                }
            }
        }
        // Swap columns to reflect permutation
        for (int j = 3; j >= 0; j--) {
            if (indxr[j] != indxc[j]) {
                for (int k = 0; k < 4; k++) {
                    float temp = minv[k][indxr[j]];
                    minv[k][indxr[j]] = minv[k][indxc[j]];
                    minv[k][indxc[j]] = temp;
                }
            }
        }
        return new Matrix4x4(minv);
    }

    public static Vector2f SolveLinearSystem2x2(float A[][], float B[]) {
        float det = A[0][0] * A[1][1] - A[0][1] * A[1][0];
        if (Math.abs(det) < 1e-10f) return null;
        float x0 = (A[1][1] * B[0] - A[0][1] * B[1]) / det;
        float x1 = (A[0][0] * B[1] - A[1][0] * B[0]) / det;
        if (Float.isNaN(x0) || Float.isNaN(x1)) return null;
        return new Vector2f(x0, x1);
    }
}