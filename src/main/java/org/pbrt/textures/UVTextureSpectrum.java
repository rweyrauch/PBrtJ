
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
import org.pbrt.core.PBrtTLogger;

import java.util.Objects;

public class UVTextureSpectrum extends Texture<Spectrum> {

    public UVTextureSpectrum(TextureMapping2D mapping) {
        this.mapping = mapping;
    }

    @Override
    public Spectrum Evaluate(SurfaceInteraction si) {
        TextureMapping2D.MapPoint point = mapping.Map(si);
        float[] rgb = {point.st.x - (float)Math.floor(point.st.x), point.st.y - (float)Math.floor(point.st.y), 0};
        return Spectrum.FromRGB(rgb);
    }

    public static Texture<Spectrum> CreateSpectrum(Transform tex2world, TextureParams tp) {
        // Initialize 2D texture mapping _map_ from _tp_
        TextureMapping2D map;
        String type = tp.FindString("mapping", "uv");
        if (Objects.equals(type, "uv")) {
            float su = tp.FindFloat("uscale", 1);
            float sv = tp.FindFloat("vscale", 1);
            float du = tp.FindFloat("udelta", 0);
            float dv = tp.FindFloat("vdelta", 0);
            map = new UVMapping2D(su, sv, du, dv);
        } else if (Objects.equals(type, "spherical"))
            map = new SphericalMapping2D(Transform.Inverse(tex2world));
        else if (Objects.equals(type, "cylindrical"))
            map = new CylindricalMapping2D(Transform.Inverse(tex2world));
        else if (Objects.equals(type, "planar"))
            map = new PlanarMapping2D(tp.FindVector3f("v1", new Vector3f(1, 0, 0)),
                    tp.FindVector3f("v2", new Vector3f(0, 1, 0)),
                    tp.FindFloat("udelta", 0.f),
                    tp.FindFloat("vdelta", 0.f));
        else {
            PBrtTLogger.Error("2D texture mapping \"%s\" unknown", type);
            map = new UVMapping2D();
        }
        return new UVTextureSpectrum(map);
    }

    private TextureMapping2D mapping;
}