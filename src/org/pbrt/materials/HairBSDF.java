/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.materials;

import org.pbrt.core.*;

import static org.pbrt.core.Reflection.FrDielectric;

public class HairBSDF extends BxDF {

    public HairBSDF(float h, float eta, Spectrum sigma_a, float beta_m, float beta_n, float alpha) {
        super(BSDF_GLOSSY | BSDF_REFLECTION | BSDF_TRANSMISSION);
        this.h = h;
        this.gammaO = SafeASin(h);
        this.eta = eta;
        this.sigma_a = new Spectrum(sigma_a);
        this.beta_m = beta_m;
        this.beta_n = beta_n;

        assert (h >= -1 && h <= 1);
        assert (beta_m >= 0 && beta_m <= 1);
        assert (beta_n >= 0 && beta_n <= 1);
        // Compute longitudinal variance from $\beta_m$
        //static_assert(pMax >= 3, "Longitudinal variance code must be updated to handle low pMax");
        v[0] = Sqr(0.726f * beta_m + 0.812f * Sqr(beta_m) + 3.7f * Pow(20, beta_m));
        v[1] = .25f * v[0];
        v[2] = 4 * v[0];
        for (int p = 3; p <= pMax; ++p)
            // TODO: is there anything better here?
            v[p] = v[2];

        // Compute azimuthal logistic scale factor from $\beta_n$
        s = SqrtPiOver8 * (0.265f * beta_n + 1.194f * Sqr(beta_n) + 5.372f * Pow(22,beta_n));
        assert (!Float.isNaN(s));

        // Compute $\alpha$ terms for hair scales
        sin2kAlpha[0] = (float)Math.sin(Math.toRadians(alpha));
        cos2kAlpha[0] = SafeSqrt(1 - Sqr(sin2kAlpha[0]));
        for (int i = 1; i < 3; ++i) {
            sin2kAlpha[i] = 2 * cos2kAlpha[i - 1] * sin2kAlpha[i - 1];
            cos2kAlpha[i] = Sqr(cos2kAlpha[i - 1]) - Sqr(sin2kAlpha[i - 1]);
        }

    }

    @Override
    public Spectrum f(Vector3f wo, Vector3f wi) {
        // Compute hair coordinate system terms related to _wo_
        float sinThetaO = wo.x;
        float cosThetaO = SafeSqrt(1 - Sqr(sinThetaO));
        float phiO = (float)Math.atan2(wo.z, wo.y);

        // Compute hair coordinate system terms related to _wi_
        float sinThetaI = wi.x;
        float cosThetaI = SafeSqrt(1 - Sqr(sinThetaI));
        float phiI = (float)Math.atan2(wi.z, wi.y);

        // Compute $\cos \thetat$ for refracted ray
        float sinThetaT = sinThetaO / eta;
        float cosThetaT = SafeSqrt(1 - Sqr(sinThetaT));

        // Compute $\gammat$ for refracted ray
        float etap = (float)Math.sqrt(eta * eta - Sqr(sinThetaO)) / cosThetaO;
        float sinGammaT = h / etap;
        float cosGammaT = SafeSqrt(1 - Sqr(sinGammaT));
        float gammaT = SafeASin(sinGammaT);

        // Compute the transmittance _T_ of a single path through the cylinder
        Spectrum T = Spectrum.Exp(sigma_a.scale(-2 * cosGammaT / cosThetaT));

        // Evaluate hair BSDF
        float phi = phiI - phiO;
        Spectrum[] ap = Ap(cosThetaO, eta, h, T);
        Spectrum fsum = new Spectrum(0);
        for (int p = 0; p < pMax; ++p) {
            // Compute $\sin \thetai$ and $\cos \thetai$ terms accounting for scales
            float sinThetaIp, cosThetaIp;
            if (p == 0) {
                sinThetaIp = sinThetaI * cos2kAlpha[1] + cosThetaI * sin2kAlpha[1];
                cosThetaIp = cosThetaI * cos2kAlpha[1] - sinThetaI * sin2kAlpha[1];
            }

            // Handle remainder of $p$ values for hair scale tilt
            else if (p == 1) {
                sinThetaIp = sinThetaI * cos2kAlpha[0] - cosThetaI * sin2kAlpha[0];
                cosThetaIp = cosThetaI * cos2kAlpha[0] + sinThetaI * sin2kAlpha[0];
            } else if (p == 2) {
                sinThetaIp = sinThetaI * cos2kAlpha[2] - cosThetaI * sin2kAlpha[2];
                cosThetaIp = cosThetaI * cos2kAlpha[2] + sinThetaI * sin2kAlpha[2];
            } else {
                sinThetaIp = sinThetaI;
                cosThetaIp = cosThetaI;
            }

            // Handle out-of-range $\cos \thetai$ from scale adjustment
            cosThetaIp = Math.abs(cosThetaIp);
            fsum = fsum.add(ap[p].scale(Mp(cosThetaIp, cosThetaO, sinThetaIp, sinThetaO, v[p]) * Np(phi, p, s, gammaO, gammaT)));
        }

        // Compute contribution of remaining terms after _pMax_
        fsum = fsum.add(ap[pMax].scale(Mp(cosThetaI, cosThetaO, sinThetaI, sinThetaO, v[pMax]) / (2.f * Pbrt.Pi)));
        if (Reflection.AbsCosTheta(wi) > 0) fsum.invScale(Reflection.AbsCosTheta(wi));
        assert (!Float.isInfinite(fsum.y()) && !Float.isNaN(fsum.y()));
        return fsum;
    }

    @Override
    public BxDFSample Sample_f(Vector3f wo, Point2f u2) {
        // Compute hair coordinate system terms related to _wo_
        float sinThetaO = wo.x;
        float cosThetaO = SafeSqrt(1 - Sqr(sinThetaO));
        float phiO = (float)Math.atan2(wo.z, wo.y);

        // Derive four random samples from _u2_
        Point2f[] u = {DemuxFloat(u2.x), DemuxFloat(u2.y)};

        // Determine which term $p$ to sample for hair scattering
        float[] apPdf = ComputeApPdf(cosThetaO);
        int p;
        for (p = 0; p < pMax; ++p) {
            if (u[0].x < apPdf[p]) break;
            u[0].x -= apPdf[p];
        }

        // Sample $M_p$ to compute $\thetai$
        u[1].x = Math.max(u[1].x, 1e-5f);
        float cosTheta = 1 + v[p] * (float)Math.log(u[1].x + (1 - u[1].x) * (float)Math.exp(-2 / v[p]));
        float sinTheta = SafeSqrt(1 - Sqr(cosTheta));
        float cosPhi = (float)Math.cos(2 * Pbrt.Pi * u[1].y);
        float sinThetaI = -cosTheta * sinThetaO + sinTheta * cosPhi * cosThetaO;
        float cosThetaI = SafeSqrt(1 - Sqr(sinThetaI));

        // Update sampled $\sin \thetai$ and $\cos \thetai$ to account for scales
        float sinThetaIp = sinThetaI, cosThetaIp = cosThetaI;
        if (p == 0) {
            sinThetaIp = sinThetaI * cos2kAlpha[1] - cosThetaI * sin2kAlpha[1];
            cosThetaIp = cosThetaI * cos2kAlpha[1] + sinThetaI * sin2kAlpha[1];
        } else if (p == 1) {
            sinThetaIp = sinThetaI * cos2kAlpha[0] + cosThetaI * sin2kAlpha[0];
            cosThetaIp = cosThetaI * cos2kAlpha[0] - sinThetaI * sin2kAlpha[0];
        } else if (p == 2) {
            sinThetaIp = sinThetaI * cos2kAlpha[2] + cosThetaI * sin2kAlpha[2];
            cosThetaIp = cosThetaI * cos2kAlpha[2] - sinThetaI * sin2kAlpha[2];
        }
        sinThetaI = sinThetaIp;
        cosThetaI = cosThetaIp;

        // Sample $N_p$ to compute $\Delta\phi$

        // Compute $\gammat$ for refracted ray
        float etap = (float)Math.sqrt(eta * eta - Sqr(sinThetaO)) / cosThetaO;
        float sinGammaT = h / etap;
        float cosGammaT = SafeSqrt(1 - Sqr(sinGammaT));
        float gammaT = SafeASin(sinGammaT);
        float dphi;
        if (p < pMax)
            dphi = Phi(p, gammaO, gammaT) + SampleTrimmedLogistic(u[0].y, s, -Pbrt.Pi, Pbrt.Pi);
        else
            dphi = 2 * Pbrt.Pi * u[0].y;

        BxDFSample bs = new BxDFSample();
        // Compute _wi_ from sampled hair scattering angles
        float phiI = phiO + dphi;
        bs.wiWorld = new Vector3f(sinThetaI, cosThetaI * (float)Math.cos(phiI), cosThetaI * (float)Math.sin(phiI));

        // Compute PDF for sampled hair scattering direction _wi_
        bs.pdf = 0;
        for (p = 0; p < pMax; ++p) {
            // Compute $\sin \thetai$ and $\cos \thetai$ terms accounting for scales
            sinThetaIp = 0;
            cosThetaIp = 0;
            if (p == 0) {
                sinThetaIp = sinThetaI * cos2kAlpha[1] + cosThetaI * sin2kAlpha[1];
                cosThetaIp = cosThetaI * cos2kAlpha[1] - sinThetaI * sin2kAlpha[1];
            }

            // Handle remainder of $p$ values for hair scale tilt
            else if (p == 1) {
                sinThetaIp = sinThetaI * cos2kAlpha[0] - cosThetaI * sin2kAlpha[0];
                cosThetaIp = cosThetaI * cos2kAlpha[0] + sinThetaI * sin2kAlpha[0];
            } else if (p == 2) {
                sinThetaIp = sinThetaI * cos2kAlpha[2] - cosThetaI * sin2kAlpha[2];
                cosThetaIp = cosThetaI * cos2kAlpha[2] + sinThetaI * sin2kAlpha[2];
            } else {
                sinThetaIp = sinThetaI;
                cosThetaIp = cosThetaI;
            }

            // Handle out-of-range $\cos \thetai$ from scale adjustment
            cosThetaIp = Math.abs(cosThetaIp);
            bs.pdf += Mp(cosThetaIp, cosThetaO, sinThetaIp, sinThetaO, v[p]) * apPdf[p] * Np(dphi, p, s, gammaO, gammaT);
        }
        bs.pdf += Mp(cosThetaI, cosThetaO, sinThetaI, sinThetaO, v[pMax]) * apPdf[pMax] * (1 / (2 * Pbrt.Pi));
        // if (std::abs(wi->x) < .9999) CHECK_NEAR(*pdf, Pdf(wo, *wi), .01);
        bs.f = f(wo, bs.wiWorld);
        return bs;
    }

    @Override
    public float Pdf(Vector3f wo, Vector3f wi) {
        // Compute hair coordinate system terms related to _wo_
        float sinThetaO = wo.x;
        float cosThetaO = SafeSqrt(1 - Sqr(sinThetaO));
        float phiO = (float)Math.atan2(wo.z, wo.y);

        // Compute hair coordinate system terms related to _wi_
        float sinThetaI = wi.x;
        float cosThetaI = SafeSqrt(1 - Sqr(sinThetaI));
        float phiI = (float)Math.atan2(wi.z, wi.y);

        // Compute $\cos \thetat$ for refracted ray
        float sinThetaT = sinThetaO / eta;
        float cosThetaT = SafeSqrt(1 - Sqr(sinThetaT));

        // Compute $\gammat$ for refracted ray
        float etap = (float)Math.sqrt(eta * eta - Sqr(sinThetaO)) / cosThetaO;
        float sinGammaT = h / etap;
        float cosGammaT = SafeSqrt(1 - Sqr(sinGammaT));
        float gammaT = SafeASin(sinGammaT);

        // Compute PDF for $A_p$ terms
        float[] apPdf = ComputeApPdf(cosThetaO);

        // Compute PDF sum for hair scattering events
        float phi = phiI - phiO;
        float pdf = 0;
        for (int p = 0; p < pMax; ++p) {
            // Compute $\sin \thetai$ and $\cos \thetai$ terms accounting for scales
            float sinThetaIp, cosThetaIp;
            if (p == 0) {
                sinThetaIp = sinThetaI * cos2kAlpha[1] + cosThetaI * sin2kAlpha[1];
                cosThetaIp = cosThetaI * cos2kAlpha[1] - sinThetaI * sin2kAlpha[1];
            }

            // Handle remainder of $p$ values for hair scale tilt
            else if (p == 1) {
                sinThetaIp = sinThetaI * cos2kAlpha[0] - cosThetaI * sin2kAlpha[0];
                cosThetaIp = cosThetaI * cos2kAlpha[0] + sinThetaI * sin2kAlpha[0];
            } else if (p == 2) {
                sinThetaIp = sinThetaI * cos2kAlpha[2] - cosThetaI * sin2kAlpha[2];
                cosThetaIp = cosThetaI * cos2kAlpha[2] + sinThetaI * sin2kAlpha[2];
            } else {
                sinThetaIp = sinThetaI;
                cosThetaIp = cosThetaI;
            }

            // Handle out-of-range $\cos \thetai$ from scale adjustment
            cosThetaIp = Math.abs(cosThetaIp);
            pdf += Mp(cosThetaIp, cosThetaO, sinThetaIp, sinThetaO, v[p]) * apPdf[p] * Np(phi, p, s, gammaO, gammaT);
        }
        pdf += Mp(cosThetaI, cosThetaO, sinThetaI, sinThetaO, v[pMax]) * apPdf[pMax] * (1 / (2 * Pbrt.Pi));
        return pdf;
    }

    @Override
    public String toString() {
        return String.format("[ Hair h: %f gammaO: %f eta: %f beta_m: %f beta_n: %f v[0]: %f s: %f sigma_a: ", h, gammaO, eta, beta_m, beta_n,
                v[0], s) + sigma_a.toString() + "  ]";
    }

    public static Spectrum SigmaAFromConcentration(float ce, float cp) {
        float[] sigma_a = {0, 0, 0};
        float[] eumelaninSigmaA = {0.419f, 0.697f, 1.37f};
        float[] pheomelaninSigmaA = {0.187f, 0.4f, 1.05f};
        for (int i = 0; i < 3; ++i)
            sigma_a[i] = (ce * eumelaninSigmaA[i] + cp * pheomelaninSigmaA[i]);
        return Spectrum.FromRGB(sigma_a);
    }

    public static Spectrum SigmaAFromReflectance(Spectrum c, float beta_n) {
        Spectrum sigma_a = new Spectrum(0);
        for (int i = 0; i < Spectrum.nSamples; ++i)
            sigma_a.set(i, Sqr((float)Math.log(c.at(i)) / (5.969f - 0.215f * beta_n + 2.532f * Sqr(beta_n) -
                    10.73f * Pow(3, beta_n) + 5.574f * Pow(4, beta_n) + 0.245f * Pow(5, beta_n))));
        return sigma_a;
    }

    private float[] ComputeApPdf(float cosThetaO) {
        // Compute array of $A_p$ values for _cosThetaO_
        float sinThetaO = SafeSqrt(1 - cosThetaO * cosThetaO);

        // Compute $\cos \thetat$ for refracted ray
        float sinThetaT = sinThetaO / eta;
        float cosThetaT = SafeSqrt(1 - Sqr(sinThetaT));

        // Compute $\gammat$ for refracted ray
        float etap = (float)Math.sqrt(eta * eta - Sqr(sinThetaO)) / cosThetaO;
        float sinGammaT = h / etap;
        float cosGammaT = SafeSqrt(1 - Sqr(sinGammaT));
        float gammaT = SafeASin(sinGammaT);

        // Compute the transmittance _T_ of a single path through the cylinder
        Spectrum T = Spectrum.Exp(sigma_a.scale(-2 * cosGammaT / cosThetaT));
        Spectrum[] ap = Ap(cosThetaO, eta, h, T);

        // Compute $A_p$ PDF from individual $A_p$ terms
        float[] apPdf = new float[pMax+1];
        float sumY = 0;
        for (int i = 0; i < ap.length; i++) {
            sumY += (s + ap[i].y());
        }
        for (int i = 0; i <= pMax; ++i) apPdf[i] = ap[i].y() / sumY;
        return apPdf;
    }

    private final float h, gammaO, eta;
    private final Spectrum sigma_a;
    private final float beta_m, beta_n;
    private float[] v = { 0, 0, 0, 0 };
    private float s;
    private float[] sin2kAlpha, cos2kAlpha;

    private static final int pMax = 3;
    private static final float SqrtPiOver8 = 0.626657069f;

    private static float Sqr(float v) { return v * v; }
    private static float Pow(int n, float v) {
        return (float)Math.pow(v, n);
    }

    private static float SafeASin(float x) {
        assert (x >= -1.0001f && x <= 1.0001f);
        return (float)Math.asin(Pbrt.Clamp(x, -1, 1));
    }

    private static float SafeSqrt(float x) {
        assert (x >= -1e-4f);
        return (float)Math.sqrt(Math.max(0, x));
    }

    private static float I0(float x) {
        float val = 0;
        float x2i = 1;
        int ifact = 1;
        int i4 = 1;
        // I0(x) \approx Sum_i x^(2i) / (4^i (i!)^2)
        for (int i = 0; i < 10; ++i) {
            if (i > 1) ifact *= i;
            val += x2i / (i4 * Sqr(ifact));
            x2i *= x * x;
            i4 *= 4;
        }
        return val;
    }

    private static float LogI0(float x) {
        if (x > 12)
            return x + 0.5f * (-(float)Math.log(2 * Pbrt.Pi) + (float)Math.log(1 / x) + 1 / (8 * x));
        else
            return (float)Math.log(I0(x));
    }

    private static float Mp(float cosThetaI, float cosThetaO, float sinThetaI, float sinThetaO, float v) {
        float a = cosThetaI * cosThetaO / v;
        float b = sinThetaI * sinThetaO / v;
        float mp = (v <= .1f) ? ((float)Math.exp(LogI0(a) - b - 1 / v + 0.6931f + (float)Math.log(1 / (2 * v))))
            : ((float)Math.exp(-b) * I0(a)) / ((float)Math.sinh(1 / v) * 2 * v);
        assert (!Float.isInfinite(mp) && !Float.isNaN(mp));
        return mp;
    }

    private static Spectrum[] Ap(float cosThetaO, float eta, float h, Spectrum T) {
        Spectrum[] ap = new Spectrum[pMax+1];
        // Compute $p=0$ attenuation at initial cylinder intersection
        float cosGammaO = SafeSqrt(1 - h * h);
        float cosTheta = cosThetaO * cosGammaO;
        float f = FrDielectric(cosTheta, 1.f, eta);
        ap[0] = new Spectrum(f);

        // Compute $p=1$ attenuation term
        ap[1] = T.scale(Sqr(1 - f));

        // Compute attenuation terms up to $p=_pMax_$
        for (int p = 2; p < pMax; ++p) ap[p] = ap[p - 1].multiply(T.scale(f));

        // Compute attenuation term accounting for remaining orders of scattering
        ap[pMax] = (ap[pMax - 1].multiply(T.scale(f))).divide(new Spectrum(1).subtract(T.scale(f)));
        return ap;
    }

    private static float Phi(int p, float gammaO, float gammaT) {
        return 2 * p * gammaT - 2 * gammaO + p * Pbrt.Pi;
    }

    private static float Logistic(float x, float s) {
        x = Math.abs(x);
        return (float)Math.exp(-x / s) / (s * Sqr(1 + (float)Math.exp(-x / s)));
    }

    private static float LogisticCDF(float x, float s) {
        return 1 / (1 + (float)Math.exp(-x / s));
    }

    private static float TrimmedLogistic(float x, float s, float a, float b) {
        assert (a < b);
        return Logistic(x, s) / (LogisticCDF(b, s) - LogisticCDF(a, s));
    }

    private static float Np(float phi, int p, float s, float gammaO, float gammaT) {
        float dphi = phi - Phi(p, gammaO, gammaT);
        // Remap _dphi_ to $[-\pi,\pi]$
        while (dphi > Pbrt.Pi) dphi -= 2 * Pbrt.Pi;
        while (dphi < -Pbrt.Pi) dphi += 2 * Pbrt.Pi;
        return TrimmedLogistic(dphi, s, -Pbrt.Pi, Pbrt.Pi);
    }

    private static float SampleTrimmedLogistic(float u, float s, float a, float b) {
        assert (a < b);
        float k = LogisticCDF(b, s) - LogisticCDF(a, s);
        float x = -s * (float)Math.log(1 / (u * k + LogisticCDF(a, s)) - 1);
        assert (!Float.isNaN(x));
        return Pbrt.Clamp(x, a, b);
    }

    // https://fgiesen.wordpress.com/2009/12/13/decoding-morton-codes/
    private static int Compact1By1(int x) {
        // TODO: as of Haswell, the PEXT instruction could do all this in a
        // single instruction.
        // x = -f-e -d-c -b-a -9-8 -7-6 -5-4 -3-2 -1-0
        x &= 0x55555555;
        // x = --fe --dc --ba --98 --76 --54 --32 --10
        x = (x ^ (x >> 1)) & 0x33333333;
        // x = ---- fedc ---- ba98 ---- 7654 ---- 3210
        x = (x ^ (x >> 2)) & 0x0f0f0f0f;
        // x = ---- ---- fedc ba98 ---- ---- 7654 3210
        x = (x ^ (x >> 4)) & 0x00ff00ff;
        // x = ---- ---- ---- ---- fedc ba98 7654 3210
        x = (x ^ (x >> 8)) & 0x0000ffff;
        return x;
    }

    private static Point2f DemuxFloat(float f) {
        assert(f >= 0 && f < 1);
        long v = (long)(f * (1L << 32));
        assert (v < 0x100000000L);
        int[] bits = {Compact1By1((int)(v)), Compact1By1((int)(v >> 1))};
        return new Point2f(bits[0] / (float)(1 << 16), bits[1] / (float)(1 << 16));
    }

}
