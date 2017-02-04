/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class Error {

    public synchronized static void Warning(String format, Object... args) {
        if (Pbrt.options.Quiet) return;
        System.out.format(format, args);
    }
    public synchronized static void Error(String format, Object... args) {
        System.err.format(format, args);
    }

}