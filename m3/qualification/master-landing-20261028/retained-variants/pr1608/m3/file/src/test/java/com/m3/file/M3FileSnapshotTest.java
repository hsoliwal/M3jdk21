// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import static com.m3.file.M3FileSnapshot.Change.CONTENT_CHANGED;
import static com.m3.file.M3FileSnapshot.Change.CREATED;
import static com.m3.file.M3FileSnapshot.Change.DELETED;
import static com.m3.file.M3FileSnapshot.Change.METADATA_CHANGED;
import static com.m3.file.M3FileSnapshot.Change.UNCHANGED;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class M3FileSnapshotTest {
    @TempDir
    Path directory;

    @Test
    void sourceAndRealClassOutputHaveIndependentContentDeltas() throws Exception {
        Path source = directory.resolve("Example.java");
        Path classes = Files.createDirectory(directory.resolve("classes"));
        Path output = classes.resolve("Example.class");
        M3File sourceFile = M3File.of(source);
        M3File classFile = M3File.of(output);
        M3FileSnapshot missingClass = classFile.snapshot(1_048_576);
        Files.writeString(source, "public class Example { public static int value() { return 1; } }\n");
        M3FileSnapshot sourceBefore = sourceFile.snapshot(1_048_576);
        compile(source, classes);
        M3FileSnapshot classBefore = classFile.snapshot(1_048_576);
        assertEquals(CREATED, classBefore.changeFrom(missingClass));
        byte[] originalClass = Files.readAllBytes(output);
        assertEquals(0xca, Byte.toUnsignedInt(originalClass[0]));
        assertEquals(0xfe, Byte.toUnsignedInt(originalClass[1]));
        assertEquals(0xba, Byte.toUnsignedInt(originalClass[2]));
        assertEquals(0xbe, Byte.toUnsignedInt(originalClass[3]));

        Files.writeString(source, "public class Example { public static int value() { return 2; } }\n");
        Files.setLastModifiedTime(source, sourceBefore.metadata().orElseThrow().lastModified());
        M3FileSnapshot sourceAfter = sourceFile.snapshot(1_048_576);
        assertEquals(sourceBefore.metadata(), sourceAfter.metadata());
        assertEquals(CONTENT_CHANGED, sourceAfter.changeFrom(sourceBefore));
        assertEquals(UNCHANGED, classFile.snapshot(1_048_576).changeFrom(classBefore));

        compile(source, classes);
        Files.setLastModifiedTime(output, classBefore.metadata().orElseThrow().lastModified());
        M3FileSnapshot classAfter = classFile.snapshot(1_048_576);
        assertEquals(classBefore.metadata(), classAfter.metadata());
        assertEquals(CONTENT_CHANGED, classAfter.changeFrom(classBefore));
        assertNotEquals(classBefore.sha256(), classAfter.sha256());
        assertEquals(sha256(originalClass), classBefore.sha256().orElseThrow());
        assertEquals(sha256(Files.readAllBytes(output)), classAfter.sha256().orElseThrow());
    }

    @Test
    void creationDeletionAndAbsentObservationsAreDirectional() throws Exception {
        Path path = directory.resolve("generated.class");
        M3File file = M3File.of(path);
        M3FileSnapshot absent = file.snapshot(32);
        assertSame(path, absent.path());
        assertFalse(absent.exists());
        assertTrue(absent.metadata().isEmpty());
        assertTrue(absent.sha256().isEmpty());
        assertTrue(absent.sha256Bytes().isEmpty());
        assertEquals(UNCHANGED, file.snapshot(32).changeFrom(absent));

        Files.write(path, new byte[] {0, (byte) 0xff, 1});
        M3FileSnapshot present = file.snapshot(32);
        assertTrue(present.exists());
        assertEquals(CREATED, present.changeFrom(absent));
        assertEquals(DELETED, absent.changeFrom(present));
        Files.delete(path);
        assertEquals(DELETED, file.snapshot(32).changeFrom(present));
        assertTrue(present.exists());
    }

    @Test
    void missingAncestorIsAbsentAndExistingReadApisStillThrow() throws Exception {
        M3File file = M3File.of(directory.resolve("absent/file.java"));
        assertFalse(file.snapshot(0).exists());
        assertThrows(NoSuchFileException.class, file::metadata);
        assertThrows(NoSuchFileException.class, file::openReadChannel);
        assertThrows(NoSuchFileException.class, () -> file.readAllBytes(0));
    }

    @Test
    void metadataOnlyChangesRetainContentIdentity() throws Exception {
        Path path = Files.write(directory.resolve("metadata.java"), new byte[] {1, 2, 3});
        M3File file = M3File.of(path);
        M3FileSnapshot before = file.snapshot(3);
        FileTime changed = FileTime.fromMillis(before.metadata().orElseThrow()
                .lastModified().toMillis() + 60_000);
        Files.setLastModifiedTime(path, changed);
        M3FileSnapshot after = file.snapshot(3);
        assertNotEquals(before.metadata(), after.metadata());
        assertEquals(before.sha256(), after.sha256());
        assertEquals(METADATA_CHANGED, after.changeFrom(before));
        assertEquals(UNCHANGED, file.snapshot(3).changeFrom(after));
    }

    @Test
    void contentChangesTakePriorityOverMetadata() throws Exception {
        Path path = Files.writeString(directory.resolve("priority.java"), "a");
        M3File file = M3File.of(path);
        M3FileSnapshot before = file.snapshot(32);
        Files.writeString(path, "longer");
        assertEquals(CONTENT_CHANGED, file.snapshot(32).changeFrom(before));
    }

    @Test
    void completeDigestIsIndependentOfReturnedArraysAndLaterDiskChanges() throws Exception {
        byte[] bytes = new byte[16_391];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) (i * 31);
        Path path = Files.write(directory.resolve("binary.class"), bytes);
        M3FileSnapshot snapshot = M3File.of(path).snapshot(bytes.length);
        String expected = sha256(bytes);
        assertEquals(expected, snapshot.sha256().orElseThrow());
        byte[] digest = snapshot.sha256Bytes().orElseThrow();
        assertArrayEquals(MessageDigest.getInstance("SHA-256").digest(bytes), digest);
        digest[0] ^= 0x7f;
        assertNotEquals(HexFormat.of().formatHex(digest), snapshot.sha256().orElseThrow());
        Files.writeString(path, "changed");
        assertEquals(expected, snapshot.sha256().orElseThrow());
        assertEquals(bytes.length, snapshot.metadata().orElseThrow().size());
    }

    @Test
    void emptyExactAndLongBudgetsAreCheckedWithoutPayloadSizedAllocation() throws Exception {
        Path path = Files.createFile(directory.resolve("budget"));
        M3File file = M3File.of(path);
        assertEquals(sha256(new byte[0]), file.snapshot(0).sha256().orElseThrow());
        assertThrows(IllegalArgumentException.class, () -> file.snapshot(-1));
        assertThrows(IllegalArgumentException.class,
                () -> M3File.of(directory.resolve("missing")).snapshot(-1));
        Files.write(path, new byte[8193]);
        assertThrows(IOException.class, () -> file.snapshot(8192));
        assertThrows(IOException.class, () -> file.snapshot(0));
        assertEquals(file.snapshot(8193).sha256(), file.snapshot(Long.MAX_VALUE).sha256());
    }

    @Test
    void differentPathsAndAliasesCannotAuthorizeReuse() throws Exception {
        Path first = Files.writeString(directory.resolve("first"), "same");
        Path second = Files.writeString(directory.resolve("second"), "same");
        M3FileSnapshot before = M3File.of(first).snapshot(32);
        M3FileSnapshot other = M3File.of(second).snapshot(32);
        assertEquals(before.sha256(), other.sha256());
        assertThrows(IllegalArgumentException.class, () -> other.changeFrom(before));
        assertThrows(NullPointerException.class, () -> before.changeFrom(null));
        M3FileSnapshot samePath = M3File.of(Path.of(first.toString())).snapshot(32);
        assertEquals(UNCHANGED, samePath.changeFrom(before));
        M3FileSnapshot alias = M3File.of(directory.resolve(".").resolve("first")).snapshot(32);
        assertThrows(IllegalArgumentException.class, () -> alias.changeFrom(before));
    }

    @Test
    void symlinksAndDirectoriesRemainRefused() throws Exception {
        Path target = Files.writeString(directory.resolve("target"), "payload");
        Path link = Files.createSymbolicLink(directory.resolve("link"), target);
        M3File file = M3File.of(link);
        assertTrue(file.metadata().symbolicLink());
        assertThrows(IOException.class, () -> file.snapshot(1_048_576));
        assertThrows(IOException.class, file::openReadChannel);
        assertThrows(IOException.class, () -> file.readAllBytes(1_048_576));
        Files.delete(target);
        assertThrows(IOException.class, () -> file.snapshot(1_048_576));
        assertThrows(IOException.class, () -> M3File.of(directory).snapshot(Long.MAX_VALUE));
    }

    @Test
    void originalReadAndMetadataContractsRemainIntact() throws Exception {
        Path path = directory.resolve("original");
        byte[] bytes = "source \u03bb".getBytes(StandardCharsets.UTF_8);
        Files.write(path, bytes);
        M3File file = M3File.of(path);
        assertSame(path, file.path());
        assertArrayEquals(bytes, file.readAllBytes(bytes.length));
        assertThrows(IOException.class, () -> file.readAllBytes(bytes.length - 1));
        assertThrows(IllegalArgumentException.class, () -> file.readAllBytes(-1));
        try (var input = file.openInputStream()) {
            assertArrayEquals(bytes, input.readAllBytes());
        }
        try (var channel = file.openReadChannel()) {
            assertTrue(channel.isOpen());
            assertEquals(bytes.length, channel.size());
        }
        Files.writeString(path, "x");
        assertEquals(1, file.metadata().size());
        assertThrows(NullPointerException.class, () -> M3File.of(null));
    }

    private static void compile(Path source, Path classes) {
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new AssertionError("a complete Java 21 JDK is required");
        var diagnostics = new ByteArrayOutputStream();
        assertEquals(0, compiler.run(null, diagnostics, diagnostics,
                "--release", "21", "-proc:none", "-d", classes.toString(), source.toString()),
                () -> diagnostics.toString(StandardCharsets.UTF_8));
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
