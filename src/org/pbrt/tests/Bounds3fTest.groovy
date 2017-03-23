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
import org.pbrt.core.Point3f

class Bounds3fTest extends GroovyTestCase {

    void testPointDistance() {
        Bounds3f b = new Bounds3f(new Point3f(0, 0, 0), new Point3f(1, 1, 1))

        // Points inside the bounding box or on faces
        assertEquals(0.0f, Bounds3f.Distance(new Point3f(0.5f, 0.5f, 0.5f), b))
        assertEquals(0.0f, Bounds3f.Distance(new Point3f(0, 1, 1), b))
        assertEquals(0.0f, Bounds3f.Distance(new Point3f(0.25f, 0.8f, 1), b))
        assertEquals(0.0f, Bounds3f.Distance(new Point3f(0, 0.25f, 0.8f), b))
        assertEquals(0.0f, Bounds3f.Distance(new Point3f(0.7f, 0, 0.8f), b))

        // Aligned with the plane of one of the faces
        assertEquals(5.0f, Bounds3f.Distance(new Point3f(6, 1, 1), b))
        assertEquals(10.0f, Bounds3f.Distance(new Point3f(0, -10, 1), b))

        // 2 of the dimensions inside the box's extent
        assertEquals(2.0f, Bounds3f.Distance(new Point3f(0.5f, 0.5f, 3), b))
        assertEquals(3.0f, Bounds3f.Distance(new Point3f(0.5f, 0.5f, -3), b))
        assertEquals(2.0f, Bounds3f.Distance(new Point3f(0.5f, 3, 0.5f), b))
        assertEquals(3.0f, Bounds3f.Distance(new Point3f(0.5f, -3, 0.5f), b))
        assertEquals(2.0f, Bounds3f.Distance(new Point3f(3, 0.5f, 0.5f), b))
        assertEquals(3.0f, Bounds3f.Distance(new Point3f(-3, 0.5f, 0.5f), b))

        // General points
        assertEquals(3 * 3 + 7 * 7 + 10 * 10, Bounds3f.DistanceSquared(new Point3f(4, 8, -10), b))
        assertEquals(6 * 6 + 10 * 10 + 7 * 7, Bounds3f.DistanceSquared(new Point3f(-6, -10, 8), b))

        // A few with a more irregular box, just to be sure
        Bounds3f bb = new Bounds3f(new Point3f(-1, -3, 5), new Point3f(2, -2, 18))
        assertEquals(0, Bounds3f.Distance(new Point3f(-0.99f, -2, 5), bb))
        assertEquals(2 * 2 + 6 * 6 + 4 * 4, Bounds3f.DistanceSquared(new Point3f(-3, -9, 22), bb))
    }
}