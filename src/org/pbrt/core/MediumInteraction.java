/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class MediumInteraction extends Interaction {

    // MediumInteraction Public Data
    public PhaseFunction phase;

    // MediumInteraction Public Methods
    public MediumInteraction() { this.phase = null; }

    public MediumInteraction(Point3f p, Vector3f wo, float time,
                      Medium medium, PhaseFunction phase) {
        super(p, wo, time, new MediumInterface(medium));
        this.phase = phase;
    }
    boolean IsValid() { return phase != null; }

}