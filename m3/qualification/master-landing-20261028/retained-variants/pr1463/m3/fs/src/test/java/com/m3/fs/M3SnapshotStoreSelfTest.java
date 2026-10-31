// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CancellationException;

/** Restart, corruption, ownership and publication tests; no external database or test runner. */
public final class M3SnapshotStoreSelfTest {
    private static int checks;
    private M3SnapshotStoreSelfTest() {}
    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            if (args[0].equals("reader")) {
                try (var reader = M3SnapshotStore.reader(Path.of(args[1]), 1048576)) {
                    check(reader.head("main").orElseThrow().root().sha256().equals(args[2]));
                }
            } else reject(IOException.class, () -> {
                try (var writer = M3SnapshotStore.writer(Path.of(args[1]), 1048576)) { writer.namespace(); }
            });
            return;
        }
        Path temp = Files.createTempDirectory("mindex-store-proof-");
        Path path = temp.resolve("store");
        M3SnapshotStore.BlobRef ref;
        UUID namespace;
        byte[] source = new byte[131077];
        new java.util.Random(27).nextBytes(source);
        try {
            try (var writer = M3SnapshotStore.writer(path, 1048576)) {
                namespace = writer.namespace();
                ByteBuffer input = ByteBuffer.wrap(source); input.position(3);
                ref = writer.put("raw-v1", M3SnapshotStore.NO_DICTIONARY, input, null);
                check(input.position() == 3);
                check(ref.equals(writer.put("raw-v1", M3SnapshotStore.NO_DICTIONARY, input, null)));
                try (var files = Files.walk(path.resolve("objects"))) {
                    check(files.filter(p -> p.toString().endsWith(".blob")).count() == 1);
                }
                ByteBuffer mapped = writer.map(ref, null);
                byte[] actual = new byte[mapped.remaining()]; mapped.get(actual);
                check(Arrays.equals(actual, Arrays.copyOfRange(source, 3, source.length)));
                check(mapped.isDirect() && mapped.isReadOnly());
                var first = writer.publish("main", 0, ref, null);
                check(first.generation() == 1);
                var alias = writer.cloneSnapshot(first, "alias", 0, null);
                check(alias.root().equals(ref));
                reject(IOException.class, () -> writer.publish("main", 0, ref, null));
                check(writer.head("main").orElseThrow().equals(first));
                reject(CancellationException.class, () -> writer.publish("main", 1, ref, M3FsProgress.of(() -> true, work -> {})));
                check(writer.head("main").orElseThrow().equals(first));
                reject(IllegalArgumentException.class, () -> writer.publish("../bad", 0, ref, null));
                var foreign = new M3SnapshotStore.BlobRef(UUID.randomUUID(), ref.format(),
                        ref.dictionarySha256(), ref.sha256(), ref.length());
                reject(IllegalArgumentException.class, () -> writer.map(foreign, null));
                reject(IOException.class, () -> {
                    try (var other = M3SnapshotStore.writer(path, 1048576)) { other.namespace(); }
                });
                try (var reader = M3SnapshotStore.reader(path, 1048576)) {
                    check(reader.head("main").orElseThrow().equals(first));
                    reject(IllegalStateException.class, () -> reader.publish("main", 1, ref, null));
                }
                child("reader", path, ref.sha256()); child("writer", path, "unused");
                // Temporary/torn uncommitted files never become heads.
                Files.write(path.resolve("heads/.mindex-interrupted.tmp"), new byte[] {1, 2, 3});
                check(writer.head("main").orElseThrow().equals(first));
                var empty = writer.put("raw-v1", M3SnapshotStore.NO_DICTIONARY, ByteBuffer.allocate(0), null);
                check(writer.map(empty, null).remaining() == 0);
                check(writer.publish("main", 1, ref, null).generation() == 2);
                reject(IllegalArgumentException.class, () -> writer.put("raw-v1", "bad", input, null));
                reject(IllegalArgumentException.class, () -> writer.put("raw-v1",
                        M3SnapshotStore.NO_DICTIONARY, ByteBuffer.allocate(1048577), null));
            }
            try (var reopened = M3SnapshotStore.writer(path, 1048576)) {
                check(reopened.namespace().equals(namespace));
                check(reopened.head("main").orElseThrow().generation() == 2);
                check(reopened.head("main").orElseThrow().root().equals(ref));
                check(reopened.put("raw-v1", M3SnapshotStore.NO_DICTIONARY,
                        ByteBuffer.wrap(source).position(3), null).equals(ref));
                Path head = path.resolve("heads/main.head"); byte[] goodHead = Files.readAllBytes(head);
                Files.write(head, new byte[] {1}); reject(IOException.class, () -> reopened.head("main"));
                Files.write(head, goodHead); goodHead[10] ^= 1; Files.write(head, goodHead);
                reject(IOException.class, () -> reopened.head("main"));
                Path blob = path.resolve("objects").resolve(ref.sha256().substring(0, 2)).resolve(ref.sha256() + ".blob");
                byte[] damaged = Files.readAllBytes(blob); damaged[0] ^= 1; Files.write(blob, damaged);
                reject(IOException.class, () -> reopened.map(ref, null));
                reject(IOException.class, () -> reopened.put("raw-v1", M3SnapshotStore.NO_DICTIONARY,
                        ByteBuffer.wrap(source).position(3), null));
            }
            Path link = temp.resolve("link"); Files.createSymbolicLink(link, path);
            reject(IOException.class, () -> {
                try (var bad = M3SnapshotStore.writer(link, 1024)) { bad.namespace(); }
            });
            System.out.println("M3SnapshotStoreSelfTest checks=" + checks + " process-read/write-lock=PASS");
        } finally {
            try (var paths = Files.walk(temp)) {
                for (Path entry : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(entry);
            }
        }
    }
    private static void child(String mode, Path path, String sha) throws Exception {
        Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", System.getProperty("java.class.path"), M3SnapshotStoreSelfTest.class.getName(),
                mode, path.toString(), sha).inheritIO().start();
        check(process.waitFor() == 0);
    }
    private interface Checked { void run() throws Exception; }
    private static void reject(Class<? extends Throwable> expected, Checked action) throws Exception {
        try { action.run(); } catch (Throwable failure) {
            if (expected.isInstance(failure)) { checks++; return; } throw failure;
        }
        throw new AssertionError("expected " + expected.getName());
    }
    private static void check(boolean valid) { checks++; if (!valid) throw new AssertionError("check " + checks); }
}
