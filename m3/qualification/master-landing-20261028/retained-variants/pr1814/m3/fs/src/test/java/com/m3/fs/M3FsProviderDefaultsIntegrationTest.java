/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class M3FsProviderDefaultsIntegrationTest {
    @TempDir Path temporary;

    @Test void registeredDatabaseProviderUsesCommonContentDefaults() throws Exception {
        Path database = temporary.resolve("builtin.sqlite");
        try (M3FS owner = M3FS.create(database)) {
            owner.createFile("/message.txt", 0644);
            owner.write("/message.txt", 0, new byte[] {1, 2, 3});
        }
        AtomicLong written = new AtomicLong();
        ByteArrayOutputStream shadow = new ByteArrayOutputStream();
        try (var entries = MFiles.from("m3db:" + database.toUri() + "!/message.txt").stream()) {
            MFile file = entries.findFirst().orElseThrow();
            file.progressify(M3FsProgress.of(() -> false, written::addAndGet))
                    .shadow(MFileShadow.of(shadow)).append(new byte[] {4, 5});
            assertArrayEquals(new byte[] {1, 2, 3, 4, 5}, file.readAllBytes(5));
        }
        assertEquals(2, written.get());
        assertArrayEquals(new byte[] {4, 5}, shadow.toByteArray());
        try (M3FS owner = M3FS.open(database)) {
            assertArrayEquals(new byte[] {1, 2, 3, 4, 5}, owner.read("/message.txt", 0, 6));
        }
    }

    @Test void installedZipFilesystemUsesCommonContentDefaultsAndRetainsOwnership() throws Exception {
        try (var zip = FileSystems.newFileSystem(temporary.resolve("provider.zip"), Map.of("create", "true"))) {
            Path path = zip.getPath("/content.txt");
            Files.writeString(path, "old");
            try (var entries = MFiles.from(path).stream()) {
                MFile file = entries.findFirst().orElseThrow();
                byte[] expected = "provider \u00e9".getBytes(StandardCharsets.UTF_8);
                assertEquals(expected.length, file.transferFrom(new ByteArrayInputStream(expected)));
                assertEquals("provider \u00e9", file.readString(expected.length, StandardCharsets.UTF_8));
            }
            assertTrue(zip.isOpen());
            assertEquals("provider \u00e9", Files.readString(path));
        }
    }
}
