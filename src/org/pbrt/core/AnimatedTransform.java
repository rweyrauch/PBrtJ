
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

import java.util.ArrayList;

public class AnimatedTransform {
    // AnimatedTransform Private Data
    private Transform startTransform, endTransform;
    private float startTime, endTime;
    private boolean actuallyAnimated;
    private Vector3f T0, T1;
    private Quaternion R0, R1;
    private Matrix4x4 S0, S1;
    private boolean hasRotation;

    private class DerivativeTerm {
        public float kc, kx, ky, kz;
        public DerivativeTerm() {}
        public DerivativeTerm(float c, float x, float y, float z) {
            kc = c;
            kx = x;
            ky = y;
            kz = z;
        }
        public float Eval(Point3f p) {
            return kc + kx * p.x + ky * p.y + kz * p.z;
        }
    }

    private DerivativeTerm c1[], c2[], c3[], c4[], c5[];

    // Interval Definitions
    private static class Interval {
        public float low, high;
        // Interval Public Methods
        public Interval(float v) {  
            low = v;
            high =v;
        } 
        public Interval(float v0, float v1) {
            low = Math.min(v0, v1);
            high = Math.max(v0, v1);
        }
        public Interval add(Interval i) {
            return new Interval(low + i.low, high + i.high);
        }
        public Interval subtract(Interval i) {
            return new Interval(low - i.high, high - i.low);
        }
        public Interval multiply(Interval i) {
            return new Interval(Math.min(Math.min(low * i.low, high * i.low), Math.min(low * i.high, high * i.high)),
                    Math.max(Math.max(low * i.low, high * i.low), Math.max(low * i.high, high * i.high)));
        }
    }
    private static Interval Sin(Interval i) {
        assert(i.low >= 0);
        assert(i.high <= 2.0001f * (float)Math.PI);
        float sinLow = (float)Math.sin(i.low), sinHigh = (float)Math.sin(i.high);
        if (sinLow > sinHigh) {
            float temp = sinLow;
            sinLow = sinHigh;
            sinHigh = temp;
        }
        if (i.low < (float)Math.PI / 2 && i.high > (float)Math.PI / 2) sinHigh = 1;
        if (i.low < (3.f / 2.f) * (float)Math.PI && i.high > (3.f / 2.f) * (float)Math.PI) sinLow = -1;
        return new Interval(sinLow, sinHigh);
    }

    private static Interval Cos(Interval i) {
        assert(i.low >= 0);
        assert(i.high <= 2.0001 * (float)Math.PI);
        float cosLow = (float)Math.cos(i.low), cosHigh = (float)Math.cos(i.high);
        if (cosLow > cosHigh) {
            float temp = cosLow;
            cosLow = cosHigh;
            cosHigh = temp;
        }
        if (i.low < (float)Math.PI && i.high > (float)Math.PI) cosLow = -1;
        return new Interval(cosLow, cosHigh);
    }

    private static ArrayList<Float> IntervalFindZeros(float c1, float c2, float c3, float c4, float c5, float theta, Interval tInterval, int depth) {
        // Evaluate motion derivative in interval form, return if no zeros
        Interval ic1 = new Interval(c1);
        Interval ic2 = new Interval(c2);
        Interval ic3 = new Interval(c3);
        Interval ic4 = new Interval(c4);
        Interval ic5 = new Interval(c5);

        ArrayList<Float> zeros = new ArrayList<>();

        Interval r1 = ic2.add(ic3.multiply(tInterval)).multiply(Cos(new Interval(2 * theta).multiply(tInterval)));
        Interval r2 = ic4.add(ic5.multiply(tInterval)).multiply(Sin(new Interval(2 * theta).multiply(tInterval)));
        Interval range = ic1.add(r1.add(r2));

        if (range.low > 0. || range.high < 0. || range.low == range.high) return null;
        if (depth > 0) {
            // Split _tInterval_ and check both resulting intervals
            Float mid = (tInterval.low + tInterval.high) * 0.5f;
            zeros.addAll(IntervalFindZeros(c1, c2, c3, c4, c5, theta, new Interval(tInterval.low, mid), depth - 1));
            zeros.addAll(IntervalFindZeros(c1, c2, c3, c4, c5, theta, new Interval(mid, tInterval.high), depth - 1));
        } else {
            // Use Newton's method to refine zero
            float tNewton = (tInterval.low + tInterval.high) * 0.5f;
            for (int i = 0; i < 4; ++i) {
                float fNewton = c1 + (c2 + c3 * tNewton) * (float)Math.cos(2.f * theta * tNewton) + (c4 + c5 * tNewton) * (float)Math.sin(2.f * theta * tNewton);
                float fPrimeNewton = (c3 + 2 * (c4 + c5 * tNewton) * theta) * (float)Math.cos(2.f * tNewton * theta) + (c5 - 2 * (c2 + c3 * tNewton) * theta) * (float)Math.sin(2.f * tNewton * theta);
                if (fNewton == 0 || fPrimeNewton == 0) break;
                tNewton = tNewton - fNewton / fPrimeNewton;
            }
            if (tNewton >= tInterval.low - 1e-3f && tNewton < tInterval.high + 1e-3f) {
                zeros.add(tNewton);
            }
        }
        return zeros;
    }

    // AnimatedTransform Public Methods
    public AnimatedTransform(Transform startTransform, float startTime,
                      Transform endTransform, float endTime) {
        this.startTransform = startTransform;
        this.endTransform = endTransform;
        this.startTime = startTime;
        this.endTime = endTime;

        MatComponents sComp = Decompose(startTransform.GetMatrix());
        MatComponents eComp = Decompose(endTransform.GetMatrix());

        T0 = sComp.T;
        R0 = sComp.R;
        S0 = sComp.S;
        
        T1 = eComp.T;
        R1 = eComp.R;
        S1 = sComp.S;
        
        // Flip _R[1]_ if needed to select shortest path
        if (Quaternion.Dot(R0, R1) < 0) R1 = R1.negate();
        hasRotation = Quaternion.Dot(R0, R1) < 0.9995f;
        // Compute terms of motion derivative function
        if (hasRotation) {
            float cosTheta = Quaternion.Dot(R0, R1);
            float theta = (float)Math.acos(Pbrt.Clamp(cosTheta, -1, 1));
            Quaternion qperp = Quaternion.Normalize(R1.subtract(R0.scale(cosTheta)));

            float t0x = T0.x;
            float t0y = T0.y;
            float t0z = T0.z;
            float t1x = T1.x;
            float t1y = T1.y;
            float t1z = T1.z;
            float q1x = R0.v.x;
            float q1y = R0.v.y;
            float q1z = R0.v.z;
            float q1w = R0.w;
            float qperpx = qperp.v.x;
            float qperpy = qperp.v.y;
            float qperpz = qperp.v.z;
            float qperpw = qperp.w;
            float s000 = S0.m[0][0];
            float s001 = S0.m[0][1];
            float s002 = S0.m[0][2];
            float s010 = S0.m[1][0];
            float s011 = S0.m[1][1];
            float s012 = S0.m[1][2];
            float s020 = S0.m[2][0];
            float s021 = S0.m[2][1];
            float s022 = S0.m[2][2];
            float s100 = S1.m[0][0];
            float s101 = S1.m[0][1];
            float s102 = S1.m[0][2];
            float s110 = S1.m[1][0];
            float s111 = S1.m[1][1];
            float s112 = S1.m[1][2];
            float s120 = S1.m[2][0];
            float s121 = S1.m[2][1];
            float s122 = S1.m[2][2];

            c1[0] = new DerivativeTerm(
                    -t0x + t1x,
                    (-1 + q1y * q1y + q1z * q1z + qperpy * qperpy + qperpz * qperpz) *
                            s000 +
                            q1w * q1z * s010 - qperpx * qperpy * s010 +
                            qperpw * qperpz * s010 - q1w * q1y * s020 -
                            qperpw * qperpy * s020 - qperpx * qperpz * s020 + s100 -
                            q1y * q1y * s100 - q1z * q1z * s100 - qperpy * qperpy * s100 -
                            qperpz * qperpz * s100 - q1w * q1z * s110 +
                            qperpx * qperpy * s110 - qperpw * qperpz * s110 +
                            q1w * q1y * s120 + qperpw * qperpy * s120 +
                            qperpx * qperpz * s120 +
                            q1x * (-(q1y * s010) - q1z * s020 + q1y * s110 + q1z * s120),
                    (-1 + q1y * q1y + q1z * q1z + qperpy * qperpy + qperpz * qperpz) *
                            s001 +
                            q1w * q1z * s011 - qperpx * qperpy * s011 +
                            qperpw * qperpz * s011 - q1w * q1y * s021 -
                            qperpw * qperpy * s021 - qperpx * qperpz * s021 + s101 -
                            q1y * q1y * s101 - q1z * q1z * s101 - qperpy * qperpy * s101 -
                            qperpz * qperpz * s101 - q1w * q1z * s111 +
                            qperpx * qperpy * s111 - qperpw * qperpz * s111 +
                            q1w * q1y * s121 + qperpw * qperpy * s121 +
                            qperpx * qperpz * s121 +
                            q1x * (-(q1y * s011) - q1z * s021 + q1y * s111 + q1z * s121),
                    (-1 + q1y * q1y + q1z * q1z + qperpy * qperpy + qperpz * qperpz) *
                            s002 +
                            q1w * q1z * s012 - qperpx * qperpy * s012 +
                            qperpw * qperpz * s012 - q1w * q1y * s022 -
                            qperpw * qperpy * s022 - qperpx * qperpz * s022 + s102 -
                            q1y * q1y * s102 - q1z * q1z * s102 - qperpy * qperpy * s102 -
                            qperpz * qperpz * s102 - q1w * q1z * s112 +
                            qperpx * qperpy * s112 - qperpw * qperpz * s112 +
                            q1w * q1y * s122 + qperpw * qperpy * s122 +
                            qperpx * qperpz * s122 +
                            q1x * (-(q1y * s012) - q1z * s022 + q1y * s112 + q1z * s122));

            c2[0] = new DerivativeTerm(
                    0,
                    -(qperpy * qperpy * s000) - qperpz * qperpz * s000 +
                            qperpx * qperpy * s010 - qperpw * qperpz * s010 +
                            qperpw * qperpy * s020 + qperpx * qperpz * s020 +
                            q1y * q1y * (s000 - s100) + q1z * q1z * (s000 - s100) +
                            qperpy * qperpy * s100 + qperpz * qperpz * s100 -
                            qperpx * qperpy * s110 + qperpw * qperpz * s110 -
                            qperpw * qperpy * s120 - qperpx * qperpz * s120 +
                            2 * q1x * qperpy * s010 * theta -
                            2 * q1w * qperpz * s010 * theta +
                            2 * q1w * qperpy * s020 * theta +
                            2 * q1x * qperpz * s020 * theta +
                            q1y *
                                    (q1x * (-s010 + s110) + q1w * (-s020 + s120) +
                                            2 * (-2 * qperpy * s000 + qperpx * s010 + qperpw * s020) *
                                                    theta) +
                            q1z * (q1w * (s010 - s110) + q1x * (-s020 + s120) -
                                    2 * (2 * qperpz * s000 + qperpw * s010 - qperpx * s020) *
                                            theta),
                    -(qperpy * qperpy * s001) - qperpz * qperpz * s001 +
                            qperpx * qperpy * s011 - qperpw * qperpz * s011 +
                            qperpw * qperpy * s021 + qperpx * qperpz * s021 +
                            q1y * q1y * (s001 - s101) + q1z * q1z * (s001 - s101) +
                            qperpy * qperpy * s101 + qperpz * qperpz * s101 -
                            qperpx * qperpy * s111 + qperpw * qperpz * s111 -
                            qperpw * qperpy * s121 - qperpx * qperpz * s121 +
                            2 * q1x * qperpy * s011 * theta -
                            2 * q1w * qperpz * s011 * theta +
                            2 * q1w * qperpy * s021 * theta +
                            2 * q1x * qperpz * s021 * theta +
                            q1y *
                                    (q1x * (-s011 + s111) + q1w * (-s021 + s121) +
                                            2 * (-2 * qperpy * s001 + qperpx * s011 + qperpw * s021) *
                                                    theta) +
                            q1z * (q1w * (s011 - s111) + q1x * (-s021 + s121) -
                                    2 * (2 * qperpz * s001 + qperpw * s011 - qperpx * s021) *
                                            theta),
                    -(qperpy * qperpy * s002) - qperpz * qperpz * s002 +
                            qperpx * qperpy * s012 - qperpw * qperpz * s012 +
                            qperpw * qperpy * s022 + qperpx * qperpz * s022 +
                            q1y * q1y * (s002 - s102) + q1z * q1z * (s002 - s102) +
                            qperpy * qperpy * s102 + qperpz * qperpz * s102 -
                            qperpx * qperpy * s112 + qperpw * qperpz * s112 -
                            qperpw * qperpy * s122 - qperpx * qperpz * s122 +
                            2 * q1x * qperpy * s012 * theta -
                            2 * q1w * qperpz * s012 * theta +
                            2 * q1w * qperpy * s022 * theta +
                            2 * q1x * qperpz * s022 * theta +
                            q1y *
                                    (q1x * (-s012 + s112) + q1w * (-s022 + s122) +
                                            2 * (-2 * qperpy * s002 + qperpx * s012 + qperpw * s022) *
                                                    theta) +
                            q1z * (q1w * (s012 - s112) + q1x * (-s022 + s122) -
                                    2 * (2 * qperpz * s002 + qperpw * s012 - qperpx * s022) *
                                            theta));

            c3[0] = new DerivativeTerm(
                    0,
                    -2 * (q1x * qperpy * s010 - q1w * qperpz * s010 +
                            q1w * qperpy * s020 + q1x * qperpz * s020 -
                            q1x * qperpy * s110 + q1w * qperpz * s110 -
                            q1w * qperpy * s120 - q1x * qperpz * s120 +
                            q1y * (-2 * qperpy * s000 + qperpx * s010 + qperpw * s020 +
                                    2 * qperpy * s100 - qperpx * s110 - qperpw * s120) +
                            q1z * (-2 * qperpz * s000 - qperpw * s010 + qperpx * s020 +
                                    2 * qperpz * s100 + qperpw * s110 - qperpx * s120)) *
                            theta,
                    -2 * (q1x * qperpy * s011 - q1w * qperpz * s011 +
                            q1w * qperpy * s021 + q1x * qperpz * s021 -
                            q1x * qperpy * s111 + q1w * qperpz * s111 -
                            q1w * qperpy * s121 - q1x * qperpz * s121 +
                            q1y * (-2 * qperpy * s001 + qperpx * s011 + qperpw * s021 +
                                    2 * qperpy * s101 - qperpx * s111 - qperpw * s121) +
                            q1z * (-2 * qperpz * s001 - qperpw * s011 + qperpx * s021 +
                                    2 * qperpz * s101 + qperpw * s111 - qperpx * s121)) *
                            theta,
                    -2 * (q1x * qperpy * s012 - q1w * qperpz * s012 +
                            q1w * qperpy * s022 + q1x * qperpz * s022 -
                            q1x * qperpy * s112 + q1w * qperpz * s112 -
                            q1w * qperpy * s122 - q1x * qperpz * s122 +
                            q1y * (-2 * qperpy * s002 + qperpx * s012 + qperpw * s022 +
                                    2 * qperpy * s102 - qperpx * s112 - qperpw * s122) +
                            q1z * (-2 * qperpz * s002 - qperpw * s012 + qperpx * s022 +
                                    2 * qperpz * s102 + qperpw * s112 - qperpx * s122)) *
                            theta);

            c4[0] = new DerivativeTerm(
                    0,
                    -(q1x * qperpy * s010) + q1w * qperpz * s010 - q1w * qperpy * s020 -
                            q1x * qperpz * s020 + q1x * qperpy * s110 -
                            q1w * qperpz * s110 + q1w * qperpy * s120 +
                            q1x * qperpz * s120 + 2 * q1y * q1y * s000 * theta +
                            2 * q1z * q1z * s000 * theta -
                            2 * qperpy * qperpy * s000 * theta -
                            2 * qperpz * qperpz * s000 * theta +
                            2 * qperpx * qperpy * s010 * theta -
                            2 * qperpw * qperpz * s010 * theta +
                            2 * qperpw * qperpy * s020 * theta +
                            2 * qperpx * qperpz * s020 * theta +
                            q1y * (-(qperpx * s010) - qperpw * s020 +
                                    2 * qperpy * (s000 - s100) + qperpx * s110 +
                                    qperpw * s120 - 2 * q1x * s010 * theta -
                                    2 * q1w * s020 * theta) +
                            q1z * (2 * qperpz * s000 + qperpw * s010 - qperpx * s020 -
                                    2 * qperpz * s100 - qperpw * s110 + qperpx * s120 +
                                    2 * q1w * s010 * theta - 2 * q1x * s020 * theta),
                    -(q1x * qperpy * s011) + q1w * qperpz * s011 - q1w * qperpy * s021 -
                            q1x * qperpz * s021 + q1x * qperpy * s111 -
                            q1w * qperpz * s111 + q1w * qperpy * s121 +
                            q1x * qperpz * s121 + 2 * q1y * q1y * s001 * theta +
                            2 * q1z * q1z * s001 * theta -
                            2 * qperpy * qperpy * s001 * theta -
                            2 * qperpz * qperpz * s001 * theta +
                            2 * qperpx * qperpy * s011 * theta -
                            2 * qperpw * qperpz * s011 * theta +
                            2 * qperpw * qperpy * s021 * theta +
                            2 * qperpx * qperpz * s021 * theta +
                            q1y * (-(qperpx * s011) - qperpw * s021 +
                                    2 * qperpy * (s001 - s101) + qperpx * s111 +
                                    qperpw * s121 - 2 * q1x * s011 * theta -
                                    2 * q1w * s021 * theta) +
                            q1z * (2 * qperpz * s001 + qperpw * s011 - qperpx * s021 -
                                    2 * qperpz * s101 - qperpw * s111 + qperpx * s121 +
                                    2 * q1w * s011 * theta - 2 * q1x * s021 * theta),
                    -(q1x * qperpy * s012) + q1w * qperpz * s012 - q1w * qperpy * s022 -
                            q1x * qperpz * s022 + q1x * qperpy * s112 -
                            q1w * qperpz * s112 + q1w * qperpy * s122 +
                            q1x * qperpz * s122 + 2 * q1y * q1y * s002 * theta +
                            2 * q1z * q1z * s002 * theta -
                            2 * qperpy * qperpy * s002 * theta -
                            2 * qperpz * qperpz * s002 * theta +
                            2 * qperpx * qperpy * s012 * theta -
                            2 * qperpw * qperpz * s012 * theta +
                            2 * qperpw * qperpy * s022 * theta +
                            2 * qperpx * qperpz * s022 * theta +
                            q1y * (-(qperpx * s012) - qperpw * s022 +
                                    2 * qperpy * (s002 - s102) + qperpx * s112 +
                                    qperpw * s122 - 2 * q1x * s012 * theta -
                                    2 * q1w * s022 * theta) +
                            q1z * (2 * qperpz * s002 + qperpw * s012 - qperpx * s022 -
                                    2 * qperpz * s102 - qperpw * s112 + qperpx * s122 +
                                    2 * q1w * s012 * theta - 2 * q1x * s022 * theta));

            c5[0] = new DerivativeTerm(
                    0,
                    2 * (qperpy * qperpy * s000 + qperpz * qperpz * s000 -
                            qperpx * qperpy * s010 + qperpw * qperpz * s010 -
                            qperpw * qperpy * s020 - qperpx * qperpz * s020 -
                            qperpy * qperpy * s100 - qperpz * qperpz * s100 +
                            q1y * q1y * (-s000 + s100) + q1z * q1z * (-s000 + s100) +
                            qperpx * qperpy * s110 - qperpw * qperpz * s110 +
                            q1y * (q1x * (s010 - s110) + q1w * (s020 - s120)) +
                            qperpw * qperpy * s120 + qperpx * qperpz * s120 +
                            q1z * (-(q1w * s010) + q1x * s020 + q1w * s110 - q1x * s120)) *
                            theta,
                    2 * (qperpy * qperpy * s001 + qperpz * qperpz * s001 -
                            qperpx * qperpy * s011 + qperpw * qperpz * s011 -
                            qperpw * qperpy * s021 - qperpx * qperpz * s021 -
                            qperpy * qperpy * s101 - qperpz * qperpz * s101 +
                            q1y * q1y * (-s001 + s101) + q1z * q1z * (-s001 + s101) +
                            qperpx * qperpy * s111 - qperpw * qperpz * s111 +
                            q1y * (q1x * (s011 - s111) + q1w * (s021 - s121)) +
                            qperpw * qperpy * s121 + qperpx * qperpz * s121 +
                            q1z * (-(q1w * s011) + q1x * s021 + q1w * s111 - q1x * s121)) *
                            theta,
                    2 * (qperpy * qperpy * s002 + qperpz * qperpz * s002 -
                            qperpx * qperpy * s012 + qperpw * qperpz * s012 -
                            qperpw * qperpy * s022 - qperpx * qperpz * s022 -
                            qperpy * qperpy * s102 - qperpz * qperpz * s102 +
                            q1y * q1y * (-s002 + s102) + q1z * q1z * (-s002 + s102) +
                            qperpx * qperpy * s112 - qperpw * qperpz * s112 +
                            q1y * (q1x * (s012 - s112) + q1w * (s022 - s122)) +
                            qperpw * qperpy * s122 + qperpx * qperpz * s122 +
                            q1z * (-(q1w * s012) + q1x * s022 + q1w * s112 - q1x * s122)) *
                            theta);

            c1[1] = new DerivativeTerm(
                    -t0y + t1y,
                    -(qperpx * qperpy * s000) - qperpw * qperpz * s000 - s010 +
                            q1z * q1z * s010 + qperpx * qperpx * s010 +
                            qperpz * qperpz * s010 - q1y * q1z * s020 +
                            qperpw * qperpx * s020 - qperpy * qperpz * s020 +
                            qperpx * qperpy * s100 + qperpw * qperpz * s100 +
                            q1w * q1z * (-s000 + s100) + q1x * q1x * (s010 - s110) + s110 -
                            q1z * q1z * s110 - qperpx * qperpx * s110 -
                            qperpz * qperpz * s110 +
                            q1x * (q1y * (-s000 + s100) + q1w * (s020 - s120)) +
                            q1y * q1z * s120 - qperpw * qperpx * s120 +
                            qperpy * qperpz * s120,
                    -(qperpx * qperpy * s001) - qperpw * qperpz * s001 - s011 +
                            q1z * q1z * s011 + qperpx * qperpx * s011 +
                            qperpz * qperpz * s011 - q1y * q1z * s021 +
                            qperpw * qperpx * s021 - qperpy * qperpz * s021 +
                            qperpx * qperpy * s101 + qperpw * qperpz * s101 +
                            q1w * q1z * (-s001 + s101) + q1x * q1x * (s011 - s111) + s111 -
                            q1z * q1z * s111 - qperpx * qperpx * s111 -
                            qperpz * qperpz * s111 +
                            q1x * (q1y * (-s001 + s101) + q1w * (s021 - s121)) +
                            q1y * q1z * s121 - qperpw * qperpx * s121 +
                            qperpy * qperpz * s121,
                    -(qperpx * qperpy * s002) - qperpw * qperpz * s002 - s012 +
                            q1z * q1z * s012 + qperpx * qperpx * s012 +
                            qperpz * qperpz * s012 - q1y * q1z * s022 +
                            qperpw * qperpx * s022 - qperpy * qperpz * s022 +
                            qperpx * qperpy * s102 + qperpw * qperpz * s102 +
                            q1w * q1z * (-s002 + s102) + q1x * q1x * (s012 - s112) + s112 -
                            q1z * q1z * s112 - qperpx * qperpx * s112 -
                            qperpz * qperpz * s112 +
                            q1x * (q1y * (-s002 + s102) + q1w * (s022 - s122)) +
                            q1y * q1z * s122 - qperpw * qperpx * s122 +
                            qperpy * qperpz * s122);

            c2[1] = new DerivativeTerm(
                    0,
                    qperpx * qperpy * s000 + qperpw * qperpz * s000 + q1z * q1z * s010 -
                            qperpx * qperpx * s010 - qperpz * qperpz * s010 -
                            q1y * q1z * s020 - qperpw * qperpx * s020 +
                            qperpy * qperpz * s020 - qperpx * qperpy * s100 -
                            qperpw * qperpz * s100 + q1x * q1x * (s010 - s110) -
                            q1z * q1z * s110 + qperpx * qperpx * s110 +
                            qperpz * qperpz * s110 + q1y * q1z * s120 +
                            qperpw * qperpx * s120 - qperpy * qperpz * s120 +
                            2 * q1z * qperpw * s000 * theta +
                            2 * q1y * qperpx * s000 * theta -
                            4 * q1z * qperpz * s010 * theta +
                            2 * q1z * qperpy * s020 * theta +
                            2 * q1y * qperpz * s020 * theta +
                            q1x * (q1w * s020 + q1y * (-s000 + s100) - q1w * s120 +
                                    2 * qperpy * s000 * theta - 4 * qperpx * s010 * theta -
                                    2 * qperpw * s020 * theta) +
                            q1w * (-(q1z * s000) + q1z * s100 + 2 * qperpz * s000 * theta -
                                    2 * qperpx * s020 * theta),
                    qperpx * qperpy * s001 + qperpw * qperpz * s001 + q1z * q1z * s011 -
                            qperpx * qperpx * s011 - qperpz * qperpz * s011 -
                            q1y * q1z * s021 - qperpw * qperpx * s021 +
                            qperpy * qperpz * s021 - qperpx * qperpy * s101 -
                            qperpw * qperpz * s101 + q1x * q1x * (s011 - s111) -
                            q1z * q1z * s111 + qperpx * qperpx * s111 +
                            qperpz * qperpz * s111 + q1y * q1z * s121 +
                            qperpw * qperpx * s121 - qperpy * qperpz * s121 +
                            2 * q1z * qperpw * s001 * theta +
                            2 * q1y * qperpx * s001 * theta -
                            4 * q1z * qperpz * s011 * theta +
                            2 * q1z * qperpy * s021 * theta +
                            2 * q1y * qperpz * s021 * theta +
                            q1x * (q1w * s021 + q1y * (-s001 + s101) - q1w * s121 +
                                    2 * qperpy * s001 * theta - 4 * qperpx * s011 * theta -
                                    2 * qperpw * s021 * theta) +
                            q1w * (-(q1z * s001) + q1z * s101 + 2 * qperpz * s001 * theta -
                                    2 * qperpx * s021 * theta),
                    qperpx * qperpy * s002 + qperpw * qperpz * s002 + q1z * q1z * s012 -
                            qperpx * qperpx * s012 - qperpz * qperpz * s012 -
                            q1y * q1z * s022 - qperpw * qperpx * s022 +
                            qperpy * qperpz * s022 - qperpx * qperpy * s102 -
                            qperpw * qperpz * s102 + q1x * q1x * (s012 - s112) -
                            q1z * q1z * s112 + qperpx * qperpx * s112 +
                            qperpz * qperpz * s112 + q1y * q1z * s122 +
                            qperpw * qperpx * s122 - qperpy * qperpz * s122 +
                            2 * q1z * qperpw * s002 * theta +
                            2 * q1y * qperpx * s002 * theta -
                            4 * q1z * qperpz * s012 * theta +
                            2 * q1z * qperpy * s022 * theta +
                            2 * q1y * qperpz * s022 * theta +
                            q1x * (q1w * s022 + q1y * (-s002 + s102) - q1w * s122 +
                                    2 * qperpy * s002 * theta - 4 * qperpx * s012 * theta -
                                    2 * qperpw * s022 * theta) +
                            q1w * (-(q1z * s002) + q1z * s102 + 2 * qperpz * s002 * theta -
                                    2 * qperpx * s022 * theta));

            c3[1] = new DerivativeTerm(
                    0, 2 * (-(q1x * qperpy * s000) - q1w * qperpz * s000 +
                            2 * q1x * qperpx * s010 + q1x * qperpw * s020 +
                            q1w * qperpx * s020 + q1x * qperpy * s100 +
                            q1w * qperpz * s100 - 2 * q1x * qperpx * s110 -
                            q1x * qperpw * s120 - q1w * qperpx * s120 +
                            q1z * (2 * qperpz * s010 - qperpy * s020 +
                                    qperpw * (-s000 + s100) - 2 * qperpz * s110 +
                                    qperpy * s120) +
                            q1y * (-(qperpx * s000) - qperpz * s020 + qperpx * s100 +
                                    qperpz * s120)) *
                            theta,
                    2 * (-(q1x * qperpy * s001) - q1w * qperpz * s001 +
                            2 * q1x * qperpx * s011 + q1x * qperpw * s021 +
                            q1w * qperpx * s021 + q1x * qperpy * s101 +
                            q1w * qperpz * s101 - 2 * q1x * qperpx * s111 -
                            q1x * qperpw * s121 - q1w * qperpx * s121 +
                            q1z * (2 * qperpz * s011 - qperpy * s021 +
                                    qperpw * (-s001 + s101) - 2 * qperpz * s111 +
                                    qperpy * s121) +
                            q1y * (-(qperpx * s001) - qperpz * s021 + qperpx * s101 +
                                    qperpz * s121)) *
                            theta,
                    2 * (-(q1x * qperpy * s002) - q1w * qperpz * s002 +
                            2 * q1x * qperpx * s012 + q1x * qperpw * s022 +
                            q1w * qperpx * s022 + q1x * qperpy * s102 +
                            q1w * qperpz * s102 - 2 * q1x * qperpx * s112 -
                            q1x * qperpw * s122 - q1w * qperpx * s122 +
                            q1z * (2 * qperpz * s012 - qperpy * s022 +
                                    qperpw * (-s002 + s102) - 2 * qperpz * s112 +
                                    qperpy * s122) +
                            q1y * (-(qperpx * s002) - qperpz * s022 + qperpx * s102 +
                                    qperpz * s122)) *
                            theta);

            c4[1] = new DerivativeTerm(
                    0,
                    -(q1x * qperpy * s000) - q1w * qperpz * s000 +
                            2 * q1x * qperpx * s010 + q1x * qperpw * s020 +
                            q1w * qperpx * s020 + q1x * qperpy * s100 +
                            q1w * qperpz * s100 - 2 * q1x * qperpx * s110 -
                            q1x * qperpw * s120 - q1w * qperpx * s120 +
                            2 * qperpx * qperpy * s000 * theta +
                            2 * qperpw * qperpz * s000 * theta +
                            2 * q1x * q1x * s010 * theta + 2 * q1z * q1z * s010 * theta -
                            2 * qperpx * qperpx * s010 * theta -
                            2 * qperpz * qperpz * s010 * theta +
                            2 * q1w * q1x * s020 * theta -
                            2 * qperpw * qperpx * s020 * theta +
                            2 * qperpy * qperpz * s020 * theta +
                            q1y * (-(qperpx * s000) - qperpz * s020 + qperpx * s100 +
                                    qperpz * s120 - 2 * q1x * s000 * theta) +
                            q1z * (2 * qperpz * s010 - qperpy * s020 +
                                    qperpw * (-s000 + s100) - 2 * qperpz * s110 +
                                    qperpy * s120 - 2 * q1w * s000 * theta -
                                    2 * q1y * s020 * theta),
                    -(q1x * qperpy * s001) - q1w * qperpz * s001 +
                            2 * q1x * qperpx * s011 + q1x * qperpw * s021 +
                            q1w * qperpx * s021 + q1x * qperpy * s101 +
                            q1w * qperpz * s101 - 2 * q1x * qperpx * s111 -
                            q1x * qperpw * s121 - q1w * qperpx * s121 +
                            2 * qperpx * qperpy * s001 * theta +
                            2 * qperpw * qperpz * s001 * theta +
                            2 * q1x * q1x * s011 * theta + 2 * q1z * q1z * s011 * theta -
                            2 * qperpx * qperpx * s011 * theta -
                            2 * qperpz * qperpz * s011 * theta +
                            2 * q1w * q1x * s021 * theta -
                            2 * qperpw * qperpx * s021 * theta +
                            2 * qperpy * qperpz * s021 * theta +
                            q1y * (-(qperpx * s001) - qperpz * s021 + qperpx * s101 +
                                    qperpz * s121 - 2 * q1x * s001 * theta) +
                            q1z * (2 * qperpz * s011 - qperpy * s021 +
                                    qperpw * (-s001 + s101) - 2 * qperpz * s111 +
                                    qperpy * s121 - 2 * q1w * s001 * theta -
                                    2 * q1y * s021 * theta),
                    -(q1x * qperpy * s002) - q1w * qperpz * s002 +
                            2 * q1x * qperpx * s012 + q1x * qperpw * s022 +
                            q1w * qperpx * s022 + q1x * qperpy * s102 +
                            q1w * qperpz * s102 - 2 * q1x * qperpx * s112 -
                            q1x * qperpw * s122 - q1w * qperpx * s122 +
                            2 * qperpx * qperpy * s002 * theta +
                            2 * qperpw * qperpz * s002 * theta +
                            2 * q1x * q1x * s012 * theta + 2 * q1z * q1z * s012 * theta -
                            2 * qperpx * qperpx * s012 * theta -
                            2 * qperpz * qperpz * s012 * theta +
                            2 * q1w * q1x * s022 * theta -
                            2 * qperpw * qperpx * s022 * theta +
                            2 * qperpy * qperpz * s022 * theta +
                            q1y * (-(qperpx * s002) - qperpz * s022 + qperpx * s102 +
                                    qperpz * s122 - 2 * q1x * s002 * theta) +
                            q1z * (2 * qperpz * s012 - qperpy * s022 +
                                    qperpw * (-s002 + s102) - 2 * qperpz * s112 +
                                    qperpy * s122 - 2 * q1w * s002 * theta -
                                    2 * q1y * s022 * theta));

            c5[1] = new DerivativeTerm(
                    0, -2 * (qperpx * qperpy * s000 + qperpw * qperpz * s000 +
                            q1z * q1z * s010 - qperpx * qperpx * s010 -
                            qperpz * qperpz * s010 - q1y * q1z * s020 -
                            qperpw * qperpx * s020 + qperpy * qperpz * s020 -
                            qperpx * qperpy * s100 - qperpw * qperpz * s100 +
                            q1w * q1z * (-s000 + s100) + q1x * q1x * (s010 - s110) -
                            q1z * q1z * s110 + qperpx * qperpx * s110 +
                            qperpz * qperpz * s110 +
                            q1x * (q1y * (-s000 + s100) + q1w * (s020 - s120)) +
                            q1y * q1z * s120 + qperpw * qperpx * s120 -
                            qperpy * qperpz * s120) *
                            theta,
                    -2 * (qperpx * qperpy * s001 + qperpw * qperpz * s001 +
                            q1z * q1z * s011 - qperpx * qperpx * s011 -
                            qperpz * qperpz * s011 - q1y * q1z * s021 -
                            qperpw * qperpx * s021 + qperpy * qperpz * s021 -
                            qperpx * qperpy * s101 - qperpw * qperpz * s101 +
                            q1w * q1z * (-s001 + s101) + q1x * q1x * (s011 - s111) -
                            q1z * q1z * s111 + qperpx * qperpx * s111 +
                            qperpz * qperpz * s111 +
                            q1x * (q1y * (-s001 + s101) + q1w * (s021 - s121)) +
                            q1y * q1z * s121 + qperpw * qperpx * s121 -
                            qperpy * qperpz * s121) *
                            theta,
                    -2 * (qperpx * qperpy * s002 + qperpw * qperpz * s002 +
                            q1z * q1z * s012 - qperpx * qperpx * s012 -
                            qperpz * qperpz * s012 - q1y * q1z * s022 -
                            qperpw * qperpx * s022 + qperpy * qperpz * s022 -
                            qperpx * qperpy * s102 - qperpw * qperpz * s102 +
                            q1w * q1z * (-s002 + s102) + q1x * q1x * (s012 - s112) -
                            q1z * q1z * s112 + qperpx * qperpx * s112 +
                            qperpz * qperpz * s112 +
                            q1x * (q1y * (-s002 + s102) + q1w * (s022 - s122)) +
                            q1y * q1z * s122 + qperpw * qperpx * s122 -
                            qperpy * qperpz * s122) *
                            theta);

            c1[2] = new DerivativeTerm(
                    -t0z + t1z, (qperpw * qperpy * s000 - qperpx * qperpz * s000 -
                            q1y * q1z * s010 - qperpw * qperpx * s010 -
                            qperpy * qperpz * s010 - s020 + q1y * q1y * s020 +
                            qperpx * qperpx * s020 + qperpy * qperpy * s020 -
                            qperpw * qperpy * s100 + qperpx * qperpz * s100 +
                            q1x * q1z * (-s000 + s100) + q1y * q1z * s110 +
                            qperpw * qperpx * s110 + qperpy * qperpz * s110 +
                            q1w * (q1y * (s000 - s100) + q1x * (-s010 + s110)) +
                            q1x * q1x * (s020 - s120) + s120 - q1y * q1y * s120 -
                            qperpx * qperpx * s120 - qperpy * qperpy * s120),
                    (qperpw * qperpy * s001 - qperpx * qperpz * s001 -
                            q1y * q1z * s011 - qperpw * qperpx * s011 -
                            qperpy * qperpz * s011 - s021 + q1y * q1y * s021 +
                            qperpx * qperpx * s021 + qperpy * qperpy * s021 -
                            qperpw * qperpy * s101 + qperpx * qperpz * s101 +
                            q1x * q1z * (-s001 + s101) + q1y * q1z * s111 +
                            qperpw * qperpx * s111 + qperpy * qperpz * s111 +
                            q1w * (q1y * (s001 - s101) + q1x * (-s011 + s111)) +
                            q1x * q1x * (s021 - s121) + s121 - q1y * q1y * s121 -
                            qperpx * qperpx * s121 - qperpy * qperpy * s121),
                    (qperpw * qperpy * s002 - qperpx * qperpz * s002 -
                            q1y * q1z * s012 - qperpw * qperpx * s012 -
                            qperpy * qperpz * s012 - s022 + q1y * q1y * s022 +
                            qperpx * qperpx * s022 + qperpy * qperpy * s022 -
                            qperpw * qperpy * s102 + qperpx * qperpz * s102 +
                            q1x * q1z * (-s002 + s102) + q1y * q1z * s112 +
                            qperpw * qperpx * s112 + qperpy * qperpz * s112 +
                            q1w * (q1y * (s002 - s102) + q1x * (-s012 + s112)) +
                            q1x * q1x * (s022 - s122) + s122 - q1y * q1y * s122 -
                            qperpx * qperpx * s122 - qperpy * qperpy * s122));

            c2[2] = new DerivativeTerm(
                    0,
                    (q1w * q1y * s000 - q1x * q1z * s000 - qperpw * qperpy * s000 +
                            qperpx * qperpz * s000 - q1w * q1x * s010 - q1y * q1z * s010 +
                            qperpw * qperpx * s010 + qperpy * qperpz * s010 +
                            q1x * q1x * s020 + q1y * q1y * s020 - qperpx * qperpx * s020 -
                            qperpy * qperpy * s020 - q1w * q1y * s100 + q1x * q1z * s100 +
                            qperpw * qperpy * s100 - qperpx * qperpz * s100 +
                            q1w * q1x * s110 + q1y * q1z * s110 - qperpw * qperpx * s110 -
                            qperpy * qperpz * s110 - q1x * q1x * s120 - q1y * q1y * s120 +
                            qperpx * qperpx * s120 + qperpy * qperpy * s120 -
                            2 * q1y * qperpw * s000 * theta + 2 * q1z * qperpx * s000 * theta -
                            2 * q1w * qperpy * s000 * theta + 2 * q1x * qperpz * s000 * theta +
                            2 * q1x * qperpw * s010 * theta + 2 * q1w * qperpx * s010 * theta +
                            2 * q1z * qperpy * s010 * theta + 2 * q1y * qperpz * s010 * theta -
                            4 * q1x * qperpx * s020 * theta - 4 * q1y * qperpy * s020 * theta),
                    (q1w * q1y * s001 - q1x * q1z * s001 - qperpw * qperpy * s001 +
                            qperpx * qperpz * s001 - q1w * q1x * s011 - q1y * q1z * s011 +
                            qperpw * qperpx * s011 + qperpy * qperpz * s011 +
                            q1x * q1x * s021 + q1y * q1y * s021 - qperpx * qperpx * s021 -
                            qperpy * qperpy * s021 - q1w * q1y * s101 + q1x * q1z * s101 +
                            qperpw * qperpy * s101 - qperpx * qperpz * s101 +
                            q1w * q1x * s111 + q1y * q1z * s111 - qperpw * qperpx * s111 -
                            qperpy * qperpz * s111 - q1x * q1x * s121 - q1y * q1y * s121 +
                            qperpx * qperpx * s121 + qperpy * qperpy * s121 -
                            2 * q1y * qperpw * s001 * theta + 2 * q1z * qperpx * s001 * theta -
                            2 * q1w * qperpy * s001 * theta + 2 * q1x * qperpz * s001 * theta +
                            2 * q1x * qperpw * s011 * theta + 2 * q1w * qperpx * s011 * theta +
                            2 * q1z * qperpy * s011 * theta + 2 * q1y * qperpz * s011 * theta -
                            4 * q1x * qperpx * s021 * theta - 4 * q1y * qperpy * s021 * theta),
                    (q1w * q1y * s002 - q1x * q1z * s002 - qperpw * qperpy * s002 +
                            qperpx * qperpz * s002 - q1w * q1x * s012 - q1y * q1z * s012 +
                            qperpw * qperpx * s012 + qperpy * qperpz * s012 +
                            q1x * q1x * s022 + q1y * q1y * s022 - qperpx * qperpx * s022 -
                            qperpy * qperpy * s022 - q1w * q1y * s102 + q1x * q1z * s102 +
                            qperpw * qperpy * s102 - qperpx * qperpz * s102 +
                            q1w * q1x * s112 + q1y * q1z * s112 - qperpw * qperpx * s112 -
                            qperpy * qperpz * s112 - q1x * q1x * s122 - q1y * q1y * s122 +
                            qperpx * qperpx * s122 + qperpy * qperpy * s122 -
                            2 * q1y * qperpw * s002 * theta + 2 * q1z * qperpx * s002 * theta -
                            2 * q1w * qperpy * s002 * theta + 2 * q1x * qperpz * s002 * theta +
                            2 * q1x * qperpw * s012 * theta + 2 * q1w * qperpx * s012 * theta +
                            2 * q1z * qperpy * s012 * theta + 2 * q1y * qperpz * s012 * theta -
                            4 * q1x * qperpx * s022 * theta -
                            4 * q1y * qperpy * s022 * theta));

            c3[2] = new DerivativeTerm(
                    0, -2 * (-(q1w * qperpy * s000) + q1x * qperpz * s000 +
                            q1x * qperpw * s010 + q1w * qperpx * s010 -
                            2 * q1x * qperpx * s020 + q1w * qperpy * s100 -
                            q1x * qperpz * s100 - q1x * qperpw * s110 -
                            q1w * qperpx * s110 +
                            q1z * (qperpx * s000 + qperpy * s010 - qperpx * s100 -
                                    qperpy * s110) +
                            2 * q1x * qperpx * s120 +
                            q1y * (qperpz * s010 - 2 * qperpy * s020 +
                                    qperpw * (-s000 + s100) - qperpz * s110 +
                                    2 * qperpy * s120)) *
                            theta,
                    -2 * (-(q1w * qperpy * s001) + q1x * qperpz * s001 +
                            q1x * qperpw * s011 + q1w * qperpx * s011 -
                            2 * q1x * qperpx * s021 + q1w * qperpy * s101 -
                            q1x * qperpz * s101 - q1x * qperpw * s111 -
                            q1w * qperpx * s111 +
                            q1z * (qperpx * s001 + qperpy * s011 - qperpx * s101 -
                                    qperpy * s111) +
                            2 * q1x * qperpx * s121 +
                            q1y * (qperpz * s011 - 2 * qperpy * s021 +
                                    qperpw * (-s001 + s101) - qperpz * s111 +
                                    2 * qperpy * s121)) *
                            theta,
                    -2 * (-(q1w * qperpy * s002) + q1x * qperpz * s002 +
                            q1x * qperpw * s012 + q1w * qperpx * s012 -
                            2 * q1x * qperpx * s022 + q1w * qperpy * s102 -
                            q1x * qperpz * s102 - q1x * qperpw * s112 -
                            q1w * qperpx * s112 +
                            q1z * (qperpx * s002 + qperpy * s012 - qperpx * s102 -
                                    qperpy * s112) +
                            2 * q1x * qperpx * s122 +
                            q1y * (qperpz * s012 - 2 * qperpy * s022 +
                                    qperpw * (-s002 + s102) - qperpz * s112 +
                                    2 * qperpy * s122)) *
                            theta);

            c4[2] = new DerivativeTerm(
                    0,
                    q1w * qperpy * s000 - q1x * qperpz * s000 - q1x * qperpw * s010 -
                            q1w * qperpx * s010 + 2 * q1x * qperpx * s020 -
                            q1w * qperpy * s100 + q1x * qperpz * s100 +
                            q1x * qperpw * s110 + q1w * qperpx * s110 -
                            2 * q1x * qperpx * s120 - 2 * qperpw * qperpy * s000 * theta +
                            2 * qperpx * qperpz * s000 * theta -
                            2 * q1w * q1x * s010 * theta +
                            2 * qperpw * qperpx * s010 * theta +
                            2 * qperpy * qperpz * s010 * theta +
                            2 * q1x * q1x * s020 * theta + 2 * q1y * q1y * s020 * theta -
                            2 * qperpx * qperpx * s020 * theta -
                            2 * qperpy * qperpy * s020 * theta +
                            q1z * (-(qperpx * s000) - qperpy * s010 + qperpx * s100 +
                                    qperpy * s110 - 2 * q1x * s000 * theta) +
                            q1y * (-(qperpz * s010) + 2 * qperpy * s020 +
                                    qperpw * (s000 - s100) + qperpz * s110 -
                                    2 * qperpy * s120 + 2 * q1w * s000 * theta -
                                    2 * q1z * s010 * theta),
                    q1w * qperpy * s001 - q1x * qperpz * s001 - q1x * qperpw * s011 -
                            q1w * qperpx * s011 + 2 * q1x * qperpx * s021 -
                            q1w * qperpy * s101 + q1x * qperpz * s101 +
                            q1x * qperpw * s111 + q1w * qperpx * s111 -
                            2 * q1x * qperpx * s121 - 2 * qperpw * qperpy * s001 * theta +
                            2 * qperpx * qperpz * s001 * theta -
                            2 * q1w * q1x * s011 * theta +
                            2 * qperpw * qperpx * s011 * theta +
                            2 * qperpy * qperpz * s011 * theta +
                            2 * q1x * q1x * s021 * theta + 2 * q1y * q1y * s021 * theta -
                            2 * qperpx * qperpx * s021 * theta -
                            2 * qperpy * qperpy * s021 * theta +
                            q1z * (-(qperpx * s001) - qperpy * s011 + qperpx * s101 +
                                    qperpy * s111 - 2 * q1x * s001 * theta) +
                            q1y * (-(qperpz * s011) + 2 * qperpy * s021 +
                                    qperpw * (s001 - s101) + qperpz * s111 -
                                    2 * qperpy * s121 + 2 * q1w * s001 * theta -
                                    2 * q1z * s011 * theta),
                    q1w * qperpy * s002 - q1x * qperpz * s002 - q1x * qperpw * s012 -
                            q1w * qperpx * s012 + 2 * q1x * qperpx * s022 -
                            q1w * qperpy * s102 + q1x * qperpz * s102 +
                            q1x * qperpw * s112 + q1w * qperpx * s112 -
                            2 * q1x * qperpx * s122 - 2 * qperpw * qperpy * s002 * theta +
                            2 * qperpx * qperpz * s002 * theta -
                            2 * q1w * q1x * s012 * theta +
                            2 * qperpw * qperpx * s012 * theta +
                            2 * qperpy * qperpz * s012 * theta +
                            2 * q1x * q1x * s022 * theta + 2 * q1y * q1y * s022 * theta -
                            2 * qperpx * qperpx * s022 * theta -
                            2 * qperpy * qperpy * s022 * theta +
                            q1z * (-(qperpx * s002) - qperpy * s012 + qperpx * s102 +
                                    qperpy * s112 - 2 * q1x * s002 * theta) +
                            q1y * (-(qperpz * s012) + 2 * qperpy * s022 +
                                    qperpw * (s002 - s102) + qperpz * s112 -
                                    2 * qperpy * s122 + 2 * q1w * s002 * theta -
                                    2 * q1z * s012 * theta));

            c5[2] = new DerivativeTerm(
                    0, 2 * (qperpw * qperpy * s000 - qperpx * qperpz * s000 +
                            q1y * q1z * s010 - qperpw * qperpx * s010 -
                            qperpy * qperpz * s010 - q1y * q1y * s020 +
                            qperpx * qperpx * s020 + qperpy * qperpy * s020 +
                            q1x * q1z * (s000 - s100) - qperpw * qperpy * s100 +
                            qperpx * qperpz * s100 +
                            q1w * (q1y * (-s000 + s100) + q1x * (s010 - s110)) -
                            q1y * q1z * s110 + qperpw * qperpx * s110 +
                            qperpy * qperpz * s110 + q1y * q1y * s120 -
                            qperpx * qperpx * s120 - qperpy * qperpy * s120 +
                            q1x * q1x * (-s020 + s120)) *
                            theta,
                    2 * (qperpw * qperpy * s001 - qperpx * qperpz * s001 +
                            q1y * q1z * s011 - qperpw * qperpx * s011 -
                            qperpy * qperpz * s011 - q1y * q1y * s021 +
                            qperpx * qperpx * s021 + qperpy * qperpy * s021 +
                            q1x * q1z * (s001 - s101) - qperpw * qperpy * s101 +
                            qperpx * qperpz * s101 +
                            q1w * (q1y * (-s001 + s101) + q1x * (s011 - s111)) -
                            q1y * q1z * s111 + qperpw * qperpx * s111 +
                            qperpy * qperpz * s111 + q1y * q1y * s121 -
                            qperpx * qperpx * s121 - qperpy * qperpy * s121 +
                            q1x * q1x * (-s021 + s121)) *
                            theta,
                    2 * (qperpw * qperpy * s002 - qperpx * qperpz * s002 +
                            q1y * q1z * s012 - qperpw * qperpx * s012 -
                            qperpy * qperpz * s012 - q1y * q1y * s022 +
                            qperpx * qperpx * s022 + qperpy * qperpy * s022 +
                            q1x * q1z * (s002 - s102) - qperpw * qperpy * s102 +
                            qperpx * qperpz * s102 +
                            q1w * (q1y * (-s002 + s102) + q1x * (s012 - s112)) -
                            q1y * q1z * s112 + qperpw * qperpx * s112 +
                            qperpy * qperpz * s112 + q1y * q1y * s122 -
                            qperpx * qperpx * s122 - qperpy * qperpy * s122 +
                            q1x * q1x * (-s022 + s122)) *
                            theta);
        }

    }
    
    public static class MatComponents {
        Vector3f T;
        Quaternion R;
        Matrix4x4 S;
    }
    
    public static MatComponents Decompose(Matrix4x4 m) {
        MatComponents comps = new MatComponents();
        // Extract translation _T_ from transformation matrix
        comps.T.x = m.m[0][3];
        comps.T.y = m.m[1][3];
        comps.T.z = m.m[2][3];

        // Compute new transformation matrix _M_ without translation
        Matrix4x4 M = m;
        for (int i = 0; i < 3; ++i) M.m[i][3] = M.m[3][i] = 0.f;
        M.m[3][3] = 1.f;

        // Extract rotation _R_ from transformation matrix
        float norm;
        int count = 0;
        Matrix4x4 R = M;
        do {
            // Compute next matrix _Rnext_ in series
            Matrix4x4 Rnext = new Matrix4x4();
            Matrix4x4 Rit = Matrix4x4.Inverse(Matrix4x4.Transpose(R));
            for (int i = 0; i < 4; ++i)
                for (int j = 0; j < 4; ++j)
                    Rnext.m[i][j] = 0.5f * (R.m[i][j] + Rit.m[i][j]);

            // Compute norm of difference between _R_ and _Rnext_
            norm = 0;
            for (int i = 0; i < 3; ++i) {
                float n = Math.abs(R.m[i][0] - Rnext.m[i][0]) +
                        Math.abs(R.m[i][1] - Rnext.m[i][1]) +
                        Math.abs(R.m[i][2] - Rnext.m[i][2]);
                norm = Math.max(norm, n);
            }
            R = Rnext;
        } while (++count < 100 && norm > .0001);
        // XXX TODO FIXME deal with flip...
        comps.R = new Quaternion(new Transform(R));

        // Compute scale _S_ using rotation and original matrix
        comps.S = Matrix4x4.Mul(Matrix4x4.Inverse(R), M);
        
        return comps;
    }
    public Transform Interpolate(float time) {
        // Handle boundary conditions for matrix interpolation
        if (!actuallyAnimated || time <= startTime) {
            return startTransform;
        }
        if (time >= endTime) {
            return endTransform;
        }
        float dt = (time - startTime) / (endTime - startTime);
        // Interpolate translation at _dt_
        Vector3f trans = T0.scale(1-dt).add(T1.scale(dt));

        // Interpolate rotation at _dt_
        Quaternion rotate = Quaternion.Slerp(dt, R0, R1);

        // Interpolate scale at _dt_
        Matrix4x4 scale = new Matrix4x4();
        for (int i = 0; i < 3; ++i)
            for (int j = 0; j < 3; ++j)
                scale.m[i][j] = Pbrt.Lerp(dt, S0.m[i][j], S1.m[i][j]);

        // Compute interpolated matrix as product of interpolated components
        return Transform.Translate(trans).concatenate(rotate.ToTransform()).concatenate(new Transform(scale));

    }
    public Ray xform(Ray r) {
        if (!actuallyAnimated || r.time <= startTime)
            return startTransform.xform(r);
        else if (r.time >= endTime)
            return endTransform.xform(r);
        else {
            Transform t = Interpolate(r.time);
            return t.xform(r);
        }
    }
    public RayDifferential xform(RayDifferential r) {
        if (!actuallyAnimated || r.time <= startTime)
            return startTransform.xform(r);
        else if (r.time >= endTime)
            return endTransform.xform(r);
        else {
            Transform t = Interpolate(r.time);
            return t.xform(r);
        }
    }
    public Point3f xform(float time, Point3f p) {
        if (!actuallyAnimated || time <= startTime)
            return startTransform.xform(p);
        else if (time >= endTime)
            return endTransform.xform(p);
        else {
            Transform t = Interpolate(time);
            return t.xform(p);
        }
    }
    public Vector3f xform(float time, Vector3f v) {
        if (!actuallyAnimated || time <= startTime)
            return startTransform.xform(v);
        else if (time >= endTime)
            return endTransform.xform(v);
        else {
            Transform t = Interpolate(time);
            return t.xform(v);
        }
    }

    public boolean HasScale() {
        return startTransform.HasScale() || endTransform.HasScale();
    }
    public Bounds3f MotionBounds(Bounds3f b) {
        if (!actuallyAnimated) return startTransform.xform(b);
        if (hasRotation == false)
            return Bounds3f.Union(startTransform.xform(b), endTransform.xform(b));
        // Return motion bounds accounting for animated rotation
        Bounds3f bounds = new Bounds3f();
        for (int corner = 0; corner < 8; ++corner)
            bounds = Bounds3f.Union(bounds, BoundPointMotion(b.Corner(corner)));
        return bounds;
    }
    Bounds3f BoundPointMotion(Point3f p) {
        if (!actuallyAnimated) return new Bounds3f(startTransform.xform(p));
        Bounds3f bounds = new Bounds3f(startTransform.xform(p), endTransform.xform(p));
        float cosTheta = Quaternion.Dot(R0, R1);
        float theta = (float)Math.acos(Pbrt.Clamp(cosTheta, -1, 1));
        for (int c = 0; c < 3; ++c) {
            // Find any motion derivative zeros for the component _c_
            ArrayList<Float> zeros = IntervalFindZeros(c1[c].Eval(p), c2[c].Eval(p), c3[c].Eval(p),
                    c4[c].Eval(p), c5[c].Eval(p), theta, new Interval(0, 1), 8);

            // Expand bounding box for any motion derivative zeros found
            for (float val : zeros) {
                Point3f pz = xform(Pbrt.Lerp(val, startTime, endTime), p);
                bounds = Bounds3f.Union(bounds, pz);
            }
        }
        return bounds;
    }

}