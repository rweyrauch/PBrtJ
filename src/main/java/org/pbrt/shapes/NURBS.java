
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.shapes;

import org.pbrt.core.*;
import org.pbrt.core.PBrtTLogger;

import java.util.ArrayList;
import java.util.function.BiFunction;

public class NURBS {

    public static ArrayList<Shape> Create(Transform object2world, Transform world2object, boolean reverseOrientation, ParamSet paramSet) {
        int nu = paramSet.FindOneInt("nu", -1);
        if (nu == -1) {
            PBrtTLogger.Error("Must provide number of control points \"nu\" with NURBS shape.");
            return null;
        }

        int uorder = paramSet.FindOneInt("uorder", -1);
        if (uorder == -1) {
            PBrtTLogger.Error("Must provide u order \"uorder\" with NURBS shape.");
            return null;
        }
        Float[] uknots = paramSet.FindFloat("uknots");
        if (uknots == null) {
            PBrtTLogger.Error("Must provide u knot vector \"uknots\" with NURBS shape.");
            return null;
        }

        if (uknots.length != nu + uorder) {
            PBrtTLogger.Error("Number of knots in u knot vector %d doesn't match sum of number of u control points %d and u order %d.",
                    uknots.length, nu, uorder);
            return null;
        }

        float u0 = paramSet.FindOneFloat("u0", uknots[uorder - 1]);
        float u1 = paramSet.FindOneFloat("u1", uknots[nu]);

        int nv = paramSet.FindOneInt("nv", -1);
        if (nv == -1) {
            PBrtTLogger.Error("Must provide number of control points \"nv\" with NURBS shape.");
            return null;
        }

        int vorder = paramSet.FindOneInt("vorder", -1);
        if (vorder == -1) {
            PBrtTLogger.Error("Must provide v order \"vorder\" with NURBS shape.");
            return null;
        }

        Float[] vknots = paramSet.FindFloat("vknots");
        if (vknots == null) {
            PBrtTLogger.Error("Must provide v knot vector \"vknots\" with NURBS shape.");
            return null;
        }

        if (vknots.length != nv + vorder) {
            PBrtTLogger.Error("Number of knots in v knot vector %d doesn't match sum of number of v control points %d and v order %d.",
                    vknots.length, nv, vorder);
            return null;
        }

        float v0 = paramSet.FindOneFloat("v0", vknots[vorder - 1]);
        float v1 = paramSet.FindOneFloat("v1", vknots[nv]);

        boolean isHomogeneous = false;
        int npts;
        Float[] Pp = null;
        Point3f[] P = paramSet.FindPoint3f("P");
        if (P == null) {
            Pp = paramSet.FindFloat("Pw");
            if (Pp == null) {
                PBrtTLogger.Error("Must provide control points via \"P\" or \"Pw\" parameter to NURBS shape.");
                return null;
            }
            npts = Pp.length;
            if ((Pp.length % 4) != 0) {
                PBrtTLogger.Error("Number of \"Pw\" control points provided to NURBS shape must be multiple of four");
                return null;
            }
            npts /= 4;
            isHomogeneous = true;
        }
        else
        {
            npts = P.length;
        }

        if (npts != nu * nv) {
            PBrtTLogger.Error("NURBS shape was expecting %dx%d=%d control points, was given %d", nu, nv, nu * nv, npts);
            return null;
        }

        // Compute NURBS dicing rates
        int diceu = 30, dicev = 30;
        float[] ueval = new float[diceu];
        float[] veval = new float[dicev];
        Point3f[] evalPs = new Point3f[diceu * dicev];
        Normal3f[] evalNs = new Normal3f[diceu * dicev];

        for (int i = 0; i < diceu; ++i)
            ueval[i] = Pbrt.Lerp((float)i / (float)(diceu - 1), u0, u1);
        for (int i = 0; i < dicev; ++i)
            veval[i] = Pbrt.Lerp((float)i / (float)(dicev - 1), v0, v1);

        // Evaluate NURBS over grid of points
        Point2f[] uvs = new Point2f[diceu * dicev];

        // Turn NURBS into triangles
        Homogeneous3[] Pw = new Homogeneous3[nu * nv];
        if (isHomogeneous) {
            for (int i = 0; i < nu * nv; ++i) {
                Pw[i] = new Homogeneous3(Pp[4 * i], Pp[4 * i + 1], Pp[4 * i + 2], Pp[4 * i + 3]);
            }
        } else {
            for (int i = 0; i < nu * nv; ++i) {
                Pw[i] = new Homogeneous3(P[i].x, P[i].y, P[i].z, 1);
            }
        }

        for (int v = 0; v < dicev; ++v) {
            for (int u = 0; u < diceu; ++u) {
                uvs[(v * diceu + u)] = new Point2f(ueval[u], veval[v]);

                Vector3f dpdu = new Vector3f(), dpdv = new Vector3f();
                Point3f pt = NURBSEvaluateSurface(uorder, uknots, nu, ueval[u], vorder, vknots, nv, veval[v], Pw, dpdu, dpdv);
                evalPs[v * diceu + u] = new Point3f(pt.x, pt.y, pt.z);
                evalNs[v * diceu + u] = new Normal3f(Vector3f.Normalize(Vector3f.Cross(dpdu, dpdv)));
            }
        }

        // Generate points-polygons mesh
        int nTris = 2 * (diceu - 1) * (dicev - 1);
        int[] vertices = new int[3 * nTris];
        int[] vertp = vertices;

        BiFunction<Integer, Integer, Integer> VN = (u, v) -> v * diceu + u;
        // Compute the vertex offset numbers for the triangles
        int ndx = 0;
        for (int v = 0; v < dicev - 1; ++v) {
            for (int u = 0; u < diceu - 1; ++u) {
                vertp[ndx++] = VN.apply(u, v);
                vertp[ndx++] = VN.apply(u + 1, v);
                vertp[ndx++] = VN.apply(u + 1, v + 1);

                vertp[ndx++] = VN.apply(u, v);
                vertp[ndx++] = VN.apply(u + 1, v + 1);
                vertp[ndx++] = VN.apply(u, v + 1);
            }
        }
        int nVerts = diceu * dicev;

        return Triangle.CreateTriangleMesh(object2world, world2object, reverseOrientation, nTris,
                vertices, nVerts, evalPs, null,
                evalNs, uvs, null, null);
    }

    private static Point3f NURBSEvaluateSurface(int uOrder, Float[] uKnot, int ucp,
                                        float u, int vOrder, Float[] vKnot,
                                        int vcp, float v, Homogeneous3[] cp,
                                        Vector3f dpdu, Vector3f dpdv) {

        Homogeneous3[] iso = new Homogeneous3[Math.max(uOrder, vOrder)];

        int uOffset = KnotOffset(uKnot, uOrder, ucp, u);
        int uFirstCp = uOffset - uOrder + 1;
        assert(uFirstCp >= 0 && uFirstCp + uOrder - 1 < ucp);

        for (int i = 0; i < uOrder; ++i)
            iso[i] = NURBSEvaluate(vOrder, vKnot, cp,uFirstCp + i, vcp, ucp, v, null);

        int vOffset = KnotOffset(vKnot, vOrder, vcp, v);
        int vFirstCp = vOffset - vOrder + 1;
        assert (vFirstCp >= 0 && vFirstCp + vOrder - 1 < vcp);

        Homogeneous3 P = NURBSEvaluate(uOrder, uKnot, iso, -uFirstCp, ucp, 1, u, dpdu);

        if (dpdv != null) {
            for (int i = 0; i < vOrder; ++i)
                iso[i] = NURBSEvaluate(uOrder, uKnot, cp,(vFirstCp + i) * ucp, ucp, 1, u, null);
            NURBSEvaluate(vOrder, vKnot, iso, -vFirstCp, vcp, 1, v, dpdv);
        }
        return new Point3f(P.x / P.w, P.y / P.w, P.z / P.w);
    }

    private static Homogeneous3 NURBSEvaluate(int order, Float[] knot, Homogeneous3[] cp, int cpi, int np, int cpStride, float t, Vector3f deriv) {
        //    int nKnots = np + order;
        float alpha;

        int knotOffset = KnotOffset(knot, order, np, t);
        int cpOffset = knotOffset - order + 1;
        assert(cpOffset >= 0 && cpOffset < np);

        Homogeneous3[] cpWork = new Homogeneous3[order];
        for (int i = 0; i < order; ++i) cpWork[i] = cp[cpi + (cpOffset + i) * cpStride];

        for (int i = 0; i < order - 2; ++i) {
            for (int j = 0; j < order - 1 - i; ++j) {
                alpha = (knot[knotOffset + 1 + j] - t) / (knot[knotOffset + 1 + j] - knot[knotOffset + j + 2 - order + i]);
                assert (alpha >= 0. && alpha <= 1.);

                cpWork[j].x = cpWork[j].x * alpha + cpWork[j + 1].x * (1 - alpha);
                cpWork[j].y = cpWork[j].y * alpha + cpWork[j + 1].y * (1 - alpha);
                cpWork[j].z = cpWork[j].z * alpha + cpWork[j + 1].z * (1 - alpha);
                cpWork[j].w = cpWork[j].w * alpha + cpWork[j + 1].w * (1 - alpha);
            }
        }
        alpha = (knot[knotOffset + 1] - t) / (knot[knotOffset + 1] - knot[knotOffset + 0]);
        assert (alpha >= 0 && alpha <= 1);

        Homogeneous3 val = new Homogeneous3(cpWork[0].x * alpha + cpWork[1].x * (1 - alpha),
                cpWork[0].y * alpha + cpWork[1].y * (1 - alpha),
                cpWork[0].z * alpha + cpWork[1].z * (1 - alpha),
                cpWork[0].w * alpha + cpWork[1].w * (1 - alpha));

        if (deriv != null) {
            float factor = (order - 1) / (knot[knotOffset + 1] - knot[knotOffset + 0]);
            Homogeneous3 delta = new Homogeneous3((cpWork[1].x - cpWork[0].x) * factor,
                    (cpWork[1].y - cpWork[0].y) * factor,
                    (cpWork[1].z - cpWork[0].z) * factor,
                    (cpWork[1].w - cpWork[0].w) * factor);

            deriv.x = delta.x / val.w - (val.x * delta.w / (val.w * val.w));
            deriv.y = delta.y / val.w - (val.y * delta.w / (val.w * val.w));
            deriv.z = delta.z / val.w - (val.z * delta.w / (val.w * val.w));
        }

        return val;
    }

    private static int KnotOffset(Float[] knot, int order, int np, float t) {
        int firstKnot = order - 1;

        int knotOffset = firstKnot;
        while (t > knot[knotOffset + 1]) ++knotOffset;
        assert(knotOffset < np);  // np == lastKnot
        assert(t >= knot[knotOffset] && t <= knot[knotOffset + 1]);
        return knotOffset;
    }

    private static class Homogeneous3 {
        Homogeneous3() { x = y = z = w = 0; }
        Homogeneous3(float xx, float yy, float zz, float ww) {
            x = xx;
            y = yy;
            z = zz;
            w = ww;
        }

        float x, y, z, w;
    }

}