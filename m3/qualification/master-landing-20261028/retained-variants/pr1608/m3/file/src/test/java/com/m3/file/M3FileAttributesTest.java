// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class M3FileAttributesTest {
    @TempDir Path directory;

    @Test void nativeAttributesAndDirectionalExistence() throws Exception {
        var file = M3File.of(directory.resolve("source.txt"));
        var absent = file.observe();
        assertFalse(absent.exists());
        assertEquals(0, absent.changesFrom(file.observe()));
        Files.writeString(file.path(), "first");
        var first = file.observe();
        assertEquals(M3FileObservation.CREATED, first.changesFrom(absent));
        assertEquals(M3FileObservation.DELETED, absent.changesFrom(first));
        assertEquals(0, file.observe().changesFrom(first));
        assertEquals(Files.readAttributes(file.path(), java.nio.file.attribute.BasicFileAttributes.class).creationTime(), first.creationTime().orElseThrow());
        if (file.path().getFileSystem().supportedFileAttributeViews().contains("unix")) {
            assertEquals(Files.getAttribute(file.path(), "unix:ctime"), first.changeTime().orElseThrow());
        }
        Files.writeString(file.path(), "length changed");
        Files.setLastModifiedTime(file.path(), FileTime.fromMillis(10));
        int changes = file.observe().changesFrom(first);
        assertEquals(M3FileObservation.SIZE | M3FileObservation.MODIFIED_TIME,
                changes & (M3FileObservation.SIZE | M3FileObservation.MODIFIED_TIME));
        assertFalse(first.sameProviderVersion(first));
        assertThrows(IllegalArgumentException.class, () -> first.changesFrom(M3File.of(directory).observe()));
        assertThrows(NullPointerException.class, () -> first.changesFrom(null));
    }

    @Test void metadataCannotAuthorizeContentReuse() throws Exception {
        var file = M3File.of(Files.writeString(directory.resolve("source"), "12345"));
        var before = file.snapshot(5);
        var observed = file.observe();
        Files.writeString(file.path(), "abcde");
        Files.setLastModifiedTime(file.path(), observed.metadata().orElseThrow().lastModified());
        var after = file.observe();
        assertEquals(before.metadata(), after.metadata());
        assertFalse(after.sameProviderVersion(observed));
        assertEquals(M3FileSnapshot.Change.CONTENT_CHANGED, file.snapshot(5).changeFrom(before));
    }

    @Test void versionIdentityRejectsMissingAndEmptyParts() {
        assertThrows(NullPointerException.class, () -> new M3FileVersionView.Version(null, "1"));
        assertThrows(NullPointerException.class, () -> new M3FileVersionView.Version("store", null));
        assertThrows(IllegalArgumentException.class, () -> new M3FileVersionView.Version("", "1"));
        assertThrows(IllegalArgumentException.class, () -> new M3FileVersionView.Version("store", ""));
    }
}
