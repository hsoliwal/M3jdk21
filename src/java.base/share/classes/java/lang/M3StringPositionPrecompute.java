/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package java.lang;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Separately bounded position precompute for BMP code-unit search.
 *
 * <p>Each 64-code-unit source block stores the same conservative two-bit code-unit signal used by
 * {@link M3StringFacts}. Negative block tests may skip exact work; positive blocks always perform
 * exact UTF-16 comparison. Entries weakly key canonical owner+coordinate and retain only primitive
 * block masks.</p>
 */
final class M3StringPositionPrecompute {
    private static final int BLOCK_SHIFT = 6;
    private static final int BLOCK_SIZE = 1 << BLOCK_SHIFT;
    private static final int BLOCK_MASK = BLOCK_SIZE - 1;

    private static final int SLOTS = 64;
    private static final int SLOT_MASK = SLOTS - 1;
    private static final int MIN_SOURCE_UNITS = 256;
    private static final int MAX_SOURCE_UNITS = 32_768;

    private static final AtomicReferenceArray<Entry> CACHE =
            new AtomicReferenceArray<>(SLOTS);

    private M3StringPositionPrecompute() {}

    static int indexOf(M3String source, char unit, int fromIndex, int endIndex) {
        int from = Math.max(0, fromIndex);
        int end = Math.min(source.length(), endIndex);
        if (from >= end) return -1;

        Blocks blocks = prepare(source);
        if (blocks == null) {
            return linearIndexOf(source, unit, from, end);
        }

        long required = M3StringFacts.codeUnitSignal(unit);
        int index = from;
        while (index < end) {
            int block = index >>> BLOCK_SHIFT;
            int blockEnd = Math.min(end, (block + 1) << BLOCK_SHIFT);
            if ((blockSignal(source, blocks, block) & required) != required) {
                index = blockEnd;
                continue;
            }
            long positions = exactBlock(source, blocks, block).mask(unit);
            int offset = index & BLOCK_MASK;
            positions &= -1L << offset;
            int width = blockEnd - (block << BLOCK_SHIFT);
            if (width < Long.SIZE) positions &= (1L << width) - 1L;
            if (positions != 0L) {
                return (block << BLOCK_SHIFT) + Long.numberOfTrailingZeros(positions);
            }
            index = blockEnd;
        }
        return -1;
    }

    static int lastIndexOf(M3String source, char unit, int fromIndex) {
        int from = Math.min(fromIndex, source.length() - 1);
        if (from < 0) return -1;

        Blocks blocks = prepare(source);
        if (blocks == null) {
            return linearLastIndexOf(source, unit, from);
        }

        long required = M3StringFacts.codeUnitSignal(unit);
        int index = from;
        while (index >= 0) {
            int block = index >>> BLOCK_SHIFT;
            int blockStart = block << BLOCK_SHIFT;
            if ((blockSignal(source, blocks, block) & required) != required) {
                index = blockStart - 1;
                continue;
            }
            long positions = exactBlock(source, blocks, block).mask(unit);
            int offset = index & BLOCK_MASK;
            if (offset < Long.SIZE - 1) positions &= (1L << (offset + 1)) - 1L;
            if (positions != 0L) {
                return blockStart + (Long.SIZE - 1 - Long.numberOfLeadingZeros(positions));
            }
            index = blockStart - 1;
        }
        return -1;
    }

    static long maximumRetainedPrimitiveBytes() {
        long blocksPerEntry =
                (MAX_SOURCE_UNITS + BLOCK_MASK) >>> BLOCK_SHIFT;
        long signalBytes = (long) SLOTS * blocksPerEntry * Long.BYTES;
        long exactBytes =
                (long) SLOTS * MAX_SOURCE_UNITS * (Character.BYTES + Long.BYTES);
        return Math.addExact(signalBytes, exactBytes);
    }

    private static Blocks prepare(M3String source) {
        int length = source.length();
        if (length < MIN_SOURCE_UNITS || length > MAX_SOURCE_UNITS) return null;

        M3StringOwner owner = source.owner();
        long coordinate = source.coordinate();
        int slot = slot(owner, coordinate);
        Entry entry = CACHE.get(slot);
        if (entry != null
                && entry.owner.get() == owner
                && entry.coordinate == coordinate
                && entry.length == length) {
            return entry.blocks;
        }

        Blocks blocks = new Blocks((length + BLOCK_MASK) >>> BLOCK_SHIFT);
        CACHE.set(
                slot,
                new Entry(
                        new WeakReference<>(owner),
                        coordinate,
                        length,
                        blocks));
        return blocks;
    }

    private static long blockSignal(M3String source, Blocks blocks, int block) {
        long current = blocks.signals.get(block);
        if (current != 0L) return current;

        int start = block << BLOCK_SHIFT;
        int end = Math.min(source.length(), start + BLOCK_SIZE);
        long computed = 0L;
        for (int index = start; index < end; index++) {
            computed |= M3StringFacts.codeUnitSignal(source.charAt(index));
        }
        if (computed == 0L) {
            throw new InternalError("M3 position block produced empty signal");
        }
        if (blocks.signals.compareAndSet(block, 0L, computed)) {
            return computed;
        }
        return blocks.signals.get(block);
    }

    private static ExactBlock exactBlock(M3String source, Blocks blocks, int block) {
        ExactBlock current = blocks.exact.get(block);
        if (current != null) return current;

        int start = block << BLOCK_SHIFT;
        int end = Math.min(source.length(), start + BLOCK_SIZE);
        char[] units = new char[end - start];
        long[] masks = new long[end - start];
        int count = 0;

        for (int index = start; index < end; index++) {
            char unit = source.charAt(index);
            int at = 0;
            while (at < count && units[at] < unit) at++;
            if (at < count && units[at] == unit) {
                masks[at] |= 1L << (index - start);
                continue;
            }
            if (at < count) {
                System.arraycopy(units, at, units, at + 1, count - at);
                System.arraycopy(masks, at, masks, at + 1, count - at);
            }
            units[at] = unit;
            masks[at] = 1L << (index - start);
            count++;
        }

        ExactBlock computed = new ExactBlock(
                Arrays.copyOf(units, count),
                Arrays.copyOf(masks, count));
        if (blocks.exact.compareAndSet(block, null, computed)) return computed;
        return blocks.exact.get(block);
    }

    private static int linearIndexOf(M3String source, char unit, int from, int end) {
        for (int index = from; index < end; index++) {
            if (source.charAt(index) == unit) return index;
        }
        return -1;
    }

    private static int linearLastIndexOf(M3String source, char unit, int from) {
        for (int index = from; index >= 0; index--) {
            if (source.charAt(index) == unit) return index;
        }
        return -1;
    }

    private static int slot(M3StringOwner owner, long coordinate) {
        long mixed = coordinate
                ^ Long.rotateLeft(owner.structuralHash64, 17)
                ^ Integer.toUnsignedLong(System.identityHashCode(owner));
        mixed ^= mixed >>> 33;
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= mixed >>> 33;
        return ((int) mixed) & SLOT_MASK;
    }

    private static final class Blocks {
        final AtomicLongArray signals;
        final AtomicReferenceArray<ExactBlock> exact;

        Blocks(int blockCount) {
            this.signals = new AtomicLongArray(blockCount);
            this.exact = new AtomicReferenceArray<>(blockCount);
        }
    }

    private static final class ExactBlock {
        final char[] units;
        final long[] masks;

        ExactBlock(char[] units, long[] masks) {
            this.units = units;
            this.masks = masks;
        }

        long mask(char unit) {
            int index = Arrays.binarySearch(units, unit);
            return index < 0 ? 0L : masks[index];
        }
    }

    private static final class Entry {
        final WeakReference<M3StringOwner> owner;
        final long coordinate;
        final int length;
        final Blocks blocks;

        Entry(WeakReference<M3StringOwner> owner, long coordinate, int length, Blocks blocks) {
            this.owner = owner;
            this.coordinate = coordinate;
            this.length = length;
            this.blocks = blocks;
        }
    }
}
