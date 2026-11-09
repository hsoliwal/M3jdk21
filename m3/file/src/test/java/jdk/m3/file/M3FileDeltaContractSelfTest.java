// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.file;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Objects;

/** Standalone Java 21 contract checks; uses actual M3NioFile rather than a fake I/O provider. */
public final class M3FileDeltaContractSelfTest {
    private static int checks;

    private M3FileDeltaContractSelfTest() { }

    public static void main(String[] args) throws Exception {
        var source1 = M3NioFileDelta.fromBytes(M3NioFileDelta.Kind.SOURCE, "src/A.java",
                "class A { int n(){return 1;} }".getBytes(StandardCharsets.UTF_8), null);
        var source2 = M3NioFileDelta.fromBytes(M3NioFileDelta.Kind.SOURCE, "src/A.java",
                "class A { int n(){return 2;} }".getBytes(StandardCharsets.UTF_8), null);
        var class1 = M3NioFileDelta.fromBytes(M3NioFileDelta.Kind.CLASS, "out/A.class",
                new byte[]{0, (byte) 0xca, (byte) 0xfe}, hex(4));
        var class2 = M3NioFileDelta.fromBytes(M3NioFileDelta.Kind.CLASS, "out/A.class",
                new byte[]{0, (byte) 0xca, (byte) 0xff}, hex(4));
        var before = M3NioFileDelta.snapshot(List.of(source1, class1), hex(11));
        var shuffled = M3NioFileDelta.snapshot(List.of(class1, source1), hex(11));
        equal(before.rootSha256(), shuffled.rootSha256());
        yes(M3NioFileDelta.compare(before, shuffled).sameEvidence());
        var after = M3NioFileDelta.snapshot(List.of(class2, source2), hex(11));
        var changed = M3NioFileDelta.compare(before, after);
        equal(2, changed.changes().size());
        equal(M3NioFileDelta.Kind.SOURCE, changed.changes().get(0).kind());
        equal(M3NioFileDelta.AbiChange.SAME_ATTESTED, changed.changes().get(1).abi());
        yes(!changed.sameEvidence());

        var unknown = M3NioFileDelta.fromBytes(M3NioFileDelta.Kind.CLASS, "out/A.class",
                new byte[]{4, 5, 6}, null);
        equal(M3NioFileDelta.AbiChange.UNKNOWN,
                M3NioFileDelta.compare(before, M3NioFileDelta.snapshot(List.of(source1, unknown), hex(11)))
                        .changes().getFirst().abi());
        yes(M3NioFileDelta.compare(before,
                M3NioFileDelta.snapshot(List.of(source1, class1), hex(12))).contextChanged());
        equal(M3NioFileDelta.Change.REMOVED,
                M3NioFileDelta.compare(before, M3NioFileDelta.snapshot(List.of(), hex(11)))
                        .changes().getFirst().change());
        rejected(IllegalArgumentException.class, () -> M3NioFileDelta.snapshot(List.of(source1, source1), hex(11)));
        rejected(IllegalArgumentException.class, () -> M3NioFileDelta.fromBytes(
                M3NioFileDelta.Kind.SOURCE, "../escape", new byte[0], null));
        rejected(IllegalArgumentException.class, () -> M3NioFileDelta.snapshot(List.of(), "invalid"));

        Path temp = Files.createTempDirectory("m3-source-class-delta-");
        Path file = temp.resolve("A.java");
        Files.writeString(file, "return 1;");
        FileTime stamp = Files.getLastModifiedTime(file);
        var diskBefore = M3NioFileDelta.capture(temp, M3NioFileDelta.Kind.SOURCE, "A.java", 64, null);
        Files.writeString(file, "return 2;");
        Files.setLastModifiedTime(file, stamp);
        var diskAfter = M3NioFileDelta.capture(temp, M3NioFileDelta.Kind.SOURCE, "A.java", 64, null);
        yes(!diskBefore.sha256().equals(diskAfter.sha256()));
        equal(diskBefore.length(), diskAfter.length());
        rejected(IOException.class, () -> M3NioFileDelta.capture(temp, M3NioFileDelta.Kind.SOURCE, "A.java", 2, null));
        Files.delete(file);
        Files.delete(temp);
        System.out.println("M3FILE_DELTA_CONTRACT_PASS checks=" + checks);
    }

    private static String hex(int n) { return String.format(java.util.Locale.ROOT, "%064x", n); }
    private static void yes(boolean success) {
        checks++;
        if (!success) throw new AssertionError("assertion failed");
    }
    private static void equal(Object wanted, Object actual) {
        checks++;
        if (!Objects.equals(wanted, actual)) {
            throw new AssertionError("expected=" + wanted + " actual=" + actual);
        }
    }
    @FunctionalInterface private interface Action { void execute() throws Exception; }
    private static void rejected(Class<? extends Throwable> type, Action action) {
        checks++;
        try { action.execute(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("wrong refusal type", failure);
        }
        throw new AssertionError("missing expected refusal " + type.getName());
    }
}
