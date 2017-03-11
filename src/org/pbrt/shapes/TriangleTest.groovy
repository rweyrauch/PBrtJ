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
import org.pbrt.core.Shape
import org.pbrt.core.Transform
import org.pbrt.core.Vector3f

/**
 * Created by rick on 3/10/17.
 */
class TriangleTest extends GroovyTestCase {
    void setUp() {
        super.setUp()
    }

    void tearDown() {

    }

    void testIntersect() {
        Transform o2w = Transform.Translate(new Vector3f(0, 0, 10))
        Transform w2o = Transform.Inverse(o2w)
        int[] indices = [0, 1, 2] //, 2, 3, 1 ]
        Point3f[] points = [new Point3f(-10, 10, 0), new Point3f(10, 10, 0), new Point3f(10, -10, 0), new Point3f(-10, -10, 0)]
        ArrayList<Shape> tris = Triangle.CreateTriangleMesh(o2w, w2o, false, 1,
                indices, points.length, points, null, null, null, null, null)

        Ray ray = new Ray(new Point3f(0, 0, -100), new Vector3f(0,0,1))
        for (Shape tri : tris) {
            boolean hit = tri.IntersectP(ray, true)
            assertTrue(hit)
        }
    }

    void testIntersectP() {

    }

}