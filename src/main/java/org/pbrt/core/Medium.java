
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

import java.util.Objects;

public abstract class Medium {
    public class MediumSample {
        public Spectrum spectrum;
        public MediumInteraction mi;
    }
    public abstract Spectrum Tr(Ray ray, Sampler sampler);
    public abstract MediumSample Sample(Ray ray, Sampler sampler);

    private static class MeasuredSS {
        public MeasuredSS(String name, double[] sigma_s, double[] sigma_a) {
            this.name = name;
            this.sigma_prime_s = sigma_s;
            this.sigma_a = sigma_a;
        }
        String name;
        double sigma_prime_s[], sigma_a[];  // mm^-1
    }
    private static MeasuredSS SubsurfaceParameterTable[] = new MeasuredSS[]{
            // From "A Practical Model for Subsurface Light Transport"
            // Jensen, Marschner, Levoy, Hanrahan
            // Proc SIGGRAPH 2001
            new MeasuredSS("Apple", new double[]{2.29, 2.39, 1.97}, new double[]{0.0030, 0.0034, 0.046}),
            new MeasuredSS("Chicken1", new double[]{0.15, 0.21, 0.38}, new double[]{0.015, 0.077, 0.19}),
            new MeasuredSS("Chicken2", new double[]{0.19, 0.25, 0.32}, new double[]{0.018, 0.088, 0.20}),
            new MeasuredSS("Cream", new double[]{7.38, 5.47, 3.15}, new double[]{0.0002, 0.0028, 0.0163}),
            new MeasuredSS("Ketchup", new double[]{0.18, 0.07, 0.03}, new double[]{0.061, 0.97, 1.45}),
            new MeasuredSS("Marble", new double[]{2.19, 2.62, 3.00}, new double[]{0.0021, 0.0041, 0.0071}),
            new MeasuredSS("Potato", new double[]{0.68, 0.70, 0.55}, new double[]{0.0024, 0.0090, 0.12}),
            new MeasuredSS("Skimmilk", new double[]{0.70, 1.22, 1.90}, new double[]{0.0014, 0.0025, 0.0142}),
            new MeasuredSS("Skin1", new double[]{0.74, 0.88, 1.01}, new double[]{0.032, 0.17, 0.48}),
            new MeasuredSS("Skin2", new double[]{1.09, 1.59, 1.79}, new double[]{0.013, 0.070, 0.145}),
            new MeasuredSS("Spectralon", new double[]{11.6, 20.4, 14.9}, new double[]{0.00, 0.00, 0.00}),
            new MeasuredSS("Wholemilk", new double[]{2.55, 3.21, 3.77}, new double[]{0.0011, 0.0024, 0.014}),

            // From "Acquiring Scattering Properties of Participating Media by
            // Dilution",
            // Narasimhan, Gupta, Donner, Ramamoorthi, Nayar, Jensen
            // Proc SIGGRAPH 2006
            new MeasuredSS("Lowfat Milk", new double[]{0.89187, 1.5136, 2.532}, new double[]{0.002875, 0.00575, 0.0115}),
            new MeasuredSS("Reduced Milk", new double[]{2.4858, 3.1669, 4.5214}, new double[]{0.0025556, 0.0051111, 0.012778}),
            new MeasuredSS("Regular Milk", new double[]{4.5513, 5.8294, 7.136}, new double[]{0.0015333, 0.0046, 0.019933}),
            new MeasuredSS("Espresso", new double[]{0.72378, 0.84557, 1.0247}, new double[]{4.7984, 6.5751, 8.8493}),
            new MeasuredSS("Mint Mocha Coffee", new double[]{0.31602, 0.38538, 0.48131}, new double[]{3.772, 5.8228, 7.82}),
            new MeasuredSS("Lowfat Soy Milk", new double[]{0.30576, 0.34233, 0.61664}, new double[]{0.0014375, 0.0071875, 0.035937}),
            new MeasuredSS("Regular Soy Milk", new double[]{0.59223, 0.73866, 1.4693}, new double[]{0.0019167, 0.0095833, 0.065167}),
            new MeasuredSS("Lowfat Chocolate Milk", new double[]{0.64925, 0.83916, 1.1057}, new double[]{0.0115, 0.0368, 0.1564}),
            new MeasuredSS("Regular Chocolate Milk", new double[]{1.4585, 2.1289, 2.9527}, new double[]{0.010063, 0.043125, 0.14375}),
            new MeasuredSS("Coke", new double[]{8.9053e-05, 8.372e-05, 0}, new double[]{0.10014, 0.16503, 0.2468}),
            new MeasuredSS("Pepsi", new double[]{6.1697e-05, 4.2564e-05, 0}, new double[]{0.091641, 0.14158, 0.20729}),
            new MeasuredSS("Sprite", new double[]{6.0306e-06, 6.4139e-06, 6.5504e-06}, new double[]{0.001886, 0.0018308, 0.0020025}),
            new MeasuredSS("Gatorade", new double[]{0.0024574, 0.003007, 0.0037325}, new double[]{0.024794, 0.019289, 0.008878}),
            new MeasuredSS("Chardonnay", new double[]{1.7982e-05, 1.3758e-05, 1.2023e-05}, new double[]{0.010782, 0.011855, 0.023997}),
            new MeasuredSS("White Zinfandel", new double[]{1.7501e-05, 1.9069e-05, 1.288e-05}, new double[]{0.012072, 0.016184, 0.019843}),
            new MeasuredSS("Merlot", new double[]{2.1129e-05, 0, 0}, new double[]{0.11632, 0.25191, 0.29434}),
            new MeasuredSS("Budweiser Beer", new double[]{2.4356e-05, 2.4079e-05, 1.0564e-05}, new double[]{0.011492, 0.024911, 0.057786}),
            new MeasuredSS("Coors Light Beer", new double[]{5.0922e-05, 4.301e-05, 0}, new double[]{0.006164, 0.013984, 0.034983}),
            new MeasuredSS("Clorox", new double[]{0.0024035, 0.0031373, 0.003991}, new double[]{0.0033542, 0.014892, 0.026297}),
            new MeasuredSS("Apple Juice", new double[]{0.00013612, 0.00015836, 0.000227}, new double[]{0.012957, 0.023741, 0.052184}),
            new MeasuredSS("Cranberry Juice", new double[]{0.00010402, 0.00011646, 7.8139e-05}, new double[]{0.039437, 0.094223, 0.12426}),
            new MeasuredSS("Grape Juice", new double[]{5.382e-05, 0, 0}, new double[]{0.10404, 0.23958, 0.29325}),
            new MeasuredSS("Ruby Grapefruit Juice", new double[]{0.011002, 0.010927, 0.011036}, new double[]{0.085867, 0.18314, 0.25262}),
            new MeasuredSS("White Grapefruit Juice", new double[]{0.22826, 0.23998, 0.32748}, new double[]{0.0138, 0.018831, 0.056781}),
            new MeasuredSS("Shampoo", new double[]{0.0007176, 0.0008303, 0.0009016}, new double[]{0.014107, 0.045693, 0.061717}),
            new MeasuredSS("Strawberry Shampoo", new double[]{0.00015671, 0.00015947, 1.518e-05}, new double[]{0.01449, 0.05796, 0.075823}),
            new MeasuredSS("Head & Shoulders Shampoo", new double[]{0.023805, 0.028804, 0.034306}, new double[]{0.084621, 0.15688, 0.20365}),
            new MeasuredSS("Lemon Tea Powder", new double[]{0.040224, 0.045264, 0.051081}, new double[]{2.4288, 4.5757, 7.2127}),
            new MeasuredSS("Orange Powder", new double[]{0.00015617, 0.00017482, 0.0001762}, new double[]{0.001449, 0.003441, 0.007863}),
            new MeasuredSS("Pink Lemonade Powder", new double[]{0.00012103, 0.00013073, 0.00012528}, new double[]{0.001165, 0.002366, 0.003195}),
            new MeasuredSS("Cappuccino Powder", new double[]{1.8436, 2.5851, 2.1662}, new double[]{35.844, 49.547, 61.084}),
            new MeasuredSS("Salt Powder", new double[]{0.027333, 0.032451, 0.031979}, new double[]{0.28415, 0.3257, 0.34148}),
            new MeasuredSS("Sugar Powder", new double[]{0.00022272, 0.00025513, 0.000271}, new double[]{0.012638, 0.031051, 0.050124}),
            new MeasuredSS("Suisse Mocha Powder", new double[]{2.7979, 3.5452, 4.3365}, new double[]{17.502, 27.004, 35.433}),
            new MeasuredSS("Pacific Ocean Surface Water", new double[]{0.0001764, 0.00032095, 0.00019617}, new double[]{0.031845, 0.031324, 0.030147})
    };

    public static class ScatteringProps {
        public Spectrum sigma_a, sigma_s;
    }
    private static float[] toFloatArray(double[] arr) {
        if (arr == null) return null;
        float[] farr = new float[arr.length];
        for (int i = 0; i < arr.length; i++) {
            farr[i] = (float)arr[i];
        }
        return farr;
    }
    public static ScatteringProps GetMediumScatteringProperties(String name) {
        for (MeasuredSS mss : SubsurfaceParameterTable) {
            if (Objects.equals(name, mss.name)) {
                ScatteringProps props = new ScatteringProps();
                props.sigma_a = Spectrum.FromRGB(toFloatArray(mss.sigma_a));
                props.sigma_s = Spectrum.FromRGB(toFloatArray(mss.sigma_prime_s));
                return props;
            }
        }
        return null;
    }

    // Media Inline Functions
    public static float PhaseHG(float cosTheta, float g) {
        float denom = 1 + g * g + 2 * g * cosTheta;
        return Pbrt.Inv4Pi * (1 - g * g) / (denom * (float)Math.sqrt(denom));
    }

}