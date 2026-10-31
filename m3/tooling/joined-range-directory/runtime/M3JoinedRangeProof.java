/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
import com.m3.text.LocalM3Arena;
import com.m3.text.LocalM3StringPiece;
import com.m3.text.M3StringPiece;
import com.m3.text.StorageIdentity;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Identical public-contract and allocation workload for original and generated joined ranges. */
public final class M3JoinedRangeProof {
    private static final MessageDigest DIGEST = digest();
    private static final Field DIRECTORY = directory();
    private static long cases;
    private static long units;
    private static volatile M3StringPiece sink;

    private M3JoinedRangeProof() { }

    private static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (java.security.NoSuchAlgorithmException failure) { throw new AssertionError(failure); }
    }
    private static Field directory() {
        try {
            Field field = Class.forName("com.m3.text.JoinedM3StringPiece").getDeclaredField("leaves");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static LocalM3StringPiece[] leaves(M3StringPiece piece) {
        if (piece instanceof LocalM3StringPiece local) return new LocalM3StringPiece[]{local};
        try { return (LocalM3StringPiece[]) DIRECTORY.get(piece); }
        catch (IllegalAccessException failure) { throw new AssertionError(failure); }
    }
    private static void number(long value) {
        for (int shift = 0; shift < 64; shift += 8) DIGEST.update((byte) (value >>> shift));
    }
    private static void value(String text) {
        number(text.length());
        for (int i = 0; i < text.length(); i++) {
            char unit = text.charAt(i);
            DIGEST.update((byte) unit);
            DIGEST.update((byte) (unit >>> 8));
        }
    }
    private static void range(M3StringPiece source, String expected, int from, int to) {
        M3StringPiece actual = source.subSequence(from, to);
        String oracle = expected.substring(from, to);
        require(actual.length() == to - from && actual.flatten().equals(oracle), "range content/length");
        for (int i = 0; i < actual.length(); i++) require(actual.charAt(i) == oracle.charAt(i), "range charAt");
        LocalM3StringPiece[] original = leaves(source);
        int outputOffset = 0;
        for (LocalM3StringPiece leaf : leaves(actual)) {
            require(leaf.length() > 0, "empty output leaf");
            for (int i = 0; i < leaf.length(); i++) {
                int position = from + outputOffset + i;
                int origin = 0;
                LocalM3StringPiece owner = null;
                for (LocalM3StringPiece input : original) {
                    if (position < origin + input.length()) { owner = input; break; }
                    origin += input.length();
                }
                require(owner != null
                        && leaf.storageIdentity().equals(owner.storageIdentity())
                        && leaf.codeUnitOffset() + i == owner.codeUnitOffset() + position - origin
                        && leaf.encoding() == owner.encoding()
                        && leaf.retainedBackingBytes() == owner.retainedBackingBytes(), "backing coordinates changed");
                units++;
                number(leaf.codeUnitOffset() + i);
                number(leaf.retainedBackingBytes());
                number(leaf.encoding().ordinal());
            }
            outputOffset += leaf.length();
        }
        require(outputOffset == actual.length(), "output leaf directory length");
        if (from == to) require(leaves(actual).length == 0, "empty slice retains a directory leaf");
        require(source.flatten().equals(expected), "source mutated");
        M3StringPiece nested = actual.subSequence(0, actual.length());
        require(nested.flatten().equals(oracle), "nested full slice");
        require(actual.subSequence(actual.length(), actual.length()).length() == 0, "nested end empty");
        value(oracle); number(from); number(to);
        cases++;
        sink = actual;
    }
    private static void invalid(M3StringPiece source, int from, int to) {
        try {
            source.subSequence(from, to);
            throw new AssertionError("invalid range admitted");
        } catch (IndexOutOfBoundsException expected) {
            value(expected.getClass().getName());
            value(String.valueOf(expected.getMessage()));
            number(from); number(to); cases++;
        }
    }
    private static void contracts() {
        LocalM3Arena arena = new LocalM3Arena();
        String[] texts = {"", "a", "abc", "\u0000\u00ff", "\ud800a\udc00", "\ud83d\ude00",
                          "a\ud83d\ude00b\r\nx", "\uffff\u0000\ud800\udc00\ud800\udc00"};
        long directories = 0;
        for (String text : texts) {
            for (boolean latin : new boolean[]{false, true}) {
                if (latin && text.chars().anyMatch(c -> c > 255)) continue;
                char[] chars = text.toCharArray();
                LocalM3StringPiece whole = latin
                        ? arena.copyLatin1(text.getBytes(StandardCharsets.ISO_8859_1)) : arena.copyUtf16(chars);
                java.util.Arrays.fill(chars, '!');
                int partitions = text.length() < 2 ? 1 : 1 << (text.length() - 1);
                for (int mask = 0; mask < partitions; mask++) {
                    List<M3StringPiece> parts = new ArrayList<>();
                    parts.add(whole.subSequence(0, 0));
                    int from = 0;
                    for (int at = 1; at <= text.length(); at++) {
                        if (at == text.length() || (mask & (1 << (at - 1))) != 0) {
                            parts.add(whole.subSequence(from, at));
                            parts.add(whole.subSequence(at, at));
                            from = at;
                        }
                    }
                    M3StringPiece[] input = parts.toArray(M3StringPiece[]::new);
                    M3StringPiece joined = M3StringPiece.join(input);
                    java.util.Arrays.fill(input, whole.subSequence(0, 0));
                    joined = M3StringPiece.join(M3StringPiece.join(), joined, M3StringPiece.join());
                    require(joined.flatten().equals(text), "input directory snapshot/nested join");
                    for (int a = 0; a <= text.length(); a++) {
                        for (int b = a; b <= text.length(); b++) range(joined, text, a, b);
                    }
                    invalid(joined, -1, 0);
                    invalid(joined, 0, text.length() + 1);
                    invalid(joined, 1, 0);
                    invalid(joined, Integer.MIN_VALUE, Integer.MAX_VALUE);
                    invalid(joined, Integer.MAX_VALUE, Integer.MAX_VALUE);
                    directories++;
                }
                M3StringPiece repeated = M3StringPiece.join(whole, whole, whole.subSequence(0, 0));
                String twice = text + text;
                for (int a = 0; a <= twice.length(); a++) {
                    range(repeated, twice, a, Math.min(twice.length(), a + 2));
                }
            }
        }
        System.out.printf(Locale.ROOT,
                "JOINED_RANGE_CONTRACT_PASS directories=%d cases=%d backingUnits=%d digest=%s%n",
                directories, cases, units, HexFormat.of().formatHex(DIGEST.digest()));
    }
    private static long retained(M3StringPiece piece) {
        Set<StorageIdentity> seen = new HashSet<>();
        long bytes = 0;
        for (LocalM3StringPiece leaf : leaves(piece)) {
            if (seen.add(leaf.storageIdentity())) bytes += leaf.retainedBackingBytes();
        }
        return bytes;
    }
    private static long execute(M3StringPiece source, int from, int to, int iterations) {
        long checksum = 0;
        for (int i = 0; i < iterations; i++) {
            M3StringPiece result = source.subSequence(from, to);
            checksum += result.length();
            if (result.length() != 0) checksum += result.charAt(0) + result.charAt(result.length() - 1);
            sink = result; // Keep each result observable; do not benchmark an eliminated allocation.
        }
        return checksum;
    }
    private static void benchmark(int sample) {
        java.lang.management.ThreadMXBean rawBean = ManagementFactory.getThreadMXBean();
        if (!(rawBean instanceof com.sun.management.ThreadMXBean bean)
                || !bean.isThreadAllocatedMemorySupported()) {
            throw new IllegalStateException("HOLD_THREAD_ALLOCATION_ACCOUNTING_UNAVAILABLE");
        }
        if (!bean.isThreadAllocatedMemoryEnabled()) bean.setThreadAllocatedMemoryEnabled(true);
        long thread = Thread.currentThread().threadId();
        LocalM3Arena arena = new LocalM3Arena();
        String[] families = {"empty-middle", "one-middle", "one-tail", "two-middle", "full"};
        for (int count : new int[]{32, 1024, 8192}) {
            M3StringPiece[] input = new M3StringPiece[count];
            for (int i = 0; i < count; i++) {
                input[i] = arena.copyUtf16(new char[]{(char) (i % 7 == 0 ? 0xd800 : i % 7 == 1 ? 0xdc00 : i % 256)});
            }
            M3StringPiece joined = M3StringPiece.join(input);
            String flat = joined.flatten();
            int middle = count / 2;
            int[][] ranges = {{middle, middle}, {middle, middle + 1}, {count - 1, count},
                              {middle - 1, middle + 1}, {0, count}};
            for (int at = 0; at < ranges.length; at++) {
                int from = ranges[at][0], to = ranges[at][1];
                M3StringPiece check = joined.subSequence(from, to);
                require(check.flatten().equals(flat.substring(from, to)), "benchmark oracle");
                int resultLeaves = leaves(check).length;
                long retainedBytes = retained(check);
                execute(joined, from, to, 128);
                long beforeBytes = bean.getThreadAllocatedBytes(thread);
                long start = System.nanoTime();
                long checksum = execute(joined, from, to, 1024);
                long elapsed = System.nanoTime() - start;
                long allocated = bean.getThreadAllocatedBytes(thread) - beforeBytes;
                require(elapsed > 0 && allocated >= 0, "allocation/timing counters");
                long perCall = to - from;
                if (from != to) perCall += flat.charAt(from) + flat.charAt(to - 1);
                require(checksum == perCall * 1024L, "benchmark checksum");
                System.out.printf(Locale.ROOT,
                        "{\"schema\":\"M3_JOINED_RANGE_SAMPLE_V1\",\"sample\":%d,\"leaves\":%d,"
                        + "\"family\":\"%s\",\"from\":%d,\"to\":%d,\"iterations\":1024,\"warmup\":128,"
                        + "\"elapsedNanos\":%d,\"allocatedBytes\":%d,\"checksum\":%d,"
                        + "\"resultLeaves\":%d,\"retainedBackingBytes\":%d}%n",
                        sample, count, families[at], from, to, elapsed, allocated, checksum, resultLeaves, retainedBytes);
            }
        }
    }
    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("contract")) contracts();
        else if (args.length == 2 && args[0].equals("benchmark")) benchmark(Integer.parseInt(args[1]));
        else throw new IllegalArgumentException("contract | benchmark <sample>");
    }
}
