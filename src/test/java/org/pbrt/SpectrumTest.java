/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */
package org.pbrt;

import org.junit.Test;

import org.pbrt.core.*;

import static org.junit.Assert.assertTrue;

public class SpectrumTest {

    interface ErrorFunction {
        float err(float val, float ref);
    }

    @Test
    public void testBlackbody() {
        // Relative error.
        ErrorFunction err = (float val, float ref) -> { return (float)Math.abs(val - ref) / ref; };

        // Planck's law.
        // A few values via
        // http://www.spectralcalc.com/blackbody_calculator/blackbody.php
        // lambda, T, expected radiance
        float[][] v = {
            {483, 6000, 3.1849e13f},
            {600, 6000, 2.86772e13f},
            {500, 3700, 1.59845e12f},
            {600, 4500, 7.46497e12f},
        };
        for (int i = 0; i < v.length; ++i) {
            float Le = Spectrum.Blackbody(v[i][0], v[i][1]);
            assertTrue(err.err(Le, v[i][2]) < .001);
        }

        // Use Wien's displacement law to compute maximum wavelength for a few
        // temperatures, then confirm that the value returned by Blackbody() is
        // consistent with this.
        float[] Temps = {2700, 3000, 4500, 5600, 6000};
        for (float T : Temps) {
            float lambdaMax = 2.8977721e-3f / T * 1e9f;
            float[] lambda = {.999f * lambdaMax, lambdaMax, 1.001f * lambdaMax};
            float[] Le = Spectrum.Blackbody(lambda, T);
            assertTrue(Le[0] < Le[1]);
            assertTrue(Le[1] > Le[2]);
        }
    }

}
