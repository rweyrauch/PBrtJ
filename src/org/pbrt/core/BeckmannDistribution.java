/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class BeckmannDistribution extends MicrofacetDistribution {

    public BeckmannDistribution(float alphax, float alphay, boolean samplevis) {
        super(samplevis);
        this.alphax = alphax;
        this.alphay = alphay;
    }

    @Override
    public float D(Vector3f wh) {
        float tan2Theta = Reflection.Tan2Theta(wh);
        if (Float.isInfinite(tan2Theta)) return 0;
        float cos4Theta = Reflection.Cos2Theta(wh) * Reflection.Cos2Theta(wh);
        return (float)Math.exp(-tan2Theta * (Reflection.Cos2Phi(wh) / (alphax * alphax) +
                Reflection.Sin2Phi(wh) / (alphay * alphay))) /
                ((float)Math.PI * alphax * alphay * cos4Theta);
    }

    @Override
    public float Lambda(Vector3f w) {
        float absTanTheta = Math.abs(Reflection.TanTheta(w));
        if (Float.isInfinite(absTanTheta)) return 0;
        // Compute _alpha_ for direction _w_
        float alpha =
                (float)Math.sqrt(Reflection.Cos2Phi(w) * alphax * alphax + Reflection.Sin2Phi(w) * alphay * alphay);
        float a = 1 / (alpha * absTanTheta);
        if (a >= 1.6f) return 0;
        return (1 - 1.259f * a + 0.396f * a * a) / (3.535f * a + 2.181f * a * a);
    }

    @Override
    public Vector3f Sample_wh(Vector3f wo, Point2f u) {
        if (!sampleVisibleArea) {
            // Sample full distribution of normals for Beckmann distribution

            // Compute $\tan^2 \theta$ and $\phi$ for Beckmann distribution sample
            float tan2Theta, phi;
            if (alphax == alphay) {
                float logSample = (float)Math.log(u.x);
                if (Float.isInfinite(logSample)) logSample = 0;
                tan2Theta = -alphax * alphax * logSample;
                phi = u.y * 2 * (float)Math.PI;
            } else {
                // Compute _tan2Theta_ and _phi_ for anisotropic Beckmann
                // distribution
                float logSample = (float)Math.log(u.x);
                if (Float.isInfinite(logSample)) logSample = 0;
                phi = (float)Math.atan(alphay / alphax * (float)Math.tan(2 * (float)Math.PI * u.y + 0.5f * (float)Math.PI));
                if (u.y > 0.5f) phi += (float)Math.PI;
                float sinPhi = (float)Math.sin(phi), cosPhi = (float)Math.cos(phi);
                float alphax2 = alphax * alphax, alphay2 = alphay * alphay;
                tan2Theta = -logSample /
                        (cosPhi * cosPhi / alphax2 + sinPhi * sinPhi / alphay2);
            }

            // Map sampled Beckmann angles to normal direction _wh_
            float cosTheta = 1 / (float)Math.sqrt(1 + tan2Theta);
            float sinTheta = (float)Math.sqrt(Math.max(0, 1 - cosTheta * cosTheta));
            Vector3f wh = Vector3f.SphericalDirection(sinTheta, cosTheta, phi);
            if (!Reflection.SameHemisphere(wo, wh)) wh = wh.negate();
            return wh;
        } else {
            // Sample visible area of normals for Beckmann distribution
            Vector3f wh;
            boolean flip = wo.z < 0;
            wh = BeckmannSample(flip ? wo.negate() : wo, alphax, alphay, u.x, u.y);
            if (flip) wh = wh.negate();
            return wh;
        }
    }

    @Override
    public String ToString() {
        return null;
    }

    public static float RoughnessToAlpha(float roughness) {
        roughness = Math.max(roughness, 1e-3f);
        float x = (float)Math.log(roughness);
        return 1.62142f + 0.819955f * x + 0.1734f * x * x +
                0.0171201f * x * x * x + 0.000640711f * x * x * x * x;
    }

    private static Vector3f BeckmannSample(Vector3f wi, float alpha_x, float alpha_y, float U1, float U2) {
        // 1. stretch wi
        Vector3f wiStretched = Vector3f.Normalize(new Vector3f(alpha_x * wi.x, alpha_y * wi.y, wi.z));

        // 2. simulate P22_{wi}(x_slope, y_slope, 1, 1)
        Float slope_x = new Float(0), slope_y = new Float(0);
        BeckmannSample11(Reflection.CosTheta(wiStretched), U1, U2, slope_x, slope_y);

        // 3. rotate
        float tmp = Reflection.CosPhi(wiStretched) * slope_x - Reflection.SinPhi(wiStretched) * slope_y;
        slope_y = Reflection.SinPhi(wiStretched) * slope_x + Reflection.CosPhi(wiStretched) * slope_y;
        slope_x = tmp;

        // 4. unstretch
        slope_x = alpha_x * slope_x;
        slope_y = alpha_y * slope_y;

        // 5. compute normal
        return Vector3f.Normalize(new Vector3f(-slope_x, -slope_y, 1.f));
    }

    private static final float SQRT_PI_INV = 1.f / (float)Math.sqrt(Math.PI);

    private static void BeckmannSample11(float cosThetaI, float U1, float U2, Float slope_x, Float slope_y) {
        /* Special case (normal incidence) */
        if (cosThetaI > .9999f) {
            float r = (float)Math.sqrt(-(float)Math.log(1.0f - U1));
            float sinPhi = (float)Math.sin(2 * (float)Math.PI * U2);
            float cosPhi = (float)Math.cos(2 * (float)Math.PI * U2);
            slope_x = r * cosPhi;
            slope_y = r * sinPhi;
            return;
        }

        /* The original inversion routine from the paper contained
           discontinuities, which causes issues for QMC integration
           and techniques like Kelemen-style MLT. The following code
           performs a numerical inversion with better behavior */
        float sinThetaI = (float)Math.sqrt(Math.max(0, 1 - cosThetaI * cosThetaI));
        float tanThetaI = sinThetaI / cosThetaI;
        float cotThetaI = 1 / tanThetaI;

        /* Search interval -- everything is parameterized
           in the Erf() domain */
        float a = -1, c = Pbrt.Erf(cotThetaI);
        float sample_x = Math.max(U1, 1e-6f);

        /* Start with a good initial guess */
        // Float b = (1-sample_x) * a + sample_x * c;

        /* We can do better (inverse of an approximation computed in
         * Mathematica) */
        float thetaI = (float)Math.acos(cosThetaI);
        float fit = 1 + thetaI * (-0.876f + thetaI * (0.4265f - 0.0594f * thetaI));
        float b = c - (1 + c) * (float)Math.pow(1 - sample_x, fit);

        /* Normalization factor for the CDF */
        float normalization = 1 / (1 + c + SQRT_PI_INV * tanThetaI * (float)Math.exp(-cotThetaI * cotThetaI));

        int it = 0;
        while (++it < 10) {
            /* Bisection criterion -- the oddly-looking
               Boolean expression are intentional to check
               for NaNs at little additional cost */
            if (!(b >= a && b <= c)) b = 0.5f * (a + c);

            /* Evaluate the CDF and its derivative
               (i.e. the density function) */
            float invErf = Pbrt.ErfInv(b);
            float value = normalization * (1 + b + SQRT_PI_INV * tanThetaI * (float)Math.exp(-invErf * invErf)) - sample_x;
            float derivative = normalization * (1 - invErf * tanThetaI);

            if (Math.abs(value) < 1e-5f) break;

            /* Update bisection intervals */
            if (value > 0)
                c = b;
            else
                a = b;

            b -= value / derivative;
        }

        /* Now convert back into a slope value */
        slope_x = Pbrt.ErfInv(b);

        /* Simulate Y component */
        slope_y = Pbrt.ErfInv(2.0f * Math.max(U2, 1e-6f) - 1.0f);

        assert(!Float.isInfinite(slope_x));
        assert(!Float.isNaN(slope_x));
        assert(!Float.isInfinite(slope_y));
        assert(!Float.isNaN(slope_y));
    }

    private final float alphax, alphay;
}