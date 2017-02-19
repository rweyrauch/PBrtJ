/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public final class EFloat {

    public EFloat() {}
    public EFloat(float v) {
        this(v, 0);
    }
    public EFloat(float v, float err) {
        this.v = v;
        if (err == 0) {
            this.low = this.high = v;
        }
        else {
            // Compute conservative bounds by rounding the endpoints away
            // from the middle. Note that this will be over-conservative in
            // cases where v-err or v+err are exactly representable in
            // floating-point, but it's probably not worth the trouble of
            // checking this case.
            low = Pbrt.NextFloatDown(v - err);
            high = Pbrt.NextFloatUp(v + err);
        }
    }

    public EFloat add(EFloat ef) {
        EFloat r = new EFloat();
        r.v = v + ef.v;
        // Interval arithemetic addition, with the result rounded away from
        // the value r.v in order to be conservative.
        r.low = Pbrt.NextFloatDown(lowerBound() + ef.lowerBound());
        r.high = Pbrt.NextFloatUp(upperBound() + ef.upperBound());
        r.check();
        return r;
    }
    public EFloat add(float f) {
        return add(new EFloat(f));
    }
    public EFloat subtract(EFloat ef) {
        EFloat r = new EFloat();
        r.v = v - ef.v;
        r.low = Pbrt.NextFloatDown(lowerBound() - ef.lowerBound());
        r.high = Pbrt.NextFloatUp(upperBound() - ef.upperBound());
        r.check();
        return r;
    }
    public EFloat subtract(float f) {
        return subtract(new EFloat(f));
    }
    public EFloat multiply(EFloat ef) {
        EFloat r = new EFloat();
        r.v = v * ef.v;

        float[] prod = {lowerBound() * ef.lowerBound(), upperBound() * ef.lowerBound(),
                lowerBound() * ef.upperBound(), upperBound() * ef.upperBound()};
        r.low = Pbrt.NextFloatDown(Math.min(Math.min(prod[0], prod[1]), Math.min(prod[2], prod[3])));
        r.high = Pbrt.NextFloatUp(Math.max(Math.max(prod[0], prod[1]), Math.max(prod[2], prod[3])));
        r.check();
        return r;
    }
    public EFloat multiply(float f) {
        return multiply(new EFloat(f));
    }
    public EFloat divide(EFloat ef) {
        EFloat r = new EFloat();
        r.v = v / ef.v;

        if (ef.low < 0 && ef.high > 0) {
            // Bah. The interval we're dividing by straddles zero, so just
            // return an interval of everything.
            r.low = -Pbrt.Infinity;
            r.high = Pbrt.Infinity;
        } else {
            float[] div = {lowerBound() / ef.lowerBound(), upperBound() / ef.lowerBound(),
                    lowerBound() / ef.upperBound(), upperBound() / ef.upperBound()};
            r.low = Pbrt.NextFloatDown(Math.min(Math.min(div[0], div[1]), Math.min(div[2], div[3])));
            r.high = Pbrt.NextFloatUp(Math.max(Math.max(div[0], div[1]), Math.max(div[2], div[3])));
        }
        r.check();
        return r;
    }
    public EFloat negate() {
        EFloat r = new EFloat();
        r.v = -v;

        r.low = -high;
        r.high = -low;
        r.check();
        return r;
    }
    public boolean equals(EFloat ef) {
        return (v == ef.v);
    }

    public float asFloat() { return v; }
    public float getAbsoluteError() { return high - low; }
    public float upperBound() { return high; }
    public float lowerBound() { return low; }

    public void check() {
        if (!Float.isInfinite(low) && !Float.isNaN(low) && !Float.isInfinite(high) && !Float.isNaN(high))
            assert (low <= high);
    }

    public static EFloat sqrt(EFloat ef) {
        EFloat r = new EFloat();
        r.v = (float)Math.sqrt(ef.v);

        r.low = Pbrt.NextFloatDown((float)Math.sqrt(ef.low));
        r.high = Pbrt.NextFloatUp((float)Math.sqrt(ef.high));
        r.check();
        return r;
    }

    public static EFloat abs(EFloat ef) {
        if (ef.low >= 0) {
            // The entire interval is greater than zero, so we're all set.
            return ef;
        } else if (ef.high <= 0) {
            // The entire interval is less than zero.
            EFloat r = new EFloat();
            r.v = -ef.v;

            r.low = -ef.high;
            r.high = -ef.low;
            r.check();
            return r;
        } else {
            // The interval straddles zero.
            EFloat r = new EFloat();
            r.v = Math.abs(ef.v);

            r.low = 0;
            r.high = Math.max(-ef.low, ef.high);
            r.check();
            return r;
        }
    }

    public static class QuadRes {
        public EFloat t0, t1;
    }
    public static QuadRes Quadratic(EFloat a, EFloat b, EFloat c) {
        // Find quadratic discriminant
        double discrim = (double)b.v * (double)b.v - 4 * (double)a.v * (double)c.v;
        if (discrim < 0) return null;
        double rootDiscrim = Math.sqrt(discrim);

        EFloat floatRootDiscrim = new EFloat((float)rootDiscrim, (float)(Pbrt.MachineEpsilon() * rootDiscrim));

        // Compute quadratic _t_ values
        EFloat q;
        final EFloat negHalf = new EFloat(-0.5f);
        if (b.v < 0)
            q = negHalf.multiply(b.subtract(floatRootDiscrim));
        else
            q = negHalf.multiply(b.add(floatRootDiscrim));

        QuadRes res = new QuadRes();
        res.t0 = q.divide(a);
        res.t1 = c.divide(q);
        if (res.t0.v > res.t1.v) {
            EFloat temp = res.t0;
            res.t0 = res.t1;
            res.t1 = temp;
        }
        return res;
    }

    private float v, low, high;
}