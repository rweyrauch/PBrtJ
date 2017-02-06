
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

import java.util.Map;
import org.pbrt.textures.ConstantTexture;

public class TextureParams {

    // TextureParams Private Data
    private Map<String, Texture<Float>> floatTextures;
    private Map<String, Texture<Spectrum>> spectrumTextures;
    private ParamSet geomParams, materialParams;

    // TextureParams Public Methods
    public TextureParams(ParamSet geomParams, ParamSet materialParams,
                         Map<String, Texture<Float>> fTex,
                         Map<String, Texture<Spectrum>> sTex) {
        this.floatTextures = fTex;
        this.spectrumTextures = sTex;
        this.geomParams = geomParams;
        this.materialParams = materialParams;
    }
    public Texture<Spectrum> GetSpectrumTexture(String name, Spectrum def) {
        String texname = geomParams.FindTexture(name);
        if (texname == "") texname = materialParams.FindTexture(name);
        if (texname != "") {
            if (spectrumTextures.get(texname) != null)
                return spectrumTextures.get(texname);
            else
                Error.Error("Couldn't find spectrum texture named \"%s\" for parameter \"%s\"", texname, name);
        }
        Spectrum val = materialParams.FindOneSpectrum(name, def);
        val = geomParams.FindOneSpectrum(name, val);
        return new ConstantTexture<Spectrum>(val);
    }
    public Texture<Spectrum> GetSpectrumTextureOrNull(String name) {
        String texname = geomParams.FindTexture(name);
        if (texname == "") texname = materialParams.FindTexture(name);
        if (texname != "") {
            if (spectrumTextures.get(texname) != null)
                return spectrumTextures.get(texname);
            else {
                Error.Error("Couldn't find spectrum texture named \"%s\" for parameter \"%s\"", texname, name);
                return null;
            }
        }
        Spectrum[] val = geomParams.FindSpectrum(name);
        if (val == null) val = materialParams.FindSpectrum(name);
        if (val != null) return new ConstantTexture<>(val[0]);
        return null;
    }
    public Texture<Float> GetFloatTexture(String name, float def) {
        String texname = geomParams.FindTexture(name);
        if (texname == "") texname = materialParams.FindTexture(name);
        if (texname != "") {
            if (floatTextures.get(texname) != null)
                return floatTextures.get(texname);
            else
                Error.Error("Couldn't find float texture named \"%s\" for parameter \"%s\"", texname, name);
        }
        float val = geomParams.FindOneFloat(name, materialParams.FindOneFloat(name, def));
        return new ConstantTexture<>(val);
    }
    public Texture<Float> GetFloatTextureOrNull(String name) {
        String texname = geomParams.FindTexture(name);
        if (texname == "") texname = materialParams.FindTexture(name);
        if (texname != "") {
            if (floatTextures.get(texname) != null)
                return floatTextures.get(texname);
            else {
                Error.Error("Couldn't find float texture named \"%s\" for parameter \"%s\"", texname, name);
                return null;
            }
        }
        Float[] val = geomParams.FindFloat(name);
        if (val == null) val = materialParams.FindFloat(name);
        if (val != null) return new ConstantTexture<>(val[0]);
        return null;
    }
    public float FindFloat(String name, float def) {
        return geomParams.FindOneFloat(name, materialParams.FindOneFloat(name, def));
    }
    public String FindString(String name, String def) {
        return geomParams.FindOneString(name, materialParams.FindOneString(name, def));
    }
    public String FindFilename(String name, String def) {
        return geomParams.FindOneFilename(name, materialParams.FindOneFilename(name, def));
    }
    public int FindInt(String name, int def) {
        return geomParams.FindOneInt(name, materialParams.FindOneInt(name, def));
    }
    public boolean FindBool(String name, boolean def) {
        return geomParams.FindOneBoolean(name, materialParams.FindOneBoolean(name, def));
    }
    public Point3f FindPoint3f(String name, Point3f def) {
        return geomParams.FindOnePoint3f(name, materialParams.FindOnePoint3f(name, def));
    }
    public Vector3f FindVector3f(String name, Vector3f def) {
        return geomParams.FindOneVector3f(name, materialParams.FindOneVector3f(name, def));
    }
    public Normal3f FindNormal3f(String name, Normal3f def) {
        return geomParams.FindOneNormal3f(name, materialParams.FindOneNormal3f(name, def));
    }
    public Spectrum FindSpectrum(String name, Spectrum def) {
        return geomParams.FindOneSpectrum(name, materialParams.FindOneSpectrum(name, def));
    }
    public void ReportUnused() {
        geomParams.ReportUnused();
        materialParams.ReportUnused();
    }
    public ParamSet GetGeomParams() { return geomParams; }
    public ParamSet GetMaterialParams() { return materialParams; }

}