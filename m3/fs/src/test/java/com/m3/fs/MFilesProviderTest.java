/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Supplier;
import java.util.function.Predicate;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.m3.fs.vfs.CacheStrategy;
import com.m3.fs.vfs.FileName;
import com.m3.fs.vfs.FileObject;
import com.m3.fs.vfs.FileSystem;
import com.m3.fs.vfs.FileSystemException;
import com.m3.fs.vfs.FileSystemOptions;
import com.m3.fs.vfs.FileType;
import com.m3.fs.vfs.cache.NullFilesCache;
import com.m3.fs.vfs.impl.DefaultFileReplicator;
import com.m3.fs.vfs.impl.DefaultFileSystemManager;
import com.m3.fs.vfs.impl.StandardFileSystemManager;
import com.m3.fs.vfs.provider.AbstractFileName;
import com.m3.fs.vfs.provider.local.DefaultLocalFileProvider;
import com.m3.fs.vfs.provider.local.LocalFile;
import com.m3.fs.vfs.provider.local.LocalFileName;
import com.m3.fs.vfs.provider.local.LocalFileSystem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Uses real local VFS IO with a provider subclass recording the ownership boundary. */
class MFilesProviderTest {
    @TempDir
    Path temp;

    // Eager factories or reused managers break lazy plans and independent content lifetimes.
    @Test
    void providerFactoriesAreLazyAndProduceIndependentOwnedSessions() throws Exception {
        Files.writeString(temp.resolve("data.txt"), "actual content");
        List<ObservedLocalProvider> providers = new ArrayList<>();
        Supplier<ObservedLocalProvider> factory = () -> {
            ObservedLocalProvider provider = new ObservedLocalProvider();
            providers.add(provider);
            return provider;
        };
        MFiles.Plan unregistered = MFiles.from(address(temp));
        MFiles.Plan plan = unregistered.withProvider("observed", factory);
        assertEquals(0, providers.size());
        try (Stream<MFile> first = plan.stream(); Stream<MFile> second = plan.stream()) {
            assertEquals(2, providers.size());
            assertNotSame(providers.get(0), providers.get(1));
            MFile firstFile = first.findFirst().orElseThrow();
            MFile secondFile = second.findFirst().orElseThrow();
            closeStream(first);
            assertTrue(providers.get(0).closed);
            assertFalse(providers.get(1).closed);
            assertThrows(IOException.class, firstFile::openInputStream);
            try (InputStream input = secondFile.openInputStream()) {
                assertEquals("actual content", new String(input.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        assertTrue(providers.get(1).closed);
        assertThrows(IOException.class, unregistered::stream);
    }

    // Opening content during projection/filtering changes this real provider's open count.
    @Test
    void metadataTraversalDoesNotRequestEntryPayloadStreams() throws Exception {
        Files.writeString(temp.resolve("data.txt"), "actual content");
        ObservedLocalProvider provider = new ObservedLocalProvider();
        MFiles.Plan plan = MFiles.from(address(temp)).withProvider("observed", () -> provider)
                .filter(file -> file.isFile() && file.size().orElse(0) > 0);
        try (Stream<MFile> entries = plan.stream()) {
            List<MFile> files = entries.toList();
            assertEquals(List.of("data.txt"), files.stream().map(MFile::name).toList());
            assertEquals(0, provider.contentOpens);
            try (InputStream input = files.getFirst().openInputStream()) {
                assertEquals('a', input.read());
                assertEquals(1, provider.contentOpens);
            }
            assertEquals(1, provider.contentCloses);
        }
        assertTrue(provider.closed);
    }

    // Ignoring provider unknown-size metadata would report a misleading negative or zero size.
    @Test
    void unknownProviderSizeStaysOptional() throws Exception {
        Files.writeString(temp.resolve("unknown-size"), "actual content");
        try (Stream<MFile> entries = MFiles.from(address(temp))
                .withProvider("observed", ObservedLocalProvider::new).stream()) {
            MFile file = entries.findFirst().orElseThrow();
            assertEquals(OptionalLong.empty(), file.size());
            try (InputStream input = file.openInputStream()) {
                assertEquals('a', input.read());
            }
        }
    }

    // Equality checks against FILE/FOLDER lose the SPI's legitimate combined type.
    @Test
    void hybridProviderEntriesRetainBothContentAndChildCapabilities() throws Exception {
        Path hybrid = Files.createDirectory(temp.resolve("hybrid"));
        Files.writeString(hybrid.resolve("child.txt"), "actual content");
        try (Stream<MFile> entries = MFiles.from(address(temp))
                .withProvider("observed", ObservedLocalProvider::new).stream()) {
            List<MFile> files = entries.toList();
            assertEquals(Set.of("hybrid", "hybrid/child.txt"),
                    files.stream().map(MFile::relativeName).collect(Collectors.toSet()));
            MFile directory = files.stream().filter(file -> file.name().equals("hybrid")).findFirst().orElseThrow();
            assertTrue(directory.isFile());
            assertTrue(directory.isDirectory());
            Object observation = invoke(directory, "observation", new Class<?>[0]);
            assertEquals(true, invoke(observation, "isFile", new Class<?>[0]));
            assertEquals(true, invoke(observation, "isDirectory", new Class<?>[0]));
        }
    }

    // Swallowing lazy listing errors yields an incomplete result; leaking ownership keeps reads alive.
    @Test
    void lazyProviderFailureClosesSessionAndPreservesItsIoCause() throws Exception {
        Files.createDirectory(temp.resolve("blocked"));
        ObservedLocalProvider provider = new ObservedLocalProvider();
        try (Stream<MFile> entries = MFiles.from(address(temp)).withProvider("observed", () -> provider).stream()) {
            var iterator = entries.iterator();
            MFile folder = iterator.next();
            assertEquals("blocked", folder.name());
            UncheckedIOException failure = assertThrows(UncheckedIOException.class, iterator::hasNext);
            assertTrue(hasMessage(failure, "controlled listing failure"));
            assertTrue(provider.closed);
            assertThrows(IOException.class, folder::openInputStream);
        }
    }

    // Listing a pruned directory would trigger the actual provider's controlled IO failure.
    @Test
    void pruningAvoidsOpeningARejectedDirectorysChildren() throws Exception {
        Files.createDirectory(temp.resolve("blocked"));
        Files.writeString(temp.resolve("data.txt"), "actual content");
        try (Stream<MFile> entries = MFiles.from(address(temp))
                .withProvider("observed", ObservedLocalProvider::new)
                .descendInto(folder -> !folder.name().equals("blocked")).stream()) {
            assertEquals(Set.of("blocked", "data.txt"), entries.map(MFile::relativeName).collect(Collectors.toSet()));
        }
    }

    // Converting content IO errors to null/empty content would hide the provider failure.
    @Test
    void contentOpenFailureRemainsChecked() throws Exception {
        Files.writeString(temp.resolve("unreadable"), "actual content");
        try (Stream<MFile> entries = MFiles.from(address(temp))
                .withProvider("observed", ObservedLocalProvider::new).stream()) {
            MFile file = entries.findFirst().orElseThrow();
            IOException failure = assertThrows(IOException.class, file::openInputStream);
            assertTrue(hasMessage(failure, "controlled content failure"));
        }
    }

    // Omitting callback-failure cleanup leaves the already opened content and provider active.
    @Test
    void mappingFailureClosesOutstandingContentAndManager() throws Exception {
        Files.writeString(temp.resolve("data.txt"), "actual content");
        ObservedLocalProvider provider = new ObservedLocalProvider();
        IllegalStateException expected = new IllegalStateException("mapping failed");
        List<InputStream> outstanding = new ArrayList<>();
        try (Stream<MFile> entries = MFiles.from(address(temp)).withProvider("observed", () -> provider).stream()) {
            IllegalStateException observed = assertThrows(IllegalStateException.class,
                    () -> entries.map(file -> {
                        try {
                            outstanding.add(file.openInputStream());
                        } catch (IOException failure) {
                            throw new UncheckedIOException(failure);
                        }
                        throw expected;
                    }).findFirst());
            assertSame(expected, observed);
            assertTrue(provider.closed);
            assertEquals(1, provider.contentCloses);
            assertThrows(IOException.class, () -> outstanding.getFirst().read());
        }
    }

    // A provider that fails during manager registration still owns its partially opened resource.
    @Test
    void failedProviderInitializationIsClosedBeforeTheOpenErrorEscapes() throws Exception {
        Path resource = Files.writeString(temp.resolve("initialization-resource"), "actual content");
        ObservedLocalProvider provider = new ObservedLocalProvider();
        provider.failInitializationWithResource = resource;
        MFiles.Plan plan = MFiles.from(address(temp)).withProvider("observed", () -> provider);
        IOException failure = assertThrows(IOException.class, plan::stream);
        assertTrue(hasMessage(failure, "controlled initialization failure"));
        assertTrue(provider.closed);
        assertEquals(1, provider.closeCalls);
        assertThrows(IOException.class, () -> provider.initializationResource.read());
    }

    // One unchecked close failure must not prevent other readers or the owning manager from closing.
    @Test
    void aReaderCloseFailureStillClosesEveryOtherOwnedResource() throws Exception {
        Files.writeString(temp.resolve("close-fails"), "first content");
        Files.writeString(temp.resolve("normal.txt"), "second content");
        ObservedLocalProvider provider = new ObservedLocalProvider();
        try (Stream<MFile> entries = MFiles.from(address(temp)).withProvider("observed", () -> provider).stream()) {
            List<InputStream> readers = new ArrayList<>();
            for (MFile file : entries.toList()) {
                readers.add(file.openInputStream());
            }
            IllegalStateException failure = assertThrows(IllegalStateException.class, () -> closeStream(entries));
            assertTrue(hasMessage(failure, "controlled close failure"));
            assertTrue(provider.closed);
            assertEquals(2, provider.contentCloses);
            for (InputStream reader : readers) {
                assertThrows(IOException.class, reader::read);
            }
        }
    }

    // HashMap encounter order must not decide which owned providers get released.
    @Test
    void providerCloseFailuresStillCloseEveryProviderAndRetainSuppressedFailures() throws Exception {
        Path resource = Files.writeString(temp.resolve("data.txt"), "actual content");
        ObservedLocalProvider first = new ObservedLocalProvider();
        ObservedLocalProvider second = new ObservedLocalProvider();
        first.initializationResourcePath = resource;
        second.initializationResourcePath = resource;
        first.closeFailure = new IllegalStateException("first provider close failure");
        second.closeFailure = new IllegalStateException("second provider close failure");
        Stream<MFile> entries = MFiles.from(address(temp))
                .withProvider("observed", () -> first).withProvider("auxiliary", () -> second).stream();
        try {
            assertEquals("data.txt", entries.findFirst().orElseThrow().name());
            RuntimeException failure = assertThrows(RuntimeException.class, () -> closeStream(entries));
            Throwable laterFailure = failure == first.closeFailure ? second.closeFailure : first.closeFailure;
            assertAll(
                    () -> assertTrue(failure == first.closeFailure || failure == second.closeFailure),
                    () -> assertEquals(1, first.closeCalls),
                    () -> assertEquals(1, second.closeCalls),
                    () -> assertTrue(hasSuppressed(failure, laterFailure)),
                    () -> assertThrows(IOException.class, () -> first.initializationResource.read()),
                    () -> assertThrows(IOException.class, () -> second.initializationResource.read()));
        } finally {
            closeAfterFailedAssertion(first);
            closeAfterFailedAssertion(second);
        }
    }

    // Standard initialization can own providers and a real replica before its final init flag is set.
    @Test
    void closingAfterConfigurationFailureReleasesPartiallyInitializedComponents() throws Exception {
        Path resource = Files.writeString(temp.resolve("owned-resource"), "actual content");
        Path replicaDirectory = Files.createDirectory(temp.resolve("replicas"));
        Path configuration = Files.writeString(temp.resolve("broken-providers.xml"), """
                <providers>
                    <provider class-name="com.m3.fs.missing.ControlledMissingProvider">
                        <scheme name="missing"/>
                    </provider>
                </providers>
                """);
        ObservedLocalProvider provider = new ObservedLocalProvider();
        provider.initializationResourcePath = resource;
        ObservedReplicator replicator = new ObservedReplicator(replicaDirectory);
        StandardFileSystemManager manager = new StandardFileSystemManager() {
            @Override
            protected DefaultFileReplicator createDefaultFileReplicator() {
                return replicator;
            }
        };
        manager.setConfiguration(configuration.toUri().toURL());
        manager.setFilesCache(new NullFilesCache());
        manager.setCacheStrategy(CacheStrategy.ON_RESOLVE);
        manager.addProvider("file", provider);
        try {
            assertThrows(FileSystemException.class, manager::init);
            assertTrue(Files.exists(replicator.replica));
            manager.close();
            assertAll(
                    () -> assertEquals(1, provider.closeCalls),
                    () -> assertThrows(IOException.class, () -> provider.initializationResource.read()),
                    () -> assertTrue(replicator.closeCalls > 0),
                    () -> assertFalse(Files.exists(replicator.replica)),
                    () -> assertFalse(manager.hasProvider("file")));
            manager.close();
            assertEquals(1, provider.closeCalls);
        } finally {
            closeAfterFailedAssertion(provider);
            if (replicator.closeCalls == 0) {
                replicator.close();
            }
        }
    }

    // A provider unregistering itself from close must not re-enter the same component's close.
    @Test
    void providerRemovalDuringManagerCloseReleasesTheComponentOnlyOnce() throws Exception {
        Path resource = Files.writeString(temp.resolve("owned-resource"), "actual content");
        DefaultFileSystemManager manager = new DefaultFileSystemManager();
        ObservedLocalProvider provider = new ObservedLocalProvider();
        provider.initializationResourcePath = resource;
        provider.closeAction = () -> manager.removeProvider("file");
        manager.addProvider("file", provider);
        manager.init();
        manager.close();
        assertAll(
                () -> assertEquals(1, provider.closeCalls),
                () -> assertThrows(IOException.class, () -> provider.initializationResource.read()),
                () -> assertFalse(manager.hasProvider("file")));
        manager.close();
        assertEquals(1, provider.closeCalls);
    }

    // A nested close must neither close the active callback twice nor skip the other provider.
    @Test
    void reentrantManagerCloseStillReleasesEachProviderOnce() throws Exception {
        Path resource = Files.writeString(temp.resolve("owned-resource"), "actual content");
        DefaultFileSystemManager manager = new DefaultFileSystemManager();
        ObservedLocalProvider first = new ObservedLocalProvider();
        ObservedLocalProvider second = new ObservedLocalProvider();
        first.initializationResourcePath = resource;
        second.initializationResourcePath = resource;
        manager.addProvider("file", first);
        manager.addProvider("auxiliary", second);
        manager.init();
        FileObject opened = manager.toFileObject(resource.toFile());
        try (InputStream input = opened.getContent().getInputStream()) {
            assertEquals('a', input.read());
        }
        first.closeAction = () -> {
            if (first.closeCalls == 1) {
                manager.close();
                manager.closeFileSystem(opened.getFileSystem());
            }
        };
        manager.close();
        assertAll(
                () -> assertEquals(1, first.closeCalls),
                () -> assertEquals(1, second.closeCalls),
                () -> assertThrows(IOException.class, () -> first.initializationResource.read()),
                () -> assertThrows(IOException.class, () -> second.initializationResource.read()),
                () -> assertFalse(manager.hasProvider("auxiliary")));
    }

    // Standard plugins historically configure manager cache settings before final initialization.
    @Test
    void pluginConfigurationRetainsItsPreInitializationSetterPhase() throws Exception {
        Path configuration = Files.writeString(temp.resolve("empty-providers.xml"), "<providers/>");
        NullFilesCache selectedCache = new NullFilesCache();
        StandardFileSystemManager manager = new StandardFileSystemManager() {
            @Override
            protected void configurePlugins() throws FileSystemException {
                setFilesCache(selectedCache);
                setCacheStrategy(CacheStrategy.ON_CALL);
            }
        };
        manager.setConfiguration(configuration.toUri().toURL());
        try {
            manager.init();
            assertSame(selectedCache, manager.getFilesCache());
            assertEquals(CacheStrategy.ON_CALL, manager.getCacheStrategy());
        } finally {
            manager.close();
        }
    }

    // Name rejection must happen before getSize(), including a file selected as the root.
    @Test
    void cheapNameFiltersAvoidSizeQueriesForRejectedEntries() throws Exception {
        Path folder = Files.createDirectory(temp.resolve("folder"));
        Path rejected = Files.writeString(folder.resolve("skip.bin"), "rejected");
        Files.writeString(folder.resolve("keep.txt"), "accepted");
        ObservedLocalProvider provider = new ObservedLocalProvider();
        MFiles.Plan plan = MFiles.from(address(temp)).withProvider("observed", () -> provider);
        plan = configure(plan, "filterName", new Class<?>[] {Predicate.class},
                (Predicate<String>) name -> name.endsWith(".txt"));
        try (Stream<MFile> entries = plan.stream()) {
            assertEquals(List.of("folder/keep.txt"), entries.map(MFile::relativeName).toList());
            assertEquals(List.of("keep.txt"), provider.sizeRequests);
            assertEquals(0, provider.contentOpens);
        }
        ObservedLocalProvider rootProvider = new ObservedLocalProvider();
        MFiles.Plan rejectedRoot = MFiles.from(address(rejected)).withProvider("observed", () -> rootProvider);
        rejectedRoot = configure(rejectedRoot, "extensions", new Class<?>[] {String[].class},
                (Object) new String[] {"txt", ".json"});
        try (Stream<MFile> entries = rejectedRoot.stream()) {
            assertEquals(0, entries.count());
            assertEquals(List.of(), rootProvider.sizeRequests);
        }
    }

    // DFS and BFS must follow the provider's actual encounter order at different depths.
    @Test
    void depthFirstAndBreadthFirstPreserveProviderOrderAndBoundedDescent() throws Exception {
        Files.createDirectories(temp.resolve("a/deep"));
        Files.createDirectory(temp.resolve("b"));
        Files.writeString(temp.resolve("a/deep/z.txt"), "z");
        Files.writeString(temp.resolve("a/one.txt"), "1");
        Files.writeString(temp.resolve("b/two.txt"), "2");
        Files.writeString(temp.resolve("top.txt"), "top");
        MFiles.Plan base = MFiles.from(address(temp)).withProvider("observed", ObservedLocalProvider::new);
        try (Stream<MFile> entries = base.stream()) {
            assertEquals(List.of("a", "a/deep", "a/deep/z.txt", "a/one.txt", "b", "b/two.txt", "top.txt"),
                    entries.map(MFile::relativeName).toList());
        }
        MFiles.Plan breadthFirst = configure(base, "breadthFirst", new Class<?>[0]);
        try (Stream<MFile> entries = breadthFirst.stream()) {
            assertEquals(List.of("a", "b", "top.txt", "a/deep", "a/one.txt", "b/two.txt", "a/deep/z.txt"),
                    entries.map(MFile::relativeName).toList());
        }
        try (Stream<MFile> entries = breadthFirst.maxDepth(2).descendInto(file -> !file.name().equals("b")).stream()) {
            assertEquals(List.of("a", "b", "top.txt", "a/deep", "a/one.txt"),
                    entries.map(MFile::relativeName).toList());
        }
    }

    // Eager child enumeration in either order reaches the controlled blocked-directory failure.
    @Test
    void breadthFirstShortCircuitDoesNotOpenTheFirstFoldersChildren() throws Exception {
        Files.createDirectory(temp.resolve("blocked"));
        ObservedLocalProvider provider = new ObservedLocalProvider();
        MFiles.Plan plan = configure(MFiles.from(address(temp)).withProvider("observed", () -> provider),
                "breadthFirst", new Class<?>[0]);
        try (Stream<MFile> entries = plan.stream()) {
            assertEquals("blocked", entries.findFirst().orElseThrow().name());
            assertEquals(1, provider.directoryLists);
        }
    }

    // Calling a factory before detecting a built-in collision creates an unregistered owned resource.
    @Test
    void builtinProviderCollisionFailsBeforeInvokingTheFactory() {
        List<ObservedLocalProvider> providers = new ArrayList<>();
        MFiles.Plan plan = MFiles.from(temp).withProvider("file", () -> {
            ObservedLocalProvider provider = new ObservedLocalProvider();
            providers.add(provider);
            return provider;
        });
        assertThrows(IOException.class, plan::stream);
        assertEquals(0, providers.size());
    }

    // Provider output errors must stay checked and cannot silently turn a failed write into success.
    @Test
    void providerOutputOpeningFailureRemainsChecked() throws Exception {
        Path path = Files.writeString(temp.resolve("unwritable"), "original");
        try (Stream<MFile> entries = MFiles.from(address(path))
                .withProvider("observed", ObservedLocalProvider::new).stream()) {
            MFile entry = entries.findFirst().orElseThrow();
            IOException failure = assertThrows(IOException.class, () -> output(entry, false));
            assertTrue(hasMessage(failure, "controlled output failure"));
            assertEquals("original", Files.readString(path));
        }
    }

    // A writer close failure must not strand another writer, a reader, or the provider.
    @Test
    void writerCloseFailureStillClosesAllOtherOwnedContentAndProvider() throws Exception {
        Files.writeString(temp.resolve("write-close-fails"), "first");
        Files.writeString(temp.resolve("normal.txt"), "second");
        ObservedLocalProvider provider = new ObservedLocalProvider();
        try (Stream<MFile> entries = MFiles.from(address(temp)).withProvider("observed", () -> provider).stream()) {
            List<MFile> files = entries.toList();
            InputStream reader = files.getFirst().openInputStream();
            List<OutputStream> writers = new ArrayList<>();
            for (MFile file : files) {
                OutputStream writer = output(file, true);
                writer.write('!');
                writers.add(writer);
            }
            IllegalStateException failure = assertThrows(IllegalStateException.class, () -> closeStream(entries));
            assertTrue(hasMessage(failure, "controlled writer close failure"));
            assertAll(
                    () -> assertEquals(2, provider.outputCloses),
                    () -> assertEquals(1, provider.contentCloses),
                    () -> assertTrue(provider.closed),
                    () -> assertThrows(IOException.class, reader::read));
            for (OutputStream writer : writers) {
                assertThrows(IOException.class, () -> writer.write('!'));
            }
        }
    }

    // Swallowing metadata IO as an absent entry would manufacture a deletion delta.
    @Test
    void observationFailureRemainsCheckedAndDoesNotOpenPayload() throws Exception {
        Path path = Files.writeString(temp.resolve("data.txt"), "content");
        ObservedLocalProvider provider = new ObservedLocalProvider();
        try (Stream<MFile> entries = MFiles.from(address(path)).withProvider("observed", () -> provider).stream()) {
            MFile entry = entries.findFirst().orElseThrow();
            provider.failSize = true;
            IOException failure = assertThrows(IOException.class,
                    () -> invoke(entry, "observation", new Class<?>[0]));
            assertTrue(hasMessage(failure, "controlled metadata failure"));
            assertEquals(0, provider.contentOpens);
            provider.failSize = false;
            Object observation = invoke(entry, "observation", new Class<?>[0]);
            assertEquals("PRESENT", invoke(observation, "presence", new Class<?>[0]).toString());
        }
    }

    // Optional provider stamps must retain their exact source and epoch domain in detached observations.
    @Test
    void observationsCaptureFreshProviderChangeStampsWithoutReadingPayload() throws Exception {
        Path path = Files.writeString(temp.resolve("data.txt"), "content");
        ObservedLocalProvider provider = new ObservedLocalProvider();
        provider.changeToken = "version-one";
        try (Stream<MFile> entries = MFiles.from(address(path)).withProvider("observed", () -> provider).stream()) {
            MFile entry = entries.findFirst().orElseThrow();
            MFileObservation before = entry.observation();
            provider.changeToken = "version-two";
            MFileObservation after = entry.observation();
            assertEquals(entry.sourceAddress(), before.changeStamp().sourceKey());
            assertEquals("test-epoch", before.changeStamp().epoch());
            assertEquals("version-one", before.changeStamp().token());
            assertEquals("version-two", after.changeStamp().token());
            assertTrue(before.changeStamp().authoritative());
            assertEquals(0, provider.contentOpens);
        }
    }

    // Owner shutdown can close the raw reader before its outer monitored wrapper. A failed close
    // must remain visible, or that wrapper could later publish a capture from failed cleanup.
    @Test
    void failedOwnerCloseCannotLaterBecomeASuccessfulCapture() throws Exception {
        Path path = Files.writeString(temp.resolve("close-fails"), "abc");
        ObservedLocalProvider provider = new ObservedLocalProvider();
        List<Object> captures = new ArrayList<>();
        try (Stream<MFile> entries = MFiles.from(address(path)).withProvider("observed", () -> provider).stream()) {
            MFile file = entries.findFirst().orElseThrow();
            MFileContent content = (MFileContent) invoke(file, "captureReads", new Class<?>[] {Consumer.class},
                    (Consumer<Object>) captures::add);
            InputStream input = content.openInputStream();
            assertEquals("abc", new String(input.readAllBytes(), StandardCharsets.UTF_8));
            assertTrue(captures.isEmpty());
            assertTrue(hasMessage(assertThrows(RuntimeException.class, () -> closeStream(entries)), "controlled close failure"));
            assertTrue(provider.closed);
            assertEquals(1, provider.contentCloses);
            assertTrue(hasMessage(assertThrows(RuntimeException.class, input::close), "controlled close failure"));
            input.close();
            assertEquals(1, provider.contentCloses);
            assertTrue(captures.isEmpty());
        }
    }

    // Reentry happens before the first provider close can report its failure. A closed bit alone
    // must not let the surrounding wrapper publish while primary cleanup is still in progress.
    @Test
    void reentrantWrapperCloseDuringOwnerCleanupCannotPublishPrematureCapture() throws Exception {
        Path path = Files.writeString(temp.resolve("close-fails"), "abc");
        ObservedLocalProvider provider = new ObservedLocalProvider();
        List<Object> captures = new ArrayList<>();
        List<Throwable> reentrantFailures = new ArrayList<>();
        try (Stream<MFile> entries = MFiles.from(address(path)).withProvider("observed", () -> provider).stream()) {
            MFile file = entries.findFirst().orElseThrow();
            MFileContent content = (MFileContent) invoke(file, "captureReads", new Class<?>[] {Consumer.class},
                    (Consumer<Object>) captures::add);
            InputStream input = content.openInputStream();
            assertEquals(3, input.readAllBytes().length);
            provider.contentCloseAction = () -> {
                try {
                    input.close();
                } catch (IOException | RuntimeException | Error failure) {
                    reentrantFailures.add(failure);
                }
            };
            assertTrue(hasMessage(assertThrows(RuntimeException.class, () -> closeStream(entries)), "controlled close failure"));
            assertTrue(captures.isEmpty(), "Reentrant close must not publish before primary cleanup returns");
            assertEquals(1, reentrantFailures.size());
            assertTrue(reentrantFailures.getFirst() instanceof IOException);
            assertTrue(captures.isEmpty());
            assertEquals(1, provider.contentCloses);
            assertTrue(provider.closed);
            input.close();
            assertTrue(captures.isEmpty());
        }
    }

    private static String address(Path path) {
        return "observed:" + path.toAbsolutePath();
    }

    private static void closeStream(Stream<?> stream) {
        stream.close();
    }

    private static MFiles.Plan configure(MFiles.Plan plan, String method, Class<?>[] types, Object... values)
            throws Exception {
        return (MFiles.Plan) invoke(plan, method, types, values);
    }

    private static OutputStream output(MFile entry, boolean append) throws Exception {
        return (OutputStream) invoke(entry, "openOutputStream", new Class<?>[] {boolean.class}, append);
    }

    private static Object invoke(Object target, String method, Class<?>[] types, Object... values) throws Exception {
        try {
            return target.getClass().getMethod(method, types).invoke(target, values);
        } catch (InvocationTargetException wrapper) {
            Throwable cause = wrapper.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new AssertionError(cause);
        }
    }

    private static boolean hasMessage(Throwable exception, String expected) {
        for (Throwable current = exception; current != null; current = current.getCause()) {
            if (current.getMessage() != null && current.getMessage().contains(expected)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasSuppressed(Throwable failure, Throwable expected) {
        for (Throwable suppressed : failure.getSuppressed()) {
            if (suppressed == expected || hasSuppressed(suppressed, expected)) {
                return true;
            }
        }
        return false;
    }

    private static void closeAfterFailedAssertion(ObservedLocalProvider provider) {
        if (!provider.closed) {
            try {
                provider.close();
            } catch (RuntimeException ignored) {
                // Release real fixture resources even when the regression intentionally fails.
            }
        }
    }

    private static final class ObservedReplicator extends DefaultFileReplicator {
        private Path replica;
        private int closeCalls;

        ObservedReplicator(Path directory) {
            super(directory.toFile());
        }

        @Override
        public void init() throws FileSystemException {
            super.init();
            if (replica == null) {
                replica = allocateFile("owned-replica").toPath();
                try {
                    Files.writeString(replica, "actual replica content");
                } catch (IOException failure) {
                    throw new FileSystemException("controlled replica creation failure", failure);
                }
            }
        }

        @Override
        public void close() {
            closeCalls++;
            super.close();
        }
    }

    private static final class ObservedLocalProvider extends DefaultLocalFileProvider {
        private int contentOpens;
        private int contentCloses;
        private int closeCalls;
        private int outputCloses;
        private int directoryLists;
        private final List<String> sizeRequests = new ArrayList<>();
        private boolean failSize;
        private String changeToken;
        private boolean closed;
        private Path initializationResourcePath;
        private Path failInitializationWithResource;
        private InputStream initializationResource;
        private RuntimeException closeFailure;
        private Runnable closeAction;
        private Runnable contentCloseAction;

        @Override
        public void init() throws FileSystemException {
            super.init();
            Path resource = initializationResourcePath == null
                    ? failInitializationWithResource : initializationResourcePath;
            if (resource != null) {
                try {
                    initializationResource = Files.newInputStream(resource);
                } catch (IOException failure) {
                    throw new FileSystemException("controlled initialization resource failure", failure);
                }
            }
            if (failInitializationWithResource != null) {
                throw new FileSystemException("controlled initialization failure");
            }
        }

        @Override
        protected FileSystem doCreateFileSystem(FileName name, FileSystemOptions options) {
            LocalFileName local = (LocalFileName) name;
            return new LocalFileSystem(local, local.getRootFile(), options) {
                @Override
                protected FileObject createFile(AbstractFileName fileName) {
                    return new ObservedLocalFile(this, local.getRootFile(), fileName, ObservedLocalProvider.this);
                }
            };
        }

        @Override
        public void close() {
            closeCalls++;
            try {
                super.close();
            } finally {
                closed = true;
                if (initializationResource != null) {
                    try {
                        initializationResource.close();
                    } catch (IOException failure) {
                        throw new UncheckedIOException(failure);
                    }
                }
            }
            if (closeAction != null) {
                closeAction.run();
            }
            if (closeFailure != null) {
                throw closeFailure;
            }
        }
    }

    private static final class ObservedLocalFile extends LocalFile implements MFileChangeSource {
        private final ObservedLocalProvider observer;

        ObservedLocalFile(LocalFileSystem fileSystem, String root, AbstractFileName name,
                ObservedLocalProvider observer) {
            super(fileSystem, root, name);
            this.observer = observer;
        }

        @Override
        public MFileObservation.ChangeStamp changeStamp() {
            return observer.changeToken == null ? MFileObservation.ChangeStamp.unavailable()
                    : MFileObservation.ChangeStamp.authoritative(getName().getURI(), "entry", "raw",
                            "controlled-local-provider", "test-epoch", observer.changeToken);
        }

        @Override
        protected long doGetContentSize() throws Exception {
            observer.sizeRequests.add(getName().getBaseName());
            if (observer.failSize) {
                throw new IOException("controlled metadata failure");
            }
            return getName().getBaseName().equals("unknown-size") ? -1 : super.doGetContentSize();
        }

        @Override
        protected FileType doGetType() {
            return getName().getBaseName().equals("hybrid") ? FileType.FILE_OR_FOLDER : super.doGetType();
        }

        @Override
        protected InputStream doGetInputStream(int bufferSize) throws Exception {
            if (getName().getBaseName().equals("unreadable")) {
                throw new IOException("controlled content failure");
            }
            observer.contentOpens++;
            return new FilterInputStream(super.doGetInputStream(bufferSize)) {
                private boolean closed;

                @Override
                public void close() throws IOException {
                    if (closed) {
                        return;
                    }
                    closed = true;
                    observer.contentCloses++;
                    super.close();
                    if (observer.contentCloseAction != null) {
                        observer.contentCloseAction.run();
                    }
                    if (getName().getBaseName().equals("close-fails")) {
                        throw new IllegalStateException("controlled close failure");
                    }
                }
            };
        }

        @Override
        protected String[] doListChildren() throws Exception {
            observer.directoryLists++;
            if (getName().getBaseName().equals("blocked")) {
                throw new FileSystemException("controlled listing failure");
            }
            String[] children = super.doListChildren();
            Arrays.sort(children);
            return children;
        }

        @Override
        protected OutputStream doGetOutputStream(boolean append) throws IOException {
            if (getName().getBaseName().equals("unwritable")) {
                throw new IOException("controlled output failure");
            }
            return new FilterOutputStream(super.doGetOutputStream(append)) {
                private boolean closed;

                @Override
                public void close() throws IOException {
                    if (closed) {
                        return;
                    }
                    closed = true;
                    observer.outputCloses++;
                    super.close();
                    if (getName().getBaseName().equals("write-close-fails")) {
                        throw new IllegalStateException("controlled writer close failure");
                    }
                }
            };
        }
    }
}
