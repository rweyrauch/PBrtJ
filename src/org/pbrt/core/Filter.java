
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public abstract class Filter {
    // Filter Public Data
    public Vector2f radius, invRadius;

    public Filter() {
        this.radius = new Vector2f(0,0);
        this.invRadius = new Vector2f(0,0);
    }
    public Filter(Vector2f radius) {
        this.radius = new Vector2f(radius);
        this.invRadius = new Vector2f(1 / radius.x, 1 / radius.y); }

    public abstract float Evaluate(Point2f p);
}