
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
import org.pbrt.core.Error;
import org.pbrt.textures.ConstantTexture;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

import org.smurn.jply.*;
import org.smurn.jply.util.NormalMode;
import org.smurn.jply.util.NormalizingPlyReader;
import org.smurn.jply.util.TesselationMode;
import org.smurn.jply.util.TextureMode;

public class PlyMesh {

    public static ArrayList<Shape> Create(Transform object2world, Transform world2object, boolean reverseOrientation, ParamSet paramSet, Map<String, Texture<Float>> floatTextures) {
        String filename = paramSet.FindOneFilename("filename", "");

        PlyReader ply = null;
        try {
            ply = new PlyReaderFile(filename);
        }
        catch (IOException e) {
            Error.Error("Couldn't open PLY file \"%s\"", filename);
            return new ArrayList<>();
        }

        ply = new NormalizingPlyReader(ply, TesselationMode.TRIANGLES, NormalMode.ADD_NORMALS_CCW, TextureMode.XY);

        int vertexCount = ply.getElementCount("vertex");
        int faceCount = ply.getElementCount("face");

        if (vertexCount == 0 || faceCount == 0) {
            Error.Error("PLY file \"%s\" is invalid! No face/vertex elements found!", filename);
            return new ArrayList<>();
        }

        Point3f[] points = new Point3f[vertexCount];
        Normal3f[] normals = new Normal3f[vertexCount];
        Point2f[] uvs = new Point2f[vertexCount];
        int[] indices = new int[faceCount * 3];

        int indexCtr = 0;
        int vertexCtr = 0;

        try {
            ElementReader reader = ply.nextElementReader();
            while (reader != null) {
                ElementType etype = reader.getElementType();
                if (Objects.equals(etype.getName(), "vertex")) {
                    Element element = reader.readElement();
                    while (element != null) {
                        float x = (float)element.getDouble("x");
                        float y = (float)element.getDouble("y");
                        float z = (float)element.getDouble("z");
                        points[vertexCtr] = new Point3f(x, y, z);

                        float nx = (float)element.getDouble("nx");
                        float ny = (float)element.getDouble("ny");
                        float nz = (float)element.getDouble("nz");
                        normals[vertexCtr] = new Normal3f(nx, ny, nz);

                        float u = (float)element.getDouble("u");
                        float v = (float)element.getDouble("v");
                        uvs[vertexCtr] = new Point2f(u, v);

                        vertexCtr++;
                        element = reader.readElement();
                    }
                }
                else if (Objects.equals(etype.getName(), "face")) {
                    Element element = reader.readElement();
                    while (element != null) {
                        int[] ndx = element.getIntList("vertex_index");
                        assert (ndx.length == 3);
                        for (int i = 0; i < 3; i++) {
                            indices[indexCtr++] = ndx[i];
                        }
                        element = reader.readElement();
                    }
                }
                reader.close();
                reader = ply.nextElementReader();
            }

            ply.close();

        } catch (IOException e) {
            Error.Error("PLY file \"%s\", failed to read elements.", filename);
            return new ArrayList<>();
        }

        // Look up an alpha texture, if applicable
        Texture<Float> alphaTex = null;
        String alphaTexName = paramSet.FindTexture("alpha");
        if (!alphaTexName.isEmpty()) {
            if (floatTextures.containsKey(alphaTexName)) {
                alphaTex = floatTextures.get(alphaTexName);
            }
            else {
                Error.Error("Couldn't find float texture \"%s\" for \"alpha\" parameter", alphaTexName);
            }
        } else if (paramSet.FindOneFloat("alpha", 1) == 0) {
            alphaTex = new ConstantTexture<Float>(new Float(0));
        }

        Texture<Float> shadowAlphaTex = null;
        String shadowAlphaTexName = paramSet.FindTexture("shadowalpha");
        if (!shadowAlphaTexName.isEmpty()) {
            if (floatTextures.containsKey(shadowAlphaTexName)) {
                shadowAlphaTex = floatTextures.get(shadowAlphaTexName);
            }
            else {
                Error.Error("Couldn't find float texture \"%s\" for \"shadowalpha\" parameter", shadowAlphaTexName);
            }
        }
        else if (paramSet.FindOneFloat("shadowalpha", 1) == 0) {
            shadowAlphaTex = new ConstantTexture<Float>(new Float(0));
        }

        return Triangle.CreateTriangleMesh(object2world, world2object, reverseOrientation,
                indexCtr / 3, indices, vertexCount, points, null, normals, uvs, alphaTex, shadowAlphaTex);
    }
}