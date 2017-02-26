
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

public class BilerpTextureFloat extends Texture<Float> {

    public BilerpTextureFloat(TextureMapping2D mapping, float v00, float v01, float v10, float v11) {
        this.mapping = mapping;
        this.v00 = v00;
        this.v01 = v01;
        this.v10 = v10;
        this.v11 = v11;
    }

    @Override
    public Float Evaluate(SurfaceInteraction si) {
        TextureMapping2D.MapPoint point = mapping.Map(si);
        Point2f st = point.st;
        return (1 - st.x) * (1 - st.y) * v00 + (1 - st.x) * (st.y) * v01 +
                (st.x) * (1 - st.y) * v10 + (st.x) * (st.y) * v11;
    }

    public static Texture<Float> CreateFloat(Transform tex2world, TextureParams tp) {
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
            Error.Error("2D texture mapping \"%s\" unknown", type);
            map = new UVMapping2D();
        }
        return new BilerpTextureFloat(map, tp.FindFloat("v00", 0), tp.FindFloat("v01", 1),
                tp.FindFloat("v10", 0), tp.FindFloat("v11", 1));
    }

    private TextureMapping2D mapping;
    private final Float v00, v01, v10, v11;

}