/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

import org.apache.commons.lang3.NotImplementedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class ParamSet {

    private class ParamSetItem<T> {
        public String Name;
        public T[] Values;
        public boolean LookedUp;

        public ParamSetItem(String name, T[] values) {
            Name = name;
            Values = values;
            LookedUp = false;
        }
    }

    public void AddFloat(String name, Float[] v) {
        EraseFloat(name);
        floats.add(new ParamSetItem<>(name, v));
    }

    public void AddInt(String name, Integer[] v) {
        EraseInt(name);
        ints.add(new ParamSetItem<>(name, v));
    }

    public void AddBoolean(String name, Boolean[] v) {
        EraseBoolean(name);
        bools.add(new ParamSetItem<>(name, v));
    }

    public void AddPoint2f(String name, Point2f[] v) {
        ErasePoint2f(name);
        point2fs.add(new ParamSetItem<>(name, v));
    }

    public void AddVector2f(String name, Vector2f[] v) {
        EraseVector2f(name);
        vector2fs.add(new ParamSetItem<>(name, v));
    }

    public void AddPoint3f(String name, Point3f[] v) {
        ErasePoint3f(name);
        point3fs.add(new ParamSetItem<>(name, v));
    }

    public void AddVector3f(String name, Vector3f[] v) {
        EraseVector3f(name);
        vector3fs.add(new ParamSetItem<>(name, v));
    }

    public void AddNormal3f(String name, Normal3f[] v) {
        EraseNormal3f(name);
        normals.add(new ParamSetItem<>(name, v));
    }

    public void AddString(String name, String[] v) {
        EraseString(name);
        strings.add(new ParamSetItem<>(name, v));
    }

    public void AddTexture(String name, String texname) {
        EraseTexture(name);
        String[] texnames = {texname};
        textures.add(new ParamSetItem<>(name, texnames));
    }

    public void AddRGBSpectrum(String name, Float[] v) {
        EraseSpectrum(name);
        assert v.length % 3 == 0;
        int nValues = v.length/3;
        Spectrum[] s = new Spectrum[nValues];
        for (int i = 0; i < nValues; i++) {
            s[i] = Spectrum.FromRGB(v[i*3], v[i*3+1], v[i*3+2]);
        }
        spectra.add(new ParamSetItem<>(name, s));
    }

    public void AddXYZSpectrum(String name, Float[] v) {
        EraseSpectrum(name);
        assert v.length % 3 == 0;
        int nValues = v.length/3;
        Spectrum[] s = new Spectrum[nValues];
        for (int i = 0; i < nValues; i++) {
            s[i] = Spectrum.FromXYZ(v[i*3], v[i*3+1], v[i*3+2]);
        }
        spectra.add(new ParamSetItem<>(name, s));
    }

    public void AddBlackbodySpectrum(String name, Float[] values) {
        EraseSpectrum(name);
        assert values.length % 2 == 0;
        int nValues = values.length/2;
        Spectrum[] s = new Spectrum[nValues];
        for (int i = 0; i < nValues; ++i) {
            float[] v = Spectrum.BlackbodyNormalized(Spectrum.CIE_lambda, values[2 * i]);
            s[i] = Spectrum.FromSampled(Spectrum.CIE_lambda, v);
            s[i].scale(values[2 * i + 1]);
        }
        spectra.add(new ParamSetItem<>(name, s));
    }

    public void AddSampledSpectrumFiles(String name, String[] filenames, int nValues) {
        EraseSpectrum(name);
        Spectrum[] s = new Spectrum[nValues];
        for (int i = 0; i < nValues; ++i) {
            String fn = FileUtil.AbsolutePath(FileUtil.ResolveFilename(filenames[i]));
            if (cachedSpectra.containsKey(fn)) {
                s[i] = cachedSpectra.get(fn);
                continue;
            }

            float[] vals = FloatFile.Read(fn);
            if (vals == null) {
                PBrtTLogger.Warning("Unable to read SPD file \"%s\".  Using black distribution.", fn);
                s[i] = new Spectrum(0);
            }
            else {
                if (vals.length % 2 != 0) {
                    PBrtTLogger.Warning("Extra value found in spectrum file \"%s\". Ignoring it.", fn);
                }
                float[] wls = new float[vals.length/2], v = new float[vals.length/2];
                for (int j = 0; j < vals.length / 2; ++j) {
                    wls[j] = vals[2 * j];
                    v[j] = vals[2 * j + 1];
                }
                s[i] = Spectrum.FromSampled(wls, v);
            }
            cachedSpectra.put(fn, s[i]);
        }

        ParamSetItem<Spectrum> psi = new ParamSetItem<>(name, s);
        spectra.add(psi);
    }

    public void AddSampledSpectrum(String name, Float[] values) {
        EraseSpectrum(name);
        assert values.length % 2 == 0;
        int nValues = values.length/2;
        float[] wl = new float[nValues];
        float[] v = new float[nValues];
        for (int i = 0; i < nValues; ++i) {
            wl[i] = values[2 * i];
            v[i] = values[2 * i + 1];
        }
        Spectrum[] s = new Spectrum[1];
        s[0] = Spectrum.FromSampled(wl, v);
        spectra.add(new ParamSetItem<>(name, s));
    }

    public boolean EraseInt(String name) {
        for (int i = 0; i < ints.size(); ++i) {
            if (Objects.equals(ints.get(i).Name, name)) {
                ints.remove(i);
                return true;
            }
        }
        return false;
    }

    public boolean EraseBoolean(String name) {
        for (int i = 0; i < bools.size(); ++i) {
            if (Objects.equals(bools.get(i).Name, name)) {
                bools.remove(i);
                return true;
            }
        }
        return false;
    }

    public boolean EraseFloat(String name) {
        for (int i = 0; i < floats.size(); ++i) {
            if (Objects.equals(floats.get(i).Name, name)) {
                floats.remove(i);
                return true;
            }
        }
        return false;
    }

    public boolean ErasePoint2f(String name) {
        for (int i = 0; i < point2fs.size(); ++i) {
            if (Objects.equals(point2fs.get(i).Name, name)) {
                point2fs.remove(i);
                return true;
            }
        }
        return false;
    }

    public boolean EraseVector2f(String name) {
        for (int i = 0; i < vector2fs.size(); ++i) {
            if (Objects.equals(vector2fs.get(i).Name, name)) {
                vector2fs.remove(i);
                return true;
            }
        }
        return false;
    }

    public boolean ErasePoint3f(String name) {
        for (int i = 0; i < point3fs.size(); ++i) {
            if (Objects.equals(point3fs.get(i).Name, name)) {
                point3fs.remove(i);
                return true;
            }
        }
        return false;
    }

    public boolean EraseVector3f(String name) {
        for (int i = 0; i < vector3fs.size(); ++i) {
            if (Objects.equals(vector3fs.get(i).Name, name)) {
                vector3fs.remove(i);
                return true;
            }
        }
        return false;
    }

    public boolean EraseNormal3f(String name) {
        for (int i = 0; i < normals.size(); ++i) {
            if (Objects.equals(normals.get(i).Name, name)) {
                normals.remove(i);
                return true;
            }
        }
        return false;
    }

    public boolean EraseSpectrum(String name) {
        for (int i = 0; i < spectra.size(); ++i) {
            if (Objects.equals(spectra.get(i).Name, name)) {
                spectra.remove(i);
                return true;
            }
        }
        return false;
    }

    public boolean EraseString(String name) {
        for (int i = 0; i < strings.size(); ++i) {
            if (Objects.equals(strings.get(i).Name, name)) {
                strings.remove(i);
                return true;
            }
        }
        return false;
    }

    public boolean EraseTexture(String name) {
        for (int i = 0; i < textures.size(); ++i) {
            if (Objects.equals(textures.get(i).Name, name)) {
                textures.remove(i);
                return true;
            }
        }
        return false;
    }

    public float FindOneFloat(String name, float d) {
        for (ParamSetItem<Float> cur : floats) {
            if (Objects.equals(cur.Name, name) && cur.Values.length == 1) {
                cur.LookedUp = true;
                return cur.Values[0];
            }
        }
        return d;
    }

    public int FindOneInt(String name, int d) {
        for (ParamSetItem<Integer> cur : ints) {
            if (Objects.equals(cur.Name, name) && cur.Values.length == 1) {
                cur.LookedUp = true;
                return cur.Values[0];
            }
        }
        return d;
    }

    public boolean FindOneBoolean(String name, boolean d) {
        for (ParamSetItem<Boolean> cur : bools) {
            if (Objects.equals(cur.Name, name) && cur.Values.length == 1) {
                cur.LookedUp = true;
                return cur.Values[0];
            }
        }
        return d;
    }

    public Point2f FindOnePoint2f(String name, Point2f d) {
        for (ParamSetItem<Point2f> cur : point2fs) {
            if (Objects.equals(cur.Name, name) && cur.Values.length == 1) {
                cur.LookedUp = true;
                return cur.Values[0];
            }
        }
        return d;
    }

    public Vector2f FindOneVector2f(String name, Vector2f d) {
        for (ParamSetItem<Vector2f> cur : vector2fs) {
            if (Objects.equals(cur.Name, name) && cur.Values.length == 1) {
                cur.LookedUp = true;
                return cur.Values[0];
            }
        }
        return d;
    }

    public Point3f FindOnePoint3f(String name, Point3f d) {
        for (ParamSetItem<Point3f> cur : point3fs) {
            if (Objects.equals(cur.Name, name) && cur.Values.length == 1) {
                cur.LookedUp = true;
                return cur.Values[0];
            }
        }
        return d;
    }

    public Vector3f FindOneVector3f(String name, Vector3f d) {
        for (ParamSetItem<Vector3f> cur : vector3fs) {
            if (Objects.equals(cur.Name, name) && cur.Values.length == 1) {
                cur.LookedUp = true;
                return cur.Values[0];
            }
        }
        return d;
    }

    public Normal3f FindOneNormal3f(String name, Normal3f d) {
        for (ParamSetItem<Normal3f> cur : normals) {
            if (Objects.equals(cur.Name, name) && cur.Values.length == 1) {
                cur.LookedUp = true;
                return cur.Values[0];
            }
        }
        return d;
    }

    public Spectrum FindOneSpectrum(String name, Spectrum d) {
        for (ParamSetItem<Spectrum> cur : spectra) {
            if (Objects.equals(cur.Name, name) && cur.Values.length == 1) {
                cur.LookedUp = true;
                return cur.Values[0];
            }
        }
        return d;
    }

    public String FindOneString(String name, String d) {
        for (ParamSetItem<String> cur : strings) {
            if (Objects.equals(cur.Name, name) && cur.Values.length == 1) {
                cur.LookedUp = true;
                return cur.Values[0];
            }
        }
        return d;
    }

    public String FindOneFilename(String name, String d) {
        String filename = FindOneString(name, "");
        if (Objects.equals(filename, "")) return d;
        filename = FileUtil.AbsolutePath(FileUtil.ResolveFilename(filename));
        return filename;
    }

    public String FindTexture(String name) {
        for (ParamSetItem<String> cur : textures) {
            if (Objects.equals(cur.Name, name) && cur.Values.length == 1) {
                cur.LookedUp = true;
                return cur.Values[0];
            }
        }
        return "";
    }

    public Float[] FindFloat(String name) {
        for (ParamSetItem<Float> cur : floats) {
            if (Objects.equals(cur.Name, name)) {
                cur.LookedUp = true;
                return cur.Values;
            }
        }
        return null;
    }

    public Integer[] FindInt(String name) {
        for (ParamSetItem<Integer> cur : ints) {
            if (Objects.equals(cur.Name, name)) {
                cur.LookedUp = true;
                return cur.Values;
            }
        }
        return null;
    }

    public Boolean[] FindBoolean(String name) {
        for (ParamSetItem<Boolean> cur : bools) {
            if (Objects.equals(cur.Name, name)) {
                cur.LookedUp = true;
                return cur.Values;
            }
        }
        return null;
    }

    public Point2f[] FindPoint2f(String name) {
        for (ParamSetItem<Point2f> cur : point2fs) {
            if (Objects.equals(cur.Name, name)) {
                cur.LookedUp = true;
                return cur.Values;
            }
        }
        return null;
    }

    public Vector2f[] FindVector2f(String name) {
        for (ParamSetItem<Vector2f> cur : vector2fs) {
            if (Objects.equals(cur.Name, name)) {
                cur.LookedUp = true;
                return cur.Values;
            }
        }
        return null;
    }

    public Point3f[] FindPoint3f(String name) {
        for (ParamSetItem<Point3f> cur : point3fs) {
            if (Objects.equals(cur.Name, name)) {
                cur.LookedUp = true;
                return cur.Values;
            }
        }
        return null;
    }

    public Vector3f[] FindVector3f(String name) {
        for (ParamSetItem<Vector3f> cur : vector3fs) {
            if (Objects.equals(cur.Name, name)) {
                cur.LookedUp = true;
                return cur.Values;
            }
        }
        return null;
    }

    public Normal3f[] FindNormal3f(String name) {
        for (ParamSetItem<Normal3f> cur : normals) {
            if (Objects.equals(cur.Name, name)) {
                cur.LookedUp = true;
                return cur.Values;
            }
        }
        return null;
    }

    public Spectrum[] FindSpectrum(String name) {
        for (ParamSetItem<Spectrum> cur : spectra) {
            if (Objects.equals(cur.Name, name)) {
                cur.LookedUp = true;
                return cur.Values;
            }
        }
        return null;
    }

    public String[] FindString(String name) {
        for (ParamSetItem<String> cur : strings) {
            if (Objects.equals(cur.Name, name)) {
                cur.LookedUp = true;
                return cur.Values;
            }
        }
        return null;
    }

    public void ReportUnused() {
        for (ParamSetItem<Boolean> bool : bools) {
            if (!bool.LookedUp) {
                PBrtTLogger.Warning("Parameter \"%s\" not used.", bool.Name);
            }
        }
        for (ParamSetItem<Integer> anInt : ints) {
            if (!anInt.LookedUp) {
                PBrtTLogger.Warning("Parameter \"%s\" not used.", anInt.Name);
            }
        }
        for (ParamSetItem<Float> aFloat : floats) {
            if (!aFloat.LookedUp) {
                PBrtTLogger.Warning("Parameter \"%s\" not used.", aFloat.Name);
            }
        }
        for (ParamSetItem<Point2f> point2f : point2fs) {
            if (!point2f.LookedUp) {
                PBrtTLogger.Warning("Parameter \"%s\" not used.", point2f.Name);
            }
        }
        for (ParamSetItem<Vector2f> vector2f : vector2fs) {
            if (!vector2f.LookedUp) {
                PBrtTLogger.Warning("Parameter \"%s\" not used.", vector2f.Name);
            }
        }
        for (ParamSetItem<Point3f> point3f : point3fs) {
            if (!point3f.LookedUp) {
                PBrtTLogger.Warning("Parameter \"%s\" not used.", point3f.Name);
            }
        }
        for (ParamSetItem<Vector3f> vector3f : vector3fs) {
            if (!vector3f.LookedUp) {
                PBrtTLogger.Warning("Parameter \"%s\" not used.", vector3f.Name);
            }
        }
        for (ParamSetItem<Normal3f> normal : normals) {
            if (!normal.LookedUp) {
                PBrtTLogger.Warning("Parameter \"%s\" not used.", normal.Name);
            }
        }
        for (ParamSetItem<Spectrum> aSpectra : spectra) {
            if (!aSpectra.LookedUp) {
                PBrtTLogger.Warning("Parameter \"%s\" not used.", aSpectra.Name);
            }
        }
        for (ParamSetItem<String> string : strings) {
            if (!string.LookedUp) {
                PBrtTLogger.Warning("Parameter \"%s\" not used.", string.Name);
            }
        }
        for (ParamSetItem<String> texture : textures) {
            if (!texture.LookedUp) {
                PBrtTLogger.Warning("Parameter \"%s\" not used.", texture.Name);
            }
        }
    }

    public void Clear() {
        bools.clear();
        ints.clear();
        floats.clear();
        point2fs.clear();
        vector2fs.clear();
        point3fs.clear();
        vector3fs.clear();
        normals.clear();
        spectra.clear();
        strings.clear();
        textures.clear();
    }

    @Override
    public String toString() {
        StringBuilder ret;
        int i;
        int j;
        String typeString;
        StringBuilder retBuilder = new StringBuilder();
        for (i = 0; i < ints.size(); ++i) {
            ParamSetItem<Integer> item = ints.get(i);
            typeString = "integer ";
            // Print _ParamSetItem_ declaration, determine how many to print
            int nPrint = item.Values.length;
            retBuilder.append("\"");
            retBuilder.append(typeString);
            retBuilder.append(item.Name);
            retBuilder.append("\"");
            retBuilder.append(" [");
            for (j = 0; j < nPrint; ++j)
                retBuilder.append(String.format("%d ", item.Values[j]));
            retBuilder.append("] ");
        }
        ret = new StringBuilder(retBuilder.toString());
        for (i = 0; i < bools.size(); ++i) {
            ParamSetItem<Boolean> item = bools.get(i);
            typeString = "bool ";
            // Print _ParamSetItem_ declaration, determine how many to print
            int nPrint = item.Values.length;
            ret.append("\"");
            ret.append(typeString);
            ret.append(item.Name);
            ret.append("\"");
            ret.append(" [");
            for (j = 0; j < nPrint; ++j)
                ret.append(String.format("\"%s\" ", item.Values[j] ? "true" : "false"));
            ret.append("] ");
        }
        for (i = 0; i < floats.size(); ++i) {
            ParamSetItem<Float> item = floats.get(i);
            typeString = "float ";
            // Print _ParamSetItem_ declaration, determine how many to print
            int nPrint = item.Values.length;
            ret.append("\"");
            ret.append(typeString);
            ret.append(item.Name);
            ret.append("\"");
            ret.append(" [");
            for (j = 0; j < nPrint; ++j)
                ret.append(String.format("%.8g ", item.Values[j]));
            ret.append("] ");
        }
        for (i = 0; i < point2fs.size(); ++i) {
            ParamSetItem<Point2f> item = point2fs.get(i);
            typeString = "point2 ";
            // Print _ParamSetItem_ declaration, determine how many to print
            int nPrint = item.Values.length;
            ret.append("\"");
            ret.append(typeString);
            ret.append(item.Name);
            ret.append("\"");
            ret.append(" [");
            for (j = 0; j < nPrint; ++j)
                ret.append(String.format("%.8g %.8g ", item.Values[j].x, item.Values[j].y));
            ret.append("] ");
        }
        for (i = 0; i < vector2fs.size(); ++i) {
            ParamSetItem<Vector2f> item = vector2fs.get(i);
            typeString = "vector2 ";
            // Print _ParamSetItem_ declaration, determine how many to print
            int nPrint = item.Values.length;
            ret.append("\"");
            ret.append(typeString);
            ret.append(item.Name);
            ret.append("\"");
            ret.append(" [");
            for (j = 0; j < nPrint; ++j)
                ret.append(String.format("%.8g %.8g ", item.Values[j].x, item.Values[j].y));
            ret.append("] ");
        }
        for (i = 0; i < point3fs.size(); ++i) {
            ParamSetItem<Point3f> item = point3fs.get(i);
            typeString = "point3 ";
            // Print _ParamSetItem_ declaration, determine how many to print
            int nPrint = item.Values.length;
            ret.append("\"");
            ret.append(typeString);
            ret.append(item.Name);
            ret.append("\"");
            ret.append(" [");
            for (j = 0; j < nPrint; ++j)
                ret.append(String.format("%.8g %.8g %.8g ", item.Values[j].x, item.Values[j].y, item.Values[j].z));
            ret.append("] ");
        }
        for (i = 0; i < vector3fs.size(); ++i) {
            ParamSetItem<Vector3f> item = vector3fs.get(i);
            typeString = "vector3 ";
            // Print _ParamSetItem_ declaration, determine how many to print
            int nPrint = item.Values.length;
            ret.append("\"");
            ret.append(typeString);
            ret.append(item.Name);
            ret.append("\"");
            ret.append(" [");
            for (j = 0; j < nPrint; ++j)
                ret.append(String.format("%.8g %.8g %.8g ", item.Values[j].x, item.Values[j].y, item.Values[j].z));
            ret.append("] ");
        }
        for (i = 0; i < normals.size(); ++i) {
            ParamSetItem<Normal3f> item = normals.get(i);
            typeString = "normal ";
            // Print _ParamSetItem_ declaration, determine how many to print
            int nPrint = item.Values.length;
            ret.append("\"");
            ret.append(typeString);
            ret.append(item.Name);
            ret.append("\"");
            ret.append(" [");
            for (j = 0; j < nPrint; ++j)
                ret.append(String.format("%.8g %.8g %.8g ", item.Values[j].x, item.Values[j].y, item.Values[j].z));
            ret.append("] ");
        }
        for (i = 0; i < strings.size(); ++i) {
            ParamSetItem<String> item = strings.get(i);
            typeString = "string ";
            // Print _ParamSetItem_ declaration, determine how many to print
            int nPrint = item.Values.length;
            ret.append("\"");
            ret.append(typeString);
            ret.append(item.Name);
            ret.append("\"");
            ret.append(" [");
            for (j = 0; j < nPrint; ++j)
                ret.append(String.format("\"%s\" ", item.Values[j]));
            ret.append("] ");
        }
        for (i = 0; i < textures.size(); ++i) {
            ParamSetItem<String> item = textures.get(i);
            typeString = "texture ";
            // Print _ParamSetItem_ declaration, determine how many to print
            int nPrint = item.Values.length;
            ret.append("\"");
            ret.append(typeString);
            ret.append(item.Name);
            ret.append("\"");
            ret.append(" [");
            for (j = 0; j < nPrint; ++j)
                ret.append(String.format("\"%s\" ", item.Values[j]));
            ret.append("] ");
        }
        for (i = 0; i < spectra.size(); ++i) {
            ParamSetItem<Spectrum> item = spectra.get(i);
            typeString = "color ";
            // Print _ParamSetItem_ declaration, determine how many to print
            int nPrint = item.Values.length;
            ret.append("\"");
            ret.append(typeString);
            ret.append(item.Name);
            ret.append("\"");
            ret.append(" [");
            for (j = 0; j < nPrint; ++j) {
                float[] rgb = item.Values[j].toRGB();
                ret.append(String.format("%.8g %.8g %.8g ", rgb[0], rgb[1], rgb[2]));
            }
            ret.append("] ");
        }
        return ret.toString();
    }

    public ParamSet() {}

    public ParamSet(ParamSet ps) {
        this.bools = new ArrayList<>(ps.bools);
        this.ints = new ArrayList<>(ps.ints);
        this.floats = new ArrayList<>(ps.floats);
        this.point2fs = new ArrayList<>(ps.point2fs);
        this.vector2fs = new ArrayList<>(ps.vector2fs);
        this.point3fs = new ArrayList<>(ps.point3fs);
        this.vector3fs = new ArrayList<>(ps.vector3fs);
        this.normals = new ArrayList<>(ps.normals);
        this.spectra = new ArrayList<>(ps.spectra);
        this.strings = new ArrayList<>(ps.strings);
        this.textures = new ArrayList<>(ps.textures);
    }

    public void Print(int indent) {
        throw new NotImplementedException("TODO");
    }

    private ArrayList<ParamSetItem<Boolean>> bools = new ArrayList<>(1);
    private ArrayList<ParamSetItem<Integer>> ints = new ArrayList<>(1);
    private ArrayList<ParamSetItem<Float>> floats = new ArrayList<>(1);
    private ArrayList<ParamSetItem<Point2f>> point2fs = new ArrayList<>(1);
    private ArrayList<ParamSetItem<Vector2f>> vector2fs = new ArrayList<>(1);
    private ArrayList<ParamSetItem<Point3f>> point3fs = new ArrayList<>(1);
    private ArrayList<ParamSetItem<Vector3f>> vector3fs = new ArrayList<>(1);
    private ArrayList<ParamSetItem<Normal3f>> normals = new ArrayList<>(1);
    private ArrayList<ParamSetItem<Spectrum>> spectra = new ArrayList<>(1);
    private ArrayList<ParamSetItem<String>> strings = new ArrayList<>(1);
    private ArrayList<ParamSetItem<String>> textures = new ArrayList<>(1);

    private static HashMap<String, Spectrum> cachedSpectra = new HashMap<>();
}