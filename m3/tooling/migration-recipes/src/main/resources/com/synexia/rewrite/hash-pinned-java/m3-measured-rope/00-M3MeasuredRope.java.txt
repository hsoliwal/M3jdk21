/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.precompute;

import java.util.Objects;

/**
 * Immutable UTF-16 text as a balanced persistent rope whose nodes carry pluggable monoid measures.
 *
 * <p>This is the measured-node contract of the M3 String composition (REGEX_PRECOMPUTE_DESIGN 2b/2d):
 * every node of the tree holds one summary per {@link M3RopeMeasure} registered in its
 * {@link M3RopeArena}, computed once when the node is created. A query over the whole text reads the root
 * summary; a range query composes at most two partial leaves with complete subtree summaries; a metric
 * query ({@link #seek}) descends one path. Concatenation, slicing, insertion, removal and replacement are
 * persistent: the receiver is never changed and shares all untouched nodes with the result.</p>
 *
 * <p>The value is a thin facade (arena plus root id); the tree lives in the arena's primitive lanes and
 * leaves are coordinate slices of immutable Strings, so no node, entry or character object is stored.
 * Coordinates are UTF-16 code units as in {@link String}. Models {@code java.lang.M3StringTuple} with a
 * generic per-node measure slot; it does not replace or modify any {@code java.lang} type.</p>
 */
public final class M3MeasuredRope implements CharSequence {
    /** Receives leaf slices {@code text[from, to)} in rope order (or reverse order). */
    @FunctionalInterface
    public interface LeafVisitor {
        void leaf(String text, int from, int to);
    }

    private final M3RopeArena arena;
    private final int root;

    M3MeasuredRope(M3RopeArena arena, int root) {
        this.arena = arena;
        this.root = root;
    }

    /** The arena holding this rope's nodes. */
    public M3RopeArena arena() { return arena; }

    @Override
    public int length() { return arena.units(root); }

    @Override
    public char charAt(int index) {
        Objects.checkIndex(index, length());
        return arena.charAt(root, index);
    }

    @Override
    public M3MeasuredRope subSequence(int start, int end) { return slice(start, end); }

    /** The text {@code [from, to)} sharing every complete subtree; at most two leaves are re-sliced. */
    public M3MeasuredRope slice(int from, int to) {
        Objects.checkFromToIndex(from, to, length());
        if (from == 0 && to == length()) return this;
        return arena.wrap(arena.slice(root, from, to));
    }

    /** This text followed by {@code other} (AVL join, {@code O(log n)} new nodes). */
    public M3MeasuredRope concat(M3MeasuredRope other) {
        return arena.wrap(arena.join(root, own(other).root));
    }

    public M3MeasuredRope concat(CharSequence other) { return concat(arena.of(other)); }

    /** This text with {@code piece} inserted at UTF-16 offset {@code at}. */
    public M3MeasuredRope insert(int at, M3MeasuredRope piece) { return replace(at, at, piece); }

    public M3MeasuredRope insert(int at, CharSequence piece) { return replace(at, at, arena.of(piece)); }

    /** This text without {@code [from, to)} (Swift {@code removeSubrange}). */
    public M3MeasuredRope remove(int from, int to) { return replace(from, to, arena.empty()); }

    /** This text with {@code [from, to)} replaced by {@code piece} (Swift {@code replaceSubrange}). */
    public M3MeasuredRope replace(int from, int to, M3MeasuredRope piece) {
        Objects.checkFromToIndex(from, to, length());
        own(piece);
        long head = arena.split(root, from);
        int tail = M3RopeArena.second(arena.split(M3RopeArena.second(head), to - from));
        return arena.wrap(arena.join(arena.join(M3RopeArena.first(head), piece.root), tail));
    }

    public M3MeasuredRope replace(int from, int to, CharSequence piece) {
        return replace(from, to, arena.of(piece));
    }

    /** The root summary of {@code measure} (a new array of {@code measure.width()} slots). */
    public long[] summary(M3RopeMeasure measure) {
        long[] out = new long[measure.width()];
        summary(measure, out, 0);
        return out;
    }

    /** Copies the root summary of {@code measure} into {@code out[at ..]}; no allocation. */
    public void summary(M3RopeMeasure measure, long[] out, int at) {
        arena.copySummary(root, arena.measureIndex(measure), out, at);
    }

    /** The summary of {@code [from, to)} composed from subtree summaries and at most two partial leaves. */
    public long[] rangeSummary(M3RopeMeasure measure, int from, int to) {
        long[] out = new long[measure.width()];
        rangeSummary(measure, from, to, out, 0);
        return out;
    }

    public void rangeSummary(M3RopeMeasure measure, int from, int to, long[] out, int at) {
        Objects.checkFromToIndex(from, to, length());
        arena.rangeSummary(root, arena.measureIndex(measure), from, to, out, at);
    }

    /**
     * The smallest UTF-16 offset {@code p} with {@code metric.size(prefix(p)) > target}, or {@link #length()}
     * when no prefix exceeds it. For {@link M3RopeMeasures#CODE_POINTS}, {@code seek(k) - 1} is the offset of
     * code point {@code k}.
     */
    public int seek(M3RopeMetric metric, long target) {
        if (target < 0) throw new IllegalArgumentException("negative metric target: " + target);
        return arena.seek(root, arena.measureIndex(metric), target);
    }

    public void forEachLeaf(LeafVisitor visitor) {
        arena.forEachLeaf(root, false, Objects.requireNonNull(visitor, "visitor"));
    }

    public void forEachLeafReverse(LeafVisitor visitor) {
        arena.forEachLeaf(root, true, Objects.requireNonNull(visitor, "visitor"));
    }

    public int leafCount() { return arena.leafCount(root); }

    /** AVL height: 0 for the empty rope, 1 for a single leaf. */
    public int height() { return arena.height(root); }

    /** Copies {@code [from, to)} into {@code destination[at ..]} leaf by leaf, like {@link String#getChars}. */
    public void getChars(int from, int to, char[] destination, int at) {
        Objects.checkFromToIndex(from, to, length());
        Objects.checkFromIndexSize(at, to - from, destination.length);
        if (root != M3RopeArena.EMPTY) arena.getChars(root, from, to, destination, at);
    }

    /** Flattens the text; the only operation that builds a contiguous copy. */
    @Override
    public String toString() {
        char[] out = new char[length()];
        getChars(0, out.length, out, 0);
        return new String(out);
    }

    /** True when {@code other} has the same UTF-16 code units (the {@link String#contentEquals} contract). */
    public boolean contentEquals(CharSequence other) {
        int n = length();
        if (other.length() != n) return false;
        for (int i = 0; i < n; i++) if (arena.charAt(root, i) != other.charAt(i)) return false;
        return true;
    }

    /**
     * Checks AVL shape, unit counts, the fold-vs-combine law for every node and measure, and that each root
     * summary equals the measure of the flattened text as one leaf.
     */
    public void verifyInvariants() {
        arena.verify(root);
        String flat = toString();
        for (int m = 0; m < arena.measureCount(); m++) arena.verifyRootFold(root, m, flat);
    }

    private M3MeasuredRope own(M3MeasuredRope other) {
        if (Objects.requireNonNull(other, "rope").arena != arena) {
            throw new IllegalArgumentException("rope belongs to another arena");
        }
        return other;
    }
}
