
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
import org.pbrt.samplers.HaltonSampler;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class SPPMIntegrator extends Integrator {

    public SPPMIntegrator(Camera camera, int nIterations, int photonsPerIteration, int maxDepth, float initialSearchRadius, int writeFrequency) {
        super();
        this.camera = camera;
        this.initialSearchRadius = initialSearchRadius;
        this.nIterations = nIterations;
        this.maxDepth = maxDepth;
        this.photonsPerIteration = (photonsPerIteration > 0) ? photonsPerIteration : camera.film.croppedPixelBounds.Area();
        this.writeFrequency = writeFrequency;
    }

    @Override
    public void Render(Scene scene) {
        //Stats.ProfilePhase p_ = new Stats.ProfilePhase(Stats.Prof.IntegratorRender);
        // Initialize _pixelBounds_ and _pixels_ array for SPPM
        Bounds2i pixelBounds = camera.film.croppedPixelBounds;
        int nPixels = pixelBounds.Area();
        SPPMPixel[] pixels = new SPPMPixel[nPixels];
        for (int i = 0; i < nPixels; ++i) pixels[i].radius = initialSearchRadius;
        final float invSqrtSPP = 1 / (float)Math.sqrt(nIterations);
        pixelMemoryBytes.increment(nPixels * SPPMPixel.sizeof());
        // Compute _lightDistr_ for sampling lights proportional to power
        Distribution1D lightDistr = ComputeLightPowerDistribution(scene);

        // Perform _nIterations_ of SPPM integration
        HaltonSampler sampler = new HaltonSampler(nIterations, pixelBounds);

        // Compute number of tiles to use for SPPM camera pass
        Vector2i pixelExtent = pixelBounds.Diagonal();
        final int tileSize = 16;
        Point2i nTiles = new Point2i((pixelExtent.x + tileSize - 1) / tileSize, (pixelExtent.y + tileSize - 1) / tileSize);
        ProgressReporter progress = new ProgressReporter(2 * nIterations, "Rendering");
        for (int iter = 0; iter < nIterations; ++iter) {
            // Generate SPPM visible points
            //std::vector<MemoryArena> perThreadArenas(MaxThreadIndex());
            {
                //Stats.ProfilePhase pp = new Stats.ProfilePhase(Stats.Prof.SPPMCameraPass);
                for (int y = 0; y < nTiles.y; y++) {
                    for (int x = 0; x < nTiles.x; x++) {
                        Point2i tile = new Point2i(x, y);
                        // Follow camera paths for _tile_ in image for SPPM
                        int tileIndex = tile.y * nTiles.x + tile.x;
                        Sampler tileSampler = sampler.Clone(tileIndex);

                        // Compute _tileBounds_ for SPPM tile
                        int x0 = pixelBounds.pMin.x + tile.x * tileSize;
                        int x1 = Math.min(x0 + tileSize, pixelBounds.pMax.x);
                        int y0 = pixelBounds.pMin.y + tile.y * tileSize;
                        int y1 = Math.min(y0 + tileSize, pixelBounds.pMax.y);
                        Bounds2i tileBounds = new Bounds2i(new Point2i(x0, y0), new Point2i(x1, y1));
                        for (int py = tileBounds.pMin.y; py < tileBounds.pMax.y; py++) {
                            for (int px = tileBounds.pMin.x; px < tileBounds.pMax.x; px++) {
                                Point2i pPixel = new Point2i(px, py);
                                // Prepare _tileSampler_ for _pPixel_
                                tileSampler.StartPixel(pPixel);
                                tileSampler.SetSampleNumber(iter);

                                // Generate camera ray for pixel for SPPM
                                Camera.CameraSample cameraSample = tileSampler.GetCameraSample(pPixel);
                                Camera.CameraRayDiff camRay = camera.GenerateRayDifferential(cameraSample);
                                RayDifferential ray = camRay.rd;
                                Spectrum beta = new Spectrum(camRay.weight);
                                ray.ScaleDifferentials(invSqrtSPP);

                                // Follow camera ray path until a visible point is created

                                // Get _SPPMPixel_ for _pPixel_
                                Point2i pPixelO = new Point2i(pPixel.subtract(pixelBounds.pMin));
                                int pixelOffset = pPixelO.x + pPixelO.y * (pixelBounds.pMax.x - pixelBounds.pMin.x);
                                SPPMPixel pixel = pixels[pixelOffset];
                                boolean specularBounce = false;
                                for (int depth = 0; depth < maxDepth; ++depth) {
                                    pointsPerInterations.incrementDenom(1);
                                    SurfaceInteraction isect = scene.Intersect(ray);
                                    if (isect == null) {
                                        // Accumulate light contributions for ray with no
                                        // intersection
                                        for (Light light : scene.lights)
                                            pixel.Ld = pixel.Ld.add(beta.multiply(light.Le(ray)));
                                        break;
                                    }
                                    // Process SPPM camera ray intersection

                                    // Compute BSDF at SPPM camera ray intersection
                                    isect.ComputeScatteringFunctions(ray, true, Material.TransportMode.Radiance);
                                    if (isect.bsdf == null) {
                                        ray = new RayDifferential(isect.SpawnRay(ray.d));
                                        --depth;
                                        continue;
                                    }
                                    final BSDF bsdf = isect.bsdf;

                                    // Accumulate direct illumination at SPPM camera ray
                                    // intersection
                                    Vector3f wo = ray.d.negate();
                                    if (depth == 0 || specularBounce)
                                        pixel.Ld = pixel.Ld.add(beta.multiply(isect.Le(wo)));
                                    pixel.Ld = pixel.Ld.add(beta.multiply(SamplerIntegrator.UniformSampleOneLight(isect, scene, tileSampler, false, null)));

                                    // Possibly create visible point and end camera path
                                    boolean isDiffuse = bsdf.NumComponents(BxDF.BSDF_DIFFUSE | BxDF.BSDF_REFLECTION | BxDF.BSDF_TRANSMISSION) > 0;
                                    boolean isGlossy = bsdf.NumComponents(BxDF.BSDF_GLOSSY | BxDF.BSDF_REFLECTION | BxDF.BSDF_TRANSMISSION) > 0;
                                    if (isDiffuse || (isGlossy && depth == maxDepth - 1)) {
                                        pixel.vp = new SPPMPixel.VisiblePoint(isect.p, wo, bsdf, beta);
                                        break;
                                    }

                                    // Spawn ray from SPPM camera path vertex
                                    if (depth < maxDepth - 1) {
                                        final BxDF.BxDFSample bxs = bsdf.Sample_f(wo, tileSampler.Get2D(), BxDF.BSDF_ALL);
                                        float pdf = bxs.pdf;
                                        Vector3f wi = bxs.wiWorld;
                                        int type = bxs.sampledType;
                                        Spectrum f = bxs.f;
                                        if (pdf == 0. || f.isBlack()) break;
                                        specularBounce = (type & BxDF.BSDF_SPECULAR) != 0;
                                        beta = beta.multiply(f.scale(Normal3f.AbsDot(wi, isect.shading.n) / pdf));
                                        if (beta.y() < 0.25f) {
                                            float continueProb = Math.min(1, beta.y());
                                            if (tileSampler.Get1D() > continueProb) break;
                                            beta = beta.scale(1 / continueProb);
                                        }
                                        ray = new RayDifferential(isect.SpawnRay(wi));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            progress.Update(1L);

            // Create grid of all SPPM visible points
            int[] gridRes = new int[3];
            Bounds3f gridBounds = new Bounds3f();
            // Allocate grid for SPPM visible points
            final int hashSize = nPixels;
            AtomicReference<SPPMPixelListNode>[] grid = new AtomicReference[hashSize];
            {
                //Stats.ProfilePhase pp_ = new Stats.ProfilePhase(Stats.Prof.SPPMGridConstruction);

                // Compute grid bounds for SPPM visible points
                float maxRadius = 0;
                for (int i = 0; i < nPixels; ++i) {
                    final SPPMPixel pixel = pixels[i];
                    if (pixel.vp.beta.isBlack()) continue;
                    Bounds3f vpBound = Bounds3f.Expand(new Bounds3f(pixel.vp.p), pixel.radius);
                    gridBounds = Bounds3f.Union(gridBounds, vpBound);
                    maxRadius = Math.max(maxRadius, pixel.radius);
                }

                // Compute resolution of SPPM grid in each dimension
                Vector3f diag = gridBounds.Diagonal();
                float maxDiag = Vector3f.MaxComponent(diag);
                int baseGridRes = (int)(maxDiag / maxRadius);
                assert (baseGridRes > 0);
                for (int i = 0; i < 3; ++i)
                    gridRes[i] = Math.max((int)(baseGridRes * diag.at(i) / maxDiag), 1);

                // Add visible points to SPPM grid
                for (int pixelIndex = 0; pixelIndex< nPixels; pixelIndex += 4096) {
                    //MemoryArena &arena = perThreadArenas[ThreadIndex];
                    SPPMPixel pixel = pixels[pixelIndex];
                    if (!pixel.vp.beta.isBlack()) {
                        // Add pixel's visible point to applicable grid cells
                        float radius = pixel.radius;
                        Point3i pMin = new Point3i(), pMax = new Point3i();
                        ToGrid(pixel.vp.p.subtract(new Vector3f(radius, radius, radius)), gridBounds, gridRes, pMin);
                        ToGrid(pixel.vp.p.add(new Vector3f(radius, radius, radius)), gridBounds, gridRes, pMax);
                        for (int z = pMin.z; z <= pMax.z; ++z) {
                            for (int y = pMin.y; y <= pMax.y; ++y) {
                                for (int x = pMin.x; x <= pMax.x; ++x) {
                                    // Add visible point to grid cell $(x, y, z)$
                                    int h = hash(new Point3i(x, y, z), hashSize);
                                    SPPMPixelListNode node = new SPPMPixelListNode();
                                    node.pixel = pixel;

                                    // Atomically add _node_ to the start of
                                    // _grid[h]_'s linked list
                                    node.next = grid[h].get();
                                    while (grid[h].weakCompareAndSet(node.next, node) == false) ;
                                }
                            }
                        }
                        gridCellsPerVisiblePoint.ReportValue((1 + pMax.x - pMin.x) * (1 + pMax.y - pMin.y) * (1 + pMax.z - pMin.z));
                    }
                }
            }

            // Trace photons and accumulate contributions
            {
                //Stats.ProfilePhase pp_ = new Stats.ProfilePhase(Stats.Prof.SPPMPhotonPass);
                //std::vector<MemoryArena> photonShootArenas(MaxThreadIndex());
                for(int photonIndex = 0; photonIndex < photonsPerIteration; photonIndex += 8192) {
                    //MemoryArena &arena = photonShootArenas[ThreadIndex];
                    // Follow photon path for _photonIndex_
                    long haltonIndex = (long)iter * (long)photonsPerIteration + photonIndex;
                    int haltonDim = 0;

                    // Choose light to shoot photon from
                    float lightSample = LowDiscrepancy.RadicalInverse(haltonDim++, haltonIndex);
                    Distribution1D.DiscreteSample ds = lightDistr.SampleDiscrete(lightSample);
                    float lightPdf = ds.pdf;
                    int lightNum = ds.offset;

                    final Light light = scene.lights.get(lightNum);

                    // Compute sample values for photon ray leaving light source
                    Point2f uLight0 = new Point2f(LowDiscrepancy.RadicalInverse(haltonDim, haltonIndex),
                            LowDiscrepancy.RadicalInverse(haltonDim + 1, haltonIndex));
                    Point2f uLight1 = new Point2f(LowDiscrepancy.RadicalInverse(haltonDim + 2, haltonIndex),
                            LowDiscrepancy.RadicalInverse(haltonDim + 3, haltonIndex));
                    float uLightTime = Pbrt.Lerp(LowDiscrepancy.RadicalInverse(haltonDim + 4, haltonIndex),
                                    camera.shutterOpen, camera.shutterClose);
                    haltonDim += 5;

                    // Generate _photonRay_ from light source and initialize _beta_
                    Light.LeResult ler = light.Sample_Le(uLight0, uLight1, uLightTime);
                    RayDifferential photonRay = new RayDifferential(ler.ray);
                    Normal3f nLight = ler.nLight;
                    float pdfPos = ler.pdfPos, pdfDir = ler.pdfDir;
                    Spectrum Le = ler.spectrum;

                    if (pdfPos == 0 || pdfDir == 0 || Le.isBlack()) return;
                    Spectrum beta = Le.scale(Normal3f.AbsDot(nLight, photonRay.d) / (lightPdf * pdfPos * pdfDir));
                    if (beta.isBlack()) return;

                    // Follow photon path through scene and record intersections
                    SurfaceInteraction isect;
                    for (int depth = 0; depth < maxDepth; ++depth) {
                        isect = scene.Intersect(photonRay);
                        if (isect == null) break;
                        pointsPerInterations.incrementDenom(1); // ++totalPhotonSurfaceInteractions;
                        if (depth > 0) {
                            // Add photon contribution to nearby visible points
                            Point3i photonGridIndex = new Point3i();
                            if (ToGrid(isect.p, gridBounds, gridRes, photonGridIndex)) {
                                int h = hash(photonGridIndex, hashSize);
                                // Add photon contribution to visible points in
                                // _grid[h]_
                                for (SPPMPixelListNode node = grid[h].get(); node != null; node = node.next) {
                                    pointsPerInterations.incrementNumer(1); //++visiblePointsChecked;
                                    SPPMPixel pixel = node.pixel;
                                    float radius = pixel.radius;
                                    if (Point3f.DistanceSquared(pixel.vp.p, isect.p) > radius * radius)
                                        continue;
                                    // Update _pixel_ $\Phi$ and $M$ for nearby
                                    // photon
                                    Vector3f wi = photonRay.d.negate();
                                    Spectrum Phi = beta.multiply(pixel.vp.bsdf.f(pixel.vp.wo, wi));
                                    for (int i = 0; i < Spectrum.nSamples; ++i)
                                        pixel.Phi[i].add(Phi.at(i));
                                    pixel.M.incrementAndGet();
                                }
                            }
                        }
                        // Sample new photon ray direction

                        // Compute BSDF at photon intersection point
                        isect.ComputeScatteringFunctions(photonRay, true, Material.TransportMode.Importance);
                        if (isect.bsdf == null) {
                            --depth;
                            photonRay = new RayDifferential(isect.SpawnRay(photonRay.d));
                            continue;
                        }
                        final BSDF photonBSDF = isect.bsdf;

                        // Sample BSDF _fr_ and direction _wi_ for reflected photon
                        Vector3f wo = photonRay.d.negate();

                        // Generate _bsdfSample_ for outgoing photon sample
                        Point2f bsdfSample = new Point2f(LowDiscrepancy.RadicalInverse(haltonDim, haltonIndex),
                                LowDiscrepancy.RadicalInverse(haltonDim + 1, haltonIndex));
                        haltonDim += 2;
                        BxDF.BxDFSample bxs = photonBSDF.Sample_f(wo, bsdfSample, BxDF.BSDF_ALL);
                        float pdf = bxs.pdf;
                        int flags = bxs.sampledType;
                        Vector3f wi = bxs.wiWorld;
                        Spectrum fr = bxs.f;
                        if (fr.isBlack() || pdf == 0) break;
                        Spectrum bnew = beta.multiply(fr.scale(Normal3f.AbsDot(wi, isect.shading.n) / pdf));

                        // Possibly terminate photon path with Russian roulette
                        float q = Math.max(0, 1 - bnew.y() / beta.y());
                        if (LowDiscrepancy.RadicalInverse(haltonDim++, haltonIndex) < q) break;
                        beta = bnew.scale(1 / (1 - q));
                        photonRay = new RayDifferential(isect.SpawnRay(wi));
                    }
                    //arena.Reset();
                }
                progress.Update(1);
                photonPaths.increment((long)photonsPerIteration);
            }

            // Update pixel values from this pass's photons
            {
                //Stats.ProfilePhase pp_ = new Stats.ProfilePhase(Stats.Prof.SPPMStatsUpdate);
                for(int i = 0; i < nPixels; i += 4096) {
                    SPPMPixel p = pixels[i];
                    if (p.M.get() > 0) {
                        // Update pixel photon count, search radius, and $\tau$ from
                        // photons
                        float gamma = 2.0f / 3.0f;
                        float Nnew = p.N + gamma * p.M.get();
                        float Rnew = p.radius * (float)Math.sqrt(Nnew / (p.N + p.M.get()));
                        Spectrum Phi = new Spectrum(0);
                        for (int j = 0; j < Spectrum.nSamples; ++j)
                            Phi.set(j, p.Phi[j].get());
                        p.tau = (p.tau.add(p.vp.beta.multiply(Phi))).scale((Rnew * Rnew) / (p.radius * p.radius));
                        p.N = Nnew;
                        p.radius = Rnew;
                        p.M.set(0);
                        for (int j = 0; j < Spectrum.nSamples; ++j)
                            p.Phi[j].set(0);
                    }
                    // Reset _VisiblePoint_ in pixel
                    p.vp.beta = new Spectrum(0);
                    p.vp.bsdf = null;
                }
            }

            // Periodically store SPPM image in film and write image
            if (iter + 1 == nIterations || ((iter + 1) % writeFrequency) == 0) {
                int x0 = pixelBounds.pMin.x;
                int x1 = pixelBounds.pMax.x;
                long Np = (long)(iter + 1) * (long)photonsPerIteration;
                Spectrum[] image = new Spectrum[pixelBounds.Area()];
                int offset = 0;
                for (int y = pixelBounds.pMin.y; y < pixelBounds.pMax.y; ++y) {
                    for (int x = x0; x < x1; ++x) {
                        // Compute radiance _L_ for SPPM pixel _pixel_
                        final SPPMPixel pixel = pixels[(y - pixelBounds.pMin.y) * (x1 - x0) + (x - x0)];
                        Spectrum L = pixel.Ld.scale(1.0f / (iter + 1));
                        L = L.add(pixel.tau.scale(1 / (Np * Pbrt.Pi * pixel.radius * pixel.radius)));
                        image[offset++] = L;
                    }
                }
                camera.film.SetImage(image);
                camera.film.WriteImage(1);
                // Write SPPM radius image, if requested
                if (!System.getenv("SPPM_RADIUS").isEmpty()) {
                    float[] rimg = new float[3 * pixelBounds.Area()];
                    float minrad = 1e30f, maxrad = 0;
                    for (int y = pixelBounds.pMin.y; y < pixelBounds.pMax.y; ++y) {
                        for (int x = x0; x < x1; ++x) {
                            final SPPMPixel p = pixels[(y - pixelBounds.pMin.y) * (x1 - x0) + (x - x0)];
                            minrad = Math.min(minrad, p.radius);
                            maxrad = Math.max(maxrad, p.radius);
                        }
                    }
                    System.err.format("iterations: %d (%.2f s) radius range: %f - %f\n", iter + 1, progress.ElapsedMS() / 1000, minrad, maxrad);
                    offset = 0;
                    for (int y = pixelBounds.pMin.y; y < pixelBounds.pMax.y; ++y) {
                        for (int x = x0; x < x1; ++x) {
                            final SPPMPixel p = pixels[(y - pixelBounds.pMin.y) * (x1 - x0) + (x - x0)];
                            float v = 1.f - (p.radius - minrad) / (maxrad - minrad);
                            rimg[offset++] = v;
                            rimg[offset++] = v;
                            rimg[offset++] = v;
                        }
                    }
                    Point2i res = new Point2i(pixelBounds.pMax.x - pixelBounds.pMin.x, pixelBounds.pMax.y - pixelBounds.pMin.y);
                    ImageIO.Write("sppm_radius.png", rimg, pixelBounds, res);
                }
            }
        }
        progress.Done();
    }

    public static Integrator Create(ParamSet params, Camera camera) {
        int nIterations = params.FindOneInt("iterations", params.FindOneInt("numiterations", 64));
        int maxDepth = params.FindOneInt("maxdepth", 5);
        int photonsPerIter = params.FindOneInt("photonsperiteration", -1);
        int writeFreq = params.FindOneInt("imagewritefrequency", 1 << 31);
        float radius = params.FindOneFloat("radius", 1);
        if (Pbrt.options.QuickRender) nIterations = Math.max(1, nIterations / 16);
        return new SPPMIntegrator(camera, nIterations, photonsPerIter, maxDepth, radius, writeFreq);
    }

    private Camera camera;
    private final float initialSearchRadius;
    private final int nIterations;
    private final int maxDepth;
    private final int photonsPerIteration;
    private final int writeFrequency;

    Stats.STAT_RATIO pointsPerInterations = new Stats.STAT_RATIO("Stochastic Progressive Photon Mapping/Visible points checked per photon intersection"); // visiblePointsChecked, totalPhotonSurfaceInteractions
    Stats.STAT_COUNTER photonPaths = new Stats.STAT_COUNTER("Stochastic Progressive Photon Mapping/Photon paths followed");
    Stats.STAT_INT_DISTRIBUTION gridCellsPerVisiblePoint = new Stats.STAT_INT_DISTRIBUTION("Stochastic Progressive Photon Mapping/Grid cells per visible point");
    Stats.STAT_MEMORY_COUNTER pixelMemoryBytes = new Stats.STAT_MEMORY_COUNTER("Memory/SPPM Pixels");
    Stats.STAT_FLOAT_DISTRIBUTION memoryArenaMB = new Stats.STAT_FLOAT_DISTRIBUTION("Memory/SPPM BSDF and Grid Memory");

    // SPPM Local Definitions
    private static class SPPMPixel {
        // SPPMPixel Public Methods
        public SPPMPixel() {
            this.M.set(0);
        }

        // SPPMPixel Public Data
        public float radius = 0;
        public Spectrum Ld = new Spectrum(0);

        public static int sizeof() {
            return 1;
        }

        private static class VisiblePoint {
            // VisiblePoint Public Methods
            public VisiblePoint() {
                this.bsdf = null;
            }
            public VisiblePoint(Point3f p, Vector3f wo, BSDF bsdf, Spectrum beta) {
                this.p = p;
                this.wo = wo;
                this.bsdf = bsdf;
                this.beta = beta;
            }

            Point3f p;
            Vector3f wo;
            BSDF bsdf;
            Spectrum beta;
        }
        VisiblePoint vp = new VisiblePoint();
        AtomicFloat[] Phi = new AtomicFloat[Spectrum.nSamples];
        AtomicInteger M = new AtomicInteger(0);
        float N = 0;
        Spectrum tau;
    }

    private static class SPPMPixelListNode {
        public SPPMPixel pixel;
        public SPPMPixelListNode next;
    }

    private static boolean ToGrid(Point3f p, Bounds3f bounds, int[] gridRes, Point3i pi) {
        boolean inBounds = true;
        Vector3f pg = bounds.Offset(p);
        for (int i = 0; i < 3; ++i) {
            pi.set(i, (int)(gridRes[i] * pg.at(i)));
            inBounds &= (pi.at(i) >= 0 && pi.at(i) < gridRes[i]);
            pi.set(i, Pbrt.Clamp(pi.at(i), 0, gridRes[i] - 1));
        }
        return inBounds;
    }

    private static int hash(Point3i p, int hashSize) {
        return ((p.x * 73856093) ^ (p.y * 19349663) ^
                (p.z * 83492791)) %
                hashSize;
    }

}