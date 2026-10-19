// Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.filesystem;

import com.m3.fs.M3FS;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.eclipse.core.filesystem.EFS;
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

/** Frozen witnesses for the declared IFileStore copy and no-op contracts. */
final class M3EfsContractRefinementTest {
    @TempDir Path temporary;
    private M3EclipseFileSystem filesystem;
    private M3FS image;
    private IFileStore root;
    private Path database;

    @BeforeEach void createImage() throws Exception {
        assertTrue(M3FS.nativeAvailable(), "Real M3JDK21 JNI is required; no skips");
        database = temporary.resolve("contract.m3fs");
        image = M3FS.create(database);
        filesystem = new M3EclipseFileSystem();
        filesystem.initialize("m3fs");
        root = filesystem.mount("work", image);
    }

    @AfterEach void closeImages() { if (filesystem != null) filesystem.close(); }

    private static void write(IFileStore file, String text) throws Exception {
        try (OutputStream output = file.openOutputStream(EFS.NONE, null)) {
            output.write(text.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String read(IFileStore file) throws Exception {
        try (InputStream input = file.openInputStream(EFS.NONE, null)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static NullProgressMonitor canceled() {
        NullProgressMonitor monitor = new NullProgressMonitor();
        monitor.setCanceled(true);
        return monitor;
    }

    @Test void recursiveCopyContinuesAfterCollisionAndRetainsTheFailure() throws Exception {
        IFileStore source = root.getChild("src").mkdir(EFS.NONE, null);
        IFileStore destination = root.getChild("dst").mkdir(EFS.NONE, null);
        write(source.getChild("a"), "source-a");
        write(source.getChild("z"), "source-z");
        write(destination.getChild("a"), "destination-a");
        CoreException failure = assertThrows(CoreException.class,
                () -> source.copy(destination, EFS.NONE, null));
        assertTrue(destination.getChild("z").fetchInfo(EFS.NONE, null).exists(),
                "EFS-COPY-SIBLINGS: an independent sibling must copy after a collision");
        assertEquals("source-z", read(destination.getChild("z")));
        assertEquals("destination-a", read(destination.getChild("a")));
        assertEquals("source-a", read(source.getChild("a")));
        assertTrue(failure.getStatus().isMultiStatus());
        assertEquals(1, failure.getStatus().getChildren().length);
        assertEquals(EFS.ERROR_EXISTS, failure.getStatus().getChildren()[0].getCode());
        IFileStore canceledTarget = root.getChild("canceled-copy");
        assertThrows(OperationCanceledException.class,
                () -> source.copy(canceledTarget, EFS.NONE, canceled()));
        assertFalse(canceledTarget.fetchInfo(EFS.NONE, null).exists());
    }

    @Test void existingDirectoryMkdirIsNoOpInReadOnlyImage() throws Exception {
        image.mkdir("/directory", 0755);
        image.symlink("directory", "/directory-link");
        write(root.getChild("file"), "unchanged");
        M3FS.Stat before = image.stat("/directory");
        IFileStore readOnly = filesystem.open("readonly", database, true);
        for (IFileStore directory : new IFileStore[] {
                readOnly, readOnly.getChild("directory"), readOnly.getChild("directory-link")}) {
            assertSame(directory, directory.mkdir(EFS.NONE, null), "EFS-MKDIR-EXISTING");
            assertSame(directory, directory.mkdir(EFS.SHALLOW, null));
            assertThrows(OperationCanceledException.class,
                    () -> directory.mkdir(EFS.NONE, canceled()));
        }
        assertEquals(before, image.stat("/directory"));
        assertThrows(CoreException.class, () -> readOnly.getChild("new").mkdir(EFS.NONE, null));
        assertThrows(CoreException.class, () -> readOnly.getChild("file").mkdir(EFS.NONE, null));
        filesystem.unmount("readonly");
        filesystem.open("readonly", database, true);
        assertThrows(CoreException.class, () -> readOnly.mkdir(EFS.NONE, null),
                "No-op must retain the mount generation fence");
    }

    @Test void absentDeletionIsNoOpInReadOnlyImageWithoutFollowingFinalLinks() throws Exception {
        image.mkdir("/directory", 0755);
        image.symlink("missing", "/dangling");
        write(root.getChild("file"), "unchanged");
        IFileStore readOnly = filesystem.open("readonly", database, true);
        IFileStore absent = readOnly.getChild("missing");
        absent.delete(EFS.NONE, null);
        readOnly.getChild("directory").getChild("missing").delete(EFS.NONE, null);
        absent.getChild("child").delete(EFS.NONE, null);
        assertThrows(OperationCanceledException.class, () -> absent.delete(EFS.NONE, canceled()));
        assertThrows(CoreException.class, () -> readOnly.getChild("file").delete(EFS.NONE, null));
        assertThrows(CoreException.class, () -> readOnly.getChild("dangling").delete(EFS.NONE, null));
        assertThrows(CoreException.class, () -> readOnly.delete(EFS.NONE, null));
        assertEquals("unchanged", read(root.getChild("file")));
        assertEquals(M3FS.Kind.SYMLINK, image.stat("/dangling").kind());
        filesystem.unmount("readonly");
        assertThrows(CoreException.class, () -> absent.delete(EFS.NONE, null));
    }

    @Test void putInfoWithoutFlagsDoesNotInspectOrMutateThePath() throws Exception {
        FileInfo ignored = new FileInfo("ignored");
        ignored.setLastModified(-1);
        ignored.setAttribute(EFS.ATTRIBUTE_READ_ONLY, true);
        IFileStore missing = root.getChild("missing");
        missing.putInfo(ignored, EFS.NONE, null);
        write(root.getChild("file"), "unchanged");
        M3FS.Stat before = image.stat("/file");
        IFileStore readOnly = filesystem.open("readonly", database, true);
        readOnly.getChild("missing").putInfo(ignored, EFS.NONE, null);
        readOnly.getChild("file").putInfo(ignored, EFS.NONE, null);
        assertEquals(before, image.stat("/file"));
        assertFalse(missing.fetchInfo(EFS.NONE, null).exists());
        assertThrows(NullPointerException.class, () -> missing.putInfo(null, EFS.NONE, null));
        assertThrows(IllegalArgumentException.class, () -> missing.putInfo(ignored, EFS.APPEND, null));
        assertThrows(OperationCanceledException.class, () -> missing.putInfo(ignored, EFS.NONE, canceled()));
        assertThrows(CoreException.class,
                () -> readOnly.getChild("file").putInfo(ignored, EFS.SET_ATTRIBUTES, null));
        filesystem.unmount("readonly");
        assertThrows(CoreException.class, () -> readOnly.putInfo(ignored, EFS.NONE, null));
    }
}
