
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

import java.util.concurrent.atomic.AtomicLong;

public class SpatialLightDistribution extends LightDistribution {

    // Voxel coordinates are packed into a uint64_t for hash table lookups;
    // 10 bits are allocated to each coordinate.  invalidPackedPos is an impossible
    // packed coordinate value, which we use to represent
    private static final long invalidPackedPos = 0xffffffffffffffffL;

    public SpatialLightDistribution(Scene scene, int maxVoxels) {
        this.scene = scene;

        // Compute the number of voxels so that the widest scene bounding box
        // dimension has maxVoxels voxels and the other dimensions have a number
        // of voxels so that voxels are roughly cube shaped.
        Bounds3f b = scene.WorldBound();
        Vector3f diag = b.Diagonal();
        float bmax = diag.at(b.MaximumExtent());
        for (int i = 0; i < 3; ++i) {
            nVoxels[i] = Math.max(1, Math.round(diag.at(i) / bmax * maxVoxels));
            // In the Lookup() method, we require that 20 or fewer bits be
            // sufficient to represent each coordinate value. It's fairly hard
            // to imagine that this would ever be a problem.
            assert (nVoxels[i] < 1 << 20);
        }

        hashTableSize = 4 * nVoxels[0] * nVoxels[1] * nVoxels[2];
        hashTable = new HashEntry[hashTableSize];
        for (int i = 0; i < hashTableSize; ++i) {
            hashTable[i] = new HashEntry();
            hashTable[i].packedPos = new AtomicLong();
            hashTable[i].packedPos.set(invalidPackedPos);
            hashTable[i].distribution = null;
        }

        Api.logger.info("SpatialLightDistribution: scene bounds %s, voxel res (%d, %d, %d)", b.toString(), nVoxels[0], nVoxels[1], nVoxels[2]);
    }

    @Override
    public Distribution1D Lookup(Point3f p) {
        return null;
    }

    private Distribution1D ComputeDistribution(Point3i pi) {
        Stats.ProfilePhase pp = new Stats.ProfilePhase(Stats.Prof.LightDistribCreation);
        nCreated.increment();
        looksPerDistrib.incrementDenom(1); // nDistributions;

        // Compute the world-space bounding box of the voxel corresponding to
        // |pi|.
        Point3f p0 = new Point3f(pi.x / (float)nVoxels[0], pi.y / (float)nVoxels[1], pi.z / (float)nVoxels[2]);
        Point3f p1 = new Point3f((pi.x + 1) / (float)nVoxels[0], (pi.y + 1) / (float)nVoxels[1], (pi.z + 1) / (float)nVoxels[2]);
        Bounds3f voxelBounds = new Bounds3f(scene.WorldBound().Lerp(p0), scene.WorldBound().Lerp(p1));

        // Compute the sampling distribution. Sample a number of points inside
        // voxelBounds using a 3D Halton sequence; at each one, sample each
        // light source and compute a weight based on Li/pdf for the light's
        // sample (ignoring visibility between the point in the voxel and the
        // point on the light source) as an approximation to how much the light
        // is likely to contribute to illumination in the voxel.
        int nSamples = 128;
        float[] lightContrib = new float[scene.lights.size()];
        for (int i = 0; i < nSamples; ++i) {
            Point3f po = voxelBounds.Lerp(new Point3f(LowDiscrepancy.RadicalInverse(0, i), LowDiscrepancy.RadicalInverse(1, i), LowDiscrepancy.RadicalInverse(2, i)));
            Interaction intr = new Interaction(po, new Normal3f(), new Vector3f(), new Vector3f(1, 0, 0), 0 /* time */, new MediumInterface());

            // Use the next two Halton dimensions to sample a point on the
            // light source.
            Point2f u = new Point2f(LowDiscrepancy.RadicalInverse(3, i), LowDiscrepancy.RadicalInverse(4, i));
            for (int j = 0; j < scene.lights.size(); ++j) {
                Light.LiResult liResult = scene.lights.get(j).Sample_Li(intr, u);
                float pdf = liResult.pdf;
                Vector3f wi = liResult.wi;
                Light.VisibilityTester vis = liResult.vis;
                Spectrum Li = liResult.spectrum;

                if (pdf > 0) {
                    // TODO: look at tracing shadow rays / computing beam
                    // transmittance.  Probably shouldn't give those full weight
                    // but instead e.g. have an occluded shadow ray scale down
                    // the contribution by 10 or something.
                    lightContrib[j] += Li.y() / pdf;
                }
            }
        }

        // We don't want to leave any lights with a zero probability; it's
        // possible that a light contributes to points in the voxel even though
        // we didn't find such a point when sampling above.  Therefore, compute
        // a minimum (small) weight and ensure that all lights are given at
        // least the corresponding probability.
        float sumContrib = 0;
        for (float c : lightContrib) sumContrib += c;
        float avgContrib = sumContrib / (nSamples * lightContrib.length);
        float minContrib = (avgContrib > 0) ? .001f * avgContrib : 1;
        for (int i = 0; i < lightContrib.length; ++i) {
            Api.logger.trace("Voxel pi = %s, light %d contrib = %f", pi.toString(), i, lightContrib[i]);
            lightContrib[i] = Math.max(lightContrib[i], minContrib);
        }
        Api.logger.info("Initialized light distribution in voxel pi= %s, avgContrib = %f", pi.toString(), avgContrib);

        // Compute a sampling distribution from the accumulated contributions.
        return new Distribution1D(lightContrib);
    }

    private final Scene scene;
    private int[] nVoxels = { 0, 0, 0};

    // The hash table is a fixed number of HashEntry structs (where we
    // allocate more than enough entries in the SpatialLightDistribution
    // constructor). During rendering, the table is allocated without
    // locks, using atomic operations. (See the Lookup() method
    // implementation for details.)
    private static class HashEntry {
        AtomicLong packedPos;
        Distribution1D distribution;
    }
    private HashEntry[] hashTable;
    private int hashTableSize;

    private static Stats.STAT_COUNTER nCreated = new Stats.STAT_COUNTER("SpatialLightDistribution/Distributions created");
    private static Stats.STAT_RATIO looksPerDistrib = new Stats.STAT_RATIO("SpatialLightDistribution/Lookups per distribution");
    private static Stats.STAT_INT_DISTRIBUTION nProbesPerLookup = new Stats.STAT_INT_DISTRIBUTION("SpatialLightDistribution/Hash probes per lookup");

}