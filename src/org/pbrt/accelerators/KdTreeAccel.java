
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KdTreeAccel extends Aggregate {

    public static Primitive Create(Primitive[] prims, ParamSet paramSet) {
        int isectCost = paramSet.FindOneInt("intersectcost", 80);
        int travCost = paramSet.FindOneInt("traversalcost", 1);
        float emptyBonus = paramSet.FindOneFloat("emptybonus", 0.5f);
        int maxPrims = paramSet.FindOneInt("maxprims", 1);
        int maxDepth = paramSet.FindOneInt("maxdepth", -1);
        return new KdTreeAccel(prims, isectCost, travCost, emptyBonus, maxPrims, maxDepth);
    }

    public KdTreeAccel(Primitive[] p, int isectCost, int traversalCost, float emptyBonus, int maxPrims, int maxDepth) {
        this.isectCost = isectCost;
        this.traversalCost = traversalCost;
        this.maxPrims = maxPrims;
        this.emptyBonus = emptyBonus;
        this.primitives = p;

        // Build kd-tree for accelerator
        Stats.ProfilePhase pp = new Stats.ProfilePhase(Stats.Prof.AccelConstruction);

        nextFreeNode = nAllocedNodes = 0;
        if (maxDepth <= 0)
            maxDepth = Math.round(8 + 1.3f * Pbrt.Log2Int(primitives.length));

        // Compute bounds for kd-tree construction
        Bounds3f[] primBounds = new Bounds3f[primitives.length];
        int pi = 0;
        for (Primitive prim : primitives) {
            Bounds3f b = prim.WorldBound();
            bounds = Bounds3f.Union(bounds, b);
            primBounds[pi++] = b;
        }

        // Allocate working memory for kd-tree construction
        ArrayList<BoundEdge>[] edges = new ArrayList[3];
        for (int i = 0; i < 3; ++i) {
            edges[i] = new ArrayList<>(2 * primitives.length);
        }
        int[] prims0 = new int[primitives.length];
        int[] prims1 = new int[(maxDepth + 1) * primitives.length];

        // Initialize _primNums_ for kd-tree construction
        int[] primNums = new int[primitives.length];
        for (int i = 0; i < primitives.length; ++i) primNums[i] = i;

        // Start recursive construction of kd-tree
        buildTree(0, bounds, primBounds, primNums, primitives.length,
                maxDepth, edges, prims0, prims1, 0);
    }

    public KdTreeAccel(Primitive[] p) {
        this(p, 80, 1, 0.5f, 1, -1);
    }

    @Override
    public Bounds3f WorldBound() {
        return bounds;
    }

    @Override
    public SurfaceInteraction Intersect(Ray ray) {
        Stats.ProfilePhase pp = new Stats.ProfilePhase(Stats.Prof.AccelIntersect);
        // Compute initial parametric range of ray inside kd-tree extent
        Bounds3f.BoundIntersect bisect = bounds.IntersectP(ray);
        if (bisect == null){
            return null;
        }
        float tMin = bisect.hit0;
        float tMax = bisect.hit1;
        // Prepare to traverse kd-tree for ray
        Vector3f invDir = new Vector3f(1 / ray.d.x, 1 / ray.d.y, 1 / ray.d.z);
        final int maxTodo = 64;
        KdToDo[] todo = new KdToDo[maxTodo];
        int todoPos = 0;

        // Traverse kd-tree nodes in order for ray
        SurfaceInteraction isect = null, hitIsect = null;
        int nodeNdx = 0;
        KdAccelNode node = nodes[nodeNdx];
        while (node != null) {
            // Bail out if we found a hit closer than the current node
            if (ray.tMax < tMin) break;
            if (!node.IsLeaf()) {
                // Process kd-tree interior node

                // Compute parametric distance along ray to split plane
                int axis = node.SplitAxis();
                float tPlane = (node.SplitPos() - ray.o.at(axis)) * invDir.at(axis);

                // Get node children pointers for ray
                int firstChild, secondChild;
                int belowFirst = (ray.o.at(axis) < node.SplitPos()) || ((ray.o.at(axis) == node.SplitPos()) && (ray.d.at(axis) <= 0)) ? 1 : 0;
                if (belowFirst != 0) {
                    firstChild = nodeNdx + 1;
                    secondChild = node.AboveChild();
                } else {
                    firstChild = node.AboveChild();
                    secondChild = nodeNdx + 1;
                }

                // Advance to next child node, possibly enqueue other child
                if (tPlane > tMax || tPlane <= 0)
                    nodeNdx = firstChild;
                else if (tPlane < tMin)
                    nodeNdx = secondChild;
                else {
                    // Enqueue _secondChild_ in todo list
                    todo[todoPos].nodeNdx = secondChild;
                    todo[todoPos].tMin = tPlane;
                    todo[todoPos].tMax = tMax;
                    ++todoPos;
                    nodeNdx = firstChild;
                    tMax = tPlane;
                }
                node = nodes[nodeNdx];
            } else {
                // Check for intersections inside leaf node
                int nPrimitives = node.nPrimitives();
                if (nPrimitives == 1) {
                Primitive p = primitives[node.onePrimitive];
                // Check one primitive inside leaf node
                isect = p.Intersect(ray);
                if (isect != null)
                    hitIsect = isect;
                } else {
                    for (int i = 0; i < nPrimitives; ++i) {
                        int index = primitiveIndices.get(node.primitiveIndicesOffset + i);
                        Primitive p = primitives[index];
                        // Check one primitive inside leaf node
                        isect = p.Intersect(ray);
                        if (isect != null) hitIsect = isect;
                    }
                }

                // Grab next node to process from todo list
                if (todoPos > 0) {
                    --todoPos;
                    nodeNdx = todo[todoPos].nodeNdx;
                    tMin = todo[todoPos].tMin;
                    tMax = todo[todoPos].tMax;
                    node = nodes[nodeNdx];
                } else
                    break;
            }
        }
        return hitIsect;
    }

    @Override
    public boolean IntersectP(Ray ray) {
        Stats.ProfilePhase pp = new Stats.ProfilePhase(Stats.Prof.AccelIntersectP);
        // Compute initial parametric range of ray inside kd-tree extent
        Bounds3f.BoundIntersect bisect = bounds.IntersectP(ray);
        if (bisect == null) {
            return false;
        }
        float tMin = bisect.hit0;
        float tMax = bisect.hit1;

        // Prepare to traverse kd-tree for ray
        Vector3f invDir = new Vector3f(1 / ray.d.x, 1 / ray.d.y, 1 / ray.d.z);
        int maxTodo = 64;
        KdToDo[] todo = new KdToDo[maxTodo];
        int todoPos = 0;
        int nodeNdx = 0;
        KdAccelNode node = nodes[nodeNdx];
        while (node != null) {
            if (node.IsLeaf()) {
                // Check for shadow ray intersections inside leaf node
                int nPrimitives = node.nPrimitives();
                if (nPrimitives == 1) {
                 Primitive p = primitives[node.onePrimitive];
                    if (p.IntersectP(ray)) {
                        return true;
                    }
                } else {
                    for (int i = 0; i < nPrimitives; ++i) {
                        int primitiveIndex = primitiveIndices.get(node.primitiveIndicesOffset + i);
                        Primitive prim = primitives[primitiveIndex];
                        if (prim.IntersectP(ray)) {
                            return true;
                        }
                    }
                }

                // Grab next node to process from todo list
                if (todoPos > 0) {
                    --todoPos;
                    nodeNdx = todo[todoPos].nodeNdx;
                    tMin = todo[todoPos].tMin;
                    tMax = todo[todoPos].tMax;
                    node = nodes[nodeNdx];
                } else
                    break;
            } else {
                // Process kd-tree interior node

                // Compute parametric distance along ray to split plane
                int axis = node.SplitAxis();
                float tPlane = (node.SplitPos() - ray.o.at(axis)) * invDir.at(axis);

                // Get node children pointers for ray
                int firstChild, secondChild;
                int belowFirst = (ray.o.at(axis) < node.SplitPos()) || (ray.o.at(axis) == node.SplitPos() && ray.d.at(axis) <= 0) ? 1 : 0;
                if (belowFirst != 0) {
                    firstChild = nodeNdx + 1;
                    secondChild = node.AboveChild();
                } else {
                    firstChild = node.AboveChild();
                    secondChild = nodeNdx + 1;
                }

                // Advance to next child node, possibly enqueue other child
                if (tPlane > tMax || tPlane <= 0) {
                    nodeNdx = firstChild;
                } else if (tPlane < tMin) {
                    nodeNdx = secondChild;
                }
                else {
                    // Enqueue _secondChild_ in todo list
                    todo[todoPos].nodeNdx = secondChild;
                    todo[todoPos].tMin = tPlane;
                    todo[todoPos].tMax = tMax;
                    ++todoPos;
                    nodeNdx = firstChild;
                    tMax = tPlane;
                }
                node = nodes[nodeNdx];
            }
        }
        return false;
    }

    private void buildTree(int nodeNum, Bounds3f nodeBounds,
                   Bounds3f[] allPrimBounds, int[] primNums,
                   int nPrimitives, int depth,
                   ArrayList<BoundEdge>[] edges, int[] prims0,
                   int[] prims1, int badRefines) {
        assert (nodeNum == nextFreeNode);
        // Get next free node from _nodes_ array
        if (nextFreeNode == nAllocedNodes) {
            int nNewAllocNodes = Math.max(2 * nAllocedNodes, 512);
            KdAccelNode[] n = new KdAccelNode[nNewAllocNodes];
            if (nAllocedNodes > 0) {
                for (int i = 0; i < nAllocedNodes; i++) {
                    n[i] = nodes[i]; // TODO: implement cloneable for KdAccelNode
                }
            }
            nodes = n;
            nAllocedNodes = nNewAllocNodes;
        }
        ++nextFreeNode;

        // Initialize leaf node if termination criteria met
        if (nPrimitives <= maxPrims || depth == 0) {
            nodes[nodeNum].InitLeaf(primNums, nPrimitives, primitiveIndices);
            return;
        }

        // Initialize interior node and continue recursion

        // Choose split axis position for interior node
        int bestAxis = -1, bestOffset = -1;
        float bestCost = Pbrt.Infinity;
        float oldCost = isectCost * (float) (nPrimitives);
        float totalSA = nodeBounds.SurfaceArea();
        float invTotalSA = 1 / totalSA;
        Vector3f d = nodeBounds.pMax.subtract(nodeBounds.pMin);

        // Choose which axis to split along
        int axis = nodeBounds.MaximumExtent();
        int retries = 0;
        boolean retrySplit = true;
        while (retrySplit)
        {
            // Initialize edges for _axis_
            for (int i = 0; i < nPrimitives; ++i) {
                int pn = primNums[i];
                Bounds3f bounds = allPrimBounds[pn];
                edges[axis].add(new BoundEdge(bounds.pMin.at(axis), pn, true));
                edges[axis].add(new BoundEdge(bounds.pMax.at(axis), pn, false));
            }

            // Sort _edges_ for _axis_
            List<BoundEdge> sortedEdges = edges[axis].subList(0, 2 * nPrimitives);
            Collections.sort(sortedEdges);
            for (int i = 0; i < sortedEdges.size(); i++) {
                edges[axis].set(i, sortedEdges.get(i));
            }

            // Compute cost of all splits for _axis_ to find best
            int nBelow = 0, nAbove = nPrimitives;
            for (int i = 0; i < 2 * nPrimitives; ++i) {
                if (edges[axis].get(i).type == EdgeType.End) --nAbove;
                float edgeT = edges[axis].get(i).t;
                if (edgeT > nodeBounds.pMin.at(axis) && edgeT < nodeBounds.pMax.at(axis)) {
                    // Compute cost for split at _i_th edge

                    // Compute child surface areas for split at _edgeT_
                    int otherAxis0 = (axis + 1) % 3, otherAxis1 = (axis + 2) % 3;
                    float belowSA = 2 * (d.at(otherAxis0) * d.at(otherAxis1) + (edgeT - nodeBounds.pMin.at(axis)) *
                            (d.at(otherAxis0) + d.at(otherAxis1)));
                    float aboveSA = 2 * (d.at(otherAxis0) * d.at(otherAxis1) + (nodeBounds.pMax.at(axis) - edgeT) *
                            (d.at(otherAxis0) + d.at(otherAxis1)));
                    float pBelow = belowSA * invTotalSA;
                    float pAbove = aboveSA * invTotalSA;
                    float eb = (nAbove == 0 || nBelow == 0) ? emptyBonus : 0;
                    float cost = traversalCost + isectCost * (1 - eb) * (pBelow * nBelow + pAbove * nAbove);

                    // Update best split if this is lowest cost so far
                    if (cost < bestCost) {
                        bestCost = cost;
                        bestAxis = axis;
                        bestOffset = i;
                    }
                }
                if (edges[axis].get(i).type == EdgeType.Start) ++nBelow;
            }
            assert (nBelow == nPrimitives && nAbove == 0);

            // Create leaf if no good splits were found
            if (bestAxis == -1 && retries < 2) {
                ++retries;
                axis = (axis + 1) % 3;
                retrySplit = true;
            }
            else {
                retrySplit = false;
            }
        }
        if (bestCost > oldCost) ++badRefines;
        if ((bestCost > 4 * oldCost && nPrimitives < 16) || bestAxis == -1 ||
                badRefines == 3) {
            nodes[nodeNum].InitLeaf(primNums, nPrimitives, primitiveIndices);
            return;
        }

        // Classify primitives with respect to split
        int n0 = 0, n1 = 0;
        for (int i = 0; i < bestOffset; ++i)
            if (edges[bestAxis].get(i).type == EdgeType.Start)
                prims0[n0++] = edges[bestAxis].get(i).primNum;
        for (int i = bestOffset + 1; i < 2 * nPrimitives; ++i)
            if (edges[bestAxis].get(i).type == EdgeType.End)
                prims1[n1++] = edges[bestAxis].get(i).primNum;

        // Recursively initialize children nodes
        float tSplit = edges[bestAxis].get(bestOffset).t;
        Bounds3f bounds0 = nodeBounds, bounds1 = nodeBounds;
        bounds0.pMax.set(bestAxis, tSplit);
        bounds1.pMin.set(bestAxis, tSplit);
/*
        buildTree(nodeNum + 1, bounds0, allPrimBounds, prims0, n0, depth - 1, edges,
                prims0, prims1 + nPrimitives, badRefines);
        int aboveChild = nextFreeNode;
        nodes[nodeNum].InitInterior(bestAxis, aboveChild, tSplit);
        buildTree(aboveChild, bounds1, allPrimBounds, prims1, n1, depth - 1, edges,
                prims0, prims1 + nPrimitives, badRefines);
*/
    }

    private final int isectCost, traversalCost, maxPrims;
    private final float emptyBonus;
    private Primitive[] primitives;
    private ArrayList<Integer> primitiveIndices = new ArrayList<>();
    private KdAccelNode[] nodes;
    private int nAllocedNodes, nextFreeNode;
    private Bounds3f bounds;

    private static class KdAccelNode {
        // KdAccelNode Methods
        public void InitLeaf(int[] primNums, int np, ArrayList<Integer> primitiveIndices) {
            flags = 3;
            nPrims |= (np << 2);
            // Store primitive ids for leaf node
            if (np == 0)
                onePrimitive = 0;
            else if (np == 1)
                onePrimitive = primNums[0];
            else {
                primitiveIndicesOffset = primitiveIndices.size();
                for (int i = 0; i < np; ++i) primitiveIndices.add(primNums[i]);
            }
        }
        public void InitInterior(int axis, int ac, Float s) {
            split = s;
            flags = axis;
            aboveChild |= (ac << 2);
        }
        public float SplitPos() { return split; }
        public int nPrimitives() { return nPrims >> 2; }
        public int SplitAxis() { return flags & 3; }
        public boolean IsLeaf() { return (flags & 3) == 3; }
        public int AboveChild() { return aboveChild >> 2; }

        //union {
        public float split;                 // Interior
        public int onePrimitive;            // Leaf
        public int primitiveIndicesOffset;  // Leaf
        //}

        //union {
        private int flags;       // Both
        private int nPrims;      // Leaf
        private int aboveChild;  // Interior
        //}
    }


    private enum EdgeType { Start, End }
    private static class BoundEdge implements Comparable<BoundEdge> {
        // BoundEdge Public Methods
        public BoundEdge() {}
        public BoundEdge(float t, int primNum, boolean starting) {
            this.t = t;
            this.primNum = primNum;
            this.type = starting ? EdgeType.Start : EdgeType.End;
        }
        public float t = 0;
        public int primNum = 0;
        public EdgeType type = EdgeType.Start;

        @Override
        public int compareTo(@NotNull BoundEdge e1) {
            if (this.t == e1.t) {
                if (this.type == e1.type) return 0;
                return (this.type.ordinal() < e1.type.ordinal()) ? -1 : 1;
            }
            else {
                return (this.t < e1.t) ? -1 : 1;
            }
        }
    }

    private static class KdToDo {
        int nodeNdx;
        float tMin, tMax;
    }

}