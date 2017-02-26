
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.textures;

import org.pbrt.core.*;

public class MarbleTextureSpectrum extends Texture<Spectrum> {

    public MarbleTextureSpectrum(TextureMapping3D mapping, int octaves, float omega, float scale, float variation) {
        this.mapping = mapping;
        this.octaves = octaves;
        this.omega = omega;
        this.scale = scale;
        this.variation = variation;
    }

    @Override
    public Spectrum Evaluate(SurfaceInteraction si) {
        TextureMapping3D.MapPoint point = mapping.Map(si);

        point.p = point.p.scale(scale);
        float marble = point.p.y + variation * FBm(point.p, point.dpdx.scale(scale), point.dpdy.scale(scale), omega, octaves);
        float t = .5f + .5f * (float)Math.sin(marble);
        // Evaluate marble spline at _t_
        final int nc = marbleColors.length;
        final int NSEG = nc - 3;

        int first = (int)Math.floor(t * NSEG);
        t = (t * NSEG - first);
        Spectrum c0 = Spectrum.FromRGB(marbleColors[first]);
        Spectrum c1 = Spectrum.FromRGB(marbleColors[first + 1]);
        Spectrum c2 = Spectrum.FromRGB(marbleColors[first + 2]);
        Spectrum c3 = Spectrum.FromRGB(marbleColors[first + 3]);
        // Bezier spline evaluated with de Castilejau's algorithm
        Spectrum s0 = c0.lerp(t, c1);
        Spectrum s1 = c1.lerp(t, c2);
        Spectrum s2 = c2.lerp(t, c3);
        s0 = s0.lerp(t, s1);
        s1 = s1.lerp(t, s2);
        // Extra scale of 1.5 to increase variation among colors
        return (s0.lerp(t, s1)).scale(1.5f);
    }

    public static Texture<Spectrum> CreateSpectrum(Transform tex2world, TextureParams tp) {
        TextureMapping3D map = new IdentityMapping3D(tex2world);
        return new MarbleTextureSpectrum(map, tp.FindInt("octaves", 8),
                tp.FindFloat("roughness", .5f),
                tp.FindFloat("scale", 1.f),
                tp.FindFloat("variation", .2f));
    }

    private TextureMapping3D mapping;
    private final int octaves;
    private final float omega, scale, variation;

    private static float[][] marbleColors = {
            {.58f, .58f, .6f}, {.58f, .58f, .6f}, {.58f, .58f, .6f},
            {.5f, .5f, .5f},   {.6f, .59f, .58f}, {.58f, .58f, .6f},
            {.58f, .58f, .6f}, {.2f, .2f, .33f},  {.58f, .58f, .6f},
    };
}