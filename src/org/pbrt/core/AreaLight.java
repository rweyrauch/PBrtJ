
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public abstract class AreaLight extends Light {
    // AreaLight Interface
    public AreaLight(Transform LightToWorld, MediumInterface medium, int nSamples) {
        super(Light.FlagArea, LightToWorld, medium, nSamples);
        numAreaLights.increment();
    }
    public abstract Spectrum L(Interaction intr, Vector3f w);

    protected static Stats.STAT_COUNTER numAreaLights = new Stats.STAT_COUNTER("Scene/AreaLights");

}