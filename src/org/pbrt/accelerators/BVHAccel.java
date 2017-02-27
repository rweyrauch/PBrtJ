
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.accelerators;

import org.jetbrains.annotations.NotNull;
import org.pbrt.core.*;
import org.pbrt.core.Error;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

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
        ArrayList<Primitive> orderedPrims = new ArrayList<>(primitives.length);
        BVHBuildNode root;
        if (splitMethod == SplitMethod.HLBVH)
            root = HLBVHBuild(primitiveInfo, orderedPrims);
        else
            root = recursiveBuild(primitiveInfo, 0, primitives.length, orderedPrims);

        for (int i = 0; i < orderedPrims.size(); i++) {
            primitives[i] = orderedPrims.get(i);
        }
        Error.Warning("BVH created with %d nodes for %d primitives.\n", totalNodes, primitives.length);

        // Compute representation of depth-first traversal of BVH tree
        //treeBytes += totalNodes * sizeof(LinearBVHNode) + sizeof(*this) +
         //       primitives.size() * sizeof(primitives[0]);
        nodes = new LinearBVHNode[totalNodes];
        for (int i = 0; i < nodes.length; i++)
            nodes[i] = new LinearBVHNode();
        int offset = flattenBVHTree(root, 0);
        assert(totalNodes == offset);

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
        SurfaceInteraction isect = null;
        //ProfilePhase p(Prof::AccelIntersect);
        boolean hit = false;
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
                            hit = true;
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
        return isect;
    }

    @Override
    public boolean IntersectP(Ray ray) {
        if (nodes == null) return false;
        //ProfilePhase p(Prof::AccelIntersectP);
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

    private BVHBuildNode recursiveBuild(ArrayList<BVHPrimitiveInfo> primitiveInfo, int start, int end, ArrayList<Primitive> orderedPrims) {
        assert (start != end);
        BVHBuildNode node = new BVHBuildNode();
        totalNodes++;
        // Compute bounds of all primitives in BVH node
        Bounds3f bounds = new Bounds3f();
        for (int i = start; i < end; ++i)
            bounds = Bounds3f.Union(bounds, primitiveInfo.get(i).bounds);
        int nPrimitives = end - start;
        Error.Warning("Number of primitives: %d\n", nPrimitives);
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
                        Collections.sort(primitiveInfo.subList(start, end), new BVHInfoComparable(dim));
                        break;
                    }
                    case SAH:
                    default: {
                        // Partition primitives using approximate SAH
                        if (nPrimitives <= 2) {
                            // Partition primitives into equally-sized subsets
                            mid = (start + end) / 2;
                            Collections.sort(primitiveInfo.subList(start, end), new BVHInfoComparable(dim));
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
                Error.Warning("Split: Start: %d  Mid: %d  End: %d\n", start, mid, end);
                node.InitInterior(dim, recursiveBuild(primitiveInfo, start, mid, orderedPrims),
                        recursiveBuild(primitiveInfo, mid, end, orderedPrims));
            }
        }
        return node;
    }

    private BVHBuildNode HLBVHBuild(ArrayList<BVHPrimitiveInfo> primitiveInfo, ArrayList<Primitive> orderedPrims) {
        return null;
    }

    private BVHBuildNode emitLBVH(
            BVHBuildNode[] buildNodes,
            ArrayList<BVHPrimitiveInfo> primitiveInfo,
            MortonPrimitive[] mortonPrims, int nPrimitives,
            ArrayList<Primitive> orderedPrims,
            AtomicInteger orderedPrimsOffset, int bitIndex) {
        return null;
    }
    private BVHBuildNode buildUpperSAH(ArrayList<BVHBuildNode> treeletRoots, int start, int end) {
        return null;
    }

    int flattenBVHTree(BVHBuildNode node, int offset) {
        LinearBVHNode linearNode = nodes[offset];
        linearNode.bounds = node.bounds;
        int myOffset = offset++;
        if (node.nPrimitives > 0) {
            assert ((node.children[0] == null) && (node.children[1] == null));
            assert (node.nPrimitives < 65536);
            linearNode.primitivesOffset = node.firstPrimOffset;
            linearNode.nPrimitives = (short)node.nPrimitives;
        } else {
            // Create interior flattened BVH node
            linearNode.axis = (char)node.splitAxis;
            linearNode.nPrimitives = 0;
            offset = flattenBVHTree(node.children[0], offset);
            linearNode.secondChildOffset = flattenBVHTree(node.children[1], offset);
        }
        return myOffset;
    }

    private final int maxPrimsInNode;
    private final SplitMethod splitMethod;
    private Primitive[] primitives;
    private LinearBVHNode[] nodes;
    private int totalNodes;

    private static class BVHPrimitiveInfo {
        BVHPrimitiveInfo() {}
        BVHPrimitiveInfo(int primitiveNumber, Bounds3f bounds) {
            this.primitiveNumber = primitiveNumber;
            this.bounds = bounds;
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
            //++leafNodes;
            //++totalLeafNodes;
            //totalPrimitives += n;
        }
        void InitInterior(int axis, BVHBuildNode c0, BVHBuildNode c1) {
            children[0] = c0;
            children[1] = c1;
            bounds = Bounds3f.Union(c0.bounds, c1.bounds);
            splitAxis = axis;
            nPrimitives = 0;
            //++interiorNodes;
        }
        Bounds3f bounds;
        BVHBuildNode[] children = { null, null };
        int splitAxis, firstPrimOffset, nPrimitives;
    }

    private static class MortonPrimitive {
        int primitiveIndex;
        int mortonCode;
    }

    private static class LBVHTreelet {
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
    };

}