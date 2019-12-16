
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

public class BilerpTextureSpectrum extends Texture<Spectrum> {

    public BilerpTextureSpectrum(TextureMapping2D mapping, Spectrum v00, Spectrum v01, Spectrum v10, Spectrum v11) {
        this.mapping = mapping;
        this.v00 = v00;
        this.v01 = v01;
        this.v10 = v10;
        this.v11 = v11;
    }

    @Override
    public Spectrum Evaluate(SurfaceInteraction si) {
        TextureMapping2D.MapPoint point = mapping.Map(si);
        Point2f st = point.st;
        return (v00.scale((1 - st.x) * (1 - st.y))).add(v01.scale((1 - st.x) * (st.y))).add(v10.scale((st.x) * (1 - st.y))).add(v11.scale((st.x) * (st.y)));
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
                    tp.FindFloat("udelta", 0),
                    tp.FindFloat("vdelta", 0));
        else {
            PBrtTLogger.Error("2D texture mapping \"%s\" unknown", type);
            map = new UVMapping2D();
        }
        return new BilerpTextureSpectrum(map, tp.FindSpectrum("v00", new Spectrum(0)),
                tp.FindSpectrum("v01", new Spectrum(1)), tp.FindSpectrum("v10", new Spectrum(0)),
                tp.FindSpectrum("v11", new Spectrum(1)));
    }

    private TextureMapping2D mapping;
    private final Spectrum v00, v01, v10, v11;

}