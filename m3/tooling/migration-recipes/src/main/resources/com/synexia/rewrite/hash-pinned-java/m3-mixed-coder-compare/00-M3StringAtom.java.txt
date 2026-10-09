/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 */
package java.lang;

import java.util.Objects;

import jdk.internal.misc.Unsafe;
import jdk.internal.util.ArraysSupport;

/**
 * Canonical scalar M3String owner.
 *
 * <p>The spelling is native/mapped storage addressed by {@code address}. No byte[] or char[]
 * payload is retained by this owner.</p>
 */
final class M3StringAtom extends M3StringOwner {
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    /**
     * Local width-2 atoms are written in the host byte order so admission and reads are bulk
     * copies (A11); the flag stays per atom and every reader (Java, HotSpot, SA) follows it, so
     * mapped lexicon atoms keep their recorded order.
     */
    private static final boolean NATIVE_BIG_ENDIAN = UNSAFE.isBigEndian();
    private static final long BYTE_BASE = Unsafe.ARRAY_BYTE_BASE_OFFSET;
    private static final long CHAR_BASE = Unsafe.ARRAY_CHAR_BASE_OFFSET;
    /** Bulk copies (a runtime call) pay only from this many units; below, the plain loops win. */
    private static final int BULK = 64;
    /** Bytes per window when a cross-coder copy goes through the inflate intrinsic. */
    private static final int WINDOW = 4096;

    /** Retains mapped ownership. VM-local native blocks are lifetime-managed by M3StringPool. */
    final Object payloadOwner;
    final long address;
    final byte storageWidth;
    final boolean bigEndian;
    final long canonicalId;

    M3StringAtom(
            Object payloadOwner,
            long address,
            byte storageWidth,
            boolean bigEndian,
            int length,
            byte coder,
            int javaHash,
            long canonicalId,
            long structuralHash64) {
        super(ATOM, length, coder, javaHash, structuralHash64);
        if (length != 0 && address == 0L) throw new IllegalArgumentException("M3 atom address");
        if (storageWidth != 1 && storageWidth != 2) throw new IllegalArgumentException("M3 atom width");
        this.payloadOwner = payloadOwner;
        this.address = address;
        this.storageWidth = storageWidth;
        this.bigEndian = bigEndian;
        this.canonicalId = canonicalId;
    }

    static M3StringAtom local(
            byte[] compactValue, byte coder, long canonicalId, long structuralHash64) {
        int length = compactValue.length >> coder;
        int javaHash =
                coder == String.LATIN1
                        ? StringLatin1.hashCode(compactValue)
                        : StringUTF16.hashCode(compactValue);
        byte width = coder == String.LATIN1 ? (byte) 1 : (byte) 2;
        long bytes = Math.max(1L, (long) length * width);
        long address = UNSAFE.allocateMemory(bytes);
        if (length == 0) {
            UNSAFE.putByte(address, (byte) 0);
        } else if (length >= BULK) {
            UNSAFE.copyMemory(compactValue, BYTE_BASE, null, address, (long) length * width);
        } else if (width == 1) {
            for (int index = 0; index < length; index++) {
                UNSAFE.putByte(address + index, compactValue[index]);
            }
        } else {
            for (int index = 0; index < length; index++) {
                UNSAFE.putChar(address + ((long) index << 1), StringUTF16.charAt(compactValue, index));
            }
        }
        return new M3StringAtom(null, address, width, NATIVE_BIG_ENDIAN, length, coder, javaHash,
                canonicalId, structuralHash64);
    }

    static M3StringAtom localCodePoints(
            int[] source,
            int offset,
            int count,
            int utf16Length,
            byte coder,
            long canonicalId,
            long structuralHash64) {
        Objects.requireNonNull(source, "source");
        Objects.checkFromIndexSize(offset, count, source.length);
        if (coder != String.LATIN1 && coder != String.UTF16) {
            throw new IllegalArgumentException("invalid String coder");
        }
        byte width = coder == String.LATIN1 ? (byte) 1 : (byte) 2;
        long bytes = Math.max(1L, (long) utf16Length * width);
        long address = UNSAFE.allocateMemory(bytes);
        int javaHash = 0;
        int output = 0;
        for (int index = 0; index < count; index++) {
            int cp = source[offset + index];
            if (Character.isBmpCodePoint(cp)) {
                char unit = (char) cp;
                if (coder == String.LATIN1 && unit > 0xff) {
                    UNSAFE.freeMemory(address);
                    throw new IllegalArgumentException("Latin1 unit out of range");
                }
                javaHash = 31 * javaHash + unit;
                putUnit(address, width, output++, unit);
            } else if (Character.isValidCodePoint(cp)) {
                char high = Character.highSurrogate(cp);
                char low = Character.lowSurrogate(cp);
                javaHash = 31 * javaHash + high;
                javaHash = 31 * javaHash + low;
                putUnit(address, width, output++, high);
                putUnit(address, width, output++, low);
            } else {
                UNSAFE.freeMemory(address);
                throw new IllegalArgumentException(Integer.toString(cp));
            }
        }
        if (output != utf16Length) {
            UNSAFE.freeMemory(address);
            throw new InternalError("M3 code-point UTF16 length mismatch");
        }
        return new M3StringAtom(
                null,
                address,
                width,
                NATIVE_BIG_ENDIAN,
                utf16Length,
                coder,
                javaHash,
                canonicalId,
                structuralHash64);
    }

    private static void putUnit(long address, byte width, int index, char unit) {
        if (width == 1) {
            UNSAFE.putByte(address + index, (byte) unit);
        } else {
            UNSAFE.putChar(address + ((long) index << 1), unit);
        }
    }



    static M3StringAtom localCompactBytes(
            byte[] source,
            int sourceOffset,
            int length,
            byte sourceCoder,
            byte targetCoder,
            long canonicalId,
            long structuralHash64) {
        Objects.requireNonNull(source, "source");
        if (sourceCoder != String.LATIN1 && sourceCoder != String.UTF16) {
            throw new IllegalArgumentException("invalid source String coder");
        }
        if (targetCoder != String.LATIN1 && targetCoder != String.UTF16) {
            throw new IllegalArgumentException("invalid target String coder");
        }
        Objects.checkFromIndexSize(sourceOffset, length, source.length >> sourceCoder);

        byte width = targetCoder == String.LATIN1 ? (byte) 1 : (byte) 2;
        long bytes = Math.max(1L, (long) length * width);
        long address = UNSAFE.allocateMemory(bytes);
        int javaHash = 0;
        for (int index = 0; index < length; index++) {
            char unit =
                    sourceCoder == String.LATIN1
                            ? (char) (source[sourceOffset + index] & 0xff)
                            : StringUTF16.charAt(source, sourceOffset + index);
            if (targetCoder == String.LATIN1 && unit > 0xff) {
                UNSAFE.freeMemory(address);
                throw new IllegalArgumentException("Latin1 unit out of range");
            }
            javaHash = 31 * javaHash + unit;
            putUnit(address, width, index, unit);
        }
        return new M3StringAtom(
                null,
                address,
                width,
                NATIVE_BIG_ENDIAN,
                length,
                targetCoder,
                javaHash,
                canonicalId,
                structuralHash64);
    }

    static M3StringAtom localLatin1Bytes(
            byte[] source,
            int offset,
            int length,
            byte coder,
            long canonicalId,
            long structuralHash64) {
        Objects.requireNonNull(source, "source");
        Objects.checkFromIndexSize(offset, length, source.length);
        if (coder != String.LATIN1 && coder != String.UTF16) {
            throw new IllegalArgumentException("invalid String coder");
        }
        byte width = coder == String.LATIN1 ? (byte) 1 : (byte) 2;
        long bytes = Math.max(1L, (long) length * width);
        long address = UNSAFE.allocateMemory(bytes);
        int javaHash = 0;
        for (int index = 0; index < length; index++) {
            char unit = (char) (source[offset + index] & 0xff);
            javaHash = 31 * javaHash + unit;
            putUnit(address, width, index, unit);
        }
        return new M3StringAtom(
                null,
                address,
                width,
                NATIVE_BIG_ENDIAN,
                length,
                coder,
                javaHash,
                canonicalId,
                structuralHash64);
    }

    static M3StringAtom localChars(
            char[] source,
            int offset,
            int length,
            byte coder,
            long canonicalId,
            long structuralHash64) {
        Objects.requireNonNull(source, "source");
        Objects.checkFromIndexSize(offset, length, source.length);
        if (coder != String.LATIN1 && coder != String.UTF16) {
            throw new IllegalArgumentException("invalid String coder");
        }
        byte width = coder == String.LATIN1 ? (byte) 1 : (byte) 2;
        long bytes = Math.max(1L, (long) length * width);
        long address = UNSAFE.allocateMemory(bytes);
        int javaHash = 0;
        for (int index = 0; index < length; index++) {
            char unit = source[offset + index];
            if (coder == String.LATIN1 && unit > 0xff) {
                UNSAFE.freeMemory(address);
                throw new IllegalArgumentException("Latin1 unit out of range");
            }
            javaHash = 31 * javaHash + unit;
            putUnit(address, width, index, unit);
        }
        return new M3StringAtom(
                null,
                address,
                width,
                NATIVE_BIG_ENDIAN,
                length,
                coder,
                javaHash,
                canonicalId,
                structuralHash64);
    }

    static M3StringAtom localUnit(
            char unit, byte coder, long canonicalId, long structuralHash64) {
        if (coder != String.LATIN1 && coder != String.UTF16) {
            throw new IllegalArgumentException("invalid String coder");
        }
        if (coder == String.LATIN1 && unit > 0xff) {
            throw new IllegalArgumentException("Latin1 unit out of range");
        }
        byte width = coder == String.LATIN1 ? (byte) 1 : (byte) 2;
        long address = UNSAFE.allocateMemory(width);
        putUnit(address, width, 0, unit);
        return new M3StringAtom(
                null,
                address,
                width,
                NATIVE_BIG_ENDIAN,
                1,
                coder,
                unit,
                canonicalId,
                structuralHash64);
    }

    static M3StringAtom mapped(
            Object payloadOwner,
            long address,
            byte storageWidth,
            boolean bigEndian,
            int length,
            byte coder,
            int javaHash,
            long canonicalId,
            long structuralHash64) {
        return new M3StringAtom(
                payloadOwner,
                address,
                storageWidth,
                bigEndian,
                length,
                coder,
                javaHash,
                canonicalId,
                structuralHash64);
    }

    @Override
    char charAt(int index) {
        Objects.checkIndex(index, length);
        if (storageWidth == 1) return (char) (UNSAFE.getByte(address + index) & 0xff);
        char unit = UNSAFE.getChar(address + ((long) index << 1));
        return bigEndian == NATIVE_BIG_ENDIAN ? unit : Character.reverseBytes(unit);
    }

    @Override
    void getChars(int start, int end, char[] destination, int destinationStart) {
        Objects.checkFromToIndex(start, end, length);
        Objects.checkFromIndexSize(destinationStart, end - start, destination.length);
        int count = end - start;
        if (storageWidth == 1) {
            long source = address + start;
            if (count >= BULK) {
                // Latin-1 storage into chars (A26): the bytes come out in bulk through a bounded
                // window and the stock inflate intrinsic widens them.
                byte[] window = new byte[Math.min(count, WINDOW)];
                for (int done = 0; done < count; ) {
                    int chunk = Math.min(window.length, count - done);
                    UNSAFE.copyMemory(null, source + done, window, BYTE_BASE, chunk);
                    StringLatin1.inflate(window, 0, destination, destinationStart + done, chunk);
                    done += chunk;
                }
                return;
            }
            for (int target = destinationStart; start < end; start++, target++, source++) {
                destination[target] = (char) (UNSAFE.getByte(source) & 0xff);
            }
            return;
        }
        long source = address + ((long) start << 1);
        if (count < BULK) {
            boolean swap = bigEndian != NATIVE_BIG_ENDIAN;
            for (int target = destinationStart; start < end; start++, target++, source += 2L) {
                char unit = UNSAFE.getChar(source);
                destination[target] = swap ? Character.reverseBytes(unit) : unit;
            }
            return;
        }
        long target = CHAR_BASE + ((long) destinationStart << 1);
        if (bigEndian == NATIVE_BIG_ENDIAN) {
            UNSAFE.copyMemory(null, source, destination, target, (long) count << 1);
        } else {
            UNSAFE.copySwapMemory(null, source, destination, target, (long) count << 1, 2L);
        }
    }

    /**
     * First index in {@code [start, start + count)} whose unit differs from the flat compact
     * value at {@code flatOffset}, {@code -1} when none: the vectorized off-heap mismatch when the
     * widths agree (Latin-1 atom against a Latin-1 value, native-order UTF-16 atom against a
     * UTF-16 value), the window loop otherwise.
     */
    @Override
    int mismatchUnits(int start, byte[] flat, int flatOffset, byte flatCoder, int count) {
        Objects.checkFromIndexSize(start, count, length);
        Objects.checkFromIndexSize(flatOffset, count, flat.length >> flatCoder);
        if (storageWidth == 1 && flatCoder == String.LATIN1) {
            return mismatchBytes(address + start, flat, BYTE_BASE + flatOffset, count, 0);
        }
        if (storageWidth == 2 && flatCoder == String.UTF16 && bigEndian == NATIVE_BIG_ENDIAN) {
            return mismatchBytes(address + ((long) start << 1), flat,
                    BYTE_BASE + ((long) flatOffset << 1), count, 1);
        }
        if (count >= BULK && (storageWidth == 1 || bigEndian == NATIVE_BIG_ENDIAN)) {
            // The other coder (A30): the flat window is brought into the storage coder by the
            // stock inflate or compress intrinsic and compared in place by the vectorized
            // mismatch; a flat UTF-16 window holding a unit above 0xff keeps the per-unit loop
            // for that window (such a unit never equals a Latin-1 storage byte).
            return storageWidth == 1
                    ? mismatchNarrowAgainstUtf16(start, flat, flatOffset, count)
                    : mismatchWideAgainstLatin1(start, flat, flatOffset, count);
        }
        return super.mismatchUnits(start, flat, flatOffset, flatCoder, count);
    }

    /** Latin-1 storage against a UTF-16 flat value: each flat window compressed, then in place. */
    private int mismatchNarrowAgainstUtf16(int start, byte[] flat, int flatOffset, int count) {
        byte[] narrow = new byte[Math.min(count, WINDOW)];
        long source = address + start;
        for (int done = 0; done < count; ) {
            int chunk = Math.min(narrow.length, count - done);
            int flatBase = flatOffset + done;
            if (StringUTF16.compress(flat, flatBase, narrow, 0, chunk) == chunk) {
                int index = mismatchBytes(source + done, narrow, BYTE_BASE, chunk, 0);
                if (index >= 0) return done + index;
            } else {
                for (int index = 0; index < chunk; index++) {
                    if ((UNSAFE.getByte(source + done + index) & 0xff) != StringUTF16.getChar(flat, flatBase + index)) {
                        return done + index;
                    }
                }
            }
            done += chunk;
        }
        return -1;
    }

    /** UTF-16 storage (native byte order) against a Latin-1 flat value: each flat window inflated, then in place. */
    private int mismatchWideAgainstLatin1(int start, byte[] flat, int flatOffset, int count) {
        byte[] wide = new byte[Math.min(count << 1, WINDOW)];
        int perWindow = wide.length >> 1;
        long source = address + ((long) start << 1);
        for (int done = 0; done < count; ) {
            int chunk = Math.min(perWindow, count - done);
            StringLatin1.inflate(flat, flatOffset + done, wide, 0, chunk);
            int index = mismatchBytes(source + ((long) done << 1), wide, BYTE_BASE, chunk, 1);
            if (index >= 0) return done + index;
            done += chunk;
        }
        return -1;
    }

    /** {@code ArraysSupport.mismatch} shape over an absolute address and a heap array. */
    private static int mismatchBytes(long source, byte[] flat, long flatOffset, int count, int log2Scale) {
        int index = 0;
        if (count > 7) {
            index = ArraysSupport.vectorizedMismatch(null, source, flat, flatOffset, count, log2Scale);
            if (index >= 0) return index;
            index = count - ~index;
        }
        if (log2Scale == 0) {
            for (; index < count; index++) {
                if (UNSAFE.getByte(source + index) != UNSAFE.getByte(flat, flatOffset + index)) return index;
            }
            return -1;
        }
        for (; index < count; index++) {
            long at = (long) index << 1;
            if (UNSAFE.getChar(source + at) != UNSAFE.getChar(flat, flatOffset + at)) return index;
        }
        return -1;
    }

    @Override
    void getBytes(
            int start,
            int end,
            byte[] destination,
            int destinationStart,
            byte destinationCoder) {
        Objects.checkFromToIndex(start, end, length);
        int count = end - start;
        Objects.checkFromIndexSize(
                destinationStart << destinationCoder,
                count << destinationCoder,
                destination.length);

        if (storageWidth == 1 && destinationCoder == String.LATIN1) {
            if (count >= BULK) {
                UNSAFE.copyMemory(null, address + start, destination, BYTE_BASE + destinationStart, count);
                return;
            }
            long source = address + start;
            for (int target = destinationStart; start < end; start++, target++, source++) {
                destination[target] = UNSAFE.getByte(source);
            }
            return;
        }
        if (storageWidth == 2 && destinationCoder == String.UTF16 && count >= BULK) {
            long source = address + ((long) start << 1);
            long target = BYTE_BASE + ((long) destinationStart << 1);
            if (bigEndian == NATIVE_BIG_ENDIAN) {
                UNSAFE.copyMemory(null, source, destination, target, (long) count << 1);
            } else {
                UNSAFE.copySwapMemory(null, source, destination, target, (long) count << 1, 2L);
            }
            return;
        }
        if (storageWidth == 1 && destinationCoder == String.UTF16 && count >= BULK) {
            // Latin-1 storage into a UTF-16 destination (A22): the bytes come out in bulk through
            // a bounded window and the stock inflate intrinsic widens them.
            byte[] window = new byte[Math.min(count, WINDOW)];
            long source = address + start;
            for (int done = 0; done < count; ) {
                int chunk = Math.min(window.length, count - done);
                UNSAFE.copyMemory(null, source + done, window, BYTE_BASE, chunk);
                StringLatin1.inflate(window, 0, destination, destinationStart + done, chunk);
                done += chunk;
            }
            return;
        }

        if (storageWidth == 2 && destinationCoder == String.LATIN1 && count >= BULK) {
            // UTF-16 storage into a Latin-1 destination (A28): the units come out in bulk through a
            // bounded window and the stock compress intrinsic narrows them; a window holding a unit
            // above 0xff keeps the per-unit truncation of the loop below.
            byte[] window = new byte[Math.min(count << 1, WINDOW)];
            int perWindow = window.length >> 1;
            long source = address + ((long) start << 1);
            for (int done = 0; done < count; ) {
                int chunk = Math.min(perWindow, count - done);
                if (bigEndian == NATIVE_BIG_ENDIAN) {
                    UNSAFE.copyMemory(null, source + ((long) done << 1), window, BYTE_BASE, (long) chunk << 1);
                } else {
                    UNSAFE.copySwapMemory(null, source + ((long) done << 1), window, BYTE_BASE, (long) chunk << 1, 2L);
                }
                if (StringUTF16.compress(window, 0, destination, destinationStart + done, chunk) != chunk) {
                    for (int index = 0; index < chunk; index++) {
                        destination[destinationStart + done + index] = (byte) StringUTF16.getChar(window, index);
                    }
                }
                done += chunk;
            }
            return;
        }

        long source = address + (storageWidth == 1 ? start : ((long) start << 1));
        for (int target = destinationStart; start < end; start++, target++) {
            char unit;
            if (storageWidth == 1) {
                unit = (char) (UNSAFE.getByte(source++) & 0xff);
            } else {
                unit = UNSAFE.getChar(source);
                if (bigEndian != NATIVE_BIG_ENDIAN) unit = Character.reverseBytes(unit);
                source += 2L;
            }
            if (destinationCoder == String.LATIN1) {
                destination[target] = (byte) unit;
            } else {
                StringUTF16.putChar(destination, target, unit);
            }
        }
    }

    long nativePayloadBytes() {
        return Math.max(1L, Math.multiplyExact((long) length, storageWidth));
    }

    boolean contentEqualsCodePoints(
            int[] source, int offset, int count, int utf16Length, byte targetCoder) {
        Objects.requireNonNull(source, "source");
        Objects.checkFromIndexSize(offset, count, source.length);
        if (targetCoder != coder || utf16Length != length) return false;
        int at = 0;
        for (int index = 0; index < count; index++) {
            int cp = source[offset + index];
            if (Character.isBmpCodePoint(cp)) {
                if (charAt(at++) != (char) cp) return false;
            } else if (Character.isValidCodePoint(cp)) {
                if (charAt(at++) != Character.highSurrogate(cp)
                        || charAt(at++) != Character.lowSurrogate(cp)) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return at == length;
    }

    boolean contentEqualsCompactBytes(
            byte[] source,
            int sourceOffset,
            int count,
            byte sourceCoder,
            byte targetCoder) {
        Objects.requireNonNull(source, "source");
        if (sourceCoder != String.LATIN1 && sourceCoder != String.UTF16) return false;
        Objects.checkFromIndexSize(sourceOffset, count, source.length >> sourceCoder);
        if (targetCoder != coder || count != length) return false;
        if (sourceCoder == coder) {
            return mismatchUnits(0, source, sourceOffset, sourceCoder, count) < 0;
        }
        for (int index = 0; index < count; index++) {
            char candidate =
                    sourceCoder == String.LATIN1
                            ? (char) (source[sourceOffset + index] & 0xff)
                            : StringUTF16.charAt(source, sourceOffset + index);
            if (candidate != charAt(index)) return false;
        }
        return true;
    }

    boolean contentEqualsLatin1Bytes(
            byte[] source, int offset, int count, byte valueCoder) {
        Objects.requireNonNull(source, "source");
        Objects.checkFromIndexSize(offset, count, source.length);
        if (valueCoder != coder || count != length) return false;
        for (int index = 0; index < count; index++) {
            if ((char) (source[offset + index] & 0xff) != charAt(index)) return false;
        }
        return true;
    }

    boolean contentEquals(char[] source, int offset, int count, byte valueCoder) {
        Objects.requireNonNull(source, "source");
        Objects.checkFromIndexSize(offset, count, source.length);
        if (valueCoder != coder || count != length) return false;
        if (storageWidth == 2 && bigEndian == NATIVE_BIG_ENDIAN) {
            return mismatchChars(address, source, CHAR_BASE + ((long) offset << 1), count) < 0;
        }
        for (int index = 0; index < count; index++) {
            if (source[offset + index] != charAt(index)) return false;
        }
        return true;
    }

    /** {@code ArraysSupport.mismatch} shape over an absolute address and a char array. */
    private static int mismatchChars(long source, char[] units, long unitsOffset, int count) {
        int index = 0;
        if (count > 7) {
            index = ArraysSupport.vectorizedMismatch(null, source, units, unitsOffset, count, 1);
            if (index >= 0) return index;
            index = count - ~index;
        }
        for (; index < count; index++) {
            long at = (long) index << 1;
            if (UNSAFE.getChar(source + at) != UNSAFE.getChar(units, unitsOffset + at)) return index;
        }
        return -1;
    }

    /**
     * First index in {@code [start, start + count)} whose unit differs from {@code other} at
     * {@code otherStart}, {@code -1} when none: native-to-native vectorized mismatch when both
     * atoms share the width and the byte order, a unit loop otherwise.
     */
    int mismatchAtom(int start, M3StringAtom other, int otherStart, int count) {
        Objects.checkFromIndexSize(start, count, length);
        Objects.checkFromIndexSize(otherStart, count, other.length);
        if (storageWidth == other.storageWidth && (storageWidth == 1 || bigEndian == other.bigEndian)) {
            int scale = storageWidth - 1;
            return mismatchNative(address + ((long) start << scale),
                    other.address + ((long) otherStart << scale), count, scale);
        }
        for (int index = 0; index < count; index++) {
            if (charAt(start + index) != other.charAt(otherStart + index)) return index;
        }
        return -1;
    }

    /** {@code ArraysSupport.mismatch} shape over two absolute addresses of equal element width. */
    private static int mismatchNative(long first, long second, int count, int log2Scale) {
        int index = 0;
        if (count > 7) {
            index = ArraysSupport.vectorizedMismatch(null, first, null, second, count, log2Scale);
            if (index >= 0) return index;
            index = count - ~index;
        }
        if (log2Scale == 0) {
            for (; index < count; index++) {
                if (UNSAFE.getByte(first + index) != UNSAFE.getByte(second + index)) return index;
            }
            return -1;
        }
        for (; index < count; index++) {
            long at = (long) index << 1;
            if (UNSAFE.getChar(first + at) != UNSAFE.getChar(second + at)) return index;
        }
        return -1;
    }

    boolean contentEquals(byte[] compactValue, byte valueCoder) {
        if (valueCoder != coder || (compactValue.length >> valueCoder) != length) return false;
        return mismatchUnits(0, compactValue, 0, valueCoder, length) < 0;
    }

    @Override
    M3StringFacts computeFacts() {
        return M3StringFacts.scan(M3String.whole(this));
    }

    @Override
    M3StringFacts computeRangeFacts(int start, int length) {
        Objects.checkFromIndexSize(start, length, this.length);
        if (start == 0 && length == this.length) return facts();
        return M3StringFacts.scan(M3String.range(this, start, length));
    }
}
