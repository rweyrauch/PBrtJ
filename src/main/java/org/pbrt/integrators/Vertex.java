/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.integrators;

import org.pbrt.core.*;
import org.pbrt.core.PBrtTLogger;

import java.util.HashMap;
import java.util.function.Function;

public class Vertex {

    public static float CorrectShadingNormal(SurfaceInteraction isect, Vector3f wo,  Vector3f wi, Material.TransportMode mode) {
        if (mode == Material.TransportMode.Importance) {
            float num = Normal3f.AbsDot(wo, isect.shading.n) * Normal3f.AbsDot(wi, isect.n);
            float denom = Normal3f.AbsDot(wo, isect.n) * Normal3f.AbsDot(wi, isect.shading.n);
            // wi is occasionally perpendicular to isect.shading.n; this is
            // fine, but we don't want to return an infinite or NaN value in
            // that case.
            if (denom == 0) return 0;
            return num / denom;
        } else
            return 1;
    }

    public static float InfiniteLightDensity(Scene scene, Distribution1D lightDistr,
                                             HashMap<Light, Integer> lightToDistrIndex, Vector3f w) {
        float pdf = 0;
        for (Light light : scene.infiniteLights) {
            assert (lightToDistrIndex.containsKey(light));
            int index = lightToDistrIndex.get(light);
            pdf += light.Pdf_Li(new Interaction(), w.negate()) * lightDistr.func[index];
        }
        return pdf / (lightDistr.funcInt * lightDistr.Count());
    }

    public static int GenerateCameraSubpath(Scene scene, Sampler sampler, int maxDepth, Camera camera, Point2f pFilm, Vertex[] path) {
        if (maxDepth == 0) return 0;
        // Sample initial ray for camera subpath
        Camera.CameraSample cameraSample = new Camera.CameraSample();
        cameraSample.pFilm = pFilm;
        cameraSample.time = sampler.Get1D();
        cameraSample.pLens = sampler.Get2D();
        final Camera.CameraRayDiff crd = camera.GenerateRayDifferential(cameraSample);
        RayDifferential ray = crd.rd;
        Spectrum beta = new Spectrum(crd.weight);
        ray.ScaleDifferentials(1 / (float)Math.sqrt(sampler.samplesPerPixel));

        // Generate first vertex on camera subpath and start random walk
        path[0] = CreateCamera(camera, ray, beta);
        Camera.CameraPdf cp = camera.Pdf_We(ray);
        float pdfPos = cp.pdfPos, pdfDir = cp.pdfDir;

        //VLOG(2) << "Starting camera subpath. Ray: " << ray << ", beta " << beta << ", pdfPos " << pdfPos << ", pdfDir " << pdfDir;
        return RandomWalk(scene, ray, sampler, beta, pdfDir, maxDepth - 1, Material.TransportMode.Radiance, path, 1) + 1;
    }

    public static int GenerateLightSubpath(Scene scene, Sampler sampler, int maxDepth, float time, Distribution1D lightDistr,
                                           HashMap<Light, Integer> lightToIndex, Vertex[] path) {
        if (maxDepth == 0) return 0;
        // Sample initial ray for light subpath
        Distribution1D.DiscreteSample ds = lightDistr.SampleDiscrete(sampler.Get1D());
        int lightNum = ds.offset;
        float lightPdf = ds.pdf;
        final Light light = scene.lights.get(lightNum);
        Light.LeResult lr = light.Sample_Le(sampler.Get2D(), sampler.Get2D(), time);
        RayDifferential ray = new RayDifferential(lr.ray);
        Normal3f nLight = lr.nLight;
        float pdfPos = lr.pdfPos, pdfDir = lr.pdfDir;
        Spectrum Le = lr.spectrum;

        if (pdfPos == 0 || pdfDir == 0 || Le.isBlack()) return 0;

        // Generate first vertex on light subpath and start random walk
        path[0] = Vertex.CreateLight(light, ray, nLight, Le, pdfPos * lightPdf);
        Spectrum beta = Le.scale(Normal3f.AbsDot(nLight, ray.d) / (lightPdf * pdfPos * pdfDir));
        //VLOG(2) << "Starting light subpath. Ray: " << ray << ", Le " << Le << ", beta " << beta << ", pdfPos " << pdfPos << ", pdfDir " << pdfDir;
        int nVertices = RandomWalk(scene, ray, sampler, beta, pdfDir, maxDepth - 1, Material.TransportMode.Importance, path, 1);

        // Correct subpath sampling densities for infinite area lights
        if (path[0].IsInfiniteLight()) {
            // Set spatial density of _path[1]_ for infinite area light
            if (nVertices > 0) {
                path[1].pdfFwd = pdfPos;
                if (path[1].IsOnSurface())
                    path[1].pdfFwd *= Normal3f.AbsDot(ray.d, path[1].ng());
            }

            // Set spatial density of _path[0]_ for infinite area light
            path[0].pdfFwd = InfiniteLightDensity(scene, lightDistr, lightToIndex, ray.d);
        }
        return nVertices + 1;
    }

    public static Spectrum ConnectBDPT(Scene scene, Vertex[] lightVertices, Vertex[] cameraVertices, int s, int t, Distribution1D lightDistr,
        HashMap<Light, Integer> lightToIndex, Camera camera, Sampler sampler, Point2f[] pRaster, float[] misWeight) {
        Spectrum L = new Spectrum(0);
        // Ignore invalid connections related to infinite area lights
        if (t > 1 && s != 0 && cameraVertices[t - 1].type == VertexType.Light)
            return new Spectrum(0);

        // Perform connection and write contribution to _L_
        Vertex sampled = new Vertex();
        if (s == 0) {
            // Interpret the camera subpath as a complete path
            final Vertex pt = cameraVertices[t - 1];
            if (pt.IsLight()) L = pt.Le(scene, cameraVertices[t - 2]).multiply(pt.beta);
            assert (!L.hasNaNs());
        } else if (t == 1) {
            // Sample a point on the camera and connect it to the light subpath
            final Vertex qs = lightVertices[s - 1];
            if (qs.IsConnectible()) {
                Camera.CameraWi cwi = camera.Sample_Wi(qs.GetInteraction(), sampler.Get2D());
                Light.VisibilityTester vis = cwi.vis;
                Vector3f wi = cwi.wi;
                float pdf = cwi.pdf;
                Spectrum Wi = cwi.swe;
                if (pRaster != null) pRaster[0] = cwi.pRaster;

                if (pdf > 0 && !Wi.isBlack()) {
                    // Initialize dynamically sampled vertex and _L_ for $t=1$ case
                    sampled = CreateCamera(camera, vis.P1(), Wi.scale(1 / pdf));
                    L = qs.beta.multiply(qs.f(sampled, Material.TransportMode.Importance).multiply(sampled.beta));
                    if (qs.IsOnSurface()) L = L.scale(Normal3f.AbsDot(wi, qs.ns()));
                    assert (!L.hasNaNs());
                    // Only check visibility after we know that the path would
                    // make a non-zero contribution.
                    if (!L.isBlack()) L = L.multiply(vis.Tr(scene, sampler));
                }
            }
        } else if (s == 1) {
            // Sample a point on a light and connect it to the camera subpath
            final Vertex pt = cameraVertices[t - 1];
            if (pt.IsConnectible()) {
                Distribution1D.DiscreteSample ds = lightDistr.SampleDiscrete(sampler.Get1D());
                float lightPdf = ds.pdf;
                int lightNum = ds.offset;
                final Light light = scene.lights.get(lightNum);
                Light.LiResult lir = light.Sample_Li(pt.GetInteraction(), sampler.Get2D());
                Spectrum lightWeight = lir.spectrum;
                Light.VisibilityTester vis = lir.vis;
                Vector3f wi = lir.wi;
                float pdf = lir.pdf;

                if (pdf > 0 && !lightWeight.isBlack()) {
                    EndpointInteraction ei = new EndpointInteraction(vis.P1(), light);
                    sampled = CreateLight(ei, lightWeight.scale(1 / (pdf * lightPdf)), 0);
                    sampled.pdfFwd = sampled.PdfLightOrigin(scene, pt, lightDistr, lightToIndex);
                    L = pt.beta.multiply(pt.f(sampled, Material.TransportMode.Radiance).multiply(sampled.beta));
                    if (pt.IsOnSurface()) L = L.scale(Normal3f.AbsDot(wi, pt.ns()));
                    // Only check visibility if the path would carry radiance.
                    if (!L.isBlack()) L = L.multiply(vis.Tr(scene, sampler));
                }
            }
        } else {
            // Handle all other bidirectional connection cases
            final Vertex qs = lightVertices[s - 1], pt = cameraVertices[t - 1];
            if (qs.IsConnectible() && pt.IsConnectible()) {
                L = qs.beta.multiply(qs.f(pt, Material.TransportMode.Importance).multiply(pt.f(qs, Material.TransportMode.Radiance).multiply(pt.beta)));
                //VLOG(2) << "General connect s: " << s << ", t: " << t <<
                //        " qs: " << qs << ", pt: " << pt << ", qs.f(pt): " << qs.f(pt, Material.TransportMode.Importance) <<
                //        ", pt.f(qs): " << pt.f(qs, Material.TransportMode.Radiance) << ", G: " << G(scene, sampler, qs, pt) <<
                //        ", dist^2: " << Point3f.DistanceSquared(qs.p(), pt.p());
                if (!L.isBlack()) L = L.multiply(G(scene, sampler, qs, pt));
            }
        }

        zeroRadiancePerc.incrementDenom(1); //++totalPaths;
        if (L.isBlack()) zeroRadiancePerc.incrementNumer(1); // ++zeroRadiancePaths;
        pathLength.ReportValue(s + t - 2);

        // Compute MIS weight for connection strategy
        float weight = L.isBlack() ? 0 : MISWeight(scene, lightVertices, cameraVertices,
                sampled, s, t, lightDistr, lightToIndex);
        //VLOG(2) << "MIS weight for (s,t) = (" << s << ", " << t << ") connection: " << weight;
        assert(!Float.isNaN(weight));
        L = L.scale(weight);
        if (misWeight != null) misWeight[0] = weight;
        return L;
    }

    enum VertexType {
        Camera,
        Light,
        Surface,
        Medium
    }

    public VertexType type;
    public Spectrum beta;

    public Interaction interaction; // EndpointInteraction, MediumInteraction or SurfaceInteraction
    public boolean delta = false;
    public float pdfFwd = 0, pdfRev = 0;

    public Vertex() {
        this.interaction = new EndpointInteraction();
    }
    public Vertex(VertexType type, EndpointInteraction ei, Spectrum beta) {
        this.type = type;
        this.beta = beta;
        this.interaction = ei;
    }
    public Vertex(SurfaceInteraction si, Spectrum beta) {
        this.type = VertexType.Surface;
        this.beta = beta;
        this.interaction = si;
    }
    public Vertex(MediumInteraction mi, Spectrum beta) {
        this.type = VertexType.Medium;
        this.beta = beta;
        this.interaction = mi;
    }

    public static Vertex CreateCamera(Camera camera, Ray ray, Spectrum beta) {
        return new Vertex(VertexType.Camera, new EndpointInteraction(camera, ray), beta);
    }
    public static Vertex CreateCamera(Camera camera, Interaction it, Spectrum beta) {
        return new Vertex(VertexType.Camera, new EndpointInteraction(it, camera), beta);
    }
    public static Vertex CreateLight(Light light, Ray ray, Normal3f nLight, Spectrum Le, float pdf) {
        Vertex v = new Vertex(VertexType.Light, new EndpointInteraction(light, ray, nLight), Le);
        v.pdfFwd = pdf;
        return v;
    }
    public static Vertex CreateLight(EndpointInteraction ei, Spectrum beta, float pdf) {
        Vertex v = new Vertex(VertexType.Light, ei, beta);
        v.pdfFwd = pdf;
        return v;
    }
    public static Vertex CreateMedium(MediumInteraction mi, Spectrum beta, float pdf, Vertex prev) {
        Vertex v = new Vertex(mi, beta);
        v.pdfFwd = prev.ConvertDensity(pdf, v);
        return v;
    }
    public static Vertex CreateSurface(SurfaceInteraction si, Spectrum beta, float pdf, Vertex prev) {
        Vertex v = new Vertex(si, beta);
        v.pdfFwd = prev.ConvertDensity(pdf, v);
        return v;
    }

    public Interaction GetInteraction() { return interaction; }

    public Point3f p() { return GetInteraction().p; }
    public float time() { return GetInteraction().time; }
    public Normal3f ng() { return GetInteraction().n; }
    public Normal3f ns() {
        if (type == VertexType.Surface) {
            SurfaceInteraction si = (SurfaceInteraction)interaction;
            return si.shading.n;
        }
        else
            return GetInteraction().n;
    }
    public boolean IsOnSurface() { return ng().notEqual(new Normal3f()); }

    public Spectrum f(Vertex next, Material.TransportMode mode) {
        Vector3f wi = next.p().subtract(p());
        if (wi.LengthSquared() == 0) return new Spectrum(0);
        wi = Vector3f.Normalize(wi);
        switch (type) {
            case Surface:
                SurfaceInteraction si = (SurfaceInteraction)interaction;
                return (si.bsdf.f(si.wo, wi)).scale(CorrectShadingNormal(si, si.wo, wi, mode));
            case Medium:
                MediumInteraction mi = (MediumInteraction)interaction;
                return new Spectrum(mi.phase.p(mi.wo, wi));
            default:
                PBrtTLogger.Error("Vertex::f(): Unimplemented");
                return new Spectrum(0);
        }
    }

    public boolean IsConnectible() {
        switch (type) {
            case Medium:
                return true;
            case Light:
                EndpointInteraction ei = (EndpointInteraction)interaction;
                return (ei.light.flags & Light.FlagDeltaDirection) == 0;
            case Camera:
                return true;
            case Surface:
                SurfaceInteraction si = (SurfaceInteraction)interaction;
                return si.bsdf.NumComponents(BxDF.BSDF_DIFFUSE | BxDF.BSDF_GLOSSY | BxDF.BSDF_REFLECTION |
                    BxDF.BSDF_TRANSMISSION) > 0;
        }
        PBrtTLogger.Error("Unhandled vertex type in IsConnectable()");
        return false;  // NOTREACHED
    }
    public boolean IsLight() {
        SurfaceInteraction si = (SurfaceInteraction)interaction;
        return (type == VertexType.Light) || ((type == VertexType.Surface) && (si.primitive.GetAreaLight() != null));
    }
    public boolean IsDeltaLight() {
        EndpointInteraction ei = (EndpointInteraction)interaction;
        return type == VertexType.Light && ei.light != null && Light.IsDeltaLight(ei.light.flags);
    }
    public boolean IsInfiniteLight() {
        EndpointInteraction ei = (EndpointInteraction)interaction;
        return type == VertexType.Light &&
                (ei.light == null || (ei.light.flags & Light.FlagInfinite) != 0 || (ei.light.flags & Light.FlagDeltaDirection) != 0);
    }
    public Spectrum Le(Scene scene, Vertex v) {
        if (!IsLight()) return new Spectrum(0);
        Vector3f w = v.p().subtract(p());
        if (w.LengthSquared() == 0) return new Spectrum(0);
        w = Vector3f.Normalize(w);
        if (IsInfiniteLight()) {
            // Return emitted radiance for infinite light sources
            Spectrum Le = new Spectrum(0);
            for (Light light : scene.infiniteLights)
            Le = Le.add(light.Le(new RayDifferential(p(), w.negate())));
            return Le;
        } else {
            SurfaceInteraction si = (SurfaceInteraction)interaction;
            AreaLight light = si.primitive.GetAreaLight();
            assert (light != null);
            return light.L(si, w);
        }
    }

    public String toString() {
        String s = "[Vertex type: ";
        switch (type) {
            case Camera:
                s += "camera";
                break;
            case Light:
                s += "light";
                break;
            case Surface:
                s += "surface";
                break;
            case Medium:
                s += "medium";
                break;
        }
        s += " connectible: ";
        s += IsConnectible() ? "true" : "false";
        s += String.format("\n  p: [ %f, %f, %f ] ng: [ %f, %f, %f ]", p().x, p().y, p().z, ng().x, ng().y, ng().z);
        s += String.format("\n  pdfFwd: %f pdfRev: %f beta: ", pdfFwd, pdfRev) + beta.toString();
        switch (type) {
            case Camera:
                // TODO
                break;
            case Light:
                // TODO
                break;
            case Surface:
                SurfaceInteraction si = (SurfaceInteraction)interaction;
                s += "\n  bsdf: " + si.bsdf.toString();
                break;
            case Medium:
                MediumInteraction mi = (MediumInteraction)interaction;
                s += "\n  phase: " + mi.phase.toString();
                break;
        }
        s += " ]";
        return s;
    }

    public float ConvertDensity(float pdf, Vertex next) {
        // Return solid angle density if _next_ is an infinite area light
        if (next.IsInfiniteLight()) return pdf;
        Vector3f w = next.p().subtract(p());
        if (w.LengthSquared() == 0) return 0;
        float invDist2 = 1 / w.LengthSquared();
        if (next.IsOnSurface())
            pdf *= Normal3f.AbsDot(next.ng(), w.scale((float)Math.sqrt(invDist2)));
        return pdf * invDist2;
    }

    public float Pdf(Scene scene, Vertex prev, Vertex next) {
        if (type == VertexType.Light) return PdfLight(scene, next);
        // Compute directions to preceding and next vertex
        Vector3f wn = next.p().subtract(p());
        if (wn.LengthSquared() == 0) return 0;
        wn = Vector3f.Normalize(wn);
        Vector3f wp = new Vector3f();
        if (prev != null) {
            wp = prev.p().subtract(p());
            if (wp.LengthSquared() == 0) return 0;
            wp = Vector3f.Normalize(wp);
        } else {
            assert (type == VertexType.Camera);
        }

        // Compute directional density depending on the vertex types
        float pdf = 0;
        if (type == VertexType.Camera) {
            EndpointInteraction ei = (EndpointInteraction)interaction;
            Camera.CameraPdf cp = ei.camera.Pdf_We(ei.SpawnRay(wn));
            pdf = cp.pdfDir;
        }
        else if (type == VertexType.Surface) {
            SurfaceInteraction si = (SurfaceInteraction)interaction;
            pdf = si.bsdf.Pdf(wp, wn);
        }
        else if (type == VertexType.Medium) {
            MediumInteraction mi = (MediumInteraction)interaction;
            pdf = mi.phase.p(wp, wn);
        }
        else {
            PBrtTLogger.Error("Vertex::Pdf(): Unimplemented");
        }

        // Return probability per unit area at vertex _next_
        return ConvertDensity(pdf, next);
    }

    public float PdfLight(Scene scene, Vertex v) {
        Vector3f w = v.p().subtract(p());
        float invDist2 = 1 / w.LengthSquared();
        w = w.scale((float)Math.sqrt(invDist2));
        float pdf;
        if (IsInfiniteLight()) {
            // Compute planar sampling density for infinite light sources
            Bounds3f.BoundSphere bs = scene.WorldBound().BoundingSphere();
            Point3f worldCenter = bs.center;
            float worldRadius = bs.radius;
            pdf = 1 / (Pbrt.Pi * worldRadius * worldRadius);
        } else {
            // Get pointer _light_ to the light source at the vertex
            assert (IsLight());
            EndpointInteraction ei = (EndpointInteraction)interaction;
            SurfaceInteraction si = (SurfaceInteraction)interaction;
            Light light = type == VertexType.Light ? ei.light : si.primitive.GetAreaLight();
            assert (light != null);

            // Compute sampling density for non-infinite light sources
            Light.PdfResult pr = light.Pdf_Le(new Ray(p(), w, time(), 0, null), ng());
            pdf = pr.pdfDir * invDist2;
        }
        if (v.IsOnSurface()) pdf *= Normal3f.AbsDot(v.ng(), w);
        return pdf;
    }

    public float PdfLightOrigin(Scene scene, Vertex v, Distribution1D lightDistr,
                         HashMap<Light, Integer> lightToDistrIndex) {
        Vector3f w = v.p().subtract(p());
        if (w.LengthSquared() == 0) return 0;
        w = Vector3f.Normalize(w);
        if (IsInfiniteLight()) {
            // Return solid angle density for infinite light sources
            return InfiniteLightDensity(scene, lightDistr, lightToDistrIndex, w);
        } else {
            // Return solid angle density for non-infinite light sources

            // Get pointer _light_ to the light source at the vertex
            assert (IsLight());
            EndpointInteraction ei = (EndpointInteraction)interaction;
            SurfaceInteraction si = (SurfaceInteraction)interaction;
            Light light = (type == VertexType.Light) ? ei.light : si.primitive.GetAreaLight();
            assert (light != null);

            // Compute the discrete probability of sampling _light_, _pdfChoice_
            assert (lightToDistrIndex.containsKey(light));
            int index = lightToDistrIndex.get(light);
            float pdfChoice = lightDistr.DiscretePDF(index);

            Light.PdfResult pr = light.Pdf_Le(new Ray(p(), w, time(), 0, null), ng());
            return pr.pdfPos * pdfChoice;
        }
    }

    private static int RandomWalk(Scene scene, RayDifferential ray, Sampler sampler,
                   Spectrum beta, float pdf, int maxDepth,
                   Material.TransportMode mode, Vertex[] path, int pathStart) {
        if (maxDepth == 0) return 0;
        int bounces = 0;
        // Declare variables for forward and reverse probability densities
        float pdfFwd = pdf, pdfRev = 0;
        while (true) {
            // Attempt to create the next subpath vertex in _path_
            MediumInteraction mi = new MediumInteraction();

            //VLOG(2) << "Random walk. Bounces " << bounces << ", beta " << beta << ", pdfFwd " << pdfFwd << ", pdfRev " << pdfRev;
            // Trace a ray and sample the medium, if any
            SurfaceInteraction isect = scene.Intersect(ray);
            boolean foundIntersection = (isect != null);
            if (ray.medium != null) {
                Medium.MediumSample ms = ray.medium.Sample(ray, sampler);
                beta = beta.multiply(ms.spectrum);
                mi = ms.mi;
            }
            if (beta.isBlack()) break;
            Vertex vertex = path[pathStart+bounces], prev = path[pathStart+bounces - 1];
            if (mi.IsValid()) {
                // Record medium interaction in _path_ and compute forward density
                vertex = CreateMedium(mi, beta, pdfFwd, prev);
                if (++bounces >= maxDepth) break;

                // Sample direction and compute reverse density at preceding vertex
                PhaseFunction.PhaseSample ps = mi.phase.Sample_p(ray.d.negate(), sampler.Get2D());
                Vector3f wi = ps.wi;
                pdfFwd = ps.phase;
                pdfRev = ps.phase;
                ray = new RayDifferential(mi.SpawnRay(wi));
            } else {
                // Handle surface interaction for path generation
                if (!foundIntersection) {
                    // Capture escaped rays when tracing from the camera
                    if (mode == Material.TransportMode.Radiance) {
                        vertex = CreateLight(new EndpointInteraction(ray), beta, pdfFwd);
                        ++bounces;
                    }
                    break;
                }

                // Compute scattering functions for _mode_ and skip over medium
                // boundaries
                isect.ComputeScatteringFunctions(ray, true, mode);
                if (isect.bsdf == null) {
                    ray = new RayDifferential(isect.SpawnRay(ray.d));
                    continue;
                }

                // Initialize _vertex_ with surface intersection information
                vertex = CreateSurface(isect, beta, pdfFwd, prev);
                if (++bounces >= maxDepth) break;

                // Sample BSDF at current vertex and compute reverse probability
                Vector3f wo = isect.wo;
                BxDF.BxDFSample bs = isect.bsdf.Sample_f(wo, sampler.Get2D(), BxDF.BSDF_ALL);
                Vector3f wi = bs.wiWorld;
                int type = bs.sampledType;
                Spectrum f = bs.f;

                //VLOG(2) << "Random walk sampled dir " << wi << " f: " << f << ", pdfFwd: " << pdfFwd;
                if (f.isBlack() || pdfFwd == 0) break;
                beta = beta.multiply(f.scale(Normal3f.AbsDot(wi, isect.shading.n) / pdfFwd));
                //VLOG(2) << "Random walk beta now " << beta;
                pdfRev = isect.bsdf.Pdf(wi, wo, BxDF.BSDF_ALL);
                if ((type & BxDF.BSDF_SPECULAR) != 0) {
                    vertex.delta = true;
                    pdfRev = pdfFwd = 0;
                }
                beta = beta.scale(CorrectShadingNormal(isect, wo, wi, mode));
                //VLOG(2) << "Random walk beta after shading normal correction " << beta;
                ray = new RayDifferential(isect.SpawnRay(wi));
            }

            // Compute reverse area density at preceding vertex
            prev.pdfRev = vertex.ConvertDensity(pdfRev, prev);
        }
        return bounces;
    }

    private static Spectrum G(Scene scene, Sampler sampler, Vertex v0, Vertex v1) {
        Vector3f d = v0.p().subtract(v1.p());
        float g = 1 / d.LengthSquared();
        d = d.scale((float)Math.sqrt(g));
        if (v0.IsOnSurface()) g *= Normal3f.AbsDot(v0.ns(), d);
        if (v1.IsOnSurface()) g *= Normal3f.AbsDot(v1.ns(), d);
        Light.VisibilityTester vis = new Light.VisibilityTester(v0.GetInteraction(), v1.GetInteraction());
        return vis.Tr(scene, sampler).scale(g);
    }

    private static class ScopedAssignment<T> {
        public ScopedAssignment() {
            this.target = null;
            this.backup = null;
        }

        public ScopedAssignment(T target, T value) {
            this.target = target;
            if (this.target != null) {
                this.backup = this.target;
                this.target = value;
            }
        }

        public void release() {
            if (target != null) {
                target = backup;
            }
        }

        private T target, backup;
    }

    private static float MISWeight(Scene scene, Vertex[] lightVertices, Vertex[] cameraVertices, Vertex sampled, int s, int t,
                                   Distribution1D lightPdf, HashMap<Light, Integer> lightToIndex) {
        if (s + t == 2) return 1;
        float sumRi = 0;
        // Define helper function _remap0_ that deals with Dirac delta functions
        Function<Float, Float> remap0 = (f) -> f != 0 ? f : 1;

        // Temporarily update vertex properties for current strategy

        // Look up connection vertices and their predecessors
        Vertex qs = s > 0 ? lightVertices[s - 1] : null,
           pt = t > 0 ? cameraVertices[t - 1] : null,
           qsMinus = s > 1 ? lightVertices[s - 2] : null,
           ptMinus = t > 1 ? cameraVertices[t - 2] : null;

        // Update sampled vertex for $s=1$ or $t=1$ strategy
        ScopedAssignment<Vertex> a1 = new ScopedAssignment<>();
        if (s == 1)
            a1 = new ScopedAssignment<>(qs, sampled);
        else if (t == 1)
            a1 = new ScopedAssignment<>(pt, sampled);

        // Mark connection vertices as non-degenerate
        ScopedAssignment<Boolean> a2 = new ScopedAssignment<>(), a3 = new ScopedAssignment<>();
        if (pt != null) a2 = new ScopedAssignment<>(pt.delta, false);
        if (qs != null) a3 = new ScopedAssignment<>(qs.delta, false);

        // Update reverse density of vertex $\pt{}_{t-1}$
        ScopedAssignment<Float> a4 = new ScopedAssignment<>();
        if (pt != null)
            a4 = new ScopedAssignment<>(pt.pdfRev, s > 0 ? qs.Pdf(scene, qsMinus, pt)
                                 : pt.PdfLightOrigin(scene, ptMinus, lightPdf, lightToIndex));

        // Update reverse density of vertex $\pt{}_{t-2}$
        ScopedAssignment<Float> a5 = new ScopedAssignment<>();
        if (ptMinus != null)
            a5 = new ScopedAssignment<>(ptMinus.pdfRev, s > 0 ? pt.Pdf(scene, qs, ptMinus)
                                      : pt.PdfLight(scene, ptMinus));

        // Update reverse density of vertices $\pq{}_{s-1}$ and $\pq{}_{s-2}$
        ScopedAssignment<Float> a6 = new ScopedAssignment<>();
        if (qs != null) a6 = new ScopedAssignment<>(qs.pdfRev, pt.Pdf(scene, ptMinus, qs));
        ScopedAssignment<Float> a7 = new ScopedAssignment<>();
        if (qsMinus != null) a7 = new ScopedAssignment<>(qsMinus.pdfRev, qs.Pdf(scene, pt, qsMinus));

        // Consider hypothetical connection strategies along the camera subpath
        float ri = 1;
        for (int i = t - 1; i > 0; --i) {
            ri *= remap0.apply(cameraVertices[i].pdfRev) / remap0.apply(cameraVertices[i].pdfFwd);
            if (!cameraVertices[i].delta && !cameraVertices[i - 1].delta)
                sumRi += ri;
        }

        // Consider hypothetical connection strategies along the light subpath
        ri = 1;
        for (int i = s - 1; i >= 0; --i) {
            ri *= remap0.apply(lightVertices[i].pdfRev) / remap0.apply(lightVertices[i].pdfFwd);
            boolean deltaLightvertex = i > 0 ? lightVertices[i - 1].delta
                    : lightVertices[0].IsDeltaLight();
            if (!lightVertices[i].delta && !deltaLightvertex) sumRi += ri;
        }

        a1.release();
        a2.release();
        a3.release();
        a4.release();
        a5.release();
        a6.release();
        a7.release();

        return 1 / (1 + sumRi);
    }

    private static Stats.Percent zeroRadiancePerc = new Stats.Percent("Integrator/Zero-radiance paths"); // zeroRadiancePaths, totalPaths
    private static Stats.IntegerDistribution pathLength = new Stats.IntegerDistribution("Integrator/Path length");
}
