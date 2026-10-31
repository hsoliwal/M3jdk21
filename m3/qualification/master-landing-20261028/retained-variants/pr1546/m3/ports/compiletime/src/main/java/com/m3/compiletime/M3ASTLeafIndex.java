/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.compiletime;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Detached, leaf-first AST evidence index. L0=file facts/atoms, L1=file summary,
 * L2=package, L3=module, L4=project, L5=repository. Each parent is admitted
 * strictly after its children. Nodes are rows in primitive lanes, not objects.
 *
 * <p>This is an evidence/selection projection, not a Java parser, mutable AST,
 * semantic-equivalence proof, or replacement for the existing MIndexAST pool.
 * It never retains caller arrays; physical atom positions survive filtering.
 */
public final class M3ASTLeafIndex {
    public static final int FACTS = 0, FILE = 1, PACKAGE = 2;
    public static final int MODULE = 3, PROJECT = 4, REPOSITORY = 5;
    public static final long LIVE = 1L, DIRTY = 1L << 1, PINNED = 1L << 2;
    public static final long MATERIALIZED = 1L << 3, TOMBSTONE = 1L << 4;
    private static final int LEVELS = 6;

    /** Caller-facing file boundary; atom IDs belong to the supplied AST owner. */
    public static final class FileFacts {
        private final String project, module, packageName, path, sourceSha256;
        private final int[] atomIds;
        private final long[] stateWords;

        public FileFacts(String project, String module, String packageName,
                String path, String sourceSha256, int[] atomIds, long[] stateWords) {
            this.project = label(project, "project");
            this.module = label(module, "module");
            this.packageName = label(packageName, "package");
            this.path = normalizedPath(path);
            this.sourceSha256 = Objects.requireNonNull(sourceSha256, "sourceSha256");
            if (!sourceSha256.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("source SHA-256 must be lower-case hex");
            }
            this.atomIds = Objects.requireNonNull(atomIds, "atomIds").clone();
            this.stateWords = Objects.requireNonNull(stateWords, "stateWords").clone();
            if (this.atomIds.length != this.stateWords.length) {
                throw new IllegalArgumentException("atom and state lanes must align");
            }
            for (int atom : this.atomIds) {
                if (atom < 0) throw new IllegalArgumentException("negative AST atom handle");
            }
        }

        public String path() { return path; }
        public int[] atomIds() { return atomIds.clone(); }
        public long[] stateWords() { return stateWords.clone(); }
    }

    private final byte[] level;
    private final String[] scope, digest;
    private final int[] parent, childStart, childCount, atomStart, atomCount, fileCount;
    private final long[] stateOr, stateAnd;
    private final int[] edges, atomIds;
    private final long[] atomState;
    private final int[] levelStart, levelCount;

    private M3ASTLeafIndex(Builder b, int[] ids, long[] states) {
        int n = b.size;
        level = Arrays.copyOf(b.level, n);
        scope = Arrays.copyOf(b.scope, n);
        digest = Arrays.copyOf(b.digest, n);
        parent = Arrays.copyOf(b.parent, n);
        childStart = Arrays.copyOf(b.childStart, n);
        childCount = Arrays.copyOf(b.childCount, n);
        atomStart = Arrays.copyOf(b.atomStart, n);
        atomCount = Arrays.copyOf(b.atomCount, n);
        fileCount = Arrays.copyOf(b.fileCount, n);
        stateOr = Arrays.copyOf(b.stateOr, n);
        stateAnd = Arrays.copyOf(b.stateAnd, n);
        edges = Arrays.copyOf(b.edges, b.edgeSize);
        atomIds = ids;
        atomState = states;
        levelStart = b.levelStart.clone();
        levelCount = b.levelCount.clone();
    }

    /** Stable under all input-file permutations; detects duplicate source owners. */
    public static M3ASTLeafIndex build(String repository, FileFacts... inputs) {
        String root = label(repository, "repository");
        FileFacts[] sorted = Objects.requireNonNull(inputs, "inputs").clone();
        for (FileFacts fact : sorted) Objects.requireNonNull(fact, "file fact");
        Arrays.sort(sorted, Comparator.comparing((FileFacts f) -> f.project)
                .thenComparing(f -> f.module).thenComparing(f -> f.packageName)
                .thenComparing(f -> f.path));
        TreeSet<String> seen = new TreeSet<>();
        int atoms = 0;
        for (FileFacts f : sorted) {
            // Relative paths repeat legitimately across modules and projects. Source
            // ownership is the framed (project, module, path) tuple; package is
            // metadata within that source and cannot disambiguate a duplicate.
            String ownerKey = frame(f.project) + frame(f.module) + frame(f.path);
            if (!seen.add(ownerKey)) {
                throw new IllegalArgumentException(
                        "duplicate source in project/module: " + f.path);
            }
            atoms = Math.addExact(atoms, f.atomIds.length);
        }
        int[] ids = new int[atoms];
        long[] states = new long[atoms];
        Builder b = new Builder();
        int[] previous = new int[sorted.length];
        int offset = 0;
        for (int i = 0; i < sorted.length; i++) {
            FileFacts f = sorted[i];
            System.arraycopy(f.atomIds, 0, ids, offset, f.atomIds.length);
            System.arraycopy(f.stateWords, 0, states, offset, f.stateWords.length);
            previous[i] = b.addLeaf(f, offset, ids, states);
            offset += f.atomIds.length;
        }
        b.closeLevel(FACTS);
        for (int i = 0; i < previous.length; i++) {
            previous[i] = b.addParent(FILE, sorted[i].path, previous, i, i + 1);
        }
        b.closeLevel(FILE);
        FileFacts[] representatives = sorted;
        for (int l = PACKAGE; l <= PROJECT; l++) {
            int[] next = new int[previous.length];
            FileFacts[] reps = new FileFacts[previous.length];
            int written = 0;
            for (int first = 0; first < previous.length; ) {
                String group = groupKey(l, representatives[first]);
                int last = first + 1;
                while (last < previous.length
                        && group.equals(groupKey(l, representatives[last]))) last++;
                next[written] = b.addParent(l, group, previous, first, last);
                reps[written++] = representatives[first];
                first = last;
            }
            previous = Arrays.copyOf(next, written);
            representatives = Arrays.copyOf(reps, written);
            b.closeLevel(l);
        }
        b.addParent(REPOSITORY, root, previous, 0, previous.length);
        b.closeLevel(REPOSITORY);
        return new M3ASTLeafIndex(b, ids, states);
    }

    public int rowCount() { return level.length; }
    public int levelCount(int l) { return levelCount[checkedLevel(l)]; }
    public int rowAt(int l, int ordinal) {
        return levelStart[checkedLevel(l)] + Objects.checkIndex(ordinal, levelCount[l]);
    }
    public int repositoryRow() { return rowAt(REPOSITORY, 0); }
    public int level(int row) { return level[checkRow(row)]; }
    public String scope(int row) { return scope[checkRow(row)]; }
    public String sha256(int row) { return digest[checkRow(row)]; }
    public int parent(int row) { return parent[checkRow(row)]; }
    public int childCount(int row) { return childCount[checkRow(row)]; }
    public int childAt(int row, int ordinal) {
        int checked = checkRow(row);
        return edges[childStart[checked] + Objects.checkIndex(ordinal, childCount[checked])];
    }
    public int atomCount(int row) { return atomCount[checkRow(row)]; }
    public int fileCount(int row) { return fileCount[checkRow(row)]; }
    public long stateUnion(int row) { return stateOr[checkRow(row)]; }
    public long stateIntersection(int row) { return stateAnd[checkRow(row)]; }
    public int physicalOffset(int row, int ordinal) {
        int checked = checkRow(row);
        return atomStart[checked] + Objects.checkIndex(ordinal, atomCount[checked]);
    }
    public int atomIdAtPhysical(int physical) { return atomIds[Objects.checkIndex(physical, atomIds.length)]; }
    public long stateAtPhysical(int physical) { return atomState[Objects.checkIndex(physical, atomState.length)]; }

    /** Select immutable physical offsets in encounter order, never moving AST payloads. */
    public int[] selectOffsets(int row, long required, long forbidden) {
        int checked = checkRow(row);
        if ((required & forbidden) != 0) throw new IllegalArgumentException("conflicting state masks");
        int start = atomStart[checked], end = start + atomCount[checked];
        int matches = 0;
        for (int i = start; i < end; i++) {
            if (matches(atomState[i], required, forbidden)) matches++;
        }
        int[] result = new int[matches];
        int write = 0;
        for (int i = start; i < end; i++) {
            if (matches(atomState[i], required, forbidden)) result[write++] = i;
        }
        return result;
    }

    /**
     * Resolve existing L4/L3/L2 scope rows. No parse, secondary index or symbol
     * resolution occurs: the row is owned by this immutable L0-L5 projection.
     * -1 means that the source scope has no admitted file facts.
     */
    public int projectRow(String project) {
        return findChild(repositoryRow(), PROJECT, frame(label(project, "project")));
    }

    public int moduleRow(String project, String module) {
        String p = label(project, "project");
        String m = label(module, "module");
        return findChild(projectRow(p), MODULE, frame(p) + frame(m));
    }

    public int packageRow(String project, String module, String packageName) {
        String p = label(project, "project");
        String m = label(module, "module");
        String name = label(packageName, "package");
        return findChild(moduleRow(p, m), PACKAGE,
                frame(p) + frame(m) + frame(name));
    }

    /**
     * The same relative file name is valid in different modules/projects.
     * The package is metadata, not another source-owner discriminator.
     */
    public int fileRow(String project, String module, String path) {
        int moduleRow = moduleRow(project, module);
        String name = normalizedPath(path);
        if (moduleRow < 0) return -1;
        for (int p = 0; p < childCount[moduleRow]; p++) {
            int packageRow = edges[childStart[moduleRow] + p];
            for (int f = 0; f < childCount[packageRow]; f++) {
                int candidate = edges[childStart[packageRow] + f];
                if (scope[candidate].equals(name)) return candidate;
            }
        }
        return -1;
    }

    private int findChild(int owner, int wantedLevel, String exactKey) {
        if (owner < 0) return -1;
        for (int i = 0; i < childCount[owner]; i++) {
            int child = edges[childStart[owner] + i];
            if (level[child] == wantedLevel && scope[child].equals(exactKey)) {
                return child;
            }
        }
        return -1;
    }

    /** The original source/file owner receives its existing file and facts rows. */
    @FunctionalInterface
    public interface FileOwner {
        void accept(int fileRow, int factsRow);
    }

    /** The original AST owner receives an occurrence, not a copied AST node. */
    @FunctionalInterface
    public interface AtomOwner {
        void accept(int atomId, int physicalOffset, long stateWord);
    }

    /**
     * Dispatch a FILE/PACKAGE/MODULE/PROJECT/REPOSITORY scope to the caller's
     * actual parser/compiler owner. Files without atoms are still visited.
     * No file is opened, parsed, copied or retained by this operation.
     */
    public int dispatchFiles(int scopeRow, FileOwner owner) {
        int selected = checkRow(scopeRow);
        Objects.requireNonNull(owner, "original file owner");
        if (level[selected] < FILE) {
            throw new IllegalArgumentException("file dispatch requires an L1-L5 scope");
        }
        int count = 0;
        for (int ordinal = 0; ordinal < levelCount[FILE]; ordinal++) {
            int file = levelStart[FILE] + ordinal;
            int ancestor = file;
            while (ancestor >= 0 && ancestor != selected) ancestor = parent[ancestor];
            if (ancestor != selected) continue;
            owner.accept(file, edges[childStart[file]]);
            count++;
        }
        return count;
    }

    /**
     * Dispatch source-owner atom handles in their original physical order.
     * Duplicate atom IDs are distinct physical occurrences. Both state masks
     * are full-width long values; no temporary selection int[] is allocated.
     */
    public int dispatchAtoms(int scopeRow, long required, long forbidden, AtomOwner owner) {
        int selected = checkRow(scopeRow);
        Objects.requireNonNull(owner, "original AST owner");
        if ((required & forbidden) != 0) {
            throw new IllegalArgumentException("conflicting state masks");
        }
        int from = atomStart[selected];
        int end = from + atomCount[selected];
        int count = 0;
        for (int physical = from; physical < end; physical++) {
            long state = atomState[physical];
            if (!matches(state, required, forbidden)) continue;
            owner.accept(atomIds[physical], physical, state);
            count++;
        }
        return count;
    }

    private static boolean matches(long word, long required, long forbidden) {
        return (word & required) == required && (word & forbidden) == 0;
    }
    private int checkRow(int row) { return Objects.checkIndex(row, level.length); }
    private static int checkedLevel(int l) { return Objects.checkIndex(l, LEVELS); }
    private static String label(String value, String role) {
        Objects.requireNonNull(value, role);
        if (value.isBlank() || value.indexOf('\0') >= 0 || value.indexOf('\r') >= 0
                || value.indexOf('\n') >= 0) throw new IllegalArgumentException("invalid " + role);
        return value;
    }
    private static String normalizedPath(String value) {
        Objects.requireNonNull(value, "path");
        String p = value.replace('\\', '/');
        if (p.isBlank() || p.startsWith("/") || p.endsWith("/") || p.contains("//")
                || p.equals(".") || p.equals("..") || p.startsWith("../")
                || p.contains("/../") || p.contains("/./") || p.endsWith("/.")
                || p.indexOf(':') >= 0 || p.indexOf('\0') >= 0 || p.indexOf('\n') >= 0
                || p.indexOf('\r') >= 0) throw new IllegalArgumentException("invalid source path");
        return p;
    }
    private static String frame(String s) { return s.length() + ":" + s; }
    private static String groupKey(int level, FileFacts f) {
        String p = frame(f.project);
        if (level == PROJECT) return p;
        String m = p + frame(f.module);
        if (level == MODULE) return m;
        return m + frame(f.packageName);
    }
    private static MessageDigest sha() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException fail) { throw new IllegalStateException(fail); }
    }
    private static void write(MessageDigest md, int value) {
        md.update((byte) (value >>> 24)); md.update((byte) (value >>> 16));
        md.update((byte) (value >>> 8)); md.update((byte) value);
    }
    private static void write(MessageDigest md, long value) {
        write(md, (int) (value >>> 32)); write(md, (int) value);
    }
    private static void write(MessageDigest md, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        write(md, bytes.length); md.update(bytes);
    }
    private static String finish(MessageDigest md) { return HexFormat.of().formatHex(md.digest()); }

    private static final class Builder {
        private byte[] level = new byte[16];
        private String[] scope = new String[16], digest = new String[16];
        private int[] parent = new int[16], childStart = new int[16], childCount = new int[16];
        private int[] atomStart = new int[16], atomCount = new int[16], fileCount = new int[16];
        private long[] stateOr = new long[16], stateAnd = new long[16];
        private int[] edges = new int[16];
        private final int[] levelStart = new int[LEVELS], levelCount = new int[LEVELS];
        private int size, edgeSize;

        private void ensure(int children) {
            if (size == level.length) {
                int grown = Math.multiplyExact(level.length, 2);
                level = Arrays.copyOf(level, grown); scope = Arrays.copyOf(scope, grown);
                digest = Arrays.copyOf(digest, grown); parent = Arrays.copyOf(parent, grown);
                childStart = Arrays.copyOf(childStart, grown); childCount = Arrays.copyOf(childCount, grown);
                atomStart = Arrays.copyOf(atomStart, grown); atomCount = Arrays.copyOf(atomCount, grown);
                fileCount = Arrays.copyOf(fileCount, grown); stateOr = Arrays.copyOf(stateOr, grown);
                stateAnd = Arrays.copyOf(stateAnd, grown);
            }
            int need = Math.addExact(edgeSize, children);
            if (need > edges.length) edges = Arrays.copyOf(edges,
                    Math.max(need, Math.multiplyExact(edges.length, 2)));
        }
        private int addLeaf(FileFacts f, int from, int[] ids, long[] states) {
            ensure(0);
            int row = size++;
            level[row] = FACTS; scope[row] = f.path; parent[row] = -1;
            childStart[row] = edgeSize; atomStart[row] = from;
            atomCount[row] = f.atomIds.length; fileCount[row] = 1;
            long union = 0, common = 0;
            MessageDigest md = sha(); write(md, "M3AST-L0-v1");
            write(md, f.project); write(md, f.module); write(md, f.packageName);
            write(md, f.path); write(md, f.sourceSha256); write(md, f.atomIds.length);
            for (int i = from; i < from + f.atomIds.length; i++) {
                write(md, ids[i]); write(md, states[i]);
                union |= states[i]; common = i == from ? states[i] : common & states[i];
            }
            stateOr[row] = union; stateAnd[row] = common; digest[row] = finish(md);
            return row;
        }
        private int addParent(int l, String key, int[] children, int start, int end) {
            ensure(end - start);
            int row = size++;
            level[row] = (byte) l; scope[row] = key; parent[row] = -1;
            childStart[row] = edgeSize; childCount[row] = end - start;
            int from = start == end ? 0 : atomStart[children[start]];
            int atomTotal = 0, files = 0;
            long union = 0, common = 0;
            boolean any = false;
            MessageDigest md = sha(); write(md, "M3AST-ROLLUP-v1");
            write(md, l); write(md, key); write(md, end - start);
            for (int i = start; i < end; i++) {
                int child = children[i];
                if (child < 0 || child >= row || level[child] != l - 1 || parent[child] != -1) {
                    throw new IllegalStateException("invalid or multiply owned leaf-first child");
                }
                if (atomCount[child] != 0 && from + atomTotal != atomStart[child]) {
                    throw new IllegalStateException("noncontiguous physical atom window");
                }
                edges[edgeSize++] = child;
                parent[child] = row;
                atomTotal = Math.addExact(atomTotal, atomCount[child]);
                files = Math.addExact(files, fileCount[child]);
                union |= stateOr[child];
                if (atomCount[child] != 0) {
                    common = any ? common & stateAnd[child] : stateAnd[child];
                    any = true;
                }
                write(md, digest[child]);
            }
            atomStart[row] = from; atomCount[row] = atomTotal; fileCount[row] = files;
            stateOr[row] = union; stateAnd[row] = any ? common : 0;
            digest[row] = finish(md);
            return row;
        }
        private void closeLevel(int l) {
            int first = l == 0 ? 0 : levelStart[l - 1] + levelCount[l - 1];
            levelStart[l] = first;
            levelCount[l] = size - first;
        }
    }
}
