/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package java.lang;

import java.util.Objects;

import static jdk.internal.access.JavaLangAccess.M3StringLeafLanes.ADDRESS;
import static jdk.internal.access.JavaLangAccess.M3StringLeafLanes.BIG_ENDIAN;
import static jdk.internal.access.JavaLangAccess.M3StringLeafLanes.DESCRIPTOR;
import static jdk.internal.access.JavaLangAccess.M3StringLeafLanes.INLINE;
import static jdk.internal.access.JavaLangAccess.M3StringLeafLanes.INLINE_HEAD_UNITS;
import static jdk.internal.access.JavaLangAccess.M3StringLeafLanes.INLINE_MAX_UNITS;
import static jdk.internal.access.JavaLangAccess.M3StringLeafLanes.INLINE_TAIL_SHIFT;
import static jdk.internal.access.JavaLangAccess.M3StringLeafLanes.NOT_M3;
import static jdk.internal.access.JavaLangAccess.M3StringLeafLanes.STRIDE;
import static jdk.internal.access.JavaLangAccess.M3StringLeafLanes.WIDTH_SHIFT;

/**
 * Writes the scalar leaf ranges of one M3String into caller-owned primitive lanes.
 *
 * <p>The traversal is the existing {@link M3StringLeafCursor} with adjacent ranges of one owner
 * coalesced. ATOM leaves report the address of their first visible unit; IMMEDIATE leaves have no
 * native payload and carry their decoded Latin-1 units inline. The layout and lifetime contract
 * are documented on {@code JavaLangAccess.stringM3LeafRanges}. Nothing here retains an address
 * or the source value after the call returns.</p>
 */
//FLLDCIM: IOP - pattern=M3StringOwnerKinds role=LeafRangeLanes contract=JavaLangAccess.stringM3LeafRanges
//FLLDCIM: DOOP - one cursor per call; leaves are primitive lane words, never objects
final class M3StringLeafRanges {
    private static final int BYTE_BITS = Byte.SIZE;
    private static final int LATIN1_UNIT_MASK = 0xff;
    private static final int LATIN1_WIDTH = 1;
    private static final int UTF16_WIDTH = 2;

    private M3StringLeafRanges() {}

    /**
     * Returns {@code NOT_M3} for {@code null} storage, otherwise the total leaf count; writes the
     * first {@code min(count, lanes.length / STRIDE)} leaves.
     */
    static int fill(M3String storage, long[] lanes) {
        Objects.requireNonNull(lanes, "lanes");
        if (storage == null) return NOT_M3;
        int capacity = lanes.length / STRIDE;
        int count = 0;
        M3StringLeafCursor cursor = new M3StringLeafCursor(storage, true);
        while (cursor.next()) {
            if (count < capacity) {
                write(cursor.owner(), cursor.start(), cursor.length(), lanes, count * STRIDE);
            }
            count++;
        }
        return count;
    }

    private static void write(M3StringOwner owner, int start, int units, long[] lanes, int at) {
        if (owner instanceof M3StringAtom atom) {
            int width = atom.storageWidth;
            long descriptor = Integer.toUnsignedLong(units) | ((long) width << WIDTH_SHIFT);
            if (width == UTF16_WIDTH && atom.bigEndian) descriptor |= BIG_ENDIAN;
            // Width 1 or 2 bytes: the byte offset of unit start is start << (width - 1).
            lanes[at + ADDRESS] = atom.address + ((long) start << (width - LATIN1_WIDTH));
            lanes[at + DESCRIPTOR] = descriptor;
        } else if (owner instanceof M3StringImmediate) {
            writeInline(owner, start, units, lanes, at);
        } else {
            throw new InternalError("M3String leaf owner kind " + owner.kind);
        }
    }

    /** Immediate owners are LATIN1-coded and at most {@code INLINE_MAX_UNITS} units long. */
    private static void writeInline(
            M3StringOwner owner, int start, int units, long[] lanes, int at) {
        if (units > INLINE_MAX_UNITS) throw new InternalError("M3String inline leaf length");
        long head = 0L;
        long tail = Integer.toUnsignedLong(units) | ((long) LATIN1_WIDTH << WIDTH_SHIFT) | INLINE;
        for (int index = 0; index < units; index++) {
            char unit = owner.charAt(start + index);
            if (unit > LATIN1_UNIT_MASK) throw new InternalError("M3String inline leaf unit");
            if (index < INLINE_HEAD_UNITS) {
                head |= (long) unit << (index * BYTE_BITS);
            } else {
                int shift = INLINE_TAIL_SHIFT + (index - INLINE_HEAD_UNITS) * BYTE_BITS;
                tail |= (long) unit << shift;
            }
        }
        lanes[at + ADDRESS] = head;
        lanes[at + DESCRIPTOR] = tail;
    }
}
