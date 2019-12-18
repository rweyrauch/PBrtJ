/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */
package org.pbrt;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import org.pbrt.accelerators.BVHAccel;
import org.pbrt.cameras.OrthographicCamera;
import org.pbrt.cameras.PerspectiveCamera;
import org.pbrt.core.AnimatedTransform;
import org.pbrt.core.Api;
import org.pbrt.core.AreaLight;
import org.pbrt.core.Bounds2f;
import org.pbrt.core.Bounds2i;
import org.pbrt.core.Camera;
import org.pbrt.core.Film;
import org.pbrt.core.Filter;
import org.pbrt.core.GeometricPrimitive;
import org.pbrt.core.Integrator;
import org.pbrt.core.Light;
import org.pbrt.core.Material;
import org.pbrt.core.MediumInterface;
import org.pbrt.core.Options;
import org.pbrt.core.Pbrt;
import org.pbrt.core.Point2f;
import org.pbrt.core.Point2i;
import org.pbrt.core.Primitive;
import org.pbrt.core.Sampler;
import org.pbrt.core.Scene;
import org.pbrt.core.Shape;
import org.pbrt.core.Spectrum;
import org.pbrt.core.Texture;
import org.pbrt.core.Transform;
import org.pbrt.core.Vector2f;
import org.pbrt.filters.BoxFilter;
import org.pbrt.integrators.BDPTIntegrator;
import org.pbrt.integrators.MLTIntegrator;
import org.pbrt.integrators.PathIntegrator;
import org.pbrt.integrators.VolPathIntegrator;
import org.pbrt.lights.DiffuseAreaLight;
import org.pbrt.lights.PointLight;
import org.pbrt.materials.MatteMaterial;
import org.pbrt.materials.UberMaterial;
import org.pbrt.samplers.HaltonSampler;
import org.pbrt.samplers.RandomSampler;
import org.pbrt.samplers.SobolSampler;
import org.pbrt.samplers.StratifiedSampler;
import org.pbrt.samplers.ZeroTwoSequence;
import org.pbrt.shapes.Sphere;
import org.pbrt.core.TextureFloat;
import org.pbrt.core.TextureSpectrum;
import org.pbrt.textures.ConstantTextureFloat;
import org.pbrt.textures.ConstantTextureSpectrum;

public class SceneTest {

    private static float epsilon = 1.0e-6f;

     static class TestScene {
        TestScene(Scene scene, String description, float expected) {
            this.scene = scene;
            this.description = description;
            this.expected = expected;
        }
        Scene scene;
        String description;
        float expected;
    }

    static class TestIntegrator {
        TestIntegrator(Integrator integrator, Film film, String desc, TestScene scene) {
            this.integrator = integrator;
            this.film = film;
            this.description = desc;
            this.scene = scene;
        }

        Integrator integrator;
        Film film;
        String description;
        TestScene scene;
    }

    @Test
    public void testRenderTestRadianceMatches() {
        Options options = new Options();
        options.Quiet = true;
        Api.pbrtInit(options);

        float delta = 0.2f;
        var tests = GetIntegrators();
        for (TestIntegrator tr : tests) {
            try {
                tr.integrator.Render(tr.scene.scene);
                float avg = GetSceneAverage("test.exr");
                if (!Pbrt.AlmostEqual(tr.scene.expected, avg, delta)) {
                    System.out.format("Render test failed, %s.  Got: %f Expected: %f\n", tr.description, avg, tr.scene.expected);
                }
            }
            catch (Exception ex) {
                System.out.format("Render test failed, %s, exception.\n", tr.description);
                ex.printStackTrace();
            }
        }
        Api.pbrtCleanup();
    }

    static ArrayList<TestScene> GetScenes() {
        ArrayList<TestScene> scenes = new ArrayList<>();

        try {
            // Unit sphere, Kd = 0.5, point light I = 3.1415 at center
            // -> With GI, should have radiance of 1.
            Transform id = new Transform();
            Shape sphere = new Sphere(id, id, true /* reverse orientation */, 1, -1, 1, 360);

            TextureSpectrum Kd = new ConstantTextureSpectrum(new Spectrum(0.5f));
            TextureFloat sigma = new ConstantTextureFloat(0.0f);
            Material material = new MatteMaterial(Kd, sigma, null);

            MediumInterface mediumInterface = new MediumInterface();
            Primitive[] prims = new Primitive[1];
            prims[0] = new GeometricPrimitive(sphere, material, null, mediumInterface);
            BVHAccel bvh = new BVHAccel(prims);

            ArrayList<Light> lights = new ArrayList<>();
            lights.add(new PointLight(new Transform(), null, new Spectrum(Pbrt.Pi)));

            Scene scene = new Scene(bvh, lights);

            scenes.add(new TestScene(scene, "Sphere, 1 light, Kd = 0.5", 1.0f));
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            // Unit sphere, Kd = 0.5, 4 point lights I = 3.1415/4 at center
            // -> With GI, should have radiance of 1.
            Transform id = new Transform();
            Shape sphere = new Sphere(id, id, true /* reverse orientation */, 1, -1, 1, 360);

            TextureSpectrum Kd = new ConstantTextureSpectrum(new Spectrum(0.5f));
            TextureFloat sigma = new ConstantTextureFloat(0.0f);
            Material material = new MatteMaterial(Kd, sigma, null);

            MediumInterface mediumInterface = new MediumInterface();
            Primitive[] prims = new Primitive[1];
            prims[0] = new GeometricPrimitive(sphere, material, null, mediumInterface);
            BVHAccel bvh = new BVHAccel(prims);

            ArrayList<Light> lights = new ArrayList<>();
            lights.add(new PointLight(new Transform(), null, new Spectrum((float)(Pbrt.Pi / 4))));
            lights.add(new PointLight(new Transform(), null, new Spectrum((float)(Pbrt.Pi / 4))));
            lights.add(new PointLight(new Transform(), null, new Spectrum((float)(Pbrt.Pi / 4))));
            lights.add(new PointLight(new Transform(), null, new Spectrum((float)(Pbrt.Pi / 4))));

            Scene scene = new Scene(bvh, lights);

            scenes.add(new TestScene(scene, "Sphere, 1 light, Kd = 0.5", 1.0f));
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            // Unit sphere, Kd = 0.5, Le = 0.5
            // -> With GI, should have radiance of 1.
            Transform id = new Transform();
            Shape sphere = new Sphere(id, id, true /* reverse orientation */, 1, -1, 1, 360);

            TextureSpectrum Kd = new ConstantTextureSpectrum(new Spectrum(0.5f));
            TextureFloat sigma = new ConstantTextureFloat(0.0f);
            Material material = new MatteMaterial(Kd, sigma, null);

            AreaLight areaLight = new DiffuseAreaLight(new Transform(), null, new Spectrum(0.5f), 1, sphere);

            ArrayList<Light> lights = new ArrayList<>();
            lights.add(areaLight);

            MediumInterface mediumInterface = new MediumInterface();
            Primitive[] prims = new Primitive[1];
            prims[0] = new GeometricPrimitive(sphere, material, null, mediumInterface);
            BVHAccel bvh = new BVHAccel(prims);

            Scene scene = new Scene(bvh, lights);

            scenes.add(new TestScene(scene, "Sphere, Kd = 0.5, Le = 0.5", 1.0f));
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            // Unit sphere, Kd = 0.25, Kr = .5, point light I = 7.4 at center
            // -> With GI, should have radiance of ~1.
            Transform id = new Transform();
            Shape sphere = new Sphere(id, id, true /* reverse orientation */, 1, -1, 1, 360);

            TextureSpectrum Kd = new ConstantTextureSpectrum(new Spectrum(0.25f));
            TextureSpectrum Kr = new ConstantTextureSpectrum(new Spectrum(0.5f));
            TextureSpectrum black = new ConstantTextureSpectrum(new Spectrum(0.0f));
            TextureSpectrum white = new ConstantTextureSpectrum(new Spectrum(1.0f));
            TextureFloat zero = new ConstantTextureFloat(0.0f);
            TextureFloat one = new ConstantTextureFloat(1.0f);
            Material material = new UberMaterial(Kd, black, Kr, black, zero, zero, zero, white, one, null, false);

            MediumInterface mediumInterface = new MediumInterface();
            Primitive[] prims = new Primitive[1];
            prims[0] = new GeometricPrimitive(sphere, material, null, mediumInterface);
            BVHAccel bvh = new BVHAccel(prims);

            ArrayList<Light> lights = new ArrayList<>();
            lights.add(new PointLight(new Transform(), null, new Spectrum((float)(3 * Pbrt.Pi))));

            Scene scene = new Scene(bvh, lights);

            scenes.add(new TestScene(scene, "Sphere, 1 light, Kd = 0.25 Kr = 0.5", 1.0f));
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        return scenes;
    }

    static class PairSamplerString {
        PairSamplerString(Sampler sampler, String desc) {
            this.first = sampler;
            this.second = desc;
        }
        Sampler first;
        String second;
    }
    static ArrayList<PairSamplerString> GetSamplers(Bounds2i sampleBounds) {
        ArrayList<PairSamplerString> samplers = new ArrayList<>();

        samplers.add(new PairSamplerString(new HaltonSampler(256, sampleBounds), "Halton 256"));
        samplers.add(new PairSamplerString(new ZeroTwoSequence(256, 4), "(0,2)-seq 256"));
        samplers.add(new PairSamplerString(new SobolSampler(256, sampleBounds), "Sobol 256"));
        samplers.add(new PairSamplerString(new RandomSampler(256), "Random 256"));
        samplers.add(new PairSamplerString(new StratifiedSampler(16, 16, true, 8), "Stratified 16x16"));

        return samplers;
    }

    static ArrayList<TestIntegrator> GetIntegrators() {
        ArrayList<TestIntegrator> integrators = new ArrayList<>();

        Point2i resolution = new Point2i(10, 10);
        AnimatedTransform identity = new AnimatedTransform(new Transform(), 0, new Transform(), 1);

        for (TestScene scene : GetScenes()) {
            // Path tracing integrators
            for (PairSamplerString sampler : GetSamplers(new Bounds2i(new Point2i(0, 0), resolution))) {
                Filter filter = new BoxFilter(new Vector2f(0.5f, 0.5f));
                Film film = new Film(resolution, new Bounds2f(new Point2f(0, 0), new Point2f(1, 1)), filter, 1.0f, "test.exr", 1.0f, Pbrt.Infinity);
                Camera camera = new PerspectiveCamera(identity, new Bounds2f(new Point2f(-1, -1), new Point2f(1, 1)), 0, 1, 0, 10, 45, film, null);

                Integrator integrator = new PathIntegrator(8, camera, sampler.first, film.croppedPixelBounds, 1.0f, "spatial");
                integrators.add(new TestIntegrator(integrator, film, "Path, depth 8, Perspective, " + sampler.second + ", " + scene.description, scene));
            }

            for (PairSamplerString sampler : GetSamplers(new Bounds2i(new Point2i(0, 0), resolution))) {
                Filter filter = new BoxFilter(new Vector2f(0.5f, 0.5f));
                Film film = new Film(resolution, new Bounds2f(new Point2f(0, 0), new Point2f(1, 1)), filter, 1.0f, "test.exr", 1.0f, Pbrt.Infinity);
                Camera camera = new OrthographicCamera(identity, new Bounds2f(new Point2f(-0.1f, -0.1f), new Point2f(0.1f, 0.1f)), 0, 1.0f, 0, 10, film, null);

                Integrator integrator = new PathIntegrator(8, camera, sampler.first, film.croppedPixelBounds, 1.0f, "spatial");
                integrators.add(new TestIntegrator(integrator, film,
                    "Path, depth 8, Ortho, " + sampler.second + ", " + scene.description, scene));
            }

            // Volume path tracing integrators
            for (PairSamplerString sampler : GetSamplers(new Bounds2i(new Point2i(0, 0), resolution))) {
                Filter filter = new BoxFilter(new Vector2f(0.5f, 0.5f));
                Film film = new Film(resolution, new Bounds2f(new Point2f(0, 0), new Point2f(1, 1)), filter, 1, "test.exr", 1, Pbrt.Infinity);
                Camera camera = new PerspectiveCamera(identity, new Bounds2f(new Point2f(-1, -1), new Point2f(1, 1)), 0, 1, 0, 10, 45, film, null);

                Integrator integrator = new VolPathIntegrator(8, camera, sampler.first, film.croppedPixelBounds, 1.0f, "spatial");
                integrators.add(new TestIntegrator(integrator, film, "VolPath, depth 8, Perspective, " +
                            sampler.second + ", " + scene.description, scene));
            }

            for (PairSamplerString sampler : GetSamplers(new Bounds2i(new Point2i(0, 0), resolution))) {
                Filter filter = new BoxFilter(new Vector2f(0.5f, 0.5f));
                Film film = new Film(resolution, new Bounds2f(new Point2f(0, 0), new Point2f(1, 1)), filter, 1.0f, "test.exr", 1.0f, Pbrt.Infinity);
                Camera camera = new OrthographicCamera(identity, new Bounds2f(new Point2f(-0.1f, -0.1f), new Point2f(0.1f, 0.1f)), 0, 1, 0, 10, film, null);

                Integrator integrator = new VolPathIntegrator(8, camera, sampler.first, film.croppedPixelBounds, 1.0f, "spatial");
                integrators.add(new TestIntegrator(integrator, film,
                    "VolPath, depth 8, Ortho, " + sampler.second + ", " + scene.description, scene));
            }

            // BDPT
            for (PairSamplerString sampler : GetSamplers(new Bounds2i(new Point2i(0, 0), resolution))) {
                Filter filter = new BoxFilter(new Vector2f(0.5f, 0.5f));
                Film film = new Film(resolution, new Bounds2f(new Point2f(0, 0), new Point2f(1, 1)), filter, 1, "test.exr", 1, Pbrt.Infinity);
                Camera camera = new PerspectiveCamera(identity, new Bounds2f(new Point2f(-1, -1), new Point2f(1, 1)), 0.0f, 1.0f, 0.0f, 10.0f, 45, film, null);

                Integrator integrator = new BDPTIntegrator(sampler.first, camera, 6, false, false, film.croppedPixelBounds, "power");
                integrators.add(new TestIntegrator(integrator, film, "BDPT, depth 8, Perspective, " + sampler.second + ", " + scene.description, scene));
            }

            // Ortho camera not currently supported with BDPT.
            for (PairSamplerString sampler : GetSamplers(new Bounds2i(new Point2i(0,0), resolution))) {
                Filter filter = new BoxFilter(new Vector2f(0.5f, 0.5f));
                Film film = new Film(resolution, new Bounds2f(new Point2f(0,0), new Point2f(1,1)), filter, 1.0f, "test.exr", 1.0f, Pbrt.Infinity);
                Camera camera = new OrthographicCamera(identity, new Bounds2f(new Point2f(-0.1f,-0.1f), new Point2f(0.1f,0.1f)), 0.0f, 1.0f, 0.0f, 10.0f, film, null);

                Integrator integrator = new BDPTIntegrator(sampler.first, camera, 8, false, false, film.croppedPixelBounds, "power");
                integrators.add(new TestIntegrator(integrator, film, "BDPT, depth 8, Ortho, " + sampler.second + ", " + scene.description, scene));
            }

            // MLT
            try {
                Filter filter = new BoxFilter(new Vector2f(0.5f, 0.5f));
                Film film = new Film(resolution, new Bounds2f(new Point2f(0, 0), new Point2f(1, 1)), filter, 1.0f, "test.exr", 1.0f, Pbrt.Infinity);
                Camera camera = new PerspectiveCamera(identity, new Bounds2f(new Point2f(-1, -1), new Point2f(1, 1)), 0.0f, 1.0f, 0.0f, 10.0f, 45, film, null);

                Integrator integrator = new MLTIntegrator(camera, 8 /* depth */, 100000 /* n bootstrap */,
                        1000 /* nchains */, 1024 /* mutations per pixel */,
                        0.01f /* sigma */, 0.3f /* large step prob */);
                integrators.add(new TestIntegrator(integrator, film, "MLT, depth 8, Perspective, " + scene.description, scene));
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return integrators;
    }

    static float GetSceneAverage(String filename) {
        org.pbrt.core.ImageIO.SpectrumImage si = org.pbrt.core.ImageIO.Read(filename);
        assertNotNull(si);

        float sum = 0;

        for (int i = 0; i < si.resolution.x * si.resolution.y; ++i) {
            for (int c = 0; c < 3; ++c) sum += si.image[i].at(c);
        }
        int nPixels = si.resolution.x * si.resolution.y * 3;
        //assertTrue(Pbrt.AlmostEqual(expected, sum / nPixels, delta))
        return sum / nPixels;
    }

}
