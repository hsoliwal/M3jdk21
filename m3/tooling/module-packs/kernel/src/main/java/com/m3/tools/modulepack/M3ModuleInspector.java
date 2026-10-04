// SPDX-License-Identifier: Apache-2.0
package com.m3.tools.modulepack;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Read-only JAR/JMOD recognizer. Pattern/IOP role: ArchiveAdapter.
 * Checks the Java-21 effective class view, explicit descriptors and signatures.
 * It does not load classes, unpack files or certify native ABI compatibility.
 */
public final class M3ModuleInspector {
    /** Stateless inspector; safe to use for independent archives. */
    public M3ModuleInspector() {}

    /**
     * Inspect an immutable build input. Multi-release JARs use the Java 21 view.
     * Signed JARs, automatic modules, duplicate entries, preview/newer effective
     * classes and byte drift are refused. JMODs retain their leading JM header.
     *
     * @param input existing JAR/JMOD path
     * @return immutable observed metadata
     * @throws IOException if archive I/O fails
     * @throws IllegalArgumentException if an admission precondition fails
     */
    public M3ModuleArtifact inspect(Path input) throws IOException {
        Path path = input.toRealPath();
        String name = path.getFileName().toString();
        boolean jmod = name.endsWith(".jmod");
        if (!jmod && !name.endsWith(".jar")) {
            throw new IllegalArgumentException("UNSUPPORTED_ARCHIVE: " + path);
        }
        String hash = sha256(path);
        try (ZipFile zip = open(path, jmod)) {
            validateEntries(zip);
            ModuleDescriptor descriptor = descriptor(path, zip, jmod);
            List<? extends ZipEntry> entries = effectiveEntries(zip, jmod);
            ArrayList<String> natives = new ArrayList<>();
            ArrayList<String> resources = new ArrayList<>();
            for (ZipEntry entry : entries) {
                if (entry.isDirectory()) continue;
                String entryName = entry.getName();
                if (entryName.endsWith(".class")) {
                    try (InputStream in = zip.getInputStream(entry)) {
                        validateClass(in, entryName);
                    }
                } else {
                    resources.add(entryName);
                    String lower = entryName.toLowerCase(Locale.ROOT);
                    if (lower.endsWith(".dll") || lower.endsWith(".dylib")
                            || lower.endsWith(".so") || lower.contains(".so.")) {
                        natives.add(entryName);
                    }
                }
            }
            if (!hash.equals(sha256(path))) {
                throw new IllegalArgumentException("INPUT_DRIFT: " + path);
            }
            return new M3ModuleArtifact(path, descriptor, hash, jmod ? "JMOD" : "JAR",
                    natives.stream().sorted().toList(), resources.stream().sorted().toList());
        }
    }

    private static ZipFile open(Path path, boolean jmod) throws IOException {
        if (!jmod) {
            return new JarFile(path.toFile(), true, ZipFile.OPEN_READ, Runtime.Version.parse("21"));
        }
        try (DataInputStream in = new DataInputStream(Files.newInputStream(path))) {
            if (in.readInt() != 0x4a4d0100) {
                throw new IllegalArgumentException("INVALID_JMOD_HEADER: " + path);
            }
        }
        return new ZipFile(path.toFile());
    }

    private static void validateEntries(ZipFile zip) {
        HashSet<String> names = new HashSet<>();
        zip.stream().forEach(entry -> {
            String name = entry.getName();
            if (!names.add(name)) {
                throw new IllegalArgumentException("DUPLICATE_ENTRY: " + name);
            }
            String upper = name.toUpperCase(Locale.ROOT);
            if (upper.startsWith("META-INF/") && (upper.endsWith(".SF")
                    || upper.endsWith(".RSA") || upper.endsWith(".DSA") || upper.endsWith(".EC"))) {
                throw new IllegalArgumentException("SIGNED_ARCHIVE_REQUIRES_REVIEW: " + name);
            }
        });
    }

    private static ModuleDescriptor descriptor(Path path, ZipFile zip, boolean jmod)
            throws IOException {
        if (jmod) {
            ZipEntry entry = zip.getEntry("classes/module-info.class");
            if (entry == null) throw new IllegalArgumentException("MISSING_DESCRIPTOR: " + path);
            try (InputStream in = zip.getInputStream(entry)) {
                return ModuleDescriptor.read(in);
            }
        }
        var modules = ModuleFinder.of(path).findAll();
        if (modules.size() != 1) throw new IllegalArgumentException("AMBIGUOUS_MODULE: " + path);
        ModuleDescriptor descriptor = modules.iterator().next().descriptor();
        if (descriptor.isAutomatic()) {
            throw new IllegalArgumentException("AUTOMATIC_MODULE: " + descriptor.name());
        }
        return descriptor;
    }

    private static List<? extends ZipEntry> effectiveEntries(ZipFile zip, boolean jmod) {
        if (jmod) return zip.stream().toList();
        return ((JarFile) zip).versionedStream().toList();
    }

    /**
     * ClassVersionAdmission atom of the ArchiveAdapter (JVMS 21 section 4.1).
     * Major 45..55 admits any u2 minor; later supported majors require minor zero
     * for this non-preview pack policy. This header check is not bytecode verification.
     */
    private static void validateClass(InputStream source, String name) throws IOException {
        DataInputStream in = new DataInputStream(source);
        if (in.readInt() != 0xcafebabe) {
            throw new IllegalArgumentException("INVALID_CLASS: " + name);
        }
        int minor = in.readUnsignedShort();
        int major = in.readUnsignedShort();
        if (major < 45 || major > 65 || (major >= 56 && minor != 0)) {
            throw new IllegalArgumentException("NOT_JAVA21_NONPREVIEW: " + name);
        }
    }

    /** Compute a streaming SHA-256 digest; no whole-archive allocation. */
    public static String sha256(Path path) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("Java platform lacks SHA-256", impossible);
        }
        try (InputStream in = Files.newInputStream(path)) {
            byte[] buffer = new byte[65536];
            int count;
            while ((count = in.read(buffer)) != -1) digest.update(buffer, 0, count);
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
