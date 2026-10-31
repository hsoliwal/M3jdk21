/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.m3.fs.vfs.Capability;
import com.m3.fs.vfs.FileObject;
import com.m3.fs.vfs.impl.StandardFileSystemManager;
import com.m3.fs.vfs.provider.local.DefaultLocalFileProvider;

/** Real-provider contracts for borrowed NIO paths and their VFS bridge. */
final class NioFileProviderTest {
    @TempDir
    Path temp;

    // Converting a ZIP Path through toFile() fails before traversal starts.
    @Test
    void zipPathTraversesMetadataAndContentWithoutClosingCallerFileSystem() throws Exception {
        try (FileSystem zip = newZip("contents.zip")) {
            Path root = Files.createDirectory(zip.getPath("/root"));
            Files.createDirectory(root.resolve("sub"));
            Files.writeString(root.resolve("first.txt"), "first");
            Files.writeString(root.resolve("sub/second.txt"), "second");
            List<String> names = new ArrayList<>();
            try (Stream<MFile> entries = MFiles.from(root).stream()) {
                for (MFile entry : entries.toList()) {
                    names.add(entry.relativeName());
                    if (entry.isFile()) {
                        assertEquals(Files.readString(root.resolve(entry.relativeName())), read(entry));
                        assertEquals(root.resolve(entry.relativeName()).toUri(), entry.uri());
                    }
                }
            }
            assertEquals(List.of("first.txt", "sub", "sub/second.txt"), names.stream().sorted().toList());
            assertTrue(zip.isOpen());
            assertEquals("first", Files.readString(root.resolve("first.txt")));
        }
    }

    // A per-session token would make repeated captures of the same live source unrelated.
    @Test
    void sameLiveFileSystemKeepsIdentityAcrossPlansAndSupportsAddressReopening() throws Exception {
        try (FileSystem zip = newZip("repeat.zip")) {
            Path file = Files.writeString(zip.getPath("/data.txt"), "borrowed content");
            String first = address(file);
            assertEquals(first, address(file));
            assertTrue(first.startsWith("m3nio:"));
            try (Stream<MFile> entries = MFiles.from(first).stream()) {
                MFile entry = entries.findFirst().orElseThrow();
                assertEquals(first, entry.sourceAddress());
                assertEquals(file.toUri(), entry.uri());
                assertEquals("borrowed content", read(entry));
            }
            assertTrue(zip.isOpen());
        }
    }

    // Native jar: URIs do not distinguish independently opened ZIP FileSystem instances.
    @Test
    void differentFileSystemInstancesWithIdenticalNativeUrisKeepDistinctSourceIdentity() throws Exception {
        Path archive = temp.resolve("instances.zip");
        try (FileSystem initial = FileSystems.newFileSystem(archive, Map.of("create", "true"))) {
            Files.writeString(initial.getPath("/data.txt"), "shared archive");
        }
        try (FileSystem first = FileSystems.newFileSystem(archive, Map.of());
                FileSystem second = FileSystems.newFileSystem(archive, Map.of())) {
            Path one = first.getPath("/data.txt");
            Path two = second.getPath("/data.txt");
            assertEquals(one.toUri(), two.toUri());
            String firstAddress = address(one);
            String secondAddress = address(two);
            assertNotEquals(firstAddress, secondAddress);
            assertEquals(firstAddress, address(one));
            try (Stream<MFile> entries = MFiles.from(secondAddress).stream()) {
                assertEquals("shared archive", read(entries.findFirst().orElseThrow()));
            }
            assertTrue(first.isOpen());
            assertTrue(second.isOpen());
        }
    }

    // Reusing generic URL parsing can decode a literal %2f or alter bracket/Unicode names.
    @Test
    void componentEscapesPreserveNamesAndAddressRoundTrips() throws Exception {
        List<String> names = List.of("literal%2f.txt", "space [50%] #?!:.txt", "café-漢字-😀.txt");
        try (FileSystem zip = newZip("names.zip")) {
            for (String name : names) {
                Files.writeString(zip.getPath("/").resolve(name), name);
            }
            List<String> actual = new ArrayList<>();
            try (Stream<MFile> entries = MFiles.from(zip.getPath("/")).stream()) {
                for (MFile entry : entries.toList()) {
                    actual.add(entry.name());
                    assertEquals(entry.name(), entry.relativeName());
                    assertEquals(entry.name(), read(entry));
                    try (Stream<MFile> reopened = MFiles.from(entry.sourceAddress()).stream()) {
                        MFile other = reopened.findFirst().orElseThrow();
                        assertEquals(entry.name(), other.name());
                        assertEquals(entry.sourceAddress(), other.sourceAddress());
                        assertEquals(entry.name(), read(other));
                    }
                }
            }
            assertEquals(names.stream().sorted().toList(), actual.stream().sorted().toList());
        }
    }

    // The runtime image is a real installed NIO provider and must remain read-only.
    @Test
    void jrtPathReadsContentAndRefusesOutputWithoutClosingRuntimeFileSystem() throws Exception {
        FileSystem jrt = FileSystems.getFileSystem(URI.create("jrt:/"));
        Path objectClass = jrt.getPath("/modules/java.base/java/lang/Object.class");
        try (Stream<MFile> entries = MFiles.from(objectClass).stream()) {
            MFile entry = entries.findFirst().orElseThrow();
            assertEquals("Object.class", entry.name());
            assertEquals(objectClass.toUri(), entry.uri());
            assertTrue(entry.size().orElseThrow() > 4);
            try (InputStream input = entry.openInputStream()) {
                assertEquals(List.of(0xca, 0xfe, 0xba, 0xbe),
                        List.of(input.read(), input.read(), input.read(), input.read()));
            }
            assertThrows(IOException.class, () -> entry.openOutputStream(false));
        }
        assertTrue(jrt.isOpen());
    }

    // A writable NIO filesystem must use its output hook and the existing observation owner.
    @Test
    void zipOutputUpdatesBackendAndObservationWithoutInventingAChangeToken() throws Exception {
        try (FileSystem zip = newZip("output.zip")) {
            Path file = Files.writeString(zip.getPath("/data.txt"), "old");
            try (Stream<MFile> entries = MFiles.from(file).stream()) {
                MFile entry = entries.findFirst().orElseThrow();
                try (OutputStream output = entry.openOutputStream(false)) {
                    output.write("new content".getBytes(StandardCharsets.UTF_8));
                }
                assertEquals("new content", Files.readString(file));
                assertEquals(3, entry.size().orElseThrow());
                MFileObservation observed = entry.observation();
                assertEquals(entry.sourceAddress(), observed.sourceKey());
                assertEquals(11, observed.size().orElseThrow());
                assertFalse(observed.changeStamp().available());
                assertThrows(IOException.class, () -> entry.openOutputStream(true));
            }
            assertTrue(zip.isOpen());
        }
    }

    // The bridge's primitive hooks must support the existing VFS template operations.
    @Test
    void inheritedVfsCreateRenameAndRecursiveDeleteOperateOnBorrowedPaths() throws Exception {
        try (FileSystem zip = newZip("mutations.zip")) {
            StandardFileSystemManager manager = new StandardFileSystemManager();
            try {
                manager.init();
                NioFileProvider provider = new NioFileProvider();
                manager.addProvider("m3nio", provider);
                FileObject root = provider.resolve(zip.getPath("/"));
                FileObject directory = root.resolveFile("made/sub");
                directory.createFolder();
                FileObject file = directory.resolveFile("before.txt");
                file.createFile();
                try (OutputStream output = file.getContent().getOutputStream()) {
                    output.write("native data".getBytes(StandardCharsets.UTF_8));
                }
                FileObject renamed = directory.resolveFile("after.txt");
                file.moveTo(renamed);
                assertFalse(Files.exists(zip.getPath("/made/sub/before.txt")));
                assertEquals("native data", Files.readString(zip.getPath("/made/sub/after.txt")));
                assertFalse(root.getFileSystem().hasCapability(Capability.RANDOM_ACCESS_READ));
                root.resolveFile("made").deleteAll();
                assertFalse(Files.exists(zip.getPath("/made")));
            } finally {
                manager.close();
            }
            assertTrue(zip.isOpen());
        }
    }

    // VFS moveTo deletes an existing destination before invoking the rename hook.
    @Test
    void moveRejectsAncestorDestinationBeforeDeletingSourceOrSiblings() throws Exception {
        for (boolean directory : List.of(false, true)) {
            try (FileSystem zip = newZip("ancestor-" + directory + ".zip");
                    StandardFileSystemManager manager = new StandardFileSystemManager()) {
                Path child = Files.createDirectories(zip.getPath("/tree/child"));
                Path file = Files.writeString(child.resolve("data.txt"), "source");
                Path sibling = Files.writeString(zip.getPath("/tree/other.txt"), "sibling");
                NioFileProvider provider = provider(manager);
                FileObject source = provider.resolve(directory ? child : file);
                FileObject target = provider.resolve(zip.getPath("/tree"));
                assertThrows(IOException.class, () -> source.moveTo(target));
                assertEquals("source", Files.readString(file));
                assertEquals("sibling", Files.readString(sibling));
            }
        }
    }

    // A target below the source must be rejected before recursive target deletion.
    @Test
    void moveRejectsDescendantDestinationBeforeDeletingChildren() throws Exception {
        for (boolean existing : List.of(false, true)) {
            try (FileSystem zip = newZip("descendant-" + existing + ".zip");
                    StandardFileSystemManager manager = new StandardFileSystemManager()) {
                Path child = Files.createDirectories(zip.getPath("/tree/child"));
                Path file = Files.writeString(child.resolve("data.txt"), "keep");
                NioFileProvider provider = provider(manager);
                FileObject source = provider.resolve(zip.getPath("/tree"));
                FileObject target = provider.resolve(existing ? child : child.resolve("missing"));
                assertThrows(IOException.class, () -> source.moveTo(target));
                assertEquals("keep", Files.readString(file));
                assertFalse(Files.exists(child.resolve("missing")));
            }
        }
    }

    // Separate VFS sessions can still refer to the very same native file.
    @Test
    void moveToSameNativeFileAcrossManagersIsANoOp() throws Exception {
        try (FileSystem zip = newZip("same-file.zip");
                StandardFileSystemManager first = new StandardFileSystemManager();
                StandardFileSystemManager second = new StandardFileSystemManager()) {
            Path file = Files.writeString(zip.getPath("/data.txt"), "retain");
            FileObject source = provider(first).resolve(file);
            FileObject target = provider(second).resolve(file);
            source.moveTo(target);
            assertEquals("retain", Files.readString(file));
            assertTrue(source.exists());
            assertTrue(target.exists());
        }
    }

    // Cross-manager VFS identity must not select the single-entry copy/delete fallback.
    @Test
    void moveBetweenManagersRenamesWithinTheSameBorrowedFileSystem() throws Exception {
        try (FileSystem zip = newZip("same-filesystem.zip");
                StandardFileSystemManager first = new StandardFileSystemManager();
                StandardFileSystemManager second = new StandardFileSystemManager()) {
            Path sourcePath = Files.writeString(zip.getPath("/before"), "moved");
            Path targetPath = Files.writeString(zip.getPath("/after"), "replaced");
            FileObject source = provider(first).resolve(sourcePath);
            FileObject target = provider(second).resolve(targetPath);
            assertTrue(source.canRenameTo(target));
            source.moveTo(target);
            assertFalse(Files.exists(sourcePath));
            assertEquals("moved", Files.readString(targetPath));
        }
    }

    // Even an empty ZIP directory is not reliably relocated by native Files.move.
    @Test
    void moveRefusesDirectorySourcesWithoutCreatingATarget() throws Exception {
        for (boolean nonempty : List.of(false, true)) {
            try (FileSystem zip = newZip("directory-source-" + nonempty + ".zip");
                    StandardFileSystemManager manager = new StandardFileSystemManager()) {
                Path sourcePath = Files.createDirectory(zip.getPath("/before"));
                Path file = sourcePath.resolve("data.txt");
                if (nonempty) {
                    Files.writeString(file, "retain");
                }
                Path targetPath = zip.getPath("/after");
                NioFileProvider provider = provider(manager);
                FileObject source = provider.resolve(sourcePath);
                FileObject target = provider.resolve(targetPath);
                assertThrows(IOException.class, () -> source.moveTo(target));
                assertTrue(Files.isDirectory(sourcePath));
                if (nonempty) {
                    assertEquals("retain", Files.readString(file));
                }
                assertFalse(Files.exists(targetPath));
            }
        }
    }

    // VFS's recursive replacement can remove unrelated contents before native rename fails.
    @Test
    void moveRefusesReplacingNonemptyTargetDirectories() throws Exception {
        try (FileSystem zip = newZip("nonempty-target.zip");
                StandardFileSystemManager manager = new StandardFileSystemManager()) {
            Path sourcePath = Files.writeString(zip.getPath("/source.txt"), "source");
            Path targetPath = Files.createDirectory(zip.getPath("/target"));
            Path child = Files.writeString(targetPath.resolve("data.txt"), "target");
            NioFileProvider provider = provider(manager);
            FileObject source = provider.resolve(sourcePath);
            FileObject target = provider.resolve(targetPath);
            assertThrows(IOException.class, () -> source.moveTo(target));
            assertEquals("source", Files.readString(sourcePath));
            assertEquals("target", Files.readString(child));
        }
    }

    // A filesystem root cannot be moved or replaced by a descendant's contents.
    @Test
    void moveRejectsFilesystemRootsBeforeAnyMutation() throws Exception {
        for (boolean sourceRoot : List.of(false, true)) {
            try (FileSystem zip = newZip("root-move-" + sourceRoot + ".zip");
                    StandardFileSystemManager manager = new StandardFileSystemManager()) {
                Path file = Files.writeString(zip.getPath("/data.txt"), "keep");
                NioFileProvider provider = provider(manager);
                FileObject source = provider.resolve(sourceRoot ? zip.getPath("/") : file);
                FileObject target = provider.resolve(sourceRoot ? zip.getPath("/moved") : zip.getPath("/"));
                assertThrows(IOException.class, () -> source.moveTo(target));
                assertEquals("keep", Files.readString(file));
                assertFalse(Files.exists(zip.getPath("/moved")));
            }
        }
    }

    // An imaginary source must be detected before an existing target is removed.
    @Test
    void missingMoveSourceDoesNotDeleteAnExistingTarget() throws Exception {
        try (FileSystem zip = newZip("missing-source.zip");
                StandardFileSystemManager manager = new StandardFileSystemManager()) {
            Path file = Files.writeString(zip.getPath("/target.txt"), "retain");
            NioFileProvider provider = provider(manager);
            FileObject source = provider.resolve(zip.getPath("/missing.txt"));
            FileObject target = provider.resolve(file);
            assertThrows(IOException.class, () -> source.moveTo(target));
            assertEquals("retain", Files.readString(file));
        }
    }

    // This native rename bridge cannot prove identity across distinct filesystem instances.
    @Test
    void crossFilesystemMoveIsRefusedBeforeDeletingEitherFile() throws Exception {
        try (FileSystem sourceSystem = newZip("source-system.zip");
                FileSystem targetSystem = newZip("target-system.zip");
                StandardFileSystemManager manager = new StandardFileSystemManager()) {
            Path sourcePath = Files.writeString(sourceSystem.getPath("/data.txt"), "source");
            Path targetPath = Files.writeString(targetSystem.getPath("/data.txt"), "target");
            NioFileProvider provider = provider(manager);
            FileObject source = provider.resolve(sourcePath);
            FileObject target = provider.resolve(targetPath);
            assertThrows(IOException.class, () -> source.moveTo(target));
            assertEquals("source", Files.readString(sourcePath));
            assertEquals("target", Files.readString(targetPath));
        }
    }

    // Forking NIO regions via string-only local reopen coordinates loses the borrowed instance.
    @Test
    void parallelNioRegionUsesOneOwningWorkerAndKeepsBorrowedFileSystemOpen() throws Exception {
        try (FileSystem zip = newZip("parallel.zip");
                ExecutorService executor = Executors.newFixedThreadPool(3)) {
            Path root = Files.createDirectory(zip.getPath("/root"));
            for (int index = 0; index < 8; index++) {
                Path directory = Files.createDirectory(root.resolve("d" + index));
                Files.writeString(directory.resolve("data.txt"), "value" + index);
            }
            Set<Thread> workers = ConcurrentHashMap.newKeySet();
            try (Stream<String> results = MFiles.from(root).filter(MFile::isFile)
                    .mapParallel(executor, 3, file -> {
                        workers.add(Thread.currentThread());
                        return read(file);
                    })) {
                assertEquals(8, results.count());
            }
            assertEquals(1, workers.size());
            assertTrue(zip.isOpen());
        }
    }

    // Traversal cleanup owns the bridge and content streams, never the supplied filesystem.
    @Test
    void traversalFailureClosesContentButPreservesCallerFileSystem() throws Exception {
        try (FileSystem zip = newZip("failure.zip")) {
            Path file = Files.writeString(zip.getPath("/data.txt"), "content");
            IllegalStateException expected = new IllegalStateException("callback failed");
            List<InputStream> opened = new ArrayList<>();
            try (Stream<MFile> entries = MFiles.from(file).stream()) {
                IllegalStateException actual = assertThrows(IllegalStateException.class,
                        () -> entries.map(entry -> {
                            try {
                                opened.add(entry.openInputStream());
                            } catch (IOException failure) {
                                throw new AssertionError(failure);
                            }
                            throw expected;
                        }).findFirst());
                assertSame(expected, actual);
                assertThrows(IOException.class, () -> opened.getFirst().read());
            }
            assertTrue(zip.isOpen());
            assertEquals("content", Files.readString(file));
        }
    }

    // A captured address must never silently reopen a different filesystem after owner close.
    @Test
    void addressOfClosedFileSystemFailsWithoutRemountingIt() throws Exception {
        String source;
        FileSystem zip = newZip("closed.zip");
        try (zip) {
            source = address(Files.writeString(zip.getPath("/data.txt"), "content"));
        }
        assertFalse(zip.isOpen());
        assertThrows(IOException.class, () -> MFiles.from(source).stream());
        assertFalse(zip.isOpen());
    }

    // An explicitly supplied provider must not be replaced silently by the internal bridge.
    @Test
    void internalSchemeConflictFailsExplicitly() throws Exception {
        try (FileSystem zip = newZip("conflict.zip")) {
            MFiles.Plan plan = MFiles.from(zip.getPath("/"))
                    .withProvider("m3nio", DefaultLocalFileProvider::new);
            assertThrows(IOException.class, plan::stream);
            assertTrue(zip.isOpen());
        }
    }

    // Ordinary local paths must keep their existing source identity and provider behavior.
    @Test
    void defaultFileSystemPathKeepsLocalProviderAddress() throws Exception {
        Path file = Files.writeString(temp.resolve("local.txt"), "local content");
        try (Stream<MFile> entries = MFiles.from(file).stream()) {
            MFile entry = entries.findFirst().orElseThrow();
            assertTrue(entry.sourceAddress().startsWith("file:"));
            assertEquals(file.toUri(), entry.uri());
            assertEquals("local content", read(entry));
        }
    }

    private FileSystem newZip(String name) throws IOException {
        return FileSystems.newFileSystem(temp.resolve(name), Map.of("create", "true"));
    }

    private static NioFileProvider provider(StandardFileSystemManager manager) throws IOException {
        manager.init();
        NioFileProvider provider = new NioFileProvider();
        manager.addProvider("m3nio", provider);
        return provider;
    }

    private static String address(Path path) throws IOException {
        try (Stream<MFile> entries = MFiles.from(path).stream()) {
            return entries.findFirst().orElseThrow().sourceAddress();
        }
    }

    private static String read(MFile file) throws IOException {
        try (InputStream input = file.openInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
