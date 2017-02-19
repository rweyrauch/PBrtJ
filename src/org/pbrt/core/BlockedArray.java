/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class BlockedArray<T> {

    public BlockedArray(int uRes, int vRes, int logBlockSize, T[] d, Class<T> cls) throws IllegalAccessException, InstantiationException {
        this.uRes = uRes;
        this.vRes = vRes;
        this.logBlockSize = logBlockSize;
        this.uBlocks = RoundUp(uRes) >> logBlockSize;

        int nAlloc = RoundUp(uRes) * RoundUp(vRes);
        this.data = new Object[nAlloc];
        for (int i = 0; i < nAlloc; i++) this.data[i] = cls.newInstance();
        if (d != null) {
            for (int v = 0; v < vRes; ++v) {
                for (int u = 0; u < uRes; ++u) {
                    set(u, v, d[v * uRes + u]);
                }
            }
        }
    }

    public int BlockSize() { return 1 << logBlockSize; }
    public int RoundUp(int x) {
        return (x + BlockSize() - 1) & ~(BlockSize() - 1);
    }

    public int uSize() { return uRes; }
    public int vSize() { return vRes; }

    public int Block(int a) { return a >> logBlockSize; }
    public int Offset(int a) { return (a & (BlockSize() - 1)); }

    public T at(int u, int v) {
        int bu = Block(u), bv = Block(v);
        int ou = Offset(u), ov = Offset(v);
        int offset = BlockSize() * BlockSize() * (uBlocks * bv + bu);
        offset += BlockSize() * ov + ou;
        return (T)data[offset];
    }

    public void set(int u, int v, T value) {
        int bu = Block(u), bv = Block(v);
        int ou = Offset(u), ov = Offset(v);
        int offset = BlockSize() * BlockSize() * (uBlocks * bv + bu);
        offset += BlockSize() * ov + ou;
        data[offset] = value;
    }

    private Object[] data;
    private final int uRes, vRes, uBlocks;
    private final int logBlockSize;
}