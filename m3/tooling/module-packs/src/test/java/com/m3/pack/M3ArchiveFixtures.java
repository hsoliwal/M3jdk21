// SPDX-License-Identifier: Apache-2.0
package com.m3.pack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import javax.tools.ToolProvider;

/** Real javac and archive fixtures; no substituted JDK or testing-library APIs. */
final class M3ArchiveFixtures {
    static Map<String, byte[]> classes(Path root, String name) throws IOException {
        Path source = Files.createDirectories(root.resolve("source-" + name));
        Path output = Files.createDirectories(root.resolve("classes-" + name));
        Files.createDirectories(source.resolve("sample"));
        Files.writeString(source.resolve("module-info.java"),
                "module " + name + " { exports sample; }");
        Files.writeString(source.resolve("sample/Leaf.java"),
                "package sample; public final class Leaf { public int result() { return 21; } }");
        int exit = ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "--release", "21", "-d", output.toString(),
                source.resolve("module-info.java").toString(), source.resolve("sample/Leaf.java").toString());
        if (exit != 0) throw new AssertionError("javac fixture failed: " + exit);
        Map<String, byte[]> result = new LinkedHashMap<>();
        result.put("module-info.class", Files.readAllBytes(output.resolve("module-info.class")));
        result.put("sample/Leaf.class", Files.readAllBytes(output.resolve("sample/Leaf.class")));
        return result;
    }

    static Path jar(Path file, Map<String, byte[]> entries, boolean multiRelease) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().putValue("Multi-Release", Boolean.toString(multiRelease));
        try (var output = new JarOutputStream(Files.newOutputStream(file), manifest)) {
            for (var entry : entries.entrySet()) {
                output.putNextEntry(new ZipEntry(entry.getKey()));
                output.write(entry.getValue());
                output.closeEntry();
            }
        }
        return file;
    }

    static byte[] version(byte[] source, int major, int minor) {
        byte[] bytes = source.clone();
        bytes[4] = (byte) (minor >>> 8);
        bytes[5] = (byte) minor;
        bytes[6] = (byte) (major >>> 8);
        bytes[7] = (byte) major;
        return bytes;
    }

    static String row(Path archive, String module) throws IOException {
        return archive.getFileName() + "\t" + M3ModuleInspector.sha256(archive) + "\t" + module + "\n";
    }

    static Path lock(Path root, String body) throws IOException {
        return Files.writeString(root.resolve("LOCK.tsv"), "path\tsha256\tmodule\n" + body);
    }
}
