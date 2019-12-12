/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.shapes

import org.pbrt.core.Point3f
import org.pbrt.core.Ray
import org.pbrt.core.SurfaceInteraction
import org.pbrt.core.Transform
import org.pbrt.core.Vector3f

/**
 * Created by rick on 3/9/17.
 */
class SphereTest extends GroovyTestCase {
    void setUp() {
        super.setUp()
    }

    void tearDown() {

    }

    void testCreate() {

    }

    void testObjectBound() {

    }

    void testIntersect() {
        Transform WorldToObject = Transform.RotateX(90).concatenate(Transform.Translate(new Vector3f(0, 0, -10)))
        Transform ObjectToWorld = Transform.Inverse(WorldToObject)
        Sphere sphere = new Sphere(ObjectToWorld, WorldToObject, false, 4, -2, 2, 360)

        Ray ray = new Ray(new Point3f(0, 0, -100), new Vector3f(0,0,1))
        float[] tHit = new float[1]
        SurfaceInteraction isect = new SurfaceInteraction()
        boolean hit = sphere.Intersect(ray, tHit, isect, true)
        assertTrue(hit)
    }

    void testIntersectP() {
        Transform WorldToObject = Transform.RotateX(90).concatenate(Transform.Translate(new Vector3f(0, 0, -10)))
        Transform ObjectToWorld = Transform.Inverse(WorldToObject)
        Sphere sphere = new Sphere(ObjectToWorld, WorldToObject, false, 4, -2, 2, 360)

        Ray ray = new Ray(new Point3f(0, 0, -100), new Vector3f(0,0,1))
        boolean hit = sphere.IntersectP(ray, true)
        assertTrue(hit)
    }

    void testArea() {

    }

    void testPdf() {

    }

    void testSample() {

    }

    void testSample1() {

    }

    void testSolidAngle() {

    }
}
