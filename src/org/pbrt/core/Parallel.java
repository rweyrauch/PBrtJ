/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class Parallel {

    public static void ParallelFor(Consumer<Long> func, long count, int chunkSize) {

        // Run iterations immediately if _count_ is small
        if (count < chunkSize) {
            for (long i = 0; i < count; ++i) func.accept(i);
            return;
        }

        ExecutorService pool = Executors.newFixedThreadPool(MaxThreadIndex());

        final long numChunks = (count + chunkSize - 1) / chunkSize;

        for (long i = 0; i < numChunks; i++) {
            final long startIndex = i * chunkSize;
            final long endIndex = Math.min(startIndex+chunkSize, count);
            Runnable task1D = () -> {
                //System.out.format("Iteration: %d to %d\n", startIndex, endIndex);
                for (long ii = startIndex; ii < endIndex; ii++) {
                    func.accept(ii);
                }
            };
            pool.execute(task1D);
        }

        try {
            pool.shutdown();
            pool.awaitTermination(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void ParallelFor2D(Consumer<Point2i> func, Point2i count) {

        if (count.x * count.y <= 1) {
            for (int y = 0; y < count.y; ++y)
                for (int x = 0; x < count.x; ++x) func.accept(new Point2i(x, y));
            return;
        }

        ExecutorService pool = Executors.newFixedThreadPool(MaxThreadIndex());

        final long chunkSize = 1;
        final long numChunks = count.x * count.y;

        for (long i = 0; i < numChunks; i++) {
            final long startIndex = i * chunkSize;
            final long endIndex = startIndex+chunkSize;
            Runnable task2D = () -> {
                for (long ii = startIndex; ii < endIndex; ii++) {
                    func.accept(new Point2i((int)(ii % count.x), (int)(ii / count.x)));
                }
            };
            pool.execute(task2D);
        }

        try {
            pool.shutdown();
            pool.awaitTermination(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int MaxThreadIndex() {
        return (Pbrt.options.NumThreads == 0) ? NumSystemCores() : Pbrt.options.NumThreads;
    }

    public static int NumSystemCores() {
        return Runtime.getRuntime().availableProcessors();
    }

    public static void ParallelInit() {
    }

    public static void ParallelCleanup() {
    }
}
