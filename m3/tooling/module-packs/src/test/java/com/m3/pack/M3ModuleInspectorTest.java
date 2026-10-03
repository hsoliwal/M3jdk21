// SPDX-License-Identifier: Apache-2.0
package com.m3.pack;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class M3ModuleInspectorTest {
    @TempDir Path temp;

    @Test void inspectsJarJmodNativeAndEffectiveMultiReleaseView() throws Exception {
        Map<String, byte[]> classes = M3ArchiveFixtures.classes(temp, "example.base");
        Map<String, byte[]> contents = new LinkedHashMap<>(classes);
        contents.put("resources/message.txt", new byte[] {21});
        contents.put("lib/test.so", new byte[] {1});
        contents.put("lib/test.dll", new byte[] {2});
        contents.put("lib/test.dylib", new byte[] {3});
        contents.put("META-INF/versions/23/sample/Leaf.class",
                M3ArchiveFixtures.version(classes.get("sample/Leaf.class"), 67, 0));
        Path jar = M3ArchiveFixtures.jar(temp.resolve("base.jar"), contents, true);
        M3ModuleArtifact result = new M3ModuleInspector().inspect(jar);
        assertEquals("example.base", result.descriptor().name());
        assertEquals(65, result.maximumClassVersion());
        assertEquals(List.of("lib/test.dll", "lib/test.dylib", "lib/test.so"), result.nativeEntries());
        assertEquals(jar.toAbsolutePath(), result.path());
        assertEquals(M3ModuleInspector.sha256(jar), result.sha256());
        Path jmod = temp.resolve("base.jmod");
        Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "jmod").toString(),
                "create", "--class-path", temp.resolve("classes-example.base").toString(),
                "--module-version", "1.0", jmod.toString()).inheritIO().start();
        assertEquals(0, process.waitFor());
        assertEquals("1.0", new M3ModuleInspector().inspect(jmod).descriptor().rawVersion().orElseThrow());
    }

    @Test void rejectsAutomaticArchivesUnsupportedPathsAndChangedBytes() throws Exception {
        Map<String, byte[]> classes = M3ArchiveFixtures.classes(temp, "example.refusal");
        Path jar = M3ArchiveFixtures.jar(temp.resolve("refusal.jar"), classes, false);
        AtomicInteger calls = new AtomicInteger();
        assertThrows(IOException.class, () -> new M3ModuleInspector(p -> "hash-" + calls.incrementAndGet()).inspect(jar));
        assertThrows(IllegalArgumentException.class, () -> new M3ModuleInspector().inspect(temp.resolve("x.zip")));
        classes.remove("module-info.class");
        Path automatic = M3ArchiveFixtures.jar(temp.resolve("automatic.jar"), classes, false);
        assertThrows(IllegalArgumentException.class, () -> new M3ModuleInspector().inspect(automatic));
        assertThrows(IOException.class, () -> new M3ModuleInspector().inspect(temp.resolve("missing.jar")));
    }

    @Test void verifiesClassVersionBoundaryAndTruncation() throws Exception {
        byte[] header = {(byte) 0xca, (byte) 0xfe, (byte) 0xba, (byte) 0xbe, 0, 0, 0, 65};
        for (int major : new int[] {45, 61, 65}) {
            assertEquals(major, M3ModuleInspector.classVersion(new ByteArrayInputStream(
                    M3ArchiveFixtures.version(header, major, 0))));
        }
        for (int major : new int[] {44, 66}) {
            assertThrows(IllegalArgumentException.class, () -> M3ModuleInspector.classVersion(
                    new ByteArrayInputStream(M3ArchiveFixtures.version(header, major, 0))));
        }
        assertThrows(IllegalArgumentException.class, () -> M3ModuleInspector.classVersion(
                new ByteArrayInputStream(M3ArchiveFixtures.version(header, 65, 65535))));
        assertThrows(EOFException.class, () -> M3ModuleInspector.classVersion(new ByteArrayInputStream(new byte[0])));
        assertThrows(IllegalArgumentException.class, () -> M3ModuleInspector.classVersion(new ByteArrayInputStream(new byte[8])));
    }

    @Test void rejectsEffectiveFuturePreviewAndUnnamedClasses() throws Exception {
        Map<String, byte[]> classes = M3ArchiveFixtures.classes(temp, "example.future");
        byte[] leaf = classes.get("sample/Leaf.class");
        for (int minor : new int[] {0, 65535}) {
            classes.put("sample/Leaf.class", M3ArchiveFixtures.version(leaf, minor == 0 ? 66 : 65, minor));
            Path jar = M3ArchiveFixtures.jar(temp.resolve("future-" + minor + ".jar"), classes, false);
            assertThrows(IllegalArgumentException.class, () -> new M3ModuleInspector().inspect(jar));
        }
        classes.remove("sample/Leaf.class");
        classes.put("Leaf.class", leaf);
        Path unnamed = M3ArchiveFixtures.jar(temp.resolve("unnamed.jar"), classes, false);
        assertThrows(IllegalArgumentException.class, () -> new M3ModuleInspector().inspect(unnamed));
    }

    @Test void inventoryValueDoesNotRetainMutableNativeList() throws Exception {
        var descriptor = java.lang.module.ModuleDescriptor.newModule("example.value").build();
        List<String> source = new ArrayList<>(List.of("lib/value.so"));
        var result = new M3ModuleArtifact(temp, "hash", descriptor, 65, source);
        source.clear();
        assertEquals(List.of("lib/value.so"), result.nativeEntries());
        assertThrows(UnsupportedOperationException.class, () -> result.nativeEntries().clear());
    }
}
