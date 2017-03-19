
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.shapes;

import org.pbrt.core.*;
import org.pbrt.core.Error;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class LoopSubdiv {

    public static ArrayList<Shape> Create(Transform object2world, Transform world2object, boolean reverseOrientation, ParamSet paramSet) {
        int nLevels = paramSet.FindOneInt("levels", paramSet.FindOneInt("nlevels", 3));
        Integer[] vertexIndices = paramSet.FindInt("indices");
        Point3f[] P = paramSet.FindPoint3f("P");
        if (vertexIndices == null) {
            Error.Error("Vertex indices \"indices\" not provided for LoopSubdiv shape.");
            return null;
        }
        if (P == null) {
            Error.Error("Vertex positions \"P\" not provided for LoopSubdiv shape.");
            return null;
        }

        // don't actually use this for now...
        String scheme = paramSet.FindOneString("scheme", "loop");
        return LoopSubdivide(object2world, world2object, reverseOrientation, nLevels, vertexIndices.length,
                vertexIndices, P.length, P);
    }

    private static ArrayList<Shape> LoopSubdivide(Transform ObjectToWorld, Transform WorldToObject, boolean reverseOrientation,
                                                  int nLevels, int nIndices, Integer[] vertexIndices, int nVertices, Point3f[] p) {

        ArrayList<SDVertex> vertices = new ArrayList<>();
        ArrayList<SDFace> faces = new ArrayList<>();
        // Allocate _LoopSubdiv_ vertices and faces
        SDVertex[] verts = new SDVertex[nVertices];
        for (int i = 0; i < nVertices; ++i) {
            verts[i] = new SDVertex(p[i]);
            vertices.add(verts[i]);
        }
        int nFaces = nIndices / 3;
        SDFace[] fs = new SDFace[nFaces];
        for (int i = 0; i < nFaces; ++i) faces.add(fs[i]);

        // Set face to vertex pointers
        int vpi = 0;
        for (int i = 0; i < nFaces; ++i, vpi += 3) {
            SDFace f = faces.get(i);
            for (int j = 0; j < 3; ++j) {
                SDVertex v = vertices.get(vertexIndices[vpi + j]);
                f.v[j] = v;
                v.startFace = f;
            }
        }

        // Set neighbor pointers in _faces_
        HashSet<SDEdge> edges = new HashSet<>();
        for (int i = 0; i < nFaces; ++i) {
            SDFace f = faces.get(i);
            for (int edgeNum = 0; edgeNum < 3; ++edgeNum) {
                // Update neighbor pointer for _edgeNum_
                int v0 = edgeNum, v1 = NEXT(edgeNum);
                SDEdge e = new SDEdge(f.v[v0], f.v[v1]);
                if (!edges.contains(e)) {
                    // Handle new edge
                    e.f[0] = f;
                    e.f0edgeNum = edgeNum;
                    edges.add(e);
                } else {
                    // Handle previously seen edge
                    //e = edges.get(e);
                    e.f[0].f[e.f0edgeNum] = f;
                    f.f[edgeNum] = e.f[0];
                    edges.remove(e);
                }
            }
        }

        // Finish vertex initialization
        for (int i = 0; i < nVertices; ++i) {
            SDVertex v = vertices.get(i);
            SDFace f = v.startFace;
            do {
                f = f.nextFace(v);
            } while (f != null && f != v.startFace);
            v.boundary = (f == null);
            if (!v.boundary && v.valence() == 6)
                v.regular = true;
            else if (v.boundary && v.valence() == 4)
                v.regular = true;
            else
                v.regular = false;
        }

        // Refine _LoopSubdiv_ into triangles
        ArrayList<SDFace> f = faces;
        ArrayList<SDVertex> v = vertices;
        for (int i = 0; i < nLevels; ++i) {
            // Update _f_ and _v_ for next level of subdivision
            ArrayList<SDFace> newFaces = new ArrayList<>();
            ArrayList<SDVertex> newVertices = new ArrayList<>();

            // Allocate next level of children in mesh tree
            for (SDVertex vertex : v) {
                vertex.child = new SDVertex();
                vertex.child.regular = vertex.regular;
                vertex.child.boundary = vertex.boundary;
                newVertices.add(vertex.child);
            }
            for (SDFace face : f) {
                for (int k = 0; k < 4; ++k) {
                    face.children[k] = new SDFace();
                    newFaces.add(face.children[k]);
                }
            }

            // Update vertex positions and create new edge vertices

            // Update vertex positions for even vertices
            for (SDVertex vertex : v) {
                if (!vertex.boundary) {
                    // Apply one-ring rule for even vertex
                    if (vertex.regular)
                        vertex.child.p = weightOneRing(vertex, 1.f / 16.f);
                    else
                        vertex.child.p = weightOneRing(vertex, beta(vertex.valence()));
                } else {
                    // Apply boundary rule for even vertex
                    vertex.child.p = weightBoundary(vertex, 1.f / 8.f);
                }
            }

            // Compute new odd edge vertices
            HashMap<SDEdge, SDVertex> edgeVerts = new HashMap<>();
            for (SDFace face : f) {
                for (int k = 0; k < 3; ++k) {
                    // Compute odd vertex on _k_th edge
                    SDEdge edge = new SDEdge(face.v[k], face.v[NEXT(k)]);
                    SDVertex vert = edgeVerts.get(edge);
                    if (vert != null) {
                        // Create and initialize new odd vertex
                        vert = new SDVertex();
                        newVertices.add(vert);
                        vert.regular = true;
                        vert.boundary = (face.f[k] == null);
                        vert.startFace = face.children[3];

                        // Apply edge rules to compute new vertex position
                        if (vert.boundary) {
                            vert.p = edge.v[0].p.scale(0.5f);
                            vert.p = vert.p.add(edge.v[1].p.scale(0.5f));
                        } else {
                            vert.p = edge.v[0].p.scale(3.f / 8.f);
                            vert.p = vert.p.add(edge.v[1].p.scale(3.f / 8.f));
                            vert.p = vert.p.add(face.otherVert(edge.v[0], edge.v[1]).p.scale(1.f/8.f));
                            vert.p = vert.p.add(face.f[k].otherVert(edge.v[0], edge.v[1]).p.scale(1.f/8.f));
                        }
                        edgeVerts.put(edge, vert);
                    }
                }
            }

            // Update new mesh topology

            // Update even vertex face pointers
            for (SDVertex vertex : v) {
                int vertNum = vertex.startFace.vnum(vertex);
                vertex.child.startFace = vertex.startFace.children[vertNum];
            }

            // Update face neighbor pointers
            for (SDFace face : f) {
                for (int j = 0; j < 3; ++j) {
                    // Update children _f_ pointers for siblings
                    face.children[3].f[j] = face.children[NEXT(j)];
                    face.children[j].f[NEXT(j)] = face.children[3];

                    // Update children _f_ pointers for neighbor children
                    SDFace f2 = face.f[j];
                    face.children[j].f[j] = (f2 != null) ? f2.children[f2.vnum(face.v[j])] : null;
                    f2 = face.f[PREV(j)];
                    face.children[j].f[PREV(j)] = (f2 != null) ? f2.children[f2.vnum(face.v[j])] : null;
                }
            }

            // Update face vertex pointers
            for (SDFace face : f) {
                for (int j = 0; j < 3; ++j) {
                    // Update child vertex pointer to new even vertex
                    face.children[j].v[j] = face.v[j].child;

                    // Update child vertex pointer to new odd vertex
                    SDVertex vert = edgeVerts.get(new SDEdge(face.v[j], face.v[NEXT(j)]));
                    face.children[j].v[NEXT(j)] = vert;
                    face.children[NEXT(j)].v[j] = vert;
                    face.children[3].v[j] = vert;
                }
            }

            // Prepare for next level of subdivision
            f = newFaces;
            v = newVertices;
        }

        // Push vertices to limit surface
        Point3f[] pLimit = new Point3f[v.size()];
        for (int i = 0; i < v.size(); ++i) {
            if (v.get(i).boundary)
            pLimit[i] = weightBoundary(v.get(i), 1.f / 5.f);
        else
            pLimit[i] = weightOneRing(v.get(i), loopGamma(v.get(i).valence()));
        }
        for (int i = 0; i < v.size(); ++i) v.get(i).p = pLimit[i];

        // Compute vertex tangents on limit surface
        ArrayList<Normal3f> Ns = new ArrayList<>(v.size());
        ArrayList<Point3f> pRing = new ArrayList<>(16);
        for (SDVertex vertex : v) {
            Vector3f S = new Vector3f(0, 0, 0), T = new Vector3f(0, 0, 0);
            int valence = vertex.valence();
            while (valence > pRing.size()) pRing.add(new Point3f());
            vertex.oneRing(pRing);
            if (!vertex.boundary) {
                // Compute tangents of interior face
                for (int j = 0; j < valence; ++j) {
                    S = S.add((new Vector3f(pRing.get(j))).scale((float)Math.cos(2 * Pbrt.Pi * j / valence)));
                    T = T.add((new Vector3f(pRing.get(j))).scale((float)Math.sin(2 * Pbrt.Pi * j / valence)));
                }
            } else {
                // Compute tangents of boundary face
                S = pRing.get(valence - 1).subtract(pRing.get(0));
                if (valence == 2)
                    T = (pRing.get(0)).add(pRing.get(1)).subtract(vertex.p.scale(2));
                else if (valence == 3)
                    T = pRing.get(1).subtract(vertex.p);
                else if (valence == 4)  // regular
                    T = new Vector3f(((pRing.get(0)).scale(-1)).add((pRing.get(1)).scale(2)).add((pRing.get(2)).scale(2)).add((pRing.get(3)).scale(-1)).add(vertex.p.scale(-2)));
                else {
                    float theta = Pbrt.Pi / (float)(valence - 1);
                    T = new Vector3f((pRing.get(0).add(pRing.get(valence - 1))).scale((float)Math.sin(theta)));
                    for (int k = 1; k < valence - 1; ++k) {
                        float wt = (2 * (float)Math.cos(theta) - 2) * (float)Math.sin((k)*theta);
                        T = T.add(new Vector3f((pRing.get(k)).scale(wt)));
                    }
                    T = T.negate();
                }
            }
            Ns.add(new Normal3f(Vector3f.Cross(S, T)));
        }

        // Create triangle mesh from subdivision mesh
        {
            int ntris = f.size();
            int[] vertsmesh = new int[3 * ntris];
            vpi = 0;
            int totVerts = v.size();
            HashMap<SDVertex, Integer> usedVerts = new HashMap<>();
            for (int i = 0; i < totVerts; ++i) usedVerts.put(v.get(i), i);
            for (int i = 0; i < ntris; ++i) {
                for (int j = 0; j < 3; ++j) {
                    vertsmesh[vpi] = usedVerts.get(f.get(i).v[j]);
                    vpi++;
                }
            }

            return Triangle.CreateTriangleMesh(ObjectToWorld, WorldToObject, reverseOrientation, ntris, vertsmesh,
                    totVerts, pLimit, null, Ns.toArray(new Normal3f[Ns.size()]),null, null, null);
        }
    }

    private static class SDVertex {
        // SDVertex Constructor
        SDVertex() {
            this(new Point3f(0, 0, 0));
        }
        SDVertex(Point3f p) {
            this.p = new Point3f(p);
            this.instanceId = nextInstance++;
        }

        // SDVertex Methods
        int valence() {
            SDFace f = startFace;
            if (!boundary) {
                // Compute valence of interior vertex
                int nf = 1;
                while ((f = f.nextFace(this)) != startFace) ++nf;
                return nf;
            } else {
                // Compute valence of boundary vertex
                int nf = 1;
                while ((f = f.nextFace(this)) != null) ++nf;
                f = startFace;
                while ((f = f.prevFace(this)) != null) ++nf;
                return nf + 1;
            }
        }
        void oneRing(ArrayList<Point3f> p) {

            int pi = 0;
            if (!boundary) {
                // Get one-ring vertices for interior vertex
                SDFace face = startFace;
                do {
                    p.set(pi++, face.nextVert(this).p);
                    face = face.nextFace(this);
                } while (face != startFace);
            } else {
                // Get one-ring vertices for boundary vertex
                SDFace face = startFace, f2;
                while ((f2 = face.nextFace(this)) != null) face = f2;
                p.set(pi++, face.nextVert(this).p);
                do {
                    p.set(pi++, face.prevVert(this).p);
                    face = face.prevFace(this);
                } while (face != null);
            }
        }

        public static SDVertex min(SDVertex v0, SDVertex v1) {
            return v0.instanceId < v1.instanceId ? v0 : v1;
        }
        public static SDVertex max(SDVertex v0, SDVertex v1) { return v0.instanceId > v1.instanceId ? v0 : v1; }

        Point3f p;
        SDFace startFace = null;
        SDVertex child = null;
        boolean regular = false, boundary = false;
        int instanceId = 0;

        private static int nextInstance = 0;
    }

    private static class SDFace {
        // SDFace Constructor
        SDFace() {
            for (int i = 0; i < 3; ++i) {
                v[i] = null;
                f[i] = null;
            }
            for (int i = 0; i < 4; ++i) children[i] = null;
        }

        // SDFace Methods
        int vnum(SDVertex vert) {
            for (int i = 0; i < 3; ++i)
                if (v[i] == vert) return i;
            Api.logger.fatal("Basic logic error in SDFace::vnum()");
            return -1;
        }

        SDFace nextFace(SDVertex vert) { return f[vnum(vert)]; }
        SDFace prevFace(SDVertex vert) { return f[PREV(vnum(vert))]; }
        SDVertex nextVert(SDVertex vert) { return v[NEXT(vnum(vert))]; }
        SDVertex prevVert(SDVertex vert) { return v[PREV(vnum(vert))]; }
        SDVertex otherVert(SDVertex v0, SDVertex v1) {
        for (int i = 0; i < 3; ++i)
            if (v[i] != v0 && v[i] != v1) return v[i];
            Api.logger.fatal("Basic logic error in SDVertex::otherVert()");
            return null;
        }
        SDVertex[] v = { null, null, null };
        SDFace[] f = { null, null, null };
        SDFace[] children = { null, null, null, null };
    }

    private static class SDEdge {
        // SDEdge Constructor
        SDEdge(SDVertex v0, SDVertex v1) {
            v[0] = SDVertex.min(v0, v1);
            v[1] = SDVertex.max(v0, v1);
            f[0] = f[1] = null;
            f0edgeNum = -1;
        }

        // SDEdge Comparison Function
        boolean less(SDEdge e2) {
            if (v[0].equals(e2.v[0])) return v[1].instanceId < e2.v[1].instanceId;
            return v[0].instanceId < e2.v[0].instanceId;
        }
        SDVertex[] v = { null, null };
        SDFace[] f = { null, null };
        int f0edgeNum;
    }

    private static float beta(int valence) {
        if (valence == 3)
            return 3.f / 16.f;
        else
            return 3.f / (8.f * valence);
    }

    private static float loopGamma(int valence) {
        return 1.f / (valence + 3.f / (8.f * beta(valence)));
    }

    private static Point3f weightOneRing(SDVertex vert, Float beta) {
        // Put _vert_ one-ring in _pRing_
        int valence = vert.valence();
        ArrayList<Point3f> pRing = new ArrayList<>(valence);
        vert.oneRing(pRing);
        Point3f p = vert.p.scale(1 - valence * beta);
        for (int i = 0; i < valence; ++i)
            p = p.add(pRing.get(i).scale(beta));
        return p;
    }

    private static Point3f weightBoundary(SDVertex vert, float beta) {
        // Put _vert_ one-ring in _pRing_
        int valence = vert.valence();
        ArrayList<Point3f> pRing = new ArrayList<>(valence);
        vert.oneRing(pRing);
        Point3f p =  vert.p.scale(1 - 2 * beta);
        p = p.add(pRing.get(0).scale(beta));
        p = p.add(pRing.get(valence - 1).scale(beta));
        return p;
    }

    private static int NEXT(int i) {
        return (((i) + 1) % 3);
    }
    private static int PREV(int i) {
        return (((i) + 2) % 3);
    }

}