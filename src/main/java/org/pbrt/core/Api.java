
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pbrt.accelerators.BVHAccel;
import org.pbrt.accelerators.KdTreeAccel;
import org.pbrt.accelerators.NoAccel;
import org.pbrt.cameras.EnvironmentCamera;
import org.pbrt.cameras.OrthographicCamera;
import org.pbrt.cameras.PerspectiveCamera;
import org.pbrt.cameras.RealisticCamera;
import org.pbrt.filters.*;
import org.pbrt.integrators.*;
import org.pbrt.lights.*;
import org.pbrt.materials.*;
import org.pbrt.media.GridDensityMedium;
import org.pbrt.media.HomogeneousMedium;
import org.pbrt.samplers.*;
import org.pbrt.shapes.*;
import org.pbrt.textures.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Stack;

public class Api {

    public static final Logger logger = LogManager.getFormatterLogger("Pbrt");

    private static final int MaxTransforms = 2;
    private static final int StartTransformBits = 1 << 0;
    private static final int EndTransformBits = 1 << 1;
    private static final int AllTransformsBits = (1 << MaxTransforms) - 1;

    private static class TransformSet {
        // TransformSet Public Methods
        public TransformSet() {
            trans[0] = new Transform();
            trans[1] = new Transform();
        }
        public TransformSet(TransformSet ts) {
            this.trans[0] = new Transform(ts.trans[0]);
            this.trans[1] = new Transform(ts.trans[1]);
        }
        public static TransformSet Inverse(TransformSet ts) {
            TransformSet tInv = new TransformSet();
            for (int i = 0; i < MaxTransforms; ++i) tInv.trans[i] = Transform.Inverse(ts.trans[i]);
            return tInv;
        }

        public boolean IsAnimated() {
            for (int i = 0; i < MaxTransforms - 1; ++i) {
                if (trans[i].notEqual(trans[i + 1])) return true;
            }
            return false;
        }

        public Transform trans[] = new Transform[MaxTransforms];
    }

    private static class RenderOptions {
        // RenderOptions Public Methods
        public Integrator MakeIntegrator() {
            Camera camera = MakeCamera();
            if (camera == null) {
                Error.Error("Unable to create camera");
                return null;
            }

            Sampler sampler = MakeSampler(SamplerName, SamplerParams, camera.film);
            if (sampler == null) {
                Error.Error("Unable to create sampler.");
                return null;
            }

            Integrator integrator = null;
            if (Objects.equals(IntegratorName, "whitted")) {
                integrator = WhittedIntegrator.Create(IntegratorParams, sampler, camera);
            } else if (Objects.equals(IntegratorName, "directlighting")) {
                integrator = DirectLightingIntegrator.Create(IntegratorParams, sampler, camera);
            } else if (Objects.equals(IntegratorName, "path")) {
                integrator = PathIntegrator.Create(IntegratorParams, sampler, camera);
            } else if (Objects.equals(IntegratorName, "volpath")) {
                integrator = VolPathIntegrator.Create(IntegratorParams, sampler, camera);
            } else if (Objects.equals(IntegratorName, "bdpt")) {
                integrator = BDPTIntegrator.Create(IntegratorParams, sampler, camera);
            } else if (Objects.equals(IntegratorName, "mlt")) {
                integrator = MLTIntegrator.Create(IntegratorParams, camera);
            } else if (Objects.equals(IntegratorName, "sppm")) {
                integrator = SPPMIntegrator.Create(IntegratorParams, camera);
            } else {
                Error.Error("Integrator \"%s\" unknown.", IntegratorName);
                return null;
            }

            if (renderOptions.haveScatteringMedia && !Objects.equals(IntegratorName, "volpath") &&
                    !Objects.equals(IntegratorName, "bdpt") && !Objects.equals(IntegratorName, "mlt")) {
                Error.Warning("Scene has scattering media but \"%s\" integrator doesn't support "+
                        "volume scattering. Consider using \"volpath\", \"bdpt\", or "+
                        "\"mlt\".", IntegratorName);
            }

            IntegratorParams.ReportUnused();
            // Warn if no light sources are defined
            if (lights.isEmpty()) {
                Error.Warning("No light sources defined in scene; rendering a black image.");
            }
            return integrator;
        }

        public Scene MakeScene() {
            Primitive[] primArray = new Primitive[1];
            Primitive[] prims = primitives.toArray(primArray);
            Primitive accelerator = MakeAccelerator(AcceleratorName, prims, AcceleratorParams);
            if (accelerator == null) {
                accelerator = new BVHAccel(prims);
            }
            Scene scene = new Scene(accelerator, lights);
            // Erase primitives and lights from _RenderOptions_
            primitives.clear();
            lights.clear();
            return scene;
        }

        public Camera MakeCamera() {
            Filter filter = MakeFilter(FilterName, FilterParams);
            Film film = MakeFilm(FilmName, FilmParams, filter);
            if (film == null) {
                Error.Error("Unable to create film.");
                return null;
            }
            Camera camera = Api.MakeCamera(CameraName, CameraParams, CameraToWorld,
                    renderOptions.transformStartTime, renderOptions.transformEndTime, film);
            return camera;
        }

        // RenderOptions Public Data
        public float transformStartTime = 0, transformEndTime = 1;
        public String FilterName = "box";
        public ParamSet FilterParams = new ParamSet();
        public String FilmName = "image";
        public ParamSet FilmParams = new ParamSet();
        public String SamplerName = "halton";
        public ParamSet SamplerParams = new ParamSet();
        public String AcceleratorName = "bvh";
        public ParamSet AcceleratorParams = new ParamSet();
        public String IntegratorName = "path";
        public ParamSet IntegratorParams = new ParamSet();
        public String CameraName = "perspective";
        public ParamSet CameraParams = new ParamSet();
        public TransformSet CameraToWorld = new TransformSet();
        public HashMap<String, Medium> namedMedia = new HashMap<>();
        public ArrayList<Light> lights = new ArrayList<>();
        public ArrayList<Primitive> primitives = new ArrayList<>();
        public HashMap<String, ArrayList<Primitive>> instances = new HashMap<>();
        public ArrayList<Primitive> currentInstance = null;
        boolean haveScatteringMedia = false;
    }

    private static class GraphicsState {
        public GraphicsState() {}

        public GraphicsState(GraphicsState gs) {
            this.currentInsideMedium = gs.currentInsideMedium;
            this.currentOutsideMedium = gs.currentOutsideMedium;
            this.floatTextures = new HashMap<>(gs.floatTextures);
            this.spectrumTextures = new HashMap<>(gs.spectrumTextures);
            this.materialParams = new ParamSet(gs.materialParams);
            this.material = gs.material;
            this.namedMaterials = new HashMap<>(gs.namedMaterials);
            this.currentNamedMaterial = gs.currentNamedMaterial;
            this.areaLightParams = new ParamSet(gs.areaLightParams);
            this.areaLight = gs.areaLight;
            this.reverseOrientation = gs.reverseOrientation;
        }

        // Graphics State Methods
        public Material CreateMaterial(ParamSet params) {
            TextureParams mp = new TextureParams(params, materialParams, floatTextures, spectrumTextures);
            Material mtl;
            if (!Objects.equals(currentNamedMaterial, "")) {
                mtl = namedMaterials.get(currentNamedMaterial);
                if (mtl == null) {
                    Error.Error("Named material \"%s\" not defined. Using \"matte\".", currentNamedMaterial);
                    mtl = MakeMaterial("matte", mp);
                }
            } else {
                mtl = MakeMaterial(material, mp);
                if (mtl == null && !Objects.equals(material, "") && !Objects.equals(material, "none"))
                    mtl = MakeMaterial("matte", mp);
            }
            return mtl;
        }

        public MediumInterface CreateMediumInterface() {
            MediumInterface m = new MediumInterface();
            if (!currentInsideMedium.isEmpty()) {
                m.inside = renderOptions.namedMedia.get(currentInsideMedium);
                if (m.inside == null) {
                    Error.Error("Named medium \"%s\" undefined.", currentInsideMedium);
                }
            }
            if (!currentOutsideMedium.isEmpty()) {
                m.outside = renderOptions.namedMedia.get(currentOutsideMedium);
                if (m.outside == null) {
                    Error.Error("Named medium \"%s\" undefined.", currentOutsideMedium);
                }
            }
            return m;
        }

        // Graphics State
        public String currentInsideMedium = "", currentOutsideMedium = "";
        public HashMap<String, Texture<Float>> floatTextures = new HashMap<>();
        public HashMap<String, Texture<Spectrum>> spectrumTextures = new HashMap<>();
        public ParamSet materialParams = new ParamSet();
        public String material = "matte";
        public HashMap<String, Material> namedMaterials = new HashMap<>();
        public String currentNamedMaterial = "";
        public ParamSet areaLightParams = new ParamSet();
        public String areaLight = "";
        public boolean reverseOrientation = false;
    }

    private static class TransformCache {

        public class TransformPair {
            Transform t;
            Transform tInv;
        }

        // TransformCache Public Methods
        public TransformPair Lookup(Transform t) {
            TransformPair entry = cache.get(t);
            if (entry == null) {
                entry = new TransformPair();
                entry.t = t;
                entry.tInv = Transform.Inverse(t);
                cache.put(t, entry);
            }
            return entry;
        }

        public void Clear() {
            cache.clear();
        }

        // TransformCache Private Data
        private HashMap<Transform, TransformPair> cache = new HashMap<>();
    }

    private enum APIState {Uninitialized, OptionsBlock, WorldBlock}


    private static APIState currentApiState = APIState.Uninitialized;
    private static TransformSet curTransform = new TransformSet();
    private static int activeTransformBits = AllTransformsBits;
    private static HashMap<String, TransformSet> namedCoordinateSystems = new HashMap<>();
    private static RenderOptions renderOptions = new RenderOptions();
    private static GraphicsState graphicsState = new GraphicsState();
    private static Stack<GraphicsState> pushedGraphicsStates = new Stack<>();
    private static Stack<TransformSet> pushedTransforms = new Stack<>();
    private static Stack<Integer> pushedActiveTransformBits = new Stack<>();
    private static TransformCache transformCache = new TransformCache();
    private static int catIndentCount = 0;

    private static ArrayList<Shape> MakeShapes(String name, Transform object2world, Transform world2object, boolean reverseOrientation, ParamSet paramSet) {
        ArrayList<Shape> shapes = new ArrayList<>();
        Shape s = null;
        if (Objects.equals(name, "sphere")) {
            s = Sphere.Create(object2world, world2object, reverseOrientation, paramSet);
        }
        else if (Objects.equals(name, "cylinder")) {
            s = Cylinder.Create(object2world, world2object, reverseOrientation, paramSet);
        } else if (Objects.equals(name, "disk")) {
            s = Disk.Create(object2world, world2object, reverseOrientation, paramSet);
        } else if (Objects.equals(name, "cone")) {
            s = Cone.Create(object2world, world2object, reverseOrientation, paramSet);
        } else if (Objects.equals(name, "paraboloid")) {
            s = Paraboloid.Create(object2world, world2object, reverseOrientation, paramSet);
        } else if (Objects.equals(name, "hyperboloid")) {
            s = Hyperboloid.Create(object2world, world2object, reverseOrientation, paramSet);
        }

        if (s != null) {
            shapes.add(s);
        }
        else if (Objects.equals(name, "curve")) {
            shapes.addAll(Curve.Create(object2world, world2object, reverseOrientation, paramSet));
        } else if (Objects.equals(name, "trianglemesh")) {
            if (Pbrt.options.ToPly) {
                /*
                 int count = 1;
                 String plyPrefix = new String(); // getenv("PLY_PREFIX") ? getenv("PLY_PREFIX") : "mesh";
                 String fn = StringPrintf("%s_%05d.ply", plyPrefix, count++);

                Integer[] vi = paramSet.FindInt("indices");
                Point3f[] P = paramSet.FindPoint3f("P");
                Point2f[] uvs = paramSet.FindPoint2f("uv");
                if (uvs == null) uvs = paramSet.FindPoint2f("st");
                if (uvs == null) {
                    Float[] fuv = paramSet.FindFloat("uv");
                    if (fuv == null) fuv = paramSet.FindFloat("st");
                    if (fuv != null) {
                        Point2f[] tempUVs = new Point2f[uvs.length/2];
                        for (int i = 0; i < tempUVs.length; ++i)
                            tempUVs[i] = new Point2f(fuv[2 * i], fuv[2 * i + 1]);
                        uvs = tempUVs;
                    }
                }
                Normal3f[] N = paramSet.FindNormal3f("N");
                Vector3f[] S = paramSet.FindVector3f("S");

                if (!WritePlyFile(fn, nvi / 3, vi, npi, P, S, N, uvs))
                    Error.Error("Unable to write PLY file \"%s\"", fn);

                System.out.format("%*sShape \"plymesh\" \"string filename\" \"%s\" ",
                        catIndentCount, "", fn);

                String alphaTex = paramSet.FindTexture("alpha");
                if (alphaTex != "")
                    System.out.format("\n%*s\"texture alpha\" \"%s\" ", catIndentCount + 8, "", alphaTex);
                else {
                    int count;
                    float[] alpha = paramSet.FindFloat("alpha");
                    if (alpha != null)
                        System.out.format("\n%*s\"float alpha\" %f ", catIndentCount + 8, "", alpha[0]);
                }

                String shadowAlphaTex = paramSet.FindTexture("shadowalpha");
                if (shadowAlphaTex != "")
                    System.out.format("\n%*s\"texture shadowalpha\" \"%s\" ",
                            catIndentCount + 8, "", shadowAlphaTex);
                else {
                    int count;
                    float[] alpha = paramSet.FindFloat("shadowalpha");
                    if (alpha != null)
                        System.out.format("\n%*s\"float shadowalpha\" %f ", catIndentCount + 8, "", alpha[0]);
                }
                System.out.format("\n");
                */
            } else {
                shapes.addAll(Triangle.Create(object2world, world2object, reverseOrientation, paramSet, graphicsState.floatTextures));
            }
        } else if (Objects.equals(name, "plymesh")) {
            shapes.addAll(PlyMesh.Create(object2world, world2object, reverseOrientation, paramSet, graphicsState.floatTextures));
        } else if (Objects.equals(name, "heightfield")) {
            shapes.addAll(HeightField.Create(object2world, world2object, reverseOrientation, paramSet));
        } else if (Objects.equals(name, "loopsubdiv")) {
            shapes.addAll(LoopSubdiv.Create(object2world, world2object, reverseOrientation, paramSet));
        } else if (Objects.equals(name, "nurbs")) {
            shapes.addAll(NURBS.Create(object2world, world2object, reverseOrientation, paramSet));
        } else {
            Error.Warning("Shape \"%s\" unknown.", name);
        }
        paramSet.ReportUnused();
        return shapes;
    }

    private static Stats.Counter nMaterialsCreated = new Stats.Counter("Scene/Materials created");

    private static Material MakeMaterial(String name, TextureParams mp) {
        Material material = null;
        if (Objects.equals(name, "") || Objects.equals(name, "none")) {
            return null;
        } else if (Objects.equals(name, "matte")) {
            material = MatteMaterial.Create(mp);
        } else if (Objects.equals(name, "plastic")) {
            material = PlasticMaterial.Create(mp);
        } else if (Objects.equals(name, "translucent")) {
            material = TranslucentMaterial.Create(mp);
        } else if (Objects.equals(name, "glass")) {
            material = GlassMaterial.Create(mp);
        } else if (Objects.equals(name, "mirror")) {
            material = MirrorMaterial.Create(mp);
        } else if (Objects.equals(name, "hair")) {
            material = HairMaterial.Create(mp);
        } else if (Objects.equals(name, "mix")) {
            String m1 = mp.FindString("namedmaterial1", "");
            String m2 = mp.FindString("namedmaterial2", "");
            Material mat1 = graphicsState.namedMaterials.get(m1);
            Material mat2 = graphicsState.namedMaterials.get(m2);
            if (mat1 == null) {
                Error.Error("Named material \"%s\" undefined.  Using \"matte\"", m1);
                mat1 = MakeMaterial("matte", mp);
            }
            if (mat2 == null) {
                Error.Error("Named material \"%s\" undefined.  Using \"matte\"", m2);
                mat2 = MakeMaterial("matte", mp);
            }

            material = MixMaterial.Create(mp, mat1, mat2);
        } else if (Objects.equals(name, "metal")) {
            material = MetalMaterial.Create(mp);
        } else if (Objects.equals(name, "substrate")) {
            material = SubstrateMaterial.Create(mp);
        } else if (Objects.equals(name, "uber")) {
            material = UberMaterial.Create(mp);
        } else if (Objects.equals(name, "subsurface")) {
            material = SubsurfaceMaterial.Create(mp);
        } else if (Objects.equals(name, "kdsubsurface")) {
            material = KdSubsurfaceMaterial.Create(mp);
        } else if (Objects.equals(name, "fourier")) {
            material = FourierMaterial.Create(mp);
        } else {
            Error.Warning("Material \"%s\" unknown. Using \"matte\".", name);
            material = MatteMaterial.Create(mp);
        }

        if ((Objects.equals(name, "subsurface") || Objects.equals(name, "kdsubsurface")) && (!Objects.equals(renderOptions.IntegratorName, "path") && (!Objects.equals(renderOptions.IntegratorName, "volpath")))) {
            Error.Warning("Subsurface scattering material \"%s\" used, but \"%s\" integrator doesn't support subsurface scattering. Use \"path\" or \"volpath\".",
                    name, renderOptions.IntegratorName);
        }
        mp.ReportUnused();
        if (material == null) {
            Error.Error("Unable to create material \"%s\"", name);
        } else {
            nMaterialsCreated.increment();
        }
        return material;
    }

    private static Texture<Float> MakeFloatTexture(String name, Transform tex2world, TextureParams tp) {
        Texture<Float> tex = null;
        if (Objects.equals(name, "constant"))
            tex = ConstantTexture.CreateFloat(tex2world, tp);
        else if (Objects.equals(name, "scale"))
            tex = ScaleTextureFloat.CreateFloat(tex2world, tp);
        else if (Objects.equals(name, "mix"))
            tex = MixTextureFloat.CreateFloat(tex2world, tp);
        else if (Objects.equals(name, "bilerp"))
            tex = BilerpTextureFloat.CreateFloat(tex2world, tp);
        else if (Objects.equals(name, "imagemap"))
            tex = ImageTextureFloat.CreateFloat(tex2world, tp);
        else if (Objects.equals(name, "checkerboard"))
            tex = CheckerBoardTextureFloat.CreateFloat(tex2world, tp);
        else if (Objects.equals(name, "dots"))
            tex = DotsTextureFloat.CreateFloat(tex2world, tp);
        else if (Objects.equals(name, "fbm"))
            tex = FBmTextureFloat.CreateFloat(tex2world, tp);
        else if (Objects.equals(name, "wrinkled"))
            tex = WrinkledTextureFloat.CreateFloat(tex2world, tp);
        else if (Objects.equals(name, "windy"))
            tex = WindyTextureFloat.CreateFloat(tex2world, tp);
        else
            Error.Warning("Float texture \"%s\" unknown.", name);
        tp.ReportUnused();
        return tex;
    }

    private static Texture<Spectrum> MakeSpectrumTexture(String name, Transform tex2world, TextureParams tp) {
        Texture<Spectrum> tex = null;
        if (Objects.equals(name, "constant"))
            tex = ConstantTexture.CreateSpectrum(tex2world, tp);
        else if (Objects.equals(name, "scale"))
            tex = ScaleTextureSpectrum.CreateSpectrum(tex2world, tp);
        else if (Objects.equals(name, "mix"))
            tex = MixTextureSpectrum.CreateSpectrum(tex2world, tp);
        else if (Objects.equals(name, "bilerp"))
            tex = BilerpTextureSpectrum.CreateSpectrum(tex2world, tp);
        else if (Objects.equals(name, "imagemap"))
            tex = ImageTextureSpectrum.CreateSpectrum(tex2world, tp);
        else if (Objects.equals(name, "uv"))
            tex = UVTextureSpectrum.CreateSpectrum(tex2world, tp);
        else if (Objects.equals(name, "checkerboard"))
            tex = CheckerBoardTextureSpectrum.CreateSpectrum(tex2world, tp);
        else if (Objects.equals(name, "dots"))
            tex = DotsTextureSpectrum.CreateSpectrum(tex2world, tp);
        else if (Objects.equals(name, "fbm"))
            tex = FBmTextureSpectrum.CreateSpectrum(tex2world, tp);
        else if (Objects.equals(name, "wrinkled"))
            tex = WrinkledTextureSpectrum.CreateSpectrum(tex2world, tp);
        else if (Objects.equals(name, "marble"))
            tex = MarbleTextureSpectrum.CreateSpectrum(tex2world, tp);
        else if (Objects.equals(name, "windy"))
            tex = WindyTextureSpectrum.CreateSpectrum(tex2world, tp);
        else
            Error.Warning("Spectrum texture \"%s\" unknown.", name);
        tp.ReportUnused();
        return tex;
    }

    private static Medium MakeMedium(String name, ParamSet paramSet, Transform medium2world) {
        float sig_a_rgb[] = {.0011f, .0024f, .014f}, sig_s_rgb[] = {2.55f, 3.21f, 3.77f};
        Spectrum sig_a = Spectrum.FromRGB(sig_a_rgb),
                sig_s = Spectrum.FromRGB(sig_s_rgb);
        String preset = paramSet.FindOneString("preset", "");
        Medium.ScatteringProps props = Medium.GetMediumScatteringProperties(preset);
        if (props == null) {
            Error.Warning("Material preset \"%s\" not found.  Using defaults.", preset);
        } else {
            sig_a = props.sigma_a;
            sig_s = props.sigma_s;
        }
        float scale = paramSet.FindOneFloat("scale", 1.f);
        float g = paramSet.FindOneFloat("g", 0.0f);
        sig_a = paramSet.FindOneSpectrum("sigma_a", sig_a);
        sig_a.scale(scale);
        sig_s = paramSet.FindOneSpectrum("sigma_s", sig_s);
        sig_s.scale(scale);
        Medium m = null;
        if (Objects.equals(name, "homogeneous")) {
            m = new HomogeneousMedium(sig_a, sig_s, g);
        } else if (Objects.equals(name, "heterogeneous")) {
            Float[] data = paramSet.FindFloat("density");
            if (data == null) {
                Error.Error("No \"density\" values provided for heterogeneous medium?");
                return null;
            }
            int nx = paramSet.FindOneInt("nx", 1);
            int ny = paramSet.FindOneInt("ny", 1);
            int nz = paramSet.FindOneInt("nz", 1);
            Point3f p0 = paramSet.FindOnePoint3f("p0", new Point3f(0.f, 0.f, 0.f));
            Point3f p1 = paramSet.FindOnePoint3f("p1", new Point3f(1.f, 1.f, 1.f));
            if (data.length != nx * ny * nz) {
                Error.Error("GridDensityMedium has %d density values; expected nx*ny*nz = %d", data.length, nx * ny * nz);
                return null;
            }
            Transform data2Medium = Transform.Translate(new Vector3f(p0)).concatenate(Transform.Scale(p1.x - p0.x, p1.y - p0.y, p1.z - p0.z));
            m = new GridDensityMedium(sig_a, sig_s, g, nx, ny, nz, medium2world.concatenate(data2Medium), data);
        } else {
            Error.Warning("Medium \"%s\" unknown.", name);
        }
        paramSet.ReportUnused();
        return m;
    }

    private static Light MakeLight(String name, ParamSet paramSet, Transform light2world, MediumInterface mediumInterface) {
        Light light = null;
        if (Objects.equals(name, "point"))
            light = PointLight.Create(light2world, mediumInterface.outside, paramSet);
        else if (Objects.equals(name, "spot"))
            light = SpotLight.Create(light2world, mediumInterface.outside, paramSet);
        else if (Objects.equals(name, "goniometric"))
            light = GonioPhotometricLight.Create(light2world, mediumInterface.outside, paramSet);
        else if (Objects.equals(name, "projection"))
            light = ProjectionLight.Create(light2world, mediumInterface.outside, paramSet);
        else if (Objects.equals(name, "distant"))
            light = DistantLight.Create(light2world, paramSet);
        else if (Objects.equals(name, "infinite") || Objects.equals(name, "exinfinite"))
            light = InfiniteAreaLight.Create(light2world, paramSet);
        else
            Error.Warning("Light \"%s\" unknown.", name);
        paramSet.ReportUnused();
        return light;
    }

    private static AreaLight MakeAreaLight(String name, Transform light2world, MediumInterface mediumInterface, ParamSet paramSet, Shape shape) {
        AreaLight area = null;
        if (Objects.equals(name, "area") || Objects.equals(name, "diffuse"))
            area = DiffuseAreaLight.Create(light2world, mediumInterface.outside, paramSet, shape);
        else
            Error.Warning("Area light \"%s\" unknown.", name);
        paramSet.ReportUnused();
        return area;
    }

    private static Primitive MakeAccelerator(String name, Primitive[] prims, ParamSet paramSet) {
        Primitive accel = null;
        if (Objects.equals(name, "bvh"))
            accel = BVHAccel.Create(prims, paramSet);
        else if (Objects.equals(name, "kdtree"))
            accel = KdTreeAccel.Create(prims, paramSet);
        else if (Objects.equals(name, "none"))
            accel = NoAccel.Create(prims, paramSet);
        else
            Error.Warning("Accelerator \"%s\" unknown.", name);
        paramSet.ReportUnused();
        return accel;
    }

    private static Camera MakeCamera(String name, ParamSet paramSet, TransformSet cam2worldSet, float transformStart, float transformEnd, Film film) {
        Camera camera = null;
        MediumInterface mediumInterface = graphicsState.CreateMediumInterface();
        TransformCache.TransformPair c2w0 = transformCache.Lookup(cam2worldSet.trans[0]);
        TransformCache.TransformPair c2w1 = transformCache.Lookup(cam2worldSet.trans[1]);
        AnimatedTransform animatedCam2World = new AnimatedTransform(c2w0.t, transformStart, c2w1.t, transformEnd);
        if (Objects.equals(name, "perspective"))
            camera = PerspectiveCamera.Create(paramSet, animatedCam2World, film, mediumInterface.outside);
        else if (Objects.equals(name, "orthographic"))
            camera = OrthographicCamera.Create(paramSet, animatedCam2World, film, mediumInterface.outside);
        else if (Objects.equals(name, "realistic"))
            camera = RealisticCamera.Create(paramSet, animatedCam2World, film, mediumInterface.outside);
        else if (Objects.equals(name, "environment"))
            camera = EnvironmentCamera.Create(paramSet, animatedCam2World, film, mediumInterface.outside);
        else
            Error.Warning("Camera \"%s\" unknown.", name);
        paramSet.ReportUnused();
        return camera;
    }

    private static Sampler MakeSampler(String name, ParamSet paramSet, Film film) {
        Sampler sampler = null;
        if (Objects.equals(name, "lowdiscrepancy") || Objects.equals(name, "02sequence"))
            sampler = ZeroTwoSequence.Create(paramSet);
        else if (Objects.equals(name, "maxmindist"))
            sampler = MaxMinDistSampler.Create(paramSet);
        else if (Objects.equals(name, "halton"))
            sampler = HaltonSampler.Create(paramSet, film.GetSampleBounds());
        else if (Objects.equals(name, "sobol"))
            sampler = SobolSampler.Create(paramSet, film.GetSampleBounds());
        else if (Objects.equals(name, "random"))
            sampler = RandomSampler.Create(paramSet);
        else if (Objects.equals(name, "stratified"))
            sampler = StratifiedSampler.Create(paramSet);
        else
            Error.Warning("Sampler \"%s\" unknown.", name);
        paramSet.ReportUnused();
        return sampler;
    }

    private static Filter MakeFilter(String name, ParamSet paramSet) {
        Filter filter = null;
        if (Objects.equals(name, "box"))
            filter = BoxFilter.Create(paramSet);
        else if (Objects.equals(name, "gaussian"))
            filter = GaussianFilter.Create(paramSet);
        else if (Objects.equals(name, "mitchell"))
            filter = MitchellFilter.Create(paramSet);
        else if (Objects.equals(name, "sinc"))
            filter = LanczosSincFilter.Create(paramSet);
        else if (Objects.equals(name, "triangle"))
            filter = TriangleFilter.Create(paramSet);
        else {
            Error.Error("Filter \"%s\" unknown.", name);
        }
        paramSet.ReportUnused();
        return filter;
    }

    private static Film MakeFilm(String name, ParamSet paramSet, Filter filter) {
        Film film = null;
        if (Objects.equals(name, "image"))
            film = Film.Create(paramSet, filter);
        else
            Error.Warning("Film \"%s\" unknown.", name);

        paramSet.ReportUnused();
        return film;
    }

    // API Function Declarations
    public static void pbrtInit(Options opt) {
        Pbrt.options = opt;

        // API Initialization
        if (currentApiState != APIState.Uninitialized)
            Error.Error("pbrtInit() has already been called.");
        currentApiState = APIState.OptionsBlock;
        renderOptions = new RenderOptions();
        graphicsState = new GraphicsState();
        catIndentCount = 0;

        // General \pbrt Initialization
        //SampledSpectrum.Init();
        //ParallelInit();  // Threads must be launched before the profiler is
        // initialized.
    }

    public static void pbrtCleanup() {
        // API Cleanup
        if (currentApiState == APIState.Uninitialized)
            Error.Error("pbrtCleanup() called without pbrtInit().");
        else if (currentApiState == APIState.WorldBlock)
            Error.Error("pbrtCleanup() called while inside world block.");
        currentApiState = APIState.Uninitialized;
        //ParallelCleanup();
        renderOptions = null;
    }

    private static char[] spaces = new char[]{' '};

    public static void pbrtIdentity() {
        VERIFY_INITIALIZED("Identity");
        for (int i = 0; i < MaxTransforms; ++i) {
            if ((activeTransformBits & (1 << i)) != 0) {
                curTransform.trans[i] = new Transform();
            }
        }
        if (Pbrt.options.Cat || Pbrt.options.ToPly)
            System.out.format("%sIdentity\n", new String(spaces, 0, catIndentCount));
    }

    public static void pbrtTranslate(float dx, float dy, float dz) {
        VERIFY_INITIALIZED("Translate");

        for (int i = 0; i < MaxTransforms; ++i) {
            if ((activeTransformBits & (1 << i)) != 0) {
                curTransform.trans[i] = curTransform.trans[i].concatenate(Transform.Translate(new Vector3f(dx, dy, dz)));
            }
        }
        if (Pbrt.options.Cat || Pbrt.options.ToPly)
            System.out.format("%sTranslate %.9g %.9g %.9g\n", new String(spaces, 0, catIndentCount), dx, dy, dz);
    }

    public static void pbrtRotate(float angle, float ax, float ay, float az) {
        VERIFY_INITIALIZED("Rotate");
        for (int i = 0; i < MaxTransforms; ++i) {
            if ((activeTransformBits & (1 << i)) != 0) {
                curTransform.trans[i] = curTransform.trans[i].concatenate(Transform.Rotate(angle, new Vector3f(ax, ay, az)));
            }
        }
        if (Pbrt.options.Cat || Pbrt.options.ToPly)
            System.out.format("%sRotate %.9g %.9g %.9g %.9g\n", new String(spaces, 0, catIndentCount), angle, ax, ay, az);
    }

    public static void pbrtScale(float sx, float sy, float sz) {
        VERIFY_INITIALIZED("Scale");
        for (int i = 0; i < MaxTransforms; ++i) {
            if ((activeTransformBits & (1 << i)) != 0) {
                curTransform.trans[i] = curTransform.trans[i].concatenate(Transform.Scale(sx, sy, sz));
            }
        }

        if (Pbrt.options.Cat || Pbrt.options.ToPly) {
            System.out.format("%sScale %.9g %.9g %.9g\n", new String(spaces, 0, catIndentCount), sx, sy, sz);
        }
    }

    public static void pbrtLookAt(float ex, float ey, float ez, float lx, float ly, float lz, float ux, float uy, float uz) {
        VERIFY_INITIALIZED("LookAt");
        Transform lookAt = Transform.LookAt(new Point3f(ex, ey, ez), new Point3f(lx, ly, lz), new Vector3f(ux, uy, uz));
        for (int i = 0; i < MaxTransforms; ++i) {
            if ((activeTransformBits & (1 << i)) != 0) {
                curTransform.trans[i] = curTransform.trans[i].concatenate(lookAt);
            }
        }
        if (Pbrt.options.Cat || Pbrt.options.ToPly) {
            System.out.format("%sLookAt %.9g %.9g %.9g\n%s%.9g %.9g %.9g\n%s%.9g %.9g %.9g\n",
                    new String(spaces, 0, catIndentCount), ex, ey, ez, new String(spaces, 0, catIndentCount+8), lx, ly, lz,
                    new String(spaces, 0, catIndentCount+8), ux, uy, uz);
        }
    }

    public static void pbrtConcatTransform(float[] tr) {
        VERIFY_INITIALIZED("ConcatTransform");
        for (int i = 0; i < MaxTransforms; ++i) {
            if ((activeTransformBits & (1 << i)) != 0) {
                curTransform.trans[i] = curTransform.trans[i].concatenate(new Transform(new Matrix4x4(tr[0], tr[4], tr[8], tr[12], tr[1], tr[5],
                                tr[9], tr[13], tr[2], tr[6], tr[10], tr[14],
                                tr[3], tr[7], tr[11], tr[15])));
            }
        }
        if (Pbrt.options.Cat || Pbrt.options.ToPly) {
            System.out.format("%sConcatTransform [ ", new String(spaces, 0, catIndentCount));
            for (int i = 0; i < 16; ++i) System.out.format("%.9g ", tr[i]);
            System.out.format(" ]\n");
        }
    }

    public static void pbrtTransform(float[] tr) {
        VERIFY_INITIALIZED("Transform");
        for (int i = 0; i < MaxTransforms; ++i) {
            if ((activeTransformBits & (1 << i)) != 0) {
                curTransform.trans[i] = new Transform(new Matrix4x4(
                        tr[0], tr[4], tr[8], tr[12], tr[1], tr[5], tr[9], tr[13], tr[2],
                        tr[6], tr[10], tr[14], tr[3], tr[7], tr[11], tr[15]));
            }
        }
        if (Pbrt.options.Cat || Pbrt.options.ToPly) {
            System.out.format("%sTransform [ ", new String(spaces, 0, catIndentCount));
            for (int i = 0; i < 16; ++i) System.out.format("%.9g ", tr[i]);
            System.out.format(" ]\n");
        }
    }

    public static void pbrtCoordinateSystem(String name) {
        VERIFY_INITIALIZED("CoordinateSystem");
        namedCoordinateSystems.put(name, new TransformSet(curTransform));
        if (Pbrt.options.Cat || Pbrt.options.ToPly)
            System.out.format("%sCoordinateSystem \"%s\"\n", new String(spaces, 0, catIndentCount), name);
    }

    public static void pbrtCoordSysTransform(String name) {
        VERIFY_INITIALIZED("CoordSysTransform");
        if (namedCoordinateSystems.containsKey(name))
            curTransform = namedCoordinateSystems.get(name);
        else
            Error.Warning("Couldn't find named coordinate system \"%s\"", name);
        if (Pbrt.options.Cat || Pbrt.options.ToPly)
            System.out.format("%sCoordSysTransform \"%s\"\n", new String(spaces, 0, catIndentCount), name);
    }

    public static void pbrtActiveTransformAll() {
        activeTransformBits = AllTransformsBits;
        if (Pbrt.options.Cat || Pbrt.options.ToPly)
            System.out.format("%sActiveTransform All\n", new String(spaces, 0, catIndentCount));
    }

    public static void pbrtActiveTransformEndTime() {
        activeTransformBits = EndTransformBits;
        if (Pbrt.options.Cat || Pbrt.options.ToPly)
            System.out.format("%sActiveTransform EndTime\n", new String(spaces, 0, catIndentCount));
    }

    public static void pbrtActiveTransformStartTime() {
        activeTransformBits = StartTransformBits;
        if (Pbrt.options.Cat || Pbrt.options.ToPly)
            System.out.format("%sActiveTransform StartTime\n", new String(spaces, 0, catIndentCount));
    }

    public static void pbrtTransformTimes(float start, float end) {
        VERIFY_OPTIONS("TransformTimes");
        renderOptions.transformStartTime = start;
        renderOptions.transformEndTime = end;
        if (Pbrt.options.Cat || Pbrt.options.ToPly)
            System.out.format("%sTransformTimes %.9g %.9g\n", new String(spaces, 0, catIndentCount), start, end);
    }

    public static void pbrtPixelFilter(String name, ParamSet params) {
        VERIFY_OPTIONS("PixelFilter");
        renderOptions.FilterName = name;
        renderOptions.FilterParams = params;
        if (Pbrt.options.Cat || Pbrt.options.ToPly) {
            System.out.format("%sPixelFilter \"%s\" ", new String(spaces, 0, catIndentCount), name);
            params.Print(catIndentCount);
            System.out.format("\n");
        }
    }

    public static void pbrtFilm(String type, ParamSet params) {
        VERIFY_OPTIONS("Film");
        renderOptions.FilmParams = params;
        renderOptions.FilmName = type;
        if (Pbrt.options.Cat || Pbrt.options.ToPly) {
            System.out.format("%sFilm \"%s\" ", new String(spaces, 0, catIndentCount), type);
            params.Print(catIndentCount);
            System.out.format("\n");
        }
    }

    public static void pbrtSampler(String name, ParamSet params) {
        VERIFY_OPTIONS("Sampler");
        renderOptions.SamplerName = name;
        renderOptions.SamplerParams = params;
        if (Pbrt.options.Cat || Pbrt.options.ToPly) {
            System.out.format("%sSampler \"%s\" ", new String(spaces, 0, catIndentCount), name);
            params.Print(catIndentCount);
            System.out.format("\n");
        }
    }

    public static void pbrtAccelerator(String name, ParamSet params) {
        VERIFY_OPTIONS("Accelerator");
        renderOptions.AcceleratorName = name;
        renderOptions.AcceleratorParams = params;
        if (Pbrt.options.Cat || Pbrt.options.ToPly) {
            System.out.format("%sAccelerator \"%s\" ", new String(spaces, 0, catIndentCount), name);
            params.Print(catIndentCount);
            System.out.format("\n");
        }
    }

    public static void pbrtIntegrator(String name, ParamSet params) {
        VERIFY_OPTIONS("Integrator");
        renderOptions.IntegratorName = name;
        renderOptions.IntegratorParams = params;
        if (Pbrt.options.Cat || Pbrt.options.ToPly) {
            System.out.format("%sIntegrator \"%s\" ", new String(spaces, 0, catIndentCount), name);
            params.Print(catIndentCount);
            System.out.format("\n");
        }
    }

    public static void pbrtCamera(String name, ParamSet params) {
        VERIFY_OPTIONS("Camera");
        renderOptions.CameraName = name;
        renderOptions.CameraParams = params;
        renderOptions.CameraToWorld = TransformSet.Inverse(curTransform);
        namedCoordinateSystems.put("camera", new TransformSet(renderOptions.CameraToWorld));
        if (Pbrt.options.Cat || Pbrt.options.ToPly) {
            System.out.format("%sCamera \"%s\" ", new String(spaces, 0, catIndentCount), name);
            params.Print(catIndentCount);
            System.out.format("\n");
        }
    }

    public static void pbrtMakeNamedMedium(String name, ParamSet params) {
        VERIFY_INITIALIZED("MakeNamedMedium");
        WARN_IF_ANIMATED_TRANSFORM("MakeNamedMedium");
        String type = params.FindOneString("type", "");
        if (type.isEmpty())
            Error.Error("No parameter string \"type\" found in MakeNamedMedium");
        else {
            Medium medium = MakeMedium(type, params, curTransform.trans[0]);
            if (medium != null) renderOptions.namedMedia.put(name, medium);
        }
        if (Pbrt.options.Cat || Pbrt.options.ToPly) {
            System.out.format("%sMakeNamedMedium \"%s\" ", new String(spaces, 0, catIndentCount), name);
            params.Print(catIndentCount);
            System.out.format("\n");
        }
    }

    public static void pbrtMediumInterface(String insideName, String outsideName) {
        VERIFY_INITIALIZED("MediumInterface");
        graphicsState.currentInsideMedium = insideName;
        graphicsState.currentOutsideMedium = outsideName;
        renderOptions.haveScatteringMedia = true;
        if (Pbrt.options.Cat || Pbrt.options.ToPly)
            System.out.format("%sMediumInterface \"%s\" \"%s\"\n", new String(spaces, 0, catIndentCount),
                    insideName, outsideName);
    }

    public static void pbrtWorldBegin() {
        VERIFY_OPTIONS("WorldBegin");
        currentApiState = APIState.WorldBlock;
        for (int i = 0; i < MaxTransforms; ++i) curTransform.trans[i] = new Transform();
        activeTransformBits = AllTransformsBits;
        namedCoordinateSystems.put("world", new TransformSet(curTransform));
        if (Pbrt.options.Cat || Pbrt.options.ToPly)
            System.out.format("\n\nWorldBegin\n\n");
    }

    public static void pbrtAttributeBegin() {
        VERIFY_WORLD("AttributeBegin");
        pushedGraphicsStates.push(new GraphicsState(graphicsState));
        pushedTransforms.push(new TransformSet(curTransform));
        pushedActiveTransformBits.push(activeTransformBits);
        if (Pbrt.options.Cat || Pbrt.options.ToPly) {
            System.out.format("\n%sAttributeBegin\n", new String(spaces, 0, catIndentCount));
            catIndentCount += 4;
        }
    }

    public static void pbrtAttributeEnd() {
        VERIFY_WORLD("AttributeEnd");
        if (pushedGraphicsStates.empty()) {
            Error.Error("Unmatched pbrtAttributeEnd() encountered. Ignoring it.");
            return;
        }
        graphicsState = pushedGraphicsStates.pop();
        curTransform = pushedTransforms.pop();
        activeTransformBits = pushedActiveTransformBits.pop();
        if (Pbrt.options.Cat || Pbrt.options.ToPly) {
            catIndentCount -= 4;
            System.out.format("%sAttributeEnd\n", new String(spaces, 0, catIndentCount));
        }
    }

    public static void pbrtTransformBegin() {
        VERIFY_WORLD("TransformBegin");
        pushedTransforms.push(new TransformSet(curTransform));
        pushedActiveTransformBits.push(activeTransformBits);
        if (Pbrt.options.Cat || Pbrt.options.ToPly) {
            System.out.format("%sTransformBegin\n", new String(spaces, 0, catIndentCount));
            catIndentCount += 4;
        }
    }

    public static void pbrtTransformEnd() {
        VERIFY_WORLD("TransformEnd");
        if (pushedTransforms.empty()) {
            Error.Error("Unmatched pbrtTransformEnd() encountered. Ignoring it.");
            return;
        }
        curTransform = pushedTransforms.pop();
        activeTransformBits = pushedActiveTransformBits.pop();
        if (Pbrt.options.Cat || Pbrt.options.ToPly) {
            catIndentCount -= 4;
            System.out.format("%sTransformEnd\n", new String(spaces, 0, catIndentCount));
        }
    }

    public static void pbrtTexture(String name, String type, String texname, ParamSet params) {
        VERIFY_WORLD("Texture");
        TextureParams tp = new TextureParams(params, params, graphicsState.floatTextures, graphicsState.spectrumTextures);
        if (Objects.equals(type, "float")) {
            // Create _Float_ texture and store in _floatTextures_
            if (graphicsState.floatTextures.containsKey(name)) {
                Error.Warning("Texture \"%s\" being redefined", name);
            }
            WARN_IF_ANIMATED_TRANSFORM("Texture");
            Texture<Float> ft = MakeFloatTexture(texname, curTransform.trans[0], tp);
            if (ft != null) graphicsState.floatTextures.put(name, ft);
        } else if (Objects.equals(type, "color") || Objects.equals(type, "spectrum")) {
            // Create _color_ texture and store in _spectrumTextures_
            if (graphicsState.spectrumTextures.containsKey(name))
                Error.Warning("Texture \"%s\" being redefined", name);
            WARN_IF_ANIMATED_TRANSFORM("Texture");
            Texture<Spectrum> st = MakeSpectrumTexture(texname, curTransform.trans[0], tp);
            if (st != null) graphicsState.spectrumTextures.put(name, st);
        } else {
            Error.Error("Texture type \"%s\" unknown.", type);
        }
        if (Pbrt.options.Cat || Pbrt.options.ToPly) {
            System.out.format("%sTexture \"%s\" \"%s\" \"%s\" ", new String(spaces, 0, catIndentCount), name, type, texname);
            params.Print(catIndentCount);
            System.out.format("\n");
        }
    }

    public static void pbrtMaterial(String name, ParamSet params) {
        VERIFY_WORLD("Material");
        graphicsState.material = name;
        graphicsState.materialParams = params;
        graphicsState.currentNamedMaterial = "";
        if (Pbrt.options.Cat || Pbrt.options.ToPly) {
            System.out.format("%sMaterial \"%s\" ", new String(spaces, 0, catIndentCount), name);
            params.Print(catIndentCount);
            System.out.format("\n");
        }
    }

    public static void pbrtMakeNamedMaterial(String name, ParamSet params) {
        VERIFY_WORLD("MakeNamedMaterial");
        // error checking, warning if replace, what to use for transform?
        ParamSet emptyParams = new ParamSet();
        TextureParams mp = new TextureParams(params, emptyParams, graphicsState.floatTextures, graphicsState.spectrumTextures);
        String matName = mp.FindString("type","");
        WARN_IF_ANIMATED_TRANSFORM("MakeNamedMaterial");
        if (matName.isEmpty()) {
            Error.Error("No parameter string \"type\" found in MakeNamedMaterial");
        }

        if (Pbrt.options.Cat || Pbrt.options.ToPly) {
            System.out.format("%sMakeNamedMaterial \"%s\" ", new String(spaces, 0, catIndentCount), name);
            params.Print(catIndentCount);
            System.out.format("\n");
        } else {
            Material mtl = MakeMaterial(matName, mp);
            if (graphicsState.namedMaterials.containsKey(name))
                Error.Warning("Named material \"%s\" redefined.", name);
            graphicsState.namedMaterials.put(name, mtl);
        }
    }

    public static void pbrtNamedMaterial(String name) {
        VERIFY_WORLD("NamedMaterial");
        graphicsState.currentNamedMaterial = name;
        if (Pbrt.options.Cat || Pbrt.options.ToPly)
            System.out.format("%sNamedMaterial \"%s\"\n", new String(spaces, 0, catIndentCount), name);
    }

    public static void pbrtLightSource(String name, ParamSet params) {
        VERIFY_WORLD("LightSource");
        WARN_IF_ANIMATED_TRANSFORM("LightSource");
        MediumInterface mi = graphicsState.CreateMediumInterface();
        Light lt = MakeLight(name, params, curTransform.trans[0], mi);
        if (lt == null) {
            Error.Error("LightSource: light type \"%s\" unknown.", name);
        } else {
            renderOptions.lights.add(lt);
        }
        if (Pbrt.options.Cat || Pbrt.options.ToPly) {
            System.out.format("%sLightSource \"%s\" ", new String(spaces, 0, catIndentCount), name);
            params.Print(catIndentCount);
            System.out.format("\n");
        }
    }

    public static void pbrtAreaLightSource(String name, ParamSet params) {
        VERIFY_WORLD("AreaLightSource");
        graphicsState.areaLight = name;
        graphicsState.areaLightParams = params;
        if (Pbrt.options.Cat || Pbrt.options.ToPly) {
            System.out.format("%sAreaLightSource \"%s\" ", new String(spaces, 0, catIndentCount), name);
            params.Print(catIndentCount);
            System.out.format("\n");
        }
    }

    public static void pbrtShape(String name, ParamSet params) {
        VERIFY_WORLD("Shape");
        ArrayList<Primitive> prims = new ArrayList<>();
        ArrayList<AreaLight> areaLights = new ArrayList<>();
        if (Pbrt.options.Cat || (Pbrt.options.ToPly && !Objects.equals(name, "trianglemesh"))) {
            System.out.format("%sShape \"%s\" ", new String(spaces, 0, catIndentCount), name);
            params.Print(catIndentCount);
            System.out.format("\n");
        }

        if (!curTransform.IsAnimated()) {
            // Initialize _prims_ and _areaLights_ for static shape

            // Create shapes for shape _name_
            TransformCache.TransformPair tp = transformCache.Lookup(curTransform.trans[0]);
            Transform ObjToWorld = tp.t;
            Transform WorldToObj = tp.tInv;

            ArrayList<Shape> shapes = MakeShapes(name, ObjToWorld, WorldToObj, graphicsState.reverseOrientation, params);
            if (shapes.isEmpty()) return;
            Material mtl = graphicsState.CreateMaterial(params);
            params.ReportUnused();
            MediumInterface mi = graphicsState.CreateMediumInterface();
            for (Shape s : shapes) {
                assert s != null;
                // Possibly create area light for shape
                AreaLight area = null;
                if (!graphicsState.areaLight.isEmpty()) {
                    area = MakeAreaLight(graphicsState.areaLight, curTransform.trans[0], mi, graphicsState.areaLightParams, s);
                    if (area != null) areaLights.add(area);
                }
                prims.add(new GeometricPrimitive(s, mtl, area, mi));
            }
        } else {
            // Initialize _prims_ and _areaLights_ for animated shape

            // Create initial shape or shapes for animated shape
            if (!graphicsState.areaLight.isEmpty()) {
                Error.Warning("Ignoring currently set area light when creating animated shape");
            }
            TransformCache.TransformPair tp = transformCache.Lookup(new Transform());
            ArrayList<Shape> shapes = MakeShapes(name, tp.t, tp.t, graphicsState.reverseOrientation, params);
            if (shapes.isEmpty()) return;

            // Create _GeometricPrimitive_(s) for animated shape
            Material mtl = graphicsState.CreateMaterial(params);
            params.ReportUnused();
            MediumInterface mi = graphicsState.CreateMediumInterface();
            for (Shape s : shapes) {
                prims.add(new GeometricPrimitive(s, mtl, null, mi));
            }

            // Create single _TransformedPrimitive_ for _prims_

            // Get _animatedObjectToWorld_ transform for shape
            TransformCache.TransformPair tp0 = transformCache.Lookup(curTransform.trans[0]);
            TransformCache.TransformPair tp1 = transformCache.Lookup(curTransform.trans[1]);
            AnimatedTransform animatedObjectToWorld = new AnimatedTransform(tp0.t, renderOptions.transformStartTime, tp1.t,
                    renderOptions.transformEndTime);
            if (prims.size() > 1) {
                Primitive[] primArray = new Primitive[1];
                Primitive bvh = new BVHAccel(prims.toArray(primArray));
                prims.clear();
                prims.add(bvh);
            }
            prims.set(0, new TransformedPrimitive(prims.get(0), animatedObjectToWorld));
        }
        // Add _prims_ and _areaLights_ to scene or current instance
        if (renderOptions.currentInstance != null) {
            if (!areaLights.isEmpty()) {
                Error.Warning("Area lights not supported with object instancing");
            }
            renderOptions.currentInstance.addAll(prims);
        } else {
            renderOptions.primitives.addAll(prims);
            if (!areaLights.isEmpty()) {
                renderOptions.lights.addAll(areaLights);
            }
        }
    }

    public static void pbrtReverseOrientation() {
        VERIFY_WORLD("ReverseOrientation");
        graphicsState.reverseOrientation = !graphicsState.reverseOrientation;
        if (Pbrt.options.Cat || Pbrt.options.ToPly) {
            System.out.format("%sReverseOrientation\n", new String(spaces, 0, catIndentCount));
        }
    }

    public static void pbrtObjectBegin(String name) {
        VERIFY_WORLD("ObjectBegin");
        pbrtAttributeBegin();
        if (renderOptions.currentInstance != null)
            Error.Error("ObjectBegin called inside of instance definition");
        renderOptions.instances.put(name, new ArrayList<>());
        renderOptions.currentInstance = renderOptions.instances.get(name);
        if (Pbrt.options.Cat || Pbrt.options.ToPly) {
            System.out.format("%sObjectBegin \"%s\"\n", new String(spaces, 0, catIndentCount), name);
        }
    }

    private static Stats.Counter nObjectInstancesCreated = new Stats.Counter("Scene/Object instances created");

    public static void pbrtObjectEnd() {
        VERIFY_WORLD("ObjectEnd");
        if (renderOptions.currentInstance == null)
            Error.Error("ObjectEnd called outside of instance definition");
        renderOptions.currentInstance = null;
        pbrtAttributeEnd();
        nObjectInstancesCreated.increment();

        if (Pbrt.options.Cat || Pbrt.options.ToPly) {
            System.out.format("%sObjectEnd\n", new String(spaces, 0, catIndentCount));
        }
    }

    private static Stats.Counter nObjectInstancesUsed = new Stats.Counter("Scene/Object instances used");

    public static void pbrtObjectInstance(String name) {
        VERIFY_WORLD("ObjectInstance");
        // Perform object instance error checking
        if (Pbrt.options.Cat || Pbrt.options.ToPly)
            System.out.format("%sObjectInstance \"%s\"\n", new String(spaces, 0, catIndentCount), name);
        if (renderOptions.currentInstance != null) {
            Error.Error("ObjectInstance can't be called inside instance definition");
            return;
        }
        if (!renderOptions.instances.containsKey(name)) {
            Error.Error("Unable to find instance named \"%s\"", name);
            return;
        }
        ArrayList<Primitive> in = renderOptions.instances.get(name);
        if (in.isEmpty()) return;

        nObjectInstancesUsed.increment();

        if (in.size() > 1) {
            // Create aggregate for instance _Primitive_s
            Primitive[] primArray = new Primitive[1];
            Primitive[] inPrims = in.toArray(primArray);
            Primitive accel = MakeAccelerator(renderOptions.AcceleratorName, inPrims, renderOptions.AcceleratorParams);
            if (accel == null) accel = new BVHAccel(inPrims);
            in.clear();
            in.add(accel);
        }

        // Create _animatedInstanceToWorld_ transform for instance
        TransformCache.TransformPair tp0 = transformCache.Lookup(curTransform.trans[0]);
        TransformCache.TransformPair tp1 = transformCache.Lookup(curTransform.trans[1]);
        AnimatedTransform animatedInstanceToWorld = new AnimatedTransform(
                tp0.t, renderOptions.transformStartTime,
                tp1.t, renderOptions.transformEndTime);
        Primitive prim = new TransformedPrimitive(in.get(0), animatedInstanceToWorld);
        renderOptions.primitives.add(prim);
    }

    public static void pbrtWorldEnd() {
        VERIFY_WORLD("WorldEnd");
        // Ensure there are no pushed graphics states
        while (!pushedGraphicsStates.empty()) {
            Error.Warning("Missing end to pbrtAttributeBegin()");
            pushedGraphicsStates.pop();
            pushedTransforms.pop();
        }
        while (!pushedTransforms.empty()) {
            Error.Warning("Missing end to pbrtTransformBegin()");
            pushedTransforms.pop();
        }

        // Create scene and render
        if (Pbrt.options.Cat || Pbrt.options.ToPly) {
            System.out.format("%sWorldEnd\n", new String(spaces, 0, catIndentCount));
        } else {
            Integrator integrator = renderOptions.MakeIntegrator();
            Scene scene = renderOptions.MakeScene();

            if ((scene != null) && (integrator != null)) {
                integrator.Render(scene);
            }

            //Parallel.MergeWorkerThreadStats();
            Stats.ReportThreadStats();
            if (!Pbrt.options.Quiet) {
                try {
                    PrintWriter pw = new PrintWriter("renderStats.txt");
                    Stats.PrintStats(pw);
                    pw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Stats.ClearStats();
            }
        }

        // Clean up after rendering
        graphicsState = new GraphicsState();
        transformCache.Clear();
        currentApiState = APIState.OptionsBlock;

        for (int i = 0; i < MaxTransforms; ++i) curTransform.trans[i] = new Transform();
        activeTransformBits = AllTransformsBits;
        namedCoordinateSystems.clear();
        ImageTextureFloat.ClearCacheFloat();
        ImageTextureSpectrum.ClearCacheSpectrum();
    }

    public static void pbrtParseFile(String filename) {
        Parser.ParseFile(filename);
    }

    public static void pbrtParseString(String str) {
        Parser.ParseString(str);
    }

    private static void VERIFY_INITIALIZED(String func) {
        if (!(Pbrt.options.Cat || Pbrt.options.ToPly) && currentApiState == APIState.Uninitialized) {
            Error.Error("pbrtInit() must be before calling \"%s()\". Ignoring.", func);
        }
    }

    private static void VERIFY_OPTIONS(String func) {
        VERIFY_INITIALIZED(func);
        if (!(Pbrt.options.Cat || Pbrt.options.ToPly) && currentApiState == APIState.WorldBlock) {
            Error.Error("Options cannot be set inside world block; \"%s\" not allowed.  Ignoring.", func);
        }
    }

    private static void VERIFY_WORLD(String func) {
        VERIFY_INITIALIZED(func);
        if(!(Pbrt.options.Cat || Pbrt.options.ToPly) && currentApiState ==APIState.OptionsBlock)  {
            Error.Error("Scene description must be inside world block; \"%s\" not allowed. Ignoring.", func);
        }
    }

    private static void WARN_IF_ANIMATED_TRANSFORM(String func) {
        if (curTransform.IsAnimated()) {
            Error.Warning("Animated transformations set; ignoring for \"%s\" " +
                    "and using the start transform only", func);
        }
    }
}