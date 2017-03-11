/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.shapes;

import groovy.util.GroovyTestCase;
import org.pbrt.core.Point3f;
import org.pbrt.core.Shape;
import org.pbrt.core.Transform;
import org.pbrt.core.Vector3f;

import java.util.ArrayList;

/**
 * Created by rick on 3/10/17.
 */
public class TriangleTest extends GroovyTestCase {
    void setUp() {
        super.setUp()
    }

    void tearDown() {

    }

    public void intersect() {
        Transform o2w = Transform.Translate(new Vector3f(0, 0, 10));
        Transform w2o = Transform.Inverse(o2w);
        int[] indices = {0, 1, 2, 2, 3, 1 };
        Point3f[] points = {new Point3f(-10, 10, 0), new Point3f(10, 10, 0), new Point3f(10, -10, 0), new Point3f(-10, -10, 0)};
        ArrayList<Shape> tris = Triangle.CreateTriangleMesh(o2w, w2o, false, 2,
                indices, points.length, points, null, null, null, null, null);

    }

    public void intersectP() {

    }

}