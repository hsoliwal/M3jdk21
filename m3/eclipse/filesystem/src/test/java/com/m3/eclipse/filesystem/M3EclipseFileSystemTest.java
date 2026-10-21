// Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.filesystem;

import com.m3.fs.M3FS;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Native-backed EFS contracts; native absence is a failure, never a skipped test. */
final class M3EclipseFileSystemTest {
    @TempDir Path temporary;
    private M3EclipseFileSystem filesystem;
    private M3FS image;
    private IFileStore root;
    private Path database;

    @BeforeEach void createImage() throws Exception {
        assertTrue(M3FS.nativeAvailable(), "M3JDK21 native backend is required");
        database = temporary.resolve("image.m3fs");
        image = M3FS.create(database);
        filesystem = new M3EclipseFileSystem();
        filesystem.initialize("m3fs");
        root = filesystem.mount("work", image);
    }

    @AfterEach void closeImages() {
        if (filesystem != null) filesystem.close();
    }

    private static byte[] bytes(String text) { return text.getBytes(StandardCharsets.UTF_8); }

    private static void write(IFileStore file, String value, int options) throws Exception {
        try (OutputStream output = file.openOutputStream(options, null)) { output.write(bytes(value)); }
    }

    private static String read(IFileStore file) throws Exception {
        try (InputStream input = file.openInputStream(EFS.NONE, null)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static IFileInfo info(IFileStore file) throws CoreException {
        return file.fetchInfo(EFS.NONE, null);
    }

    @Test void uriHandlesRepresentLocationsWithoutMaterializingMetadata() throws Exception {
        IFileStore file = root.getChild("space #\u00e9");
        assertEquals(URI.create("m3fs://work/space%20%23\u00e9"), file.toURI());
        assertEquals(file, filesystem.getStore(file.toURI()));
        assertEquals(file, filesystem.getStore(URI.create(file.toURI().toASCIIString())));
        assertEquals(root.getChild("\ufffd"), filesystem.getStore(URI.create("m3fs://work/%EF%BF%BD")));
        assertEquals(file.hashCode(), filesystem.getStore(file.toURI()).hashCode());
        assertEquals(root, file.getParent());
        assertNull(root.getParent());
        assertEquals("", root.getName());
        assertSame(filesystem, file.getFileSystem());
        assertFalse(info(file).exists());
        assertTrue(info(root).isDirectory());
        assertNull(file.toLocalFile(EFS.NONE, null));
        assertTrue(filesystem.canWrite());
        assertTrue(filesystem.canDelete());
        assertTrue(filesystem.isCaseSensitive());
        assertEquals(0, root.childNames(EFS.NONE, null).length);
    }

    @Test void streamsHonorSubrangesOffsetsAppendEofAndImmediateTruncate() throws Exception {
        IFileStore file = root.getChild("data");
        OutputStream output = file.openOutputStream(EFS.NONE, null);
        output.write(bytes("!abc?"), 1, 3);
        output.write('d');
        output.write(bytes("EF"));
        output.flush();
        assertEquals(6, info(file).getLength());
        assertEquals("abcdEF", read(file));
        output.close();
        assertThrows(IOException.class, () -> output.write('x'));
        write(file, "GH", EFS.APPEND);
        assertEquals("abcdEFGH", read(file));
        try (InputStream input = file.openInputStream(EFS.NONE, null)) {
            byte[] target = new byte[6];
            assertEquals(3, input.read(target, 1, 3));
            assertArrayEquals(bytes("abc"), Arrays.copyOfRange(target, 1, 4));
            assertEquals(3, input.skip(3));
            assertEquals('G', input.read());
            assertEquals('H', input.read());
            assertEquals(-1, input.read());
            assertEquals(0, input.read(target, 0, 0));
        }
        try (OutputStream empty = file.openOutputStream(EFS.NONE, null)) {
            assertEquals(0, info(file).getLength(), "ordinary open truncates before first payload");
            empty.write(new byte[0]);
        }
        assertEquals("", read(file));
    }

    @Test void metadataFlagsPersistModeAndTimeAndEnforceReadOnly() throws Exception {
        IFileStore file = root.getChild("mode");
        write(file, "text", EFS.NONE);
        FileInfo change = new FileInfo("mode");
        change.setLastModified(1_700_000_123_456L);
        change.setAttribute(EFS.ATTRIBUTE_READ_ONLY, true);
        change.setAttribute(EFS.ATTRIBUTE_EXECUTABLE, true);
        file.putInfo(change, EFS.SET_ATTRIBUTES | EFS.SET_LAST_MODIFIED, null);
        assertEquals(change.getLastModified(), info(file).getLastModified());
        assertTrue(info(file).getAttribute(EFS.ATTRIBUTE_READ_ONLY));
        assertTrue(info(file).getAttribute(EFS.ATTRIBUTE_EXECUTABLE));
        assertEquals(0, image.stat("/mode").mode() & 0222);
        assertEquals(change.getLastModified() * 1_000_000L, image.stat("/mode").modifiedTimeNanos());
        assertEquals(EFS.ERROR_READ_ONLY, assertThrows(CoreException.class,
                () -> file.openOutputStream(EFS.NONE, null)).getStatus().getCode());
        change.setAttribute(EFS.ATTRIBUTE_READ_ONLY, false);
        change.setAttribute(EFS.ATTRIBUTE_EXECUTABLE, false);
        file.putInfo(change, EFS.SET_ATTRIBUTES, null);
        assertFalse(info(file).getAttribute(EFS.ATTRIBUTE_READ_ONLY));
        assertFalse(info(file).getAttribute(EFS.ATTRIBUTE_EXECUTABLE));
        assertEquals(change.getLastModified(), info(file).getLastModified());
        change.setLastModified(-1);
        assertThrows(CoreException.class, () -> file.putInfo(change, EFS.SET_LAST_MODIFIED, null));
        assertEquals("text", read(file));
    }

    @Test void directoriesSupportShallowAndRecursiveCreationAndRejectWrongTypes() throws Exception {
        IFileStore nested = root.getChild("a").getChild("b").getChild("c");
        assertThrows(CoreException.class, () -> nested.mkdir(EFS.SHALLOW, null));
        assertSame(nested, nested.mkdir(EFS.NONE, null));
        assertTrue(info(nested).isDirectory());
        assertSame(nested, nested.mkdir(EFS.SHALLOW, null));
        IFileStore file = root.getChild("file");
        write(file, "x", EFS.NONE);
        assertThrows(CoreException.class, () -> file.mkdir(EFS.NONE, null));
        assertThrows(CoreException.class, () -> file.childNames(EFS.NONE, null));
        assertFalse(info(file.getChild("missing")).exists());
        assertThrows(CoreException.class, () -> nested.openInputStream(EFS.NONE, null));
        assertThrows(CoreException.class, () -> nested.openOutputStream(EFS.NONE, null));
        assertThrows(CoreException.class, () -> root.delete(EFS.NONE, null));
    }

    @Test void pagedUnicodeEnumerationAndIterativeDeletionCoverMultipleNativePages() throws Exception {
        IFileStore directory = root.getChild("entries").mkdir(EFS.NONE, null);
        for (int i = 0; i < 530; i++) image.createFile("/entries/f" + String.format("%04d", i), 0644);
        image.createFile("/entries/\ue000", 0644);
        image.createFile("/entries/\ud83d\ude00", 0644);
        String[] names = directory.childNames(EFS.NONE, null);
        assertEquals(532, names.length);
        assertEquals("f0000", names[0]);
        assertEquals("f0529", names[529]);
        assertEquals("\ue000", names[530], "cursor order compares UTF-8, not UTF-16 surrogate values");
        assertEquals("\ud83d\ude00", names[531]);
        IFileInfo[] infos = directory.childInfos(EFS.NONE, null);
        assertEquals(names.length, infos.length);
        for (int i = 0; i < names.length; i++) {
            assertEquals(names[i], infos[i].getName());
            assertTrue(infos[i].exists());
            assertFalse(infos[i].isDirectory());
        }
        IFileStore deep = directory;
        for (int i = 0; i < 160; i++) deep = deep.getChild("d").mkdir(EFS.SHALLOW, null);
        write(deep.getChild("leaf"), "value", EFS.NONE);
        directory.delete(EFS.NONE, null);
        assertFalse(info(directory).exists());
        assertEquals(0, root.childNames(EFS.NONE, null).length);
    }

    @Test void copyAndAtomicMovePreserveDestinationsUnlessOverwriteIsRequested() throws Exception {
        IFileStore source = root.getChild("source");
        IFileStore destination = root.getChild("destination");
        write(source, "new-data", EFS.NONE);
        write(destination, "old", EFS.NONE);
        assertEquals(EFS.ERROR_EXISTS, assertThrows(CoreException.class,
                () -> source.copy(destination, EFS.NONE, null)).getStatus().getCode());
        assertEquals("old", read(destination));
        assertEquals(EFS.ERROR_EXISTS, assertThrows(CoreException.class,
                () -> source.move(destination, EFS.NONE, null)).getStatus().getCode());
        assertEquals("new-data", read(source));
        assertEquals("old", read(destination));
        source.copy(destination, EFS.OVERWRITE, null);
        assertEquals("new-data", read(destination));
        IFileStore moved = root.getChild("moved");
        source.move(moved, EFS.NONE, null);
        assertFalse(info(source).exists());
        assertEquals("new-data", read(moved));
        write(destination, "replace-me", EFS.NONE);
        moved.move(destination, EFS.OVERWRITE, null);
        assertFalse(info(moved).exists());
        assertEquals("new-data", read(destination));
        assertThrows(CoreException.class, () -> destination.move(destination, EFS.OVERWRITE, null));
        assertEquals("new-data", read(destination));
        IFileStore directory = root.getChild("copy-source").mkdir(EFS.NONE, null);
        IFileStore merge = root.getChild("copy-destination").mkdir(EFS.NONE, null);
        write(directory.getChild("incoming"), "incoming", EFS.NONE);
        write(merge.getChild("kept"), "kept", EFS.NONE);
        directory.copy(merge, EFS.NONE, null);
        assertEquals("incoming", read(merge.getChild("incoming")));
        assertEquals("kept", read(merge.getChild("kept")));
        IFileStore shallow = root.getChild("shallow-copy");
        directory.copy(shallow, EFS.SHALLOW, null);
        assertTrue(info(shallow).isDirectory());
        assertEquals(0, shallow.childNames(EFS.NONE, null).length);
    }

    @Test void symbolicLinksFollowTargetsWhileDanglingEntriesRemainEnumerableAndAbsent() throws Exception {
        IFileStore target = root.getChild("target");
        write(target, "payload", EFS.NONE);
        image.symlink("target", "/link");
        IFileStore link = root.getChild("link");
        assertEquals("payload", read(link));
        assertTrue(info(link).getAttribute(EFS.ATTRIBUTE_SYMLINK));
        assertEquals("target", info(link).getStringAttribute(EFS.ATTRIBUTE_LINK_TARGET));
        FileInfo change = new FileInfo("link");
        change.setLastModified(1_234_567L);
        link.putInfo(change, EFS.SET_LAST_MODIFIED, null);
        assertEquals(1_234_567L, info(target).getLastModified());
        image.mkdir("/directory", 0755);
        image.symlink("directory", "/dirlink");
        write(root.getChild("dirlink").getChild("through"), "inside", EFS.NONE);
        assertEquals("inside", read(root.getChild("directory").getChild("through")));
        image.symlink("missing", "/dangling");
        IFileStore dangling = root.getChild("dangling");
        IFileInfo absent = info(dangling);
        assertFalse(absent.exists(), "EFS projects existence of the dereferenced target");
        assertTrue(absent.getAttribute(EFS.ATTRIBUTE_SYMLINK));
        assertEquals("missing", absent.getStringAttribute(EFS.ATTRIBUTE_LINK_TARGET));
        assertTrue(Arrays.asList(root.childNames(EFS.NONE, null)).contains("dangling"));
        IFileInfo enumerated = Arrays.stream(root.childInfos(EFS.NONE, null))
                .filter(row -> row.getName().equals("dangling")).findFirst().orElseThrow();
        assertFalse(enumerated.exists());
        assertTrue(enumerated.getAttribute(EFS.ATTRIBUTE_SYMLINK));
        assertThrows(CoreException.class, () -> dangling.openOutputStream(EFS.NONE, null));
        assertThrows(java.nio.file.NoSuchFileException.class, () -> image.stat("/missing"));
        image.symlink("target/child", "/non-directory-target");
        assertFalse(info(root.getChild("non-directory-target")).exists());
        assertTrue(info(root.getChild("non-directory-target")).getAttribute(EFS.ATTRIBUTE_SYMLINK));
        IFileStore renamed = root.getChild("renamed-link");
        dangling.move(renamed, EFS.NONE, null);
        assertEquals(M3FS.Kind.SYMLINK, image.stat("/renamed-link").kind());
        renamed.delete(EFS.NONE, null);
        link.delete(EFS.NONE, null);
        assertEquals("payload", read(target));
        root.getChild("dirlink").delete(EFS.NONE, null);
        assertEquals("inside", read(root.getChild("directory").getChild("through")));
    }

    @Test void aliasProtectionRefusesHardLinksSymlinkAliasesAndDirectoryCycles() throws Exception {
        IFileStore file = root.getChild("file");
        write(file, "preserve", EFS.NONE);
        image.link("/file", "/hard");
        image.symlink("file", "/symbolic");
        assertThrows(CoreException.class, () -> file.copy(root.getChild("hard"), EFS.OVERWRITE, null));
        assertThrows(CoreException.class, () -> file.copy(root.getChild("symbolic"), EFS.OVERWRITE, null));
        assertEquals("preserve", read(file));
        IFileStore directory = root.getChild("directory").mkdir(EFS.NONE, null);
        image.symlink(".", "/directory/loop");
        assertThrows(CoreException.class, () -> directory.copy(root.getChild("copied"), EFS.NONE, null));
        assertThrows(CoreException.class, () -> directory.copy(directory.getChild("inside"), EFS.NONE, null));
        image.symlink("cycle-b", "/cycle-a");
        image.symlink("cycle-a", "/cycle-b");
        assertThrows(CoreException.class, () -> info(root.getChild("cycle-a")));
        directory.delete(EFS.NONE, null);
        assertFalse(info(directory).exists());
        assertEquals("preserve", read(root.getChild("hard")));
    }

    @Test void physicalImageAliasesAreProtectedButRetainedSnapshotsCanRestoreContent() throws Exception {
        IFileStore file = root.getChild("file");
        write(file, "old", EFS.NONE);
        IFileStore alias = filesystem.open("alias", database, false);
        assertThrows(CoreException.class, () -> file.copy(alias.getChild("file"), EFS.OVERWRITE, null));
        assertThrows(CoreException.class, () -> root.copy(alias.getChild("inside"), EFS.NONE, null));
        image.link("/file", "/hard");
        assertThrows(CoreException.class, () -> file.copy(alias.getChild("hard"), EFS.OVERWRITE, null));
        IFileStore snapshot = filesystem.mount("snapshot", M3FS.openSnapshot(database));
        write(file, "new-value", EFS.NONE);
        assertEquals("old", read(snapshot.getChild("file")));
        snapshot.getChild("file").copy(file, EFS.OVERWRITE, null);
        assertEquals("old", read(file));
        assertEquals("old", read(alias.getChild("file")));
    }

    @Test void independentOwnersAppendTransactionallyAndRejectReplacedStreamPaths() throws Exception {
        IFileStore file = root.getChild("shared");
        write(file, "prefix:", EFS.NONE);
        try (M3EclipseFileSystem second = new M3EclipseFileSystem()) {
            second.initialize("m3fs");
            IFileStore other = second.open("other", database, false).getChild("shared");
            try (OutputStream left = file.openOutputStream(EFS.APPEND, null);
                 OutputStream right = other.openOutputStream(EFS.APPEND, null);
                 var executor = Executors.newFixedThreadPool(2)) {
                Future<?> a = executor.submit(() -> append(left, 'A', 200));
                Future<?> b = executor.submit(() -> append(right, 'B', 200));
                a.get();
                b.get();
            }
            String content = read(file);
            assertEquals(407, content.length());
            assertTrue(content.startsWith("prefix:"));
            assertEquals(200, content.chars().filter(c -> c == 'A').count());
            assertEquals(200, content.chars().filter(c -> c == 'B').count());
            try (InputStream oldInput = file.openInputStream(EFS.NONE, null);
                 OutputStream oldOutput = file.openOutputStream(EFS.APPEND, null);
                 M3FS external = M3FS.open(database)) {
                external.rename("/shared", "/original");
                external.createFile("/shared", 0644);
                external.write("/shared", 0, bytes("replacement"));
                assertThrows(IOException.class, oldInput::read);
                assertThrows(IOException.class, () -> oldOutput.write('!'));
                assertEquals("replacement", read(other));
                assertEquals(content, read(root.getChild("original")));
            }
        }
    }

    private static void append(OutputStream output, int value, int count) {
        try { for (int i = 0; i < count; i++) output.write(value); }
        catch (IOException problem) { throw new java.io.UncheckedIOException(problem); }
    }

    @Test void removedInodesAndReusedMountNamesCannotRetargetOldHandles() throws Exception {
        IFileStore file = root.getChild("identity");
        write(file, "first", EFS.NONE);
        long original = image.stat("/identity").inode();
        InputStream oldInput = file.openInputStream(EFS.NONE, null);
        OutputStream oldOutput = file.openOutputStream(EFS.APPEND, null);
        image.unlink("/identity");
        image.createFile("/identity", 0644);
        image.write("/identity", 0, bytes("second"));
        assertNotEquals(original, image.stat("/identity").inode());
        assertThrows(IOException.class, oldInput::read);
        assertThrows(IOException.class, () -> oldOutput.write('x'));
        oldInput.close();
        oldOutput.close();
        filesystem.unmount("work");
        assertFalse(image.isOpen());
        IFileStore reopened = filesystem.open("work", database, false).getChild("identity");
        assertEquals(file, reopened, "logical URI equality does not bypass the mount generation fence");
        assertThrows(CoreException.class, () -> info(file));
        assertEquals("second", read(reopened));
    }

    @Test void readOnlyImagesAdvertiseAndEnforceTheImagePolicy() throws Exception {
        write(root.getChild("file"), "value", EFS.NONE);
        IFileStore readOnly = filesystem.open("readonly", database, true);
        IFileStore file = readOnly.getChild("file");
        assertEquals("value", read(file));
        assertTrue(info(file).getAttribute(EFS.ATTRIBUTE_READ_ONLY));
        assertThrows(CoreException.class, () -> file.openOutputStream(EFS.APPEND, null));
        assertThrows(CoreException.class, () -> file.delete(EFS.NONE, null));
        assertThrows(CoreException.class, () -> file.move(readOnly.getChild("renamed"), EFS.NONE, null));
        assertThrows(CoreException.class, () -> readOnly.getChild("dir").mkdir(EFS.NONE, null));
        assertThrows(CoreException.class, () -> file.putInfo(new FileInfo("file"), EFS.SET_ATTRIBUTES, null));
        assertEquals("value", read(root.getChild("file")));
    }

    @Test void cancellationMissingPathsAndUnsupportedFlagsKeepTheirEfsContracts() throws Exception {
        NullProgressMonitor canceled = new NullProgressMonitor();
        canceled.setCanceled(true);
        assertThrows(OperationCanceledException.class, () -> root.childNames(EFS.NONE, canceled));
        assertThrows(OperationCanceledException.class, () -> root.getChild("x").mkdir(EFS.NONE, canceled));
        IFileStore absent = root.getChild("missing");
        assertFalse(info(absent).exists());
        assertEquals(EFS.ERROR_NOT_EXISTS, assertThrows(CoreException.class,
                () -> absent.openInputStream(EFS.NONE, null)).getStatus().getCode());
        absent.delete(EFS.NONE, null);
        assertThrows(IllegalArgumentException.class, () -> root.childNames(EFS.APPEND, null));
        assertThrows(IllegalArgumentException.class, () -> absent.openOutputStream(EFS.OVERWRITE, null));
        assertThrows(IllegalArgumentException.class, () -> absent.putInfo(new FileInfo("missing"), EFS.APPEND, null));
    }

    @Test void namespaceValidationRejectsEscapesInvalidUtf16AndOverlongPaths() throws Exception {
        for (String name : new String[] {"", ".", "..", "a/b", "\0", "\ud800", "\udc00", "\u00e9".repeat(128)}) {
            assertThrows(IllegalArgumentException.class, () -> root.getChild(name), name);
        }
        IFileStore literal = root.getChild("a\\b");
        write(literal, "literal host-independent name", EFS.NONE);
        assertEquals("literal host-independent name", read(literal));
        assertThrows(IllegalArgumentException.class, () -> filesystem.create("../host", temporary.resolve("bad")));
        IFileStore longPath = root;
        for (int i = 0; i < 15; i++) longPath = longPath.getChild("x".repeat(255));
        IFileStore maximum = longPath.getChild("x".repeat(255));
        assertEquals(4096, maximum.toURI().getPath().length());
        assertThrows(IllegalArgumentException.class, () -> maximum.getChild("x"));
    }
}

