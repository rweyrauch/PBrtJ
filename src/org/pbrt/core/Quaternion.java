
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class Quaternion {
    // Quaternion Public Data
    public Vector3f v = new Vector3f();
    public float w;

    // Quaternion Public Methods
    public Quaternion() {
        v = new Vector3f(0, 0, 0);
        w = 1;
    }
    public Quaternion(Vector3f v, float w) {
        this.v = v;
        this.w = w;
    }
    public void increment(Quaternion q) {
        v.increment(q.v);
        w += q.w;
    }
    public Quaternion add(Quaternion q) {
        Quaternion ret = new Quaternion(v.add(q.v), w+q.w);
        return ret;
    }
    public static Quaternion add(Quaternion q1, Quaternion q2) {
        Quaternion ret = q1.add(q2);
        return ret;
    }
    public Quaternion negate() {
        Quaternion ret = new Quaternion(v.negate(), -w);
        return ret;
    }
    public Quaternion subtract(Quaternion q) {
        Quaternion ret = new Quaternion(v.subtract(q.v), w-q.w);
        return ret;
    }
    public static Quaternion subtract(Quaternion q1, Quaternion q2) {
        Quaternion ret = q1.subtract(q2);
        return ret;
    }
    public Quaternion scale(float f) {
        Quaternion ret = new Quaternion(v.scale(f), w * f);
        return ret;
    }
    public Quaternion invScale(float f) {
        float invf = 1 / f;
        Quaternion ret = new Quaternion(v.scale(invf), w * invf);
        return ret;
    }
    public Transform ToTransform() {
        float xx = v.x * v.x, yy = v.y * v.y, zz = v.z * v.z;
        float xy = v.x * v.y, xz = v.x * v.z, yz = v.y * v.z;
        float wx = v.x * w, wy = v.y * w, wz = v.z * w;

        Matrix4x4 m = new Matrix4x4();
        m.m[0][0] = 1 - 2 * (yy + zz);
        m.m[0][1] = 2 * (xy + wz);
        m.m[0][2] = 2 * (xz - wy);
        m.m[1][0] = 2 * (xy - wz);
        m.m[1][1] = 1 - 2 * (xx + zz);
        m.m[1][2] = 2 * (yz + wx);
        m.m[2][0] = 2 * (xz + wy);
        m.m[2][1] = 2 * (yz - wx);
        m.m[2][2] = 1 - 2 * (xx + yy);

        // Transpose since we are left-handed.  Ugh.
        return new Transform(Matrix4x4.Transpose(m), m);
    }

    public Quaternion(Transform t) {
        Matrix4x4 m = t.GetMatrix();
        float trace = m.m[0][0] + m.m[1][1] + m.m[2][2];
        if (trace > 0.f) {
            // Compute w from matrix trace, then xyz
            // 4w^2 = m[0][0] + m[1][1] + m[2][2] + m[3][3] (but m[3][3] == 1)
            float s = (float)Math.sqrt(trace + 1.0f);
            w = s / 2.0f;
            s = 0.5f / s;
            v.x = (m.m[2][1] - m.m[1][2]) * s;
            v.y = (m.m[0][2] - m.m[2][0]) * s;
            v.z = (m.m[1][0] - m.m[0][1]) * s;
        } else {
            // Compute largest of $x$, $y$, or $z$, then remaining components
            int nxt[] = {1, 2, 0};
            float q[] = {0, 0, 0};
            int i = 0;
            if (m.m[1][1] > m.m[0][0]) i = 1;
            if (m.m[2][2] > m.m[i][i]) i = 2;
            int j = nxt[i];
            int k = nxt[j];
            float s = (float)Math.sqrt((m.m[i][i] - (m.m[j][j] + m.m[k][k])) + 1.0f);
            q[i] = s * 0.5f;
            if (s != 0.f) s = 0.5f / s;
            w = (m.m[k][j] - m.m[j][k]) * s;
            q[j] = (m.m[j][i] + m.m[i][j]) * s;
            q[k] = (m.m[k][i] + m.m[i][k]) * s;
            v.x = q[0];
            v.y = q[1];
            v.z = q[2];
        }
    }

    public static Quaternion Slerp(float t, Quaternion q1, Quaternion q2) {
        float cosTheta = Quaternion.Dot(q1, q2);
        if (cosTheta > .9995f)
            return Quaternion.Normalize(q1.scale(1-t).add(q2.scale(t)));
        else {
            float theta = (float)Math.acos(Pbrt.Clamp(cosTheta, -1, 1));
            float thetap = theta * t;
            Quaternion qperp = Quaternion.Normalize(q2.subtract(q1.scale(cosTheta)));
            return q1.scale((float)Math.cos(thetap)).add(qperp.scale((float)Math.sin(thetap)));
        }
    }

    public static float Dot(Quaternion q1, Quaternion q2) {
        return Vector3f.Dot(q1.v, q2.v) + q1.w * q2.w;
    }

    public static Quaternion Normalize(Quaternion q) {
        return q.invScale((float)Math.sqrt(Quaternion.Dot(q, q)));
    }
}