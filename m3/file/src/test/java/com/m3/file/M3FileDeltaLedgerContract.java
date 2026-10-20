// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

/** Java 21 standalone assertions: java -ea com.m3.file.M3FileDeltaLedgerContract. */
public final class M3FileDeltaLedgerContract {
    private static int checks;

    private M3FileDeltaLedgerContract() {}

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("m3-file-delta-");
        try {
            Path source = root.resolve("src/A.java");
            Path binary = root.resolve("out/A.class");
            Path config = root.resolve("pom.xml");
            Path snapshot = root.resolve("facts.m3dl");
            Files.createDirectories(source.getParent());
            Files.createDirectories(binary.getParent());
            Files.writeString(source, "class A { int f() { return 1; } }");
            Files.write(binary, new byte[] {1, 2, 3, 4});
            Files.writeString(config, "<project/>");
            List<Path> tracked = List.of(Path.of("src/A.java"), Path.of("out/A.class"), Path.of("pom.xml"));
            var first = M3FileDeltaLedger.scan(root, null, tracked);
            ok(first.slots() == 3, "initial slots");
            ok(first.changedSlots(M3FileDeltaLedger.Kind.SOURCE).length == 1, "source created");
            ok(first.changedSlots(M3FileDeltaLedger.Kind.CLASS).length == 1, "class created");
            ok(first.changedSlots(M3FileDeltaLedger.Kind.BUILD_INPUT).length == 1, "build input created");
            int sourceSlot = slot(first, "src/A.java");
            int binarySlot = slot(first, "out/A.class");
            ok((first.stateAt(sourceSlot) & M3FileDeltaLedger.CREATED) != 0, "source created mask");
            ok(first.digestAt(sourceSlot).length == 32, "SHA-256 length");

            M3FileDeltaLedger.save(snapshot, first);
            var restored = M3FileDeltaLedger.load(snapshot, root);
            ok(restored.slots() == 3 && restored.stateAt(sourceSlot) == M3FileDeltaLedger.LIVE,
                    "persistent image drops transient delta bits");
            var same = M3FileDeltaLedger.scan(root, restored,
                    List.of(Path.of("pom.xml"), Path.of("src/A.java"), Path.of("out/A.class")));
            ok(same.changedSlots(M3FileDeltaLedger.Kind.SOURCE).length == 0, "source no-op");
            ok(same.changedSlots(M3FileDeltaLedger.Kind.CLASS).length == 0, "class no-op");
            ok(same.changedSlots(M3FileDeltaLedger.Kind.BUILD_INPUT).length == 0, "config no-op");
            ok(slot(same, "src/A.java") == sourceSlot && slot(same, "out/A.class") == binarySlot,
                    "original physical slots retained");
            byte[] oldSourceDigest = same.digestAt(sourceSlot);
            oldSourceDigest[0] ^= (byte) 0xff;
            ok(!java.util.Arrays.equals(oldSourceDigest, same.digestAt(sourceSlot)),
                    "digest getter is detached");

            FileTime timestamp = Files.getLastModifiedTime(source);
            Files.writeString(source, "class A { int f() { return 2; } }");
            Files.setLastModifiedTime(source, timestamp); // same size + timestamp must not hide the edit
            var changedSource = M3FileDeltaLedger.scan(root, same, tracked);
            ok(changedSource.changedSlots(M3FileDeltaLedger.Kind.SOURCE).length == 1,
                    "same-size/same-mtime source delta");
            ok(changedSource.changedSlots(M3FileDeltaLedger.Kind.CLASS).length == 0,
                    "unchanged compiled class");
            ok((changedSource.stateAt(sourceSlot) & M3FileDeltaLedger.MODIFIED) != 0,
                    "source modified mask");
            Files.write(binary, new byte[] {1, 2, 3, 5});
            var changedClass = M3FileDeltaLedger.scan(root, changedSource, tracked);
            ok(changedClass.changedSlots(M3FileDeltaLedger.Kind.SOURCE).length == 0,
                    "source stable after class edit");
            ok(changedClass.changedSlots(M3FileDeltaLedger.Kind.CLASS).length == 1,
                    "binary class delta detected");
            ok((changedClass.stateAt(binarySlot) & M3FileDeltaLedger.MODIFIED) != 0,
                    "class modified mask");

            Files.delete(binary);
            var missing = M3FileDeltaLedger.scan(root, changedClass,
                    List.of(Path.of("src/A.java"), Path.of("pom.xml")));
            ok((missing.stateAt(binarySlot) & M3FileDeltaLedger.REMOVED) != 0,
                    "removed class tombstone");
            ok(missing.changedSlots(M3FileDeltaLedger.Kind.CLASS).length == 1,
                    "class removal enters invalidation");
            var missingAgain = M3FileDeltaLedger.scan(root, missing,
                    List.of(Path.of("src/A.java"), Path.of("pom.xml")));
            ok(missingAgain.changedSlots(M3FileDeltaLedger.Kind.CLASS).length == 0,
                    "unmodified tombstone not repeatedly emitted");
            Files.write(binary, new byte[] {4, 3, 2, 1});
            var recreated = M3FileDeltaLedger.scan(root, missingAgain, tracked);
            ok(slot(recreated, "out/A.class") == binarySlot, "recreated class reuses physical slot");
            ok((recreated.stateAt(binarySlot) & M3FileDeltaLedger.CREATED) != 0,
                    "recreated class state");

            Path big = root.resolve("large.bin");
            byte[] bigData = new byte[2 * 65536 + 7];
            bigData[bigData.length - 1] = 1;
            Files.write(big, bigData);
            var bigFirst = M3FileDeltaLedger.scan(root, null, List.of(big));
            ok(bigFirst.changedSlots(M3FileDeltaLedger.Kind.BUILD_INPUT).length == 1,
                    "multi-buffer input created");
            var bigSame = M3FileDeltaLedger.scan(root, bigFirst, List.of(big));
            ok(bigSame.changedSlots(M3FileDeltaLedger.Kind.BUILD_INPUT).length == 0,
                    "multi-buffer input unchanged");
            bigData[65536 + 1] ^= 1;
            Files.write(big, bigData);
            var bigModified = M3FileDeltaLedger.scan(root, bigSame, List.of(big));
            ok((bigModified.stateAt(0) & M3FileDeltaLedger.MODIFIED) != 0,
                    "multi-buffer interior delta detected");

            M3FileDeltaLedger.save(snapshot, recreated);
            byte[] corrupt = Files.readAllBytes(snapshot);
            corrupt[corrupt.length - 2] ^= 0x01;
            Files.write(snapshot, corrupt);
            try {
                M3FileDeltaLedger.load(snapshot, root);
                throw new AssertionError("corrupt image accepted");
            } catch (IOException expected) { checks++; }
            try {
                M3FileDeltaLedger.scan(root, recreated, List.of(Path.of("../escape.java")));
                throw new AssertionError("escaped root accepted");
            } catch (IllegalArgumentException expected) { checks++; }
            Path other = Files.createTempDirectory("m3-delta-other-");
            try {
                M3FileDeltaLedger.scan(other, recreated, List.of());
                throw new AssertionError("cross-root snapshot accepted");
            } catch (IllegalArgumentException expected) { checks++; }
            finally { Files.delete(other); }
            try {
                Path link = root.resolve("fake.class");
                Files.createSymbolicLink(link, binary);
                try {
                    M3FileDeltaLedger.scan(root, recreated, List.of(link));
                    throw new AssertionError("symlink admitted");
                } catch (IOException expected) { checks++; }
            } catch (UnsupportedOperationException | java.nio.file.FileSystemException ignored) {
                // Provider/user might lack symlink capability; all other tests still apply.
            }
            System.out.println("M3_FILE_DELTA_PASS checks=" + checks);
        } finally {
            try (var paths = Files.walk(root)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); }
                    catch (IOException ex) { throw new java.io.UncheckedIOException(ex); }
                });
            }
        }
    }

    private static int slot(M3FileDeltaLedger.Snapshot snapshot, String path) {
        int slot = snapshot.physicalSlot(Path.of(path));
        if (slot < 0) throw new AssertionError("missing tracked file " + path);
        return slot;
    }

    private static void ok(boolean good, String claim) {
        checks++;
        if (!good) throw new AssertionError(claim);
    }
}
