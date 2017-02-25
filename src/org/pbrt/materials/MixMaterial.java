
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

public class MixMaterial extends Material {

    public static Material Create(TextureParams mp, Material mat1, Material mat2) {
        Texture<Spectrum> scale = mp.GetSpectrumTexture("amount", new Spectrum(0.5f));
        return new MixMaterial(mat1, mat2, scale);
    }

    public MixMaterial(Material m1, Material m2, Texture<Spectrum> scale) {
        this.m1 = m1;
        this.m2 = m2;
        this.scale = scale;
    }

    @Override
    public SurfaceInteraction ComputeScatteringFunctions(SurfaceInteraction si, TransportMode mode, boolean allowMultipleLobes) {
        // Compute weights and original _BxDF_s for mix material
        Spectrum s1 = scale.Evaluate(si).clamp(0, Pbrt.Infinity);
        Spectrum s2 = (new Spectrum(1).subtract(s1)).clamp(0, Pbrt.Infinity);
        SurfaceInteraction si2 = si;
        m1.ComputeScatteringFunctions(si, mode, allowMultipleLobes);
        m2.ComputeScatteringFunctions(si2, mode, allowMultipleLobes);

        // Initialize _si->bsdf_ with weighted mixture of _BxDF_s
        int n1 = si.bsdf.NumComponents(), n2 = si2.bsdf.NumComponents();
        for (int i = 0; i < n1; ++i)
            si.bsdf.bxdfs[i] = new ScaledBxDF(si.bsdf.bxdfs[i], s1);
        for (int i = 0; i < n2; ++i)
            si.bsdf.Add(new ScaledBxDF(si2.bsdf.bxdfs[i], s2));
        return si;
    }

    private Material m1, m2;
    private Texture<Spectrum> scale;
}