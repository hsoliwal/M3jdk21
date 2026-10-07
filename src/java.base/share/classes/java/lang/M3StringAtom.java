/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 */
package java.lang;

import java.util.Objects;

import jdk.internal.misc.Unsafe;

/**
 * Canonical scalar M3String owner.
 *
 * <p>The spelling is native/mapped storage addressed by {@code address}. No byte[] or char[]
 * payload is retained by this owner.</p>
 */
final class M3StringAtom extends M3StringOwner {
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();

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
        } else if (width == 1) {
            for (int index = 0; index < length; index++) {
                UNSAFE.putByte(address + index, compactValue[index]);
            }
        } else {
            for (int index = 0; index < length; index++) {
                char unit = StringUTF16.charAt(compactValue, index);
                long at = address + ((long) index << 1);
                UNSAFE.putByte(at, (byte) (unit >>> 8));
                UNSAFE.putByte(at + 1L, (byte) unit);
            }
        }
        return new M3StringAtom(
                null, address, width, true, length, coder, javaHash, canonicalId, structuralHash64);
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
                true,
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
            long at = address + ((long) index << 1);
            UNSAFE.putByte(at, (byte) (unit >>> 8));
            UNSAFE.putByte(at + 1L, (byte) unit);
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
            if (width == 1) {
                UNSAFE.putByte(address + index, (byte) unit);
            } else {
                long at = address + ((long) index << 1);
                UNSAFE.putByte(at, (byte) (unit >>> 8));
                UNSAFE.putByte(at + 1L, (byte) unit);
            }
        }
        return new M3StringAtom(
                null,
                address,
                width,
                true,
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
            if (width == 1) {
                UNSAFE.putByte(address + index, (byte) unit);
            } else {
                long at = address + ((long) index << 1);
                UNSAFE.putByte(at, (byte) 0);
                UNSAFE.putByte(at + 1L, (byte) unit);
            }
        }
        return new M3StringAtom(
                null,
                address,
                width,
                true,
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
            if (width == 1) {
                UNSAFE.putByte(address + index, (byte) unit);
            } else {
                long at = address + ((long) index << 1);
                UNSAFE.putByte(at, (byte) (unit >>> 8));
                UNSAFE.putByte(at + 1L, (byte) unit);
            }
        }
        return new M3StringAtom(
                null,
                address,
                width,
                true,
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
        if (width == 1) {
            UNSAFE.putByte(address, (byte) unit);
        } else {
            UNSAFE.putByte(address, (byte) (unit >>> 8));
            UNSAFE.putByte(address + 1L, (byte) unit);
        }
        return new M3StringAtom(
                null,
                address,
                width,
                true,
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
        long at = address + ((long) index << 1);
        int first = UNSAFE.getByte(at) & 0xff;
        int second = UNSAFE.getByte(at + 1L) & 0xff;
        return bigEndian
                ? (char) ((first << 8) | second)
                : (char) (first | (second << 8));
    }

    @Override
    void getChars(int start, int end, char[] destination, int destinationStart) {
        Objects.checkFromToIndex(start, end, length);
        Objects.checkFromIndexSize(destinationStart, end - start, destination.length);
        if (storageWidth == 1) {
            long source = address + start;
            for (int target = destinationStart; start < end; start++, target++, source++) {
                destination[target] = (char) (UNSAFE.getByte(source) & 0xff);
            }
            return;
        }
        long source = address + ((long) start << 1);
        for (int target = destinationStart; start < end; start++, target++, source += 2L) {
            int first = UNSAFE.getByte(source) & 0xff;
            int second = UNSAFE.getByte(source + 1L) & 0xff;
            destination[target] = bigEndian
                    ? (char) ((first << 8) | second)
                    : (char) (first | (second << 8));
        }
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
            long source = address + start;
            for (int target = destinationStart; start < end; start++, target++, source++) {
                destination[target] = UNSAFE.getByte(source);
            }
            return;
        }

        long source = address + (storageWidth == 1 ? start : ((long) start << 1));
        for (int target = destinationStart; start < end; start++, target++) {
            char unit;
            if (storageWidth == 1) {
                unit = (char) (UNSAFE.getByte(source++) & 0xff);
            } else {
                int first = UNSAFE.getByte(source) & 0xff;
                int second = UNSAFE.getByte(source + 1L) & 0xff;
                unit = bigEndian
                        ? (char) ((first << 8) | second)
                        : (char) (first | (second << 8));
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
        for (int index = 0; index < count; index++) {
            if (source[offset + index] != charAt(index)) return false;
        }
        return true;
    }

    boolean contentEquals(byte[] compactValue, byte valueCoder) {
        if (valueCoder != coder || (compactValue.length >> valueCoder) != length) return false;
        for (int index = 0; index < length; index++) {
            char candidate =
                    valueCoder == String.LATIN1
                            ? StringLatin1.charAt(compactValue, index)
                            : StringUTF16.charAt(compactValue, index);
            if (candidate != charAt(index)) return false;
        }
        return true;
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
