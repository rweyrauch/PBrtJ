
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
import org.pbrt.textures.ConstantTextureFloat;

import java.util.ArrayList;
import java.util.Map;

public class Triangle extends Shape {

    public Triangle(Transform ObjectToWorld, Transform WorldToObject, boolean reverseOrientation, TriangleMesh mesh, int triNumber) {
        super(ObjectToWorld, WorldToObject, reverseOrientation);
        this.mesh = mesh;
        this.v = new int[]{mesh.vertexIndices[3*triNumber], mesh.vertexIndices[3*triNumber+1], mesh.vertexIndices[3*triNumber+2]};
    }

    public static ArrayList<Shape> Create(Transform object2world, Transform world2object, boolean reverseOrientation, ParamSet paramSet, Map<String, TextureFloat> floatTextures) {
        Integer[] vi = paramSet.FindInt("indices");
        Point3f[] P = paramSet.FindPoint3f("P");
        Point2f[] uvs = paramSet.FindPoint2f("uv");
        if (uvs == null) uvs = paramSet.FindPoint2f("st");
        Point2f[] tempUVs;
        if (uvs == null) {
            Float[] fuv = paramSet.FindFloat("uv");
            if (fuv == null) fuv = paramSet.FindFloat("st");
            if (fuv != null) {
                tempUVs = new Point2f[fuv.length/2];
                for (int i = 0; i < tempUVs.length; ++i)
                    tempUVs[i] = new Point2f(fuv[2 * i], fuv[2 * i + 1]);
                uvs = tempUVs;
            }
        }
        if (uvs != null) {
            if (uvs.length < P.length) {
                PBrtTLogger.Error(
                        "Not enough of \"uv\"s for triangle mesh.  Expected %d, found %d.  Discarding.", P.length, uvs.length);
                uvs = null;
            } else if (uvs.length > P.length) {
                PBrtTLogger.Warning("More \"uv\"s provided than will be used for triangle mesh.  (%d expcted, %d found)", P.length, uvs.length);
            }
        }
        if (vi == null) {
            PBrtTLogger.Error("Vertex indices \"indices\" not provided with triangle mesh shape");
            return new ArrayList<>();
        }
        if (P == null) {
            PBrtTLogger.Error("Vertex positions \"P\" not provided with triangle mesh shape");
            return new ArrayList<>();
        }
        Vector3f[] S = paramSet.FindVector3f("S");
        if ((S != null) && S.length != P.length) {
            PBrtTLogger.Error("Number of \"S\"s for triangle mesh must match \"P\"s");
            S = null;
        }
        Normal3f[] N = paramSet.FindNormal3f("N");
        if ((N != null) && N.length != P.length) {
            PBrtTLogger.Error("Number of \"N\"s for triangle mesh must match \"P\"s");
            N = null;
        }
        for (Integer aVi : vi) {
            if (aVi >= P.length) {
                PBrtTLogger.Error("trianglemesh has out of-bounds vertex index %d (%d \"P\" values were given", aVi, P.length);
                return new ArrayList<>();
            }
        }
        TextureFloat alphaTex = null;
        String alphaTexName = paramSet.FindTexture("alpha");
        if (!alphaTexName.isEmpty()) {
            if (floatTextures.containsKey(alphaTexName)) {
                alphaTex = floatTextures.get(alphaTexName);
            } else {
                PBrtTLogger.Error("Couldn't find float texture \"%s\" for \"alpha\" parameter", alphaTexName);
            }
        } else if (paramSet.FindOneFloat("alpha", 1) == 0) {
            alphaTex = new ConstantTextureFloat(0.0f);
        }

        TextureFloat shadowAlphaTex = null;
        String shadowAlphaTexName = paramSet.FindTexture("shadowalpha");
        if (!shadowAlphaTexName.isEmpty()) {
            if (floatTextures.containsKey(shadowAlphaTexName)) {
                shadowAlphaTex = floatTextures.get(shadowAlphaTexName);
            } else {
                PBrtTLogger.Error("Couldn't find float texture \"%s\" for \"shadowalpha\" parameter", shadowAlphaTexName);
            }
        } else if (paramSet.FindOneFloat("shadowalpha", 1) == 0) {
            shadowAlphaTex = new ConstantTextureFloat(0.0f);
        }

        int[] vii = new int[vi.length];
        for (int i = 0; i < vi.length; i++) vii[i] = vi[i];
        return CreateTriangleMesh(object2world, world2object, reverseOrientation, vi.length / 3, vii, P.length, P,
                S, N, uvs, alphaTex, shadowAlphaTex);
    }

    public static ArrayList<Shape> CreateTriangleMesh(Transform o2w,  Transform w2o, boolean reverseOrientation,
                                                      int nTriangles, int[] vertexIndices, int nVertices, Point3f[] p,
                                                      Vector3f[] s, Normal3f[] n, Point2f[] uv,
                                                      TextureFloat alphaTexture, TextureFloat shadowAlphaTexture) {
        TriangleMesh mesh = new TriangleMesh(o2w, nTriangles, vertexIndices, nVertices, p, s, n, uv, alphaTexture, shadowAlphaTexture);
        ArrayList<Shape> tris = new ArrayList<>(nTriangles);
        for (int i = 0; i < nTriangles; ++i) {
            tris.add(new Triangle(o2w, w2o, reverseOrientation, mesh, i));
        }
        return tris;
    }

    public static boolean WritePlyFile(String filename, int nTriangles, int[] vertexIndices, int nVertices, Point3f[] P,
                                       Vector3f[] S, Normal3f[] N, Point2f[] UV) {
        return false;
    }

    @Override
    public Bounds3f ObjectBound() {
        // Get triangle vertices in _p0_, _p1_, and _p2_
        Point3f p0 = mesh.p[v[0]];
        Point3f p1 = mesh.p[v[1]];
        Point3f p2 = mesh.p[v[2]];
        return Bounds3f.Union(new Bounds3f(WorldToObject.xform(p0), WorldToObject.xform(p1)), WorldToObject.xform(p2));
    }

    @Override
    public Bounds3f WorldBound() {
        // Get triangle vertices in _p0_, _p1_, and _p2_
        Point3f p0 = mesh.p[v[0]];
        Point3f p1 = mesh.p[v[1]];
        Point3f p2 = mesh.p[v[2]];
        return Bounds3f.Union(new Bounds3f(p0, p1), p2);
    }

    @Override
    public HitResult Intersect(Ray ray, boolean testAlphaTexture) {
        interPerRayTri.incrementDenom(1); //++nTests;

        // Get triangle vertices in _p0_, _p1_, and _p2_
        Point3f p0 = mesh.p[v[0]];
        Point3f p1 = mesh.p[v[1]];
        Point3f p2 = mesh.p[v[2]];

        // Perform ray--triangle intersection test

        // Transform triangle vertices to ray coordinate space

        // Translate vertices based on ray origin
        Point3f p0t = p0.subtract(new Vector3f(ray.o));
        Point3f p1t = p1.subtract(new Vector3f(ray.o));
        Point3f p2t = p2.subtract(new Vector3f(ray.o));

        // Permute components of triangle vertices and ray direction
        int kz = Vector3f.MaxDimension(Vector3f.Abs(ray.d));
        int kx = kz + 1;
        if (kx == 3) kx = 0;
        int ky = kx + 1;
        if (ky == 3) ky = 0;
        Vector3f d = Vector3f.Permute(ray.d, kx, ky, kz);
        p0t = Point3f.Permute(p0t, kx, ky, kz);
        p1t = Point3f.Permute(p1t, kx, ky, kz);
        p2t = Point3f.Permute(p2t, kx, ky, kz);

        // Apply shear transformation to translated vertex positions
        float Sx = -d.x / d.z;
        float Sy = -d.y / d.z;
        float Sz = 1.f / d.z;
        p0t.x += Sx * p0t.z;
        p0t.y += Sy * p0t.z;
        p1t.x += Sx * p1t.z;
        p1t.y += Sy * p1t.z;
        p2t.x += Sx * p2t.z;
        p2t.y += Sy * p2t.z;

        // Compute edge function coefficients _e0_, _e1_, and _e2_
        float e0 = p1t.x * p2t.y - p1t.y * p2t.x;
        float e1 = p2t.x * p0t.y - p2t.y * p0t.x;
        float e2 = p0t.x * p1t.y - p0t.y * p1t.x;

        // Fall back to double precision test at triangle edges
        if ((e0 == 0.0f || e1 == 0.0f || e2 == 0.0f)) {
            double p2txp1ty = (double)p2t.x * (double)p1t.y;
            double p2typ1tx = (double)p2t.y * (double)p1t.x;
            e0 = (float)(p2typ1tx - p2txp1ty);
            double p0txp2ty = (double)p0t.x * (double)p2t.y;
            double p0typ2tx = (double)p0t.y * (double)p2t.x;
            e1 = (float)(p0typ2tx - p0txp2ty);
            double p1txp0ty = (double)p1t.x * (double)p0t.y;
            double p1typ0tx = (double)p1t.y * (double)p0t.x;
            e2 = (float)(p1typ0tx - p1txp0ty);
        }

        // Perform triangle edge and determinant tests
        if ((e0 < 0 || e1 < 0 || e2 < 0) && (e0 > 0 || e1 > 0 || e2 > 0))
            return null;
        float det = e0 + e1 + e2;
        if (det == 0) return null;

        // Compute scaled hit distance to triangle and test against ray $t$ range
        p0t.z *= Sz;
        p1t.z *= Sz;
        p2t.z *= Sz;
        float tScaled = e0 * p0t.z + e1 * p1t.z + e2 * p2t.z;
        if (det < 0 && (tScaled >= 0 || tScaled < ray.tMax * det))
            return null;
        else if (det > 0 && (tScaled <= 0 || tScaled > ray.tMax * det))
            return null;

        // Compute barycentric coordinates and $t$ value for triangle intersection
        float invDet = 1 / det;
        float b0 = e0 * invDet;
        float b1 = e1 * invDet;
        float b2 = e2 * invDet;
        float t = tScaled * invDet;

        // Ensure that computed triangle $t$ is conservatively greater than zero

        // Compute $\delta_z$ term for triangle $t$ error bounds
        float maxZt = Vector3f.MaxComponent(Vector3f.Abs(new Vector3f(p0t.z, p1t.z, p2t.z)));
        float deltaZ = Pbrt.gamma(3) * maxZt;

        // Compute $\delta_x$ and $\delta_y$ terms for triangle $t$ error bounds
        float maxXt = Vector3f.MaxComponent(Vector3f.Abs(new Vector3f(p0t.x, p1t.x, p2t.x)));
        float maxYt = Vector3f.MaxComponent(Vector3f.Abs(new Vector3f(p0t.y, p1t.y, p2t.y)));
        float deltaX = Pbrt.gamma(5) * (maxXt + maxZt);
        float deltaY = Pbrt.gamma(5) * (maxYt + maxZt);

        // Compute $\delta_e$ term for triangle $t$ error bounds
        float deltaE = 2 * (Pbrt.gamma(2) * maxXt * maxYt + deltaY * maxXt + deltaX * maxYt);

        // Compute $\delta_t$ term for triangle $t$ error bounds and check _t_
        float maxE = Vector3f.MaxComponent(Vector3f.Abs(new Vector3f(e0, e1, e2)));
        float deltaT = 3 * (Pbrt.gamma(3) * maxE * maxZt + deltaE * maxZt + deltaZ * maxE) * Math.abs(invDet);
        if (t <= deltaT) return null;

        // Compute triangle partial derivatives
        Vector3f dpdu = null, dpdv = null;
        Point2f[] uv = GetUVs();

        // Compute deltas for triangle partial derivatives
        Vector2f duv02 = uv[0].subtract(uv[2]), duv12 = uv[1].subtract(uv[2]);
        Vector3f dp02 = p0.subtract(p2), dp12 = p1.subtract(p2);
        float determinant = duv02.x * duv12.y - duv02.y * duv12.x;
        boolean degenerateUV = Math.abs(determinant) < 1e-8;
        if (!degenerateUV) {
            float invdet = 1 / determinant;
            dpdu = (dp02.scale(duv12.y).add(dp12.scale(-duv02.y))).scale(invdet);
            dpdv = (dp02.scale(-duv12.x).add(dp12.scale(duv02.x))).scale(invdet);
        }
        if (degenerateUV || Vector3f.Cross(dpdu, dpdv).LengthSquared() == 0) {
            // Handle zero determinant for triangle partial derivative matrix
            Vector3f.CoordSystem coordSystem = Vector3f.CoordinateSystem(Vector3f.Normalize(Vector3f.Cross(p2.subtract(p0), p1.subtract(p0))));
            dpdu = coordSystem.v2;
            dpdv = coordSystem.v3;
        }

        // Compute error bounds for triangle intersection
        float xAbsSum = (Math.abs(b0 * p0.x) + Math.abs(b1 * p1.x) + Math.abs(b2 * p2.x));
        float yAbsSum = (Math.abs(b0 * p0.y) + Math.abs(b1 * p1.y) + Math.abs(b2 * p2.y));
        float zAbsSum = (Math.abs(b0 * p0.z) + Math.abs(b1 * p1.z) + Math.abs(b2 * p2.z));
        Vector3f pError = (new Vector3f(xAbsSum, yAbsSum, zAbsSum)).scale(Pbrt.gamma(7));

        // Interpolate $(u,v)$ parametric coordinates and hit point
        Point3f pHit = p0.scale(b0).add(p1.scale(b1).add(p2.scale(b2)));
        Point2f uvHit = uv[0].scale(b0).add(uv[1].scale(b1).add(uv[2].scale(b2)));

        // Test intersection against alpha texture, if present
        if (testAlphaTexture && mesh.alphaMask != null) {
            SurfaceInteraction isectLocal = new SurfaceInteraction(pHit, new Vector3f(0, 0, 0), uvHit, ray.d.negate(),
                    dpdu, dpdv, new Normal3f(0, 0, 0), new Normal3f(0, 0, 0), ray.time, this);
            if (mesh.alphaMask.Evaluate(isectLocal) == 0) return null;
        }

        // Fill in _SurfaceInteraction_ from triangle hit
        HitResult hr = new HitResult();
        hr.isect = new SurfaceInteraction(pHit, pError, uvHit, ray.d.negate(), dpdu, dpdv,
                new Normal3f(0, 0, 0), new Normal3f(0, 0, 0), ray.time, this);

        // Override surface normal in _isect_ for triangle
        hr.isect.n = hr.isect.shading.n = new Normal3f(Vector3f.Normalize(Vector3f.Cross(dp02, dp12)));
        if (mesh.n != null || mesh.s != null) {
            // Initialize _Triangle_ shading geometry

            // Compute shading normal _ns_ for triangle
            Normal3f ns;
            if (mesh.n != null) {
                ns = (mesh.n[v[0]].scale(b0).add(mesh.n[v[1]].scale(b1).add(mesh.n[v[2]].scale(b2))));
                if (ns.LengthSquared() > 0)
                    ns = Normal3f.Normalize(ns);
                else
                    ns = hr.isect.n;
            } else
                ns = hr.isect.n;

            // Compute shading tangent _ss_ for triangle
            Vector3f ss;
            if (mesh.s != null) {
                ss = (mesh.s[v[0]].scale(b0).add(mesh.s[v[1]].scale(b1).add(mesh.s[v[2]].scale(b2))));
                if (ss.LengthSquared() > 0)
                    ss = Vector3f.Normalize(ss);
                else
                    ss = Vector3f.Normalize(hr.isect.dpdu);
            } else
                ss = Vector3f.Normalize(hr.isect.dpdu);

            // Compute shading bitangent _ts_ for triangle and adjust _ss_
            Vector3f ts = Vector3f.Cross(ss, ns);
            if (ts.LengthSquared() > 0) {
                ts = Vector3f.Normalize(ts);
                ss = Vector3f.Cross(ts, ns);
            } else {
                Vector3f.CoordSystem cs = Vector3f.CoordinateSystem(new Vector3f(ns));
                ss = cs.v2;
                ts = cs.v3;
            }
            // Compute $\dndu$ and $\dndv$ for triangle shading geometry
            Normal3f dndu, dndv;
            if (mesh.n != null) {
                // Compute deltas for triangle partial derivatives of normal
                duv02 = uv[0].subtract(uv[2]);
                duv12 = uv[1].subtract(uv[2]);
                Normal3f dn1 = mesh.n[v[0]].subtract(mesh.n[v[2]]);
                Normal3f dn2 = mesh.n[v[1]].subtract(mesh.n[v[2]]);
                determinant = duv02.x * duv12.y - duv02.y * duv12.x;
                degenerateUV = Math.abs(determinant) < 1e-8;
                if (degenerateUV)
                    dndu = dndv = new Normal3f(0, 0, 0);
                else {
                    invDet = 1 / determinant;
                    dndu = (dn1.scale(duv12.y).add(dn2.scale(-duv02.y))).scale(invDet);
                    dndv = (dn1.scale(-duv12.x).add(dn2.scale(duv02.x))).scale(invDet);
                }
            } else
                dndu = dndv = new Normal3f(0, 0, 0);
            hr.isect.SetShadingGeometry(ss, ts, dndu, dndv, true);
        }

        // Ensure correct orientation of the geometric normal
        if (mesh.n != null)
            hr.isect.n = Normal3f.Faceforward(hr.isect.n, hr.isect.shading.n);
        else if (reverseOrientation ^ transformSwapsHandedness) {
            hr.isect.shading.n = hr.isect.n = hr.isect.n.negate();
        }
        hr.tHit = t;
        interPerRayTri.incrementNumer(1); //++nHits;
        return hr;
    }

    @Override
    public boolean IntersectP(Ray ray, boolean testAlphaTexture) {
        interPerRayTri.incrementDenom(1); //++nTests;

        // Get triangle vertices in _p0_, _p1_, and _p2_
        Point3f p0 = mesh.p[v[0]];
        Point3f p1 = mesh.p[v[1]];
        Point3f p2 = mesh.p[v[2]];

        // Perform ray--triangle intersection test

        // Transform triangle vertices to ray coordinate space

        // Translate vertices based on ray origin
        Point3f p0t = p0.subtract(new Vector3f(ray.o));
        Point3f p1t = p1.subtract(new Vector3f(ray.o));
        Point3f p2t = p2.subtract(new Vector3f(ray.o));

        // Permute components of triangle vertices and ray direction
        int kz = Vector3f.MaxDimension(Vector3f.Abs(ray.d));
        int kx = kz + 1;
        if (kx == 3) kx = 0;
        int ky = kx + 1;
        if (ky == 3) ky = 0;
        Vector3f d = Vector3f.Permute(ray.d, kx, ky, kz);
        p0t = Point3f.Permute(p0t, kx, ky, kz);
        p1t = Point3f.Permute(p1t, kx, ky, kz);
        p2t = Point3f.Permute(p2t, kx, ky, kz);

        // Apply shear transformation to translated vertex positions
        float Sx = -d.x / d.z;
        float Sy = -d.y / d.z;
        float Sz = 1.f / d.z;
        p0t.x += Sx * p0t.z;
        p0t.y += Sy * p0t.z;
        p1t.x += Sx * p1t.z;
        p1t.y += Sy * p1t.z;
        p2t.x += Sx * p2t.z;
        p2t.y += Sy * p2t.z;

        // Compute edge function coefficients _e0_, _e1_, and _e2_
        float e0 = p1t.x * p2t.y - p1t.y * p2t.x;
        float e1 = p2t.x * p0t.y - p2t.y * p0t.x;
        float e2 = p0t.x * p1t.y - p0t.y * p1t.x;

        // Fall back to double precision test at triangle edges
        if ((e0 == 0.0f || e1 == 0.0f || e2 == 0.0f)) {
            double p2txp1ty = (double)p2t.x * (double)p1t.y;
            double p2typ1tx = (double)p2t.y * (double)p1t.x;
            e0 = (float)(p2typ1tx - p2txp1ty);
            double p0txp2ty = (double)p0t.x * (double)p2t.y;
            double p0typ2tx = (double)p0t.y * (double)p2t.x;
            e1 = (float)(p0typ2tx - p0txp2ty);
            double p1txp0ty = (double)p1t.x * (double)p0t.y;
            double p1typ0tx = (double)p1t.y * (double)p0t.x;
            e2 = (float)(p1typ0tx - p1txp0ty);
        }

        // Perform triangle edge and determinant tests
        if ((e0 < 0 || e1 < 0 || e2 < 0) && (e0 > 0 || e1 > 0 || e2 > 0))
            return false;
        float det = e0 + e1 + e2;
        if (det == 0) return false;

        // Compute scaled hit distance to triangle and test against ray $t$ range
        p0t.z *= Sz;
        p1t.z *= Sz;
        p2t.z *= Sz;
        float tScaled = e0 * p0t.z + e1 * p1t.z + e2 * p2t.z;
        if (det < 0 && (tScaled >= 0 || tScaled < ray.tMax * det))
            return false;
        else if (det > 0 && (tScaled <= 0 || tScaled > ray.tMax * det))
            return false;

        // Compute barycentric coordinates and $t$ value for triangle intersection
        float invDet = 1 / det;
        float b0 = e0 * invDet;
        float b1 = e1 * invDet;
        float b2 = e2 * invDet;
        float t = tScaled * invDet;

        // Ensure that computed triangle $t$ is conservatively greater than zero

        // Compute $\delta_z$ term for triangle $t$ error bounds
        float maxZt = Vector3f.MaxComponent(Vector3f.Abs(new Vector3f(p0t.z, p1t.z, p2t.z)));
        float deltaZ = Pbrt.gamma(3) * maxZt;

        // Compute $\delta_x$ and $\delta_y$ terms for triangle $t$ error bounds
        float maxXt = Vector3f.MaxComponent(Vector3f.Abs(new Vector3f(p0t.x, p1t.x, p2t.x)));
        float maxYt = Vector3f.MaxComponent(Vector3f.Abs(new Vector3f(p0t.y, p1t.y, p2t.y)));
        float deltaX = Pbrt.gamma(5) * (maxXt + maxZt);
        float deltaY = Pbrt.gamma(5) * (maxYt + maxZt);

        // Compute $\delta_e$ term for triangle $t$ error bounds
        float deltaE = 2 * (Pbrt.gamma(2) * maxXt * maxYt + deltaY * maxXt + deltaX * maxYt);

        // Compute $\delta_t$ term for triangle $t$ error bounds and check _t_
        float maxE = Vector3f.MaxComponent(Vector3f.Abs(new Vector3f(e0, e1, e2)));
        float deltaT = 3 * (Pbrt.gamma(3) * maxE * maxZt + deltaE * maxZt + deltaZ * maxE) * Math.abs(invDet);
        if (t <= deltaT) return false;

        // Test shadow ray intersection against alpha texture, if present
        if (testAlphaTexture && (mesh.alphaMask != null || mesh.shadowAlphaMask != null)) {
            // Compute triangle partial derivatives
            Vector3f dpdu = null, dpdv = null;
            Point2f[] uv = GetUVs();

            // Compute deltas for triangle partial derivatives
            Vector2f duv02 = uv[0].subtract(uv[2]), duv12 = uv[1].subtract(uv[2]);
            Vector3f dp02 = p0.subtract(p2), dp12 = p1.subtract(p2);
            float determinant = duv02.x * duv12.y - duv02.y * duv12.x;
            boolean degenerateUV = Math.abs(determinant) < 1e-8;
            if (!degenerateUV) {
                float invdet = 1 / determinant;
                dpdu = (dp02.scale(duv12.y).add(dp12.scale(-duv02.y))).scale(invdet);
                dpdv = (dp02.scale(-duv12.x).add(dp12.scale(duv02.x))).scale(invdet);
            }
            if (degenerateUV || Vector3f.Cross(dpdu, dpdv).LengthSquared() == 0) {
                // Handle zero determinant for triangle partial derivative matrix
                Vector3f.CoordSystem coordSystem = Vector3f.CoordinateSystem(Vector3f.Normalize(Vector3f.Cross(p2.subtract(p0), p1.subtract(p0))));
                dpdu = coordSystem.v2;
                dpdv = coordSystem.v3;
            }

            // Interpolate $(u,v)$ parametric coordinates and hit point
            Point3f pHit = p0.scale(b0).add(p1.scale(b1).add(p2.scale(b2)));
            Point2f uvHit = uv[0].scale(b0).add(uv[1].scale(b1).add(uv[2].scale(b2)));
            SurfaceInteraction isectLocal = new SurfaceInteraction(pHit, new Vector3f(0, 0, 0), uvHit, ray.d.negate(),
                    dpdu, dpdv, new Normal3f(0, 0, 0), new Normal3f(0, 0, 0), ray.time, this);
            if (mesh.alphaMask != null && mesh.alphaMask.Evaluate(isectLocal) == 0)
                return false;
            if (mesh.shadowAlphaMask != null && mesh.shadowAlphaMask.Evaluate(isectLocal) == 0)
                return false;
        }
        interPerRayTri.incrementNumer(1); //++nHits;
        return true;
    }

    @Override
    public float Area() {
        // Get triangle vertices in _p0_, _p1_, and _p2_
        Point3f p0 = mesh.p[v[0]];
        Point3f p1 = mesh.p[v[1]];
        Point3f p2 = mesh.p[v[2]];
        return 0.5f * Vector3f.Cross(p1.subtract(p0), p2.subtract(p0)).Length();
    }

    @Override
    public SampleResult Sample(Point2f u) {
        Point2f b = Sampling.UniformSampleTriangle(u);
        // Get triangle vertices in _p0_, _p1_, and _p2_
        Point3f p0 = mesh.p[v[0]];
        Point3f p1 = mesh.p[v[1]];
        Point3f p2 = mesh.p[v[2]];
        SurfaceInteraction it = new SurfaceInteraction();
        it.p = p0.scale(b.x).add(p1.scale(b.y).add(p2.scale(1 - b.x - b.y)));
        // Compute surface normal for sampled point on triangle
        it.n = Normal3f.Normalize(new Normal3f(Vector3f.Cross(p1.subtract(p0), p2.subtract(p0))));
        // Ensure correct orientation of the geometric normal; follow the same
        // approach as was used in Triangle::Intersect().
        if (mesh.n != null) {
            Normal3f ns = mesh.n[v[0]].scale(b.x).add(mesh.n[v[1]].scale(b.y).add(mesh.n[v[2]].scale(1 - b.x - b.y)));
            it.n = Normal3f.Faceforward(it.n, ns);
        } else if (reverseOrientation ^ transformSwapsHandedness)
            it.n = it.n.negate();

        // Compute error bounds for sampled point on triangle
        Point3f pAbsSum = Point3f.Abs(p0.scale(b.x)).add(Point3f.Abs(p1.scale(b.y)).add(Point3f.Abs(p2.scale(1 - b.x - b.y))));
        it.pError = (new Vector3f(pAbsSum.x, pAbsSum.y, pAbsSum.z)).scale(Pbrt.gamma(6));
        SampleResult result = new SampleResult();
        result.isect = it;
        result.pdf = 1 / Area();
        return result;
    }

    // Returns the solid angle subtended by the triangle w.r.t. the given
    // reference point p.
    public float SolidAngle(Point3f p, int nSamples) {
        // Project the vertices into the unit sphere around p.
        Vector3f[] pSphere = { Vector3f.Normalize(mesh.p[v[0]].subtract(p)),
                Vector3f.Normalize(mesh.p[v[1]].subtract(p)),
                Vector3f.Normalize(mesh.p[v[2]].subtract(p))};

        // http://math.stackexchange.com/questions/9819/area-of-a-spherical-triangle
        // Girard's theorem: surface area of a spherical triangle on a unit
        // sphere is the 'excess angle' alpha+beta+gamma-pi, where
        // alpha/beta/gamma are the interior angles at the vertices.
        //
        // Given three vertices on the sphere, a, b, c, then we can compute,
        // for example, the angle c->a->b by
        //
        // cos theta =  Dot(Cross(c, a), Cross(b, a)) /
        //              (Length(Cross(c, a)) * Length(Cross(b, a))).
        //
        Vector3f cross01 = Vector3f.Cross(pSphere[0], pSphere[1]);
        Vector3f cross12 = Vector3f.Cross(pSphere[1], pSphere[2]);
        Vector3f cross20 = Vector3f.Cross(pSphere[2], pSphere[0]);

        // Some of these vectors may be degenerate. In this case, we don't want
        // to normalize them so that we don't hit an assert. This is fine,
        // since the corresponding dot products below will be zero.
        if (cross01.LengthSquared() > 0) cross01 = Vector3f.Normalize(cross01);
        if (cross12.LengthSquared() > 0) cross12 = Vector3f.Normalize(cross12);
        if (cross20.LengthSquared() > 0) cross20 = Vector3f.Normalize(cross20);

        // We only need to do three cross products to evaluate the angles at
        // all three vertices, though, since we can take advantage of the fact
        // that Cross(a, b) = -Cross(b, a).
        return (float)Math.abs(Math.acos(Pbrt.Clamp(Vector3f.Dot(cross01, cross12.negate()), -1, 1)) +
                Math.acos(Pbrt.Clamp(Vector3f.Dot(cross12, cross20.negate()), -1, 1)) +
                Math.acos(Pbrt.Clamp(Vector3f.Dot(cross20, cross01.negate()), -1, 1)) - Math.PI);
    }

    private Point2f[] GetUVs() {
        Point2f[] uv = new Point2f[3];
        if (mesh.uv != null) {
            uv[0] = mesh.uv[v[0]];
            uv[1] = mesh.uv[v[1]];
            uv[2] = mesh.uv[v[2]];
        } else {
            uv[0] = new Point2f(0, 0);
            uv[1] = new Point2f(1, 0);
            uv[2] = new Point2f(1, 1);
        }
        return uv;
    }

    public static class TriangleMesh {

        public TriangleMesh(Transform ObjectToWorld, int nTriangles, int[] vertexIndices, int nVertices, Point3f[] P,
                     Vector3f[] S, Normal3f[] N, Point2f[] UV, TextureFloat alphaMask, TextureFloat shadowAlphaMask) {
            this.nTriangles = nTriangles;
            this.nVertices = nVertices;
            this.vertexIndices = vertexIndices;
            this.alphaMask = alphaMask;
            this.shadowAlphaMask = shadowAlphaMask;

            trisPerMesh.incrementDenom(1); // ++nMeshes;
            trisPerMesh.incrementNumer(nTriangles); //nTris += nTriangles;

            //triMeshBytes.increment(ObjectSizeCalculator.getObjectSize(this) + (3 * nTriangles * ObjectSizeCalculator.getObjectSize(new Integer(0))) +
            //        nVertices * (ObjectSizeCalculator.getObjectSize(P) + ((N != null) ? ObjectSizeCalculator.getObjectSize(N) : 0) +
            //        ((S != null) ? ObjectSizeCalculator.getObjectSize(S) : 0) + ((UV != null) ? ObjectSizeCalculator.getObjectSize(UV) : 0)));

            // Transform mesh vertices to world space
            this.p = new Point3f[nVertices];
            for (int i = 0; i < nVertices; ++i) p[i] = ObjectToWorld.xform(P[i]);

            // Copy _UV_, _N_, and _S_ vertex data, if present
            if (UV != null) {
                this.uv = UV.clone();
            }
            if (N != null) {
                this.n = new Normal3f[nVertices];
                for (int i = 0; i < nVertices; ++i) n[i] = ObjectToWorld.xform(N[i]);
            }
            if (S != null) {
                this.s = new Vector3f[nVertices];
                for (int i = 0; i < nVertices; ++i) s[i] = ObjectToWorld.xform(S[i]);
            }
        }

        public int nTriangles, nVertices;
        public int[] vertexIndices;
        public Point3f[] p;
        public Normal3f[] n;
        public Vector3f[] s;
        public Point2f[] uv;
        public TextureFloat alphaMask, shadowAlphaMask;
    }

    private TriangleMesh mesh;
    private int[] v;

    private static Stats.Percent interPerRayTri = new Stats.Percent("Intersections/Ray-triangle intersection tests"); // nHits per nTests
    private static Stats.Ratio trisPerMesh = new Stats.Ratio("Scene/Triangles per triangle mesh"); // nTris per nMeshes
    private static Stats.MemoryCounter triMeshBytes = new Stats.MemoryCounter("Memory/Triangle meshes");
}