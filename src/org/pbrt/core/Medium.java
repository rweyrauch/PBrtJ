
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public abstract class Medium {
    public class MediumSample {
        Spectrum spectrum;
        MediumInteraction mi;
    }
    public abstract Spectrum Tr(Ray ray, Sampler sampler);
    public abstract MediumSample Sample(Ray ray, Sampler sampler);

}