// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Owner of one rooted directory tree held in structure-of-arrays lanes ({@code m3/file/DESIGN.md}).
 *
 * <p>Row ids are dense and allocated in pre-order; {@code 0} is the root. Lanes: {@code parent},
 * {@code depth}, a {@code char[]} name pool with per-row offsets (pattern of
 * {@code com.m3.fs.M3DirectoryTable}), CSR child slices, {@code size}, {@code mtime}, and one
 * {@link BitSet} per {@link M3FileFlag}. {@link M3File} facades are thin {@code (tree, id)} handles
 * produced by {@link #file(int)} and kept in a bounded {@link M3FileResidency}; a {@code String} or
 * {@code Path} is only built when a caller asks for one.</p>
 *
 * <p>Invariants: {@code parent[id] < id}; {@code depth[id] == depth[parent[id]] + 1}; every child
 * slice is sorted by {@link #compareNames}; {@code childIds} holds every non-root id exactly once;
 * {@link M3FileFlag#DELETED} rows keep their lanes and are skipped by {@link #ids()}.</p>
 */
public final class M3FileTree {
    static final int DEFAULT_RESIDENCY = 4096;
    private static final int INITIAL_ROWS = 64;
    private static final M3FileTree DETACHED = new M3FileTree(Path.of(""), 1);

    private final Path root;
    private final BitSet[] flags = new BitSet[M3FileFlag.VALUES.length];
    private final M3FileResidency residency;
    private int[] parent = new int[INITIAL_ROWS];
    private short[] depth = new short[INITIAL_ROWS];
    private int[] nameOff = new int[INITIAL_ROWS + 1];
    private char[] namePool = new char[INITIAL_ROWS * 16];
    private int[] childStart = new int[INITIAL_ROWS];
    private int[] childCount = new int[INITIAL_ROWS];
    private int[] childIds = new int[INITIAL_ROWS];
    private long[] size = new long[INITIAL_ROWS];
    private long[] mtime = new long[INITIAL_ROWS];
    private int rows;
    private int childTop;
    private int nameTop;

    private M3FileTree(Path root, int residencyCapacity) {
        this.root = Objects.requireNonNull(root, "root");
        this.residency = new M3FileResidency(residencyCapacity);
        for (int i = 0; i < flags.length; i++) flags[i] = new BitSet();
    }

    /** Eager scan of {@code root} with {@code NOFOLLOW_LINKS} and unbounded depth. */
    public static M3FileTree open(Path root) throws IOException {
        return open(root, DEFAULT_RESIDENCY);
    }

    /** As {@link #open(Path)} with an explicit facade residency capacity (power of two). */
    public static M3FileTree open(Path root, int residencyCapacity) throws IOException {
        return M3FileTreeScanner.scan(new M3FileTree(root, residencyCapacity));
    }

    /** Shared empty tree behind {@link M3File#of(Path)}; it has no rows. */
    public static M3FileTree detached() {
        return DETACHED;
    }

    public Path root() {
        return root;
    }

    public M3File rootFile() {
        return file(0);
    }

    /** Facade for {@code id}; identical instance while resident, equal instance after eviction. */
    public M3File file(int id) {
        checkId(id);
        return residency.file(this, id);
    }

    /** Rows allocated, tombstones included. */
    public int rows() {
        return rows;
    }

    public int liveCount() {
        return rows - flags[M3FileFlag.DELETED.ordinal()].cardinality();
    }

    /** Live ids in pre-order. */
    public IntStream ids() {
        BitSet deleted = flags[M3FileFlag.DELETED.ordinal()];
        return IntStream.range(0, rows).filter(id -> !deleted.get(id));
    }

    public int residentCount() {
        return residency.residentCount();
    }

    public int parent(int id) {
        checkId(id);
        return parent[id];
    }

    public int depth(int id) {
        checkId(id);
        return depth[id];
    }

    public int childCount(int id) {
        checkId(id);
        return childCount[id];
    }

    /** Allocation-free child access: {@code index} in {@code [0, childCount(id))}. */
    public int childId(int id, int index) {
        checkId(id);
        Objects.checkIndex(index, childCount[id]);
        return childIds[childStart[id] + index];
    }

    public IntStream childIds(int id) {
        checkId(id);
        int start = childStart[id];
        return IntStream.range(start, start + childCount[id]).map(i -> childIds[i]);
    }

    public long size(int id) {
        checkId(id);
        return size[id];
    }

    public long mtime(int id) {
        checkId(id);
        return mtime[id];
    }

    public boolean flag(M3FileFlag flag, int id) {
        checkId(id);
        return flags[flag.ordinal()].get(id);
    }

    public int nameLength(int id) {
        checkId(id);
        return nameOff[id + 1] - nameOff[id];
    }

    public char nameCharAt(int id, int index) {
        Objects.checkIndex(index, nameLength(id));
        return namePool[nameOff[id] + index];
    }

    /** Cold: materialises the name of {@code id}. */
    public String name(int id) {
        checkId(id);
        return new String(namePool, nameOff[id], nameOff[id + 1] - nameOff[id]);
    }

    /** UTF-16 code unit order, then length; locale-free and allocation-free. */
    public int compareNames(int a, int b) {
        int lengthA = nameLength(a);
        int lengthB = nameLength(b);
        int offA = nameOff[a];
        int offB = nameOff[b];
        int common = Math.min(lengthA, lengthB);
        for (int i = 0; i < common; i++) {
            int difference = namePool[offA + i] - namePool[offB + i];
            if (difference != 0) return difference;
        }
        return lengthA - lengthB;
    }

    /** Cold: {@code root} for id {@code 0}, otherwise {@code root.resolve(names joined by the separator)}. */
    public Path path(int id) {
        checkId(id);
        if (id == 0) return root;
        StringBuilder relative = new StringBuilder();
        appendRelative(relative, id);
        return root.resolve(relative.toString());
    }

    private void appendRelative(StringBuilder out, int id) {
        if (parent[id] > 0) {
            appendRelative(out, parent[id]);
            out.append(root.getFileSystem().getSeparator());
        }
        out.append(namePool, nameOff[id], nameOff[id + 1] - nameOff[id]);
    }

    void setFlag(M3FileFlag flag, int id, boolean value) {
        flags[flag.ordinal()].set(id, value);
    }

    /** Scanner entry: appends one row in pre-order and returns its id. */
    int append(int parentId, String name, boolean dir, boolean link, boolean hidden, long bytes, long modified) {
        ensureRows(rows + 1);
        int id = rows++;
        parent[id] = parentId;
        depth[id] = (short) (parentId < 0 ? 0 : depth[parentId] + 1);
        size[id] = bytes;
        mtime[id] = modified;
        appendName(name);
        nameOff[id + 1] = nameTop;
        setFlag(M3FileFlag.DIR, id, dir);
        setFlag(M3FileFlag.LINK, id, link);
        setFlag(M3FileFlag.HIDDEN, id, hidden);
        return id;
    }

    /** Scanner entry: writes the sorted CSR slice {@code ids[from, to)} for directory {@code dirId}. */
    void closeDirectory(int dirId, int[] ids, int from, int to) {
        int count = to - from;
        if (childTop + count > childIds.length) {
            childIds = Arrays.copyOf(childIds, Math.max(childIds.length * 2, childTop + count));
        }
        childStart[dirId] = childTop;
        childCount[dirId] = count;
        System.arraycopy(ids, from, childIds, childTop, count);
        childTop += count;
        setFlag(M3FileFlag.SCANNED, dirId, true);
    }

    private void appendName(String name) {
        int length = name.length();
        if (nameTop + length > namePool.length) {
            namePool = Arrays.copyOf(namePool, Math.max(namePool.length * 2, nameTop + length));
        }
        name.getChars(0, length, namePool, nameTop);
        nameTop += length;
    }

    private void ensureRows(int needed) {
        if (needed <= parent.length) return;
        int capacity = Math.max(parent.length * 2, needed);
        parent = Arrays.copyOf(parent, capacity);
        depth = Arrays.copyOf(depth, capacity);
        nameOff = Arrays.copyOf(nameOff, capacity + 1);
        childStart = Arrays.copyOf(childStart, capacity);
        childCount = Arrays.copyOf(childCount, capacity);
        size = Arrays.copyOf(size, capacity);
        mtime = Arrays.copyOf(mtime, capacity);
    }

    private void checkId(int id) {
        Objects.checkIndex(id, rows);
    }
}
