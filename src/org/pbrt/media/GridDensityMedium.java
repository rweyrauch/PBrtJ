
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.media;

import org.pbrt.core.*;
import org.pbrt.core.Error;

public class GridDensityMedium extends Medium {

    // GridDensityMedium Private Data
    private Spectrum sigma_a, sigma_s;
    private float g;
    private int nx, ny, nz;
    private Transform WorldToMedium;
    private Float[] density;
    private float sigma_t;
    private float invMaxDensity;

    private int nTrSteps = 0, nTrCalls = 0;

    public GridDensityMedium(Spectrum sig_a, Spectrum sig_s, float g, int nx, int ny, int nz, Transform concatenate, Float[] data) {
        super();
        this.sigma_a = sig_a;
        this.sigma_s = sig_s;
        this.g = g;
        this.nx = nx;
        this.ny = ny;
        this.nz = nz;
        this.WorldToMedium = Transform.Inverse(concatenate);
        this.density = new Float[nx * ny * nz];
        this.density = data.clone();
        // Precompute values for Monte Carlo sampling of _GridDensityMedium_
        this.sigma_t = Spectrum.Add(sigma_a,sigma_s).at(0);
        if (new Spectrum(sigma_t).notEqual(Spectrum.Add(sigma_a,sigma_s)))
            Error.Error("GridDensityMedium requires a spectrally uniform attenuation coefficient!");
        float maxDensity = 0;
        for (int i = 0; i < nx * ny * nz; ++i)
            maxDensity = Math.max(maxDensity, density[i]);
        invMaxDensity = 1 / maxDensity;
    }

    public float Density(Point3f p) {
        // Compute voxel coordinates and offsets for _p_
        Point3f pSamples = new Point3f(p.x * nx - .5f, p.y * ny - .5f, p.z * nz - .5f);
        Point3i pi = new Point3i(Point3f.Floor(pSamples));
        Vector3f d = pSamples.subtract(new Point3f(pi));

        // Trilinearly interpolate density values to compute local density
        float d00 = Pbrt.Lerp(d.x, D(pi), D(pi.add(new Vector3i(1, 0, 0))));
        float d10 = Pbrt.Lerp(d.x, D(pi.add(new Vector3i(0, 1, 0))), D(pi.add(new Vector3i(1, 1, 0))));
        float d01 = Pbrt.Lerp(d.x, D(pi.add(new Vector3i(0, 0, 1))), D(pi.add(new Vector3i(1, 0, 1))));
        float d11 = Pbrt.Lerp(d.x, D(pi.add(new Vector3i(0, 1, 1))), D(pi.add(new Vector3i(1, 1, 1))));
        float d0 = Pbrt.Lerp(d.y, d00, d10);
        float d1 = Pbrt.Lerp(d.y, d01, d11);
        return Pbrt.Lerp(d.z, d0, d1);
    }
    float D(Point3i p) {
        Bounds3i sampleBounds = new Bounds3i(new Point3i(0, 0, 0), new Point3i(nx, ny, nz));
        if (!Bounds3i.InsideExclusive(p, sampleBounds)) return 0;
        return density[(p.z * ny + p.y) * nx + p.x];
    }

    @Override
    public Spectrum Tr(Ray rWorld, Sampler sampler) {
        ++nTrCalls;

        Ray ray = WorldToMedium.xform(new Ray(rWorld.o, Vector3f.Normalize(rWorld.d), rWorld.tMax * rWorld.d.Length(), 0, null));
        // Compute $[\tmin, \tmax]$ interval of _ray_'s overlap with medium bounds
        Bounds3f b = new Bounds3f(new Point3f(0, 0, 0), new Point3f(1, 1, 1));
        Bounds3f.BoundIntersect isect = b.IntersectP(ray);
        if (isect == null) return new Spectrum(1);

        // Perform ratio tracking to estimate the transmittance value
        float Tr = 1, t = isect.hit0;
        while (true) {
            ++nTrSteps;
            t -= Math.log(1 - sampler.Get1D()) * invMaxDensity / sigma_t;
            if (t >= isect.hit1) break;
            float density = Density(ray.at(t));
            Tr *= 1 - Math.max(0, density * invMaxDensity);
            // Added after book publication: when transmittance gets low,
            // start applying Russian roulette to terminate sampling.
            float rrThreshold = .1f;
            if (Tr < rrThreshold) {
                float q = Math.max(.05f, 1 - Tr);
                if (sampler.Get1D() < q) return new Spectrum(0);
                Tr /= 1 - q;
            }
        }
        return new Spectrum(Tr);
    }

    @Override
    public MediumSample Sample(Ray rWorld, Sampler sampler) {
        MediumSample ms = new MediumSample();
        ms.spectrum = new Spectrum(1);
        ms.mi = null;

        Ray ray = WorldToMedium.xform(new Ray(rWorld.o, Vector3f.Normalize(rWorld.d), rWorld.tMax * rWorld.d.Length(), 0, null));
        // Compute $[\tmin, \tmax]$ interval of _ray_'s overlap with medium bounds
        Bounds3f b = new Bounds3f(new Point3f(0, 0, 0), new Point3f(1, 1, 1));
        Bounds3f.BoundIntersect isect = b.IntersectP(ray);
        if (isect == null) return ms;

        // Run delta-tracking iterations to sample a medium interaction
        float t = isect.hit0;
        while (true) {
            t -= Math.log(1 - sampler.Get1D()) * invMaxDensity / sigma_t;
            if (t >= isect.hit1) break;
            if (Density(ray.at(t)) * invMaxDensity > sampler.Get1D()) {
                // Populate _mi_ with medium interaction information and return
                PhaseFunction phase = new HenyeyGreenstein(g);
                ms.mi = new MediumInteraction(rWorld.at(t), rWorld.d.negate(), rWorld.time, this, phase);
                ms.spectrum = sigma_s;
                ms.spectrum.invScale(sigma_t);
                return ms;
            }
        }
        return ms;
    }
}