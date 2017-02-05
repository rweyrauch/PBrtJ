
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

public class Api {

    private static final int MaxTransforms = 2;
    private static final int StartTransformBits = 1 << 0;
    private static final int EndTransformBits = 1 << 1;
    private static final int AllTransformsBits = (1 << MaxTransforms) - 1;
    private class TransformSet {
        // TransformSet Public Methods
        Transform &operator[](int i) {
            CHECK_GE(i, 0);
            CHECK_LT(i, MaxTransforms);
            return t[i];
        }
     Transform &operator[](int i) const {
            CHECK_GE(i, 0);
            CHECK_LT(i, MaxTransforms);
            return t[i];
        }
        friend TransformSet Inverse(const TransformSet &ts) {
            TransformSet tInv;
            for (int i = 0; i < MaxTransforms; ++i) tInv.t[i] = Inverse(ts.t[i]);
            return tInv;
        }
        bool IsAnimated() const {
            for (int i = 0; i < MaxTransforms - 1; ++i)
                if (t[i] != t[i + 1]) return true;
            return false;
        }

        private Transform t[MaxTransforms];
    }

    private class RenderOptions {
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

    private class GraphicsState {
        // Graphics State Methods
        public Material CreateMaterial(ParamSet params) {

        }
        public MediumInterface CreateMediumInterface() {

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

    private class TransformCache {
        // TransformCache Public Methods
        public void Lookup(Transform t, Transform **tCached,
                    Transform **tCachedInverse) {
            auto iter = cache.find(t);
            if (iter == cache.end()) {
                Transform *tr = arena.Alloc<Transform>();
            *tr = t;
                Transform *tinv = arena.Alloc<Transform>();
            *tinv = Transform(Inverse(t));
                cache[t] = std::make_pair(tr, tinv);
                iter = cache.find(t);
            }
            if (tCached) *tCached = iter->second.first;
            if (tCachedInverse) *tCachedInverse = iter->second.second;
        }
        public void Clear() {
            arena.Reset();
            cache.erase(cache.begin(), cache.end());
        }

        // TransformCache Private Data
        private Map<Transform, Pair<Transform, Transform>> cache;
        private MemoryArena arena;
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

    private static Shape[] MakeShapes(String name,
                                                Transform object2world,
                                                Transform world2object,
                                                   boolean reverseOrientation,
                                                ParamSet paramSet) {
        Shape[] shapes = null;
        Shape s;
        if (name == "sphere")
            s = CreateSphereShape(object2world, world2object, reverseOrientation,
                    paramSet);
            // Create remaining single _Shape_ types
        else if (name == "cylinder")
            s = CreateCylinderShape(object2world, world2object, reverseOrientation,
                    paramSet);
        else if (name == "disk")
            s = CreateDiskShape(object2world, world2object, reverseOrientation,
                    paramSet);
        else if (name == "cone")
            s = CreateConeShape(object2world, world2object, reverseOrientation,
                    paramSet);
        else if (name == "paraboloid")
            s = CreateParaboloidShape(object2world, world2object,
                    reverseOrientation, paramSet);
        else if (name == "hyperboloid")
            s = CreateHyperboloidShape(object2world, world2object,
                    reverseOrientation, paramSet);
        if (s != null) shapes.push_back(s);

            // Create multiple-_Shape_ types
        else if (name == "curve")
            shapes = CreateCurveShape(object2world, world2object,
                    reverseOrientation, paramSet);
        else if (name == "trianglemesh") {
            if (Pbrt.options.toPly) {
                static int count = 1;
            const char *plyPrefix =
                        getenv("PLY_PREFIX") ? getenv("PLY_PREFIX") : "mesh";
                String fn = StringPrintf("%s_%05d.ply", plyPrefix, count++);

                int nvi, npi, nuvi, nsi, nni;
            const int *vi = paramSet.FindInt("indices", &nvi);
            const Point3f *P = paramSet.FindPoint3f("P", &npi);
            const Point2f *uvs = paramSet.FindPoint2f("uv", &nuvi);
                if (!uvs) uvs = paramSet.FindPoint2f("st", &nuvi);
                std::vector<Point2f> tempUVs;
                if (!uvs) {
                const Float *fuv = paramSet.FindFloat("uv", &nuvi);
                    if (!fuv) fuv = paramSet.FindFloat("st", &nuvi);
                    if (fuv) {
                        nuvi /= 2;
                        tempUVs.reserve(nuvi);
                        for (int i = 0; i < nuvi; ++i)
                            tempUVs.push_back(Point2f(fuv[2 * i], fuv[2 * i + 1]));
                        uvs = &tempUVs[0];
                    }
                }
             Normal3f[] N = paramSet.FindNormal3f("N", &nni);
             Vector3f[] S = paramSet.FindVector3f("S", &nsi);

                if (!WritePlyFile(fn, nvi / 3, vi, npi, P, S, N, uvs))
                    Error.Error("Unable to write PLY file \"%s\"", fn);

                printf("%*sShape \"plymesh\" \"string filename\" \"%s\" ",
                        catIndentCount, "", fn);

                String alphaTex = paramSet.FindTexture("alpha");
                if (alphaTex != "")
                    printf("\n%*s\"texture alpha\" \"%s\" ", catIndentCount + 8, "",
                            alphaTex);
                else {
                    int count;
                 float[] alpha = paramSet.FindFloat("alpha", &count);
                    if (alpha)
                        printf("\n%*s\"float alpha\" %f ", catIndentCount + 8, "",
                                *alpha);
                }

                String shadowAlphaTex = paramSet.FindTexture("shadowalpha");
                if (shadowAlphaTex != "")
                    printf("\n%*s\"texture shadowalpha\" \"%s\" ",
                            catIndentCount + 8, "", shadowAlphaTex);
                else {
                    int count;
                    float[] alpha = paramSet.FindFloat("shadowalpha", &count);
                    if (alpha)
                        printf("\n%*s\"float shadowalpha\" %f ", catIndentCount + 8,
                                "", *alpha);
                }
                printf("\n");
            } else
                shapes = CreateTriangleMeshShape(object2world, world2object,
                        reverseOrientation, paramSet,
                        &graphicsState.floatTextures);
        } else if (name == "plymesh")
            shapes = CreatePLYMesh(object2world, world2object, reverseOrientation,
                    paramSet, &graphicsState.floatTextures);
    else if (name == "heightfield")
            shapes = CreateHeightfield(object2world, world2object,
                    reverseOrientation, paramSet);
        else if (name == "loopsubdiv")
            shapes = CreateLoopSubdiv(object2world, world2object,
                    reverseOrientation, paramSet);
        else if (name == "nurbs")
            shapes = CreateNURBS(object2world, world2object, reverseOrientation,
                    paramSet);
        else
            Error.Warning("Shape \"%s\" unknown.", name);
        paramSet.ReportUnused();
        return shapes;
    }

    Material MakeMaterial(String name,
                                        TextureParams mp) {
        Material material = null;
        if (name == "" || name == "none")
            return null;
        else if (name == "matte")
            material = CreateMatteMaterial(mp);
        else if (name == "plastic")
            material = CreatePlasticMaterial(mp);
        else if (name == "translucent")
            material = CreateTranslucentMaterial(mp);
        else if (name == "glass")
            material = CreateGlassMaterial(mp);
        else if (name == "mirror")
            material = CreateMirrorMaterial(mp);
        else if (name == "hair")
            material = CreateHairMaterial(mp);
        else if (name == "mix") {
            String m1 = mp.FindString("namedmaterial1", "");
            String m2 = mp.FindString("namedmaterial2", "");
            Material mat1 = graphicsState.namedMaterials[m1];
            Material mat2 = graphicsState.namedMaterials[m2];
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

            material = CreateMixMaterial(mp, mat1, mat2);
        } else if (name == "metal")
            material = CreateMetalMaterial(mp);
        else if (name == "substrate")
            material = CreateSubstrateMaterial(mp);
        else if (name == "uber")
            material = CreateUberMaterial(mp);
        else if (name == "subsurface")
            material = CreateSubsurfaceMaterial(mp);
        else if (name == "kdsubsurface")
            material = CreateKdSubsurfaceMaterial(mp);
        else if (name == "fourier")
            material = CreateFourierMaterial(mp);
        else {
            Error.Warning("Material \"%s\" unknown. Using \"matte\".", name);
            material = CreateMatteMaterial(mp);
        }

        if ((name == "subsurface" || name == "kdsubsurface") &&
                (renderOptions->IntegratorName != "path" &&
                        (renderOptions->IntegratorName != "volpath")))
            Error.Warning(
                    "Subsurface scattering material \"%s\" used, but \"%s\" "
                    "integrator doesn't support subsurface scattering. "
                    "Use \"path\" or \"volpath\".",
                    name, renderOptions->IntegratorName);

        mp.ReportUnused();
        if (!material) Error("Unable to create material \"%s\"", name);
        else ++nMaterialsCreated;
        return material;
    }

    Texture<Float> MakeFloatTexture(String name,
                                                  Transform tex2world,
                                                  TextureParams tp) {
        Texture<Float> tex = null;
        if (name == "constant")
            tex = CreateConstantFloatTexture(tex2world, tp);
        else if (name == "scale")
            tex = CreateScaleFloatTexture(tex2world, tp);
        else if (name == "mix")
            tex = CreateMixFloatTexture(tex2world, tp);
        else if (name == "bilerp")
            tex = CreateBilerpFloatTexture(tex2world, tp);
        else if (name == "imagemap")
            tex = CreateImageFloatTexture(tex2world, tp);
        else if (name == "uv")
            tex = CreateUVFloatTexture(tex2world, tp);
        else if (name == "checkerboard")
            tex = CreateCheckerboardFloatTexture(tex2world, tp);
        else if (name == "dots")
            tex = CreateDotsFloatTexture(tex2world, tp);
        else if (name == "fbm")
            tex = CreateFBmFloatTexture(tex2world, tp);
        else if (name == "wrinkled")
            tex = CreateWrinkledFloatTexture(tex2world, tp);
        else if (name == "marble")
            tex = CreateMarbleFloatTexture(tex2world, tp);
        else if (name == "windy")
            tex = CreateWindyFloatTexture(tex2world, tp);
        else
            Error.Warning("Float texture \"%s\" unknown.", name);
        tp.ReportUnused();
        return tex;
    }

    Texture<Spectrum> MakeSpectrumTexture(
     String name,  Transform tex2world,
     TextureParams tp) {
        Texture<Spectrum> tex = null;
        if (name == "constant")
            tex = CreateConstantSpectrumTexture(tex2world, tp);
        else if (name == "scale")
            tex = CreateScaleSpectrumTexture(tex2world, tp);
        else if (name == "mix")
            tex = CreateMixSpectrumTexture(tex2world, tp);
        else if (name == "bilerp")
            tex = CreateBilerpSpectrumTexture(tex2world, tp);
        else if (name == "imagemap")
            tex = CreateImageSpectrumTexture(tex2world, tp);
        else if (name == "uv")
            tex = CreateUVSpectrumTexture(tex2world, tp);
        else if (name == "checkerboard")
            tex = CreateCheckerboardSpectrumTexture(tex2world, tp);
        else if (name == "dots")
            tex = CreateDotsSpectrumTexture(tex2world, tp);
        else if (name == "fbm")
            tex = CreateFBmSpectrumTexture(tex2world, tp);
        else if (name == "wrinkled")
            tex = CreateWrinkledSpectrumTexture(tex2world, tp);
        else if (name == "marble")
            tex = CreateMarbleSpectrumTexture(tex2world, tp);
        else if (name == "windy")
            tex = CreateWindySpectrumTexture(tex2world, tp);
        else
            Error.Warning("Spectrum texture \"%s\" unknown.", name);
        tp.ReportUnused();
        return tex;
    }

    Medium MakeMedium( String name,
                                    ParamSet paramSet,
                                    Transform medium2world) {
        Float sig_a_rgb[3] = {.0011f, .0024f, .014f},
        sig_s_rgb[3] = {2.55f, 3.21f, 3.77f};
        Spectrum sig_a = Spectrum.FromRGB(sig_a_rgb),
                sig_s = Spectrum.FromRGB(sig_s_rgb);
        String preset = paramSet.FindOneString("preset", "");
        boolean found = GetMediumScatteringProperties(preset, &sig_a, &sig_s);
        if (preset != "" && !found)
            Error.Warning("Material preset \"%s\" not found.  Using defaults.",
                    preset);
        Float scale = paramSet.FindOneFloat("scale", 1.f);
        Float g = paramSet.FindOneFloat("g", 0.0f);
        sig_a = paramSet.FindOneSpectrum("sigma_a", sig_a) * scale;
        sig_s = paramSet.FindOneSpectrum("sigma_s", sig_s) * scale;
        Medium m = null;
        if (name == "homogeneous") {
            m = new HomogeneousMedium(sig_a, sig_s, g);
        } else if (name == "heterogeneous") {
            int nitems;
         Float *data = paramSet.FindFloat("density", &nitems);
            if (!data) {
                Error.Error("No \"density\" values provided for heterogeneous medium?");
                return null;
            }
            int nx = paramSet.FindOneInt("nx", 1);
            int ny = paramSet.FindOneInt("ny", 1);
            int nz = paramSet.FindOneInt("nz", 1);
            Point3f p0 = paramSet.FindOnePoint3f("p0", Point3f(0.f, 0.f, 0.f));
            Point3f p1 = paramSet.FindOnePoint3f("p1", Point3f(1.f, 1.f, 1.f));
            if (nitems != nx * ny * nz) {
                Error.Error(
                        "GridDensityMedium has %d density values; expected nx*ny*nz = "
                        "%d",
                        nitems, nx * ny * nz);
                return null;
            }
            Transform data2Medium = Translate(Vector3f(p0)) *
                    Scale(p1.x - p0.x, p1.y - p0.y, p1.z - p0.z);
            m = new GridDensityMedium(sig_a, sig_s, g, nx, ny, nz,
                    medium2world * data2Medium, data);
        } else
            Error.Warning("Medium \"%s\" unknown.", name);
        paramSet.ReportUnused();
        return m;
    }

    Light MakeLight( String name,
                                  ParamSet paramSet,
                                  Transform light2world,
                                  MediumInterface mediumInterface) {
        Light light = null;
        if (name == "point")
            light =
                    CreatePointLight(light2world, mediumInterface.outside, paramSet);
        else if (name == "spot")
            light = CreateSpotLight(light2world, mediumInterface.outside, paramSet);
        else if (name == "goniometric")
            light = CreateGoniometricLight(light2world, mediumInterface.outside,
                    paramSet);
        else if (name == "projection")
            light = CreateProjectionLight(light2world, mediumInterface.outside,
                    paramSet);
        else if (name == "distant")
            light = CreateDistantLight(light2world, paramSet);
        else if (name == "infinite" || name == "exinfinite")
            light = CreateInfiniteLight(light2world, paramSet);
        else
            Error.Warning("Light \"%s\" unknown.", name);
        paramSet.ReportUnused();
        return light;
    }

    AreaLight MakeAreaLight(String name,
                                          Transform light2world,
                                          MediumInterface mediumInterface,
                                          ParamSet paramSet,
                                         Shape shape) {
        AreaLight area = null;
        if (name == "area" || name == "diffuse")
            area = CreateDiffuseAreaLight(light2world, mediumInterface.outside,
                    paramSet, shape);
        else
            Error.Warning("Area light \"%s\" unknown.", name);
        paramSet.ReportUnused();
        return area;
    }

    Primitive MakeAccelerator(
     String name,
    const std::vector<std::shared_ptr<Primitive>> &prims,
     ParamSet paramSet) {
        Primitive accel = null;
        if (name == "bvh")
            accel = CreateBVHAccelerator(prims, paramSet);
        else if (name == "kdtree")
            accel = CreateKdTreeAccelerator(prims, paramSet);
        else
            Error.Warning("Accelerator \"%s\" unknown.", name);
        paramSet.ReportUnused();
        return accel;
    }

    Camera MakeCamera( String name,  ParamSet paramSet,
                    TransformSet cam2worldSet, float transformStart,
                       float transformEnd, Film film) {
        Camera camera = null;
        MediumInterface mediumInterface = graphicsState.CreateMediumInterface();
        static_assert(MaxTransforms == 2,
                "TransformCache assumes only two transforms");
        Transform cam2world[2];
        transformCache.Lookup(cam2worldSet[0], &cam2world[0], null);
        transformCache.Lookup(cam2worldSet[1], &cam2world[1], null);
        AnimatedTransform animatedCam2World(cam2world[0], transformStart,
                cam2world[1], transformEnd);
        if (name == "perspective")
            camera = CreatePerspectiveCamera(paramSet, animatedCam2World, film,
                    mediumInterface.outside);
        else if (name == "orthographic")
            camera = CreateOrthographicCamera(paramSet, animatedCam2World, film,
                    mediumInterface.outside);
        else if (name == "realistic")
            camera = CreateRealisticCamera(paramSet, animatedCam2World, film,
                    mediumInterface.outside);
        else if (name == "environment")
            camera = CreateEnvironmentCamera(paramSet, animatedCam2World, film,
                    mediumInterface.outside);
        else
            Error.Warning("Camera \"%s\" unknown.", name);
        paramSet.ReportUnused();
        return camera;
    }

    Sampler MakeSampler( String name,
                                      ParamSet paramSet,
                                      Film film) {
        Sampler sampler = null;
        if (name == "lowdiscrepancy" || name == "02sequence")
            sampler = CreateZeroTwoSequenceSampler(paramSet);
        else if (name == "maxmindist")
            sampler = CreateMaxMinDistSampler(paramSet);
        else if (name == "halton")
            sampler = CreateHaltonSampler(paramSet, film->GetSampleBounds());
        else if (name == "sobol")
            sampler = CreateSobolSampler(paramSet, film->GetSampleBounds());
        else if (name == "random")
            sampler = CreateRandomSampler(paramSet);
        else if (name == "stratified")
            sampler = CreateStratifiedSampler(paramSet);
        else
            Error.Warning("Sampler \"%s\" unknown.", name);
        paramSet.ReportUnused();
        return sampler;
    }

    Filter MakeFilter( String name,
                                    ParamSet paramSet) {
        Filter filter = null;
        if (name == "box")
            filter = CreateBoxFilter(paramSet);
        else if (name == "gaussian")
            filter = CreateGaussianFilter(paramSet);
        else if (name == "mitchell")
            filter = CreateMitchellFilter(paramSet);
        else if (name == "sinc")
            filter = CreateSincFilter(paramSet);
        else if (name == "triangle")
            filter = CreateTriangleFilter(paramSet);
        else {
            Error.Error("Filter \"%s\" unknown.", name);
            exit(1);
        }
        paramSet.ReportUnused();
        return filter;
    }

    Film MakeFilm(String name,  ParamSet paramSet,
                   Filter filter) {
        Film film = null;
        if (name == "image")
            film = CreateFilm(paramSet, filter);
        else
            Error.Warning("Film \"%s\" unknown.", name);
        
        paramSet.ReportUnused();
        return film;
    }

    // API Function Declarations
    public static void pbrtInit(Options opt) {
        Pbrt.options = opt;

        // API Initialization
        if (currentApiState != APIState::Uninitialized)
            Error("pbrtInit() has already been called.");
        currentApiState = APIState::OptionsBlock;
        renderOptions.reset(new RenderOptions);
        graphicsState = GraphicsState();
        catIndentCount = 0;

        // General \pbrt Initialization
        SampledSpectrum::Init();
        ParallelInit();  // Threads must be launched before the profiler is
        // initialized.
        InitProfiler();
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