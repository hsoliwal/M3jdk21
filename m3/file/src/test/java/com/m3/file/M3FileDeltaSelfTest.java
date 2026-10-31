// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Standalone executable Java 21 contract fixture; no synthetic implementation of M3File. */
public final class M3FileDeltaSelfTest {
    private static int checks;

    private M3FileDeltaSelfTest() { }

    public static void main(String[] arguments) throws Exception {
        snapshotOrderingAndFixedPoint();
        roleAndPathIdentity();
        sourceAndClassDelta();
        independentlyAttestedAbiEvidence();
        buildContextInvalidation();
        staleOrDifferentAbiEvidence();
        addedRemovedAndEmptySnapshots();
        refusedInputs();
        realNioCaptureAndTimestampInvariant();
        symlinkAndSizeRefusal();
        deterministicAdversarialReplay();
        changedAbiFactWithIdenticalClassBytes();
        cliManifestDiffWithRealM3File();
        realClassfileDeltaAfterJavac();
        System.out.println("M3FILE_DELTA_SELFTEST_PASS checks=" + checks);
    }

    private static String context(int n) { return String.format(java.util.Locale.ROOT, "%064x", n); }

    private static M3FileDelta.Entry bytes(M3FileDelta.Kind role, String path, String payload, String abi) {
        return M3FileDelta.fromBytes(role, path, payload.getBytes(StandardCharsets.UTF_8), abi);
    }

    private static void snapshotOrderingAndFixedPoint() {
        var a = bytes(M3FileDelta.Kind.SOURCE, "src/B.java", "record B() {}", null);
        var b = bytes(M3FileDelta.Kind.CLASS, "classes/B.class", "\0\u00ca\u00fe", context(7));
        var c = bytes(M3FileDelta.Kind.BUILD_INPUT, "pom.xml", "<project/>", null);
        var one = M3FileDelta.snapshot(List.of(a, b, c), context(9));
        var two = M3FileDelta.snapshot(List.of(c, a, b), context(9));
        eq(one.rootSha256(), two.rootSha256(), "input permutations");
        eq(3, one.size(), "snapshot cardinality");
        eq(a, one.entry(0), "source sorts first");
        eq(b, one.entry(1), "class sorts second");
        yes(M3FileDelta.compare(one, two).sameEvidence(), "stable replay");
        eq(0, M3FileDelta.compare(one, two).changes().size(), "no changes");
    }

    private static void roleAndPathIdentity() {
        var first = bytes(M3FileDelta.Kind.SOURCE, "a", "same", null);
        var second = bytes(M3FileDelta.Kind.CLASS, "a", "same", context(1));
        eq(2, M3FileDelta.snapshot(List.of(first, second), context(2)).size(), "typed identity");
        throwsType(IllegalArgumentException.class, () -> M3FileDelta.snapshot(List.of(first, first), context(2)));
    }

    private static void sourceAndClassDelta() {
        var beforeSource = bytes(M3FileDelta.Kind.SOURCE, "src/A.java", "class A { int value(){return 1;} }", null);
        var afterSource = bytes(M3FileDelta.Kind.SOURCE, "src/A.java", "class A { int value(){return 2;} }", null);
        var compiled = bytes(M3FileDelta.Kind.CLASS, "classes/A.class", "same-bytecode", context(55));
        var result = M3FileDelta.compare(M3FileDelta.snapshot(List.of(beforeSource, compiled), context(8)),
                M3FileDelta.snapshot(List.of(compiled, afterSource), context(8)));
        eq(1, result.changes().size(), "source-only exact change");
        eq(M3FileDelta.Change.MODIFIED, result.changes().getFirst().change(), "modified source");
        eq(M3FileDelta.Kind.SOURCE, result.changes().getFirst().kind(), "source role");
        yes(!result.sameEvidence(), "source invalidation signal");
    }

    private static void independentlyAttestedAbiEvidence() {
        var before = bytes(M3FileDelta.Kind.CLASS, "out/A.class", "method body v1", context(11));
        var after = bytes(M3FileDelta.Kind.CLASS, "out/A.class", "method body v2", context(11));
        var delta = M3FileDelta.compare(M3FileDelta.snapshot(List.of(before), context(1)),
                M3FileDelta.snapshot(List.of(after), context(1))).changes().getFirst();
        eq(M3FileDelta.AbiChange.SAME_ATTESTED, delta.abi(), "explicit ABI equality only");
        eq(M3FileDelta.Change.MODIFIED, delta.change(), "binary still changed");
        eq(before.sha256(), delta.previousSha256(), "old digest");
        eq(after.sha256(), delta.currentSha256(), "new digest");
    }

    private static void buildContextInvalidation() {
        var x = bytes(M3FileDelta.Kind.SOURCE, "src/C.java", "class C{}", null);
        var a = M3FileDelta.snapshot(List.of(x), context(100));
        var b = M3FileDelta.snapshot(List.of(x), context(101));
        var delta = M3FileDelta.compare(a, b);
        yes(delta.contextChanged(), "compiler/plugin/processor context invalidated");
        eq(0, delta.changes().size(), "file bytes independently unchanged");
        yes(!delta.sameEvidence(), "cannot reuse unseen changed context");
        yes(!a.rootSha256().equals(b.rootSha256()), "domain-separated roots");
    }

    private static void staleOrDifferentAbiEvidence() {
        var a = bytes(M3FileDelta.Kind.CLASS, "classes/Z.class", "z1", context(1));
        var b = bytes(M3FileDelta.Kind.CLASS, "classes/Z.class", "z2", context(2));
        var c = bytes(M3FileDelta.Kind.CLASS, "classes/Z.class", "z2", null);
        eq(M3FileDelta.AbiChange.DIFFERENT_ATTESTED,
                M3FileDelta.compare(M3FileDelta.snapshot(List.of(a), context(1)),
                        M3FileDelta.snapshot(List.of(b), context(1))).changes().getFirst().abi(),
                "ABI-changing supplied facts");
        eq(M3FileDelta.AbiChange.UNKNOWN,
                M3FileDelta.compare(M3FileDelta.snapshot(List.of(a), context(1)),
                        M3FileDelta.snapshot(List.of(c), context(1))).changes().getFirst().abi(),
                "unknown ABI no compile avoidance");
    }

    private static void addedRemovedAndEmptySnapshots() {
        var empty = M3FileDelta.snapshot(List.of(), context(1));
        var file = bytes(M3FileDelta.Kind.CLASS, "classes/Old.class", "binary", context(1));
        var populated = M3FileDelta.snapshot(List.of(file), context(1));
        var added = M3FileDelta.compare(empty, populated).changes().getFirst();
        var removed = M3FileDelta.compare(populated, empty).changes().getFirst();
        eq(M3FileDelta.Change.ADDED, added.change(), "class added");
        eq(M3FileDelta.Change.REMOVED, removed.change(), "class deleted");
        eq(M3FileDelta.AbiChange.UNKNOWN, removed.abi(), "deleted ABI unknown");
        eq(null, removed.currentSha256(), "no fabricated missing digest");
    }

    private static void refusedInputs() {
        var file = bytes(M3FileDelta.Kind.SOURCE, "src/A.java", "foo", null);
        throwsType(IllegalArgumentException.class, () -> M3FileDelta.snapshot(List.of(file, file), context(1)));
        throwsType(IllegalArgumentException.class, () -> M3FileDelta.snapshot(List.of(file), "BAD_HASH"));
        for (String invalid : List.of("/abs", "../up", "src/../up", "src//A.java", "src/./A.java", "C:/x", "a\\b", "")) {
            throwsType(IllegalArgumentException.class, () -> bytes(M3FileDelta.Kind.SOURCE, invalid, "v", null));
        }
        throwsType(IllegalArgumentException.class,
                () -> bytes(M3FileDelta.Kind.SOURCE, "src/A.java", "v", context(9)));
        throwsType(IllegalArgumentException.class,
                () -> new M3FileDelta.Entry(M3FileDelta.Kind.SOURCE, "src/A.java", 1, "bad", null));
        throwsType(IndexOutOfBoundsException.class, () -> M3FileDelta.snapshot(List.of(file), context(1)).entry(3));
    }

    private static void realNioCaptureAndTimestampInvariant() throws IOException {
        Path root = Files.createTempDirectory("m3-file-delta-");
        Path src = root.resolve("src/D.java");
        Files.createDirectories(src.getParent());
        Files.writeString(src, "class D { int x=1; }", StandardCharsets.UTF_8);
        FileTime original = Files.getLastModifiedTime(src);
        var first = M3FileDelta.capture(root, M3FileDelta.Kind.SOURCE, "src/D.java", 1024, null);
        Files.writeString(src, "class D { int x=2; }", StandardCharsets.UTF_8);
        Files.setLastModifiedTime(src, original);
        var second = M3FileDelta.capture(root, M3FileDelta.Kind.SOURCE, "src/D.java", 1024, null);
        yes(!first.sha256().equals(second.sha256()), "exact bytes, identical mtime and size");
        eq(first.length(), second.length(), "same byte lengths");
        var deltas = M3FileDelta.compare(M3FileDelta.snapshot(List.of(first), context(1)),
                M3FileDelta.snapshot(List.of(second), context(1))).changes();
        eq(1, deltas.size(), "mtime cannot hide source change");
        // No shared retained file handle: caller can delete after capture.
        Files.delete(src);
        Files.delete(src.getParent());
        Files.delete(root);
    }

    private static void symlinkAndSizeRefusal() throws IOException {
        Path root = Files.createTempDirectory("m3-delta-refusal-");
        Path src = root.resolve("A.java");
        Files.writeString(src, "123456789");
        throwsType(IOException.class, () -> M3FileDelta.capture(root, M3FileDelta.Kind.SOURCE, "A.java", 4, null));
        throwsType(IllegalArgumentException.class,
                () -> M3FileDelta.capture(root, M3FileDelta.Kind.SOURCE, "A.java", -1, null));
        Path link = root.resolve("Alias.java");
        try {
            Files.createSymbolicLink(link, src);
            throwsType(IOException.class,
                    () -> M3FileDelta.capture(root, M3FileDelta.Kind.SOURCE, "Alias.java", 100, null));
            Files.delete(link);
        } catch (UnsupportedOperationException | java.nio.file.FileSystemException unavailable) {
            // Some supported providers cannot create symlinks; actual M3File still rejects them.
        }
        Files.delete(src);
        Files.delete(root);
    }

    private static void deterministicAdversarialReplay() {
        List<M3FileDelta.Entry> before = new ArrayList<>();
        List<M3FileDelta.Entry> after = new ArrayList<>();
        for (int i = 0; i < 517; i++) {
            String path = "src/pkg/Unit" + i + ".java";
            before.add(bytes(M3FileDelta.Kind.SOURCE, path, "unit" + i, null));
            after.add(bytes(M3FileDelta.Kind.SOURCE, path, i % 7 == 0 ? "replacement" + i : "unit" + i, null));
        }
        var a = M3FileDelta.snapshot(before, context(1));
        var b = M3FileDelta.snapshot(after, context(1));
        java.util.Collections.reverse(before);
        java.util.Collections.reverse(after);
        eq(a.rootSha256(), M3FileDelta.snapshot(before, context(1)).rootSha256(), "order stable A");
        eq(b.rootSha256(), M3FileDelta.snapshot(after, context(1)).rootSha256(), "order stable B");
        eq(74, M3FileDelta.compare(a, b).changes().size(), "exact changed units");
    }

    private static void changedAbiFactWithIdenticalClassBytes() {
        var sameBinary = bytes(M3FileDelta.Kind.CLASS, "classes/K.class", "unchanged bytes", context(3));
        var changedAttestation = bytes(M3FileDelta.Kind.CLASS, "classes/K.class", "unchanged bytes", context(4));
        var difference = M3FileDelta.compare(M3FileDelta.snapshot(List.of(sameBinary), context(1)),
                M3FileDelta.snapshot(List.of(changedAttestation), context(1)));
        eq(M3FileDelta.Change.ATTESTATION_CHANGED, difference.changes().getFirst().change(),
                "ABI fact-only mutation is reported");
        eq(M3FileDelta.AbiChange.SAME_BYTES, difference.changes().getFirst().abi(),
                "byte identity beats unverified ABI story");
    }

    private static void cliManifestDiffWithRealM3File() throws IOException {
        Path directory = Files.createTempDirectory("m3-delta-cli-");
        Path before = directory.resolve("before.tsv");
        Path after = directory.resolve("after.tsv");
        var original = bytes(M3FileDelta.Kind.SOURCE, "src/A.java", "v1", null);
        var updated = bytes(M3FileDelta.Kind.SOURCE, "src/A.java", "v2", null);
        String head = "M3FILEDELTA_MANIFEST_V1\n";
        java.util.function.Function<M3FileDelta.Entry, String> row = e ->
                e.kind() + "\t" + e.path() + "\t" + e.length() + "\t"
                        + e.sha256() + "\t-\n";
        Files.writeString(before, head + row.apply(original));
        Files.writeString(after, head + row.apply(updated));
        var output = new java.io.ByteArrayOutputStream();
        try (var printer = new java.io.PrintStream(output, true, StandardCharsets.UTF_8)) {
            M3FileDeltaCli.execute(before, after, context(9), context(9), printer);
        }
        String proof = output.toString(StandardCharsets.UTF_8);
        yes(proof.startsWith("M3FILEDELTA_RECEIPT_V1\n"), "CLI receipt version");
        yes(proof.contains("SOURCE\tsrc/A.java\tMODIFIED\tNOT_CLASS"), "CLI source diff");
        yes(proof.contains("CONTEXT_CHANGED\tfalse"), "CLI unchanged task envelope");
        Files.writeString(after, head + "SOURCE\tsrc/A.java\t999\tbad\t-\n");
        throwsType(IOException.class, () -> M3FileDeltaCli.execute(before, after,
                context(9), context(9), System.out));
        Files.delete(before); Files.delete(after); Files.delete(directory);
    }

    private static void realClassfileDeltaAfterJavac() throws Exception {
        Path root = Files.createTempDirectory("m3-delta-javac-");
        Path src = root.resolve("src/DeltaFixture.java");
        Path out = root.resolve("classes");
        Files.createDirectories(src.getParent());
        Files.createDirectories(out);
        String versionOne = "public class DeltaFixture { public int value(){return 1;} }";
        String versionTwo = "public class DeltaFixture { public int value(){return 2;} }";
        Files.writeString(src, versionOne);
        compileFixture(out, src);
        var first = M3FileDelta.capture(root, M3FileDelta.Kind.CLASS,
                "classes/DeltaFixture.class", 65536, null);
        Files.writeString(src, versionTwo);
        compileFixture(out, src);
        var second = M3FileDelta.capture(root, M3FileDelta.Kind.CLASS,
                "classes/DeltaFixture.class", 65536, null);
        var delta = M3FileDelta.compare(M3FileDelta.snapshot(List.of(first), context(11)),
                M3FileDelta.snapshot(List.of(second), context(11))).changes().getFirst();
        eq(M3FileDelta.Kind.CLASS, delta.kind(), "real javac class role");
        eq(M3FileDelta.Change.MODIFIED, delta.change(), "real changed class binary");
        eq(M3FileDelta.AbiChange.UNKNOWN, delta.abi(), "no invented ABI identity");
        yes(!first.sha256().equals(second.sha256()), "real bytecode fingerprint differs");
        Files.delete(out.resolve("DeltaFixture.class"));
        Files.delete(out);
        Files.delete(src);
        Files.delete(src.getParent());
        Files.delete(root);
    }

    private static void compileFixture(Path out, Path source) throws IOException, InterruptedException {
        String executable = System.getProperty("os.name").startsWith("Windows")
                ? "javac.exe" : "javac";
        Path javac = Path.of(System.getProperty("java.home"), "bin", executable);
        Process process = new ProcessBuilder(javac.toString(), "--release", "21",
                "-d", out.toString(), source.toString())
                .redirectErrorStream(true).start();
        String log;
        try (var input = process.getInputStream()) {
            log = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        if (process.waitFor() != 0) throw new AssertionError("fixture javac failed: " + log);
    }

    private static void yes(boolean value, String what) {
        checks++;
        if (!value) throw new AssertionError(what);
    }

    private static void eq(Object expected, Object actual, String what) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(what + " expected=" + expected + " actual=" + actual);
        }
    }

    @FunctionalInterface
    private interface Checked {
        void run() throws Exception;
    }

    private static void throwsType(Class<? extends Throwable> type, Checked call) {
        checks++;
        try { call.run(); }
        catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("wanted " + type.getName() + ", found " + error, error);
        }
        throw new AssertionError("wanted " + type.getName());
    }
}
