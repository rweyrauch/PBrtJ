/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class BlockedArray {

    public BlockedArray(int uRes, int vRes, int logBlockSize, Object[] d) {
        this.uRes = uRes;
        this.vRes = vRes;
        this.logBlockSize = logBlockSize;
        this.blockSize = 1 << logBlockSize;
        this.uBlocks = RoundUp(uRes) >> logBlockSize;

        int nAlloc = RoundUp(uRes) * RoundUp(vRes);
        this.data = new Object[nAlloc];
        for (int i = 0; i < nAlloc; i++) {
            this.data[i] = new Object();
        }
        if (d != null) {
            for (int v = 0; v < vRes; ++v) {
                for (int u = 0; u < uRes; ++u) {
                    set(u, v, d[v * uRes + u]);
                }
            }
        }
    }

    public int RoundUp(int x) {
        return (x + blockSize - 1) & ~(blockSize - 1);
    }

    public int uSize() { return uRes; }
    public int vSize() { return vRes; }

    public int Block(int a) { return a >> logBlockSize; }
    public int Offset(int a) { return (a & (blockSize - 1)); }

    public Object at(int u, int v) {
        final int bu = Block(u), bv = Block(v);
        final int ou = Offset(u), ov = Offset(v);
        int offset = blockSize * blockSize * (uBlocks * bv + bu);
        offset += blockSize * ov + ou;
        return data[offset];
    }

    public void set(int u, int v, Object value) {
        final int bu = Block(u), bv = Block(v);
        final int ou = Offset(u), ov = Offset(v);
        int offset = blockSize * blockSize * (uBlocks * bv + bu);
        offset += blockSize * ov + ou;
        data[offset] = value;
    }

    private Object[] data;
    private final int uRes, vRes, uBlocks;
    private final int logBlockSize;
    private final int blockSize;
}