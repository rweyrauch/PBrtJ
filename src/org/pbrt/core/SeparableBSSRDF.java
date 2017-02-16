
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public abstract class SeparableBSSRDF extends BSSRDF {

    // SeparableBSSRDF Private Data
    private Normal3f ns;
    private Vector3f ss, ts;
    private Material material;
    private Material.TransportMode mode;

    public SeparableBSSRDF(SurfaceInteraction po, float eta, Material material, Material.TransportMode mode) {
        super(po, eta);
        this.ns = po.shading.n;
        this.ss = Vector3f.Normalize(po.shading.dpdu);
        this.ts = Vector3f.Cross(this.ns, this.ss);
        this.material = material;
        this.mode = mode;
    }

    @Override
    public Spectrum S(SurfaceInteraction pi, Vector3f wi) {
        return null;
    }

    @Override
    public BSSRDFSample Sample_S(Scene scene, float u1, Point2f u2) {
        return null;
    }

    public Spectrum Sw(Vector3f w) {
        return null;
    }
    public Spectrum Sp(SurfaceInteraction pi) {
        return Sr(Point3f.Distance(po.p, pi.p));
    }

    public BSSRDFSample Sample_Sp(Scene scene, float u1, Point2f u2) {
        return null;
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
        float chProb = 1 / (float)Spectrum.nSamples;
        for (int axis = 0; axis < 3; ++axis)
            for (int ch = 0; ch < Spectrum.nSamples; ++ch)
                pdf += Pdf_Sr(ch, rProj[axis]) * Math.abs(nLocal.at(axis)) * chProb *
                axisProb[axis];
        return pdf;
    }

    // SeparableBSSRDF Interface
    public abstract Spectrum Sr(float d);
    public abstract float Sample_Sr(int ch, float u);
    public abstract float Pdf_Sr(int ch, float r);
}