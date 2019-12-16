
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.textures;

import org.pbrt.core.*;
import org.pbrt.core.PBrtTLogger;
import java.util.HashMap;
import java.util.Objects;

public class ImageTextureSpectrum extends Texture<Spectrum> {

    public static ImageTextureSpectrum CreateSpectrum(Transform tex2world, TextureParams tp) {
        // Initialize 2D texture mapping _map_ from _tp_
        TextureMapping2D map;
        String type = tp.FindString("mapping", "uv");
        if (Objects.equals(type, "uv")) {
            float su = tp.FindFloat("uscale", 1);
            float sv = tp.FindFloat("vscale", 1);
            float du = tp.FindFloat("udelta", 0);
            float dv = tp.FindFloat("vdelta", 0);
            map = new UVMapping2D(su, sv, du, dv);
        } else if (Objects.equals(type, "spherical"))
            map = new SphericalMapping2D(Transform.Inverse(tex2world));
        else if (Objects.equals(type, "cylindrical"))
            map = new CylindricalMapping2D(Transform.Inverse(tex2world));
        else if (Objects.equals(type, "planar"))
            map = new PlanarMapping2D(tp.FindVector3f("v1", new Vector3f(1, 0, 0)),
                    tp.FindVector3f("v2", new Vector3f(0, 1, 0)),
                    tp.FindFloat("udelta", 0),
                    tp.FindFloat("vdelta", 0));
        else {
            PBrtTLogger.Error("2D texture mapping \"%s\" unknown", type);
            map = new UVMapping2D();
        }

        // Initialize _ImageTexture_ parameters
        float maxAniso = tp.FindFloat("maxanisotropy", 8.f);
        boolean trilerp = tp.FindBool("trilinear", false);
        String wrap = tp.FindString("wrap", "repeat");
        Texture.ImageWrap wrapMode = Texture.ImageWrap.Repeat;
        if (Objects.equals(wrap, "black"))
            wrapMode = Texture.ImageWrap.Black;
        else if (Objects.equals(wrap, "clamp"))
            wrapMode = Texture.ImageWrap.Clamp;
        float scale = tp.FindFloat("scale", 1);
        String filename = tp.FindFilename("filename", "");
        boolean gamma = tp.FindBool("gamma", FileUtil.HasExtension(filename, ".tga") ||
                FileUtil.HasExtension(filename, ".png"));
        return new ImageTextureSpectrum(map, filename, trilerp, maxAniso, wrapMode, scale, gamma);
    }

    public ImageTextureSpectrum(TextureMapping2D mapping, String filename, boolean doTrilinear, float maxAniso, Texture.ImageWrap wrap, float scale, boolean gamma) {
        super();
        this.mapping = mapping;
        this.mipmap = GetTexture(filename, doTrilinear, maxAniso, wrap, scale, gamma);
    }

    @Override
    public Spectrum Evaluate(SurfaceInteraction si) {
        TextureMapping2D.MapPoint point = mapping.Map(si);
        return mipmap.Lookup(point.st, point.dstdx, point.dstdy);
    }

    private TextureMapping2D mapping;
    private MIPMapSpectrum mipmap;

    private static HashMap<Texture.TexInfo, MIPMapSpectrum> texturesSpectrum = new HashMap<>();

    public static void ClearCacheSpectrum() {
        texturesSpectrum.clear();
    }

    private static MIPMapSpectrum GetTexture(String filename, boolean doTrilinear, float maxAniso,
                                          Texture.ImageWrap wrap, float scale, boolean gamma) {
        // Return _MIPMap_ from texture cache if present
        Texture.TexInfo texInfo = new Texture.TexInfo(filename, doTrilinear, maxAniso, wrap, scale, gamma);
        if (texturesSpectrum.containsKey(texInfo))
            return texturesSpectrum.get(texInfo);

        // Create _MIPMap_ for _filename_
        ImageIO.SpectrumImage image = ImageIO.Read(filename);
        if (image == null) {
            PBrtTLogger.Warning("Creating a constant grey texture to replace \"%s\".", filename);
            image = new ImageIO.SpectrumImage();
            image.resolution.x = image.resolution.y = 1;
            Spectrum[] rgb = new Spectrum[1];
            rgb[0] = new Spectrum(0.5f);
            image.image = rgb;
        }

        // Flip image in y; texture coordinate space has (0,0) at the lower
        // left corner.
        for (int y = 0; y < image.resolution.y / 2; ++y)
            for (int x = 0; x < image.resolution.x; ++x) {
                int o1 = y * image.resolution.x + x;
                int o2 = (image.resolution.y - 1 - y) * image.resolution.x + x;
                Spectrum temp = image.image[o1];
                image.image[o2] = image.image[o1];
                image.image[o1] = temp;
            }

        MIPMapSpectrum mipmap = null;
        if (image.image != null) {
            // Convert texels to type _Tmemory_ and create _MIPMap_
            Spectrum[] convertedTexels = new Spectrum[image.resolution.x * image.resolution.y];
            for (int i = 0; i < image.resolution.x * image.resolution.y; ++i)
                convertedTexels[i] = convertIn(image.image[i], scale, gamma);
            mipmap = new MIPMapSpectrum(image.resolution, convertedTexels, doTrilinear, maxAniso, wrap, new Spectrum(0));
        } else {
            // Create one-valued _MIPMap_
            Spectrum[] oneVal = { new Spectrum(scale) };
            mipmap = new MIPMapSpectrum(new Point2i(1, 1), oneVal, doTrilinear, maxAniso, wrap, new Spectrum(0));
        }
        texturesSpectrum.put(texInfo, mipmap);
        return mipmap;
    }

    private static Spectrum convertIn(Spectrum from, float scale, boolean gamma) {
        return (gamma ? from.scale(scale) : from.scale(scale)); // TODO: what about gamma?
    }

}