/* SPDX-License-Identifier: Apache-2.0 */
// Relocated from com.synexia m3-fastsearch/jdk/internal/m3/fastsearch/knowledge/util/Utf8Fnv1a64.java (blob 0935545e35c1) by M3_SEARCH_RELOCATE v4.
package jdk.m3.search;

/** Allocation-free FNV-1a over the UTF-8 encoding of String ranges. */
public final class M3Utf8Fnv1a64 {
    public static final long OFFSET_BASIS = 0xcbf29ce484222325L;
    private static final long PRIME = 0x100000001b3L;

    private M3Utf8Fnv1a64() {
    }

    public static long hash(String value, int start, int end) {
        return update(OFFSET_BASIS, value, start, end);
    }

    public static long update(long hash, String value, int start, int end) {
        if (start < 0 || end < start || end > value.length()) {
            throw new IndexOutOfBoundsException("invalid String range");
        }
        int index = start;
        while (index < end) {
            char first = value.charAt(index++);
            if (first < 0x80) {
                hash = updateByte(hash, first);
                continue;
            }
            if (first < 0x800) {
                hash = updateByte(hash, 0xc0 | (first >>> 6));
                hash = updateByte(hash, 0x80 | (first & 0x3f));
                continue;
            }
            if (Character.isHighSurrogate(first)
                    && index < end
                    && Character.isLowSurrogate(value.charAt(index))) {
                int codePoint = Character.toCodePoint(first, value.charAt(index++));
                hash = updateByte(hash, 0xf0 | (codePoint >>> 18));
                hash = updateByte(hash, 0x80 | ((codePoint >>> 12) & 0x3f));
                hash = updateByte(hash, 0x80 | ((codePoint >>> 6) & 0x3f));
                hash = updateByte(hash, 0x80 | (codePoint & 0x3f));
                continue;
            }
            if (Character.isSurrogate(first)) {
                // String.getBytes(UTF_8) uses '?' as the replacement for an unpaired surrogate.
                hash = updateByte(hash, '?');
                continue;
            }
            hash = updateByte(hash, 0xe0 | (first >>> 12));
            hash = updateByte(hash, 0x80 | ((first >>> 6) & 0x3f));
            hash = updateByte(hash, 0x80 | (first & 0x3f));
        }
        return hash;
    }

    public static long updateByte(long hash, int value) {
        return (hash ^ (value & 0xffL)) * PRIME;
    }
}
