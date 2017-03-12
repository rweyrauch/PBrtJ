
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

public class HomogeneousMedium extends Medium {

    // HomogeneousMedium Private Data
    private Spectrum sigma_a, sigma_s, sigma_t;
    private float g;

    public HomogeneousMedium(Spectrum sigma_a, Spectrum sigma_s, float g) {
        this.sigma_a = sigma_a;
        this.sigma_s = sigma_s;
        this.sigma_t = Spectrum.Add(sigma_s, sigma_a);
        this.g = g;
    }

    @Override
    public Spectrum Tr(Ray ray, Sampler sampler) {
        Stats.ProfilePhase pp = new Stats.ProfilePhase(Stats.Prof.MediumTr);
        return Spectrum.Exp(sigma_t.negate().scale(Math.min(ray.tMax * ray.d.Length(), Pbrt.MaxFloat)));
    }

    @Override
    public MediumSample Sample(Ray ray, Sampler sampler) {
        Stats.ProfilePhase pp = new Stats.ProfilePhase(Stats.Prof.MediumSample);
        MediumSample ms = new MediumSample();
        // Sample a channel and distance along the ray
        int channel = Math.min((int)(sampler.Get1D() * Spectrum.nSamples), Spectrum.nSamples - 1);
        float dist = -(float)Math.log(1 - sampler.Get1D()) / sigma_t.at(channel);
        float t = Math.min(dist * ray.d.Length(), ray.tMax);
        boolean sampledMedium = t < ray.tMax;
        if (sampledMedium)
            ms.mi = new MediumInteraction(ray.at(t), ray.d.negate(), ray.time, this, new HenyeyGreenstein(g));

        // Compute the transmittance and sampling density
        Spectrum Tr = Spectrum.Exp(sigma_t.negate().scale(Math.min(t, Pbrt.MaxFloat) * ray.d.Length()));

        // Return weighting factor for scattering from homogeneous medium
        Spectrum density = sampledMedium ? (sigma_t.multiply(Tr)) : Tr;
        float pdf = 0;
        for (int i = 0; i < Spectrum.nSamples; ++i) pdf += density.at(i);
        pdf *= 1 / (float)Spectrum.nSamples;
        if (pdf == 0) {
            assert (Tr.isBlack());
            pdf = 1;
        }
        if (sampledMedium)
            ms.spectrum = Tr.multiply(sigma_s.scale(1 / pdf));
        else
            ms.spectrum = Tr.scale(1 / pdf);
        return ms;
    }
}