/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Validated, deterministic view of one ZIP/JAR class-path input. */
public record M3ArchiveIndex(boolean multiRelease, boolean signed,
                             Map<String, String> selectedClassEntries,
                             List<String> resourceEntries) {
    /** Normalizes immutable index state. */
    public M3ArchiveIndex {
        selectedClassEntries = Collections.unmodifiableMap(new TreeMap<>(selectedClassEntries));
        resourceEntries = List.copyOf(resourceEntries);
    }

    /** Reads an archive, rejecting duplicate or unsafe entry names. */
    public static M3ArchiveIndex read(Path archive) throws IOException {
        Path normalized = archive.toAbsolutePath().normalize();
        try (ZipFile zip = new ZipFile(normalized.toFile())) {
            boolean multiRelease = isMultiRelease(zip);
            boolean signed = false;
            Set<String> physicalNames = new HashSet<>();
            Map<String, VersionedEntry> selected = new TreeMap<>();
            List<String> resources = new ArrayList<>();
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                requireSafeEntryName(name);
                if (!physicalNames.add(name)) {
                    throw new IOException("Duplicate archive entry: " + name + " in " + normalized);
                }
                if (entry.isDirectory()) {
                    continue;
                }
                signed |= isSignatureEntry(name);
                if (!name.endsWith(".class")) {
                    resources.add(name);
                    continue;
                }
                VersionedName versioned = versionedName(name, multiRelease);
                if (versioned == null || isMetadataClass(versioned.baseName())) {
                    resources.add(name);
                    continue;
                }
                if (versioned.version() > Runtime.version().feature()) {
                    continue;
                }
                VersionedEntry current = selected.get(versioned.baseName());
                if (current == null || versioned.version() > current.version()) {
                    selected.put(versioned.baseName(), new VersionedEntry(versioned.version(), name));
                }
            }
            resources.sort(String::compareTo);
            Map<String, String> selectedNames = new TreeMap<>();
            selected.forEach((base, physical) -> selectedNames.put(base, physical.physicalName()));
            return new M3ArchiveIndex(multiRelease, signed, selectedNames, resources);
        }
    }

    /** Returns whether an entry is class metadata rather than an application class. */
    public static boolean isMetadataClass(String entry) {
        return entry.equals("module-info.class") || entry.endsWith("/module-info.class")
                || entry.equals("package-info.class") || entry.endsWith("/package-info.class");
    }

    /** Rejects ZIP-slip paths and platform-dependent separators. */
    public static void requireSafeEntryName(String name) throws IOException {
        if (name == null || name.isEmpty() || name.startsWith("/") || name.startsWith("\\")
                || name.contains("\\") || name.indexOf('\0') >= 0) {
            throw new IOException("Unsafe archive entry name: " + name);
        }
        for (String segment : name.split("/", -1)) {
            if (segment.equals("..")) {
                throw new IOException("Archive entry escapes root: " + name);
            }
        }
    }

    private static boolean isMultiRelease(ZipFile zip) throws IOException {
        ZipEntry entry = zip.getEntry("META-INF/MANIFEST.MF");
        if (entry == null) {
            return false;
        }
        try (var input = zip.getInputStream(entry)) {
            Manifest manifest = new Manifest(input);
            String value = manifest.getMainAttributes().getValue(Attributes.Name.MULTI_RELEASE);
            return value != null && Boolean.parseBoolean(value.trim());
        }
    }

    private static boolean isSignatureEntry(String name) {
        String upper = name.toUpperCase(Locale.ROOT);
        if (!upper.startsWith("META-INF/") || upper.indexOf('/', "META-INF/".length()) >= 0) {
            return false;
        }
        return upper.endsWith(".SF") || upper.endsWith(".RSA") || upper.endsWith(".DSA")
                || upper.endsWith(".EC") || upper.startsWith("META-INF/SIG-");
    }

    private static VersionedName versionedName(String entryName, boolean multiRelease) {
        String prefix = "META-INF/versions/";
        if (!entryName.startsWith(prefix)) {
            return new VersionedName(0, entryName);
        }
        if (!multiRelease) {
            return null;
        }
        int slash = entryName.indexOf('/', prefix.length());
        if (slash < 0) {
            return null;
        }
        try {
            int version = Integer.parseInt(entryName.substring(prefix.length(), slash));
            if (version < 9) {
                return null;
            }
            return new VersionedName(version, entryName.substring(slash + 1));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private record VersionedName(int version, String baseName) {
    }

    private record VersionedEntry(int version, String physicalName) {
    }
}
