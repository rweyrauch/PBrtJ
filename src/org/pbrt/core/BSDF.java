
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class BSDF {

    // BSDF Private Data
    private final Normal3f ns, ng;
    private final Vector3f ss, ts;
    private int nBxDFs = 0;
    private static final int MaxBxDFs = 8;
    public BxDF[] bxdfs = new BxDF[MaxBxDFs];

    // BSDF Public Methods
    public BSDF(SurfaceInteraction si, float eta) {
        this.eta = eta;
        this.ns = new Normal3f(si.shading.n);
        this.ng = new Normal3f(si.n);
        this.ss = Vector3f.Normalize(si.shading.dpdu);
        this.ts = Vector3f.Cross(ns, ss);
    }

    public void Add(BxDF b) {
        assert(nBxDFs < MaxBxDFs);
        bxdfs[nBxDFs++] = b;
    }

    public int NumComponents(int flags) {
        int num = 0;
        for (int i = 0; i < nBxDFs; ++i)
            if (bxdfs[i].MatchesFlags(flags)) ++num;
        return num;
    }
    public int NumComponents() {
        return NumComponents(BxDF.BSDF_ALL);
    }
    public Vector3f WorldToLocal(Vector3f v) {
        return new Vector3f(Vector3f.Dot(v, ss), Vector3f.Dot(v, ts), Normal3f.Dot(v, ns));
    }
    public Vector3f LocalToWorld(Vector3f v) {
        return new Vector3f(ss.x * v.x + ts.x * v.y + ns.x * v.z,
                ss.y * v.x + ts.y * v.y + ns.y * v.z,
                ss.z * v.x + ts.z * v.y + ns.z * v.z);
    }
    public Spectrum f(Vector3f woW, Vector3f wiW, int flags) {
        Stats.ProfilePhase pp = new Stats.ProfilePhase(Stats.Prof.BSDFEvaluation);
        Vector3f wi = WorldToLocal(wiW), wo = WorldToLocal(woW);
        if (wo.z == 0) return new Spectrum(0);
        boolean reflect = Normal3f.Dot(wiW, ng) * Normal3f.Dot(woW, ng) > 0;
        Spectrum f = new Spectrum(0);
        for (int i = 0; i < nBxDFs; ++i)
            if (bxdfs[i].MatchesFlags(flags) && ((reflect && ((bxdfs[i].type & BxDF.BSDF_REFLECTION) != 0)) ||
                (!reflect && ((bxdfs[i].type & BxDF.BSDF_TRANSMISSION) != 0))))
                f = Spectrum.Add(f, bxdfs[i].f(wo, wi));
        return f;
    }
    public Spectrum f(Vector3f woW, Vector3f wiW) {
        return f(woW, wiW, BxDF.BSDF_ALL);
    }
    public Spectrum rho(int nSamples, Point2f[] samples1, Point2f[] samples2, int flags) {
        Spectrum ret = new Spectrum(0);
        for (int i = 0; i < nBxDFs; ++i)
            if (bxdfs[i].MatchesFlags(flags))
                ret = Spectrum.Add(ret, bxdfs[i].rho(nSamples, samples1, samples2));
        return ret;
    }
    public Spectrum rho(int nSamples, Point2f[] samples1, Point2f[] samples2) {
        return rho(nSamples, samples1, samples2, BxDF.BSDF_ALL);
    }

    public Spectrum rho(Vector3f wo, int nSamples, Point2f[] samples, int flags) {
        Spectrum ret = new Spectrum(0);
        for (int i = 0; i < nBxDFs; ++i)
            if (bxdfs[i].MatchesFlags(flags))
                ret = Spectrum.Add(ret, bxdfs[i].rho(wo, nSamples, samples));
        return ret;
    }
    public Spectrum rho(Vector3f wo, int nSamples, Point2f[] samples) {
        return rho(wo, nSamples, samples, BxDF.BSDF_ALL);
    }

    public BxDF.BxDFSample Sample_f(Vector3f woWorld, Point2f u, int type) {
        Stats.ProfilePhase pp = new Stats.ProfilePhase(Stats.Prof.BSDFSampling);
        // Choose which _BxDF_ to sample
        BxDF.BxDFSample sample = new BxDF.BxDFSample();
        sample.pdf = 0;
        sample.wiWorld = null;
        sample.sampledType = BxDF.BSDF_NONE;
        sample.f = new Spectrum(0);
        int matchingComps = NumComponents(type);
        if (matchingComps == 0) {
            return sample;
        }
        int comp = Math.min((int)Math.floor(u.at(0) * matchingComps), matchingComps - 1);

        // Get _BxDF_ pointer for chosen component
        BxDF bxdf = null;
        int count = comp;
        for (int i = 0; i < nBxDFs; ++i)
            if (bxdfs[i].MatchesFlags(type) && count-- == 0) {
            bxdf = bxdfs[i];
            break;
        }
        assert (bxdf != null);
        Api.logger.trace("BSDF::Sample_f chose comp = %d / matching = %d, bxdf: %s", comp, matchingComps, bxdf.toString());

        // Remap _BxDF_ sample _u_ to $[0,1)^2$
        Point2f uRemapped = new Point2f(Math.min(u.at(0) * matchingComps - comp, Pbrt.OneMinusEpsilon), u.at(1));

        // Sample chosen _BxDF_
        Vector3f wi, wo = WorldToLocal(woWorld);
        if (wo.z == 0) return sample;

        sample.pdf = 0;
        sample.sampledType = bxdf.type;
        BxDF.BxDFSample bsample = bxdf.Sample_f(wo, uRemapped);
        sample.f = bsample.f;
        wi = bsample.wiWorld;
        sample.pdf = bsample.pdf;
        Api.logger.trace("For wo = %s, sampled f = %s, pdf = %f, ratio = %f, wi = %s", wo.toString(), sample.f,
                sample.pdf, (sample.pdf > 0) ? sample.f.scale(1/sample.pdf).toString() : new Spectrum(0).toString(), wi);
        if (sample.pdf == 0) {
            sample.sampledType = BxDF.BSDF_NONE;
            return sample;
        }

        sample.wiWorld = LocalToWorld(wi);

        // Compute overall PDF with all matching _BxDF_s
        if ((bxdf.type & BxDF.BSDF_SPECULAR) == 0 && matchingComps > 1)
            for (int i = 0; i < nBxDFs; ++i)
                if (bxdfs[i] != bxdf && bxdfs[i].MatchesFlags(type))
                    sample.pdf += bxdfs[i].Pdf(wo, wi);
        if (matchingComps > 1) sample.pdf /= matchingComps;

        // Compute value of BSDF for sampled direction
        if ((bxdf.type & BxDF.BSDF_SPECULAR) == 0 && matchingComps > 1) {
            boolean reflect = Normal3f.Dot(sample.wiWorld, ng) * Normal3f.Dot(woWorld, ng) > 0;
            for (int i = 0; i < nBxDFs; ++i) {
                if (bxdfs[i].MatchesFlags(type) &&
                        ((reflect && ((bxdfs[i].type & BxDF.BSDF_REFLECTION) != 0)) ||
                                (!reflect && ((bxdfs[i].type & BxDF.BSDF_TRANSMISSION) != 0)))) {
                    Spectrum bxf = bxdfs[i].f(wo, wi);
                    assert bxf != null;
                    sample.f = Spectrum.Add(sample.f, bxf);
                }
            }
        }
        Api.logger.trace("Overall f = %s, pdf = %f, ratio = %f", sample.f, sample.pdf, (sample.pdf > 0) ? sample.f.scale(1/ sample.pdf) : new Spectrum(0));
        return sample;
    }
    public BxDF.BxDFSample Sample_f(Vector3f woWorld, Point2f u) {
        return Sample_f(woWorld, u, BxDF.BSDF_ALL);
    }

    public float Pdf(Vector3f woWorld, Vector3f wiWorld, int flags) {
        Stats.ProfilePhase pp = new Stats.ProfilePhase(Stats.Prof.BSDFPdf);
        if (nBxDFs == 0.f) return 0.f;
        Vector3f wo = WorldToLocal(woWorld), wi = WorldToLocal(wiWorld);
        if (wo.z == 0) return 0;
        float pdf = 0;
        int matchingComps = 0;
        for (int i = 0; i < nBxDFs; ++i)
            if (bxdfs[i].MatchesFlags(flags)) {
            ++matchingComps;
            pdf += bxdfs[i].Pdf(wo, wi);
        }
        return matchingComps > 0 ? pdf / matchingComps : 0;
    }
    public float Pdf(Vector3f wo, Vector3f wi) {
        return Pdf(wo, wi, BxDF.BSDF_ALL);
    }

    @Override
    public String toString() {
        String s = String.format("[ BSDF eta: %f nBxDFs: %d", eta, nBxDFs);
        for (int i = 0; i < nBxDFs; ++i)
            s += String.format("\n  bxdfs[%d]: ", i) + bxdfs[i].toString();
        return s + " ]";
    }

    // BSDF Public Data
    public float eta;

}