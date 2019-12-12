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

public class EndpointInteraction extends Interaction {

    public Camera camera;
    public Light light;

    public EndpointInteraction() {
        super();
        this.light = null;
        this.camera = null;
    }
    public EndpointInteraction(Interaction it, Camera camera) {
        super(it);
        this.camera = camera;
        this.light = null;
    }
    public EndpointInteraction(Camera camera, Ray ray) {
        super(ray.o, ray.time, new MediumInterface(ray.medium));
        this.camera = camera;
        this.light = null;
    }
    public EndpointInteraction(Light light, Ray ray, Normal3f nl) {
        super(ray.o, ray.time, new MediumInterface(ray.medium));
        this.light = light;
        this.camera = null;
        this.n = new Normal3f(nl);
    }
    public EndpointInteraction(Interaction it, Light light) {
        super(it);
        this.light = light;
        this.camera = null;
    }
    public EndpointInteraction(Ray ray) {
        super(ray.at(1), ray.time, new MediumInterface(ray.medium));
        this.n = new Normal3f(ray.d.negate());
    }
}