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

import org.junit.Test;

import org.pbrt.core.Pbrt;


public class BitopsTest {

    @Test
    public void testLog2() {
        for (int i = 0; i < 31; ++i) {
            final int ui = 1 << i;
            assertEquals(i, Pbrt.Log2Int(ui));
            assertEquals((long)i, Pbrt.Log2Int((long)ui));
        }
    
        for (int i = 1; i < 31; ++i) {
            final int ui = 1 << i;
            assertEquals(i, Pbrt.Log2Int(ui + 1));
            assertEquals((long)i, Pbrt.Log2Int((long)(ui + 1)));
        }
    
        for (long i = 0; i < 63; ++i) {
            final long ui = 1L << i;
            assertEquals(i, Pbrt.Log2Int(ui));
        }
    
        for (long i = 1; i < 63; ++i) {
            final long ui = 1L << i;
            assertEquals(i, Pbrt.Log2Int(ui + 1));
        }    
        
    }

    @Test
    public void testPow2() {
        for (int i = 0; i < 31; ++i) {
            final int ui = 1 << i;
            assertEquals(true, Pbrt.IsPowerOf2(ui));
            if (ui > 1) {
                assertEquals(false, Pbrt.IsPowerOf2(ui + 1));
            }
            if (ui > 2) {
                assertEquals(false, Pbrt.IsPowerOf2(ui - 1));
            }
        }    
    }

    @Test
    public void testCountTrailing() {
        for (int i = 0; i < 32; ++i) {
            final int ui = 1 << i;
            assertEquals(i, Pbrt.CountTrailingZeros(ui));
        }    
    }

    @Test
    public void testRoundUpPow2() {
        assertEquals(Pbrt.RoundUpPow2(7), 8);
        for (int i = 1; i < (1 << 24); ++i) {
            if (Pbrt.IsPowerOf2(i)) {
                assertEquals(Pbrt.RoundUpPow2(i), i);
            } else {
                assertEquals(Pbrt.RoundUpPow2(i), 1 << (Pbrt.Log2Int(i) + 1));
            }
        }

        for (long i = 1; i < (1L << 24); ++i) {
            if (Pbrt.IsPowerOf2(i)) {
                assertEquals(Pbrt.RoundUpPow2(i), i);
            } else {
                assertEquals(Pbrt.RoundUpPow2(i), 1 << (Pbrt.Log2Int(i) + 1));
            }
        }

        for (int i = 0; i < 30; ++i) {
            int v = 1 << i;
            assertEquals(Pbrt.RoundUpPow2(v), v);
            if (v > 2) assertEquals(Pbrt.RoundUpPow2(v - 1), v);
            assertEquals(Pbrt.RoundUpPow2(v + 1), 2 * v);
        }
    
        for (long i = 0; i < 60; ++i) {
            long v = 1L << i;
            assertEquals(Pbrt.RoundUpPow2(v), v);
            if (v > 2) assertEquals(Pbrt.RoundUpPow2(v - 1), v);
            assertEquals(Pbrt.RoundUpPow2(v + 1), 2 * v);
        }    
    }
 }
