/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.runtime.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import javax.tools.ToolProvider;

/** Fixtures use actual class files, ModuleReader contracts, and the platform compiler. */
public final class ModuleFixture {
    private ModuleFixture() { }

    public static Path compileJar(Path work, String jarName, Map<String, String> source,
            Map<String, String> headers, Map<String, byte[]> resources) throws IOException {
        Path sources = Files.createDirectories(work.resolve("source"));
        Path classes = Files.createDirectories(work.resolve("classes"));
        java.util.List<String> args = new java.util.ArrayList<>();
        args.addAll(java.util.List.of("--release", "21", "-classpath", classPath(), "-d", classes.toString()));
        for (var file : source.entrySet()) {
            Path path = sources.resolve(file.getKey());
            Files.createDirectories(path.getParent());
            Files.writeString(path, file.getValue());
            args.add(path.toString());
        }
        int status = ToolProvider.getSystemJavaCompiler().run(null, null, null, args.toArray(String[]::new));
        if (status != 0) throw new IOException("Fixture compilation failed: " + status);
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        headers.forEach((name, value) -> manifest.getMainAttributes().putValue(name, value));
        Path jar = work.resolve(jarName);
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar), manifest);
                Stream<Path> entries = Files.walk(classes)) {
            for (Path entry : entries.filter(Files::isRegularFile).sorted().toList()) {
                out.putNextEntry(new JarEntry(classes.relativize(entry).toString().replace('\\', '/')));
                Files.copy(entry, out);
                out.closeEntry();
            }
            for (var entry : resources.entrySet()) {
                out.putNextEntry(new JarEntry(entry.getKey()));
                out.write(entry.getValue());
                out.closeEntry();
            }
        }
        return jar;
    }

    public static String classPath() {
        return System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
    }

    public static ModuleReference reference(Path jar) {
        return ModuleFinder.of(jar).findAll().iterator().next();
    }

    public static CountingReference classes(String moduleName, Class<?>... types) throws IOException {
        LinkedHashMap<String, byte[]> rows = new LinkedHashMap<>();
        for (Class<?> type : types) {
            String path = type.getName().replace('.', '/') + ".class";
            try (InputStream in = type.getClassLoader().getResourceAsStream(path)) {
                if (in == null) throw new IOException("Missing fixture class: " + path);
                rows.put(path, in.readAllBytes());
            }
        }
        return new CountingReference(moduleName, rows);
    }

    public static final class CountingReference extends ModuleReference {
        public final AtomicInteger opened = new AtomicInteger();
        public final AtomicInteger closed = new AtomicInteger();
        public final AtomicInteger payloadReads = new AtomicInteger();
        public final AtomicInteger listCalls = new AtomicInteger();
        private final Map<String, byte[]> rows;

        public CountingReference(String name, Map<String, byte[]> rows) {
            super(ModuleDescriptor.newAutomaticModule(name).build(), URI.create("m3-test:/" + name));
            this.rows = new LinkedHashMap<>(rows);
        }

        @Override
        public ModuleReader open() {
            opened.incrementAndGet();
            return new ModuleReader() {
                private boolean open = true;
                private void requireOpen() throws IOException {
                    if (!open) throw new java.nio.channels.ClosedChannelException();
                }
                @Override public Optional<URI> find(String name) throws IOException {
                    requireOpen();
                    return rows.containsKey(name) ? Optional.of(URI.create("m3-test:/" + descriptor().name() + "/" + name)) : Optional.empty();
                }
                @Override public Optional<InputStream> open(String name) throws IOException {
                    requireOpen();
                    byte[] bytes = rows.get(name);
                    if (bytes == null) return Optional.empty();
                    payloadReads.incrementAndGet();
                    return Optional.of(new ByteArrayInputStream(bytes));
                }
                @Override public Stream<String> list() throws IOException {
                    requireOpen();
                    listCalls.incrementAndGet();
                    return rows.keySet().stream();
                }
                @Override public void close() {
                    if (open) {
                        open = false;
                        closed.incrementAndGet();
                    }
                }
            };
        }
    }
}
