// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Copyright 2026 Hitesh Soliwal and contributors

import java.util.Arrays;
import java.util.Random;
import java.util.regex.Pattern;

/** Test binding to the complete production C entry. No native algorithm is reimplemented here. */
public final class RegexDpNativeHarness {
    private static long checks;
    private static long digest;
    private static volatile long sink;

    static {
        System.loadLibrary("m3regexproof");
    }

    private RegexDpNativeHarness() {}

    private static native long run(byte[] bytes, int capacity, int start, int length,
            int width, boolean bigEndian, long incoming, int[] atoms, long[] closure,
            int addressMode);

    private static byte[] encode(String text, int width, boolean bigEndian) {
        byte[] bytes = new byte[text.length() * width];
        for (int at = 0; at < text.length(); at++) {
            char unit = text.charAt(at);
            if (width == 1) {
                if (unit > 255) throw new IllegalArgumentException("fixture Latin1 range");
                bytes[at] = (byte) unit;
            } else {
                bytes[at * 2] = (byte) (bigEndian ? unit >>> 8 : unit);
                bytes[at * 2 + 1] = (byte) (bigEndian ? unit : unit >>> 8);
            }
        }
        return bytes;
    }

    private static void equal(long actual, long expected, String label) {
        checks++;
        if (actual != expected) {
            throw new AssertionError(label + " actual=" + actual + " expected=" + expected);
        }
        digest = Long.rotateLeft(digest, 7) ^ actual;
    }

    private static void refuse(String expected, Runnable invocation) {
        checks++;
        try {
            invocation.run();
            throw new AssertionError("expected IllegalArgumentException: " + expected);
        } catch (IllegalArgumentException exception) {
            if (!expected.equals(exception.getMessage())) {
                throw new AssertionError("refusal order expected=" + expected
                        + " actual=" + exception.getMessage(), exception);
            }
            digest = Long.rotateLeft(digest, 7) ^ expected.hashCode();
        }
    }

    private static void exhaustivePackets() {
        long packets = 0;
        for (int count = 1; count <= 7; count++) {
            for (long stars = 0; stars < (1L << count); stars++) {
                int[] atoms = RegexGraph.atoms(count, stars, 2);
                long[] closure = RegexGraph.closures(atoms);
                for (long bits = 0; bits < (1L << (count + 1)); bits++) {
                    final long incoming = bits;
                    if (RegexGraph.close(bits, stars) == bits) {
                        equal(run(null, 0, 0, 0, 1, false, bits, atoms, closure, 1),
                                bits, "closed packet");
                    } else {
                        refuse("incoming frontier is not closed",
                                () -> run(null, 0, 0, 0, 1, false, incoming,
                                        atoms, closure, 1));
                    }
                    packets++;
                }
            }
        }
        equal(packets, 43_688, "exhaustive packet count");
    }

    private static void malformedPackets() {
        int[] atom = {'a' | RegexGraph.STAR};
        long[] closure = {3, 2};
        refuse("missing regex plan",
                () -> run(null, -1, -1, -1, 3, false, -1, null, null, 1));
        refuse("missing regex plan",
                () -> run(null, 0, 0, 0, 1, false, 3, atom, null, 1));
        refuse("invalid regex plan shape",
                () -> run(null, -1, 0, 0, 1, false, 0, new int[0], new long[1], 1));
        refuse("invalid regex plan shape",
                () -> run(null, 0, 0, 0, 1, false, 0, new int[33], new long[34], 1));
        refuse("invalid regex plan shape",
                () -> run(null, 0, 0, 0, 1, false, 3, atom, new long[1], 1));
        int[][] ranges = {
                {-1, 0, 0, 1}, {1, -1, 0, 1}, {1, 2, 0, 1},
                {1, 0, -1, 1}, {1, 0, 2, 1}, {8193, 0, 8193, 1},
                {0, 0, 0, 0}, {0, 0, 0, 3},
                {Integer.MAX_VALUE, Integer.MAX_VALUE, 1, 1}
        };
        for (int[] range : ranges) {
            refuse("invalid canonical regex range",
                    () -> run(null, range[0], range[1], range[2], range[3],
                            false, -1, atom, closure, 1));
        }
        refuse("invalid incoming regex frontier",
                () -> run(null, 0, 0, 0, 1, false, -1, atom, closure, 1));
        refuse("invalid incoming regex frontier",
                () -> run(null, 0, 0, 0, 1, false, 4, atom, closure, 1));
        refuse("invalid accepting closure",
                () -> run(null, 0, 0, 0, 1, false, 3, atom, new long[]{3, 0}, 1));
        refuse("invalid accepting closure",
                () -> run(null, 0, 0, 0, 1, false, 3, new int[]{'+'}, new long[]{0, 0}, 1));
        refuse("invalid regex opcode",
                () -> run(null, 0, 0, 0, 1, false, 3, new int[]{'+'}, closure, 1));
        refuse("invalid regex opcode",
                () -> run(null, 0, 0, 0, 1, false, 3,
                        new int[]{'a' | (1 << 17)}, closure, 1));
        refuse("invalid epsilon closure",
                () -> run(null, 0, 0, 0, 1, false, 3, atom, new long[]{1, 2}, 1));
        refuse("incoming frontier is not closed",
                () -> run(null, 0, 0, 0, 1, false, 1, atom, closure, 2));
        equal(run(null, 0, 0, 0, 1, false, 3, atom, closure, 1),
                3, "empty ignores zero address");
        equal(run(null, 0, 0, 0, 2, true, 3, atom, closure, 2),
                3, "empty ignores negative address");
        refuse("invalid native regex address",
                () -> run(null, 1, 0, 1, 1, false, 3, atom, closure, 1));
        refuse("invalid native regex address",
                () -> run(null, 1, 0, 1, 2, true, 3, atom, closure, 2));
        int[] plain = {'a'};
        long[] plainClosure = {1, 2};
        equal(run(new byte[]{'a'}, 1, 0, 1, 1, false, 1, plain, plainClosure, 0),
                2, "native zero-star plan remains valid");
        int[] thirtyTwo = RegexGraph.atoms(32, 0xffff_ffffL, 0);
        long[] thirtyTwoClosure = RegexGraph.closures(thirtyTwo);
        equal(run(null, 0, 0, 0, 1, false, 1L << 32,
                        thirtyTwo, thirtyTwoClosure, 1),
                1L << 32, "accepting bit32");
        refuse("invalid incoming regex frontier",
                () -> run(null, 0, 0, 0, 1, false, 1L << 33,
                        thirtyTwo, thirtyTwoClosure, 1));
    }

    private static String expression(int[] atoms) {
        StringBuilder regex = new StringBuilder();
        for (int atom : atoms) {
            regex.append((char) atom);
            if ((atom & RegexGraph.STAR) != 0) regex.append('*');
        }
        return regex.toString();
    }

    private static void execution(String body, int[] atoms, long seeds, boolean split) {
        long incoming = RegexGraph.close(seeds, RegexGraph.stars(atoms));
        long[] closures = RegexGraph.closures(atoms);
        String padded = "Q!" + body + "?R";
        long expected = RegexGraph.advance(padded, 2, body.length(), incoming, atoms);
        boolean latin1 = padded.chars().allMatch(unit -> unit <= 255);
        for (int format = latin1 ? 0 : 1; format < 3; format++) {
            int width = format == 0 ? 1 : 2;
            boolean bigEndian = format == 2;
            byte[] bytes = encode(padded, width, bigEndian);
            long actual = run(bytes, padded.length(), 2, body.length(), width,
                    bigEndian, incoming, atoms, closures, 0);
            equal(actual, expected, "actual production C scan");
            if (split) {
                for (int cut = 0; cut <= body.length(); cut++) {
                    long left = run(bytes, padded.length(), 2, cut, width,
                            bigEndian, incoming, atoms, closures, 0);
                    long combined = left < 0 ? left : run(bytes, padded.length(), 2 + cut,
                            body.length() - cut, width, bigEndian, left, atoms, closures, 0);
                    equal(combined, expected, "continuous chunk frontier");
                }
            }
        }
        if (incoming == closures[0] && expected >= 0) {
            boolean matched = (expected & (1L << atoms.length)) != 0;
            equal(matched ? 1 : 0, Pattern.compile(expression(atoms)).matcher(body).matches()
                    ? 1 : 0, "JDK Pattern oracle");
        }
    }

    private static void runtime() {
        String[] inputs = {"", "a", "b", "aaab", "abab", ".", "\0", "\n", "\r",
                "\u0085", "\u2028", "\u2029", "\u0100", "a\ud800", "b\udfff",
                "\ud83d\ude42", "a".repeat(255), "a".repeat(256), "a".repeat(8192)};
        for (int count : new int[]{1, 2, 7, 8, 9, 16, 31, 32}) {
            long valid = (1L << count) - 1;
            for (long stars : new long[]{0, valid, valid & 0xaaaa_aaaaL}) {
                for (int style = 0; style < 3; style++) {
                    int[] atoms = RegexGraph.atoms(count, stars, style);
                    for (String input : inputs) {
                        execution(input, atoms, 1, input.length() <= 8);
                        execution(input, atoms, 0, false);
                        execution(input, atoms, (1L << count) | 1, false);
                    }
                }
            }
        }
        Random random = new Random(0x4d334a4e494450L);
        char[] alphabet = {'a', 'b', 'c', 'A', '0', '\0', '\n', '\r',
                '\u0085', '\u0100', '\u2028', '\u2029', '\ud800', '\udfff'};
        for (int trial = 0; trial < 2_048; trial++) {
            int count = 1 + random.nextInt(32);
            long stars = Integer.toUnsignedLong(random.nextInt()) & ((1L << count) - 1);
            int[] atoms = RegexGraph.atoms(count, stars, random.nextInt(3));
            char[] units = new char[random.nextInt(65)];
            for (int at = 0; at < units.length; at++) {
                units[at] = alphabet[random.nextInt(alphabet.length)];
            }
            long seeds = random.nextLong() & ((1L << (count + 1)) - 1);
            execution(new String(units), atoms, seeds, trial < 64);
        }
    }

    private static long measured(byte[] bytes, int length, int[] atoms, long[] closure,
                                 long incoming, int repetitions) {
        long answer = 0;
        for (int at = 0; at < repetitions; at++) {
            answer ^= run(bytes, length, 0, length, 1, false, incoming,
                    atoms, closure, length == 0 ? 1 : 0);
        }
        sink ^= answer;
        return answer;
    }

    private static void benchmark() {
        for (String scenario : new String[]{"validation", "small", "sparse", "dense"}) {
            int count = scenario.equals("small") ? 7 : 32;
            int length = scenario.equals("validation") ? 0 : 256;
            long stars = scenario.equals("sparse") ? 0 : (1L << count) - 1;
            int[] atoms = RegexGraph.atoms(count, stars, 0);
            long[] closures = RegexGraph.closures(atoms);
            long incoming = closures[0];
            byte[] bytes = new byte[length];
            Arrays.fill(bytes, (byte) 'a');
            long start = System.nanoTime();
            measured(bytes, length, atoms, closures, incoming, 1);
            long first = System.nanoTime() - start;
            measured(bytes, length, atoms, closures, incoming, 2_000);
            long[] samples = new long[7];
            for (int sample = 0; sample < samples.length; sample++) {
                start = System.nanoTime();
                measured(bytes, length, atoms, closures, incoming, 5_000);
                samples[sample] = System.nanoTime() - start;
            }
            Arrays.sort(samples);
            System.out.println("M3_REGEX_NATIVE_BENCH scenario=" + scenario
                    + " first_ns=" + first + " median_ns_per_call=" + samples[3] / 5_000.0
                    + " min_ns_per_call=" + samples[0] / 5_000.0
                    + " fixture_buffer_borrow_included=true sink=" + sink);
        }
    }

    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("benchmark")) {
            benchmark();
            return;
        }
        if (args.length != 0) throw new IllegalArgumentException("no args or benchmark");
        exhaustivePackets();
        malformedPackets();
        runtime();
        System.out.println("M3_REGEX_NATIVE_PROOF PASS checks=" + checks
                + " digest=" + Long.toUnsignedString(digest, 16)
                + " actual_target_c=true matched_m3jdk=false");
    }
}
