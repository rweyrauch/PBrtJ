/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.tests

import org.pbrt.core.Bounds3f
import org.pbrt.core.Interaction
import org.pbrt.core.LowDiscrepancy
import org.pbrt.core.MediumInterface
import org.pbrt.core.Normal3f
import org.pbrt.core.Pbrt
import org.pbrt.core.Point2f
import org.pbrt.core.Point3f
import org.pbrt.core.RNG
import org.pbrt.core.Ray
import org.pbrt.core.Sampling
import org.pbrt.core.Shape
import org.pbrt.core.SurfaceInteraction
import org.pbrt.core.Transform
import org.pbrt.core.Vector3f
import org.pbrt.shapes.Cylinder
import org.pbrt.shapes.Disk
import org.pbrt.shapes.Sphere
import org.pbrt.shapes.Triangle

import java.util.function.Supplier

class ShapesTest extends GroovyTestCase {

    private static float pExp(RNG rng, float exp) {
        float logu = Pbrt.Lerp(rng.UniformFloat(), -exp, exp)
        return (float)Math.pow(10, logu)
    }

    private static float pUnif(RNG rng, float range) {
        return Pbrt.Lerp(rng.UniformFloat(), -range, range)
    }

    private static int offset(int nPhi, int t, int p) { return t * nPhi + p }

    void testTriangleWatertight() {
        RNG rng = new RNG(12111)
        int nTheta = 16, nPhi = 16
        assertTrue(nTheta >= 3)
        assertTrue(nPhi >= 4)

        // Make a triangle mesh representing a triangulated sphere (with
        // vertices randomly offset along their normal), centered at the
        // origin.
        int nVertices = nTheta * nPhi
        ArrayList<Point3f> vertices = new ArrayList<>()
        for (int t = 0; t < nTheta; ++t) {
            float theta = Pbrt.Pi * (float)t / (float)(nTheta - 1)
            float cosTheta = (float)Math.cos(theta)
            float sinTheta = (float)Math.sin(theta)
            for (int p = 0; p < nPhi; ++p) {
                float phi = 2 * Pbrt.Pi * (float)p / (float)(nPhi - 1)
                float radius = 1
                // Make sure all of the top and bottom vertices are coincident.
                if (t == 0)
                    vertices.add(new Point3f(0, 0, radius))
                else if (t == nTheta - 1)
                    vertices.add(new Point3f(0, 0, -radius))
                else if (p == nPhi - 1)
                // Close it up exactly at the end
                    vertices.add(vertices[vertices.size() - (nPhi - 1)])
                else {
                    radius += 5 * rng.UniformFloat()
                    vertices.add(new Point3f(0, 0, 0).add(Vector3f.SphericalDirection(sinTheta, cosTheta, phi).scale(radius)))
                }
            }
        }
        assertEquals(nVertices, vertices.size())

        ArrayList<Integer> indices = new ArrayList<>()
        // fan at top
        for (int p = 0; p < nPhi - 1; ++p) {
            indices.add(offset(nPhi, 0, 0))
            indices.add(offset(nPhi, 1, p))
            indices.add(offset(nPhi, 1, p + 1))
        }

        // quads in the middle rows
        for (int t = 1; t < nTheta - 2; ++t) {
            for (int p = 0; p < nPhi - 1; ++p) {
                indices.add(offset(nPhi, t, p))
                indices.add(offset(nPhi, t + 1, p))
                indices.add(offset(nPhi, t + 1, p + 1))

                indices.add(offset(nPhi, t, p))
                indices.add(offset(nPhi, t + 1, p + 1))
                indices.add(offset(nPhi, t, p + 1))
            }
        }

        // fan at bottom
        for (int p = 0; p < nPhi - 1; ++p) {
            indices.add(offset(nPhi, nTheta - 1, 0))
            indices.add(offset(nPhi, nTheta - 2, p))
            indices.add(offset(nPhi, nTheta - 2, p + 1))
        }

        int[] ndx = new int[indices.size()]
        for (int i = 0; i < ndx.length; i++) {
            ndx[i] = indices.get(i)
        }

        Point3f[] vtx = new Point3f[vertices.size()]
        for (int i = 0; i < vtx.length; i++) {
            vtx[i] = vertices.get(i)
        }

        Transform identity = new Transform()
        ArrayList<Shape> tris = Triangle.CreateTriangleMesh(identity, identity, false, (int)(ndx.length / 3), ndx, vtx.length, vtx, null, null, null, null, null)

        for (int i = 0; i < 100000; ++i) {
            RNG rng2 = new RNG(i)
            // Choose a random point in sphere of radius 0.5 around the origin.
            Point2f u = new Point2f()
            u.x = rng2.UniformFloat()
            u.y = rng2.UniformFloat()
            Point3f p = (new Point3f(0, 0, 0)).add((Sampling.UniformSampleSphere(u)).scale(0.5f))

            // Choose a random direction.
            u.x = rng2.UniformFloat()
            u.y = rng2.UniformFloat()
            Ray r = new Ray(p, Sampling.UniformSampleSphere(u))
            int nHits = 0;
            for (Shape tri : tris) {
                def intersect = tri.Intersect(r, false)
                if (intersect != null) ++nHits
            }
            assertTrue(nHits >= 1)

            // Now tougher: shoot directly at a vertex.
            Point3f pVertex = vertices.get(rng2.UniformInt32(vertices.size()))
            r.d = pVertex.subtract(r.o)
            nHits = 0
            for (Shape tri : tris) {
                def intersect = tri.Intersect(r, false)
                if (intersect != null) ++nHits
            }
            assertTrue(nHits >= 1)// << pVertex
        }
    }

    static Triangle GetRandomTriangle(Supplier<Float> value) {
        // Triangle vertices
        Point3f[] v = new Point3f[3]
        for (int j = 0; j < 3; ++j) {
            v[j] = new Point3f()
            for (int k = 0; k < 3; ++k) v[j].set(k, value.get())
        }
        if ((Vector3f.Cross(v[1].subtract(v[0]), v[2].subtract(v[0])).LengthSquared()) < 1e-20f)
        // Don't into trouble with ~degenerate triangles.
            return null

        // Create the corresponding Triangle.
        final Transform identity = new Transform()
        int[] indices = [0, 1, 2]
        ArrayList<Shape> triVec = Triangle.CreateTriangleMesh(identity, identity, false, 1, indices, 3, v,
                        null, null, null, null, null)
        assertEquals(1, triVec.size())
        Triangle tri = (Triangle)(triVec.get(0))
        assertNotNull(tri)
        return tri
    }

    void testTriangleReintersect() {
        for (int i = 0; i < 1000; ++i) {
            RNG rng = new RNG(i)
            Supplier<Float> func = [get: {return pExp(rng, 8.0f)}] as Supplier
            Triangle tri = GetRandomTriangle(func)
            if (tri == null) continue

            // Sample a point on the triangle surface to shoot the ray toward.
            Point2f u = new Point2f()
            u.x = rng.UniformFloat()
            u.y = rng.UniformFloat()
            def sample = tri.Sample(u)
            Interaction pTri = sample.isect
            float pdf = sample.pdf

            // Choose a ray origin.
            Point3f o = new Point3f()
            for (int j = 0; j < 3; ++j) o.set(j, pExp(rng, 8.0f))

            // Intersect the ray with the triangle.
            Ray r = new Ray(o, pTri.p.subtract(o))

            def intersect = tri.Intersect(r, false)
            if (intersect == null)
                // We should almost always find an intersection, but rarely
                // miss, due to round-off error. Just do another go-around in
                // this case.
                continue

            // Now trace a bunch of rays leaving the intersection point.
            for (int j = 0; j < 10000; ++j) {
                // Random direction leaving the intersection point.
                Point2f uu = new Point2f()
                uu.x = rng.UniformFloat()
                uu.y = rng.UniformFloat()
                Vector3f w = Sampling.UniformSampleSphere(uu)
                Ray rOut = intersect.isect.SpawnRay(w)
                assertFalse(tri.IntersectP(rOut, false))

                assertTrue(tri.Intersect(rOut, false) == null)

                // Choose a random point to trace rays to.
                Point3f p2 = new Point3f()
                for (int k = 0; k < 3; ++k) p2.set(k, pExp(rng))
                rOut = intersect.isect.SpawnRayTo(p2)

                assertFalse(tri.IntersectP(rOut, false))
                assertTrue(tri.Intersect(rOut, false) == null)
            }
        }
    }

    // Now make sure that the two computed solid angle values are
    // fairly close.
    // Absolute error for small solid angles, relative for large.
    private static double error(double a, double b) {
        if (Math.abs(a) < 1e-4 || Math.abs(b) < 1e-4)
            return Math.abs(a - b)
        return Math.abs((a - b) / b)
    }

    // Computes the projected solid angle subtended by a series of random
    // triangles both using uniform spherical sampling as well as
    // Triangle::Sample(), in order to verify Triangle::Sample().
    void testTriangleSampling() {
        for (int i = 0; i < 30; ++i) {
            final float range = 10
            RNG rng = new RNG(i)
            Supplier<Float> func = [get: {return pUnif(rng, range)}] as Supplier
            Triangle tri = GetRandomTriangle(func)
            if (tri == null) continue

            // Ensure that the reference point isn't too close to the
            // triangle's surface (which makes the Monte Carlo stuff have more
            // variance, thus requiring more samples).
            Point3f pc = new Point3f(pUnif(rng, range), pUnif(rng, range), pUnif(rng, range))
            pc.set(rng.UniformInt32() % 3, (rng.UniformFloat() > 0.5f) ? (float)(-range - 3) : (float)(range + 3))

            // Compute reference value using Monte Carlo with uniform spherical
            // sampling.
            final int count = 512 * 1024
            int hits = 0
            for (int j = 0; j < count; ++j) {
                Point2f u = new Point2f(LowDiscrepancy.RadicalInverse(0, j), LowDiscrepancy.RadicalInverse(1, j))
                Vector3f w = Sampling.UniformSampleSphere(u)
                if (tri.IntersectP(new Ray(pc, w), false)) ++hits
            }
            double unifEstimate = hits / (double)(count * Sampling.UniformSpherePdf())

            // Now use Triangle::Sample()...
            Interaction ref = new Interaction(pc, new Normal3f(), new Vector3f(), new Vector3f(0, 0, 1), 0, new MediumInterface())
            double triSampleEstimate = 0
            for (int j = 0; j < count; ++j) {
                Point2f u = new Point2f(LowDiscrepancy.RadicalInverse(0, j), LowDiscrepancy.RadicalInverse(1, j))
                def sample = tri.Sample(ref, u)
                float pdf = sample.pdf
                Interaction pTri = sample.isect

                Vector3f wi = Vector3f.Normalize(pTri.p.subtract(pc))
                assertTrue(pdf > 0)
                triSampleEstimate += 1.0 / (count * pdf)
            }

            // Don't compare really small triangles, since uniform sampling
            // doesn't get a good estimate for them.
            if (triSampleEstimate > 1e-3)
                // The error tolerance is fairly large so that we can use a
                // reasonable number of samples.  It has been verified that for
                // larger numbers of Monte Carlo samples, the error continues to
                // tighten.
                assertTrue(error(triSampleEstimate, unifEstimate) < 0.1)
        }
    }

    // Checks the closed-form solid angle computation for triangles against a
    // Monte Carlo estimate of it.
    void testTriangleSolidAngle() {
        for (int i = 0; i < 50; ++i) {
            final float range = 10
            RNG rng = new RNG(100 + i)  // Use different triangles than the Triangle/Sample test.
            Supplier<Float> func = [get: {return pUnif(rng, range)}] as Supplier
            Triangle tri = GetRandomTriangle(func)
            if (tri == null) continue

            // Ensure that the reference point isn't too close to the
            // triangle's surface (which makes the Monte Carlo stuff have more
            // variance, thus requiring more samples).
            Point3f pc = new Point3f(pUnif(rng, range), pUnif(rng, range), pUnif(rng, range))
            pc.set(rng.UniformInt32() % 3, (rng.UniformFloat() > 0.5f) ? (float)(-range - 3) : (float)(range + 3))

            // Compute a reference value using Triangle::Sample()
            final int count = 64 * 1024
            Interaction ref = new Interaction(pc, new Normal3f(), new Vector3f(), new Vector3f(0, 0, 1), 0, new MediumInterface())
            double triSampleEstimate = 0
            for (int j = 0; j < count; ++j) {
                Point2f u = new Point2f(LowDiscrepancy.RadicalInverse(0, j), LowDiscrepancy.RadicalInverse(1, j))
                def sample = tri.Sample(ref, u)
                float pdf = sample.pdf
                Interaction pTri = sample.isect

                assertTrue(pdf > 0)
                triSampleEstimate += 1.0 / (count * pdf)
            }

            // Now compute the subtended solid angle of the triangle in closed
            // form.
            float sphericalArea = tri.SolidAngle(pc, 0)

            assertTrue(error(sphericalArea, triSampleEstimate) < 0.015)
        }
    }

    // Use Quasi Monte Carlo with uniform sphere sampling to esimate the solid
    // angle subtended by the given shape from the given point.
    private static float mcSolidAngle(Point3f p, Shape shape, int nSamples) {
        int nHits = 0
        for (int i = 0; i < nSamples; ++i) {
            Point2f u = new Point2f(LowDiscrepancy.RadicalInverse(0, i), LowDiscrepancy.RadicalInverse(1, i))
            Vector3f w = Sampling.UniformSampleSphere(u)
            if (shape.IntersectP(new Ray(p, w), false)) ++nHits
        }
        return nHits / (Sampling.UniformSpherePdf() * nSamples)
    }

    void testSphereSolidAngle() {
        Transform tr = Transform.Translate(new Vector3f(1, 0.5f, -0.8f)).concatenate(Transform.RotateX(30))
        Transform trInv = Transform.Inverse(tr)
        Sphere sphere = new Sphere(tr, trInv, false, 1, -1, 1, 360)

        // Make sure we get a subtended solid angle of 4pi for a point
        // inside the sphere.
        Point3f pInside = new Point3f(1, 0.9f, -0.8f)
        final int nSamples = 128 * 1024
        assertTrue(Math.abs(mcSolidAngle(pInside, sphere, nSamples) - 4 * Pbrt.Pi) < 0.01f)
        assertTrue(Math.abs(sphere.SolidAngle(pInside, nSamples) - 4 * Pbrt.Pi) < 0.01f)

        // Now try a point outside the sphere
        Point3f p = new Point3f(-0.25f, -1, 0.8f)
        float mcSA = mcSolidAngle(p, sphere, nSamples)
        float sphereSA = sphere.SolidAngle(p, nSamples)
        assertTrue(Math.abs(mcSA - sphereSA) < 0.001f)
    }

    void testCylinderSolidAngle() {
        Transform tr = Transform.Translate(new Vector3f(1, 0.5f, -0.8f)).concatenate(Transform.RotateX(30))
        Transform trInv = Transform.Inverse(tr)
        Cylinder cyl = new Cylinder(tr, trInv, false, 0.25f, -1, 1, 360.0f)

        Point3f p = new Point3f(0.5f, 0.25f, 0.5f)
        final int nSamples = 128 * 1024
        float solidAngle = mcSolidAngle(p, cyl, nSamples)
        assertTrue(Math.abs(solidAngle - cyl.SolidAngle(p, nSamples)) < 0.001f)
    }

    void testDiskSolidAngle() {
        Transform tr = Transform.Translate(new Vector3f(1, 0.5f, -0.8f)).concatenate(Transform.RotateX(30))
        Transform trInv = Transform.Inverse(tr)
        Disk disk = new Disk(tr, trInv, false, 0, 1.25f, 0, 360)

        Point3f p = new Point3f(0.5f, -0.8f, 0.5f)
        final int nSamples = 128 * 1024
        float solidAngle = mcSolidAngle(p, disk, nSamples)
        assertTrue(Math.abs(solidAngle - disk.SolidAngle(p, nSamples)) < 0.001f)
    }

    // Check for incorrect self-intersection: assumes that the shape is convex,
    // such that if the dot product of an outgoing ray and the surface normal
    // at a point is positive, then a ray leaving that point in that direction
    // should never intersect the shape.
    private static void TestReintersectConvex(Shape shape, RNG rng) {
        // Ray origin
        Point3f o = new Point3f(pExp(rng, 8.0f), pExp(rng, 8.0f), pExp(rng, 8.0f))

        // Destination: a random point in the shape's bounding box.
        Bounds3f bbox = shape.WorldBound()
        Point3f t = new Point3f(rng.UniformFloat(), rng.UniformFloat(), rng.UniformFloat())
        Point3f p2 = bbox.Lerp(t)

        // Ray to intersect with the shape.
        Ray r = new Ray(o, p2.subtract(o))
        if (rng.UniformFloat() < 0.5f) r.d = Vector3f.Normalize(r.d)

        // We should usually (but not always) find an intersection.
        def intersect = shape.Intersect(r, false)
        if (intersect == null) return

        SurfaceInteraction isect = intersect.isect

        // Now trace a bunch of rays leaving the intersection point.
        for (int j = 0; j < 10000; ++j) {
            // Random direction leaving the intersection point.
            Point2f u = new Point2f(rng.UniformFloat(), rng.UniformFloat())
            Vector3f w = Sampling.UniformSampleSphere(u)
            // Make sure it's in the same hemisphere as the surface normal.
            w = Normal3f.Faceforward(w, isect.n)
            Ray rOut = isect.SpawnRay(w)
            assertFalse(shape.IntersectP(rOut, false))

            assertNull(shape.Intersect(rOut, false))

            // Choose a random point to trace rays to.
            Point3f p22 = new Point3f(pExp(rng, 8.0f), pExp(rng, 8.0f), pExp(rng, 8.0f))
            // Make sure that the point we're tracing rays toward is in the
            // hemisphere about the intersection point's surface normal.
            w = p22.subtract(isect.p)
            w = Normal3f.Faceforward(w, isect.n)
            p22 = isect.p.add(w)
            rOut = isect.SpawnRayTo(p22)

            assertFalse(shape.IntersectP(rOut, false))
            assertNull(shape.Intersect(rOut, false))
        }
    }

    void testFullSphereReintersect() {
        for (int i = 0; i < 100; ++i) {
            RNG rng = new RNG(i)
            Transform identity = new Transform()
            float radius = pExp(rng, 4.0f)
            float zMin = -radius
            float zMax = radius
            float phiMax = 360
            Sphere sphere = new Sphere(identity, identity, false, radius, zMin, zMax, phiMax)

            TestReintersectConvex(sphere, rng)
        }
    }

    void testParialSphereNormal() {
        for (int i = 0; i < 100; ++i) {
            RNG rng = new RNG(i)
            Transform identity = new Transform()
            float radius = pExp(rng, 4)
            float zMin = (rng.UniformFloat() < 0.5f) ? -radius : Pbrt.Lerp(rng.UniformFloat(), -radius, radius)
            float zMax = (rng.UniformFloat() < 0.5f) ? radius : Pbrt.Lerp(rng.UniformFloat(), -radius, radius)
            float phiMax = (rng.UniformFloat() < 0.5) ? 360.0f : rng.UniformFloat() * 360.0f
            Sphere sphere = new Sphere(identity, identity, false, radius, zMin, zMax, phiMax)

            // Ray origin
            Point3f o = new Point3f(pExp(rng, 8.0f), pExp(rng, 8.0f), pExp(rng, 8.0f))

            // Destination: a random point in the shape's bounding box.
            Bounds3f bbox = sphere.WorldBound()
            Point3f t = new Point3f(rng.UniformFloat(), rng.UniformFloat(), rng.UniformFloat())
            Point3f p2 = bbox.Lerp(t)

            // Ray to intersect with the shape.
            Ray r = new Ray(o, p2.subtract(o))
            if (rng.UniformFloat() < 0.5f) r.d = Vector3f.Normalize(r.d)

            // We should usually (but not always) find an intersection.
            def intersect = sphere.Intersect(r, false)
            if (intersect == null) continue
            SurfaceInteraction isect = intersect.isect

            float dot = Normal3f.Dot(Normal3f.Normalize(isect.n), Vector3f.Normalize(new Vector3f(isect.p)))
            assertEquals(1.0, dot)
        }
    }

    void testPartialSphereReintersect() {
        for (int i = 0; i < 100; ++i) {
            RNG rng = new RNG(i)
            Transform identity = new Transform()
            float radius = pExp(rng, 4)
            float zMin = (rng.UniformFloat() < 0.5f) ? -radius : Pbrt.Lerp(rng.UniformFloat(), -radius, radius)
            float zMax = (rng.UniformFloat() < 0.5f) ? radius : Pbrt.Lerp(rng.UniformFloat(), -radius, radius)
            float phiMax = (rng.UniformFloat() < 0.5f) ? 360.0f : rng.UniformFloat() * 360.0f
            Sphere sphere = new Sphere(identity, identity, false, radius, zMin, zMax, phiMax)

            TestReintersectConvex(sphere, rng)
        }
    }

    void testCylinderReintersect() {
        for (int i = 0; i < 100; ++i) {
            RNG rng = new RNG(i)
            Transform identity = new Transform()
            float radius = pExp(rng, 4)
            float zMin = pExp(rng, 4) * ((rng.UniformFloat() < 0.5f) ? -1 : 1)
            float zMax = pExp(rng, 4) * ((rng.UniformFloat() < 0.5f) ? -1 : 1)
            float phiMax = (rng.UniformFloat() < 0.5f) ? 360.0f : rng.UniformFloat() * 360.0f
            Cylinder cyl = new Cylinder(identity, identity, false, radius, zMin, zMax, phiMax)

            TestReintersectConvex(cyl, rng)
        }
    }

}
