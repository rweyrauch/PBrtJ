/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

import org.apache.commons.lang.NotImplementedException;

import java.io.InputStream;
import java.io.PrintStream;

public class CoefficientSpectrum {

    public enum SpectrumType {
        Reflectance,
        Illuminant
    }

    protected float[] c;

    protected CoefficientSpectrum(float v, int num) {
        c = new float[num];
        for (int i = 0; i < c.length; i++) {
            c[i] = v;
        }
    }
    protected CoefficientSpectrum(CoefficientSpectrum cs) {
        this.c = cs.c.clone();
    }

    public void Print(PrintStream ps) {
        ps.print("[ ");
        for (int i = 0; i < c.length; i++) {
            ps.printf("%f", c[i]);
            if (i != c.length - 1) ps.printf(", ");
        }
        ps.print("]");
    }

    public void add(CoefficientSpectrum s2) {
        assert (!s2.HasNaNs());
        for (int i = 0; i < c.length; ++i) {
            this.c[i] += s2.c[i];
        }
    }
    public static CoefficientSpectrum add(CoefficientSpectrum s1, CoefficientSpectrum s2) {
        assert !s1.HasNaNs();
        assert !s2.HasNaNs();
        CoefficientSpectrum cs = new CoefficientSpectrum(0,s1.c.length);
        for (int i = 0; i < s1.c.length; ++i) {
            cs.c[i] = s1.c[i] + s2.c[i];
        }
        return cs;
    }

    public void subtract(CoefficientSpectrum s2) {
        assert (!s2.HasNaNs());
        for (int i = 0; i < c.length; ++i) {
            this.c[i] -= s2.c[i];
        }
    }
    public void divide(CoefficientSpectrum s2) {
        assert (!s2.HasNaNs());
        for (int i = 0; i < c.length; ++i) {
            assert s2.c[i] != 0.0f;
            this.c[i] /= s2.c[i];
        }
    }
    public void multiply(CoefficientSpectrum s2) {
        assert (!s2.HasNaNs());
        for (int i = 0; i < c.length; ++i) {
            this.c[i] *= s2.c[i];
        }
    }
    public void scale(float a) {
        for (int i = 0; i < c.length; ++i) {
            this.c[i] *= a;
        }
        assert (!HasNaNs());
    }
    public void invScale(float a) {
        assert (a != 0.0f);
        for (int i = 0; i < c.length; ++i) {
            this.c[i] /= a;
        }
    }
    public static CoefficientSpectrum scale(CoefficientSpectrum cs, float scale) {
        CoefficientSpectrum scs = new CoefficientSpectrum(cs);
        scs.scale(scale);
        return scs;
    }
    public boolean equal(CoefficientSpectrum s2) {
        for (int i = 0; i < c.length; ++i) {
            if (c[i] != s2.c[i]) return false;
        }
        return true;
    }
    public boolean notEqual(CoefficientSpectrum s2) {
        return !equal(s2);
    }

    public boolean IsBlack() {
        for (int i = 0; i < c.length; ++i) {
            if (c[i] != 0.0f) return false;
        }
        return true;
    }

    public static CoefficientSpectrum Sqrt(CoefficientSpectrum s) {
        CoefficientSpectrum ret = new CoefficientSpectrum(0.0f, s.c.length);
        for (int i = 0; i < s.c.length; ++i) {
            ret.c[i] = (float)Math.sqrt(s.c[i]);
        }
        assert !ret.HasNaNs();
        return ret;
    }

    public static CoefficientSpectrum Pow(CoefficientSpectrum s, float e) {
        CoefficientSpectrum ret = new CoefficientSpectrum(0.0f, s.c.length);
        for (int i = 0; i < s.c.length; ++i) {
            ret.c[i] = (float)Math.pow(s.c[i], e);
        }
        assert !ret.HasNaNs();
        return ret;
    }

    public CoefficientSpectrum negate() {
        CoefficientSpectrum ret = new CoefficientSpectrum(0.0f, c.length);
        for (int i = 0; i < c.length; ++i) {
            ret.c[i] = c[i];
        }
        return ret;
    }

    public static CoefficientSpectrum Exp(CoefficientSpectrum s, float e) {
        CoefficientSpectrum ret = new CoefficientSpectrum(0.0f, s.c.length);
        for (int i = 0; i < s.c.length; ++i) {
            ret.c[i] = (float)Math.exp(s.c[i]);
        }
        assert !ret.HasNaNs();
        return ret;
    }

    public CoefficientSpectrum Clamp(float low, float high) {
        CoefficientSpectrum ret = new CoefficientSpectrum(0.0f, c.length);
        for (int i = 0; i < c.length; ++i)
            ret.c[i] = Pbrt.Clamp(c[i], low, high);
        assert !ret.HasNaNs();
        return ret;
    }

    public float MaxComponentValue() {
        float m = c[0];
        for (int i = 1; i < c.length; ++i)
            m = Math.max(m, c[i]);
        return m;
    }

    boolean Write(PrintStream f) {
        for (int i = 0; i < c.length; ++i)
            f.printf("%f ", c[i]);
        return true;
    }
    boolean Read(InputStream f) {
        throw new NotImplementedException("TODO");
    }

    public int numSamples() {
        return c.length;
    }
    public void set(int i, float v) {
        assert (i >= 0 && i < c.length);
        c[i] = v;
    }
    public float at(int i) {
        assert (i >= 0 && i < c.length);
        return c[i];
    }

    boolean HasNaNs() {
        for (int i = 0; i < c.length; i++) {
            if (Float.isNaN(c[i])) return true;
        }
        return false;
    }

}