/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * GPLv2 with the Classpath exception.
 * @test
 * @summary Receive Synexia regex v7 precomputed bytes without parser/runtime substitution
 * @modules java.base/jdk.internal.mindex
 * @run main/othervm -Xcheck:jni M3RX7CodecTest
 * @run main/othervm -Xint -Xcheck:jni M3RX7CodecTest
 */

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.regex.Pattern;
import jdk.internal.mindex.M3RX7;
import jdk.internal.mindex.M3RX7Codec;
import jdk.internal.mindex.M3RX7Image;

public class M3RX7CodecTest {
    private static final int MAGIC = 0x534e5852;
    private static final int V1 = 1;
    private static final int V2 = 2;
    private static final int V4 = 4;
    private static final int V5 = 5;
    private static final int V7 = 7;
    private static int checks;

    public static void main(String[] args) throws Exception {
        byte[] wire = v7("ab+", false, goodTransitions(), new byte[] {0, 0, 1});
        String root = sha256(wire);

        M3RX7Image.Precomputed decoded = M3RX7Codec.decodePrecomputed(wire);
        check(decoded.transportRoot().equals(root), "transport root");
        check(decoded.execution().program().expression().equals("ab+"), "expression");
        check(decoded.execution().program().semantics() == M3RX7Image.Semantics.JDK, "semantics");
        check(decoded.execution().program().stateCount() == 3, "state geometry");
        check(decoded.execution().program().classCount() == 2, "class geometry");
        check(decoded.execution().anchoredAsciiDfa() != null, "anchored DFA");
        check(decoded.searchDfa() == null, "search DFA intentionally absent");
        check(decoded.branchingStateCount() == 0, "dispatch facts");

        M3RX7 candidate = decoded.anchoredCandidate();
        check(candidate != null, "anchored candidate adapter");
        Pattern oracle = Pattern.compile("ab+");
        for (String value : new String[] {"", "a", "ab", "abb", "abbb", "ac", "xab"}) {
            boolean truth = oracle.matcher(value).matches();
            int result = candidate.matchesCandidate(value);
            check(result >= 0 && (result == 1) == truth, "candidate " + value);
            check(candidate.matchesVerified(value, oracle) == truth, "verified " + value);
        }

        // The decoded image owns independent primitive snapshots, not the input byte array.
        short[] transitions = decoded.execution().anchoredAsciiDfa().transitions();
        transitions['a'] = -1;
        wire[wire.length - 1] ^= 1;
        check(candidate.matchesCandidate("abbb") == 1, "defensive DFA copy");
        check(decoded.execution().anchoredAsciiDfa().transitions()['a'] == 1,
                "DFA accessor defensive copy");

        // A direct/read-only buffer may start at a nonzero position; decode must not move it.
        byte[] fresh = v7("ab+", false, goodTransitions(), new byte[] {0, 0, 1});
        ByteBuffer direct = ByteBuffer.allocateDirect(fresh.length + 11).order(ByteOrder.LITTLE_ENDIAN);
        direct.position(7);
        direct.put(fresh);
        direct.flip();
        direct.position(7);
        int position = direct.position();
        int limit = direct.limit();
        M3RX7Image.Precomputed directDecoded =
                M3RX7Codec.decodePrecomputed(direct.asReadOnlyBuffer());
        check(direct.position() == position && direct.limit() == limit, "caller position/limit");
        check(directDecoded.transportRoot().equals(sha256(fresh)), "direct transport root");

        // Raw UTF-16 expression metadata must preserve lone surrogate units exactly.
        String lone = new String(new char[] {(char) 0xD800});
        byte[] utf16 = v7(lone, true, goodTransitions(), new byte[] {0, 0, 1});
        M3RX7Image.Precomputed utf16Decoded = M3RX7Codec.decodePrecomputed(utf16);
        check(utf16Decoded.execution().program().expression().equals(lone), "raw UTF16 metadata");

        // A validly checksummed but forged DFA remains only a candidate; the JDK oracle detects it.
        byte[] forged = v7("ab+", false, goodTransitions(), new byte[] {0, 1, 0});
        M3RX7 forgedCandidate = M3RX7Codec.decodePrecomputed(forged).anchoredCandidate();
        expect(IllegalStateException.class, () -> forgedCandidate.matchesVerified("a", oracle));

        byte[] corrupt = fresh.clone();
        corrupt[corrupt.length - 1] ^= 1;
        expect(IllegalArgumentException.class, () -> M3RX7Codec.decodePrecomputed(corrupt));

        byte[] wrongVersion = fresh.clone();
        ByteBuffer.wrap(wrongVersion).order(ByteOrder.BIG_ENDIAN).putInt(4, 6);
        expect(IllegalArgumentException.class, () -> M3RX7Codec.decodePrecomputed(wrongVersion));

        for (int length = 0; length < Math.min(64, fresh.length); length++) {
            byte[] truncated = Arrays.copyOf(fresh, length);
            expect(IllegalArgumentException.class, () -> M3RX7Codec.decodePrecomputed(truncated));
        }

        short[] malformedTransitions = new short[] {1};
        byte[] malformedDfa = v7("ab+", false, malformedTransitions, new byte[] {0, 0, 1});
        expect(IllegalArgumentException.class, () -> M3RX7Codec.decodePrecomputed(malformedDfa));

        byte[] familyMismatch =
                v7WithNestedVersions("ab+", false, V4, V2, goodTransitions(), new byte[] {0, 0, 1});
        expect(IllegalArgumentException.class, () -> M3RX7Codec.decodePrecomputed(familyMismatch));

        System.out.println("M3RX7CodecTest checks=" + checks + " PASS");
    }

    private static short[] goodTransitions() {
        short[] transitions = new short[3 * 128];
        Arrays.fill(transitions, (short) -1);
        transitions['a'] = 1;
        transitions[1 * 128 + 'b'] = 2;
        transitions[2 * 128 + 'b'] = 2;
        return transitions;
    }

    private static byte[] v7(
            String expression, boolean utf16, short[] transitions, byte[] accepting)
            throws Exception {
        return v7WithNestedVersions(
                expression,
                utf16,
                utf16 ? V4 : V1,
                utf16 ? V5 : V2,
                transitions,
                accepting);
    }

    private static byte[] v7WithNestedVersions(
            String expression,
            boolean utf16,
            int baseVersion,
            int executionVersion,
            short[] transitions,
            byte[] accepting)
            throws Exception {
        byte[] program = envelope(baseVersion, programPayload(expression, utf16));
        byte[] execution =
                envelope(
                        executionVersion,
                        executionPayload(program, transitions, accepting));

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(execution.length);
            out.write(execution);
            out.writeLong(0L);
            out.writeLong(1L << ('a' - 64));
            out.writeInt(0);
        }
        return envelope(V7, bytes.toByteArray());
    }

    private static byte[] programPayload(String expression, boolean utf16) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(1); // JDK semantics ordinal.
            if (utf16) {
                out.writeInt(expression.length());
                for (int index = 0; index < expression.length(); index++) {
                    out.writeChar(expression.charAt(index));
                }
            } else {
                byte[] text = expression.getBytes(StandardCharsets.UTF_8);
                out.writeInt(text.length);
                out.write(text);
            }

            out.writeInt(0); // start
            out.writeInt(2); // match
            out.writeInt(2); // min length
            out.writeInt(-1); // max length
            out.writeInt(0); // captures
            out.writeLong(0L); // required presence
            out.writeInt(0); // character flags
            out.writeBoolean(false); // surrogate consumption

            out.writeInt(3); // states
            out.writeInt(2); // classes
            out.writeInt(0); // ranges
            out.write(new byte[] {1, 1, 6}); // CHAR, CHAR, MATCH
            writeInts(out, new int[] {1, 2, -1});
            writeInts(out, new int[] {-1, -1, -1});
            writeInts(out, new int[] {0, 1, -1});
            writeLongs(out, new long[] {0L, 0L});
            writeLongs(
                    out,
                    new long[] {
                        1L << ('a' - 64),
                        1L << ('b' - 64)
                    });
            writeInts(out, new int[] {0, 0});
            writeInts(out, new int[] {0, 0});
        }
        return bytes.toByteArray();
    }

    private static byte[] executionPayload(
            byte[] program, short[] transitions, byte[] accepting)
            throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(program.length);
            out.write(program);
            countedLongs(out, new long[0]);
            out.writeInt(0); // epsilon words
            countedInts(out, new int[0]);
            countedLongs(out, new long[0]);
            out.writeInt(0); // subset bits
            out.writeInt(0); // subset source count
            countedLongs(out, new long[0]);
            out.writeInt(0); // BMP class words
            countedInts(out, new int[0]);
            countedLongs(out, new long[0]);
            writeDfa(out, 0, transitions, accepting);
            out.writeBoolean(false); // no search DFA in this bounded fixture
        }
        return bytes.toByteArray();
    }

    private static void writeDfa(
            DataOutputStream out, int start, short[] transitions, byte[] accepting)
            throws Exception {
        out.writeBoolean(true);
        out.writeInt(start);
        out.writeInt(transitions.length);
        for (short transition : transitions) out.writeShort(transition);
        out.writeInt(accepting.length);
        out.write(accepting);
    }

    private static void countedInts(DataOutputStream out, int[] values) throws Exception {
        out.writeInt(values.length);
        writeInts(out, values);
    }

    private static void countedLongs(DataOutputStream out, long[] values) throws Exception {
        out.writeInt(values.length);
        writeLongs(out, values);
    }

    private static void writeInts(DataOutputStream out, int[] values) throws Exception {
        for (int value : values) out.writeInt(value);
    }

    private static void writeLongs(DataOutputStream out, long[] values) throws Exception {
        for (long value : values) out.writeLong(value);
    }

    private static byte[] envelope(int version, byte[] payload) throws Exception {
        ByteBuffer encoded =
                ByteBuffer.allocate(12 + 32 + payload.length).order(ByteOrder.BIG_ENDIAN);
        encoded.putInt(MAGIC);
        encoded.putInt(version);
        encoded.putInt(payload.length);
        encoded.put(MessageDigest.getInstance("SHA-256").digest(payload));
        encoded.put(payload);
        return encoded.array();
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    private static void expect(Class<? extends Throwable> type, Throwing action) throws Exception {
        checks++;
        try {
            action.run();
        } catch (Throwable thrown) {
            if (type.isInstance(thrown)) return;
            throw new AssertionError("wrong refusal " + thrown, thrown);
        }
        throw new AssertionError("missing refusal " + type.getName());
    }

    @FunctionalInterface
    private interface Throwing {
        void run() throws Exception;
    }
}
