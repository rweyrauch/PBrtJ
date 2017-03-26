/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.tests

import org.pbrt.core.Options
import org.pbrt.core.Parallel
import org.pbrt.core.Pbrt
import org.pbrt.core.Point2i

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Consumer

class ParallelTest extends GroovyTestCase {

    void testParallelBasics() {
        Pbrt.options = new Options()

        Parallel.ParallelInit()

        AtomicLong counter = new AtomicLong()
        counter.set(0)
        Consumer<Long> funcL = [accept:{ long i -> counter.incrementAndGet() }] as Consumer
        Parallel.ParallelFor(funcL, 1000, 1)
        assertEquals(1000, counter.get())

        counter.set(0)
        Parallel.ParallelFor(funcL, 1000, 19)
        assertEquals(1000, counter.get())

        counter.set(0)
        Consumer<Point2i> funcP = [accept:{ Point2i p -> counter.incrementAndGet() }] as Consumer
        Parallel.ParallelFor2D(funcP, new Point2i(15, 14))
        assertEquals(15*14, counter.get())

        Parallel.ParallelCleanup()
    }

    void testParallelDoNothing() {
        Pbrt.options = new Options()

        Parallel.ParallelInit()

        AtomicInteger counter = new AtomicInteger()
        counter.set(0)
        Consumer<Long> funcL = [accept:{ long i -> counter.incrementAndGet() }] as Consumer
        Parallel.ParallelFor(funcL, 0L, 1)
        assertEquals(0, counter.get())

        counter.set(0)
        Consumer<Point2i> funcP = [accept:{ Point2i p -> counter.incrementAndGet() }] as Consumer
        Parallel.ParallelFor2D(funcP, new Point2i(0, 0))
        assertEquals(0, counter.get())

        Parallel.ParallelCleanup()
    }

}
