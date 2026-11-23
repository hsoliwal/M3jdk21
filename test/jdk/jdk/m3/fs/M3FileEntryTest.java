/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.fs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Public entry API, real archive providers, resource ownership and legacy identity. */
class M3FileEntryTest {
    @TempDir Path temp;

    @Test void filtersFoldersAndTransformsFilesLazily() throws Exception {
        Files.createDirectory(temp.resolve("folder"));
        Files.writeString(temp.resolve("folder/data.txt"), "payload");
        Files.writeString(temp.resolve("skip.bin"), "skip");
        AtomicInteger transforms = new AtomicInteger();
        try (Stream<String> entries = M3File.stream(temp, M3File::isFile, entry -> {
            transforms.incrementAndGet();
            return entry.relativeName();
        })) {
            assertEquals(0, transforms.get());
            assertEquals(Set.of("folder/data.txt", "skip.bin"), entries.collect(Collectors.toSet()));
            assertEquals(2, transforms.get());
        }
        try (Stream<String> folders = M3File.stream(temp, M3File::isDirectory, M3File::name)) {
            assertEquals(List.of("folder"), folders.toList());
        }
    }

    @Test void preferredEntryIsTheOriginalObject() throws Exception {
        Files.writeString(temp.resolve("data.txt"), "payload");
        AtomicReference<MFile> original = new AtomicReference<>();
        MFiles.Plan plan = MFiles.from(temp).filter(entry -> { original.set(entry); return true; });
        try (Stream<M3File> entries = M3File.stream(plan)) {
            M3File entry = entries.findFirst().orElseThrow();
            assertSame(original.get(), entry);
            assertTrue(entry instanceof MFile);
            assertEquals(7, entry.size().orElseThrow());
            assertEquals(temp.resolve("data.txt").toUri(), entry.uri());
            assertFalse(entry.isSymbolicLink());
        }
    }

    @Test void shortCircuitCloseRetainsMetadataAndRevokesContent() throws Exception {
        Files.writeString(temp.resolve("a.txt"), "payload");
        M3File retained;
        InputStream opened;
        try (Stream<M3File> entries = M3File.stream(temp)) {
            retained = entries.findFirst().orElseThrow();
            opened = retained.openInputStream();
            assertEquals('p', opened.read());
        }
        assertEquals("a.txt", retained.name());
        assertThrows(IOException.class, retained::openInputStream);
        assertThrows(IOException.class, opened::read);
        opened.close();
    }

    @Test void readsActualZipAndJarUsingTheSameEntryContract() throws Exception {
        for (String scheme : List.of("zip", "jar")) {
            Path archive = zip(temp.resolve("data." + scheme), "dir/data.txt", "payload".getBytes(StandardCharsets.UTF_8));
            try (Stream<String> contents = M3File.stream(scheme + ":" + archive.toUri() + "!/",
                    M3File::isFile, M3FileEntryTest::read)) {
                assertEquals(List.of("payload"), contents.toList());
            }
        }
    }

    @Test void readsActualTarAndGzipProviders() throws Exception {
        byte[] data = "payload".getBytes(StandardCharsets.UTF_8);
        Path tar = temp.resolve("data.tar");
        try (TarArchiveOutputStream out = new TarArchiveOutputStream(Files.newOutputStream(tar))) {
            TarArchiveEntry entry = new TarArchiveEntry("data.txt");
            entry.setSize(data.length);
            out.putArchiveEntry(entry);
            out.write(data);
            out.closeArchiveEntry();
        }
        Path gzip = temp.resolve("data.txt.gz");
        try (GZIPOutputStream out = new GZIPOutputStream(Files.newOutputStream(gzip))) { out.write(data); }
        for (String address : List.of("tar:" + tar.toUri() + "!/", "gz:" + gzip.toUri() + "!/")) {
            try (Stream<String> contents = M3File.stream(address, M3File::isFile, M3FileEntryTest::read)) {
                assertEquals(List.of("payload"), contents.toList());
            }
        }
    }

    @Test void nestedArchivesAreAnExplicitBoundedPlanChoice() throws Exception {
        Path inner = zip(temp.resolve("inner.zip"), "data.txt", "nested".getBytes(StandardCharsets.UTF_8));
        Path outer = zip(temp.resolve("outer.zip"), "inner.zip", Files.readAllBytes(inner));
        try (Stream<M3File> entries = M3File.stream(outer)) {
            assertEquals(List.of("outer.zip"), entries.map(M3File::name).toList());
        }
        MFiles.Plan plan = MFiles.from(outer).enterArchives(entry -> entry.name().endsWith(".zip"), 2);
        try (Stream<String> contents = M3File.stream(plan, entry -> entry.name().equals("data.txt"), M3FileEntryTest::read)) {
            assertEquals(List.of("nested"), contents.toList());
        }
    }

    @Test void nonDefaultNioFilesystemRemainsCallerOwned() throws Exception {
        Path archive = temp.resolve("nio.zip");
        try (var filesystem = FileSystems.newFileSystem(URI.create("jar:" + archive.toUri()), Map.of("create", "true"))) {
            Files.writeString(filesystem.getPath("/data.txt"), "payload");
            try (Stream<M3File> entries = M3File.stream(filesystem.getPath("/"))) {
                assertEquals("payload", read(entries.findFirst().orElseThrow()));
            }
            assertTrue(filesystem.isOpen());
            assertEquals("payload", Files.readString(filesystem.getPath("/data.txt")));
        }
    }

    @Test void predicateAndTransformerFailuresPreserveOwnershipAndSkipLaterWork() throws Exception {
        Files.writeString(temp.resolve("a.txt"), "payload");
        AtomicReference<M3File> retained = new AtomicReference<>();
        IllegalStateException failure = new IllegalStateException("callback");
        try (Stream<String> entries = M3File.stream(temp, entry -> {
            retained.set(entry);
            throw failure;
        }, entry -> { throw new AssertionError("transform must be skipped"); })) {
            assertSame(failure, assertThrows(IllegalStateException.class, entries::toList));
        }
        assertThrows(IOException.class, retained.get()::openInputStream);
        try (Stream<String> entries = M3File.stream(temp, M3File::isFile, entry -> {
            retained.set(entry);
            throw failure;
        })) {
            assertSame(failure, assertThrows(IllegalStateException.class, entries::toList));
        }
        assertThrows(IOException.class, retained.get()::openInputStream);
    }

    @Test void contravariantCallbacksAndNullResultsRemainSupported() throws Exception {
        Files.writeString(temp.resolve("a.txt"), "payload");
        Predicate<Object> all = entry -> true;
        Function<Object, String> nullable = entry -> null;
        try (Stream<String> entries = M3File.stream(temp, all, nullable)) {
            List<String> result = entries.toList();
            assertEquals(1, result.size());
            assertEquals(null, result.getFirst());
        }
    }

    @Test void invalidCallbacksAreRejectedBeforeProviderResolution() {
        String missing = "missing-provider:ignored";
        assertThrows(NullPointerException.class, () -> M3File.stream(missing, null, M3File::name));
        assertThrows(NullPointerException.class, () -> M3File.stream(missing, M3File::isFile, null));
        assertThrows(NullPointerException.class, () -> M3File.stream((MFiles.Plan) null));
        assertThrows(NullPointerException.class, () -> M3File.stream((Path) null));
        assertThrows(NullPointerException.class, () -> M3File.stream((String) null));
        assertThrows(IOException.class, () -> M3File.stream(missing));
    }

    @Test void planPruningDiffersFromOutputFiltering() throws Exception {
        Files.createDirectories(temp.resolve("skip"));
        Files.writeString(temp.resolve("skip/hidden.txt"), "payload");
        Files.writeString(temp.resolve("visible.txt"), "payload");
        try (Stream<M3File> entries = M3File.stream(MFiles.from(temp)
                .descendInto(folder -> !folder.name().equals("skip")))) {
            assertEquals(Set.of("skip", "visible.txt"), entries.map(M3File::relativeName).collect(Collectors.toSet()));
        }
    }

    @Test void contentIsNotCapturedBeforeAFilterRequestsIt() throws Exception {
        Path file = Files.writeString(temp.resolve("data.txt"), "before");
        List<String> values = new ArrayList<>();
        try (Stream<M3File> entries = M3File.stream(file)) {
            M3File entry = entries.findFirst().orElseThrow();
            Files.writeString(file, "after");
            values.add(read(entry));
        }
        assertEquals(List.of("after"), values);
    }

    private static String read(M3File entry) {
        try { return entry.readString(4096, StandardCharsets.UTF_8); }
        catch (IOException error) { throw new java.io.UncheckedIOException(error); }
    }

    private static Path zip(Path path, String name, byte[] data) throws IOException {
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(path))) {
            out.putNextEntry(new ZipEntry(name)); out.write(data); out.closeEntry();
        }
        return path;
    }
}
