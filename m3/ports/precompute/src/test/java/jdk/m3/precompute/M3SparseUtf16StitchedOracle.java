// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Finite, deterministic long-input UTF-16 boundary oracle for original DFA + JNI. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/ca1aafc4d94f9dd1ee288226dcca070cc60991c9/synexia-openrewrite-recipes/recipe-crates/m3-utf16-stitch-20261022/src/test/java/com/synexia/m3/mastery/M3SparseUtf16StitchEvidence.java blob 5469b24af16c; recipe m3/ports/precompute/recipe/utf16-stitch-20261106/relocate.py v3
public final class M3SparseUtf16StitchedOracle {
    private static final int TRIALS = 256;
    private static final int STEP = 16;
    private static final String ALPHABET = "ab/*?[]{}\u0000caf\u00e9\u0915\u4e16\uD83D\uDE00";
    private static final List<String> SEAMS = List.of("a", "ab", "baa", "\u0915", "\u00e9a",
            "\uD83D", "\uDE00", "\uD83D\uDE00", "/*", "[a]", "a".repeat(63));

    private M3SparseUtf16StitchedOracle() { }
    record Batch(String witness, long checks, long crossings) { }
    public record Report(int trials, long checks, long crossings, String sha256) { }

    private static Batch trial(int start, int end, boolean nativeRequired) {
        var witness = new StringBuilder();
        long comparisons = 0, crossings = 0;
        for (int id = start; id < end; id++) {
            SplittableRandom r = new SplittableRandom(0x5117_5eaf_2026_1022L + id);
            int size = switch (id % 12) {
                case 0 -> 0;
                case 1 -> 1;
                case 2 -> 62;
                case 3 -> 63;
                case 4 -> 4033;
                case 5 -> 4034;
                case 6 -> 4035;
                case 7 -> 4096;
                case 8 -> 8067;
                case 9 -> 8068;
                case 10 -> 12034;
                default -> 17000;
            };
            char[] text = new char[size];
            for (int i = 0; i < text.length; i++) {
                text[i] = ALPHABET.charAt(r.nextInt(ALPHABET.length()));
            }
            // Place a guaranteed 63-unit match over the first physical seam.
            // The seeded corpus also includes independently discovered matches.
            if (size >= 4096 && (id & 1) == 0) {
                Arrays.fill(text, 4034 - 20, 4034 - 20 + 63, 'a');
            }
            List<String> needles = new ArrayList<>(SEAMS);
            if (size > 0) {
                int from = r.nextInt(size);
                int length = Math.min(63, size - from);
                if (length > 0) needles.add(new String(text, from, length));
            }
            var plan = M3SparseUtf16LiteralPlan.compile(needles, 0);
            // Two distinct logical regions verify nonzero physical source offsets.
            for (int mode = 0; mode < 2; mode++) {
                int from = mode == 0 ? 0 : (size > 11 ? 7 : 0);
                int to = mode == 0 ? size : (size > 13 ? size - 3 : size);
                int[] actual = nativeRequired
                        ? M3SparseUtf16Stitched.firstNative(plan, text, from, to)
                        : M3SparseUtf16Stitched.firstJava(plan, text, from, to);
                int[] independent = M3SparseUtf16Stitched.firstJava(plan, text, from, to);
                String original = new String(text);
                for (int p = 0; p < needles.size(); p++) {
                    String word = needles.get(p);
                    int relative = original.substring(from, to).indexOf(word);
                    int expected = relative < 0 ? -1 : from + relative;
                    if (actual[p] != expected || independent[p] != expected) {
                        throw new AssertionError("STITCH_JDK_DRIFT " + id + '/' + mode + '/'
                                + p + " wanted=" + expected + " got=" + actual[p]);
                    }
                    // Count actual first hits touching either side of a chunk seam.
                    if (expected >= 0 && expected < from + 4034 && expected + word.length() > from + 4034) {
                        crossings++;
                    }
                    comparisons += nativeRequired ? 3 : 2;
                    witness.append(id).append(':').append(mode).append(':').append(p)
                            .append('=').append(expected).append(';');
                }
                witness.append('\n');
            }
        }
        return new Batch(witness.toString(), comparisons, crossings);
    }

    public static Report verify(int workers, boolean nativeRequired) throws Exception {
        if (workers < 1 || workers > 4) throw new IllegalArgumentException("workers 1..4");
        if (nativeRequired && !M3SparseUtf16LiteralPlan.nativeAvailable()) {
            throw new UnsatisfiedLinkError("sparse UTF-16 JNI explicitly required");
        }
        var futures = new ArrayList<Future<Batch>>();
        try (var pool = Executors.newFixedThreadPool(workers)) {
            for (int i = 0; i < TRIALS; i += STEP) {
                int from = i, to = Math.min(TRIALS, i + STEP);
                Callable<Batch> job = () -> trial(from, to, nativeRequired);
                futures.add(pool.submit(job));
            }
            MessageDigest root = digest();
            long checks = 0, crossings = 0;
            for (Future<Batch> item : futures) {
                Batch row = item.get();
                root.update(row.witness().getBytes(StandardCharsets.UTF_8));
                checks += row.checks();
                crossings += row.crossings();
            }
            return new Report(TRIALS, checks, crossings, HexFormat.of().formatHex(root.digest()));
        }
    }

    /**
     * An early hit for every literal is irrevocable once the next physical
     * window starts beyond it. If even one literal is absent, later native
     * batches must still be inspected; never infer absence from a prefix.
     */
    public static void verifyFrontier(boolean nativeRequired) {
        char[] text = new char[4034 * 66 + 200];
        Arrays.fill(text, 'z');
        String longNeedle = "a".repeat(63);
        var terms = List.of("a", "bc", longNeedle);
        var plan = M3SparseUtf16LiteralPlan.compile(terms, 0);
        Arrays.fill(text, 21, 84, 'a');
        text[100] = 'b';
        text[101] = 'c';
        int[] wanted = {21, 100, 21};
        int[] found = nativeRequired
                ? M3SparseUtf16Stitched.firstNative(plan, text, 0, text.length)
                : M3SparseUtf16Stitched.firstJava(plan, text, 0, text.length);
        if (!Arrays.equals(wanted, found)) {
            throw new AssertionError("FRONTIER_EARLY_FIRST_INDEX_DRIFT");
        }
        // A missing third literal must force continuation beyond the first
        // native batch even when the other two are already known.
        Arrays.fill(text, 21, 84, 'z');
        text[21] = 'a';
        int late = 4034 * 64 + 9;
        Arrays.fill(text, late, late + 63, 'a');
        wanted = new int[] {21, 100, late};
        found = nativeRequired
                ? M3SparseUtf16Stitched.firstNative(plan, text, 0, text.length)
                : M3SparseUtf16Stitched.firstJava(plan, text, 0, text.length);
        if (!Arrays.equals(wanted, found)) {
            throw new AssertionError("FRONTIER_LATE_BATCH_WITNESS_DRIFT");
        }
    }

    public static void verifyRefusals(boolean nativeRequired) {
        verifyFrontier(nativeRequired);
        var plan = M3SparseUtf16LiteralPlan.compile(List.of("a".repeat(63), "\uD83D", "\uDE00"), 0);
        char[] seam = new char[8100];
        Arrays.fill(seam, 'b');
        Arrays.fill(seam, 4010, 4073, 'a');
        int[] expected = {4010, -1, -1};
        if (!Arrays.equals(expected, M3SparseUtf16Stitched.firstJava(plan, seam, 0, seam.length))) {
            throw new AssertionError("63-unit match crossing a 4034-unit chunk seam was lost");
        }
        if (nativeRequired && !Arrays.equals(expected,
                M3SparseUtf16Stitched.firstNative(plan, seam, 0, seam.length))) {
            throw new AssertionError("native 63-unit cross-window match was lost");
        }
        for (Runnable bad : List.<Runnable>of(
                () -> M3SparseUtf16Stitched.firstJava(plan, null, 0, 0),
                () -> M3SparseUtf16Stitched.firstJava(plan, seam, -1, 12),
                () -> M3SparseUtf16Stitched.firstJava(plan, seam, 2, seam.length + 1),
                () -> M3SparseUtf16Stitched.windowCount(-1),
                () -> M3SparseUtf16Stitched.windowCount(16_523_265))) {
            try { bad.run(); throw new AssertionError("invalid stitched region admitted"); }
            catch (NullPointerException | IndexOutOfBoundsException | IllegalArgumentException expectedFailure) {
                // Required fail-closed geometry checks.
            }
        }
        if (M3SparseUtf16Stitched.windowCount(0) != 1
                || M3SparseUtf16Stitched.windowCount(16_523_264) != 4096) {
            throw new AssertionError("bounded window arithmetic incorrect");
        }
        // 65 windows requires a second original JNI batch, not a new symbol.
        char[] beyond = new char[4034 * 65 + 80];
        Arrays.fill(beyond, 'b');
        int hit = 4034 * 64 - 30;
        Arrays.fill(beyond, hit, hit + 63, 'a');
        int expectedAcrossBatch = new String(beyond).indexOf("a".repeat(63));
        if (expectedAcrossBatch != hit) throw new AssertionError("bad independent seam fixture");
        if (M3SparseUtf16Stitched.firstJava(plan, beyond, 0, beyond.length)[0] != hit) {
            throw new AssertionError("Java scan lost match crossing 64-window batch boundary");
        }
        if (nativeRequired
                && M3SparseUtf16Stitched.firstNative(plan, beyond, 0, beyond.length)[0] != hit) {
            throw new AssertionError("native scan lost match crossing 64-window batch boundary");
        }
    }

    private static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException fail) { throw new AssertionError(fail); }
    }

    public static void main(String[] args) throws Exception {
        boolean required = Arrays.asList(args).contains("--native-required");
        verifyRefusals(required);
        Report one = verify(1, required);
        if (one.crossings() == 0) throw new AssertionError("seam witness absent");
        for (int w : List.of(2, 4)) {
            if (!one.equals(verify(w, required))) throw new AssertionError("STITCH_FAN_IN_DRIFT:" + w);
        }
        System.out.println("M3_UTF16_FRONTIER_PASS early_all_and_late_missing native=" + required);
        System.out.println("M3_UTF16_STITCH_PASS cases=" + one.trials()
                + " checked=" + one.checks() + " seamMatches=" + one.crossings()
                + " native=" + required + " sha256=" + one.sha256());
    }
}
