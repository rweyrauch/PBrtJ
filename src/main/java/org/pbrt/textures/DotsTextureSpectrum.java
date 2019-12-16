
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

public class DotsTextureSpectrum extends Texture<Spectrum> {

    public DotsTextureSpectrum(TextureMapping2D mapping,
                Texture<Spectrum> outsideDot,
                Texture<Spectrum> insideDot) {
        this.mapping = mapping;
        this.outsideDot = outsideDot;
        this.insideDot =insideDot;
    }

    @Override
    public Spectrum Evaluate(SurfaceInteraction si) {
        // Compute cell indices for dots
        TextureMapping2D.MapPoint point = mapping.Map(si);
        Point2f st = point.st;
        int sCell = (int)Math.floor(st.x + .5f), tCell = (int)Math.floor(st.y + .5f);

        // Return _insideDot_ result if point is inside dot
        if (Noise(sCell + .5f, tCell + .5f) > 0) {
            float radius = .35f;
            float maxShift = 0.5f - radius;
            float sCenter = sCell + maxShift * Noise(sCell + 1.5f, tCell + 2.8f);
            float tCenter = tCell + maxShift * Noise(sCell + 4.5f, tCell + 9.8f);
            Vector2f dst = st.subtract(new Point2f(sCenter, tCenter));
            if (dst.LengthSquared() < radius * radius)
                return insideDot.Evaluate(si);
        }
        return outsideDot.Evaluate(si);
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
        return new DotsTextureSpectrum(map,
                tp.GetSpectrumTexture("inside", new Spectrum(1)),
                tp.GetSpectrumTexture("outside", new Spectrum(0)));
    }

    private TextureMapping2D mapping;
    Texture<Spectrum> outsideDot, insideDot;
}