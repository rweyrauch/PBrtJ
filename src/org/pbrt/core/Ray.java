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
    public Point3f o;
    public Vector3f d;
    public float tMax = Pbrt.Infinity;
    public float time = 0.0f;
    public Medium medium;

    // Ray Public Methods
    public Ray() {
        this.o = new Point3f();
        this.d = new Vector3f();
        this.medium = null;
    }

    public Ray(Point3f o, Vector3f d) {
        this.o = new Point3f(o);
        this.d = new Vector3f(d);
        this.medium = null;
    }

    public Ray(Point3f o, Vector3f d, float tMax,
               float time, Medium medium) {
        this.o = new Point3f(o);
        this.d = new Vector3f(d);
        this.tMax = tMax;
        this.time = time;
        this.medium = medium;
    }

    public Ray(Ray r) {
        this.o = new Point3f(r.o);
        this.d = new Vector3f(r.d);
        this.tMax = r.tMax;
        this.time = r.time;
        this.medium = r.medium;
    }

    public Point3f at(float t) {
        return o.add(d.scale(t));
    }

    public boolean HasNaNs() {
        return (o.HasNaNs() || d.HasNaNs() || Float.isNaN(tMax));
    }

    @Override
    public String toString() {
        return "[o=" + this.o.toString() + ", d=" + this.d.toString() + ", tMax=" + this.tMax + ", time=" + this.time + "]";
    }
}