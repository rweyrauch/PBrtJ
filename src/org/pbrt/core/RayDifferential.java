/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class RayDifferential extends Ray {

    // RayDifferential Public Data
    public boolean hasDifferentials = false;
    public Point3f rxOrigin, ryOrigin;
    public Vector3f rxDirection, ryDirection;

    // RayDifferential Public Methods
    public RayDifferential() {
        super();
    }

    public RayDifferential(Point3f o, Vector3f d) {
        super(o, d);
        this.hasDifferentials = false;
    }

    public RayDifferential(Point3f o, Vector3f d, Float tMax,
               float time, Medium medium) {
        super(o, d, tMax, time, medium);
        this.hasDifferentials = false;
    }

    public RayDifferential(Ray ray) {
        super(ray.o, ray.d, ray.tMax, ray.time, ray.medium);
        this.hasDifferentials = false;
    }

    public boolean HasNaNs() {
        return super.HasNaNs() || (hasDifferentials &&
                (rxOrigin.HasNaNs() || ryOrigin.HasNaNs() || rxDirection.HasNaNs() || ryDirection.HasNaNs()));
    }

    public void ScaleDifferentials(float s) {
        rxOrigin = o.add((rxOrigin.subtract(o)).scale(s));
        ryOrigin = o.add((ryOrigin.subtract(o)).scale(s));
        rxDirection = d.add((rxDirection.subtract(d)).scale(s));
        ryDirection = d.add((ryDirection.subtract(d)).scale(s));
    }

}