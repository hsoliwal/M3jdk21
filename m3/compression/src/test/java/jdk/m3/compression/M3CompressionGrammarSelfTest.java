// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compression;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Random;

/** Deterministic, hand-constructed LZ4 and Snappy token grammar probes. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/6fdee5330b8ab005005785aeff0d1105ba40d12c/synexia-algo/src/test/java/com/synexia/algorithms/compression/M3CompressionGrammarSelfTest.java blob 27c4c8ba1530; recipe m3/compression/recipe/compression-20261106/relocate.py v3
public final class M3CompressionGrammarSelfTest {
    private static int checks;

    private M3CompressionGrammarSelfTest() {}

    public static void main(String[] args) {
        runAll();
    }

    public static void runAll() {
        lz4Grammar();
        snappyGrammar();
        malformed();
        System.out.println("M3 compression grammar checks=" + checks);
    }

    private static void lz4Grammar() {
        Random r = new Random(0x4d33_2026_4c5a4cL);
        for (int t = 0; t < 1600; t++) {
            Lz4Builder b = new Lz4Builder();
            int n = 1 + r.nextInt(5);
            for (int i = 0; i < n; i++) {
                byte[] literal = new byte[1 + r.nextInt(t % 8 == 0 ? 350 : 42)];
                r.nextBytes(literal);
                int distance = 1 + r.nextInt(b.size() + literal.length);
                int matched = 4 + r.nextInt(t % 11 == 0 ? 400 : 75);
                b.sequence(literal, distance, matched);
            }
            byte[] end = new byte[8 + r.nextInt(t % 9 == 0 ? 512 : 100)];
            r.nextBytes(end);
            b.finish(end);
            byte[] raw = b.toCompressed();
            byte[] expected = b.toOutput();
            check(Arrays.equals(expected, M3Lz4RawBlockDecoder.decode(raw,
                    expected.length, expected.length)), "LZ4 generated block");
            check(Arrays.equals(expected, M3Lz4RawBlockDecoder.decode(raw,
                    expected.length, expected.length + 1)), "LZ4 bound slack");
        }
        check(Arrays.equals(new byte[0], M3Lz4RawBlockDecoder.decode(new byte[] {0}, 0, 0)),
                "LZ4 empty");
        Lz4Builder large = new Lz4Builder();
        byte[] data = new byte[70_000];
        r.nextBytes(data);
        large.finish(data);
        check(Arrays.equals(data, M3Lz4RawBlockDecoder.decode(large.toCompressed(),
                data.length, data.length)), "LZ4 multiple extension bytes");
    }

    private static void snappyGrammar() {
        Random r = new Random(0x534e_4150_2026L);
        for (int t = 0; t < 1500; t++) {
            SnappyBuilder b = new SnappyBuilder();
            byte[] first = new byte[1 + r.nextInt(90)];
            r.nextBytes(first);
            b.literal(first);
            int n = 1 + r.nextInt(8);
            for (int i = 0; i < n; i++) {
                if (r.nextBoolean()) {
                    byte[] extra = new byte[1 + r.nextInt(t % 17 == 0 ? 750 : 60)];
                    r.nextBytes(extra);
                    b.literal(extra);
                }
                int kind = 1 + r.nextInt(3);
                int distance = 1 + r.nextInt(kind == 1 ? Math.min(2047, b.size()) : b.size());
                int count = kind == 1 ? 4 + r.nextInt(8) : 1 + r.nextInt(64);
                b.copy(distance, count, kind);
            }
            byte[] expected = b.toOutput();
            check(Arrays.equals(expected, M3SnappyRawDecoder.decode(b.toCompressed(),
                    expected.length)), "Snappy all copy tags");
        }
        SnappyBuilder b = new SnappyBuilder();
        byte[] large = new byte[70_000];
        r.nextBytes(large);
        b.literal(large);
        check(Arrays.equals(large, M3SnappyRawDecoder.decode(b.toCompressed(), large.length)),
                "Snappy extended literal");
    }

    private static void malformed() {
        reject(() -> M3Lz4RawBlockDecoder.decode(new byte[0], 0, 0));
        reject(() -> M3Lz4RawBlockDecoder.decode(new byte[] {1}, 0, 0));
        reject(() -> M3Lz4RawBlockDecoder.decode(new byte[] {0}, -1, 0));
        reject(() -> M3Lz4RawBlockDecoder.decode(new byte[] {0}, 1, 0));
        reject(() -> M3Lz4RawBlockDecoder.decode(new byte[] {(byte) 0xf0}, 1, 1));
        reject(() -> M3Lz4RawBlockDecoder.decode(new byte[] {(byte) 0xf0, (byte) 255}, 1, 1));
        reject(() -> M3Lz4RawBlockDecoder.decode(new byte[] {0x10}, 1, 1));
        reject(() -> M3Lz4RawBlockDecoder.decode(new byte[] {0x00, 0, 0}, 4, 4));
        reject(() -> M3Lz4RawBlockDecoder.decode(new byte[] {0x00, 1}, 4, 4));
        reject(() -> M3Lz4RawBlockDecoder.decode(new byte[] {0x10, 65, 1, 0}, 5, 5));
        reject(() -> M3Lz4RawBlockDecoder.decode(new byte[] {0x10, 65, 1, 0, 0}, 6, 6));
        reject(() -> M3Lz4RawBlockDecoder.decode(new byte[] {(byte) 0x1f, 65, 1, 0}, 100, 100));
        reject(() -> M3SnappyRawDecoder.decode(new byte[] {1, (byte) 0xfc}, 1));
        reject(() -> M3SnappyRawDecoder.decode(new byte[] {2, 0, 65, 7, 1, 0, 0, 0}, 2));
    }

    private static void reject(Runnable runnable) {
        try {
            runnable.run();
        } catch (IllegalArgumentException accepted) {
            checks++;
            return;
        }
        throw new AssertionError("malformed stream accepted");
    }

    private static void check(boolean condition, String name) {
        checks++;
        if (!condition) throw new AssertionError(name + " check=" + checks);
    }

    private static final class OutputOracle {
        private byte[] values = new byte[128];
        private int size;

        int size() { return size; }
        byte[] toByteArray() { return Arrays.copyOf(values, size); }
        void writeBytes(byte[] bytes) {
            reserve(bytes.length);
            System.arraycopy(bytes, 0, values, size, bytes.length);
            size += bytes.length;
        }
        void repeat(int distance, int count) {
            reserve(count);
            for (int i = 0; i < count; i++) {
                values[size] = values[size - distance];
                size++;
            }
        }
        private void reserve(int length) {
            long desired = (long) size + length;
            if (desired > Integer.MAX_VALUE) throw new AssertionError("test output too large");
            if (desired > values.length) {
                values = Arrays.copyOf(values, (int) Math.max(desired, (long) values.length * 2));
            }
        }
    }

    private static final class Lz4Builder {
        private final ByteArrayOutputStream body = new ByteArrayOutputStream();
        private final OutputOracle output = new OutputOracle();

        int size() { return output.size(); }
        byte[] toOutput() { return output.toByteArray(); }
        byte[] toCompressed() { return body.toByteArray(); }

        void sequence(byte[] literals, int distance, int match) {
            if (distance <= 0 || distance > size() + literals.length || match < 4) {
                throw new AssertionError("invalid generated LZ4 match");
            }
            token(literals.length, match - 4);
            body.writeBytes(literals);
            body.write(distance & 255);
            body.write(distance >>> 8);
            if (match - 4 >= 15) extension(match - 4 - 15);
            append(literals);
            repeat(distance, match);
        }

        void finish(byte[] literals) {
            token(literals.length, -1);
            body.writeBytes(literals);
            append(literals);
        }

        private void token(int literalCount, int matchMinusFour) {
            body.write((Math.min(literalCount, 15) << 4)
                    | (matchMinusFour < 0 ? 0 : Math.min(matchMinusFour, 15)));
            if (literalCount >= 15) extension(literalCount - 15);
        }

        private void extension(int count) {
            while (count >= 255) {
                body.write(255);
                count -= 255;
            }
            body.write(count);
        }

        private void append(byte[] bytes) { output.writeBytes(bytes); }

        private void repeat(int distance, int length) {
            output.repeat(distance, length);
        }
    }

    private static final class SnappyBuilder {
        private final ByteArrayOutputStream body = new ByteArrayOutputStream();
        private final OutputOracle output = new OutputOracle();

        int size() { return output.size(); }
        byte[] toOutput() { return output.toByteArray(); }

        byte[] toCompressed() {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            int size = size();
            while ((size & ~127) != 0) {
                result.write((size & 127) | 128);
                size >>>= 7;
            }
            result.write(size);
            result.writeBytes(body.toByteArray());
            return result.toByteArray();
        }

        void literal(byte[] bytes) {
            if (bytes.length == 0) throw new AssertionError("empty literal token");
            int lengthMinusOne = bytes.length - 1;
            if (bytes.length <= 60) {
                body.write(lengthMinusOne << 2);
            } else {
                int width = lengthMinusOne <= 255 ? 1 : lengthMinusOne <= 65535 ? 2 : 3;
                body.write((59 + width) << 2);
                for (int i = 0; i < width; i++) {
                    body.write((lengthMinusOne >>> (8 * i)) & 255);
                }
            }
            body.writeBytes(bytes);
            output.writeBytes(bytes);
        }

        void copy(int distance, int length, int kind) {
            if (distance <= 0 || distance > size() || length <= 0) {
                throw new AssertionError("invalid generated Snappy copy");
            }
            if (kind == 1) {
                if (distance > 2047 || length < 4 || length > 11) {
                    throw new AssertionError("invalid generated Snappy copy1");
                }
                body.write(((distance >>> 8) << 5) | ((length - 4) << 2) | 1);
                body.write(distance & 255);
            } else if (kind == 2 || kind == 3) {
                body.write(((length - 1) << 2) | kind);
                body.write(distance & 255);
                body.write((distance >>> 8) & 255);
                if (kind == 3) {
                    body.write((distance >>> 16) & 255);
                    body.write((distance >>> 24) & 255);
                }
            } else throw new AssertionError("unknown Snappy copy kind");
            output.repeat(distance, length);
        }
    }
}
