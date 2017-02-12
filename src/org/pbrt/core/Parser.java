
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
import java.util.Objects;

public class Parser {
    public static boolean ParseFile(String filename) {
        return false;
    }

    public static void PushInclude(String filename) {
        Error.Warning("Parser does not support include statement yet.");
    }

    public static class PbrtParameter {

        public String type;
        public String name;
        public Object value;

        public PbrtParameter(String descriptor, Object value) {
            String[] tokens = descriptor.split(" ");
            assert tokens.length == 2;
            this.type = tokens[0];
            this.name = tokens[1];
            this.value = value;
        }
    }

    public static ParamSet CreateParamSet(ArrayList<PbrtParameter> paramlist) {
        if (paramlist == null) return null;

        ParamSet pset = new ParamSet();
        for (PbrtParameter param : paramlist) {
            if (Objects.equals(param.type, "int")) {
                if (param.value instanceof Float[]) {
                    // parser converts all values to float, convert back to int
                    Float[] fvalue = (Float[])param.value;
                    Integer[] ivalue = new Integer[fvalue.length];
                    for (int i = 0; i < fvalue.length; i++) {
                        ivalue[i] = (int)Math.floor(fvalue[i]);
                    }
                    pset.AddInt(param.name, ivalue);
                }
                else {
                    Error.Error("Unexpected value array type for 'integer' parameter.\n");
                }
            }
            else if (Objects.equals(param.type, "bool")) {
                if (param.value instanceof Boolean[]) {
                    pset.AddBoolean(param.name, (Boolean[])param.value);
                }
                else {
                    Error.Error("Unexpected value array type for 'bool' parameter.\n");
                }
            }
            else if (Objects.equals(param.type, "float")) {
                if (param.value instanceof Float[]) {
                    pset.AddFloat(param.name, (Float[])param.value);
                }
                else {
                    Error.Error("Unexpected value array type for 'float' parameter.\n");
                }
            }
            else if (Objects.equals(param.type, "point2")) {
                if (param.value instanceof Float[]) {
                    Float[] pvalues = (Float[])param.value;
                    if (pvalues.length % 2 == 0) {
                        Point2f[] points = new Point2f[pvalues.length/2];
                        for (int i = 0; i < pvalues.length; i += 2) {
                            points[i] = new Point2f(pvalues[i], pvalues[i+1]);
                        }
                        pset.AddPoint2f(param.name, points);
                    }
                    else {
                        Error.Error("Length of 'point2' parameter list must be a factor of 2.");
                    }
                }
                else {
                    Error.Error("Unexpected value array type for 'point2' parameter.\n");
                }
            }
            else if (Objects.equals(param.type, "vector2")) {
                if (param.value instanceof Float[]) {
                    Float[] vvalues = (Float[])param.value;
                    if (vvalues.length % 2 == 0) {
                        Vector2f[] vectors = new Vector2f[vvalues.length/2];
                        for (int i = 0; i < vvalues.length; i += 2) {
                            vectors[i] = new Vector2f(vvalues[i], vvalues[i+1]);
                        }
                        pset.AddVector2f(param.name, vectors);
                    }
                    else {
                        Error.Error("Length of 'vector2' parameter list must be a factor of 2.");
                    }
                }
                else {
                    Error.Error("Unexpected value array type for 'vector2' parameter.\n");
                }
            }
            else if (Objects.equals(param.type, "point3")) {
                if (param.value instanceof Float[]) {
                    Float[] pvalues = (Float[])param.value;
                    if (pvalues.length % 3 == 0) {
                        Point3f[] points = new Point3f[pvalues.length/3];
                        for (int i = 0; i < pvalues.length; i += 3) {
                            points[i] = new Point3f(pvalues[i], pvalues[i+1], pvalues[i+2]);
                        }
                        pset.AddPoint3f(param.name, points);
                    }
                    else {
                        Error.Error("Length of 'point3' parameter list must be a factor of 3.");
                    }
                }
                else {
                    Error.Error("Unexpected value array type for 'point3' parameter.\n");
                }
            }
            else if (Objects.equals(param.type, "vector3")) {
                if (param.value instanceof Float[]) {
                    Float[] vvalues = (Float[])param.value;
                    if (vvalues.length % 3 == 0) {
                        Vector3f[] vectors = new Vector3f[vvalues.length/3];
                        for (int i = 0; i < vvalues.length; i += 3) {
                            vectors[i] = new Vector3f(vvalues[i], vvalues[i+1], vvalues[i+2]);
                        }
                        pset.AddVector3f(param.name, vectors);
                    }
                    else {
                        Error.Error("Length of 'vector3' parameter list must be a factor of 3.");
                    }
                }
                else {
                    Error.Error("Unexpected value array type for 'vector3' parameter.\n");
                }
            }
            else if (Objects.equals(param.type, "normal")) {
                if (param.value instanceof Float[]) {
                    Float[] nvalues = (Float[])param.value;
                    if (nvalues.length % 3 == 0) {
                        Normal3f[] normals = new Normal3f[nvalues.length/3];
                        for (int i = 0; i < nvalues.length; i += 3) {
                            normals[i] = new Normal3f(nvalues[i], nvalues[i+1], nvalues[i+2]);
                        }
                        pset.AddNormal3f(param.name, normals);
                    }
                    else {
                        Error.Error("Length of 'normal' parameter list must be a factor of 3.");
                    }
                }
                else {
                    Error.Error("Unexpected value array type for 'normal' parameter.\n");
                }
            }
            else if (Objects.equals(param.type, "rgb") || Objects.equals(param.type, "color")) {
                if (param.value instanceof Float[]) {
                    Float[] cvalues = (Float[])param.value;
                    if (cvalues.length % 3 == 0) {
                        pset.AddRGBSpectrum(param.name, cvalues);
                    }
                    else {
                        Error.Error("Length of 'rgb' or 'color' parameter list must be a factor of 3.");
                    }
                }
                else {
                    Error.Error("Unexpected value array type for 'rgb' or 'color' parameter.\n");
                }
            }
            else if (Objects.equals(param.type, "xyz")) {
                if (param.value instanceof Float[]) {
                    Float[] xvalues = (Float[])param.value;
                    if (xvalues.length % 3 == 0) {
                        pset.AddXYZSpectrum(param.name, xvalues);
                    }
                    else {
                        Error.Error("Length of 'xyz' parameter list must be a factor of 3.");
                    }
                }
                else {
                    Error.Error("Unexpected value array type for 'xyz' parameter'.\n");
                }
            }
            else if (Objects.equals(param.type, "blackbody")) {
                if (param.value instanceof Float[]) {
                    pset.AddBlackbodySpectrum(param.name, (Float[])param.value);
                }
                else {
                    Error.Error("Unexpected value array type for 'blackbody' parameter.\n");
                }
            }
            else if (Objects.equals(param.type, "spectrum")) {
                if (param.value instanceof Float[]) {
                    pset.AddSampledSpectrum(param.name, (Float[])param.value);
                }
                else if (param.value instanceof String[]) {
                    pset.AddSampledSpectrumFiles(param.name, (String[])param.value);
                }
                else {
                    Error.Error("Unexpected value array type for 'spectrum' parameter.\n");
                }
            }
            else if (Objects.equals(param.type, "string")) {
                if (param.value instanceof String[]) {
                    pset.AddString(param.name, (String[])param.value);
                }
                else {
                    Error.Error("Unexpected value array type for 'string' parameter.\n");
                }
            }
            else if (Objects.equals(param.type, "texture")) {
                if (param.value instanceof String[]) {
                    String[] strings = (String[])param.value;
                    pset.AddTexture(param.name, strings[0]);
                }
                else {
                    Error.Error("Unexpected value array type for 'texture' parameter.\n");
                }
            }
            else {
                Error.Error("Unknown parameter type: %s\n", param.type);
            }
        }
        return pset;
    }

}