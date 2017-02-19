
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
        return null;
    }

    @Override
    public MediumSample Sample(Ray ray, Sampler sampler) {
        return null;
    }
}