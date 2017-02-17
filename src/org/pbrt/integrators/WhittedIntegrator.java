
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.integrators;

import org.pbrt.core.*;

public class WhittedIntegrator extends SamplerIntegrator {

    public WhittedIntegrator(Camera camera, Sampler sampler, Bounds2i pixelBounds) {
        super(camera, sampler, pixelBounds);
    }

    @Override
    public Spectrum Li(RayDifferential ray, Scene scene, Sampler sampler, int depth) {
        return null;
    }

    public static WhittedIntegrator Create(ParamSet params, Sampler sampler, Camera camera){
        return null;
    }

}