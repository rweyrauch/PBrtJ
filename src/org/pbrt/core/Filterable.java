/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public abstract class Filterable {

    public abstract Filterable Scale(float scaler);
    public abstract Filterable Add(Filterable v1);
    public abstract Filterable Black();
    public abstract Filterable Zero();
}