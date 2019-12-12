
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public abstract class Material {

    public enum TransportMode {
        Radiance,
        Importance
    }

    public abstract void ComputeScatteringFunctions(SurfaceInteraction si, TransportMode mode, boolean allowMultipleLobes);

    public static void Bump(Texture<Float> d, SurfaceInteraction si) {
        // Compute offset positions and evaluate displacement texture
        SurfaceInteraction siEval = new SurfaceInteraction(si);

        // Shift _siEval_ _du_ in the $u$ direction
        float du = .5f * (Math.abs(si.dudx) + Math.abs(si.dudy));
        // The most common reason for du to be zero is for ray that start from
        // light sources, where no differentials are available. In this case,
        // we try to choose a small enough du so that we still get a decently
        // accurate bump value.
        if (du == 0) du = .0005f;
        siEval.p = si.p.add(si.shading.dpdu.scale(du));
        siEval.uv = si.uv.add(new Vector2f(du, 0.f));
        siEval.n = new Normal3f(Vector3f.Normalize(Vector3f.Cross(si.shading.dpdu, si.shading.dpdv).add(si.dndu.scale(du))));
        float uDisplace = d.Evaluate(siEval);

        // Shift _siEval_ _dv_ in the $v$ direction
        float dv = .5f * (Math.abs(si.dvdx) + Math.abs(si.dvdy));
        if (dv == 0) dv = .0005f;
        siEval.p = si.p.add(si.shading.dpdv.scale(dv));
        siEval.uv = si.uv.add(new Vector2f(0.f, dv));
        siEval.n = new Normal3f(Vector3f.Normalize(Vector3f.Cross(si.shading.dpdu, si.shading.dpdv).add(si.dndv.scale(dv))));
        float vDisplace = d.Evaluate(siEval);
        float displace = d.Evaluate(si);

        // Compute bump-mapped differential geometry
        Vector3f dpdu = si.shading.dpdu.add(si.shading.n.scale((uDisplace - displace) / du)).add(si.shading.dndu.scale(displace));
        Vector3f dpdv = si.shading.dpdv.add(si.shading.n.scale((vDisplace - displace) / dv)).add(si.shading.dndv.scale(displace));
        si.SetShadingGeometry(dpdu, dpdv, si.shading.dndu, si.shading.dndv, false);
    }

}