/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class SurfaceInteraction extends Interaction {

    // SurfaceInteraction Public Data
    public Point2f uv;
    public Vector3f dpdu, dpdv;
    public Normal3f dndu, dndv;
    public Shape shape = null;

    public class Shading {
        public Normal3f n;
        public Vector3f dpdu, dpdv;
        public Normal3f dndu, dndv;
    }
    public Shading shading = new Shading();
    public Primitive primitive = null;
    public BSDF bsdf = null;
    public BSSRDF bssrdf = null;
    public Vector3f dpdx, dpdy;
    public float dudx = 0, dvdx = 0, dudy = 0, dvdy = 0;

    // SurfaceInteraction Public Methods
    public SurfaceInteraction() {
        super();
    }
    public SurfaceInteraction(Point3f p, Vector3f pError,
                        Point2f uv,  Vector3f wo,
                        Vector3f dpdu,  Vector3f dpdv,
                        Normal3f dndu,  Normal3f dndv, float time,
                        Shape sh) {
        super(p, new Normal3f(Vector3f.Normalize(Vector3f.Cross(dpdu, dpdv))), pError, wo, time, null);
        this.uv = uv;
        this.dpdu = dpdu;
        this.dpdv = dpdv;
        this.dndu = dndu;
        this.dndv = dndv;
        this.shape = sh;

        // Initialize shading geometry from true geometry
        shading.n = n;
        shading.dpdu = dpdu;
        shading.dpdv = dpdv;
        shading.dndu = dndu;
        shading.dndv = dndv;

        // Adjust normal based on orientation and handedness
        if (shape != null && (shape.reverseOrientation ^ shape.transformSwapsHandedness)) {
            n.flip();
            shading.n.flip();
        }
    }
    public void SetShadingGeometry(Vector3f dpdus, Vector3f dpdvs, Normal3f dndus, Normal3f dndvs, boolean orientationIsAuthoritative) {
        // Compute _shading.n_ for _SurfaceInteraction_
        shading.n = new Normal3f(Vector3f.Normalize(Vector3f.Cross(dpdus, dpdvs)));
        if (shape != null && (shape.reverseOrientation ^ shape.transformSwapsHandedness))
            shading.n.flip();
        if (orientationIsAuthoritative)
            n = Normal3f.Faceforward(n, shading.n);
        else
            shading.n = Normal3f.Faceforward(shading.n, n);

        // Initialize _shading_ partial derivative values
        shading.dpdu = dpdus;
        shading.dpdv = dpdvs;
        shading.dndu = dndus;
        shading.dndv = dndvs;
    }

    public void ComputeScatteringFunctions(RayDifferential ray) {
        ComputeScatteringFunctions(ray,false, Material.TransportMode.Radiance);
    }
    public void ComputeScatteringFunctions(RayDifferential ray, boolean allowMultipleLobes, Material.TransportMode mode) {
        ComputeDifferentials(ray);
        primitive.ComputeScatteringFunctions(this, mode, allowMultipleLobes);
    }
    public void ComputeDifferentials(RayDifferential ray) {
        if (ray.hasDifferentials) {
            // Estimate screen space change in $\pt{}$ and $(u,v)$

            // Compute auxiliary intersection points with plane
            float d = Normal3f.Dot(n, new Vector3f(p.x, p.y, p.z));
            float tx = -(Normal3f.Dot(n, new Vector3f(ray.rxOrigin)) - d) / Normal3f.Dot(n, ray.rxDirection);
            if (Float.isNaN(tx)) {
                dudx = dvdx = 0;
                dudy = dvdy = 0;
                dpdx = dpdy = new Vector3f(0, 0, 0);
                return;
            }
            Point3f px = ray.rxOrigin.add(ray.rxDirection.scale(tx));
            float ty = -(Normal3f.Dot(n, new Vector3f(ray.ryOrigin)) - d) / Normal3f.Dot(n, ray.ryDirection);
            if (Float.isNaN(ty)) {
                dudx = dvdx = 0;
                dudy = dvdy = 0;
                dpdx = dpdy = new Vector3f(0, 0, 0);
                return;
            }
            Point3f py = ray.ryOrigin.add(ray.ryDirection.scale(ty));
            dpdx = px.subtract(p);
            dpdy = py.subtract(p);

            // Compute $(u,v)$ offsets at auxiliary points

            // Choose two dimensions to use for ray offset computation
            int dim[] = { 0, 0 };
            if (Math.abs(n.x) > Math.abs(n.y) && Math.abs(n.x) > Math.abs(n.z)) {
                dim[0] = 1;
                dim[1] = 2;
            } else if (Math.abs(n.y) > Math.abs(n.z)) {
                dim[0] = 0;
                dim[1] = 2;
            } else {
                dim[0] = 0;
                dim[1] = 1;
            }

            // Initialize _A_, _Bx_, and _By_ matrices for offset computation
            float A[][] = {{dpdu.at(dim[0]), dpdv.at(dim[0])}, {dpdu.at(dim[1]), dpdv.at(dim[1])}};
            float Bx[] = {px.at(dim[0]) - p.at(dim[0]), px.at(dim[1]) - p.at(dim[1])};
            float By[] = {py.at(dim[0]) - p.at(dim[0]), py.at(dim[1]) - p.at(dim[1])};

            Vector2f dx = Matrix4x4.SolveLinearSystem2x2(A, Bx);
            if (dx != null) {
                dudx = dx.x;
                dvdx = dx.y;
            }
            else {
                dudx = dvdx = 0;
            }

            Vector2f dy = Matrix4x4.SolveLinearSystem2x2(A, By);
            if (dy != null) {
                dudy = dy.x;
                dvdy = dy.y;
            }
            else {
                dudy = dvdy = 0;
            }
        }
        else {
            dudx = dvdx = 0;
            dudy = dvdy = 0;
            dpdx = dpdy = new Vector3f(0, 0, 0);
        }
    }
    public Spectrum Le(Vector3f w) {
        AreaLight area = primitive.GetAreaLight();
        return (area != null) ? area.L(this, w) : new Spectrum(0.f);
    }

}