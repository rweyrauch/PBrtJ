/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class Options {

    public int NumThreads = 0;
    public boolean QuickRender = false;
    public boolean Quiet = false;
    public boolean Cat = false;
    public boolean ToPly = false;
    public String ImageFile;
    // x0, x1, y0, y1
    public float[][] CropWindow = { {0, 1}, {0, 1}};
}