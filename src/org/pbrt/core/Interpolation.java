/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class Interpolation {

    // Spline Interpolation Declarations
    public static float CatmullRom(int size, float[] nodes, float[] values, float x) {
        if (!(x >= nodes[0] && x <= nodes[size - 1])) return 0;
        int idx = Pbrt.FindInterval(size, i -> nodes[i] <= x);
        float x0 = nodes[idx], x1 = nodes[idx + 1];
        float f0 = values[idx], f1 = values[idx + 1];
        float width = x1 - x0;
        float d0, d1;
        if (idx > 0)
            d0 = width * (f1 - values[idx - 1]) / (x1 - nodes[idx - 1]);
        else
            d0 = f1 - f0;

        if (idx + 2 < size)
            d1 = width * (values[idx + 2] - f0) / (nodes[idx + 2] - x0);
        else
            d1 = f1 - f0;

        float t = (x - x0) / (x1 - x0), t2 = t * t, t3 = t2 * t;
        return (2 * t3 - 3 * t2 + 1) * f0 + (-2 * t3 + 3 * t2) * f1 +
                (t3 - 2 * t2 + t) * d0 + (t3 - t2) * d1;
    }

    public static class Weights {
        public int offset;
        public float[] weights;
    }
    public static Weights CatmullRomWeights(int size, float[] nodes, float x) {
        // Return _false_ if _x_ is out of bounds
        if (!(x >= nodes[0] && x <= nodes[size - 1])) return null;

        Weights weights = new Weights();
        // Search for the interval _idx_ containing _x_
        int idx = Pbrt.FindInterval(size, i -> nodes[i] <= x);
        weights.offset = idx - 1;
        weights.weights = new float[4];

        float x0 = nodes[idx], x1 = nodes[idx + 1];

        // Compute the $t$ parameter and powers
        float t = (x - x0) / (x1 - x0), t2 = t * t, t3 = t2 * t;

        // Compute initial node weights $w_1$ and $w_2$
        weights.weights[1] = 2 * t3 - 3 * t2 + 1;
        weights.weights[2] = -2 * t3 + 3 * t2;

        // Compute first node weight $w_0$
        if (idx > 0) {
            float w0 = (t3 - 2 * t2 + t) * (x1 - x0) / (x1 - nodes[idx - 1]);
            weights.weights[0] = -w0;
            weights.weights[2] += w0;
        } else {
            float w0 = t3 - 2 * t2 + t;
            weights.weights[0] = 0;
            weights.weights[1] -= w0;
            weights.weights[2] += w0;
        }

        // Compute last node weight $w_3$
        if (idx + 2 < size) {
            float w3 = (t3 - t2) * (x1 - x0) / (nodes[idx + 2] - x0);
            weights.weights[1] -= w3;
            weights.weights[3] = w3;
        } else {
            float w3 = t3 - t2;
            weights.weights[1] -= w3;
            weights.weights[2] += w3;
            weights.weights[3] = 0;
        }
        return weights;
    }

    public static class SampleCR {
        public float sample;
        public float fval;
        public float pdf;
    }
    public static SampleCR SampleCatmullRom(int n, float[] x, float[] f, float[] F, float u) {
        // Map _u_ to a spline interval by inverting _F_
        u *= F[n - 1];
        float su = u;
        int i = Pbrt.FindInterval(n, index -> F[index] <= su);

        // Look up $x_i$ and function values of spline segment _i_
        float x0 = x[i], x1 = x[i + 1];
        float f0 = f[i], f1 = f[i + 1];
        float width = x1 - x0;

        // Approximate derivatives using finite differences
        float d0, d1;
        if (i > 0)
            d0 = width * (f1 - f[i - 1]) / (x1 - x[i - 1]);
        else
            d0 = f1 - f0;
        if (i + 2 < n)
            d1 = width * (f[i + 2] - f0) / (x[i + 2] - x0);
        else
            d1 = f1 - f0;

        // Re-scale _u_ for continuous spline sampling step
        u = (u - F[i]) / width;

        // Invert definite integral over spline segment and return solution

        // Set initial guess for $t$ by importance sampling a linear interpolant
        float t;
        if (f0 != f1)
            t = (f0 - (float)Math.sqrt(Math.max(0, f0 * f0 + 2 * u * (f1 - f0)))) / (f0 - f1);
        else
            t = u / f0;

        float a = 0, b = 1, Fhat, fhat;
        while (true) {
            // Fall back to a bisection step when _t_ is out of bounds
            if (!(t > a && t < b)) t = 0.5f * (a + b);

            // Evaluate target function and its derivative in Horner form
            Fhat = t * (f0 +
                    t * (.5f * d0 +
                            t * ((1.f / 3.f) * (-2 * d0 - d1) + f1 - f0 +
                                    t * (.25f * (d0 + d1) + .5f * (f0 - f1)))));
            fhat = f0 +
                    t * (d0 +
                            t * (-2 * d0 - d1 + 3 * (f1 - f0) +
                                    t * (d0 + d1 + 2 * (f0 - f1))));

            // Stop the iteration if converged
            if (Math.abs(Fhat - u) < 1e-6f || b - a < 1e-6f) break;

            // Update bisection bounds using updated _t_
            if (Fhat - u < 0)
                a = t;
            else
                b = t;

            // Perform a Newton step
            t -= (Fhat - u) / fhat;
        }

        // Return the sample position and function value
        SampleCR samp = new SampleCR();
        samp.fval = fhat;
        samp.pdf = fhat / F[n - 1];
        samp.sample = x0 + width * t;
        return samp;
    }

    // Define a lambda function to interpolate table entries
    private static float interpolate(float[] array, int idx, float weights[], int offset, int size2) {
        float value = 0;
        for (int i = 0; i < 4; ++i)
            if (weights[i] != 0)
                value += array[(offset + i) * size2 + idx] * weights[i];
        return value;
    }

    public static SampleCR SampleCatmullRom2D(int size1, int size2, float[] nodes1,
                         float[] nodes2, float[] values,
                         float[] cdf, float alpha, float u) {
        // Determine offset and coefficients for the _alpha_ parameter
        Weights weights = CatmullRomWeights(size1, nodes1, alpha);
        if (weights == null) return null;

        // Map _u_ to a spline interval by inverting the interpolated _cdf_
        float maximum = interpolate(cdf, size2 - 1, weights.weights, weights.offset, size2);
        u *= maximum;
        float su = u;
        int idx = Pbrt.FindInterval(size2, i -> interpolate(cdf, i, weights.weights, weights.offset, size2) <= su);

        // Look up node positions and interpolated function values
        Float f0 = interpolate(values, idx, weights.weights, weights.offset, size2), f1 = interpolate(values, idx + 1, weights.weights, weights.offset, size2);
        Float x0 = nodes2[idx], x1 = nodes2[idx + 1];
        Float width = x1 - x0;
        Float d0, d1;

        // Re-scale _u_ using the interpolated _cdf_
        u = (u - interpolate(cdf, idx, weights.weights, weights.offset, size2)) / width;

        // Approximate derivatives using finite differences of the interpolant
        if (idx > 0)
            d0 = width * (f1 - interpolate(values, idx - 1, weights.weights, weights.offset, size2)) /
                    (x1 - nodes2[idx - 1]);
        else
            d0 = f1 - f0;
        if (idx + 2 < size2)
            d1 = width * (interpolate(values, idx + 2, weights.weights, weights.offset, size2) - f0) /
                    (nodes2[idx + 2] - x0);
        else
            d1 = f1 - f0;

        // Invert definite integral over spline segment and return solution

        // Set initial guess for $t$ by importance sampling a linear interpolant
        float t;
        if (f0 != f1)
            t = (f0 - (float)Math.sqrt(Math.max(0, f0 * f0 + 2 * u * (f1 - f0)))) / (f0 - f1);
        else
            t = u / f0;

        float a = 0, b = 1, Fhat, fhat;
        while (true) {
            // Fall back to a bisection step when _t_ is out of bounds
            if (!(t >= a && t <= b)) t = 0.5f * (a + b);

            // Evaluate target function and its derivative in Horner form
            Fhat = t * (f0 +
                    t * (.5f * d0 +
                            t * ((1.f / 3.f) * (-2 * d0 - d1) + f1 - f0 +
                                    t * (.25f * (d0 + d1) + .5f * (f0 - f1)))));
            fhat = f0 +
                    t * (d0 +
                            t * (-2 * d0 - d1 + 3 * (f1 - f0) +
                                    t * (d0 + d1 + 2 * (f0 - f1))));

            // Stop the iteration if converged
            if (Math.abs(Fhat - u) < 1e-6f || b - a < 1e-6f) break;

            // Update bisection bounds using updated _t_
            if (Fhat - u < 0)
                a = t;
            else
                b = t;

            // Perform a Newton step
            t -= (Fhat - u) / fhat;
        }

        // Return the sample position and function value
        SampleCR samp = new SampleCR();
        samp.fval = fhat;
        samp.pdf = fhat / maximum;
        samp.sample = x0 + width * t;
        return samp;
    }

    public static class IntegCR {
        public float[] cdf;
        public float sum;
    }
    public static IntegCR IntegrateCatmullRom(int n, float[] x, float[] values) {
        IntegCR result = new IntegCR();

        result.sum = 0;
        result.cdf = new float[n+1];

        for (int i = 0; i < n - 1; ++i) {
            // Look up $x_i$ and function values of spline segment _i_
            float x0 = x[i], x1 = x[i + 1];
            float f0 = values[i], f1 = values[i + 1];
            float width = x1 - x0;

            // Approximate derivatives using finite differences
            float d0, d1;
            if (i > 0)
                d0 = width * (f1 - values[i - 1]) / (x1 - x[i - 1]);
            else
                d0 = f1 - f0;
            if (i + 2 < n)
                d1 = width * (values[i + 2] - f0) / (x[i + 2] - x0);
            else
                d1 = f1 - f0;

            // Keep a running sum and build a cumulative distribution function
            result.sum += ((d0 - d1) * (1.f / 12.f) + (f0 + f1) * .5f) * width;
            result.cdf[i + 1] = result.sum;
        }
        return result;
    }
    public static float InvertCatmullRom(int n, float[] x, float[] values, float u) {
        // Stop when _u_ is out of bounds
        if (!(u > values[0]))
            return x[0];
        else if (!(u < values[n - 1]))
            return x[n - 1];

        // Map _u_ to a spline interval by inverting _values_
        int i = Pbrt.FindInterval(n, index -> values[index] <= u);

        // Look up $x_i$ and function values of spline segment _i_
        float x0 = x[i], x1 = x[i + 1];
        float f0 = values[i], f1 = values[i + 1];
        float width = x1 - x0;

        // Approximate derivatives using finite differences
        float d0, d1;
        if (i > 0)
            d0 = width * (f1 - values[i - 1]) / (x1 - x[i - 1]);
        else
            d0 = f1 - f0;
        if (i + 2 < n)
            d1 = width * (values[i + 2] - f0) / (x[i + 2] - x0);
        else
            d1 = f1 - f0;

        // Invert the spline interpolant using Newton-Bisection
        float a = 0, b = 1, t = .5f;
        float Fhat, fhat;
        while (true) {
            // Fall back to a bisection step when _t_ is out of bounds
            if (!(t > a && t < b)) t = 0.5f * (a + b);

            // Compute powers of _t_
            float t2 = t * t, t3 = t2 * t;

            // Set _Fhat_ using Equation (8.27)
            Fhat = (2 * t3 - 3 * t2 + 1) * f0 + (-2 * t3 + 3 * t2) * f1 +
                    (t3 - 2 * t2 + t) * d0 + (t3 - t2) * d1;

            // Set _fhat_ using Equation (not present)
            fhat = (6 * t2 - 6 * t) * f0 + (-6 * t2 + 6 * t) * f1 +
                    (3 * t2 - 4 * t + 1) * d0 + (3 * t2 - 2 * t) * d1;

            // Stop the iteration if converged
            if (Math.abs(Fhat - u) < 1e-6f || b - a < 1e-6f) break;

            // Update bisection bounds using updated _t_
            if (Fhat - u < 0)
                a = t;
            else
                b = t;

            // Perform a Newton step
            t -= (Fhat - u) / fhat;
        }
        return x0 + t * width;
    }

    // Fourier Interpolation Declarations
    public static float Fourier(float[] a, int m, double cosPhi) {
        double value = 0.0;
        // Initialize cosine iterates
        double cosKMinusOnePhi = cosPhi;
        double cosKPhi = 1;
        for (int k = 0; k < m; ++k) {
            // Add the current summand and update the cosine iterates
            value += a[k] * cosKPhi;
            double cosKPlusOnePhi = 2 * cosPhi * cosKPhi - cosKMinusOnePhi;
            cosKMinusOnePhi = cosKPhi;
            cosKPhi = cosKPlusOnePhi;
        }
        return (float)value;
    }

    public static class SampleF {
        float sample;
        float pdf;
        float phiPtr;
    }
    public static SampleF SampleFourier(float[] ak, float[] recip, int m, float u) {
        // Pick a side and declare bisection variables
        boolean flip = (u >= 0.5);
        if (flip)
            u = 1 - 2 * (u - .5f);
        else
            u *= 2;
        double a = 0, b = Math.PI, phi = 0.5 * Math.PI;
        double F, f;
        while (true) {
            // Evaluate $F(\phi)$ and its derivative $f(\phi)$

            // Initialize sine and cosine iterates
            double cosPhi = Math.cos(phi);
            double sinPhi = Math.sqrt(Math.max(0, 1 - cosPhi * cosPhi));
            double cosPhiPrev = cosPhi, cosPhiCur = 1;
            double sinPhiPrev = -sinPhi, sinPhiCur = 0;

            // Initialize _F_ and _f_ with the first series term
            F = ak[0] * phi;
            f = ak[0];
            for (int k = 1; k < m; ++k) {
                // Compute next sine and cosine iterates
                double sinPhiNext = 2 * cosPhi * sinPhiCur - sinPhiPrev;
                double cosPhiNext = 2 * cosPhi * cosPhiCur - cosPhiPrev;
                sinPhiPrev = sinPhiCur;
                sinPhiCur = sinPhiNext;
                cosPhiPrev = cosPhiCur;
                cosPhiCur = cosPhiNext;

                // Add the next series term to _F_ and _f_
                F += ak[k] * recip[k] * sinPhiNext;
                f += ak[k] * cosPhiNext;
            }
            F -= u * ak[0] * Math.PI;

            // Update bisection bounds using updated $\phi$
            if (F > 0)
                b = phi;
            else
                a = phi;

            // Stop the Fourier bisection iteration if converged
            if (Math.abs(F) < 1e-6f || b - a < 1e-6f) break;

            // Perform a Newton step given $f(\phi)$ and $F(\phi)$
            phi -= F / f;

            // Fall back to a bisection step when $\phi$ is out of bounds
            if (!(phi > a && phi < b)) phi = 0.5f * (a + b);
        }
        // Potentially flip $\phi$ and return the result
        if (flip) phi = 2 * Math.PI - phi;

        SampleF result = new SampleF();
        result.sample = (float)f;
        result.pdf = (float)((1 / 2 * Math.PI) * f / ak[0]);
        result.phiPtr = (float)phi;
        return result;
    }

}