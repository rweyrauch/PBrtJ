
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
import org.pbrt.core.Error;

import java.util.Objects;

public class CheckerBoardTextureFloat extends Texture<Float> {

    public CheckerBoardTextureFloat(TextureMapping2D mapping,
                          Texture<Float> tex1,
                          Texture<Float> tex2,
                          AAMethod aaMethod) {
        this.mapping = mapping;
        this.tex1 = tex1;
        this.tex2 = tex2;
        this.aaMethod = aaMethod;
    }

    @Override
    public Float Evaluate(SurfaceInteraction si) {
        TextureMapping2D.MapPoint point = mapping.Map(si);
        if (aaMethod == Texture.AAMethod.None) {
            // Point sample _Checkerboard2DTexture_
            if (((int)Math.floor(point.st.x) + (int)Math.floor(point.st.y)) % 2 == 0)
                return tex1.Evaluate(si);
            return tex2.Evaluate(si);
        }
        else {
            // Compute closed-form box-filtered _Checkerboard2DTexture_ value
            Point2f st = point.st;
            Vector2f dstdx = point.dstdx;
            Vector2f dstdy = point.dstdy;

            // Evaluate single check if filter is entirely inside one of them
            float ds = Math.max(Math.abs(dstdx.x), Math.abs(dstdy.x));
            float dt = Math.max(Math.abs(dstdx.y), Math.abs(dstdy.y));
            float s0 = st.x - ds, s1 = st.x + ds;
            float t0 = st.y - dt, t1 = st.y + dt;
            if (Math.floor(s0) == Math.floor(s1) && Math.floor(t0) == Math.floor(t1)) {
                // Point sample _Checkerboard2DTexture_
                if (((int)Math.floor(st.x) + (int)Math.floor(st.y)) % 2 == 0)
                    return tex1.Evaluate(si);
                return tex2.Evaluate(si);
            }

            // Apply box filter to checkerboard region
            float sint = (bumpInt(s1) - bumpInt(s0)) / (2 * ds);
            float tint = (bumpInt(t1) - bumpInt(t0)) / (2 * dt);
            float area2 = sint + tint - 2 * sint * tint;
            if (ds > 1 || dt > 1) area2 = .5f;
            return (1 - area2) * tex1.Evaluate(si) +
                    area2 * tex2.Evaluate(si);
        }
    }

    private static float bumpInt(float x) {
        return (int)Math.floor(x / 2) + 2 * Math.max(x / 2 - (int)Math.floor(x / 2) - 0.5f, 0);
    }

    public static Texture<Float> CreateFloat(Transform tex2world, TextureParams tp) {
        int dim = tp.FindInt("dimension", 2);
        if (dim != 2 && dim != 3) {
            Error.Error("%d dimensional checkerboard texture not supported", dim);
            return null;
        }
        Texture<Float> tex1 = tp.GetFloatTexture("tex1", 1);
        Texture<Float> tex2 = tp.GetFloatTexture("tex2", 0);
        if (dim == 2) {
            // Initialize 2D texture mapping _map_ from _tp_
            TextureMapping2D map;
            String type = tp.FindString("mapping", "uv");
            if (Objects.equals(type, "uv")) {
                Float su = tp.FindFloat("uscale", 1);
                Float sv = tp.FindFloat("vscale", 1);
                Float du = tp.FindFloat("udelta", 0);
                Float dv = tp.FindFloat("vdelta", 0);
                map = new UVMapping2D(su, sv, du, dv);
            } else if (Objects.equals(type, "spherical"))
                map = new SphericalMapping2D(Transform.Inverse(tex2world));
            else if (Objects.equals(type, "cylindrical"))
                map = new CylindricalMapping2D(Transform.Inverse(tex2world));
            else if (Objects.equals(type, "planar"))
                map = new PlanarMapping2D(
                        tp.FindVector3f("v1", new Vector3f(1, 0, 0)),
                        tp.FindVector3f("v2", new Vector3f(0, 1, 0)),
                        tp.FindFloat("udelta", 0), tp.FindFloat("vdelta", 0));
            else {
                Error.Error("2D texture mapping \"%s\" unknown", type);
                map = new UVMapping2D();
            }

            // Compute _aaMethod_ for _CheckerboardTexture_
            String aa = tp.FindString("aamode", "closedform");
            Texture.AAMethod aaMethod;
            if (Objects.equals(aa, "none"))
                aaMethod = AAMethod.None;
            else if (Objects.equals(aa, "closedform"))
                aaMethod = AAMethod.ClosedForm;
            else {
                Error.Warning("Antialiasing mode \"%s\" not understood by Checkerboard2DTexture; using \"closedform\"", aa);
                aaMethod = AAMethod.ClosedForm;
            }
            return new CheckerBoardTextureFloat(map, tex1, tex2, aaMethod);
        }
        else {
            // Initialize 3D texture mapping _map_ from _tp_
            //TextureMapping3D map = new IdentityMapping3D(tex2world);
            //return new CheckerBoard3DTextureFloat(map, tex1, tex2);
            return null;
        }
    }

    private TextureMapping2D mapping;
    private final Texture<Float> tex1, tex2;
    private final Texture.AAMethod aaMethod;
}