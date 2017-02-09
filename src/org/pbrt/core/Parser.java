
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class Parser {
    public static boolean ParseFile(String filename) {
        return false;
    }

    public static void PushInclude(String filename) {
        Error.Warning("Parser does not support include statement yet.");
    }
}