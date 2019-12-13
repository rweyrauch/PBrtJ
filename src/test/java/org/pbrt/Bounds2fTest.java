/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */
package org.pbrt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.pbrt.core.Bounds2f;
import org.pbrt.core.Point2f;

public class Bounds2fTest {

    private static final float epsilon = 1.0e-3f;

    @Test
    public void testUnion() {
        Bounds2f a = new Bounds2f(new Point2f(-10, -10), new Point2f(0, 20));
        Bounds2f b = new Bounds2f(); // degenerate
        Bounds2f c = Bounds2f.Union(a, b);
        assertTrue(a.equal(c));

        final var bb = Bounds2f.Union(b, b);
        assertTrue(b.equal(Bounds2f.Union(b, b)));
    
        Bounds2f d = new Bounds2f(new Point2f(-15, 10));
        Bounds2f e = Bounds2f.Union(a, d);
        assertTrue(e.equal(new Bounds2f(new Point2f(-15, -10), new Point2f(0, 20))));
    }
 }
