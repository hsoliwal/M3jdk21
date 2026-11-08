// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays.packed;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable, lossless integer codebook with bit-packed dictionary indices.
 * A primitive open-addressing table is used only while encoding: no boxed
 * per-value objects, retained hash table, or aliased caller storage.
 *
 * <p>The encoder conservatively selects RAW storage when codebook bytes plus
 * indices are not strictly smaller than four bytes per source element.
 * This compares payloads only: Java object headers are not included.
 */
public final class IntCodebookArray {
    private static final int MAX_DICTIONARY_VALUES = 1 << 15;

    private final int length;
    private final int[] raw;
    private final int[] dictionary;
    private final byte[] indices;
    private final int bits;

    private IntCodebookArray(int length, int[] raw, int[] dictionary,
                             byte[] indices, int bits) {
        this.length = length;
        this.raw = raw;
        this.dictionary = dictionary;
        this.indices = indices;
        this.bits = bits;
    }

    public static IntCodebookArray of(int[] source) {
        Objects.requireNonNull(source, "source");
        int[] snapshot = source.clone();
        if (snapshot.length < 2) return raw(snapshot);

        int[] symbols = new int[Math.min(snapshot.length, 16)];
        int[] slots = new int[16]; // Zero is empty; otherwise id + 1.
        int distinct = 0;
        for (int value : snapshot) {
            int slot = locate(slots, symbols, value);
            if (slots[slot] != 0) continue;
            if (distinct == MAX_DICTIONARY_VALUES) return raw(snapshot);
            if ((distinct + 1) * 4 >= slots.length * 3) {
                slots = growSlots(slots, symbols);
                slot = locate(slots, symbols, value);
            }
            if (distinct == symbols.length) {
                symbols = Arrays.copyOf(symbols,
                        Math.min(MAX_DICTIONARY_VALUES, symbols.length * 2));
            }
            symbols[distinct] = value;
            slots[slot] = ++distinct;
        }
        int bits = distinct == 1 ? 0 : Integer.SIZE - Integer.numberOfLeadingZeros(distinct - 1);
        long indexBytes = ((long) snapshot.length * bits + 7L) >>> 3;
        long encodedBytes = (long) distinct * Integer.BYTES + indexBytes;
        if (encodedBytes >= (long) snapshot.length * Integer.BYTES
                || indexBytes > Integer.MAX_VALUE - 8L) return raw(snapshot);

        byte[] indices = new byte[(int) indexBytes];
        for (int i = 0; i < snapshot.length; i++) {
            int id = slots[locate(slots, symbols, snapshot[i])] - 1;
            writeIndex(indices, (long) i * bits, bits, id);
        }
        return new IntCodebookArray(snapshot.length, null,
                Arrays.copyOf(symbols, distinct), indices, bits);
    }

    private static IntCodebookArray raw(int[] snapshot) {
        return new IntCodebookArray(snapshot.length, snapshot, null, null, 0);
    }

    private static int locate(int[] slots, int[] symbols, int key) {
        int h = key;
        h ^= h >>> 16;
        h *= 0x7feb352d;
        h ^= h >>> 15;
        h *= 0x846ca68b;
        h ^= h >>> 16;
        int slot = h & (slots.length - 1);
        while (slots[slot] != 0 && symbols[slots[slot] - 1] != key)
            slot = (slot + 1) & (slots.length - 1);
        return slot;
    }

    private static int[] growSlots(int[] previous, int[] symbols) {
        int[] next = new int[previous.length * 2];
        for (int idPlusOne : previous) {
            if (idPlusOne != 0) {
                int slot = locate(next, symbols, symbols[idPlusOne - 1]);
                next[slot] = idPlusOne;
            }
        }
        return next;
    }

    private static void writeIndex(byte[] out, long offset, int width, int id) {
        for (int remaining = width; remaining != 0;) {
            int shift = (int) (offset & 7);
            int take = Math.min(8 - shift, remaining);
            int mask = (1 << take) - 1;
            out[(int) (offset >>> 3)] |= (byte) ((id & mask) << shift);
            id >>>= take;
            offset += take;
            remaining -= take;
        }
    }

    private static int readIndex(byte[] bytes, long offset, int width) {
        int id = 0;
        for (int remaining = width, outputShift = 0; remaining != 0;) {
            int shift = (int) (offset & 7);
            int take = Math.min(8 - shift, remaining);
            int mask = (1 << take) - 1;
            id |= (((bytes[(int) (offset >>> 3)] & 0xff) >>> shift) & mask) << outputShift;
            offset += take;
            outputShift += take;
            remaining -= take;
        }
        return id;
    }

    public int length() { return length; }
    public boolean dictionaryEncoded() { return dictionary != null; }
    public int bitsPerIndex() { return bits; }
    public int dictionarySize() { return dictionary == null ? 0 : dictionary.length; }

    /** Payload bytes only, excluding array/object headers. */
    public long payloadBytes() {
        return dictionary == null ? (long) raw.length * Integer.BYTES
                : (long) dictionary.length * Integer.BYTES + indices.length;
    }

    /** Exact signed-int random access; 0..15 bit-reads on encoded values. */
    public int get(int index) {
        Objects.checkIndex(index, length);
        if (dictionary == null) return raw[index];
        return dictionary[readIndex(indices, (long) index * bits, bits)];
    }

    public int[] toIntArray() {
        if (dictionary == null) return raw.clone();
        int[] result = new int[length];
        for (int i = 0; i < result.length; i++) result[i] = get(i);
        return result;
    }

    /** Detached dictionary values in first-occurrence order. */
    public int[] dictionaryCopy() {
        return dictionary == null ? new int[0] : dictionary.clone();
    }

    /** Detached indices; empty for RAW and zero-bit single-symbol planes. */
    public byte[] packedIndicesCopy() {
        return indices == null ? new byte[0] : indices.clone();
    }

    /** Required JNI exact decode; RAW mode has no codebook to decode. */
    public int[] toIntArrayNativeRequired() {
        if (dictionary == null) throw new IllegalStateException("RAW mode has no codebook");
        return IntCodebookNative.decodeRequired(indices, dictionary, length, bits);
    }
}
