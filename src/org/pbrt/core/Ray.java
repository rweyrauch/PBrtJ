/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class Ray {

    // Ray Public Data
    Point3f o;
    Vector3f d;
    float tMax = Float.MAX_VALUE;
    float time = 0.0f;
    Medium medium = null;

    // Ray Public Methods
    public Ray() {
    }

    public Ray(Point3f o, Vector3f d) {
        this.o = o;
        this.d = d;
    }

    public Ray(Point3f o, Vector3f d, Float tMax,
               float time, Medium medium) {
        this.o = o;
        this.d = d;
        this.tMax = tMax;
        this.time = time;
        this.medium = medium;
    }

    public Point3f at(float t) {
        return o.add(d.scale(t));
    }

    public boolean HasNaNs() {
        return (o.HasNaNs() || d.HasNaNs() || Float.isNaN(tMax));
    }

}