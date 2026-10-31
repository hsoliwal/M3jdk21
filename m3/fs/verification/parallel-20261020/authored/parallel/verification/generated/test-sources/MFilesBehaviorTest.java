/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The same consumer suite runs against the pre-facade baseline and generated
 * receiver. Reflection keeps an absent facade an explicit assertion failure,
 * rather than making the baseline test fail to compile.
 */
class MFilesBehaviorTest {
    @TempDir
    Path temp;

    // Removing recursive enumeration, or including the directory root, fails this test.
    @Test
    void directoryTraversalIncludesFilesAndFoldersButExcludesRoot() throws Exception {
        Path root = tree();
        try (Stream<?> stream = open(plan(root))) {
            assertFalse(stream.isParallel());
            assertEquals(Set.of("keep", "keep/a.txt", "keep/b.bin", "skip", "skip/deep",
                    "skip/deep/c.txt", "top.txt"), names(stream));
        }
    }

    // Reusing a mutable cursor would change previously returned names or identities.
    @Test
    void collectedEntriesHaveDistinctStableMetadata() throws Exception {
        List<?> entries;
        try (Stream<?> stream = open(plan(tree()))) {
            entries = stream.toList();
        }
        assertEquals(7, entries.size());
        for (int i = 0; i < entries.size(); i++) {
            for (int j = i + 1; j < entries.size(); j++) {
                assertNotSame(entries.get(i), entries.get(j));
            }
        }
        assertEquals(7, new HashSet<>(entries.stream().map(MFilesBehaviorTest::relative).toList()).size());
        Object file = entries.stream().filter(e -> relative(e).equals("top.txt")).findFirst().orElseThrow();
        assertEquals("top.txt", text(file, "name"));
        assertTrue(flag(file, "isFile"));
        assertFalse(flag(file, "isDirectory"));
        assertFalse(flag(file, "isSymbolicLink"));
        assertEquals(OptionalLong.of(3), call(file, "size"));
        assertInstanceOf(URI.class, call(file, "uri"));
    }

    // Treating an output exclusion as pruning would lose nested text files.
    @Test
    void outputFilterDoesNotPruneDirectories() throws Exception {
        Object plan = filter(plan(tree()), e -> flag(e, "isFile") && relative(e).endsWith(".txt"));
        try (Stream<?> stream = open(plan)) {
            assertEquals(Set.of("keep/a.txt", "skip/deep/c.txt", "top.txt"), names(stream));
        }
    }

    // Ignoring the descent predicate would include skip/deep and its descendants.
    @Test
    void descentPredicatePrunesChildrenButStillEmitsTheFolder() throws Exception {
        Object plan = call(plan(tree()), "descendInto", new Class<?>[] {Predicate.class},
                (Predicate<Object>) e -> !text(e, "name").equals("skip"));
        try (Stream<?> stream = open(plan)) {
            assertEquals(Set.of("keep", "keep/a.txt", "keep/b.bin", "skip", "top.txt"), names(stream));
        }
    }

    // Off-by-one depth handling would include grandchildren or omit immediate children.
    @Test
    void finiteDepthUsesSelectedRootAsDepthZero() throws Exception {
        Object base = plan(tree());
        try (Stream<?> stream = open(depth(base, 0))) {
            assertEquals(Set.of(), names(stream));
        }
        try (Stream<?> stream = open(depth(base, 1))) {
            assertEquals(Set.of("keep", "skip", "top.txt"), names(stream));
        }
        try (Stream<?> stream = open(depth(base, 2))) {
            assertEquals(Set.of("keep", "keep/a.txt", "keep/b.bin", "skip", "skip/deep", "top.txt"), names(stream));
        }
        assertThrows(IllegalArgumentException.class, () -> depth(base, -1));
    }

    // Dropping root files or applying directory depth rules to them fails this test.
    @Test
    void rootFileIsEmittedAtDepthZero() throws Exception {
        Path file = Files.writeString(temp.resolve("single.txt"), "single");
        try (Stream<?> stream = open(depth(plan(file), 0))) {
            List<?> entries = stream.toList();
            assertEquals(1, entries.size());
            assertEquals("single.txt", relative(entries.getFirst()));
            assertEquals("single", read(entries.getFirst()));
        }
    }

    // URI decoding mistakes corrupt literal percent, spaces, or Unicode characters.
    @Test
    void localNamesPreserveUnicodePercentAndSpaces() throws Exception {
        Path root = Files.createDirectory(temp.resolve("目录 % root"));
        String name = "café 日本 %25 % snow ☃.txt";
        Path file = Files.writeString(root.resolve(name), "unicode data");
        try (Stream<?> stream = open(plan(root))) {
            Object entry = stream.findFirst().orElseThrow();
            assertEquals(name, text(entry, "name"));
            assertEquals(name, relative(entry));
            assertEquals("unicode data", read(entry));
            assertEquals(file, Path.of((URI) call(entry, "uri")));
            assertFalse(text(entry, "sourceAddress").isEmpty());
        }
    }

    // Failure to forward the explicit zip provider URI loses archive entries or their content.
    @Test
    void zipRootUriStreamsMetadataAndExplicitContent() throws Exception {
        Path zip = temp.resolve("archive.zip");
        writeZip(zip, new String[] {"dir/", "dir/café %25.txt", "plain.txt"},
                new byte[][] {new byte[0], utf8("inside"), utf8("plain")});
        try (Stream<?> stream = open(plan("zip:" + zip.toUri() + "!/"))) {
            List<?> entries = stream.toList();
            assertEquals(Set.of("dir", "dir/café %25.txt", "plain.txt"), relativeNames(entries));
            assertEquals("inside", read(entries.stream().filter(e -> relative(e).equals("dir/café %25.txt"))
                    .findFirst().orElseThrow()));
        }
    }

    // Flattening or splitting a layered URI incorrectly would miss the nested archive.
    @Test
    void nestedZipUriUsesTheProviderLayering() throws Exception {
        ByteArrayOutputStream inner = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(inner, StandardCharsets.UTF_8)) {
            zip.putNextEntry(new ZipEntry("deep.txt"));
            zip.write(utf8("nested"));
            zip.closeEntry();
        }
        Path outer = temp.resolve("outer.zip");
        writeZip(outer, new String[] {"inner.zip"}, new byte[][] {inner.toByteArray()});
        try (Stream<?> stream = open(plan("zip:zip:" + outer.toUri() + "!/inner.zip!/"))) {
            Object entry = stream.findFirst().orElseThrow();
            assertEquals("deep.txt", relative(entry));
            assertEquals("nested", read(entry));
        }
    }

    // Mutating plan fields in filter/maxDepth would change the reusable base plan.
    @Test
    void plansAreImmutableAndRepeatExecutionSeesCurrentProviderState() throws Exception {
        Path root = temp.resolve("created-after-plan");
        Object base = plan(root);
        Object textOnly = filter(base, e -> relative(e).endsWith(".txt"));
        Object shallow = depth(base, 0);
        Files.createDirectory(root);
        Files.writeString(root.resolve("one.txt"), "1");
        Files.writeString(root.resolve("two.bin"), "2");
        try (Stream<?> stream = open(shallow)) {
            assertEquals(Set.of(), names(stream));
        }
        try (Stream<?> stream = open(textOnly)) {
            assertEquals(Set.of("one.txt"), names(stream));
        }
        try (Stream<?> stream = open(base)) {
            assertEquals(Set.of("one.txt", "two.bin"), names(stream));
        }
        Files.writeString(root.resolve("three.txt"), "3");
        try (Stream<?> stream = open(textOnly)) {
            assertEquals(Set.of("one.txt", "three.txt"), names(stream));
        }
    }

    // Making from(String) resolve immediately would reject this deliberately unknown provider.
    @Test
    void providerResolutionIsDeferredUntilStreamOpen() throws Exception {
        Object invalid = plan("m3-no-such-provider:/not-present");
        assertThrows(IOException.class, () -> open(invalid));
    }

    // Silently treating an absent local root as an empty traversal would hide an IO failure.
    @Test
    void missingRootReportsCheckedIoFailureOnOpen() throws Exception {
        Object missing = plan(temp.resolve("not-created"));
        assertThrows(IOException.class, () -> open(missing));
    }

    // Failing to preserve Java stream close handlers in the convenience overload leaks ownership.
    @Test
    void lambdaConvenienceOverloadsFilterAndTransformActualEntries() throws Exception {
        Path root = tree();
        Predicate<Object> onlyText = e -> flag(e, "isFile") && relative(e).endsWith(".txt");
        Function<Object, String> uppercase = e -> relative(e).toUpperCase(java.util.Locale.ROOT);
        try (Stream<?> stream = convenience(root, onlyText, uppercase)) {
            assertEquals(Set.of("KEEP/A.TXT", "SKIP/DEEP/C.TXT", "TOP.TXT"), new HashSet<>(stream.toList()));
        }
        try (Stream<?> stream = convenience(root.toUri().toString(), onlyText, uppercase)) {
            assertEquals(Set.of("KEEP/A.TXT", "SKIP/DEEP/C.TXT", "TOP.TXT"), new HashSet<>(stream.toList()));
        }
    }

    // Closing the stream must expire reads even when findFirst never exhausts traversal.
    @Test
    void shortCircuitCloseRetainsMetadataAndExpiresContent() throws Exception {
        Object retained;
        try (Stream<?> stream = open(filter(plan(tree()), e -> flag(e, "isFile")))) {
            retained = stream.findFirst().orElseThrow();
            assertFalse(read(retained).isEmpty());
        }
        String original = relative(retained);
        assertFalse(original.isEmpty());
        assertTrue(flag(retained, "isFile"));
        IOException error = assertThrows(IOException.class, () -> input(retained));
        assertTrue(error.getMessage().toLowerCase(java.util.Locale.ROOT).contains("closed"));
        assertEquals(original, relative(retained));
    }

    // Tracking only returned MFile objects, instead of opened streams, leaves this reader usable.
    @Test
    void closeClosesOutstandingContentStreams() throws Exception {
        Path file = Files.writeString(temp.resolve("content.txt"), "outstanding stream");
        InputStream input;
        try (Stream<?> stream = open(plan(file))) {
            input = input(stream.findFirst().orElseThrow());
            assertEquals('o', input.read());
        }
        assertThrows(IOException.class, input::read);
        input.close();
    }

    // Automatically closing on exhaustion prevents legitimate collected-entry reads inside the scope.
    @Test
    void entriesCanBeReadAfterTraversalExhaustionWhileOwnerIsOpen() throws Exception {
        Path root = tree();
        try (Stream<?> stream = open(filter(plan(root), e -> flag(e, "isFile")))) {
            List<?> entries = stream.toList();
            assertEquals(4, entries.size());
            for (Object entry : entries) {
                assertFalse(read(entry).isEmpty());
            }
        }
    }

    // Following directory symlinks traverses outside the selected tree or loops back to the root.
    @Test
    void directorySymlinksAreEmittedWithoutDescent() throws Exception {
        Path root = Files.createDirectory(temp.resolve("links"));
        Path real = Files.createDirectory(root.resolve("real"));
        Files.writeString(real.resolve("inside.txt"), "inside");
        Files.createSymbolicLink(root.resolve("alias"), real);
        Files.createSymbolicLink(root.resolve("cycle"), root);
        try (Stream<?> stream = open(plan(root))) {
            List<?> entries = stream.toList();
            assertEquals(Set.of("real", "real/inside.txt", "alias", "cycle"), relativeNames(entries));
            Object link = entries.stream().filter(e -> relative(e).equals("alias")).findFirst().orElseThrow();
            assertTrue(flag(link, "isSymbolicLink"));
        }
    }

    // A symbolic link selected as the root must not be a way around the no-descent rule.
    @Test
    void directorySymlinkRootDoesNotDescend() throws Exception {
        Path real = Files.createDirectory(temp.resolve("real-root"));
        Files.writeString(real.resolve("inside.txt"), "inside");
        Path link = Files.createSymbolicLink(temp.resolve("root-link"), real);
        try (Stream<?> stream = open(plan(link))) {
            assertEquals(0, stream.count());
        }
    }

    // Name exclusions must not prune directories; suffix matching must not accept trailing junk.
    @Test
    void extensionAndNameFiltersComposeWithoutChangingTheBasePlan() throws Exception {
        Path root = Files.createDirectory(temp.resolve("names"));
        Path folder = Files.createDirectory(root.resolve("folder"));
        for (String name : List.of("data.txt", "data.json", "data.bin", "data.TXT", "data.txt.bak", "other.txt")) {
            Files.writeString(folder.resolve(name), name);
        }
        Object base = plan(root);
        Object selected = extensions(base, ".txt", "json", ".bin");
        selected = call(selected, "filterName", new Class<?>[] {Predicate.class},
                (Predicate<String>) name -> name.startsWith("data."));
        try (Stream<?> stream = open(selected)) {
            assertEquals(Set.of("folder/data.txt", "folder/data.json", "folder/data.bin"), names(stream));
        }
        try (Stream<?> stream = open(base)) {
            assertEquals(7, stream.count());
        }
        try (Stream<?> stream = open(extensions(base))) {
            assertEquals(0, stream.count());
        }
        assertThrows(IllegalArgumentException.class, () -> extensions(base, "."));
    }

    // Merely traversing an entry must not truncate it; explicit append/replace must honor their flags.
    @Test
    void explicitOutputStreamsReplaceAndAppendRealLocalContent() throws Exception {
        Path path = Files.writeString(temp.resolve("writable.txt"), "original");
        try (Stream<?> stream = open(plan(path))) {
            Object entry = stream.findFirst().orElseThrow();
            assertEquals("original", Files.readString(path));
            try (OutputStream output = output(entry, false)) {
                output.write(utf8("new"));
            }
            try (OutputStream output = output(entry, true)) {
                output.write(utf8(" content"));
            }
            assertEquals("new content", Files.readString(path));
            assertEquals(OptionalLong.of(8), call(entry, "size"));
        }
    }

    // Untracked writers remain usable after close and can silently write outside the owner scope.
    @Test
    void ownerCloseFlushesOutstandingWriterAndExpiresAllFutureWrites() throws Exception {
        Path path = Files.writeString(temp.resolve("writer.txt"), "old");
        Object entry;
        OutputStream output;
        try (Stream<?> stream = open(plan(path))) {
            entry = stream.findFirst().orElseThrow();
            output = output(entry, false);
            output.write(utf8("owned write"));
        }
        assertEquals("owned write", Files.readString(path));
        assertThrows(IOException.class, () -> output.write('!'));
        assertThrows(IOException.class, output::flush);
        assertThrows(IOException.class, () -> output(entry, true));
        output.close();
        assertEquals("writer.txt", relative(entry));
    }

    // A read-only archive must reject the open as checked IO and retain its original entry bytes.
    @Test
    void archiveOutputOpeningFailsWithoutChangingTheEntry() throws Exception {
        Path archive = temp.resolve("read-only.zip");
        writeZip(archive, new String[] {"inside.txt"}, new byte[][] {utf8("inside")});
        try (Stream<?> stream = open(plan("zip:" + archive.toUri() + "!/"))) {
            Object entry = stream.findFirst().orElseThrow();
            assertThrows(IOException.class, () -> output(entry, false));
            assertEquals("inside", read(entry));
        }
    }

    // Layer traversal must keep each mounted namespace separate and include the archive boundary.
    @Test
    void optionalArchiveTraversalReachesNestedZipWithUnambiguousNames() throws Exception {
        Path outer = nestedArchive();
        Object layered = archives(plan(outer), entry -> text(entry, "name").endsWith(".zip"), 2);
        try (Stream<?> stream = open(layered)) {
            List<?> entries = stream.toList();
            assertEquals(Set.of("outer.zip", "outer.zip!/inner.zip", "outer.zip!/plain.txt",
                    "outer.zip!/inner.zip!/deep.txt"), relativeNames(entries));
            Object deep = entries.stream().filter(entry -> relative(entry).endsWith("!/deep.txt"))
                    .findFirst().orElseThrow();
            assertEquals("nested", read(deep));
            assertTrue(text(deep, "sourceAddress").startsWith("zip:zip:"));
        }
    }

    // Output exclusions must not prevent a selected archive layer from being entered.
    @Test
    void selectedArchiveExpansionIgnoresOutputNameAndMetadataFilters() throws Exception {
        Path outer = nestedArchive();
        Object layered = archives(plan(outer), entry -> text(entry, "name").endsWith(".zip"), 2);
        layered = extensions(layered, "txt");
        layered = filter(layered, entry -> relative(entry).endsWith("deep.txt"));
        try (Stream<?> stream = open(layered)) {
            Object deep = stream.findFirst().orElseThrow();
            assertEquals("outer.zip!/inner.zip!/deep.txt", relative(deep));
            assertEquals("nested", read(deep));
        }
    }

    // Display-layer notation is not an identity key when a real directory contains a literal bang.
    @Test
    void literalBangDisplayCollisionRetainsDistinctResolvableSourceAddresses() throws Exception {
        Path root = Files.createDirectory(temp.resolve("colliding-display-names"));
        writeZip(root.resolve("outer.zip"), new String[] {"file.txt"}, new byte[][] {utf8("archive")});
        Path literal = Files.createDirectory(root.resolve("outer.zip!"));
        Files.writeString(literal.resolve("file.txt"), "directory");
        try (Stream<?> stream = open(archives(plan(root), entry -> text(entry, "name").endsWith(".zip"), 1))) {
            List<?> matches = stream.filter(entry -> relative(entry).equals("outer.zip!/file.txt")).toList();
            assertEquals(2, matches.size());
            assertEquals(2, new HashSet<>(matches.stream().map(entry -> text(entry, "sourceAddress")).toList()).size());
            assertEquals(Set.of("archive", "directory"), Set.of(read(matches.get(0)), read(matches.get(1))));
        }
    }

    // Layer budgets and global depth boundaries are independent; both must bound recursion.
    @Test
    void archiveLayerBudgetGlobalDepthAndSelectorBoundExpansion() throws Exception {
        Path outer = nestedArchive();
        Object base = plan(outer);
        try (Stream<?> stream = open(base)) {
            assertEquals(Set.of("outer.zip"), names(stream));
        }
        try (Stream<?> stream = open(archives(base, entry -> true, 0))) {
            assertEquals(Set.of("outer.zip"), names(stream));
        }
        Set<String> oneLayer = Set.of("outer.zip", "outer.zip!/inner.zip", "outer.zip!/plain.txt");
        try (Stream<?> stream = open(archives(base, entry -> true, 1))) {
            assertEquals(oneLayer, names(stream));
        }
        try (Stream<?> stream = open(depth(archives(base, entry -> true, 2), 1))) {
            assertEquals(oneLayer, names(stream));
        }
        try (Stream<?> stream = open(archives(base, entry -> !text(entry, "name").equals("inner.zip"), 2))) {
            assertEquals(oneLayer, names(stream));
        }
        assertThrows(IllegalArgumentException.class, () -> archives(base, entry -> true, -1));
    }

    // Eager mounting would reject this corrupt archive before findFirst can return its ordinary entry.
    @Test
    void archiveMountingIsDeferredUntilTraversalContinuesPastTheArchiveEntry() throws Exception {
        Path corrupt = Files.writeString(temp.resolve("corrupt.zip"), "this is not a ZIP archive");
        AtomicInteger selections = new AtomicInteger();
        Object layered = archives(plan(corrupt), entry -> {
            selections.incrementAndGet();
            return true;
        }, 1);
        try (Stream<?> stream = open(layered)) {
            assertEquals("corrupt.zip", relative(stream.findFirst().orElseThrow()));
            assertEquals(0, selections.get());
        }
        try (Stream<?> stream = open(layered)) {
            var entries = stream.iterator();
            assertEquals("corrupt.zip", relative(entries.next()));
            assertThrows(UncheckedIOException.class, entries::hasNext);
        }
        assertEquals(1, selections.get());
    }

    // A fresh observation must reflect external changes while earlier observations and MFile stay stable.
    @Test
    void explicitObservationRefreshesMetadataAndRetainsStableValuesAfterClose() throws Exception {
        Path path = Files.writeString(temp.resolve("observed.txt"), "old");
        Files.setLastModifiedTime(path, FileTime.fromMillis(0));
        Object entry;
        Object before;
        Object after;
        try (Stream<?> stream = open(plan(path))) {
            entry = stream.findFirst().orElseThrow();
            before = call(entry, "observation");
            assertEquals(text(entry, "sourceAddress"), call(before, "sourceKey"));
            assertEquals(OptionalLong.of(3), call(before, "size"));
            assertEquals(OptionalLong.of(0), call(before, "modifiedTimeMillis"));
            assertEquals("PRESENT", call(before, "presence").toString());
            Files.writeString(path, "updated bytes");
            Files.setLastModifiedTime(path, FileTime.fromMillis(2_000));
            after = call(entry, "observation");
            assertEquals(OptionalLong.of(13), call(after, "size"));
            assertEquals(OptionalLong.of(2_000), call(after, "modifiedTimeMillis"));
            assertEquals(OptionalLong.of(3), call(entry, "size"));
        }
        assertEquals(OptionalLong.of(3), call(before, "size"));
        assertEquals(OptionalLong.of(13), call(after, "size"));
        assertThrows(IOException.class, () -> call(entry, "observation"));
    }

    // A provider-confirmed absent path is different from an observation that could not be collected.
    @Test
    void explicitObservationReportsDeletionOnlyAfterSuccessfulRefresh() throws Exception {
        Path path = Files.writeString(temp.resolve("deleted.txt"), "content");
        try (Stream<?> stream = open(plan(path))) {
            Object entry = stream.findFirst().orElseThrow();
            Object before = call(entry, "observation");
            Files.delete(path);
            Object after = call(entry, "observation");
            assertEquals("PRESENT", call(before, "presence").toString());
            assertEquals("ABSENT", call(after, "presence").toString());
            assertEquals(OptionalLong.empty(), call(after, "size"));
        }
    }

    // Signed local FileTime values are valid timestamps, rather than a generic unknown-size sentinel.
    @Test
    void localObservationPreservesKnownPreEpochModificationTime() throws Exception {
        Path path = Files.writeString(temp.resolve("pre-epoch.txt"), "content");
        Files.setLastModifiedTime(path, FileTime.fromMillis(-2_000));
        try (Stream<?> stream = open(plan(path))) {
            Object observation = call(stream.findFirst().orElseThrow(), "observation");
            assertEquals(OptionalLong.of(-2_000), call(observation, "modifiedTimeMillis"));
        }
    }

    // ZipEntry.getTime() has a provider-specific -1 sentinel that must not become a known timestamp.
    @Test
    void zipUnknownModificationSentinelRemainsUnknown() throws Exception {
        Path path = temp.resolve("unknown-time.zip");
        long windowsTime = 116_444_736_000_000_000L - 10_000L;
        byte[] extra = ByteBuffer.allocate(36).order(ByteOrder.LITTLE_ENDIAN)
                .putShort((short) 0x000a).putShort((short) 32).putInt(0)
                .putShort((short) 1).putShort((short) 24)
                .putLong(windowsTime).putLong(windowsTime).putLong(windowsTime).array();
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(path))) {
            ZipEntry entry = new ZipEntry("unknown.txt");
            entry.setTime(0);
            entry.setExtra(extra);
            zip.putNextEntry(entry);
            zip.write(utf8("content"));
            zip.closeEntry();
        }
        try (ZipFile zip = new ZipFile(path.toFile())) {
            assertEquals(-1L, zip.getEntry("unknown.txt").getTime(), "fixture exposes the JDK ZIP sentinel");
        }
        try (Stream<?> stream = open(plan("zip:" + path.toUri() + "!/"))) {
            Object observation = call(stream.findFirst().orElseThrow(), "observation");
            assertEquals(OptionalLong.empty(), call(observation, "modifiedTimeMillis"));
        }
    }

    private Path nestedArchive() throws IOException {
        ByteArrayOutputStream inner = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(inner, StandardCharsets.UTF_8)) {
            zip.putNextEntry(new ZipEntry("deep.txt"));
            zip.write(utf8("nested"));
            zip.closeEntry();
        }
        Path outer = temp.resolve("outer.zip");
        writeZip(outer, new String[] {"inner.zip", "plain.txt"}, new byte[][] {inner.toByteArray(), utf8("plain")});
        return outer;
    }

    private Path tree() throws IOException {
        Path root = Files.createDirectory(temp.resolve("tree"));
        Files.createDirectories(root.resolve("keep"));
        Files.createDirectories(root.resolve("skip/deep"));
        Files.writeString(root.resolve("keep/a.txt"), "a");
        Files.writeString(root.resolve("keep/b.bin"), "b");
        Files.writeString(root.resolve("skip/deep/c.txt"), "c");
        Files.writeString(root.resolve("top.txt"), "top");
        return root;
    }

    private static byte[] utf8(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static void writeZip(Path file, String[] names, byte[][] contents) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(file), StandardCharsets.UTF_8)) {
            for (int i = 0; i < names.length; i++) {
                zip.putNextEntry(new ZipEntry(names[i]));
                zip.write(contents[i]);
                zip.closeEntry();
            }
        }
    }

    private static Class<?> facade() {
        try {
            return Class.forName("com.m3.fs.MFiles");
        } catch (ClassNotFoundException exception) {
            return fail("M3FS does not yet expose the required com.m3.fs.MFiles stream facade", exception);
        }
    }

    private static Object plan(Path root) throws Exception {
        return invoke(facade().getMethod("from", Path.class), null, root);
    }

    private static Object plan(String root) throws Exception {
        return invoke(facade().getMethod("from", String.class), null, root);
    }

    private static Object filter(Object plan, Predicate<Object> filter) throws Exception {
        return call(plan, "filter", new Class<?>[] {Predicate.class}, filter);
    }

    private static Object depth(Object plan, int maxDepth) throws Exception {
        return call(plan, "maxDepth", new Class<?>[] {int.class}, maxDepth);
    }

    private static Object extensions(Object plan, String... suffixes) throws Exception {
        return call(plan, "extensions", new Class<?>[] {String[].class}, (Object) suffixes);
    }

    private static Object archives(Object plan, Predicate<Object> select, int layers) throws Exception {
        return call(plan, "enterArchives", new Class<?>[] {Predicate.class, int.class}, select, layers);
    }

    private static Stream<?> open(Object plan) throws Exception {
        return (Stream<?>) call(plan, "stream");
    }

    private static Stream<?> convenience(Object root, Predicate<Object> predicate, Function<Object, String> mapper)
            throws Exception {
        Class<?> type = root instanceof Path ? Path.class : String.class;
        return (Stream<?>) invoke(facade().getMethod("stream", type, Predicate.class, Function.class),
                null, root, predicate, mapper);
    }

    private static Set<String> names(Stream<?> stream) {
        return relativeNames(stream.toList());
    }

    private static Set<String> relativeNames(List<?> entries) {
        List<String> names = new ArrayList<>();
        entries.forEach(entry -> names.add(relative(entry)));
        assertEquals(names.size(), new HashSet<>(names).size(), "each hierarchy name occurs once");
        return Set.copyOf(names);
    }

    private static String relative(Object entry) {
        return text(entry, "relativeName");
    }

    private static String text(Object target, String method) {
        try {
            return (String) call(target, method);
        } catch (Exception exception) {
            throw new AssertionError("Could not call " + method, exception);
        }
    }

    private static boolean flag(Object target, String method) {
        try {
            return (boolean) call(target, method);
        } catch (Exception exception) {
            throw new AssertionError("Could not call " + method, exception);
        }
    }

    private static InputStream input(Object entry) throws Exception {
        return (InputStream) call(entry, "openInputStream");
    }

    private static OutputStream output(Object entry, boolean append) throws Exception {
        return (OutputStream) call(entry, "openOutputStream", new Class<?>[] {boolean.class}, append);
    }

    private static String read(Object entry) throws Exception {
        try (InputStream input = input(entry)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Object call(Object target, String name) throws Exception {
        return call(target, name, new Class<?>[0]);
    }

    private static Object call(Object target, String name, Class<?>[] types, Object... arguments) throws Exception {
        return invoke(target.getClass().getMethod(name, types), target, arguments);
    }

    private static Object invoke(Method method, Object target, Object... arguments) throws Exception {
        try {
            return method.invoke(target, arguments);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception checked) {
                throw checked;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new AssertionError(cause);
        }
    }
}
