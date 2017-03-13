
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
import java.util.concurrent.atomic.AtomicReference;

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
            hashTable[i].distribution = new AtomicReference(null);
        }

        Api.logger.info("SpatialLightDistribution: scene bounds %s, voxel res (%d, %d, %d)", b.toString(), nVoxels[0], nVoxels[1], nVoxels[2]);
    }

    @Override
    public Distribution1D Lookup(Point3f p) {
        //Stats.ProfilePhase pp = new Stats.ProfilePhase(Stats.Prof.LightDistribLookup);
        looksPerDistrib.incrementDenom(1); // nLookups

        // First, compute integer voxel coordinates for the given point |p|
        // with respect to the overall voxel grid.
        Vector3f offset = scene.WorldBound().Offset(p);  // offset in [0,1].
        Point3i pi = new Point3i();
        for (int i = 0; i < 3; ++i)
            // The clamp should almost never be necessary, but is there to be
            // robust to computed intersection points being slightly outside
            // the scene bounds due to floating-point roundoff error.
            pi.set(i, Pbrt.Clamp((int)(offset.at(i) * nVoxels[i]), 0, nVoxels[i] - 1));

        // Pack the 3D integer voxel coordinates into a single 64-bit value.
        long packedPos = ((long)(pi.x) << 40) | ((long)(pi.y) << 20) | pi.z;
        assert (packedPos != invalidPackedPos);

        // Compute a hash value from the packed voxel coordinates.  We could
        // just take packedPos mod the hash table size, but since packedPos
        // isn't necessarily well distributed on its own, it's worthwhile to do
        // a little work to make sure that its bits values are individually
        // fairly random. For details of and motivation for the following, see:
        // http://zimbry.blogspot.ch/2011/09/better-bit-mixing-improving-on.html
        long hash = packedPos;
        hash ^= (hash >> 31);
        hash *= 0x7fb5d329728ea185L;
        hash ^= (hash >> 27);
        hash *= 0x81dadef4bc2dd44dL;
        hash ^= (hash >> 33);
        hash %= hashTableSize;
        assert (hash >= 0);

        // Now, see if the hash table already has an entry for the voxel. We'll
        // use quadratic probing when the hash table entry is already used for
        // another value; step stores the square root of the probe step.
        int step = 1;
        int nProbes = 0;
        while (true) {
            ++nProbes;
            HashEntry entry = hashTable[(int)hash];
            // Does the hash table entry at offset |hash| match the current point?
            long entryPackedPos = entry.packedPos.get();
            if (entryPackedPos == packedPos) {
                // Yes! Most of the time, there should already by a light
                // sampling distribution available.
                Distribution1D dist = entry.distribution.get();
                if (dist == null) {
                    // Rarely, another thread will have already done a lookup
                    // at this point, found that there isn't a sampling
                    // distribution, and will already be computing the
                    // distribution for the point.  In this case, we spin until
                    // the sampling distribution is ready.  We assume that this
                    // is a rare case, so don't do anything more sophisticated
                    // than spinning.
                    //Stats.ProfilePhase ppp = new Stats.ProfilePhase(Stats.Prof.LightDistribSpinWait);
                    while ((dist = entry.distribution.get()) == null)
                        // spin :-(. If we were fancy, we'd have any threads
                        // that hit this instead help out with computing the
                        // distribution for the voxel...
                        ;
                }
                // We have a valid sampling distribution.
                nProbesPerLookup.ReportValue(nProbes);
                return dist;
            } else if (entryPackedPos != invalidPackedPos) {
                // The hash table entry we're checking has already been
                // allocated for another voxel. Advance to the next entry with
                // quadratic probing.
                hash += step * step;
                if (hash >= hashTableSize)
                    hash %= hashTableSize;
                ++step;
            } else {
                // We have found an invalid entry. (Though this may have
                // changed since the load into entryPackedPos above.)  Use an
                // atomic compare/exchange to try to claim this entry for the
                // current position.
                long invalid = invalidPackedPos;
                if (entry.packedPos.weakCompareAndSet(invalid, packedPos)) {
                    // Success; we've claimed this position for this voxel's
                    // distribution. Now compute the sampling distribution and
                    // add it to the hash table. As long as packedPos has been
                    // set but the entry's distribution pointer is nullptr, any
                    // other threads looking up the distribution for this voxel
                    // will spin wait until the distribution pointer is
                    // written.
                    Distribution1D dist = ComputeDistribution(pi);
                    entry.distribution.set(dist);
                    nProbesPerLookup.ReportValue(nProbes);
                    return dist;
                }
            }
        }
    }

    private Distribution1D ComputeDistribution(Point3i pi) {
        //Stats.ProfilePhase pp = new Stats.ProfilePhase(Stats.Prof.LightDistribCreation);
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
            //Api.logger.trace("Voxel pi = %s, light %d contrib = %f", pi.toString(), i, lightContrib[i]);
            lightContrib[i] = Math.max(lightContrib[i], minContrib);
        }
        //Api.logger.info("Initialized light distribution in voxel pi= %s, avgContrib = %f", pi.toString(), avgContrib);

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
        AtomicReference<Distribution1D> distribution;
    }
    private HashEntry[] hashTable;
    private int hashTableSize;

    private static Stats.STAT_COUNTER nCreated = new Stats.STAT_COUNTER("SpatialLightDistribution/Distributions created");
    private static Stats.STAT_RATIO looksPerDistrib = new Stats.STAT_RATIO("SpatialLightDistribution/Lookups per distribution");
    private static Stats.STAT_INT_DISTRIBUTION nProbesPerLookup = new Stats.STAT_INT_DISTRIBUTION("SpatialLightDistribution/Hash probes per lookup");

}