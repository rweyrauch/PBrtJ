
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class MediumInterface implements Cloneable {

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

    @Override
    public MediumInterface clone() {
        return new MediumInterface(this.inside, this.outside);
    }

    public boolean IsMediumTransition() { return inside != outside; }
}