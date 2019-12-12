
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class MediumInterface {

    public Medium inside = null, outside = null;

    // MediumInterface Public Methods
    public MediumInterface() {}
    public MediumInterface(Medium medium) {
        inside = medium;
        outside = medium;
    }

    public MediumInterface(Medium inside, Medium outside) {
        this.inside = inside;
        this.outside = outside;
    }

    public MediumInterface(MediumInterface mi) {
        this(mi.inside, mi.outside);
    }

    public boolean IsMediumTransition() { return inside != outside; }
}