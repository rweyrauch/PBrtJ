
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.textures;

import org.jetbrains.annotations.NotNull;
import org.pbrt.core.*;

import java.util.HashMap;
import java.util.Objects;

public class ImageMap<T extends ArithmeticOps<T>> extends Texture {

    public static Texture<Float> CreateFloat(Transform tex2world, TextureParams tp) {
        return null;
    }

    public static Texture<Spectrum> CreateSpectrum(Transform tex2world, TextureParams tp) {
        return null;
    }

    @Override
    public Object Evaluate(SurfaceInteraction si) {
        return null;
    }

    private TextureMapping2D mapping;
    private MIPMap<T> mipmap;

    private static HashMap<TexInfo, MIPMap<FloatTexel>> texturesFloat;
    private static HashMap<TexInfo, MIPMap<Spectrum>> texturesSpectrum;

    private static class TexInfo implements Comparable<TexInfo> {
        public TexInfo(String filename, boolean doTrilinear, float maxAniso, MIPMap.ImageWrap wrapMode, float scale, boolean gamma) {
            this.filename = filename;
            this.doTrilinear = doTrilinear;
            this.maxAniso = maxAniso;
            this.wrapMode = wrapMode;
            this.scale = scale;
            this.gamma = gamma;
        }

        @Override
        public int compareTo(@NotNull TexInfo t2) {
            if (!Objects.equals(filename, t2.filename)) return filename.compareTo(t2.filename);
            if (doTrilinear != t2.doTrilinear) return -1;
            if (maxAniso != t2.maxAniso) return (maxAniso < t2.maxAniso) ? -1 : 1;
            if (scale != t2.scale) return (scale < t2.scale) ? -1 : 1;
            if (gamma != t2.gamma) return -1;
            return (wrapMode != t2.wrapMode) ? -1 : 0;
        }

        public String filename;
        public boolean doTrilinear;
        public float maxAniso;
        public MIPMap.ImageWrap wrapMode;
        public float scale;
        public boolean gamma;
    }
}