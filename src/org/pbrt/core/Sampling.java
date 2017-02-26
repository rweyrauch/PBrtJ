/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class Sampling {

    public static Float[] StratifiedSample1D(Float[] samp, int firstIndex, int endIndex, RNG rng, boolean jitter) {
        int nsamples = endIndex-firstIndex;
        float invNSamples = 1 / (float)nsamples;
        for (int i = firstIndex; i < endIndex; ++i) {
            float delta = jitter ? rng.UniformFloat() : 0.5f;
            samp[i] = Math.min((i + delta) * invNSamples, Pbrt.OneMinusEpsilon);
        }
        return samp;
    }

    public static Float[] StratifiedSample1D(Float[] samp, int nsamples, RNG rng, boolean jitter) {
        return StratifiedSample1D(samp, 0, nsamples, rng, jitter);
    }

    public static Float[] StratifiedSample1D(Float[] samp, int nsamples, RNG rng) {
        return StratifiedSample1D(samp, nsamples, rng, true);
    }

    public static Point2f[] StratifiedSample2D(Point2f[] samp, int nx, int ny, RNG rng, boolean jitter) {
        float dx = 1 / (float)nx, dy = 1 / (float)ny;
        int i = 0;
        for (int y = 0; y < ny; ++y) {
            for (int x = 0; x < nx; ++x) {
                float jx = jitter ? rng.UniformFloat() : 0.5f;
                float jy = jitter ? rng.UniformFloat() : 0.5f;
                samp[i].x = Math.min((x + jx) * dx, Pbrt.OneMinusEpsilon);
                samp[i].y = Math.min((y + jy) * dy, Pbrt.OneMinusEpsilon);
                i++;
            }
        }
        return samp;
    }

    public static Point2f[] StratifiedSample2D(Point2f[] samp, int nx, int ny, RNG rng) {
        return StratifiedSample2D(samp, nx, ny, rng, true);
    }

    public static Float[] LatinHypercube(Float[] samples, int startIndex, int endIndex, RNG rng) {
        // Generate LHS samples along diagonal
        int nSamples = endIndex - startIndex;
        float invNSamples = 1 / (float)nSamples;
        for (int i = startIndex; i < endIndex; ++i) {
                float sj = (i + (rng.UniformFloat())) * invNSamples;
                samples[i] = Math.min(sj, Pbrt.OneMinusEpsilon);
        }

        // Permute LHS samples in each dimension
        for (int j = startIndex; j < endIndex; ++j) {
            int other = j + rng.UniformInt32(nSamples - j);
            float temp = samples[j];
            samples[j] = samples[other];
            samples[other] = temp;
        }
        return samples;
    }

    public static Float[] LatinHypercube(Float[] samples, int nSamples, RNG rng) {
        return LatinHypercube(samples, 0, nSamples, rng);
    }

    public static Point2f[] LatinHypercube(Point2f[] samples, int startIndex, int endIndex, RNG rng) {
        // Generate LHS samples along diagonal
        int nSamples = endIndex - startIndex;
        float invNSamples = 1 / (float)nSamples;
        for (int i = startIndex; i < endIndex; ++i) {
            float sj = (i + (rng.UniformFloat())) * invNSamples;
            samples[i].x = Math.min(sj, Pbrt.OneMinusEpsilon);
        }
        for (int i = startIndex; i < endIndex; ++i) {
            float sj = (i + (rng.UniformFloat())) * invNSamples;
            samples[i].y = Math.min(sj, Pbrt.OneMinusEpsilon);
        }
        // Permute LHS samples in each dimension
        for (int j = startIndex; j < endIndex; ++j) {
            int other = j + rng.UniformInt32(nSamples - j);
            Point2f temp = samples[j];
            samples[j] = samples[other];
            samples[other] = temp;
        }
        return samples;
    }
    public static Point2f[] LatinHypercube(Point2f[] samples, int nSamples, RNG rng) {
        return LatinHypercube(samples, 0, nSamples, rng);
    }

    public static Point2f RejectionSampleDisk(RNG rng) {
        Point2f p = new Point2f();
        do {
            p.x = 1 - 2 * rng.UniformFloat();
            p.y = 1 - 2 * rng.UniformFloat();
        } while (p.x * p.x + p.y * p.y > 1);
        return p;
    }

    public static Vector3f UniformSampleHemisphere(Point2f u) {
        float z = u.x;
        float r = (float)Math.sqrt(Math.max(0, 1 - z * z));
        float phi = 2 * (float)Math.PI * u.y;
        return new Vector3f(r * (float)Math.cos(phi), r * (float)Math.sin(phi), z);
    }
    public static float UniformHemispherePdf() {
        return 1 / (float)Math.PI;
    }

    public static Vector3f UniformSampleSphere(Point2f u) {
        float z = 1 - 2 * u.x;
        float r = (float)Math.sqrt(Math.max(0, 1 - z * z));
        float phi = 2 * (float)Math.PI * u.y;
        return new Vector3f(r * (float)Math.cos(phi), r * (float)Math.sin(phi), z);
    }
    public static float UniformSpherePdf() {
        return 1 / (4 * (float)Math.PI);
    }
    public static Vector3f UniformSampleCone(Point2f u, float cosThetaMax) {
        float cosTheta = (1 - u.x) + u.x * cosThetaMax;
        float sinTheta = (float)Math.sqrt(1 - cosTheta * cosTheta);
        float phi = u.y * 2 * (float)Math.PI;
        return new Vector3f((float)Math.cos(phi) * sinTheta, (float)Math.sin(phi) * sinTheta, cosTheta);
    }
    public static Vector3f UniformSampleCone(Point2f u, float cosThetaMax, Vector3f x, Vector3f y, Vector3f z) {
        float cosTheta = Pbrt.Lerp(u.x, cosThetaMax, 1.f);
        float sinTheta = (float)Math.sqrt(1 - cosTheta * cosTheta);
        float phi = u.y * 2 * (float)Math.PI;
        return x.scale((float)Math.cos(phi) * sinTheta).add(y.scale((float)Math.sin(phi) * sinTheta).add(z.scale(cosTheta)));
    }
    public static float UniformConePdf(float cosThetaMax) {
        return 1 / (2 * (float)Math.PI * (1 - cosThetaMax));
    }

    public static Point2f UniformSampleDisk(Point2f u) {
        float r = (float)Math.sqrt(u.x);
        float theta = 2 * (float)Math.PI * u.y;
        return new Point2f(r * (float)Math.cos(theta), r * (float)Math.sin(theta));
    }

    public static Point2f UniformSampleTriangle(Point2f u) {
        float su0 = (float)Math.sqrt(u.x);
        return new Point2f(1 - su0, u.y * su0);
    }

    public static Point2f ConcentricSampleDisk(Point2f u) {
        // Map uniform random numbers to $[-1,1]^2$
        Point2f uOffset = u.scale(2).subtract(new Vector2f(1, 1));

        // Handle degeneracy at the origin
        if (uOffset.x == 0 && uOffset.y == 0) return new Point2f(0, 0);

        float PiOver4 = (float)Math.PI / 4;
        float PiOver2 = (float)Math.PI / 2;

        // Apply concentric mapping to point
        float theta, r;
        if (Math.abs(uOffset.x) > Math.abs(uOffset.y)) {
            r = uOffset.x;
            theta = PiOver4 * (uOffset.y / uOffset.x);
        } else {
            r = uOffset.y;
            theta = PiOver2 - PiOver4 * (uOffset.x / uOffset.y);
        }
        return new Point2f((float)Math.cos(theta), (float)Math.sin(theta)).scale(r);
    }

    public static <T> T[] Shuffle(T[] samp, int startIndex, int endIndex, int nDimensions, RNG rng) {
        int count = endIndex-startIndex;
        for (int i = startIndex; i < endIndex; ++i) {
            int other = i + rng.UniformInt32(count - (i - startIndex));
            for (int j = 0; j < nDimensions; ++j) {
                T temp = samp[nDimensions * i + j];
                samp[nDimensions * i + j] = samp[nDimensions * other + j];
                samp[nDimensions * other + j] = temp;
            }
        }
        return samp;
    }

    public static Vector3f CosineSampleHemisphere(Point2f u) {
        Point2f d = ConcentricSampleDisk(u);
        float z = (float)Math.sqrt(Math.max(0, 1 - d.x * d.x - d.y * d.y));
        return new Vector3f(d.x, d.y, z);
    }

    public static float CosineHemispherePdf(float cosTheta) { return cosTheta / (float)Math.PI; }

}