/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt;

import org.pbrt.core.Point3f;
import org.pbrt.core.Ray;
import org.pbrt.core.SurfaceInteraction;
import org.pbrt.core.Transform;
import org.pbrt.core.Vector3f;
import org.pbrt.shapes.Sphere;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class SphereTest {

    @Test 
    public void testCreate() {

    }

    @Test
    public void testObjectBound() {

    }

    @Test
    public void testIntersect() {
        Transform WorldToObject = Transform.RotateX(90).concatenate(Transform.Translate(new Vector3f(0, 0, -10)));
        Transform ObjectToWorld = Transform.Inverse(WorldToObject);
        Sphere sphere = new Sphere(ObjectToWorld, WorldToObject, false, 4, -2, 2, 360);

        Ray ray = new Ray(new Point3f(0, 0, -100), new Vector3f(0,0,1));
        var hit = sphere.Intersect(ray, true);
        assertTrue(hit != null);
    }

    @Test
    public void testIntersectP() {
        Transform WorldToObject = Transform.RotateX(90).concatenate(Transform.Translate(new Vector3f(0, 0, -10)));
        Transform ObjectToWorld = Transform.Inverse(WorldToObject);
        Sphere sphere = new Sphere(ObjectToWorld, WorldToObject, false, 4, -2, 2, 360);

        Ray ray = new Ray(new Point3f(0, 0, -100), new Vector3f(0,0,1));
        boolean hit = sphere.IntersectP(ray, true);
        assertTrue(hit);
    }

    @Test
    public void testArea() {

    }

    @Test
    public void testPdf() {

    }

    @Test
    public void testSample() {

    }

    @Test
    public void testSample1() {

    }

    @Test
    public void testSolidAngle() {

    }
}
