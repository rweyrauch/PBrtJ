
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.shapes;

import org.pbrt.core.*;

import java.util.ArrayList;

public class HeightField {

    public static ArrayList<Shape> Create(Transform object2world, Transform world2object, boolean reverseOrientation, ParamSet paramSet) {
        int nx = paramSet.FindOneInt("nu", -1);
        int ny = paramSet.FindOneInt("nv", -1);
        
        Float[] z = paramSet.FindFloat("Pz");
        assert (z.length == nx * ny);
        assert (nx != -1 && ny != -1 && z != null);

        int ntris = 2 * (nx - 1) * (ny - 1);
        int[] indices = new int[3 * ntris];
        Point3f[] P = new Point3f[nx * ny];
        Point2f[] uvs = new Point2f[nx * ny];
        int nverts = nx * ny;
        // Compute heightfield vertex positions
        int pos = 0;
        for (int y = 0; y < ny; ++y) {
            for (int x = 0; x < nx; ++x) {
                P[pos].x = uvs[pos].x = (float)x / (float)(nx - 1);
                P[pos].y = uvs[pos].y = (float)y / (float)(ny - 1);
                P[pos].z = z[pos];
                ++pos;
            }
        }

        // Fill in heightfield vertex offset array
        int i = 0;
        for (int y = 0; y < ny - 1; ++y) {
            for (int x = 0; x < nx - 1; ++x) {
                indices[i++] = VERT(x, y, nx);
                indices[i++] = VERT(x + 1, y, nx);
                indices[i++] = VERT(x + 1, y + 1, nx);

                indices[i++] = VERT(x, y, nx);
                indices[i++] = VERT(x + 1, y + 1, nx);
                indices[i++] = VERT(x, y + 1, nx);
            }
        }

        return Triangle.CreateTriangleMesh(object2world, world2object, reverseOrientation,
                ntris, indices, nverts, P, null, null, uvs, null, null);
    }

    private static int VERT(int x, int y, int nx) {
        return x + y * nx;
    }
}