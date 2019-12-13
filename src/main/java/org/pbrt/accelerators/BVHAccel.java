
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.accelerators;

import org.pbrt.core.*;
import org.pbrt.core.Error;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class BVHAccel extends Aggregate {

    public enum SplitMethod {
        SAH,
        HLBVH,
        Middle,
        EqualCounts
    }

    public BVHAccel(Primitive[] prims, int maxPrimsInNode, SplitMethod splitMethod) {
        this.maxPrimsInNode = Math.min(255, maxPrimsInNode);
        this.splitMethod = splitMethod;
        this.primitives = prims;

        if (primitives.length == 0) return;
        // Build BVH from _primitives_

        // Initialize _primitiveInfo_ array for primitives
        ArrayList<BVHPrimitiveInfo> primitiveInfo = new ArrayList<>(primitives.length);
        for (int i = 0; i < primitives.length; ++i)
            primitiveInfo.add(new BVHPrimitiveInfo(i, primitives[i].WorldBound()));

        // Build BVH tree for primitives using _primitiveInfo_
        Integer[] totalNodes = { 0 };

        ArrayList<Primitive> orderedPrims = new ArrayList<>(primitives.length);
        BVHBuildNode root;
        if (splitMethod == SplitMethod.HLBVH)
            root = HLBVHBuild(primitiveInfo, totalNodes, orderedPrims);
        else
            root = recursiveBuild(primitiveInfo, 0, primitives.length, totalNodes, orderedPrims);

        for (int i = 0; i < orderedPrims.size(); i++) {
            primitives[i] = orderedPrims.get(i);
        }
        Api.logger.info("BVH created with %d nodes for %d primitives.\n", totalNodes[0], primitives.length);

        // Compute representation of depth-first traversal of BVH tree
        //treeBytes.increment(totalNodes[0] * ObjectSizeCalculator.getObjectSize(new LinearBVHNode()) + ObjectSizeCalculator.getObjectSize(this) + primitives.length * ObjectSizeCalculator.getObjectSize(primitives[0]));

        nodes = new LinearBVHNode[totalNodes[0]];
        for (int i = 0; i < nodes.length; i++)
            nodes[i] = new LinearBVHNode();
        Integer[] offset = { 0 };
        flattenBVHTree(root, offset);
        assert(Objects.equals(totalNodes[0], offset[0]));

    }
    public BVHAccel(Primitive[] prims) {
        this(prims, 1, SplitMethod.SAH);
    }

    @Override
    public Bounds3f WorldBound() {
        return nodes != null ? nodes[0].bounds : new Bounds3f();
    }

    @Override
    public SurfaceInteraction Intersect(Ray ray) {
        if (nodes == null) return null;
        SurfaceInteraction isect = null, hitIsect = null;
        Vector3f invDir = new Vector3f(1 / ray.d.x, 1 / ray.d.y, 1 / ray.d.z);
        int[] dirIsNeg = {invDir.x < 0 ? 1 : 0, invDir.y < 0 ? 1 : 0, invDir.z < 0 ? 1 :0};
        // Follow ray through BVH nodes to find primitive intersections
        int toVisitOffset = 0, currentNodeIndex = 0;
        int[] nodesToVisit = new int[64];
        while (true) {
            LinearBVHNode node = nodes[currentNodeIndex];
            // Check ray against BVH node
            if (node.bounds.IntersectP(ray, invDir, dirIsNeg)) {
                if (node.nPrimitives > 0) {
                    // Intersect ray with primitives in leaf BVH node
                    for (int i = 0; i < node.nPrimitives; ++i) {
                        isect = primitives[node.primitivesOffset + i].Intersect(ray);
                        if (isect != null) {
                            hitIsect = isect;
                        }
                    }
                    if (toVisitOffset == 0) break;
                    currentNodeIndex = nodesToVisit[--toVisitOffset];
                }
                else {
                    // Put far BVH node on _nodesToVisit_ stack, advance to near
                    // node
                    if (dirIsNeg[node.axis] != 0) {
                        nodesToVisit[toVisitOffset++] = currentNodeIndex + 1;
                        currentNodeIndex = node.secondChildOffset;
                    } else {
                        nodesToVisit[toVisitOffset++] = node.secondChildOffset;
                        currentNodeIndex = currentNodeIndex + 1;
                    }
                }
            } else {
                if (toVisitOffset == 0) break;
                currentNodeIndex = nodesToVisit[--toVisitOffset];
            }
        }
        return hitIsect;
    }

    @Override
    public boolean IntersectP(Ray ray) {
        if (nodes == null) return false;
        Vector3f invDir = new Vector3f(1.f / ray.d.x, 1.f / ray.d.y, 1.f / ray.d.z);
        int[] dirIsNeg = {invDir.x < 0 ? 1 : 0, invDir.y < 0 ? 1 : 0, invDir.z < 0 ? 1 : 0};
        int[] nodesToVisit = new int[64];
        int toVisitOffset = 0, currentNodeIndex = 0;
        while (true) {
         LinearBVHNode node = nodes[currentNodeIndex];
            if (node.bounds.IntersectP(ray, invDir, dirIsNeg)) {
                // Process BVH node _node_ for traversal
                if (node.nPrimitives > 0) {
                    for (int i = 0; i < node.nPrimitives; ++i) {
                        if (primitives[node.primitivesOffset + i].IntersectP(ray)) {
                            return true;
                        }
                    }
                    if (toVisitOffset == 0) break;
                    currentNodeIndex = nodesToVisit[--toVisitOffset];
                } else {
                    if (dirIsNeg[node.axis] != 0) {
                        /// second child first
                        nodesToVisit[toVisitOffset++] = currentNodeIndex + 1;
                        currentNodeIndex = node.secondChildOffset;
                    } else {
                        nodesToVisit[toVisitOffset++] = node.secondChildOffset;
                        currentNodeIndex = currentNodeIndex + 1;
                    }
                }
            } else {
                if (toVisitOffset == 0) break;
                currentNodeIndex = nodesToVisit[--toVisitOffset];
            }
        }
        return false;
    }

    public static Primitive Create(Primitive[] prims, ParamSet paramSet) {
        String splitMethodName = paramSet.FindOneString("splitmethod", "sah");
        BVHAccel.SplitMethod splitMethod;
        if (Objects.equals(splitMethodName, "sah"))
            splitMethod = BVHAccel.SplitMethod.SAH;
        else if (Objects.equals(splitMethodName, "hlbvh"))
            splitMethod = BVHAccel.SplitMethod.HLBVH;
        else if (Objects.equals(splitMethodName, "middle"))
            splitMethod = BVHAccel.SplitMethod.Middle;
        else if (Objects.equals(splitMethodName, "equal"))
            splitMethod = BVHAccel.SplitMethod.EqualCounts;
        else {
            Error.Warning("BVH split method \"%s\" unknown.  Using \"sah\".", splitMethodName);
            splitMethod = BVHAccel.SplitMethod.SAH;
        }

        int maxPrimsInNode = paramSet.FindOneInt("maxnodeprims", 4);
        return new BVHAccel(prims, maxPrimsInNode, splitMethod);
    }

    private int partition(ArrayList<BVHPrimitiveInfo> infos, int first, int last, int dim, float pmid) {
        if (first == last) {
            return first;
        }
        first++;
        int part = first;
        if (first == last) {
            if (infos.get(part).centroid.at(dim) < pmid) {
                return first;
            } else {
                return part;
            }
        }
        while (first != last) {
            if (infos.get(part).centroid.at(dim) < pmid) {
                part++;
            } else if (infos.get(first).centroid.at(dim) < pmid) {
                Collections.swap(infos, first, part);
                part++;
            }
            first++;
        }
        return part;
    }

    private int partition(ArrayList<BVHPrimitiveInfo> infos, int first, int last, Comparator<BVHPrimitiveInfo> comp) {
        if (first == last) {
            return first;
        }
        first++;
        int part = first;
        if (first == last) {
            if (comp.compare(infos.get(part), null) < 0) {
                return first;
            } else {
                return part;
            }
        }
        while (first != last) {
            if (comp.compare(infos.get(part), null) < 0) {
                part++;
            } else if (comp.compare(infos.get(first), null) < 0) {
                Collections.swap(infos, first, part);
                part++;
            }
            first++;
        }
        return part;
    }

    private int partitionNodes(ArrayList<BVHBuildNode> nodes, int first, int last, BVHBuildNode node, int dim, Bounds3f centroidBounds, int nBuckets, int minCostSplitBucket) {
        if (first == last) {
            return first;
        }
        float centroid = (node.bounds.pMin.at(dim) + node.bounds.pMax.at(dim)) * 0.5f;
        int b = nBuckets * (int)((centroid - centroidBounds.pMin.at(dim)) / (centroidBounds.pMax.at(dim) - centroidBounds.pMin.at(dim)));
        if (b == nBuckets) b = nBuckets - 1;

        first++;
        int part = first;
        if (first == last) {
            if (b <= minCostSplitBucket) {
                return first;
            } else {
                return part;
            }
        }
        while (first != last) {
            if (b <= minCostSplitBucket) {
                part++;
            } else if (b <= minCostSplitBucket) {
                Collections.swap(nodes, first, part);
                part++;
            }
            first++;
        }
        return part;
    }

    private class BVHInfoComparable implements Comparator<BVHPrimitiveInfo> {
        BVHInfoComparable(int dim) {
            this.dim = dim;
        }

        private int dim;

        @Override
        public int compare(BVHPrimitiveInfo a, BVHPrimitiveInfo b) {
            if (a.centroid.at(dim) < b.centroid.at(dim)) return -1;
            else if (a.centroid.at(dim) > b.centroid.at(dim)) return 1;
            return 0;
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }
    }

    private class BVHSplitComparable implements Comparator<BVHPrimitiveInfo> {
        BVHSplitComparable(int dim, int nBuckets, int minCostSplitBucket, Bounds3f centroidBounds) {
            this.dim = dim;
            this.nBuckets = nBuckets;
            this.minCostSplitBucket = minCostSplitBucket;
            this.centroidBounds = centroidBounds;
        }

        private int dim, nBuckets, minCostSplitBucket;
        private Bounds3f centroidBounds;

        @Override
        public int compare(BVHPrimitiveInfo a, BVHPrimitiveInfo b) {
            int bndx = nBuckets * (int)centroidBounds.Offset(a.centroid).at(dim);
            if (bndx == nBuckets) bndx = nBuckets - 1;
            assert(bndx >= 0);
            assert(bndx < nBuckets);
            if (bndx < minCostSplitBucket) return -1;
            else if (bndx > minCostSplitBucket) return 1;
            return 0;
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }
    }

    private BVHBuildNode recursiveBuild(ArrayList<BVHPrimitiveInfo> primitiveInfo, int start, int end, Integer[] totalNodes, ArrayList<Primitive> orderedPrims) {
        assert (start != end);
        BVHBuildNode node = new BVHBuildNode();
        totalNodes[0]++;
        // Compute bounds of all primitives in BVH node
        Bounds3f bounds = new Bounds3f();
        for (int i = start; i < end; ++i)
            bounds = Bounds3f.Union(bounds, primitiveInfo.get(i).bounds);
        int nPrimitives = end - start;
        Api.logger.info("Number of primitives: %d\n", nPrimitives);
        if (nPrimitives == 1) {
            // Create leaf _BVHBuildNode_
            int firstPrimOffset = orderedPrims.size();
            for (int i = start; i < end; ++i) {
                int primNum = primitiveInfo.get(i).primitiveNumber;
                orderedPrims.add(primitives[primNum]);
            }
            node.InitLeaf(firstPrimOffset, nPrimitives, bounds);
            return node;
        }
        else {
            // Compute bound of primitive centroids, choose split dimension _dim_
            Bounds3f centroidBounds = new Bounds3f();
            for (int i = start; i < end; ++i)
                centroidBounds = Bounds3f.Union(centroidBounds, primitiveInfo.get(i).centroid);
            int dim = centroidBounds.MaximumExtent();

            // Partition primitives into two sets and build children
            int mid = (start + end) / 2;
            if (centroidBounds.pMax.at(dim) == centroidBounds.pMin.at(dim)) {
                // Create leaf _BVHBuildNode_
                int firstPrimOffset = orderedPrims.size();
                for (int i = start; i < end; ++i) {
                    int primNum = primitiveInfo.get(i).primitiveNumber;
                    orderedPrims.add(primitives[primNum]);
                }
                node.InitLeaf(firstPrimOffset, nPrimitives, bounds);
                return node;
            } else {
                // Partition primitives based on _splitMethod_
                switch (splitMethod) {
                    case Middle: {
                        // Partition primitives through node's midpoint
                        float pmid = (centroidBounds.pMin.at(dim) + centroidBounds.pMax.at(dim)) / 2;
                        mid = partition(primitiveInfo, start, end, dim, pmid);
                        // For lots of prims with large overlapping bounding boxes, this
                        // may fail to partition; in that case don't break and fall
                        // through
                        // to EqualCounts.
                        if (mid != start && mid != end) break;
                    }
                    case EqualCounts: {
                        // Partition primitives into equally-sized subsets
                        mid = (start + end) / 2;
                        primitiveInfo.subList(start, end).sort(new BVHInfoComparable(dim));
                        break;
                    }
                    case SAH:
                    default: {
                        // Partition primitives using approximate SAH
                        if (nPrimitives <= 2) {
                            // Partition primitives into equally-sized subsets
                            mid = (start + end) / 2;
                            primitiveInfo.subList(start, end).sort(new BVHInfoComparable(dim));
                        } else {
                            // Allocate _BucketInfo_ for SAH partition buckets
                            final int nBuckets = 12;
                            BucketInfo[] buckets = new BucketInfo[nBuckets];
                            for (int i = 0; i < buckets.length; i++) {
                                buckets[i] = new BucketInfo(0, new Bounds3f());
                            }

                            // Initialize _BucketInfo_ for SAH partition buckets
                            for (int i = start; i < end; ++i) {
                                int b = nBuckets * (int)centroidBounds.Offset(primitiveInfo.get(i).centroid).at(dim);
                                if (b == nBuckets) b = nBuckets - 1;
                                assert(b >= 0);
                                assert(b < nBuckets);
                                buckets[b].count++;
                                buckets[b].bounds = Bounds3f.Union(buckets[b].bounds, primitiveInfo.get(i).bounds);
                            }

                            // Compute costs for splitting after each bucket
                            float[] cost = new float[nBuckets - 1];
                            for (int i = 0; i < nBuckets - 1; ++i) {
                                Bounds3f b0 = new Bounds3f(), b1 = new Bounds3f();
                                int count0 = 0, count1 = 0;
                                for (int j = 0; j <= i; ++j) {
                                    b0 = Bounds3f.Union(b0, buckets[j].bounds);
                                    count0 += buckets[j].count;
                                }
                                for (int j = i + 1; j < nBuckets; ++j) {
                                    b1 = Bounds3f.Union(b1, buckets[j].bounds);
                                    count1 += buckets[j].count;
                                }
                                cost[i] = 1 + (count0 * b0.SurfaceArea() + count1 * b1.SurfaceArea()) / bounds.SurfaceArea();
                            }

                            // Find bucket to split at that minimizes SAH metric
                            float minCost = cost[0];
                            int minCostSplitBucket = 0;
                            for (int i = 1; i < nBuckets - 1; ++i) {
                                if (cost[i] < minCost) {
                                    minCost = cost[i];
                                    minCostSplitBucket = i;
                                }
                            }

                            // Either create leaf or split primitives at selected SAH
                            // bucket
                            float leafCost = nPrimitives;
                            if (nPrimitives > maxPrimsInNode || minCost < leafCost) {
                                mid = partition(primitiveInfo, start, end, new BVHSplitComparable(dim, nBuckets, minCostSplitBucket, centroidBounds));
                            } else {
                                // Create leaf _BVHBuildNode_
                                int firstPrimOffset = orderedPrims.size();
                                for (int i = start; i < end; ++i) {
                                    int primNum = primitiveInfo.get(i).primitiveNumber;
                                    orderedPrims.add(primitives[primNum]);
                                }
                                node.InitLeaf(firstPrimOffset, nPrimitives, bounds);
                                return node;
                            }
                        }
                        break;
                    }
                }
                Api.logger.info("Split: Start: %d  Mid: %d  End: %d\n", start, mid, end);
                node.InitInterior(dim, recursiveBuild(primitiveInfo, start, mid, totalNodes, orderedPrims),
                        recursiveBuild(primitiveInfo, mid, end, totalNodes, orderedPrims));
            }
        }
        return node;
    }

    private BVHBuildNode HLBVHBuild(ArrayList<BVHPrimitiveInfo> primitiveInfo, Integer[] totalNodes, ArrayList<Primitive> orderedPrims) {
        // Compute bounding box of all primitive centroids
        Bounds3f bounds = new Bounds3f();
        for (BVHPrimitiveInfo pi : primitiveInfo)
            bounds = Bounds3f.Union(bounds, pi.centroid);

        // Compute Morton indices of primitives
        final MortonPrimitive[] mortonPrims = new MortonPrimitive[primitiveInfo.size()];
        for (int i = 0; i < mortonPrims.length; i++) mortonPrims[i] = new MortonPrimitive();
        final Bounds3f localBounds = bounds;
        Consumer<Long> mortonFunc = (Long li) -> {
            int i = Math.toIntExact(li);
            // Initialize _mortonPrims[i]_ for _i_th primitive
            final int mortonBits = 10;
            final int mortonScale = 1 << mortonBits;
            mortonPrims[i].primitiveIndex = primitiveInfo.get(i).primitiveNumber;
            Vector3f centroidOffset = localBounds.Offset(primitiveInfo.get(i).centroid);
            mortonPrims[i].mortonCode = EncodeMorton3(centroidOffset.scale(mortonScale));
        };
        Parallel.ParallelFor(mortonFunc, primitiveInfo.size(), 512);

        // Radix sort primitive Morton indices
        MortonPrimitive[] mortonPrimsSorted = RadixSort(mortonPrims);

        // Create LBVH treelets at bottom of BVH

        // Find intervals of primitives for each treelet
        ArrayList<LBVHTreelet> treeletsToBuild = new ArrayList<>();
        for (int start = 0, end = 1; end <= mortonPrimsSorted.length; ++end) {
            int mask = 0b00111111111111000000000000000000;
            if (end == mortonPrimsSorted.length ||
                    ((mortonPrimsSorted[start].mortonCode & mask) !=
                            (mortonPrimsSorted[end].mortonCode & mask))) {
                // Add entry to _treeletsToBuild_ for this treelet
                int nPrimitives = end - start;
                int maxBVHNodes = 2 * nPrimitives;
                BVHBuildNode[] nodes = new BVHBuildNode[maxBVHNodes];
                treeletsToBuild.add(new LBVHTreelet(start, nPrimitives, nodes));
                start = end;
            }
        }

        // Create LBVHs for treelets in parallel
        AtomicInteger atomicTotal = new AtomicInteger(0);
        AtomicInteger orderedPrimsOffset = new AtomicInteger(0);
        orderedPrims.ensureCapacity(primitives.length);
        Consumer<Long> treeletFunc = (Long li) -> {
            int i = Math.toIntExact(li);
            // Generate _i_th LBVH treelet
            Integer[] nodesCreated = { 0 };
            final int firstBitIndex = 29 - 12;
            LBVHTreelet tr = treeletsToBuild.get(i);
            tr.buildNodes[i] =
                    emitLBVH(tr.buildNodes, i, primitiveInfo, mortonPrimsSorted, tr.startIndex,
                    tr.nPrimitives, nodesCreated, orderedPrims,
                     orderedPrimsOffset, firstBitIndex);
            atomicTotal.addAndGet(nodesCreated[0]);
        };
        Parallel.ParallelFor(treeletFunc, treeletsToBuild.size(), 1);
        totalNodes[0] = atomicTotal.get();

        // Create and return SAH BVH from LBVH treelets
        ArrayList<BVHBuildNode> finishedTreelets = new ArrayList<>(treeletsToBuild.size());
        for (LBVHTreelet treelet : treeletsToBuild)
            finishedTreelets.add(treelet.buildNodes[0]);

        return buildUpperSAH(finishedTreelets, 0, finishedTreelets.size(), totalNodes);
    }

    private BVHBuildNode emitLBVH(
            BVHBuildNode[] buildNodes, int nodeIndex,
            ArrayList<BVHPrimitiveInfo> primitiveInfo,
            MortonPrimitive[] mortonPrims, int startPrim, int nPrimitives, Integer[] totalNodes,
            ArrayList<Primitive> orderedPrims,
            AtomicInteger orderedPrimsOffset, int bitIndex) {

        assert (nPrimitives > 0);
        if (bitIndex == -1 || nPrimitives < maxPrimsInNode) {
            // Create and return leaf node of LBVH treelet
            totalNodes[0]++;
            BVHBuildNode node = buildNodes[nodeIndex++];
            Bounds3f bounds = new Bounds3f();
            int firstPrimOffset = orderedPrimsOffset.getAndAdd(nPrimitives);
            for (int i = 0; i < nPrimitives; ++i) {
                int primitiveIndex = mortonPrims[i].primitiveIndex;
                orderedPrims.set(firstPrimOffset + i, primitives[primitiveIndex]);
                bounds = Bounds3f.Union(bounds, primitiveInfo.get(primitiveIndex).bounds);
            }
            node.InitLeaf(firstPrimOffset, nPrimitives, bounds);
            return node;
        } else {
            int mask = 1 << bitIndex;
            // Advance to next subtree level if there's no LBVH split for this bit
            if ((mortonPrims[0].mortonCode & mask) ==
                    (mortonPrims[nPrimitives - 1].mortonCode & mask))
                return emitLBVH(buildNodes, nodeIndex, primitiveInfo, mortonPrims, startPrim, nPrimitives,
                        totalNodes, orderedPrims, orderedPrimsOffset,
                        bitIndex - 1);

            // Find LBVH split point for this dimension
            int searchStart = 0, searchEnd = nPrimitives - 1;
            while (searchStart + 1 != searchEnd) {
                assert (searchStart != searchEnd);
                int mid = (searchStart + searchEnd) / 2;
                if ((mortonPrims[searchStart].mortonCode & mask) ==
                        (mortonPrims[mid].mortonCode & mask))
                    searchStart = mid;
                else {
                    assert ((mortonPrims[mid].mortonCode & mask) == (mortonPrims[searchEnd].mortonCode & mask));
                    searchEnd = mid;
                }
            }
            int splitOffset = searchEnd;
            assert (splitOffset <= nPrimitives - 1);
            assert ((mortonPrims[splitOffset - 1].mortonCode & mask) != (mortonPrims[splitOffset].mortonCode & mask));

            // Create and return interior LBVH node
            totalNodes[0]++;
            BVHBuildNode node = buildNodes[nodeIndex++];
            BVHBuildNode[] lbvh = {
                    emitLBVH(buildNodes, nodeIndex, primitiveInfo, mortonPrims, startPrim, splitOffset,
                            totalNodes, orderedPrims, orderedPrimsOffset,
                            bitIndex - 1),
                    emitLBVH(buildNodes, nodeIndex, primitiveInfo, mortonPrims, startPrim+splitOffset,
                    nPrimitives - splitOffset, totalNodes, orderedPrims,
                    orderedPrimsOffset, bitIndex - 1)};
            int axis = bitIndex % 3;
            node.InitInterior(axis, lbvh[0], lbvh[1]);
            return node;
        }
    }

    private BVHBuildNode buildUpperSAH(ArrayList<BVHBuildNode> treeletRoots, int start, int end, Integer[] totalNodes) {
        assert (start < end);
        int nNodes = end - start;
        if (nNodes == 1) return treeletRoots.get(start);
        totalNodes[0]++;
        BVHBuildNode node = new BVHBuildNode();

        // Compute bounds of all nodes under this HLBVH node
        Bounds3f bounds = new Bounds3f();
        for (int i = start; i < end; ++i)
            bounds = Bounds3f.Union(bounds, treeletRoots.get(i).bounds);

        // Compute bound of HLBVH node centroids, choose split dimension _dim_
        Bounds3f centroidBounds = new Bounds3f();
        for (int i = start; i < end; ++i) {
            Point3f centroid = ((treeletRoots.get(i).bounds.pMin.add(treeletRoots.get(i).bounds.pMax))).scale(0.5f);
            centroidBounds = Bounds3f.Union(centroidBounds, centroid);
        }
        int dim = centroidBounds.MaximumExtent();
        // FIXME: if this hits, what do we need to do?
        // Make sure the SAH split below does something... ?
        assert(centroidBounds.pMax.at(dim) != centroidBounds.pMin.at(dim));

        // Allocate _BucketInfo_ for SAH partition buckets
        final int nBuckets = 12;
        BucketInfo[] buckets = new BucketInfo[nBuckets];

        // Initialize _BucketInfo_ for HLBVH SAH partition buckets
        for (int i = start; i < end; ++i) {
            float centroid = (treeletRoots.get(i).bounds.pMin.at(dim) + treeletRoots.get(i).bounds.pMax.at(dim)) * 0.5f;
            int b = (int)(nBuckets * ((centroid - centroidBounds.pMin.at(dim)) / (centroidBounds.pMax.at(dim) - centroidBounds.pMin.at(dim))));
            if (b == nBuckets) b = nBuckets - 1;
            assert (b >= 0);
            assert (b < nBuckets);
            buckets[b].count++;
            buckets[b].bounds = Bounds3f.Union(buckets[b].bounds, treeletRoots.get(i).bounds);
        }

        // Compute costs for splitting after each bucket
        float[] cost = new float[nBuckets - 1];
        for (int i = 0; i < nBuckets - 1; ++i) {
            Bounds3f b0 = new Bounds3f(), b1 = new Bounds3f();
            int count0 = 0, count1 = 0;
            for (int j = 0; j <= i; ++j) {
                b0 = Bounds3f.Union(b0, buckets[j].bounds);
                count0 += buckets[j].count;
            }
            for (int j = i + 1; j < nBuckets; ++j) {
                b1 = Bounds3f.Union(b1, buckets[j].bounds);
                count1 += buckets[j].count;
            }
            cost[i] = .125f + (count0 * b0.SurfaceArea() + count1 * b1.SurfaceArea()) / bounds.SurfaceArea();
        }

        // Find bucket to split at that minimizes SAH metric
        float minCost = cost[0];
        int minCostSplitBucket = 0;
        for (int i = 1; i < nBuckets - 1; ++i) {
            if (cost[i] < minCost) {
                minCost = cost[i];
                minCostSplitBucket = i;
            }
        }

        int mid = partitionNodes(treeletRoots, start, end, node, dim, centroidBounds, nBuckets, minCostSplitBucket);
        assert (mid > start);
        assert (mid < end);
        node.InitInterior(dim, this.buildUpperSAH(treeletRoots, start, mid, totalNodes),
                this.buildUpperSAH(treeletRoots, mid, end, totalNodes));
        return node;
    }

    int flattenBVHTree(BVHBuildNode node, Integer[] offset) {
        LinearBVHNode linearNode = nodes[offset[0]];
        linearNode.bounds = node.bounds;
        int myOffset = offset[0]++;
        if (node.nPrimitives > 0) {
            assert ((node.children[0] == null) && (node.children[1] == null));
            assert (node.nPrimitives < 65536);
            linearNode.primitivesOffset = node.firstPrimOffset;
            linearNode.nPrimitives = (short)node.nPrimitives;
        } else {
            // Create interior flattened BVH node
            linearNode.axis = (char)node.splitAxis;
            linearNode.nPrimitives = 0;
            flattenBVHTree(node.children[0], offset);
            linearNode.secondChildOffset = flattenBVHTree(node.children[1], offset);
        }
        return myOffset;
    }

    private final int maxPrimsInNode;
    private final SplitMethod splitMethod;
    private Primitive[] primitives;
    private LinearBVHNode[] nodes;

    private static class BVHPrimitiveInfo {
        BVHPrimitiveInfo(int primitiveNumber, Bounds3f bounds) {
            this.primitiveNumber = primitiveNumber;
            this.bounds = new Bounds3f(bounds);
            this.centroid = bounds.pMin.scale(0.5f).add(bounds.pMax.scale(0.5f));
        }
        int primitiveNumber;
        Bounds3f bounds;
        Point3f centroid;
    }

    private static class BVHBuildNode {
        // BVHBuildNode Public Methods
        void InitLeaf(int first, int n, Bounds3f b) {
            firstPrimOffset = first;
            nPrimitives = n;
            bounds = b;
            children[0] = children[1] = null;
            leafNodes.increment();
            primsPerLeaf.incrementDenom(1);
            primsPerLeaf.incrementNumer(n);
        }
        void InitInterior(int axis, BVHBuildNode c0, BVHBuildNode c1) {
            children[0] = c0;
            children[1] = c1;
            bounds = Bounds3f.Union(c0.bounds, c1.bounds);
            splitAxis = axis;
            nPrimitives = 0;
            interiorNodes.increment();
        }
        Bounds3f bounds = new Bounds3f();
        BVHBuildNode[] children = { null, null };
        int splitAxis, firstPrimOffset, nPrimitives;
    }

    private static class MortonPrimitive {
        int primitiveIndex;
        int mortonCode;
    }

    private static class LBVHTreelet {
        public LBVHTreelet(int startIndex, int nPrimitives, BVHBuildNode[] nodes) {
            this.startIndex = startIndex;
            this.nPrimitives = nPrimitives;
            this.buildNodes = nodes;
        }
        int startIndex, nPrimitives;
        BVHBuildNode[] buildNodes;
    }

    private static class LinearBVHNode {
        Bounds3f bounds;

        int primitivesOffset;   // leaf
        int secondChildOffset;  // interior

        short nPrimitives;  // 0 -> interior node
        char axis;          // interior node: xyz
    }

    private static class BucketInfo {
        BucketInfo(int count, Bounds3f bounds) {
            this.count = count;
            this.bounds = bounds;
        }
        int count = 0;
        Bounds3f bounds;
    }

    private static int LeftShift3(int x) {
        assert(x <= (1 << 10));
        if (x == (1 << 10)) --x;

        x = (x | (x << 16)) & 0b00000011000000000000000011111111;
        // x = ---- --98 ---- ---- ---- ---- 7654 3210
        x = (x | (x << 8)) & 0b00000011000000001111000000001111;
        // x = ---- --98 ---- ---- 7654 ---- ---- 3210
        x = (x | (x << 4)) & 0b00000011000011000011000011000011;
        // x = ---- --98 ---- 76-- --54 ---- 32-- --10
        x = (x | (x << 2)) & 0b00001001001001001001001001001001;
        // x = ---- 9--8 --7- -6-- 5--4 --3- -2-- 1--0
        return x;
    }

    private static int EncodeMorton3(Vector3f v) {
        assert(v.x >= 0);
        assert(v.y >= 0);
        assert(v.z >= 0);
        return (LeftShift3((int)v.z) << 2) | (LeftShift3((int)v.y) << 1) | LeftShift3((int)v.x);
    }

    private static MortonPrimitive[] RadixSort(MortonPrimitive[] v) {
        MortonPrimitive[] tempVector = new MortonPrimitive[v.length];
        final int bitsPerPass = 6;
        final int nBits = 30;
        //assert((nBits % bitsPerPass) == 0, "Radix sort bitsPerPass must evenly divide nBits");
        final int nPasses = nBits / bitsPerPass;

        for (int pass = 0; pass < nPasses; ++pass) {
            // Perform one pass of radix sort, sorting _bitsPerPass_ bits
            int lowBit = pass * bitsPerPass;

            // Set in and out vector pointers for radix sort pass
            MortonPrimitive[] in = (pass & 1) != 0 ? tempVector : v;
            MortonPrimitive[] out = (pass & 1) != 0 ? v : tempVector;

            // Count number of zero bits in array for current radix sort bit
            final int nBuckets = 1 << bitsPerPass;
            int[]  bucketCount = new int[nBuckets];
            final int bitMask = (1 << bitsPerPass) - 1;
            for (MortonPrimitive mp : in) {
                int bucket = (mp.mortonCode >>> lowBit) & bitMask;
                assert (bucket >= 0);
                assert (bucket < nBuckets);
                ++bucketCount[bucket];
            }

            // Compute starting index in output array for each bucket
            int[] outIndex = new int[nBuckets];
            outIndex[0] = 0;
            for (int i = 1; i < nBuckets; ++i)
                outIndex[i] = outIndex[i - 1] + bucketCount[i - 1];

            // Store sorted values in output array
            for (MortonPrimitive mp : in) {
                int bucket = (mp.mortonCode >>> lowBit) & bitMask;
                out[outIndex[bucket]++] = mp;
            }
        }
        // Copy final result from _tempVector_, if needed
        if ((nPasses % 2) == 1) {
            v = tempVector;
        }
        return v;
    }

    private static Stats.Ratio primsPerLeaf = new Stats.Ratio("BVH/Primitives per leaf node");
    private static Stats.Counter interiorNodes = new Stats.Counter("BVH/Interior nodes");
    private static Stats.Counter leafNodes = new Stats.Counter("BVH/Leaf nodes");
    private static Stats.MemoryCounter treeBytes = new Stats.MemoryCounter("Memory/BVH tree");
}