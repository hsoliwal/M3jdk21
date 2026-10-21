// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Modified 2026-10-21 by the Synexia m3-project-delta recipe: timestamp and image-migration contracts.
package com.m3.file;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.time.Instant;
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
            Files.setLastModifiedTime(source, FileTime.from(Instant.ofEpochSecond(1_700_000_000L, 123_456_789)));
            FileTime initialTime = Files.getLastModifiedTime(source);
            M3File.Observation observation = M3File.of(source).observe(Long.MAX_VALUE);
            ok(observation.lastModified().equals(initialTime), "M3File observation retains checked timestamp");
            ok(observation.fingerprint().equals(M3File.of(source).fingerprint(Long.MAX_VALUE)),
                    "M3File observation preserves original fingerprint contract");
            ok(observation.fingerprint().size() == Files.size(source), "observation retains exact byte size");
            try {
                M3File.of(source).observe(Files.size(source) - 1);
                throw new AssertionError("observation exceeded byte budget");
            } catch (IOException expected) { checks++; }
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
            ok(first.hasLastModifiedAt(sourceSlot) && first.lastModifiedAt(sourceSlot).equals(initialTime),
                    "ledger retains observed timestamp precision");
            ok(java.util.Arrays.equals(first.digestAt(sourceSlot),
                    MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(source))),
                    "M3File digest words retain standard SHA-256 byte order in ledger");

            M3FileDeltaLedger.save(snapshot, first);
            ok(ByteBuffer.wrap(Files.readAllBytes(snapshot)).getInt(4) == 2, "writer emits version 2");
            var restored = M3FileDeltaLedger.load(snapshot, root);
            ok(restored.slots() == 3 && restored.stateAt(sourceSlot) == M3FileDeltaLedger.LIVE,
                    "persistent image drops transient delta bits");
            ok(restored.lastModifiedAt(sourceSlot).equals(initialTime), "version-2 timestamp round trip");
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

            Path legacyImage = root.resolve("version-1.m3dl");
            writeFixture(legacyImage, first, 1, -1, 0, 0);
            var legacy = M3FileDeltaLedger.load(legacyImage, root);
            ok(legacy.slots() == first.slots() && legacy.sizeAt(sourceSlot) == first.sizeAt(sourceSlot)
                    && java.util.Arrays.equals(legacy.digestAt(sourceSlot), first.digestAt(sourceSlot)),
                    "version-1 content and physical slots are readable");
            ok(!legacy.hasLastModifiedAt(sourceSlot), "version-1 timestamp is explicitly unknown");
            try {
                legacy.lastModifiedAt(sourceSlot);
                throw new AssertionError("legacy image fabricated a timestamp");
            } catch (IllegalStateException expected) { checks++; }
            Path migratedImage = root.resolve("migrated.m3dl");
            M3FileDeltaLedger.save(migratedImage, legacy);
            var migrated = M3FileDeltaLedger.load(migratedImage, root);
            ok(!migrated.hasLastModifiedAt(sourceSlot), "version-2 preserves an unobserved legacy timestamp");
            var upgraded = M3FileDeltaLedger.scan(root, migrated, tracked);
            ok(upgraded.hasLastModifiedAt(sourceSlot) && upgraded.lastModifiedAt(sourceSlot).equals(initialTime),
                    "rescan obtains legacy timestamp without fabrication");
            ok(upgraded.changedSlots(M3FileDeltaLedger.Kind.SOURCE).length == 0
                    && upgraded.changedSlots(M3FileDeltaLedger.Kind.CLASS).length == 0
                    && upgraded.changedSlots(M3FileDeltaLedger.Kind.BUILD_INPUT).length == 0,
                    "legacy timestamp migration does not create content deltas");
            var upgradedWithTimePolicy = M3FileDeltaLedger.scan(root, migrated, tracked, true);
            ok(!upgradedWithTimePolicy.changedAt(sourceSlot)
                    && upgradedWithTimePolicy.hasLastModifiedAt(sourceSlot),
                    "timestamp policy does not fabricate a delta for unknown legacy time");

            Path fixture = root.resolve("format-fixture.m3dl");
            writeFixture(fixture, first, 2, sourceSlot, -1, 987_654_321);
            var precise = M3FileDeltaLedger.load(fixture, root);
            FileTime preciseTime = FileTime.from(Instant.ofEpochSecond(-1, 987_654_321));
            ok(precise.lastModifiedAt(sourceSlot).equals(preciseTime),
                    "negative epoch and submillisecond precision survive format loading");
            M3FileDeltaLedger.save(fixture, precise);
            ok(M3FileDeltaLedger.load(fixture, root).lastModifiedAt(sourceSlot).equals(preciseTime),
                    "negative epoch and nanoseconds survive format round trip");
            long[][] invalidTimes = {{0, -2}, {0, 1_000_000_000}, {1, -1}, {Long.MAX_VALUE, 0}};
            for (long[] invalid : invalidTimes) {
                writeFixture(fixture, first, 2, sourceSlot, invalid[0], (int) invalid[1]);
                expectLoadFailure(fixture, root, "invalid sealed timestamp accepted");
            }
            writeFixture(fixture, first, 3, sourceSlot, 0, 0);
            expectLoadFailure(fixture, root, "unknown image version accepted");
            int timestampOffset = writeFixture(fixture, first, 2, sourceSlot, 0, 123);
            byte[] tamperedTimestamp = Files.readAllBytes(fixture);
            tamperedTimestamp[timestampOffset + 7] ^= 1; // Still valid metadata, but no longer sealed.
            Files.write(fixture, tamperedTimestamp);
            expectLoadFailure(fixture, root, "unsealed timestamp change accepted");

            Files.setLastModifiedTime(source, FileTime.from(initialTime.toInstant().plusSeconds(2)));
            FileTime touchedTime = Files.getLastModifiedTime(source);
            ok(!touchedTime.equals(initialTime), "timestamp-only fixture changes observed time");
            var touched = M3FileDeltaLedger.scan(root, same, tracked);
            ok(touched.lastModifiedAt(sourceSlot).equals(touchedTime), "timestamp-only observation refreshes metadata");
            ok(!touched.changedAt(sourceSlot)
                    && java.util.Arrays.equals(touched.digestAt(sourceSlot), same.digestAt(sourceSlot)),
                    "timestamp-only edit leaves content unchanged");
            var touchedWithTimePolicy = M3FileDeltaLedger.scan(root, same, tracked, true);
            ok((touchedWithTimePolicy.stateAt(sourceSlot) & M3FileDeltaLedger.MODIFIED) != 0
                    && java.util.Arrays.equals(touchedWithTimePolicy.digestAt(sourceSlot), same.digestAt(sourceSlot)),
                    "explicit timestamp policy selects a touch without changing content identity");
            var stableWithTimePolicy = M3FileDeltaLedger.scan(root, touchedWithTimePolicy, tracked, true);
            ok(!stableWithTimePolicy.changedAt(sourceSlot), "timestamp policy does not repeat an unchanged touch");
            ok(M3File.contentDelta(observation.fingerprint(), M3File.of(source).observe(Long.MAX_VALUE).fingerprint()) == 0,
                    "observation timestamp is excluded from original content delta");
            same = touched;
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
            ok(changedSource.lastModifiedAt(sourceSlot).equals(same.lastModifiedAt(sourceSlot)),
                    "same timestamp and size do not hide changed SHA-256");
            var changedWithTimePolicy = M3FileDeltaLedger.scan(root, same, tracked, true);
            ok((changedWithTimePolicy.stateAt(sourceSlot) & M3FileDeltaLedger.MODIFIED) != 0,
                    "timestamp policy still detects same-size same-time content mutation");
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
            ok(missing.lastModifiedAt(binarySlot).equals(changedClass.lastModifiedAt(binarySlot)),
                    "removed class retains last observed timestamp");
            M3FileDeltaLedger.save(snapshot, missing);
            var savedTombstone = M3FileDeltaLedger.load(snapshot, root);
            ok(!savedTombstone.liveAt(binarySlot)
                    && savedTombstone.lastModifiedAt(binarySlot).equals(missing.lastModifiedAt(binarySlot)),
                    "tombstone timestamp survives persistence");
            var missingAgain = M3FileDeltaLedger.scan(root, missing,
                    List.of(Path.of("src/A.java"), Path.of("pom.xml")));
            ok(missingAgain.changedSlots(M3FileDeltaLedger.Kind.CLASS).length == 0,
                    "unmodified tombstone not repeatedly emitted");
            Files.write(binary, new byte[] {4, 3, 2, 1});
            Files.setLastModifiedTime(binary,
                    FileTime.from(missing.lastModifiedAt(binarySlot).toInstant().plusSeconds(3)));
            var recreated = M3FileDeltaLedger.scan(root, missingAgain, tracked);
            ok(slot(recreated, "out/A.class") == binarySlot, "recreated class reuses physical slot");
            ok((recreated.stateAt(binarySlot) & M3FileDeltaLedger.CREATED) != 0,
                    "recreated class state");
            ok(recreated.lastModifiedAt(binarySlot).equals(Files.getLastModifiedTime(binary))
                    && !recreated.lastModifiedAt(binarySlot).equals(missing.lastModifiedAt(binarySlot)),
                    "recreated class replaces tombstone timestamp");

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

    /** Independent fixture writer pins the prior format and supplies hostile but correctly sealed rows. */
    private static int writeFixture(Path image, M3FileDeltaLedger.Snapshot snapshot, int version,
            int overriddenSlot, long seconds, int nanos) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int timestampOffset = -1;
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(0x4d33444c);
            out.writeInt(version);
            out.writeUTF(snapshot.root().toString());
            out.writeInt(snapshot.slots());
            for (int i = 0; i < snapshot.slots(); i++) {
                out.writeUTF(snapshot.pathAt(i));
                out.writeByte(snapshot.kindAt(i).ordinal());
                out.writeLong(snapshot.sizeAt(i));
                if (version >= 2) {
                    if (i == overriddenSlot) {
                        timestampOffset = bytes.size();
                        out.writeLong(seconds);
                        out.writeInt(nanos);
                    } else {
                        Instant time = snapshot.hasLastModifiedAt(i) ? snapshot.lastModifiedAt(i).toInstant() : null;
                        out.writeLong(time == null ? 0 : time.getEpochSecond());
                        out.writeInt(time == null ? -1 : time.getNano());
                    }
                }
                out.writeLong(snapshot.stateAt(i) & M3FileDeltaLedger.LIVE);
                out.write(snapshot.digestAt(i));
            }
        }
        byte[] payload = bytes.toByteArray();
        try (var output = Files.newOutputStream(image)) {
            output.write(payload);
            output.write(MessageDigest.getInstance("SHA-256").digest(payload));
        }
        return timestampOffset;
    }

    private static void expectLoadFailure(Path image, Path root, String claim) throws IOException {
        try {
            M3FileDeltaLedger.load(image, root);
            throw new AssertionError(claim);
        } catch (IOException expected) { checks++; }
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
