/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class TrowbridgeReitzDistribution extends MicrofacetDistribution {

    public TrowbridgeReitzDistribution(float alphax, float alphay, boolean samplevis) {
        super(samplevis);
        this.alphax = alphax;
        this.alphay = alphay;
    }

    @Override
    public float D(Vector3f wh) {
        return 0;
    }

    @Override
    public float Lambda(Vector3f w) {
        return 0;
    }

    @Override
    public Vector3f Sample_wh(Vector3f wo, Point2f u) {
        Vector3f wh;
        if (!sampleVisibleArea) {
            float cosTheta, phi = (2 * (float)Math.PI) * u.y;
            if (alphax == alphay) {
                float tanTheta2 = alphax * alphax * u.x / (1.0f - u.x);
                cosTheta = 1 / (float)Math.sqrt(1 + tanTheta2);
            } else {
                phi = (float)Math.atan(alphay / alphax * (float)Math.tan(2 * (float)Math.PI * u.y + .5f * (float)Math.PI));
                if (u.y > .5f) phi += (float)Math.PI;
                float sinPhi = (float)Math.sin(phi), cosPhi = (float)Math.cos(phi);
                final float alphax2 = alphax * alphax, alphay2 = alphay * alphay;
                final float alpha2 = 1 / (cosPhi * cosPhi / alphax2 + sinPhi * sinPhi / alphay2);
                float tanTheta2 = alpha2 * u.x / (1 - u.x);
                cosTheta = 1 / (float)Math.sqrt(1 + tanTheta2);
            }
            float sinTheta = (float)Math.sqrt(Math.max(0, 1 - cosTheta * cosTheta));
            wh = Vector3f.SphericalDirection(sinTheta, cosTheta, phi);
            if (!Reflection.SameHemisphere(wo, wh)) wh = wh.negate();
        } else {
            boolean flip = wo.z < 0;
            wh = TrowbridgeReitzSample(flip ? wo.negate() : wo, alphax, alphay, u.x, u.y);
            if (flip) wh = wh.negate();
        }
        return wh;
    }

    @Override
    public String toString() {
        return String.format("[ TrowbridgeReitzDistribution alphax: %f alphay: %f ]", alphax, alphay);
    }

    public static float RoughnessToAlpha(float roughness) {
        roughness = Math.max(roughness, 1e-3f);
        float x = (float)Math.log(roughness);
        return 1.62142f + 0.819955f * x + 0.1734f * x * x + 0.0171201f * x * x * x + 0.000640711f * x * x * x * x;
    }

    private static class Sample11 {
        public float slope_x, slope_y;
    }
    static Sample11 TrowbridgeReitzSample11(float cosTheta, float U1, float U2) {

        Sample11 samp = new Sample11();
        // special case (normal incidence)
        if (cosTheta > .9999f) {
            float r = (float)Math.sqrt(U1 / (1 - U1));
            float phi = 6.28318530718f * U2;
            samp.slope_x = r * (float)Math.cos(phi);
            samp.slope_y = r * (float)Math.sin(phi);
            return samp;
        }

        float sinTheta = (float)Math.sqrt(Math.max(0, 1 - cosTheta * cosTheta));
        float tanTheta = sinTheta / cosTheta;
        float a = 1 / tanTheta;
        float G1 = 2 / (1 + (float)Math.sqrt(1 + 1 / (a * a)));

        // sample slope_x
        float A = 2 * U1 / G1 - 1;
        float tmp = 1 / (A * A - 1);
        if (tmp > 1e10f) tmp = 1e10f;
        float B = tanTheta;
        float D = (float)Math.sqrt(Math.max(B * B * tmp * tmp - (A * A - B * B) * tmp, 0));
        float slope_x_1 = B * tmp - D;
        float slope_x_2 = B * tmp + D;

        samp.slope_x = (A < 0 || slope_x_2 > 1 / tanTheta) ? slope_x_1 : slope_x_2;

        // sample slope_y
        float S;
        if (U2 > 0.5f) {
            S = 1.f;
            U2 = 2.f * (U2 - .5f);
        } else {
            S = -1.f;
            U2 = 2.f * (.5f - U2);
        }
        float z = (U2 * (U2 * (U2 * 0.27385f - 0.73369f) + 0.46341f)) /
                (U2 * (U2 * (U2 * 0.093073f + 0.309420f) - 1.000000f) + 0.597999f);
        samp.slope_y = S * z * (float)Math.sqrt(1.f + samp.slope_x * samp.slope_x);

        assert (!Float.isInfinite(samp.slope_y));
        assert (!Float.isNaN(samp.slope_y));

        return samp;
    }

    private static Vector3f TrowbridgeReitzSample(Vector3f wi, float alpha_x,
                                          float alpha_y, float U1, float U2) {
        // 1. stretch wi
        Vector3f wiStretched = Vector3f.Normalize(new Vector3f(alpha_x * wi.x, alpha_y * wi.y, wi.z));

        // 2. simulate P22_{wi}(x_slope, y_slope, 1, 1)
        Sample11 samp = TrowbridgeReitzSample11(Reflection.CosTheta(wiStretched), U1, U2);

        // 3. rotate
        float tmp = Reflection.CosPhi(wiStretched) * samp.slope_x - Reflection.SinPhi(wiStretched) * samp.slope_y;
        samp.slope_y = Reflection.SinPhi(wiStretched) * samp.slope_x + Reflection.CosPhi(wiStretched) * samp.slope_y;
        samp.slope_x = tmp;

        // 4. unstretch
        samp.slope_x = alpha_x * samp.slope_x;
        samp.slope_y = alpha_y * samp.slope_y;

        // 5. compute normal
        return Vector3f.Normalize(new Vector3f(-samp.slope_x, -samp.slope_y, 1));
    }

    private final float alphax, alphay;
}