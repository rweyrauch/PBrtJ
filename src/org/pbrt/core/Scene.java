
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

import java.util.ArrayList;

public class Scene {
    // Scene Private Data
    private Primitive aggregate;
    private Bounds3f worldBound;

    // Scene Public Data
    public ArrayList<Light> lights = new ArrayList<>();
    // Store infinite light sources separately for cases where we only want
    // to loop over them.
    public ArrayList<Light> infiniteLights = new ArrayList<>();

    // Scene Public Methods
    public Scene(Primitive aggregate,
          ArrayList<Light> lights) {
        this.aggregate = aggregate;
        this.lights = (ArrayList<Light>)(lights.clone());

        // Scene Constructor Implementation
        worldBound = aggregate.WorldBound();
        for (Light light : lights) {
            light.Preprocess(this);
            if ((light.flags & Light.FlagInfinite) != 0)
                infiniteLights.add(light);
        }
    }
    public Bounds3f WorldBound() { return worldBound; }
    public SurfaceInteraction Intersect(Ray ray) {
        return aggregate.Intersect(ray);
    }
    public boolean IntersectP(Ray ray) {
        return aggregate.IntersectP(ray);
    }

    public class TrIntersection {
        SurfaceInteraction isect;
        Spectrum Tr;
    }
    public TrIntersection IntersectTr(Ray ray, Sampler sampler) {
        TrIntersection tsect = new TrIntersection();
        tsect.Tr = new Spectrum(1.f);
        while (true) {
            tsect.isect = Intersect(ray);

            // Accumulate beam transmittance for ray segment
            if (ray.medium != null) tsect.Tr.multiply(ray.medium.Tr(ray, sampler));

            // Initialize next ray segment or terminate transmittance computation
            if (tsect.isect == null) return null;
            if (tsect.isect.primitive.GetMaterial() != null) return tsect;
            ray = tsect.isect.SpawnRay(ray.d);
        }
    }

}