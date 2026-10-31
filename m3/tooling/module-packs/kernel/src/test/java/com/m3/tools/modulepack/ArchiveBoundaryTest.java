// SPDX-License-Identifier: Apache-2.0
package com.m3.tools.modulepack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Provider;
import java.security.Security;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Real archive boundaries complement the retained VM/header and graph differential tests. */
final class ArchiveBoundaryTest {
    @TempDir static Path root;
    private static byte[] descriptor;
    private static byte[] value;

    @BeforeAll
    static void compileOwnedFixture() throws IOException {
        Path source = Files.createDirectory(root.resolve("source"));
        Path classes = Files.createDirectory(root.resolve("classes"));
        Path pkg = Files.createDirectories(source.resolve("owned"));
        Path module = source.resolve("module-info.java");
        Path type = pkg.resolve("Value.java");
        Files.writeString(module, "module owned.fixture { exports owned; }");
        Files.writeString(type, "package owned; public final class Value { public Value() {} }");
        int status = ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "--release", "21", "-Xlint:all", "-Werror", "-d", classes.toString(),
                module.toString(), type.toString());
        assertEquals(0, status);
        descriptor = Files.readAllBytes(classes.resolve("module-info.class"));
        value = Files.readAllBytes(classes.resolve("owned/Value.class"));
    }

    @Test
    void directoriesAndNativeSuffixesAreClassifiedWithoutLoading() throws IOException {
        Map<String, byte[]> entries = entries();
        entries.put("docs/", new byte[0]);
        for (String name : List.of("lib/a.dll", "lib/b.dylib", "lib/c.so", "lib/d.so.2")) {
            entries.put(name, new byte[] {1, 2, 3});
        }
        entries.put("docs/readme", new byte[] {4});
        Path path = archive("native-shapes.jar", entries, false);
        String before = M3ModuleInspector.sha256(path);
        M3ModuleArtifact result = new M3ModuleInspector().inspect(path);
        assertEquals(List.of("lib/a.dll", "lib/b.dylib", "lib/c.so", "lib/d.so.2"),
                result.nativeEntries());
        assertTrue(result.resources().contains("docs/readme"));
        assertTrue(!result.resources().contains("docs/"));
        assertEquals(before, M3ModuleInspector.sha256(path));
    }

    @Test
    void everySignedArchiveSuffixIsRefused() throws IOException {
        for (String suffix : List.of("SF", "RSA", "DSA", "EC")) {
            Map<String, byte[]> entries = entries();
            entries.put("META-INF/owned." + suffix, new byte[] {1});
            Path path = archive("signature-" + suffix + ".jar", entries, false);
            IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                    () -> new M3ModuleInspector().inspect(path));
            assertTrue(failure.getMessage().startsWith("SIGNED_ARCHIVE_REQUIRES_REVIEW:"));
        }
    }

    @Test
    void aDuplicateDirectoryEntryIsRefusedBeforeDescriptorRead() throws IOException {
        Map<String, byte[]> entries = entries();
        entries.put("dup-a.txt", new byte[] {1});
        entries.put("dup-b.txt", new byte[] {2});
        Path path = archive("duplicate.jar", entries, false);
        byte[] bytes = Files.readAllBytes(path);
        byte[] from = "dup-b.txt".getBytes(StandardCharsets.US_ASCII);
        byte[] to = "dup-a.txt".getBytes(StandardCharsets.US_ASCII);
        int replacements = 0;
        for (int i = 0; i <= bytes.length - from.length; i++) {
            if (Arrays.equals(bytes, i, i + from.length, from, 0, from.length)) {
                System.arraycopy(to, 0, bytes, i, to.length);
                replacements++;
            }
        }
        assertEquals(2, replacements, "one local and one central directory name");
        Files.write(path, bytes);
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> new M3ModuleInspector().inspect(path));
        assertEquals("DUPLICATE_ENTRY: dup-a.txt", failure.getMessage());
    }

    @Test
    void descriptorAndClassMagicFailuresRemainTyped() throws IOException {
        Path noDescriptor = archive("no-descriptor.jmod",
                Map.of("classes/owned/Value.class", value), true);
        IllegalArgumentException missing = assertThrows(IllegalArgumentException.class,
                () -> new M3ModuleInspector().inspect(noDescriptor));
        assertTrue(missing.getMessage().startsWith("MISSING_DESCRIPTOR:"));
        Map<String, byte[]> entries = entries();
        byte[] invalid = value.clone();
        invalid[0] = 0;
        entries.put("owned/Value.class", invalid);
        Path badClass = archive("bad-class.jar", entries, false);
        IllegalArgumentException magic = assertThrows(IllegalArgumentException.class,
                () -> new M3ModuleInspector().inspect(badClass));
        assertEquals("INVALID_CLASS: owned/Value.class", magic.getMessage());
    }

    @Test
    void descriptorAtomRejectsAnEmptyModuleFinderResult() throws Exception {
        // Isolate the private descriptor atom's zero-module guard. This does not claim
        // that the public ZipFile admission path accepts directories as archives.
        Path empty = Files.createDirectory(root.resolve("empty-module-path"));
        Path control = archive("descriptor-control.jar", entries(), false);
        var method = M3ModuleInspector.class.getDeclaredMethod(
                "descriptor", Path.class, ZipFile.class, boolean.class);
        assertTrue(method.trySetAccessible());
        try (ZipFile zip = new ZipFile(control.toFile())) {
            InvocationTargetException wrapper = assertThrows(InvocationTargetException.class,
                    () -> method.invoke(null, empty, zip, false));
            IllegalArgumentException cause = assertInstanceOf(
                    IllegalArgumentException.class, wrapper.getCause());
            assertTrue(cause.getMessage().startsWith("AMBIGUOUS_MODULE:"));
        }
    }

    @Test
    void missingMandatoryDigestProviderFailsWithoutTouchingInput() throws IOException {
        Path input = root.resolve("digest-input");
        byte[] content = new byte[] {1, 2, 3};
        Files.write(input, content);
        // The module's Surefire execution is serial. Restore the exact provider order
        // in finally; no provider implementation or production hashing stub is used.
        Provider[] providers = Security.getProviders();
        try {
            for (Provider provider : providers) Security.removeProvider(provider.getName());
            IllegalStateException failure = assertThrows(IllegalStateException.class,
                    () -> M3ModuleInspector.sha256(input));
            assertEquals("Java platform lacks SHA-256", failure.getMessage());
        } finally {
            for (int index = 0; index < providers.length; index++) {
                Security.insertProviderAt(providers[index], index + 1);
            }
        }
        assertEquals(List.of(providers), List.of(Security.getProviders()));
        assertTrue(Arrays.equals(content, Files.readAllBytes(input)));
    }

    private static Map<String, byte[]> entries() {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("module-info.class", descriptor);
        entries.put("owned/Value.class", value);
        return entries;
    }

    private static Path archive(String name, Map<String, byte[]> entries, boolean jmod)
            throws IOException {
        Path file = root.resolve(name);
        try (var out = Files.newOutputStream(file)) {
            if (jmod) out.write(new byte[] {0x4a, 0x4d, 0x01, 0});
            try (ZipOutputStream zip = new ZipOutputStream(out)) {
                for (var item : entries.entrySet()) {
                    ZipEntry entry = new ZipEntry(item.getKey());
                    entry.setTime(0L);
                    zip.putNextEntry(entry);
                    zip.write(item.getValue());
                    zip.closeEntry();
                }
            }
        }
        return file;
    }
}
