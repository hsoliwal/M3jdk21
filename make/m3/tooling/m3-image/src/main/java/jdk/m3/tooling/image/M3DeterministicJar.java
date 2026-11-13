/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Creates stable JAR bytes from directories or filtered application archives. */
public final class M3DeterministicJar {
    private static final long ZIP_EPOCH_MILLIS = 315_532_800_000L; // 1980-01-01T00:00:00Z

    private M3DeterministicJar() {
    }

    /** Packs every regular file in lexicographic relative-path order; symlinks are refused. */
    public static void pack(Path directory, Path target) throws IOException {
        packDirectory(directory, target, null);
    }

    /** Packs all resources and only the admitted class entries from an exploded directory. */
    public static void packFilteredDirectory(Path directory, Path target, Set<String> classEntries)
            throws IOException {
        packDirectory(directory, target, Set.copyOf(classEntries));
    }

    /** Rewrites an archive deterministically, retaining resources and admitted runtime-selected classes. */
    public static void packFilteredArchive(Path archive, Path target, Set<String> classEntries)
            throws IOException {
        Path source = archive.toAbsolutePath().normalize();
        M3ArchiveIndex index = M3ArchiveIndex.read(source);
        if (index.signed()) {
            throw new IOException("Closed-world pruning refuses signed archive: " + source);
        }
        Files.createDirectories(target.toAbsolutePath().normalize().getParent());
        Map<String, String> outputs = new TreeMap<>();
        for (String resource : index.resourceEntries()) {
            if (!resource.equalsIgnoreCase("META-INF/INDEX.LIST")) {
                outputs.put(resource, resource);
            }
        }
        for (Map.Entry<String, String> selected : index.selectedClassEntries().entrySet()) {
            if (classEntries.contains(selected.getKey())) {
                if (outputs.put(selected.getKey(), selected.getValue()) != null) {
                    throw new IOException("Duplicate filtered archive output: " + selected.getKey());
                }
            }
        }
        try (ZipFile zip = new ZipFile(source.toFile());
             JarOutputStream output = output(target)) {
            for (Map.Entry<String, String> entry : outputs.entrySet()) {
                ZipEntry sourceEntry = zip.getEntry(entry.getValue());
                if (sourceEntry == null) {
                    throw new IOException("Archive entry disappeared: " + entry.getValue());
                }
                try (InputStream input = zip.getInputStream(sourceEntry)) {
                    writeEntry(output, entry.getKey(), input);
                }
            }
        }
    }

    private static void packDirectory(Path directory, Path target, Set<String> classEntries)
            throws IOException {
        Path source = directory.toAbsolutePath().normalize();
        Path normalizedTarget = target.toAbsolutePath().normalize();
        if (!Files.isDirectory(source) || Files.isSymbolicLink(source)) {
            throw new IOException("Expected non-symbolic directory: " + source);
        }
        if (normalizedTarget.startsWith(source)) {
            throw new IOException("JAR target must not be inside its source directory: " + normalizedTarget);
        }
        Files.createDirectories(normalizedTarget.getParent());
        Set<String> emitted = new HashSet<>();
        try (JarOutputStream output = output(normalizedTarget);
             var stream = Files.walk(source)) {
            for (Path file : stream.sorted().toList()) {
                if (Files.isSymbolicLink(file)) {
                    throw new IOException("Symlink is not admitted in deterministic JAR: " + file);
                }
                if (!Files.isRegularFile(file)) {
                    continue;
                }
                String entryName = source.relativize(file).toString()
                        .replace(file.getFileSystem().getSeparator(), "/");
                M3ArchiveIndex.requireSafeEntryName(entryName);
                if (classEntries != null && entryName.endsWith(".class")
                        && !M3ArchiveIndex.isMetadataClass(entryName)
                        && !classEntries.contains(entryName)) {
                    continue;
                }
                if (!emitted.add(entryName)) {
                    throw new IOException("Duplicate deterministic JAR entry: " + entryName);
                }
                try (InputStream input = Files.newInputStream(file)) {
                    writeEntry(output, entryName, input);
                }
            }
        }
    }

    private static JarOutputStream output(Path target) throws IOException {
        JarOutputStream output = new JarOutputStream(new BufferedOutputStream(Files.newOutputStream(target)));
        output.setLevel(9);
        return output;
    }

    private static void writeEntry(JarOutputStream output, String entryName, InputStream input)
            throws IOException {
        M3ArchiveIndex.requireSafeEntryName(entryName);
        JarEntry entry = new JarEntry(entryName);
        entry.setTime(ZIP_EPOCH_MILLIS);
        entry.setCreationTime(java.nio.file.attribute.FileTime.fromMillis(ZIP_EPOCH_MILLIS));
        entry.setLastAccessTime(java.nio.file.attribute.FileTime.fromMillis(ZIP_EPOCH_MILLIS));
        entry.setLastModifiedTime(java.nio.file.attribute.FileTime.fromMillis(ZIP_EPOCH_MILLIS));
        output.putNextEntry(entry);
        input.transferTo(output);
        output.closeEntry();
    }
}
