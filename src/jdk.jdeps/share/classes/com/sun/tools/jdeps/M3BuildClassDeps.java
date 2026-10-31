/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors.
 * All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only,
 * as published by the Free Software Foundation. Oracle designates
 * this particular file as subject to the "Classpath" exception.
 */
package com.sun.tools.jdeps;

import com.sun.tools.classfile.AccessFlags;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ConstantPoolException;
import com.sun.tools.classfile.Dependencies;
import com.sun.tools.classfile.Dependency;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.spi.ToolProvider;

/**
 * M3JDK21-native class-file dependency fact tool, reusing the original jdeps
 * ClassFile/Dependencies engines, without constructing a second parser.
 *
 * <p>A detached fact snapshot retains only one compact sorted String[] target
 * lane and the original input-byte SHA-256. In particular, the temporary
 * open-addressed dictionary retains no per-edge entry graph or AST nodes.
 * The caller remains responsible for matching toolchain, ABI, module graph,
 * annotation processors and output provenance before build-result reuse.</p>
 */
public final class M3BuildClassDeps implements ToolProvider {
    public enum Scope { CLASS, PUBLIC_API }

    private static final int DEFAULT_MAX_BYTES = 32 * 1024 * 1024;
    private static final int DEFAULT_MAX_EDGES = 131_072;
    private static final int MAX_EDGES = 1 << 20;
    private static final String FORMAT = "M3JDK21_CLASS_DEPS_V1";

    /** One immutable, source-byte authenticated dependency snapshot. */
    public record Snapshot(String className, Scope scope, byte[] contentSha256, String[] targets) {
        public Snapshot {
            Objects.requireNonNull(className, "className");
            Objects.requireNonNull(scope, "scope");
            contentSha256 = Objects.requireNonNull(contentSha256, "contentSha256").clone();
            targets = Objects.requireNonNull(targets, "targets").clone();
            if (contentSha256.length != 32) throw new IllegalArgumentException("SHA-256 width");
            for (int i = 0; i < targets.length; i++) {
                if (targets[i] == null || (i > 0 && targets[i - 1].compareTo(targets[i]) >= 0)) {
                    throw new IllegalArgumentException("targets must be sorted and unique");
                }
            }
        }
        @Override public byte[] contentSha256() { return contentSha256.clone(); }
        @Override public String[] targets() { return targets.clone(); }
        public String digestHex() { return HexFormat.of().formatHex(contentSha256); }
        public boolean hasTarget(String binaryName) {
            return Arrays.binarySearch(targets, Objects.requireNonNull(binaryName)) >= 0;
        }
        /** Canonical, lossless ASCII wire format with Base64URL-encoded JVM names. */
        public void writeTo(PrintWriter out) {
            Objects.requireNonNull(out, "out");
            out.print(FORMAT); out.print('\t'); out.print(scope.name());
            out.print('\t'); out.print(digestHex()); out.print('\t');
            out.print(encoded(className)); out.print('\t'); out.println(targets.length);
            for (String name : targets) { out.print("D\t"); out.println(encoded(name)); }
            out.flush();
        }
    }

    /**
     * Read one bounded .class input and use original jdeps public-API/class
     * finders. Classpath/JAR expansion is deliberately not done in this atom.
     */
    public static Snapshot inspect(Path classFile, Scope scope, int maxBytes, int maxEdges)
            throws IOException {
        Objects.requireNonNull(classFile, "classFile");
        Objects.requireNonNull(scope, "scope");
        if (maxBytes < 1 || maxBytes > DEFAULT_MAX_BYTES || maxEdges < 1 || maxEdges > MAX_EDGES) {
            throw new IllegalArgumentException("invalid build-tool resource budget");
        }
        BasicFileAttributes before = Files.readAttributes(
                classFile, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!before.isRegularFile() || before.size() > maxBytes) {
            throw new IOException("build tool requires bounded regular class input");
        }
        byte[] bytes;
        try (InputStream input = Channels.newInputStream(Files.newByteChannel(classFile,
                Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)))) {
            bytes = input.readNBytes(maxBytes + 1);
        }
        if (bytes.length > maxBytes || before.size() != bytes.length) {
            throw new IOException("class input changed or exceeded byte budget");
        }
        ClassFile parsed;
        try {
            parsed = ClassFile.read(new ByteArrayInputStream(bytes));
        } catch (ConstantPoolException | Dependencies.ClassFileError invalid) {
            throw new IOException("invalid class-file dependency input", invalid);
        }
        String className;
        try {
            className = parsed.getName().replace('/', '.');
        } catch (ConstantPoolException invalid) {
            throw new IOException("invalid class-file identity", invalid);
        }
        Names names = new Names(maxEdges);
        if (!parsed.access_flags.is(AccessFlags.ACC_MODULE)) {
            Dependency.Finder finder = scope == Scope.PUBLIC_API
                    ? Dependencies.getAPIFinder(AccessFlags.ACC_PUBLIC)
                    : Dependencies.getClassDependencyFinder();
            try {
                for (Dependency dependency : finder.findDependencies(parsed)) {
                    names.add(dependency.getTarget().getClassName());
                }
            } catch (Dependencies.ClassFileError invalid) {
                throw new IOException("invalid class-file dependency data", invalid);
            }
        }
        return new Snapshot(className, scope, digest(bytes), names.sorted());
    }

    /** Parent-owned open-addressed dynamic String[] slots, no per-entry wrapper. */
    private static final class Names {
        private String[] slots = new String[16];
        private int count;
        private final int max;
        Names(int max) { this.max = max; }
        void add(String value) throws IOException {
            if (value == null) throw new IOException("null class dependency");
            if ((long) (count + 1) * 4L >= (long) slots.length * 3L) grow();
            int mask = slots.length - 1;
            for (int index = value.hashCode() & mask;; index = (index + 1) & mask) {
                String existing = slots[index];
                if (value.equals(existing)) return;
                if (existing == null) {
                    if (count == max) throw new IOException("class dependency count budget exceeded");
                    slots[index] = value;
                    count++;
                    return;
                }
            }
        }
        private void grow() throws IOException {
            if (slots.length > (long) MAX_EDGES * 4L) throw new IOException("dependency slot budget exceeded");
            String[] previous = slots;
            slots = new String[slots.length << 1];
            count = 0;
            for (String entry : previous) if (entry != null) add(entry);
        }
        String[] sorted() {
            String[] result = new String[count];
            int write = 0;
            for (String name : slots) if (name != null) result[write++] = name;
            Arrays.sort(result);
            return result;
        }
    }

    private static byte[] digest(byte[] content) {
        try { return MessageDigest.getInstance("SHA-256").digest(content); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    private static String encoded(String name) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                name.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Override public String name() { return "m3-class-deps"; }
    @Override public Optional<String> description() {
        return Optional.of("M3JDK21 native original-jdeps class and API dependency facts");
    }
    /** ToolProvider entry, not a second VM/process/JavacTask. */
    @Override public int run(PrintWriter out, PrintWriter err, String... arguments) {
        Objects.requireNonNull(out, "out");
        Objects.requireNonNull(err, "err");
        Objects.requireNonNull(arguments, "arguments");
        if (arguments.length != 1 && arguments.length != 2) {
            err.println("usage: m3-class-deps [--api] <class-file>");
            return 2;
        }
        Scope scope = Scope.CLASS;
        String path = arguments[arguments.length - 1];
        if (arguments.length == 2) {
            if (!"--api".equals(arguments[0])) {
                err.println("unknown m3-class-deps option");
                return 2;
            }
            scope = Scope.PUBLIC_API;
        }
        try {
            inspect(Path.of(path), scope, DEFAULT_MAX_BYTES, DEFAULT_MAX_EDGES).writeTo(out);
            return 0;
        } catch (IOException | RuntimeException failure) {
            err.println("m3-class-deps: " + failure.getClass().getSimpleName());
            return 1;
        }
    }
}
