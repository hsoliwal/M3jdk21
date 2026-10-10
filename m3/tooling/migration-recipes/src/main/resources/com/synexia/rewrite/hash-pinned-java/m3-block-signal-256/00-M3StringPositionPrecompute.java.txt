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
 * <p>Each 64-code-unit source block stores a conservative 256-bit code-unit signal in four words:
 * a unit sets two bits inside the one word its hash picks, so a block test reads one word (A40;
 * the whole-String facts keep their 64-bit signal). Negative block tests may skip exact work;
 * positive blocks always perform exact UTF-16 comparison. Entries weakly key canonical owner+coordinate and retain only primitive
 * block masks: an exact block keys each mask by the block-relative offset of a unit's first
 * occurrence and reads that unit back from the canonical text, so no code unit is stored twice
 * (invariant 6: no second spelling store).</p>
 */
final class M3StringPositionPrecompute {
    private static final int BLOCK_SHIFT = 6;
    private static final int BLOCK_SIZE = 1 << BLOCK_SHIFT;
    private static final int BLOCK_MASK = BLOCK_SIZE - 1;
    /**
     * Lookups a forward walk may make before it judges its signals (A37): a walk that has looked
     * into this many blocks and found the signal passing in at least half of the blocks it walked
     * is paying a lookup through the canonical text per block, more than the vectorized forward
     * scan costs per block, and hands the rest of its range to the linear lane. A signal passes
     * a unit when both its bits fall among the block's, which grows likelier with the block's
     * alphabet (two bits per unit in one of four 63-bit words since A40, in 64 bits before). The
     * reverse walk hands off the same way (A38): its lane tests each window with the forward
     * intrinsic.
     */
    private static final int DENSE_LOOKUPS = 8;
    /** Words of 64 bits in a block signal (A40). */
    private static final int SIGNAL_WORDS = 4;
    private static final int SIGNAL_SHIFT = 2;
    /** Bit 0 of every word of a published signal is set; units set bits 1..63 of their word. */
    private static final long SIGNAL_PUBLISHED = 1L;

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

        Blocks blocks;
        try {
            blocks = prepare(source);
        } catch (OutOfMemoryError unavailable) {
            return linearIndexOf(source, unit, from, end);
        }
        if (blocks == null) {
            return linearIndexOf(source, unit, from, end);
        }

        int word = signalWord(unit);
        long required = signalBits(unit);
        int index = from;
        int walked = 0;
        int lookups = 0;
        while (index < end) {
            int block = index >>> BLOCK_SHIFT;
            int blockEnd = Math.min(end, (block + 1) << BLOCK_SHIFT);
            walked++;
            if ((blockSignal(source, blocks, block, word) & required) != required) {
                index = blockEnd;
                continue;
            }
            if (++lookups >= DENSE_LOOKUPS && lookups * 2 >= walked) {
                return linearIndexOf(source, unit, index, end);
            }
            long positions;
            try {
                positions = exactBlock(source, blocks, block).mask(source, block << BLOCK_SHIFT, unit);
            } catch (OutOfMemoryError unavailable) {
                for (; index < blockEnd; index++) {
                    if (source.charAt(index) == unit) return index;
                }
                continue;
            }
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

        Blocks blocks;
        try {
            blocks = prepare(source);
        } catch (OutOfMemoryError unavailable) {
            return linearLastIndexOf(source, unit, from);
        }
        if (blocks == null) {
            return linearLastIndexOf(source, unit, from);
        }

        int word = signalWord(unit);
        long required = signalBits(unit);
        int index = from;
        int walked = 0;
        int lookups = 0;
        while (index >= 0) {
            int block = index >>> BLOCK_SHIFT;
            int blockStart = block << BLOCK_SHIFT;
            walked++;
            if ((blockSignal(source, blocks, block, word) & required) != required) {
                index = blockStart - 1;
                continue;
            }
            if (++lookups >= DENSE_LOOKUPS && lookups * 2 >= walked) {
                return linearLastIndexOf(source, unit, index);
            }
            long positions;
            try {
                positions = exactBlock(source, blocks, block).mask(source, blockStart, unit);
            } catch (OutOfMemoryError unavailable) {
                for (; index >= blockStart; index--) {
                    if (source.charAt(index) == unit) return index;
                }
                continue;
            }
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
        long signalBytes = (long) SLOTS * blocksPerEntry * Long.BYTES * SIGNAL_WORDS;
        long exactBytes =
                (long) SLOTS * MAX_SOURCE_UNITS * (Byte.BYTES + Long.BYTES);
        return Math.addExact(signalBytes, exactBytes);
    }

    /**
     * The block index of a source searched before, or {@code null} when this search should scan
     * (A32): a first search registers the source without blocks and scans it through the window
     * lane, the next search of the same source allocates the blocks (their signals and exact
     * masks still fill lazily), so a source searched once never pays the index and a source
     * searched again pays it once. Sources outside the block range never register.
     */
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
            if (entry.blocks != null) return entry.blocks;
            Blocks blocks = new Blocks((length + BLOCK_MASK) >>> BLOCK_SHIFT);
            CACHE.set(slot, new Entry(entry.owner, coordinate, length, blocks));
            return blocks;
        }

        CACHE.set(
                slot,
                new Entry(
                        new WeakReference<>(owner),
                        coordinate,
                        length,
                        null));
        return null;
    }

    /**
     * The {@code word}-th signal word of {@code block} (A40), published on first use together
     * with the block's other words: every word of a published signal carries bit 0, so a word
     * read as zero is unpublished.
     */
    private static long blockSignal(M3String source, Blocks blocks, int block, int word) {
        int at = (block << SIGNAL_SHIFT) + word;
        long current = blocks.signals.get(at);
        if (current != 0L) return current;

        int start = block << BLOCK_SHIFT;
        int end = Math.min(source.length(), start + BLOCK_SIZE);
        char[] scratch = new char[end - start];
        source.getChars(start, end, scratch, 0);
        long[] words = new long[SIGNAL_WORDS];
        Arrays.fill(words, SIGNAL_PUBLISHED);
        for (char unit : scratch) {
            words[signalWord(unit)] |= signalBits(unit);
        }
        // Two publishers of one block compute the same words; every word stands on its own.
        int base = block << SIGNAL_SHIFT;
        for (int index = 0; index < SIGNAL_WORDS; index++) {
            blocks.signals.set(base + index, words[index]);
        }
        return words[word];
    }

    /** The word, 0..3, a unit's two signal bits live in (A40). */
    private static int signalWord(char unit) {
        return mix32(unit) & (SIGNAL_WORDS - 1);
    }

    /** The two bits, in 1..63 of the unit's word, a unit sets in a block signal (A40). */
    private static long signalBits(char unit) {
        int mixed = mix32(unit);
        int first = 1 + ((mixed >>> 2) & 0xffff) % 63;
        int second = 1 + ((mixed >>> 18) & 0x3fff) % 63;
        return (1L << first) | (1L << second);
    }

    private static int mix32(int value) {
        int mixed = value;
        mixed ^= mixed >>> 16;
        mixed *= 0x7feb352d;
        mixed ^= mixed >>> 15;
        mixed *= 0x846ca68b;
        return mixed ^ (mixed >>> 16);
    }

    private static ExactBlock exactBlock(M3String source, Blocks blocks, int block) {
        ExactBlock current = blocks.exact.get(block);
        if (current != null) return current;

        int start = block << BLOCK_SHIFT;
        int end = Math.min(source.length(), start + BLOCK_SIZE);
        char[] scratch = new char[end - start];
        source.getChars(start, end, scratch, 0);
        // Scratch only: the distinct units order the lanes during construction and are then
        // dropped; the block keeps the first-occurrence offsets (ordered by unit) and the masks.
        char[] units = new char[scratch.length];
        byte[] offsets = new byte[scratch.length];
        long[] masks = new long[scratch.length];
        int count = 0;

        for (int index = 0; index < scratch.length; index++) {
            char unit = scratch[index];
            int at = 0;
            while (at < count && units[at] < unit) at++;
            if (at < count && units[at] == unit) {
                masks[at] |= 1L << index;
                continue;
            }
            if (at < count) {
                System.arraycopy(units, at, units, at + 1, count - at);
                System.arraycopy(offsets, at, offsets, at + 1, count - at);
                System.arraycopy(masks, at, masks, at + 1, count - at);
            }
            units[at] = unit;
            offsets[at] = (byte) index;
            masks[at] = 1L << index;
            count++;
        }

        ExactBlock computed = new ExactBlock(
                Arrays.copyOf(offsets, count),
                Arrays.copyOf(masks, count));
        if (blocks.exact.compareAndSet(block, null, computed)) return computed;
        return blocks.exact.get(block);
    }

    /**
     * Sources outside the block range (shorter than {@link #MIN_SOURCE_UNITS}, longer than
     * {@link #MAX_SOURCE_UNITS}, or without a block entry) and the rest of a forward walk whose
     * signals kept passing (A37): the units come out in bulk windows in the source coder (graded
     * through {@link M3String#windowUnits}, A33) and the stock single-unit search runs over each
     * window (A31); a unit above 0xff is never in Latin-1 storage.
     */
    private static int linearIndexOf(M3String source, char unit, int from, int end) {
        byte coder = source.coder();
        if (coder == String.LATIN1 && unit > 0xff) return -1;
        byte[] window = null;
        for (int base = from; base < end; ) {
            int count = M3String.windowUnits(base - from, end - base);
            if (window == null || window.length < count << coder) window = new byte[count << coder];
            source.getBytes(window, base, 0, coder, count);
            int index = coder == String.LATIN1
                    ? StringLatin1.indexOf(window, unit, 0, count)
                    : StringUTF16.indexOf(window, unit, 0, count);
            if (index >= 0) return base + index;
            base += count;
        }
        return -1;
    }

    /**
     * The reverse of {@link #linearIndexOf}: windows walked from {@code from} down to the start.
     * The stock reverse single-unit search is a scalar loop while the forward one is an intrinsic,
     * so each window is first tested forward (A38); only a window holding the unit is then walked
     * back from its end to the last occurrence, at most the window's length.
     */
    private static int linearLastIndexOf(M3String source, char unit, int from) {
        byte coder = source.coder();
        if (coder == String.LATIN1 && unit > 0xff) return -1;
        byte[] window = null;
        for (int stop = from + 1; stop > 0; ) {
            int count = M3String.windowUnits(from + 1 - stop, stop);
            int base = stop - count;
            if (window == null || window.length < count << coder) window = new byte[count << coder];
            source.getBytes(window, base, 0, coder, count);
            if (coder == String.LATIN1) {
                if (StringLatin1.indexOf(window, unit, 0, count) >= 0) {
                    return base + StringLatin1.lastIndexOf(window, unit, count - 1);
                }
            } else if (StringUTF16.indexOf(window, unit, 0, count) >= 0) {
                return base + StringUTF16.lastIndexOf(window, unit, count - 1);
            }
            stop -= count;
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
            this.signals = new AtomicLongArray(blockCount << SIGNAL_SHIFT);
            this.exact = new AtomicReferenceArray<>(blockCount);
        }
    }

    /**
     * Masks keyed by coordinate, not by spelling: {@code firstOffsets[i]} is the block-relative
     * offset of the first occurrence of the i-th distinct unit in unit order, and lookups read
     * that unit back from the canonical source. Retained lanes are one byte and one long per
     * distinct unit.
     */
    private static final class ExactBlock {
        final byte[] firstOffsets;
        final long[] masks;

        ExactBlock(byte[] firstOffsets, long[] masks) {
            this.firstOffsets = firstOffsets;
            this.masks = masks;
        }

        long mask(M3String source, int blockStart, char unit) {
            int low = 0;
            int high = firstOffsets.length - 1;
            while (low <= high) {
                int mid = (low + high) >>> 1;
                char candidate = source.charAt(blockStart + firstOffsets[mid]);
                if (candidate < unit) {
                    low = mid + 1;
                } else if (candidate > unit) {
                    high = mid - 1;
                } else {
                    return masks[mid];
                }
            }
            return 0L;
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
