
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

public abstract class SeparableBSSRDF extends BSSRDF {

    public SeparableBSSRDF(SurfaceInteraction po, float eta, Material material, Material.TransportMode mode) {
        super(po, eta);
        this.ns = new Normal3f(po.shading.n);
        this.ss = Vector3f.Normalize(po.shading.dpdu);
        this.ts = Vector3f.Cross(this.ns, this.ss);
        this.material = material;
        this.mode = mode;
    }

    @Override
    public Spectrum S(SurfaceInteraction pi, Vector3f wi) {
        Stats.ProfilePhase pp = new Stats.ProfilePhase(Stats.Prof.BSSRDFEvaluation);
        float Ft = Reflection.FrDielectric(Reflection.CosTheta(po.wo), 1, eta);
        return (Sp(pi).multiply(Sw(wi))).scale(1 - Ft);
    }

    @Override
    public BSSRDFSample Sample_S(Scene scene, float u1, Point2f u2) {
        Stats.ProfilePhase pp = new Stats.ProfilePhase(Stats.Prof.BSSRDFSampling);
        BSSRDFSample bs = Sample_Sp(scene, u1, u2);
        if (!bs.s.isBlack()) {
            // Initialize material model at sampled surface interaction
            bs.si.bsdf = new BSDF(bs.si, 1);
            bs.si.bsdf.Add(new SeparableBSSRDFAdapter(this));
            bs.si.wo = new Vector3f(bs.si.shading.n);
        }
        return bs;
    }

    public Spectrum Sw(Vector3f w) {
        float c = 1 - 2 * FresnelMoment1(1 / eta);
        return new Spectrum((1 - Reflection.FrDielectric(Reflection.CosTheta(w), 1, eta)) / (c * (float)Math.PI));
    }

    public Spectrum Sp(SurfaceInteraction pi) {
        return Sr(Point3f.Distance(po.p, pi.p));
    }

    public BSSRDFSample Sample_Sp(Scene scene, float u1, Point2f u2) {
        Stats.ProfilePhase pp = new Stats.ProfilePhase(Stats.Prof.BSSRDFEvaluation);

        BSSRDFSample bs = new BSSRDFSample();
        bs.s = new Spectrum(0);
        bs.pdf = 0;
        bs.si = null;

        // Choose projection axis for BSSRDF sampling
        Vector3f vx, vy, vz;
        if (u1 < .5f) {
            vx = ss;
            vy = ts;
            vz = new Vector3f(ns);
            u1 *= 2;
        } else if (u1 < .75f) {
            // Prepare for sampling rays with respect to _ss_
            vx = ts;
            vy = new Vector3f(ns);
            vz = ss;
            u1 = (u1 - .5f) * 4;
        } else {
            // Prepare for sampling rays with respect to _ts_
            vx = new Vector3f(ns);
            vy = ss;
            vz = ts;
            u1 = (u1 - .75f) * 4;
        }

        // Choose spectral channel for BSSRDF sampling
        int ch = Pbrt.Clamp((int)(u1 * Spectrum.nSamples), 0, Spectrum.nSamples - 1);
        u1 = u1 * Spectrum.nSamples - ch;

        // Sample BSSRDF profile in polar coordinates
        float r = Sample_Sr(ch, u2.x);
        if (r < 0) return bs;
        float phi = 2 * (float)Math.PI * u2.y;

        // Compute BSSRDF profile bounds and intersection height
        float rMax = Sample_Sr(ch, 0.999f);
        if (r >= rMax) return bs;
        float l = 2 * (float)Math.sqrt(rMax * rMax - r * r);

        // Compute BSSRDF sampling ray segment
        Interaction base = new Interaction();
        base.p = po.p.add(((vx.scale((float)Math.cos(phi)).add(vy.scale((float)Math.sin(phi)))).scale(r)).subtract(vz.scale(1 * 0.5f)));
        base.time = po.time;
        Point3f pTarget = base.p.add(vz.scale(l));

        // Intersect BSSRDF sampling ray against the scene geometry

        // Declare _IntersectionChain_ and linked list
        ArrayList<SurfaceInteraction> chain = new ArrayList<>();

        // Accumulate chain of intersections along ray
        int nFound = 0;
        SurfaceInteraction isect;
        while ((isect = scene.Intersect(base.SpawnRayTo(pTarget))) != null) {
            base = isect;
            // Append admissible intersection to _IntersectionChain_
            if (isect.primitive.GetMaterial() == this.material) {
                chain.add(isect);
                nFound++;
            }
        }

        // Randomly choose one of several intersections during BSSRDF sampling
        if (nFound == 0) return bs;
        int selected = Pbrt.Clamp((int)(u1 * nFound), 0, nFound - 1);
        bs.si = chain.get(selected);

        // Compute sample PDF and return the spatial BSSRDF term $\Sp$
        bs.pdf = this.Pdf_Sp(bs.si) / nFound;
        bs.s = this.Sp(bs.si);
        return bs;
    }

    public float Pdf_Sp(SurfaceInteraction pi) {
        // Express $\pti-\pto$ and $\bold{n}_i$ with respect to local coordinates at
        // $\pto$
        Vector3f d = po.p.subtract(pi.p);
        Vector3f dLocal = new Vector3f(Vector3f.Dot(ss, d), Vector3f.Dot(ts, d), Vector3f.Dot(new Vector3f(ns), d));
        Normal3f nLocal = new Normal3f(Vector3f.Dot(ss, new Vector3f(pi.shading.n)), Vector3f.Dot(ts, new Vector3f(pi.shading.n)), Normal3f.Dot(ns, pi.shading.n));

        // Compute BSSRDF profile radius under projection along each axis
        float rProj[] = {(float)Math.sqrt(dLocal.y * dLocal.y + dLocal.z * dLocal.z),
                (float)Math.sqrt(dLocal.z * dLocal.z + dLocal.x * dLocal.x),
                (float)Math.sqrt(dLocal.x * dLocal.x + dLocal.y * dLocal.y)};

        // Return combined probability from all BSSRDF sampling strategies
        float pdf = 0, axisProb[] = {.25f, .25f, .5f};
        float chProb = 1 / (float)Spectrum.numSamples();
        for (int axis = 0; axis < 3; ++axis)
            for (int ch = 0; ch < Spectrum.numSamples(); ++ch)
                pdf += Pdf_Sr(ch, rProj[axis]) * Math.abs(nLocal.at(axis)) * chProb *
                axisProb[axis];
        return pdf;
    }

    // SeparableBSSRDF Interface
    public abstract Spectrum Sr(float d);
    public abstract float Sample_Sr(int ch, float u);
    public abstract float Pdf_Sr(int ch, float r);

    // SeparableBSSRDF Private Data
    private final Normal3f ns;
    private final Vector3f ss, ts;
    private final Material material;
    public final Material.TransportMode mode;
}