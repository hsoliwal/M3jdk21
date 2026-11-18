// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.fs;

// Modified 2026-10-21: preserve explicit legacy timestamp knowledge and v1 byte round trips.
// Modified 2026-11-18: cover clean/incremental javac parity and row-local dependency bounds.

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/** Runnable without JUnit or JNI; the M3FS Maven suite invokes the same oracle. */
public final class M3BuildStateContract {
    private static final int MAX_BYTES = 16_000_000;
    private static int checks;

    private M3BuildStateContract() { }
    private static byte[] sha(String data) {
        try { return MessageDigest.getInstance("SHA-256").digest(data.getBytes(StandardCharsets.UTF_8)); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    private static void invalid(Runnable task) {
        checks++;
        try { task.run(); throw new AssertionError("accepted invalid input"); }
        catch (IllegalArgumentException expected) { /* expected */ }
    }
    private static void badImage(byte[] bytes) {
        checks++;
        try { M3BuildState.decode(bytes, MAX_BYTES, 1_000, 10_000); throw new AssertionError("accepted corrupt image"); }
        catch (IOException expected) { /* expected */ }
    }

    private static M3BuildState sample(String a, String abiA, String context) {
        String[] keys = {"a/A.java", "b/B.class", "c/prop.txt"};
        byte[] role = {M3BuildState.SOURCE, M3BuildState.CLASS, M3BuildState.RESOURCE};
        long[] sizes = {a.length(), 8L, 4L};
        byte[] digests = new byte[3 * 32];
        byte[] abis = new byte[3 * 32];
        put(digests, 0, sha(a)); put(digests, 1, sha("class")); put(digests, 2, sha("resource"));
        put(abis, 0, sha(abiA));
        return new M3BuildState(keys, role, sizes, digests, abis, new long[]{1L},
                new int[]{0, 0, 1, 2}, new int[]{0, 1}, sha(context));
    }

    private static M3BuildState overlappingChanges(boolean now) {
        byte[] contents = new byte[96], abi = new byte[96];
        put(contents, 0, sha(now ? "a new" : "a old"));
        put(contents, 1, sha(now ? "b new" : "b old"));
        put(contents, 2, sha("c unchanged"));
        put(abi, 0, sha(now ? "A NEW ABI" : "A OLD ABI"));
        put(abi, 1, sha("B identical ABI"));
        return new M3BuildState(new String[]{"a", "b", "c"},
                fill(3, M3BuildState.SOURCE), new long[]{5, 5, 11}, contents,
                abi, new long[]{3}, new int[]{0, 0, 1, 2}, new int[]{0, 1}, sha("ctx"));
    }

    private static void put(byte[] lane, int row, byte[] value) {
        System.arraycopy(value, 0, lane, row * 32, 32);
    }

    public static int verify() throws IOException {
        checks = 0;
        M3BuildState baseline = sample("old", "sameABI", "ctx");
        byte[] encoded = baseline.encode(MAX_BYTES);
        M3BuildState decoded = M3BuildState.decode(encoded, MAX_BYTES, 3, 2);
        check(Arrays.equals(encoded, decoded.encode(MAX_BYTES)), "canonical round-trip");
        check(decoded.size() == 3 && decoded.dependencyCount() == 2, "shape");
        check(decoded.dependency(2, 0) == 1 && decoded.dependency(1, 0) == 0, "edge identity");
        check(decoded.find("b/B.class", M3BuildState.CLASS) == 1 && decoded.find("absent", M3BuildState.CLASS) == -1, "binary search");
        check(decoded.hasAbi(0) && !decoded.hasAbi(1), "ABI bitmap");
        check(java.nio.ByteBuffer.wrap(encoded).getInt(4) == 1, "legacy constructor preserves v1 schema");
        check(!decoded.completeInventory() && decoded.modifiedTimeNanos(0).isEmpty(), "v1 timestamp remains unknown");
        check(!decoded.hasModifiedTime(1), "unknown timestamp is not epoch zero");
        check(decoded.changedFrom(decoded).affectedCount() == 0L, "unchanged exact state");
        M3BuildState changed = sample("new body", "sameABI", "ctx");
        M3BuildState.Plan plan = changed.changedFrom(baseline);
        check(!plan.fullRebuild() && plan.changed(0) && plan.affected(0), "modified source");
        check(!plan.affected(1) && !plan.affected(2), "compiler-proven ABI stops propagation");
        check(plan.affectedCount() == 1L, "ABI-limited count");
        M3BuildState apiChanged = sample("new body", "DIFFERENT ABI", "ctx");
        plan = apiChanged.changedFrom(baseline);
        check(plan.affectedCount() == 3L && plan.affected(2), "reverse dependency closure");
        // A changed exported ABI forces B to rebuild, even though B's independently
        // changed source has an equal ABI. B must not block the propagation to C.
        M3BuildState oldChain = overlappingChanges(false);
        M3BuildState newChain = overlappingChanges(true);
        M3BuildState.Plan overlapping = newChain.changedFrom(oldChain);
        check(overlapping.changed(0) && overlapping.changed(1), "overlapping direct changes");
        check(overlapping.affectedCount() == 3L && overlapping.affected(2),
                "independently dirty middle vertex cannot suppress ancestor invalidation");
        check(Arrays.equals(plan.changedMask(), new long[]{1L}), "only source changed");
        long[] hacked = plan.affectedMask(); hacked[0] = 0L;
        check(plan.affectedCount() == 3L, "defensive bitmap projection");
        check(apiChanged.changedFrom(sample("old", "sameABI", "new ctx")).fullRebuild(), "context invalidation");
        check(apiChanged.changedFrom(null).fullRebuild(), "cold first build");
        check(apiChanged.changedFrom(baseline).affectedCount() == 3L, "repeatable planning");
        M3BuildState removed = new M3BuildState(new String[]{"a/A.java"},
                new byte[]{M3BuildState.SOURCE}, new long[]{3}, sha("old"), sha("sameABI"),
                new long[]{1L}, new int[]{0, 0}, new int[0], sha("ctx"));
        check(removed.changedFrom(baseline).fullRebuild(), "removed input fail-closed");
        check(removed.changedFrom(baseline).affectedCount() == 1L, "removed input surviving rows");
        byte[] corrupt = encoded.clone(); corrupt[10] ^= 0x01; badImage(corrupt);
        badImage(Arrays.copyOf(encoded, encoded.length - 1));
        badImage(Arrays.copyOf(encoded, MAX_BYTES + 1));
        // Counts that fit the caller's row limit but not the image budget must
        // be refused before allocating any large column arrays.
        byte[] impossible = encoded.clone();
        java.nio.ByteBuffer.wrap(impossible).putInt(8, 30_000);
        putSeal(impossible);
        checks++;
        try { M3BuildState.decode(impossible, MAX_BYTES, 40_000, 10_000);
              throw new AssertionError("accepted impossible image geometry"); }
        catch (IOException expected) { /* expected */ }
        checks++;
        try { baseline.encode(encoded.length - 1);
              throw new AssertionError("admitted output exceeding exact bound"); }
        catch (IOException expected) { /* expected */ }
        invalid(() -> sampleInvalid(new String[]{"same", "same"}, new int[]{0, 0, 0}, new int[0]));
        invalid(() -> sampleInvalid(new String[]{"../bad", "safe"}, new int[]{0, 0, 0}, new int[0]));
        invalid(() -> sampleInvalid(new String[]{"a//b", "safe"}, new int[]{0, 0, 0}, new int[0]));
        invalid(() -> sampleInvalid(new String[]{"a/./b", "safe"}, new int[]{0, 0, 0}, new int[0]));
        invalid(() -> sampleInvalid(new String[]{"a/", "safe"}, new int[]{0, 0, 0}, new int[0]));
        invalid(() -> sampleInvalid(new String[]{"a", "b"}, new int[]{0, 0, 1}, new int[]{2}));
        invalid(() -> sampleInvalid(new String[]{"a", "b"}, new int[]{0, 0, 2}, new int[]{0, 0}));
        invalid(() -> sampleInvalid(new String[]{"a", "b"}, new int[]{0, 0, 0}, new int[0], new long[]{4L}));
        buildInputMatchesCleanCompilation();
        buildInputInvalidation();
        dependencyOrdinals();
        fuzzReverseClosure();
        return checks;
    }

    /** A real compiler option change must not reuse a stale class with identical source/ABI. */
    private static void buildInputMatchesCleanCompilation() throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        check(compiler != null, "Java 21 compiler is required for the clean/incremental oracle");
        Path directory = Files.createTempDirectory("m3-build-state-clean-");
        try {
            Path source = directory.resolve("Example.java");
            String code = "public class Example { public int value(int n) { int x = n + 1; return x; } }\n";
            Files.writeString(source, code, StandardCharsets.UTF_8);
            Path incremental = directory.resolve("incremental"), clean = directory.resolve("clean");
            compile(compiler, source, incremental, "-g:none");
            byte[] originalClass = Files.readAllBytes(incremental.resolve("Example.class"));
            M3BuildState previous = compilerState("-g:none", code, 0, null, false);
            M3BuildState next = compilerState("-g:lines,vars,source", code, 0, null, false);
            // Exercise the persisted production facts, not a test-only planning model.
            previous = M3BuildState.decode(previous.encode(MAX_BYTES), MAX_BYTES, 2, 0);
            next = M3BuildState.decode(next.encode(MAX_BYTES), MAX_BYTES, 2, 0);
            M3BuildState.Plan plan = next.changedFrom(previous);
            if (plan.affected(1)) compile(compiler, source, incremental, "-g:lines,vars,source");
            compile(compiler, source, clean, "-g:lines,vars,source");
            byte[] cleanClass = Files.readAllBytes(clean.resolve("Example.class"));
            check(!Arrays.equals(originalClass, cleanClass), "oracle must observe changed javac output");
            check(Arrays.equals(Files.readAllBytes(incremental.resolve("Example.class")), cleanClass),
                    "build-input-only edit must match clean javac class bytes");
            check(plan.fullRebuild() && plan.affectedCount() == 2,
                    "build options without semantic edges invalidate the complete inventory");
        } finally {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }

    private static void compile(JavaCompiler compiler, Path source, Path output, String debug) throws IOException {
        Files.createDirectories(output);
        ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
        int status = compiler.run(null, diagnostics, diagnostics, "-proc:none", "--release", "21",
                debug, "-d", output.toString(), source.toString());
        check(status == 0, "javac failed: " + diagnostics.toString(StandardCharsets.UTF_8));
    }

    private static M3BuildState compilerState(String options, String source, long extraLength,
            String buildAbi, boolean buildEdge) {
        byte[] hashes = new byte[64], abis = new byte[64];
        put(hashes, 0, sha(options)); put(hashes, 1, sha(source));
        put(abis, 1, sha("public class Example { public int value(int); }"));
        if (buildAbi != null) put(abis, 0, sha(buildAbi));
        return new M3BuildState(new String[]{"build/options.args", "src/Example.java"},
                new byte[]{M3BuildState.BUILD_INPUT, M3BuildState.SOURCE},
                new long[]{options.length() + extraLength, source.length()}, hashes, abis,
                new long[]{buildAbi == null ? 2L : 3L},
                buildEdge ? new int[]{0, 1, 1} : new int[]{0, 0, 0},
                buildEdge ? new int[]{1} : new int[0], sha("stable Java21 compilation context"));
    }

    private static void buildInputInvalidation() throws IOException {
        M3BuildState old = compilerState("option-A", "body-A", 0, null, false);
        M3BuildState same = compilerState("option-A", "body-A", 0, null, false);
        check(!same.changedFrom(old).fullRebuild() && same.changedFrom(old).affectedCount() == 0,
                "unchanged build inputs preserve the empty plan");
        M3BuildState body = compilerState("option-A", "body-B", 0, null, false);
        M3BuildState.Plan local = body.changedFrom(old);
        check(!local.fullRebuild() && local.affectedCount() == 1 && local.affected(1) && !local.affected(0),
                "unchanged build inputs preserve the source ABI fast path");
        M3BuildState[] changed = {
            compilerState("option-B", "body-A", 0, null, false),
            compilerState("option-A", "body-A", 1, null, false),
            compilerState("option-A", "body-A", 0, "known build ABI", false),
            compilerState("option-A", "body-A", 0, null, true)
        };
        for (M3BuildState next : changed) {
            M3BuildState.Plan plan = next.changedFrom(old);
            check(plan.fullRebuild() && Arrays.equals(plan.changedMask(), new long[]{3L})
                    && Arrays.equals(plan.affectedMask(), new long[]{3L}),
                    "changed build-input facts must invalidate every current row");
            M3BuildState restored = M3BuildState.decode(next.encode(MAX_BYTES), MAX_BYTES, 2, 1);
            check(restored.changedFrom(old).fullRebuild(), "persisted build-input change stays conservative");
        }
        M3BuildState known = compilerState("option-A", "body-A", 0, "known build ABI", false);
        check(old.changedFrom(known).fullRebuild(), "lost build-input ABI evidence is conservative");
        check(compilerState("option-A", "body-A", 0, "changed build ABI", false)
                .changedFrom(known).fullRebuild(), "changed build-input ABI is conservative");
        check(compilerState("option-B", "body-A", 0, "known build ABI", false)
                .changedFrom(known).fullRebuild(), "equal ABI cannot scope a changed build input");
        M3BuildState sourceOnly = new M3BuildState(new String[]{"src/Example.java"},
                new byte[]{M3BuildState.SOURCE}, new long[]{6}, sha("body-A"),
                sha("public class Example { public int value(int); }"), new long[]{1},
                new int[]{0, 0}, new int[0], sha("stable Java21 compilation context"));
        check(old.changedFrom(sourceOnly).fullRebuild(), "new build input invalidates existing source");

        // Full masks must retain their canonical tail across the 64-bit word boundary.
        int n = 65;
        String[] keys = new String[n];
        byte[] roles = new byte[n], before = new byte[n * 32], after = new byte[n * 32];
        for (int i = 0; i < n; i++) {
            keys[i] = "row/" + (100 + i); roles[i] = M3BuildState.SOURCE;
            put(before, i, sha("unchanged")); put(after, i, sha("unchanged"));
        }
        roles[64] = M3BuildState.BUILD_INPUT; put(after, 64, sha("changed options"));
        M3BuildState previous = new M3BuildState(keys, roles, new long[n], before,
                new byte[n * 32], new long[2], new int[n + 1], new int[0], sha("ctx"));
        M3BuildState next = new M3BuildState(keys, roles, new long[n], after,
                new byte[n * 32], new long[2], new int[n + 1], new int[0], sha("ctx"));
        M3BuildState.Plan plan = next.changedFrom(previous);
        check(plan.fullRebuild() && plan.affectedCount() == n
                && Arrays.equals(plan.changedMask(), new long[]{-1L, 1L})
                && Arrays.equals(plan.affectedMask(), new long[]{-1L, 1L}),
                "build-input invalidation masks must clear unused tail bits");
    }

    private static void dependencyOrdinals() {
        M3BuildState state = sample("old", "sameABI", "ctx");
        for (int row = 0; row < state.size(); row++) {
            for (int ordinal : new int[]{-1, Integer.MIN_VALUE, Integer.MAX_VALUE, state.dependencyCount(row)}) {
                checks++;
                try {
                    state.dependency(row, ordinal);
                    throw new AssertionError("accepted invalid row-local dependency ordinal: " + row + "/" + ordinal);
                } catch (IndexOutOfBoundsException expected) { /* expected */ }
            }
        }
        check(state.dependency(1, 0) == 0 && state.dependency(2, 0) == 1,
                "valid row-local dependency ordinals preserve edge identities");
        M3BuildState emptyLast = new M3BuildState(new String[]{"a", "b", "c"},
                fill(3, M3BuildState.SOURCE), new long[3], new byte[96], new byte[96], new long[1],
                new int[]{0, 0, 1, 1}, new int[]{0}, sha("ctx"));
        checks++;
        try {
            emptyLast.dependency(2, -1);
            throw new AssertionError("empty row read a preceding row's dependency");
        } catch (IndexOutOfBoundsException expected) { /* expected */ }
    }

    private static void putSeal(byte[] encoded) {
        byte[] data = Arrays.copyOf(encoded, encoded.length - 32);
        byte[] hash;
        try { hash = MessageDigest.getInstance("SHA-256").digest(data); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
        System.arraycopy(hash, 0, encoded, data.length, 32);
    }

    private static byte[] fill(int n, byte value) { byte[] values = new byte[n]; Arrays.fill(values, value); return values; }

    private static void sampleInvalid(String[] keys, int[] offsets, int[] edges) {
        sampleInvalid(keys, offsets, edges, new long[]{0L});
    }
    private static void sampleInvalid(String[] keys, int[] offsets, int[] edges, long[] mask) {
        byte[] ids = new byte[keys.length * 32];
        new M3BuildState(keys, fill(keys.length, M3BuildState.SOURCE), new long[keys.length], ids, ids,
                mask, offsets, edges, sha("ctx"));
    }

    private static void fuzzReverseClosure() throws IOException {
        Random random = new Random(210_2026L);
        for (int trial = 0; trial < 180; trial++) {
            int n = 1 + random.nextInt(160), changedRow = random.nextInt(n);
            String[] keys = new String[n]; byte[] roles = new byte[n]; long[] length = new long[n];
            byte[] oldHashes = new byte[n * 32], newHashes = new byte[n * 32], unknown = new byte[n * 32];
            int[] offsets = new int[n + 1], pool = new int[n * 7]; int count = 0;
            boolean[][] deps = new boolean[n][n];
            for (int i = 0; i < n; i++) {
                keys[i] = String.format("unit/%05d.java", i); roles[i] = M3BuildState.SOURCE; length[i] = 7L;
                put(oldHashes, i, sha("unit:" + i)); put(newHashes, i, sha(i == changedRow ? "CHANGED:" + i : "unit:" + i));
                offsets[i] = count;
                for (int j = 0; j < n; j++) {
                    if (i != j && random.nextInt(150) < 2) {
                        if (count == pool.length) pool = Arrays.copyOf(pool, pool.length * 2 + 1);
                        deps[i][j] = true; pool[count++] = j;
                    }
                }
            }
            offsets[n] = count;
            int[] edges = Arrays.copyOf(pool, count);
            long[] noAbi = new long[(n + 63) >>> 6];
            M3BuildState old = new M3BuildState(keys, roles, length, oldHashes, unknown,
                    noAbi, offsets, edges, sha("ctx"));
            M3BuildState next = new M3BuildState(keys, roles, length, newHashes, unknown,
                    noAbi, offsets, edges, sha("ctx"));
            M3BuildState.Plan plan = next.changedFrom(old);
            boolean[] reference = new boolean[n]; reference[changedRow] = true;
            boolean updated;
            do {
                updated = false;
                for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) {
                    if (deps[i][j] && reference[j] && !reference[i]) { reference[i] = true; updated = true; }
                }
            } while (updated);
            for (int i = 0; i < n; i++) check(plan.affected(i) == reference[i], "differential reverse closure row " + i);
            byte[] image = next.encode(MAX_BYTES);
            check(Arrays.equals(image, M3BuildState.decode(image, MAX_BYTES, n, edges.length).encode(MAX_BYTES)), "random roundtrip");
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("M3_BUILD_STATE_JAVA21_PASS checks=" + verify());
    }
}
