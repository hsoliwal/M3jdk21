/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.m3.fs.vfs.Capability;
import com.m3.fs.vfs.FileObject;
import com.m3.fs.vfs.FileSystemException;
import com.m3.fs.vfs.FileType;
import com.m3.fs.vfs.Selectors;
import com.m3.fs.vfs.impl.DefaultFileSystemManager;
import com.m3.fs.vfs.provider.UriParser;
import com.m3.fs.vfs.provider.local.DefaultLocalFileProvider;
import com.m3.fs.vfs.provider.ram.RamFileProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Public VFS behavior checked against the existing native SQLite owner. */
class DatabaseFileProviderTest {
    @TempDir Path temp;

    @BeforeAll
    static void requireNativeOwner() {
        org.junit.jupiter.api.Assumptions.assumeTrue(Boolean.getBoolean("m3.fs.test"));
        assertTrue(M3FS.nativeAvailable(), "real native SQLite owner is required");
        assertTrue(M3FS.supportsCheckedInodeIo());
        assertTrue(M3FS.supportsSetMetadata());
    }

    @Test
    void inheritedCreationCopyRenameAndDeletePersistInTheNativeDatabase() throws Exception {
        Path database = database("persistent.sqlite");
        byte[] bytes = new byte[150_003];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) (i * 37);
        Path local = temp.resolve("source.bin");
        Files.write(local, bytes);
        try (DefaultFileSystemManager manager = manager()) {
            FileObject root = manager.resolveFile(address(database, "/"));
            FileObject nested = root.resolveFile("one/two");
            nested.createFolder();
            FileObject file = nested.resolveFile("data.bin");
            file.copyFrom(manager.toFileObject(local.toFile()), Selectors.SELECT_SELF);
            assertArrayEquals(bytes, read(file));
            file.createFile(); // Existing content must survive this inherited convenience method.
            assertArrayEquals(bytes, read(file));
            FileObject copy = root.resolveFile("copy");
            copy.copyFrom(root.resolveFile("one"), Selectors.SELECT_ALL);
            assertArrayEquals(bytes, read(copy.resolveFile("two/data.bin")));
            FileObject moved = nested.resolveFile("moved.bin");
            file.moveTo(moved);
            assertFalse(file.exists());
            assertArrayEquals(bytes, read(moved));
            assertEquals(3, copy.deleteAll());
            assertFalse(copy.exists());
        }
        try (M3FS nativeOwner = M3FS.open(database)) {
            assertEquals(M3FS.Kind.DIRECTORY, nativeOwner.stat("/one/two").kind());
            assertArrayEquals(bytes, nativeOwner.read("/one/two/moved.bin", 0, bytes.length + 1));
            assertThrows(NoSuchFileException.class, () -> nativeOwner.stat("/one/two/data.bin"));
            assertThrows(NoSuchFileException.class, () -> nativeOwner.stat("/copy"));
            nativeOwner.verify();
        }
        try (DefaultFileSystemManager reopened = manager()) {
            assertArrayEquals(bytes, read(reopened.resolveFile(address(database, "/one/two/moved.bin"))));
        }
    }

    @Test
    void emptyMissingTruncateAppendAndUnsignedReadsUseOrdinaryStreamContracts() throws Exception {
        Path database = database("streams.sqlite");
        try (DefaultFileSystemManager manager = manager()) {
            FileObject root = manager.resolveFile(address(database, "/"));
            assertEquals(0, root.getChildren().length);
            FileObject file = root.resolveFile("new/empty");
            assertEquals(FileType.IMAGINARY, file.getType());
            assertThrows(IOException.class, () -> file.getContent().getInputStream());
            file.createFile();
            assertEquals(0, file.getContent().getSize());
            try (InputStream input = file.getContent().getInputStream(1)) {
                assertEquals(0, input.read(new byte[0]));
                assertEquals(-1, input.read());
                assertEquals(-1, input.read(new byte[3]));
            }
            write(file, false, new byte[] {(byte) 255, 1, 2, 3});
            write(file, true, new byte[] {4, 5});
            try (InputStream input = file.getContent().getInputStream(1)) {
                assertEquals(255, input.read());
                assertArrayEquals(new byte[] {1, 2, 3, 4, 5}, input.readAllBytes());
            }
            write(file, false, new byte[] {7});
            assertArrayEquals(new byte[] {7}, read(file));
            try (OutputStream output = file.getContent().getOutputStream()) {
                assertEquals(0, file.getContent().getSize());
                output.write(new byte[0]);
            }
            assertArrayEquals(new byte[0], read(file));
            assertThrows(IOException.class, () -> root.getContent().getInputStream());
            assertThrows(IOException.class, () -> root.getContent().getOutputStream());
            assertFalse(root.getFileSystem().hasCapability(Capability.RANDOM_ACCESS_READ));
            assertFalse(root.getFileSystem().hasCapability(Capability.RANDOM_ACCESS_WRITE));
            assertFalse(root.getFileSystem().hasCapability(Capability.ATTRIBUTES));
        }
    }

    @Test
    void readOnlyMountRejectsMutationsAndMetadataUsesMilliseconds() throws Exception {
        Path database = database("readonly.sqlite");
        long modified = 1_700_000_000_123L;
        try (M3FS nativeOwner = M3FS.open(database)) {
            nativeOwner.mkdir("/folder", 0755);
            nativeOwner.createFile("/folder/file", 0000);
            nativeOwner.write("/folder/file", 0, new byte[] {9, 8});
        }
        try (DefaultFileSystemManager manager = manager()) {
            FileObject file = manager.resolveFile(address(database, "/folder/file"));
            assertTrue(file.isReadable(), "mode metadata is not an authorization model");
            assertTrue(file.isWriteable());
            assertFalse(file.setWritable(false, true), "permission mutation is unsupported");
            file.getContent().setLastModifiedTime(modified);
            assertEquals(modified, file.getContent().getLastModifiedTime());
            assertThrows(IOException.class, () -> file.getContent().setLastModifiedTime(-1));
            assertThrows(IOException.class, () -> file.getContent().setLastModifiedTime(Long.MAX_VALUE));
            FileObject readOnly = manager.resolveFile(address(database, "/folder/file"),
                    DatabaseFileProvider.options(true));
            assertArrayEquals(new byte[] {9, 8}, read(readOnly));
            assertFalse(readOnly.isWriteable());
            assertFalse(readOnly.getFileSystem().hasCapability(Capability.WRITE_CONTENT));
            assertFalse(readOnly.getFileSystem().hasCapability(Capability.SET_LAST_MODIFIED_FILE));
            assertThrows(IOException.class, () -> readOnly.getContent().getOutputStream());
            assertThrows(IOException.class, () -> readOnly.getContent().getOutputStream(true));
            assertThrows(IOException.class, readOnly::delete);
            assertThrows(IOException.class, () -> readOnly.getParent().resolveFile("new").createFolder());
            assertThrows(IOException.class, () -> readOnly.getContent().setLastModifiedTime(1));
            assertThrows(IOException.class, () -> readOnly.moveTo(readOnly.getParent().resolveFile("moved")));
        }
        try (M3FS nativeOwner = M3FS.openReadOnly(database)) {
            assertEquals(modified * 1_000_000L, nativeOwner.stat("/folder/file").modifiedTimeNanos());
            assertEquals(0000, nativeOwner.stat("/folder/file").mode());
            assertArrayEquals(new byte[] {9, 8}, nativeOwner.read("/folder/file", 0, 3));
        }
    }

    @Test
    void namesRoundTripThroughInheritedListingAndDatabaseUrisStayDistinct() throws Exception {
        Path first = database("database % ! with spaces.sqlite");
        Path second = database("second.sqlite");
        List<String> names = List.of("with space", "100%literal", "literal!bang", "β中文😀", "scheme:name", "q?#");
        try (M3FS nativeOwner = M3FS.open(first)) {
            for (String name : names) {
                nativeOwner.createFile("/" + name, 0644);
                nativeOwner.write("/" + name, 0, name.getBytes(StandardCharsets.UTF_8));
            }
        }
        try (DefaultFileSystemManager manager = manager()) {
            FileObject root = manager.resolveFile(address(first, "/"));
            Set<String> found = Arrays.stream(root.getChildren()).map(child -> {
                try {
                    String name = UriParser.decode(child.getName().getBaseName());
                    assertArrayEquals(name.getBytes(StandardCharsets.UTF_8), read(child));
                    assertArrayEquals(read(child), read(manager.resolveFile(child.getName().getURI())));
                    return name;
                } catch (IOException failure) {
                    throw new java.io.UncheckedIOException(failure);
                }
            }).collect(Collectors.toSet());
            assertEquals(Set.copyOf(names), found);
            FileObject otherRoot = manager.resolveFile(address(second, "/"));
            assertNotSame(root.getFileSystem(), otherRoot.getFileSystem());
            assertNotEquals(root.getName().getRootURI(), otherRoot.getName().getRootURI());
            assertEquals(0, otherRoot.getChildren().length);
            assertThrows(IOException.class, () -> root.resolveFile("../../outside"));
            assertThrows(IOException.class, () -> root.resolveFile("x".repeat(256)).createFile());
        }
    }

    @Test
    void backingMustBeAnExistingLocalRegularM3fsDatabase() throws Exception {
        Path corrupt = temp.resolve("ordinary.bin");
        Files.writeString(corrupt, "not an M3FS database");
        Path missing = temp.resolve("missing.sqlite");
        try (DefaultFileSystemManager manager = manager()) {
            assertThrows(IOException.class, () -> manager.resolveFile(address(missing, "/")));
            assertFalse(Files.exists(missing));
            assertThrows(IOException.class, () -> manager.resolveFile(address(temp, "/")));
            assertThrows(IOException.class, () -> manager.resolveFile(address(corrupt, "/")));
            FileObject ram = manager.resolveFile("ram:/database");
            write(ram, false, new byte[] {1, 2});
            assertThrows(IOException.class, () -> manager.resolveFile("m3db:ram:/database!/"));
        }
        assertEquals("not an M3FS database", Files.readString(corrupt));
    }

    @Test
    void checkedStreamsNeverReadOrModifyAReplacementInode() throws Exception {
        Path database = database("replacement.sqlite");
        try (M3FS nativeOwner = M3FS.open(database); DefaultFileSystemManager manager = manager()) {
            nativeOwner.createFile("/read", 0644);
            nativeOwner.write("/read", 0, new byte[] {1, 2, 3});
            nativeOwner.createFile("/write", 0644);
            FileObject read = manager.resolveFile(address(database, "/read"));
            FileObject write = manager.resolveFile(address(database, "/write"));
            try (InputStream input = read.getContent().getInputStream(1)) {
                assertEquals(1, input.read());
                nativeOwner.unlink("/read");
                nativeOwner.createFile("/read", 0644);
                nativeOwner.write("/read", 0, new byte[] {8});
                assertThrows(IOException.class, input::read);
            }
            OutputStream output = write.getContent().getOutputStream(true, 1);
            nativeOwner.unlink("/write");
            nativeOwner.createFile("/write", 0644);
            nativeOwner.write("/write", 0, new byte[] {9});
            try {
                assertThrows(IOException.class, () -> output.write(new byte[] {4, 5}));
            } finally {
                output.close();
            }
            assertArrayEquals(new byte[] {8}, nativeOwner.read("/read", 0, 4));
            assertArrayEquals(new byte[] {9}, nativeOwner.read("/write", 0, 4));
        }
    }

    @Test
    void independentMountedOwnersAppendCompleteRecordsWithoutLostWrites() throws Exception {
        Path database = database("append.sqlite");
        try (M3FS nativeOwner = M3FS.open(database)) {
            nativeOwner.createFile("/log", 0644);
        }
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> appendRecords(database, 1, start));
            var second = executor.submit(() -> appendRecords(database, 2, start));
            start.countDown();
            assertEquals(40, first.get());
            assertEquals(40, second.get());
        }
        try (M3FS nativeOwner = M3FS.openReadOnly(database)) {
            byte[] content = nativeOwner.read("/log", 0, 1000);
            assertEquals(640, content.length);
            int[] counts = new int[3];
            ByteBuffer records = ByteBuffer.wrap(content);
            while (records.hasRemaining()) {
                int owner = records.getInt();
                assertTrue(owner == 1 || owner == 2);
                assertEquals(counts[owner]++, records.getInt());
            }
            assertArrayEquals(new int[] {0, 40, 40}, counts);
        }
    }

    @Test
    void hardLinkMovesAreNoOpsAndSymbolicLinksAreNotDereferenced() throws Exception {
        Path database = database("links.sqlite");
        try (M3FS nativeOwner = M3FS.open(database); DefaultFileSystemManager manager = manager()) {
            nativeOwner.createFile("/file", 0644);
            nativeOwner.write("/file", 0, new byte[] {7});
            nativeOwner.link("/file", "/alias");
            nativeOwner.symlink("/file", "/symbolic");
            FileObject file = manager.resolveFile(address(database, "/file"));
            FileObject alias = manager.resolveFile(address(database, "/alias"));
            file.moveTo(alias);
            assertTrue(file.exists());
            assertTrue(alias.exists());
            file.moveTo(file);
            assertTrue(file.exists());
            try (DefaultFileSystemManager other = manager()) {
                file.moveTo(other.resolveFile(address(database, "/alias")));
                file.moveTo(other.resolveFile(address(database, "/file")));
            }
            assertEquals(2, nativeOwner.stat("/file").linkCount());
            FileObject symbolic = manager.resolveFile(address(database, "/symbolic"));
            assertTrue(symbolic.isSymbolicLink());
            assertFalse(symbolic.isReadable());
            assertFalse(symbolic.isWriteable());
            assertThrows(IOException.class, () -> symbolic.getContent().getInputStream());
            assertThrows(IOException.class, () -> symbolic.getContent().getOutputStream());
            assertTrue(symbolic.delete());
            assertArrayEquals(new byte[] {7}, nativeOwner.read("/file", 0, 3));
        }
    }

    @Test
    void rootAndDescendantMoveGuardsRejectBeforeDeletingAnyChildren() throws Exception {
        Path database = database("root.sqlite");
        try (DefaultFileSystemManager manager = manager()) {
            FileObject root = manager.resolveFile(address(database, "/"));
            FileObject file = root.resolveFile("tree/child/file");
            write(file, false, new byte[] {3});
            assertThrows(IOException.class, root::deleteAll);
            assertThrows(IOException.class, () -> root.moveTo(root.resolveFile("elsewhere")));
            assertThrows(IOException.class, () -> file.moveTo(root));
            assertThrows(IOException.class, () -> root.resolveFile("tree").moveTo(root.resolveFile("tree/child")));
            assertArrayEquals(new byte[] {3}, read(file));
        }
    }

    @Test
    void ancestorMoveGuardsPreserveBothDirectoryAndFileSources() throws Exception {
        for (boolean directorySource : new boolean[] {true, false}) {
            Path database = database(directorySource ? "ancestor-directory.sqlite" : "ancestor-file.sqlite");
            try (DefaultFileSystemManager manager = manager()) {
                FileObject root = manager.resolveFile(address(database, "/"));
                FileObject file = root.resolveFile("tree/child/file");
                FileObject sibling = root.resolveFile("tree/sibling");
                write(file, false, new byte[] {3});
                write(sibling, false, new byte[] {4});
                FileObject source = directorySource ? file.getParent() : file;
                assertThrows(IOException.class, () -> source.moveTo(root.resolveFile("tree")));
            }
            // Inspect the independent native namespace, rather than cached VFS types.
            try (M3FS nativeOwner = M3FS.open(database)) {
                assertEquals(M3FS.Kind.DIRECTORY, nativeOwner.stat("/tree").kind());
                assertEquals(M3FS.Kind.DIRECTORY, nativeOwner.stat("/tree/child").kind());
                assertArrayEquals(new byte[] {3}, nativeOwner.read("/tree/child/file", 0, 3));
                assertArrayEquals(new byte[] {4}, nativeOwner.read("/tree/sibling", 0, 3));
                nativeOwner.verify();
            }
        }
    }

    @Test
    void missingMoveSourceDoesNotDeleteDatabaseOrLocalDestination() throws Exception {
        Path sourceDatabase = database("missing-source.sqlite");
        Path targetDatabase = database("existing-target.sqlite");
        Path localTarget = Files.write(temp.resolve("existing-target.bin"), new byte[] {5});
        try (DefaultFileSystemManager manager = manager()) {
            FileObject missing = manager.resolveFile(address(sourceDatabase, "/missing"));
            FileObject target = manager.resolveFile(address(targetDatabase, "/target"));
            write(target, false, new byte[] {4});
            assertThrows(IOException.class, () -> missing.moveTo(target));
            try (M3FS nativeOwner = M3FS.open(targetDatabase)) {
                assertArrayEquals(new byte[] {4}, nativeOwner.read("/target", 0, 3));
                nativeOwner.verify();
            }
            FileObject local = manager.resolveFile(localTarget.toUri().toString());
            assertThrows(IOException.class, () -> missing.moveTo(local));
            assertArrayEquals(new byte[] {5}, Files.readAllBytes(localTarget));
        }
    }

    @Test
    void crossMountMoveIsRefusedBeforeOverwritingAnyDestination() throws Exception {
        for (String targetKind : new String[] {"same-database", "other-database", "local"}) {
            for (boolean directorySource : new boolean[] {false, true}) {
                String fixture = targetKind + '-' + directorySource;
                Path sourceDatabase = database("source-" + fixture + ".sqlite");
                Path targetDatabase = targetKind.equals("same-database") ? sourceDatabase
                        : database("target-" + fixture + ".sqlite");
                Path localTarget = temp.resolve("target-" + fixture);
                String sourcePath = directorySource ? "/source/child" : "/source";
                String targetPath = directorySource ? "/target/safe" : "/target";
                try (DefaultFileSystemManager sourceManager = manager();
                        DefaultFileSystemManager targetManager = manager()) {
                    FileObject source = sourceManager.resolveFile(address(sourceDatabase, "/source"));
                    FileObject target = targetManager.resolveFile(targetKind.equals("local")
                            ? localTarget.toUri().toString() : address(targetDatabase, "/target"));
                    write(directorySource ? source.resolveFile("child") : source, false, new byte[] {9});
                    write(directorySource ? target.resolveFile("safe") : target, false, new byte[] {8});
                    assertThrows(IOException.class, () -> source.moveTo(target));
                }
                try (M3FS nativeOwner = M3FS.open(sourceDatabase)) {
                    assertArrayEquals(new byte[] {9}, nativeOwner.read(sourcePath, 0, 3));
                    nativeOwner.verify();
                }
                if (targetKind.equals("local")) {
                    assertArrayEquals(new byte[] {8}, Files.readAllBytes(
                            directorySource ? localTarget.resolve("safe") : localTarget));
                } else {
                    try (M3FS nativeOwner = M3FS.open(targetDatabase)) {
                        assertArrayEquals(new byte[] {8}, nativeOwner.read(targetPath, 0, 3));
                        nativeOwner.verify();
                    }
                }
            }
        }
    }

    @Test
    void refreshSeesExternalChangesAndManagerCloseEndsContentLifetime() throws Exception {
        Path database = database("lifetime.sqlite");
        DefaultFileSystemManager manager = manager();
        InputStream input = null;
        try {
            FileObject root = manager.resolveFile(address(database, "/"));
            FileObject missing = root.resolveFile("later");
            assertFalse(missing.exists());
            assertEquals(0, root.getChildren().length);
            try (M3FS nativeOwner = M3FS.open(database)) {
                nativeOwner.createFile("/later", 0644);
                nativeOwner.write("/later", 0, new byte[] {6});
            }
            missing.refresh();
            root.refresh();
            assertTrue(missing.exists());
            assertEquals(1, root.getChildren().length);
            input = missing.getContent().getInputStream(1);
            manager.close();
            InputStream closedOwnerInput = input;
            assertThrows(IOException.class, closedOwnerInput::read);
            assertThrows(IOException.class, () -> missing.getContent().getInputStream());
        } finally {
            if (input != null) input.close();
            manager.close();
        }
        try (DefaultFileSystemManager reopened = manager()) {
            assertArrayEquals(new byte[] {6}, read(reopened.resolveFile(address(database, "/later"))));
        }
    }

    @Test
    void inheritedMfilesProgressShadowsAndCaptureUseTheDatabaseProvider() throws Exception {
        Path database = database("facade.sqlite");
        byte[] bytes = "database-backed β content".getBytes(StandardCharsets.UTF_8);
        try (M3FS nativeOwner = M3FS.open(database)) {
            nativeOwner.createFile("/file", 0644);
            nativeOwner.write("/file", 0, bytes);
        }
        AtomicLong work = new AtomicLong();
        ByteArrayOutputStream shadow = new ByteArrayOutputStream();
        List<MFileCapture> captures = new ArrayList<>();
        MFile retained;
        String candidateAddress = "testdb:" + database.toUri() + "!/file";
        try (Stream<MFile> entries = MFiles.from(candidateAddress)
                .withProvider("testdb", DatabaseFileProvider::new).stream()) {
            retained = entries.findFirst().orElseThrow();
            assertEquals(bytes.length, retained.size().orElseThrow());
            MFileContent view = retained.progressify(M3FsProgress.of(() -> false, work::addAndGet))
                    .shadow("db-bytes", MFileShadow.of(shadow)).captureReads(captures::add);
            assertEquals(0, work.get());
            try (InputStream input = view.openInputStream()) {
                assertArrayEquals(bytes, input.readAllBytes());
            }
            assertEquals(bytes.length, work.get());
            assertArrayEquals(bytes, shadow.toByteArray());
            assertEquals(1, captures.size());
            assertEquals(M3ContentId.digest(bytes), captures.getFirst().manifest().raw().id());
            assertEquals(bytes.length, captures.getFirst().manifest().raw().size());
            assertEquals(candidateAddress, captures.getFirst().observation().sourceKey());
            try (OutputStream output = retained.openOutputStream(true)) {
                output.write('!');
            }
        }
        assertEquals("file", retained.name());
        assertThrows(IOException.class, retained::openInputStream);
        try (M3FS nativeOwner = M3FS.openReadOnly(database)) {
            byte[] expected = Arrays.copyOf(bytes, bytes.length + 1);
            expected[bytes.length] = '!';
            assertArrayEquals(expected, nativeOwner.read("/file", 0, expected.length + 1));
        }
    }

    private int appendRecords(Path database, int owner, CountDownLatch start) throws Exception {
        try (DefaultFileSystemManager manager = manager()) {
            FileObject file = manager.resolveFile(address(database, "/log"));
            try (OutputStream output = file.getContent().getOutputStream(true, 1)) {
                start.await();
                for (int i = 0; i < 40; i++) {
                    output.write(ByteBuffer.allocate(8).putInt(owner).putInt(i).array());
                }
            }
        }
        return 40;
    }

    private Path database(String name) throws IOException {
        Path path = temp.resolve(name);
        try (M3FS nativeOwner = M3FS.create(path)) {
            assertEquals(M3FS.Kind.DIRECTORY, nativeOwner.stat("/").kind());
        }
        return path;
    }

    private static String address(Path database, String path) {
        return "m3db:" + database.toUri() + "!" + path;
    }

    private static DefaultFileSystemManager manager() throws FileSystemException {
        DefaultFileSystemManager manager = new DefaultFileSystemManager();
        manager.addProvider("file", new DefaultLocalFileProvider());
        manager.addProvider("m3db", new DatabaseFileProvider());
        manager.addProvider("ram", new RamFileProvider());
        manager.init();
        return manager;
    }

    private static byte[] read(FileObject file) throws IOException {
        try (InputStream input = file.getContent().getInputStream()) {
            return input.readAllBytes();
        }
    }

    private static void write(FileObject file, boolean append, byte[] bytes) throws IOException {
        try (OutputStream output = file.getContent().getOutputStream(append)) {
            output.write(bytes);
        }
    }
}
