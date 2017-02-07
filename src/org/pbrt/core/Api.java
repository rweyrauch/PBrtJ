
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
import java.util.Map;
import java.util.Stack;
import org.pbrt.shapes.*;
import org.pbrt.textures.*;
import org.pbrt.materials.*;
import org.pbrt.lights.*;
import org.pbrt.cameras.*;
import org.pbrt.filters.*;
import org.pbrt.samplers.*;
import org.pbrt.accelerators.*;
import org.pbrt.media.*;

public class Api {

    private static final int MaxTransforms = 2;
    private static final int StartTransformBits = 1 << 0;
    private static final int EndTransformBits = 1 << 1;
    private static final int AllTransformsBits = (1 << MaxTransforms) - 1;
    private static class TransformSet {
        // TransformSet Public Methods
        public Transform at(int i) {
            assert (i >= 0);
            assert (i < MaxTransforms);
            return t[i];
        }
        public static TransformSet Inverse(TransformSet ts) {
            TransformSet tInv = new TransformSet();
            for (int i = 0; i < MaxTransforms; ++i) tInv.t[i] = Transform.Inverse(ts.t[i]);
            return tInv;
        }
        public boolean IsAnimated() {
            for (int i = 0; i < MaxTransforms - 1; ++i)
                if (t[i] != t[i + 1]) return true;
            return false;
        }

        private Transform t[] = new Transform[MaxTransforms];
    }

    private static class RenderOptions {
        // RenderOptions Public Methods
        public Integrator MakeIntegrator() {
            return null;
        }
        public Scene MakeScene() {
            return null;
        }
        public Camera MakeCamera() {
            return null;
        }

        // RenderOptions Public Data
        public float transformStartTime = 0, transformEndTime = 1;
        public String FilterName = "box";
        public ParamSet FilterParams;
        public String FilmName = "image";
        public ParamSet FilmParamsPair;
        public String SamplerName = "halton";
        public ParamSet SamplerParams;
        public String AcceleratorName = "bvh";
        public ParamSet AcceleratorParams;
        public String IntegratorName = "path";
        public ParamSet IntegratorParams;
        public String CameraName = "perspective";
        public ParamSet CameraParams;
        public TransformSet CameraToWorld;
        public Map<String, Medium> namedMedia;
        public ArrayList<Light> lights;
        public ArrayList<Primitive> primitives;
        public Map<String, ArrayList<Primitive>> instances;
        public ArrayList<Primitive> currentInstance = null;
        boolean haveScatteringMedia = false;
    }

    private static class GraphicsState {
        // Graphics State Methods
        public Material CreateMaterial(ParamSet params) {
            TextureParams mp = new TextureParams(params, materialParams, floatTextures, spectrumTextures);
            Material mtl;
            if (currentNamedMaterial != "") {
                mtl = namedMaterials.get(currentNamedMaterial);
                if (mtl == null) {
                    Error.Error("Named material \"%s\" not defined. Using \"matte\".", currentNamedMaterial);
                    mtl = MakeMaterial("matte", mp);
                }
            } else {
                mtl = MakeMaterial(material, mp);
                if (mtl == null && material != "" && material != "none")
                    mtl = MakeMaterial("matte", mp);
            }
            return mtl;
        }
        public MediumInterface CreateMediumInterface() {
            MediumInterface m = new MediumInterface();
            if (currentInsideMedium != "") {
                m.inside = renderOptions.namedMedia.get(currentInsideMedium);
                if (m.inside == null)
                    Error.Error("Named medium \"%s\" undefined.", currentInsideMedium);
            }
            if (currentOutsideMedium != "") {
                m.outside = renderOptions.namedMedia.get(currentOutsideMedium);
                if (m.outside == null)
                    Error.Error("Named medium \"%s\" undefined.", currentOutsideMedium);
            }
            return m;
        }

        // Graphics State
        public String currentInsideMedium, currentOutsideMedium;
        public Map<String, Texture<Float>> floatTextures;
        public Map<String, Texture<Spectrum>> spectrumTextures;
        public ParamSet materialParams;
        public String material = "matte";
        public Map<String, Material> namedMaterials;
        public String currentNamedMaterial;
        public ParamSet areaLightParams;
        public String areaLight;
        public boolean reverseOrientation = false;
    }

    private static class TransformCache {

        private class TransformPair {
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
        private Map<Transform, TransformPair> cache;
    }

    private enum APIState { Uninitialized, OptionsBlock, WorldBlock };
    private static APIState currentApiState = APIState.Uninitialized;
    private static TransformSet curTransform;
    private static int activeTransformBits = AllTransformsBits;
    private static Map<String, TransformSet> namedCoordinateSystems;
    private static RenderOptions renderOptions;
    private static GraphicsState graphicsState;
    private static Stack<GraphicsState> pushedGraphicsStates;
    private static Stack<TransformSet> pushedTransforms;
    private static Stack<Integer> pushedActiveTransformBits;
    private static TransformCache transformCache;
    private static int catIndentCount = 0;

    private static ArrayList<Shape> MakeShapes(String name, Transform object2world, Transform world2object, boolean reverseOrientation, ParamSet paramSet) {
        ArrayList<Shape> shapes = new ArrayList<>();
        Shape s = null;
        if (name == "sphere")
            s = Sphere.Create(object2world, world2object, reverseOrientation, paramSet);
            // Create remaining single _Shape_ types
        else if (name == "cylinder")
            s = Cylinder.Create(object2world, world2object, reverseOrientation, paramSet);
        else if (name == "disk")
            s = Disk.Create(object2world, world2object, reverseOrientation, paramSet);
        else if (name == "cone")
            s = Cone.Create(object2world, world2object, reverseOrientation, paramSet);
        else if (name == "paraboloid")
            s = Paraboloid.Create(object2world, world2object, reverseOrientation, paramSet);
        else if (name == "hyperboloid")
            s = Hyperboloid.Create(object2world, world2object, reverseOrientation, paramSet);

        if (s != null) shapes.add(s);
            // Create multiple-_Shape_ types
        else if (name == "curve")
            shapes.addAll(Curve.Create(object2world, world2object, reverseOrientation, paramSet));
        else if (name == "trianglemesh") {
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

                printf("%*sShape \"plymesh\" \"string filename\" \"%s\" ",
                        catIndentCount, "", fn);

                String alphaTex = paramSet.FindTexture("alpha");
                if (alphaTex != "")
                    printf("\n%*s\"texture alpha\" \"%s\" ", catIndentCount + 8, "", alphaTex);
                else {
                    int count;
                    float[] alpha = paramSet.FindFloat("alpha");
                    if (alpha != null)
                        printf("\n%*s\"float alpha\" %f ", catIndentCount + 8, "", alpha[0]);
                }

                String shadowAlphaTex = paramSet.FindTexture("shadowalpha");
                if (shadowAlphaTex != "")
                    printf("\n%*s\"texture shadowalpha\" \"%s\" ",
                            catIndentCount + 8, "", shadowAlphaTex);
                else {
                    int count;
                    float[] alpha = paramSet.FindFloat("shadowalpha");
                    if (alpha != null)
                        printf("\n%*s\"float shadowalpha\" %f ", catIndentCount + 8, "", alpha[0]);
                }
                printf("\n");
                */
            }
            else {
                shapes.addAll(Triangle.Create(object2world, world2object, reverseOrientation, paramSet, graphicsState.floatTextures));
            }
        }
        else if (name == "plymesh")
            shapes.addAll(PlyMesh.Create(object2world, world2object, reverseOrientation, paramSet, graphicsState.floatTextures));
        else if (name == "heightfield")
            shapes.addAll(HeightField.Create(object2world, world2object, reverseOrientation, paramSet));
        else if (name == "loopsubdiv")
            shapes.addAll(LoopSubdiv.Create(object2world, world2object, reverseOrientation, paramSet));
        else if (name == "nurbs")
            shapes.addAll(NURBS.Create(object2world, world2object, reverseOrientation, paramSet));
        else
            Error.Warning("Shape \"%s\" unknown.", name);
        paramSet.ReportUnused();
        return shapes;
    }

    private static int nMaterialsCreated = 0;

    static Material MakeMaterial(String name, TextureParams mp) {
        Material material = null;
        if (name == "" || name == "none")
            return null;
        else if (name == "matte")
            material = MatteMaterial.Create(mp);
        else if (name == "plastic")
            material = PlasticMaterial.Create(mp);
        else if (name == "translucent")
            material = TranslucentMaterial.Create(mp);
        else if (name == "glass")
            material = GlassMaterial.Create(mp);
        else if (name == "mirror")
            material = MirrorMaterial.Create(mp);
        else if (name == "hair")
            material = HairMaterial.Create(mp);
        else if (name == "mix") {
            String m1 = mp.FindString("namedmaterial1", "");
            String m2 = mp.FindString("namedmaterial2", "");
            Material mat1 = graphicsState.namedMaterials.get(m1);
            Material mat2 = graphicsState.namedMaterials.get(m2);
            if (mat1 == null) {
                Error.Error("Named material \"%s\" undefined.  Using \"matte\"",
                        m1);
                mat1 = MakeMaterial("matte", mp);
            }
            if (mat2 == null) {
                Error.Error("Named material \"%s\" undefined.  Using \"matte\"",
                        m2);
                mat2 = MakeMaterial("matte", mp);
            }

            material = MixMaterial.Create(mp, mat1, mat2);
        } else if (name == "metal")
            material = MetalMaterial.Create(mp);
        else if (name == "substrate")
            material = SubstrateMaterial.Create(mp);
        else if (name == "uber")
            material = UberMaterial.Create(mp);
        else if (name == "subsurface")
            material = SubsurfaceMaterial.Create(mp);
        else if (name == "kdsubsurface")
            material = KdSubsurfaceMaterial.Create(mp);
        else if (name == "fourier")
            material = FourierMaterial.Create(mp);
        else {
            Error.Warning("Material \"%s\" unknown. Using \"matte\".", name);
            material = MatteMaterial.Create(mp);
        }

        if ((name == "subsurface" || name == "kdsubsurface") && (renderOptions.IntegratorName != "path" && (renderOptions.IntegratorName != "volpath"))) {
            Error.Warning("Subsurface scattering material \"%s\" used, but \"%s\" integrator doesn't support subsurface scattering. Use \"path\" or \"volpath\".",
                    name, renderOptions.IntegratorName);
        }
        mp.ReportUnused();
        if (material == null) Error.Error("Unable to create material \"%s\"", name);
        else
            ++nMaterialsCreated;
        return material;
    }

    Texture<Float> MakeFloatTexture(String name, Transform tex2world, TextureParams tp) {
        Texture<Float> tex = null;
        if (name == "constant")
            tex = ConstantTexture.CreateFloat(tex2world, tp);
        else if (name == "scale")
            tex = ScaleTexture.CreateFloat(tex2world, tp);
        else if (name == "mix")
            tex = MixTexture.CreateFloat(tex2world, tp);
        else if (name == "bilerp")
            tex = BilerpTexture.CreateFloat(tex2world, tp);
        else if (name == "imagemap")
            tex = ImageMap.CreateFloat(tex2world, tp);
        else if (name == "uv")
            tex = UVTexture.CreateFloat(tex2world, tp);
        else if (name == "checkerboard")
            tex = CheckerBoardTexture.CreateFloat(tex2world, tp);
        else if (name == "dots")
            tex = DotsTexture.CreateFloat(tex2world, tp);
        else if (name == "fbm")
            tex = FBMTexture.CreateFloat(tex2world, tp);
        else if (name == "wrinkled")
            tex = WrinkledTexture.CreateFloat(tex2world, tp);
        else if (name == "marble")
            tex = MarbleTexture.CreateFloat(tex2world, tp);
        else if (name == "windy")
            tex = WindyTexture.CreateFloat(tex2world, tp);
        else
            Error.Warning("Float texture \"%s\" unknown.", name);
        tp.ReportUnused();
        return tex;
    }

    Texture<Spectrum> MakeSpectrumTexture(String name, Transform tex2world, TextureParams tp) {
        Texture<Spectrum> tex = null;
        if (name == "constant")
            tex = ConstantTexture.CreateSpectrum(tex2world, tp);
        else if (name == "scale")
            tex = ScaleTexture.CreateSpectrum(tex2world, tp);
        else if (name == "mix")
            tex = MixTexture.CreateSpectrum(tex2world, tp);
        else if (name == "bilerp")
            tex = BilerpTexture.CreateSpectrum(tex2world, tp);
        else if (name == "imagemap")
            tex = ImageMap.CreateSpectrum(tex2world, tp);
        else if (name == "uv")
            tex = UVTexture.CreateSpectrum(tex2world, tp);
        else if (name == "checkerboard")
            tex = CheckerBoardTexture.CreateSpectrum(tex2world, tp);
        else if (name == "dots")
            tex = DotsTexture.CreateSpectrum(tex2world, tp);
        else if (name == "fbm")
            tex = FBMTexture.CreateSpectrum(tex2world, tp);
        else if (name == "wrinkled")
            tex = WrinkledTexture.CreateSpectrum(tex2world, tp);
        else if (name == "marble")
            tex = MarbleTexture.CreateSpectrum(tex2world, tp);
        else if (name == "windy")
            tex = WindyTexture.CreateSpectrum(tex2world, tp);
        else
            Error.Warning("Spectrum texture \"%s\" unknown.", name);
        tp.ReportUnused();
        return tex;
    }

    Medium MakeMedium(String name, ParamSet paramSet, Transform medium2world) {
        float sig_a_rgb[] = {.0011f, .0024f, .014f}, sig_s_rgb[] = {2.55f, 3.21f, 3.77f};
        Spectrum sig_a = Spectrum.FromRGB(sig_a_rgb),
                 sig_s = Spectrum.FromRGB(sig_s_rgb);
        String preset = paramSet.FindOneString("preset", "");
        Medium.ScatteringProps props = Medium.GetMediumScatteringProperties(preset);
        if (preset != "" && props == null) {
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
        if (name == "homogeneous") {
            m = new HomogeneousMedium(sig_a, sig_s, g);
        } else if (name == "heterogeneous") {
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
        } else
            Error.Warning("Medium \"%s\" unknown.", name);
        paramSet.ReportUnused();
        return m;
    }

    Light MakeLight(String name,ParamSet paramSet, Transform light2world, MediumInterface mediumInterface) {
        Light light = null;
        if (name == "point")
            light = Point.Create(light2world, mediumInterface.outside, paramSet);
        else if (name == "spot")
            light = Spot.Create(light2world, mediumInterface.outside, paramSet);
        else if (name == "goniometric")
            light = Goniometric.Create(light2world, mediumInterface.outside, paramSet);
        else if (name == "projection")
            light = Projection.Create(light2world, mediumInterface.outside, paramSet);
        else if (name == "distant")
            light = Distant.Create(light2world, paramSet);
        else if (name == "infinite" || name == "exinfinite")
            light = Infinite.Create(light2world, paramSet);
        else
            Error.Warning("Light \"%s\" unknown.", name);
        paramSet.ReportUnused();
        return light;
    }

    AreaLight MakeAreaLight(String name, Transform light2world, MediumInterface mediumInterface, ParamSet paramSet, Shape shape) {
        AreaLight area = null;
        if (name == "area" || name == "diffuse")
            area = Diffuse.Create(light2world, mediumInterface.outside, paramSet, shape);
        else
            Error.Warning("Area light \"%s\" unknown.", name);
        paramSet.ReportUnused();
        return area;
    }

    Primitive MakeAccelerator(String name, Primitive[] prims, ParamSet paramSet) {
        Primitive accel = null;
        if (name == "bvh")
            accel = BVHAccel.Create(prims, paramSet);
        else if (name == "kdtree")
            accel = KdTreeAccel.Create(prims, paramSet);
        else
            Error.Warning("Accelerator \"%s\" unknown.", name);
        paramSet.ReportUnused();
        return accel;
    }

    Camera MakeCamera(String name, ParamSet paramSet, TransformSet cam2worldSet, float transformStart, float transformEnd, Film film) {
        Camera camera = null;
        MediumInterface mediumInterface = graphicsState.CreateMediumInterface();
        TransformCache.TransformPair c2w0 = transformCache.Lookup(cam2worldSet.at(0));
        TransformCache.TransformPair c2w1 = transformCache.Lookup(cam2worldSet.at(1));
        AnimatedTransform animatedCam2World = new AnimatedTransform(c2w0.t, transformStart, c2w1.t, transformEnd);
        if (name == "perspective")
            camera = PerspectiveCamera.Create(paramSet, animatedCam2World, film, mediumInterface.outside);
        else if (name == "orthographic")
            camera = OrthographicCamera.Create(paramSet, animatedCam2World, film, mediumInterface.outside);
        else if (name == "realistic")
            camera = RealisticCamera.Create(paramSet, animatedCam2World, film, mediumInterface.outside);
        else if (name == "environment")
            camera = EnvironmentCamera.Create(paramSet, animatedCam2World, film, mediumInterface.outside);
        else
            Error.Warning("Camera \"%s\" unknown.", name);
        paramSet.ReportUnused();
        return camera;
    }

    Sampler MakeSampler(String name, ParamSet paramSet, Film film) {
        Sampler sampler = null;
        if (name == "lowdiscrepancy" || name == "02sequence")
            sampler = ZeroTwoSequence.Create(paramSet);
        else if (name == "maxmindist")
            sampler = MaxMinSampler.Create(paramSet);
        else if (name == "halton")
            sampler = HaltonSampler.Create(paramSet, film.GetSampleBounds());
        else if (name == "sobol")
            sampler = SobolSampler.Create(paramSet, film.GetSampleBounds());
        else if (name == "random")
            sampler = RandomSampler.Create(paramSet);
        else if (name == "stratified")
            sampler = StratifiedSampler.Create(paramSet);
        else
            Error.Warning("Sampler \"%s\" unknown.", name);
        paramSet.ReportUnused();
        return sampler;
    }

    Filter MakeFilter(String name, ParamSet paramSet) {
        Filter filter = null;
        if (name == "box")
            filter = BoxFilter.Create(paramSet);
        else if (name == "gaussian")
            filter = GaussianFilter.Create(paramSet);
        else if (name == "mitchell")
            filter = MitchellFilter.Create(paramSet);
        else if (name == "sinc")
            filter = SincFilter.Create(paramSet);
        else if (name == "triangle")
            filter = TriangleFilter.Create(paramSet);
        else {
            Error.Error("Filter \"%s\" unknown.", name);
        }
        paramSet.ReportUnused();
        return filter;
    }

    Film MakeFilm(String name, ParamSet paramSet, Filter filter) {
        Film film = null;
        if (name == "image")
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
        //InitProfiler();
    }

    public static void pbrtCleanup() {

    }

    public static void pbrtIdentity() {

    }

    public static void pbrtTranslate(float dx, float dy, float dz) {

    }

    public static void pbrtRotate(float angle, float ax, float ay, float az) {

    }

    public static void pbrtScale(float sx, float sy, float sz) {

    }

    public static void pbrtLookAt(float ex, float ey, float ez, float lx, float ly, float lz, float ux, float uy, float uz) {

    }

    public static void pbrtConcatTransform(float[] transform) {

    }

    public static void pbrtTransform(float[] transform) {

    }

    public static void pbrtCoordinateSystem(String name) {

    }

    public static void pbrtCoordSysTransform(String name) {

    }

    public static void pbrtActiveTransformAll() {

    }

    public static void pbrtActiveTransformEndTime() {

    }

    public static void pbrtActiveTransformStartTime() {

    }

    public static void pbrtTransformTimes(float start, float end) {

    }

    public static void pbrtPixelFilter(String name, ParamSet params) {

    }

    public static void pbrtFilm(String type, ParamSet params) {

    }

    public static void pbrtSampler(String name, ParamSet params) {

    }

    public static void pbrtAccelerator(String name, ParamSet params) {
    }

    public static void pbrtIntegrator(String name, ParamSet params) {
    }

    public static void pbrtCamera(String name, ParamSet cameraParams) {
    }

    public static void pbrtMakeNamedMedium(String name, ParamSet params) {
    }

    public static void pbrtMediumInterface(String insideName, String outsideName) {
    }

    public static void pbrtWorldBegin() {
    }

    public static void pbrtAttributeBegin() {
    }

    public static void pbrtAttributeEnd() {
    }

    public static void pbrtTransformBegin() {
    }

    public static void pbrtTransformEnd() {
    }

    public static void pbrtTexture(String name, String type, String texname, ParamSet params) {
    }

    public static void pbrtMaterial(String name, ParamSet params) {
    }

    public static void pbrtMakeNamedMaterial(String name, ParamSet params) {
    }

    public static void pbrtNamedMaterial(String name) {
    }

    public static void pbrtLightSource(String name, ParamSet params) {
    }

    public static void pbrtAreaLightSource(String name, ParamSet params) {
    }

    public static void pbrtShape(String name, ParamSet params) {
    }

    public static void pbrtReverseOrientation() {
    }

    public static void pbrtObjectBegin(String name) {
    }

    public static void pbrtObjectEnd() {
    }

    public static void pbrtObjectInstance(String name) {
    }

    public static void pbrtWorldEnd() {
    }

}