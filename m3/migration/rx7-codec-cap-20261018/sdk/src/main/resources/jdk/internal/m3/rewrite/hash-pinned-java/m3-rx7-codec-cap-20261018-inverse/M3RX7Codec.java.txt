// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.internal.mindex;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Strict receive-only decoder for Synexia regex precompute image version 7.
 *
 * <p>The wire format is owned by the canonical Synexia producer. M3JDK does not define a second
 * compiler or encoder here. All decoded arrays become defensive M3RX7Image snapshots and the
 * encoded buffer is never retained.</p>
 */
public final class M3RX7Codec {
    private static final int MAGIC = 0x534e5852; // SNXR
    private static final int PROGRAM_VERSION = 1;
    private static final int EXECUTION_VERSION = 2;
    private static final int UTF16_PROGRAM_VERSION = 4;
    private static final int UTF16_EXECUTION_VERSION = 5;
    private static final int PRECOMPUTED_VERSION = 7;

    private static final int DIGEST_BYTES = 32;
    private static final int HEADER_BYTES = Integer.BYTES * 3 + DIGEST_BYTES;
    private static final int MAX_PROGRAM_BYTES = 32 * 1024 * 1024;
    private static final int MAX_EXECUTION_BYTES = 64 * 1024 * 1024;
    private static final int MAX_EXPRESSION_BYTES = 1024 * 1024;
    private static final int MAX_STATES = 100_000;
    private static final int MAX_CLASSES = 65_536;
    private static final int MAX_RANGES = 1_000_000;

    private M3RX7Codec() {}

    /** Decode exact version-7 bytes after taking an independent bounded snapshot. */
    public static M3RX7Image.Precomputed decodePrecomputed(byte[] encoded) {
        byte[] bytes = Objects.requireNonNull(encoded, "encoded").clone();
        return decodePrecomputed(ByteBuffer.wrap(bytes));
    }

    /**
     * Decode version-7 bytes from the caller's current position to limit without changing either.
     *
     * <p>Heap, direct and read-only buffers are accepted. No reference to the supplied buffer
     * survives this call.</p>
     */
    public static M3RX7Image.Precomputed decodePrecomputed(ByteBuffer encoded) {
        ByteBuffer source = Objects.requireNonNull(encoded, "encoded").asReadOnlyBuffer();
        if (source.remaining() < HEADER_BYTES || source.remaining() > MAX_EXECUTION_BYTES) {
            throw new IllegalArgumentException("RX7 v7 image byte geometry");
        }
        String transportRoot = sha256Hex(source.asReadOnlyBuffer());

        ByteBuffer input = payload(source, PRECOMPUTED_VERSION, MAX_EXECUTION_BYTES);
        int executionLength =
                boundedCount(input, "nested execution image", MAX_EXECUTION_BYTES);
        requireRemaining(input, executionLength, "nested execution image");
        ByteBuffer executionBytes = input.slice().order(ByteOrder.BIG_ENDIAN);
        executionBytes.limit(executionLength);
        input.position(Math.addExact(input.position(), executionLength));

        M3RX7Image.Execution execution = decodeExecution(executionBytes);
        long firstAsciiLow = getLong(input, "first ASCII low");
        long firstAsciiHigh = getLong(input, "first ASCII high");
        int branchingStateCount = getInt(input, "branching state count");
        if (input.hasRemaining()) {
            throw new IllegalArgumentException("RX7 v7 trailing payload");
        }
        return new M3RX7Image.Precomputed(
                execution,
                firstAsciiLow,
                firstAsciiHigh,
                branchingStateCount,
                transportRoot);
    }

    private static M3RX7Image.Execution decodeExecution(ByteBuffer encoded) {
        int version =
                imageVersion(
                        encoded,
                        EXECUTION_VERSION,
                        UTF16_EXECUTION_VERSION);
        boolean utf16 = version == UTF16_EXECUTION_VERSION;
        ByteBuffer input = payload(encoded, version, MAX_EXECUTION_BYTES);

        int baseLength = boundedCount(input, "nested program image", MAX_PROGRAM_BYTES);
        requireRemaining(input, baseLength, "nested program image");
        ByteBuffer base = input.slice().order(ByteOrder.BIG_ENDIAN);
        base.limit(baseLength);
        input.position(Math.addExact(input.position(), baseLength));
        requireNestedVersion(
                base,
                utf16 ? UTF16_PROGRAM_VERSION : PROGRAM_VERSION);
        M3RX7Image.Program program = decodeProgram(base);

        long[] asciiStateMasks = readCountedLongs(input, "execution ASCII state masks");
        int epsilonWordsPerRow = getInt(input, "execution epsilon words");
        int[] epsilonRowByState = readCountedInts(input, "execution epsilon rows");
        long[] epsilonTerminals = readCountedLongs(input, "execution epsilon terminals");
        int asciiSubsetBits = getInt(input, "execution ASCII subset bits");
        int asciiSubsetSourceCount = getInt(input, "execution ASCII subset sources");
        long[] asciiSubsetTransitions =
                readCountedLongs(input, "execution ASCII subset transitions");
        int bmpClassWordCount = getInt(input, "execution BMP class words");
        int[] bmpPageByHighByte = readCountedInts(input, "execution BMP pages");
        long[] bmpClassMasks = readCountedLongs(input, "execution BMP masks");
        M3RX7Image.Dfa anchored = readDfa(input, "anchored");
        M3RX7Image.Dfa search = readDfa(input, "search");
        if (input.hasRemaining()) {
            throw new IllegalArgumentException("RX7 execution trailing payload");
        }

        return new M3RX7Image.Execution(
                program,
                asciiStateMasks,
                epsilonWordsPerRow,
                epsilonRowByState,
                epsilonTerminals,
                asciiSubsetBits,
                asciiSubsetSourceCount,
                asciiSubsetTransitions,
                bmpClassWordCount,
                bmpPageByHighByte,
                bmpClassMasks,
                anchored,
                search);
    }

    private static M3RX7Image.Program decodeProgram(ByteBuffer encoded) {
        int version = imageVersion(encoded, PROGRAM_VERSION, UTF16_PROGRAM_VERSION);
        boolean utf16 = version == UTF16_PROGRAM_VERSION;
        ByteBuffer input = payload(encoded, version, MAX_PROGRAM_BYTES);

        int semanticsOrdinal = getInt(input, "semantics");
        M3RX7Image.Semantics[] semantics = M3RX7Image.Semantics.values();
        if (semanticsOrdinal < 0 || semanticsOrdinal >= semantics.length) {
            throw new IllegalArgumentException("RX7 semantics");
        }

        String expression = readExpression(input, utf16);
        int startState = getInt(input, "start state");
        int matchState = getInt(input, "match state");
        int minLength = getInt(input, "minimum length");
        int maxLength = getInt(input, "maximum length");
        int captureCount = getInt(input, "capture count");
        long requiredPresence64 = getLong(input, "presence");
        int requiredCharacterFlags = getInt(input, "character flags");
        requireRemaining(input, 1, "surrogate flag");
        byte surrogate = input.get();
        if (surrogate != 0 && surrogate != 1) {
            throw new IllegalArgumentException("RX7 surrogate flag");
        }

        int states = boundedCount(input, "states", MAX_STATES);
        int classes = boundedCount(input, "classes", MAX_CLASSES);
        int ranges = boundedCount(input, "ranges", MAX_RANGES);

        requireRemaining(input, states, "opcodes");
        byte[] op = new byte[states];
        input.get(op);
        int[] out1 = readInts(input, states, "out1");
        int[] out2 = readInts(input, states, "out2");
        int[] classId = readInts(input, states, "class ids");
        long[] asciiLow = readLongs(input, classes, "ASCII low");
        long[] asciiHigh = readLongs(input, classes, "ASCII high");
        int[] rangeOffset = readInts(input, classes, "range offsets");
        int[] rangeCount = readInts(input, classes, "range counts");
        char[] rangeLow = readChars(input, ranges, "range low");
        char[] rangeHigh = readChars(input, ranges, "range high");
        if (input.hasRemaining()) {
            throw new IllegalArgumentException("RX7 program trailing payload");
        }

        return new M3RX7Image.Program(
                expression,
                semantics[semanticsOrdinal],
                op,
                out1,
                out2,
                classId,
                asciiLow,
                asciiHigh,
                rangeOffset,
                rangeCount,
                rangeLow,
                rangeHigh,
                startState,
                matchState,
                minLength,
                maxLength,
                captureCount,
                requiredPresence64,
                requiredCharacterFlags,
                surrogate != 0);
    }

    private static M3RX7Image.Dfa readDfa(ByteBuffer input, String lane) {
        requireRemaining(input, 1, lane + " DFA presence");
        byte present = input.get();
        if (present == 0) return null;
        if (present != 1) {
            throw new IllegalArgumentException("RX7 DFA presence");
        }

        int start = getInt(input, lane + " DFA start");
        int transitionCount =
                boundedCount(
                        input,
                        lane + " DFA transitions",
                        MAX_EXECUTION_BYTES / Short.BYTES);
        requireRemaining(
                input,
                Math.multiplyExact(transitionCount, Short.BYTES),
                lane + " DFA transitions");
        short[] transitions = new short[transitionCount];
        for (int index = 0; index < transitionCount; index++) {
            transitions[index] = input.getShort();
        }

        int acceptingCount =
                boundedCount(input, lane + " DFA accepting", MAX_EXECUTION_BYTES);
        requireRemaining(input, acceptingCount, lane + " DFA accepting");
        byte[] accepting = new byte[acceptingCount];
        input.get(accepting);
        return new M3RX7Image.Dfa(start, transitions, accepting);
    }

    private static String readExpression(ByteBuffer input, boolean utf16) {
        if (utf16) {
            int units =
                    boundedCount(
                            input,
                            "expression UTF-16",
                            MAX_EXPRESSION_BYTES / Character.BYTES);
            return new String(readChars(input, units, "expression UTF-16"));
        }

        int length = boundedCount(input, "expression", MAX_EXPRESSION_BYTES);
        requireRemaining(input, length, "expression");
        byte[] bytes = new byte[length];
        input.get(bytes);
        String expression = new String(bytes, StandardCharsets.UTF_8);
        if (!Arrays.equals(expression.getBytes(StandardCharsets.UTF_8), bytes)) {
            throw new IllegalArgumentException("RX7 expression UTF-8");
        }
        return expression;
    }

    private static int imageVersion(ByteBuffer encoded, int legacy, int utf16) {
        ByteBuffer input =
                Objects.requireNonNull(encoded, "encoded")
                        .asReadOnlyBuffer()
                        .order(ByteOrder.BIG_ENDIAN);
        requireRemaining(input, Integer.BYTES * 2, "header");
        int version = input.getInt(input.position() + Integer.BYTES);
        if (version != legacy && version != utf16) {
            throw new IllegalArgumentException("RX7 image version");
        }
        return version;
    }

    private static void requireNestedVersion(ByteBuffer encoded, int expected) {
        imageVersion(encoded, expected, expected);
    }

    private static ByteBuffer payload(ByteBuffer encoded, int version, int maximumBytes) {
        ByteBuffer input =
                Objects.requireNonNull(encoded, "encoded")
                        .asReadOnlyBuffer()
                        .order(ByteOrder.BIG_ENDIAN);
        if (input.remaining() < HEADER_BYTES || input.remaining() > maximumBytes) {
            throw new IllegalArgumentException("RX7 image byte geometry");
        }
        if (input.getInt() != MAGIC) {
            throw new IllegalArgumentException("RX7 image magic");
        }
        if (input.getInt() != version) {
            throw new IllegalArgumentException("RX7 image version");
        }
        int payloadLength = input.getInt();
        if (payloadLength < 0 || payloadLength != input.remaining() - DIGEST_BYTES) {
            throw new IllegalArgumentException("RX7 image payload length");
        }

        byte[] expectedDigest = new byte[DIGEST_BYTES];
        input.get(expectedDigest);
        ByteBuffer result = input.slice().order(ByteOrder.BIG_ENDIAN);
        result.limit(payloadLength);
        if (!MessageDigest.isEqual(
                expectedDigest,
                sha256(result.asReadOnlyBuffer()))) {
            throw new IllegalArgumentException("RX7 image checksum");
        }
        return result;
    }

    private static int[] readCountedInts(ByteBuffer input, String lane) {
        int count =
                boundedCount(
                        input,
                        lane,
                        MAX_EXECUTION_BYTES / Integer.BYTES);
        return readInts(input, count, lane);
    }

    private static long[] readCountedLongs(ByteBuffer input, String lane) {
        int count =
                boundedCount(
                        input,
                        lane,
                        MAX_EXECUTION_BYTES / Long.BYTES);
        return readLongs(input, count, lane);
    }

    private static int[] readInts(ByteBuffer input, int count, String lane) {
        requireRemaining(input, Math.multiplyExact(count, Integer.BYTES), lane);
        int[] result = new int[count];
        for (int index = 0; index < count; index++) {
            result[index] = input.getInt();
        }
        return result;
    }

    private static long[] readLongs(ByteBuffer input, int count, String lane) {
        requireRemaining(input, Math.multiplyExact(count, Long.BYTES), lane);
        long[] result = new long[count];
        for (int index = 0; index < count; index++) {
            result[index] = input.getLong();
        }
        return result;
    }

    private static char[] readChars(ByteBuffer input, int count, String lane) {
        requireRemaining(input, Math.multiplyExact(count, Character.BYTES), lane);
        char[] result = new char[count];
        for (int index = 0; index < count; index++) {
            result[index] = input.getChar();
        }
        return result;
    }

    private static int boundedCount(ByteBuffer input, String lane, int maximum) {
        int value = getInt(input, lane);
        if (value < 0 || value > maximum) {
            throw new IllegalArgumentException("RX7 " + lane + " count");
        }
        return value;
    }

    private static int getInt(ByteBuffer input, String lane) {
        requireRemaining(input, Integer.BYTES, lane);
        return input.getInt();
    }

    private static long getLong(ByteBuffer input, String lane) {
        requireRemaining(input, Long.BYTES, lane);
        return input.getLong();
    }

    private static void requireRemaining(ByteBuffer input, int bytes, String lane) {
        if (bytes < 0 || input.remaining() < bytes) {
            throw new IllegalArgumentException("truncated RX7 " + lane);
        }
    }

    private static String sha256Hex(ByteBuffer bytes) {
        return HexFormat.of().formatHex(sha256(bytes));
    }

    private static byte[] sha256(ByteBuffer bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(bytes);
            return digest.digest();
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }
}
