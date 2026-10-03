// SPDX-License-Identifier: Apache-2.0
package com.m3.pack;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Archive Adapter: inspect without loading classes or executing archive contents. */
public final class M3ModuleInspector {
    private static final Runtime.Version TARGET = Runtime.Version.parse("21");
    private final Hasher hasher;

    /** Digest Strategy for stable-read verification and deterministic fault injection. */
    @FunctionalInterface
    interface Hasher {
        String hash(Path path) throws IOException;
    }

    /** Uses the platform SHA-256 implementation. */
    public M3ModuleInspector() {
        this(M3ModuleInspector::sha256);
    }

    M3ModuleInspector(Hasher hasher) {
        this.hasher = java.util.Objects.requireNonNull(hasher, "hasher");
    }

    /** Reads the effective Java 21 archive view; refuses automatic modules and bytecode drift. */
    public M3ModuleArtifact inspect(Path path) throws IOException {
        String name = path.getFileName().toString();
        boolean jmod = name.endsWith(".jmod");
        if (!jmod && !name.endsWith(".jar")) {
            throw new IllegalArgumentException("UNSUPPORTED_ARCHIVE: " + path);
        }
        String before = hasher.hash(path);
        try (ZipFile archive = jmod ? new ZipFile(path.toFile())
                : new JarFile(path.toFile(), true, ZipFile.OPEN_READ, TARGET)) {
            String prefix = jmod ? "classes/" : "";
            ZipEntry descriptorEntry = archive.getEntry(prefix + "module-info.class");
            if (descriptorEntry == null) {
                throw new IllegalArgumentException("EXPLICIT_DESCRIPTOR_REQUIRED: " + path);
            }
            List<? extends ZipEntry> entries = archive instanceof JarFile jar
                    ? jar.versionedStream().toList() : archive.stream().toList();
            int maximum = 0;
            Set<String> packages = new HashSet<>();
            List<String> natives = new ArrayList<>();
            for (ZipEntry entry : entries) {
                String entryName = entry.getName();
                if (entryName.startsWith(prefix) && entryName.endsWith(".class")) {
                    String className = entryName.substring(prefix.length());
                    if (!className.equals("module-info.class")) {
                        int slash = className.lastIndexOf('/');
                        if (slash < 0) {
                            throw new IllegalArgumentException("UNNAMED_PACKAGE_CLASS: " + className);
                        }
                        packages.add(className.substring(0, slash).replace('/', '.'));
                    }
                    try (InputStream input = archive.getInputStream(entry)) {
                        maximum = Math.max(maximum, classVersion(input));
                    }
                }
                if (entryName.endsWith(".so") || entryName.endsWith(".dll")
                        || entryName.endsWith(".dylib")) {
                    natives.add(entryName);
                }
            }
            ModuleDescriptor descriptor;
            try (InputStream input = archive.getInputStream(descriptorEntry)) {
                descriptor = ModuleDescriptor.read(input, () -> packages);
            }
            if (!descriptor.packages().containsAll(packages)) {
                throw new IllegalArgumentException("DESCRIPTOR_PACKAGE_MISMATCH: " + path);
            }
            if (!before.equals(hasher.hash(path))) {
                throw new IOException("ARCHIVE_CHANGED_DURING_INSPECTION: " + path);
            }
            return new M3ModuleArtifact(path.toAbsolutePath().normalize(), before,
                    descriptor, maximum, natives.stream().sorted().toList());
        }
    }

    /** Class-header boundary atom; this is not a verifier for method bodies or API linkage. */
    static int classVersion(InputStream source) throws IOException {
        DataInputStream input = new DataInputStream(source);
        if (input.readInt() != 0xcafebabe) {
            throw new IllegalArgumentException("INVALID_CLASS_MAGIC");
        }
        int minor = input.readUnsignedShort();
        int major = input.readUnsignedShort();
        if (major < 45 || major > 65 || minor == 65535) {
            throw new IllegalArgumentException("NON_JAVA21_CLASS: " + major + "." + minor);
        }
        return major;
    }

    /** Streams bytes into the standard digest; a hash proves identity, not publisher authenticity. */
    public static String sha256(Path path) throws IOException {
        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("Java platform lacks mandatory SHA-256", impossible);
        }
        try (InputStream input = new DigestInputStream(Files.newInputStream(path), digest)) {
            input.transferTo(OutputStream.nullOutputStream());
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
