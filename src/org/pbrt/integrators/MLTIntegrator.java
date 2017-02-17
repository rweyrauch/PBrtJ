
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.integrators;

import org.pbrt.core.Camera;
import org.pbrt.core.Integrator;
import org.pbrt.core.ParamSet;
import org.pbrt.core.Scene;

public class MLTIntegrator extends Integrator {

    @Override
    public void Render(Scene scene) {

    }

    public static MLTIntegrator Create(ParamSet params, Camera camera) {
        return null;
    }

}