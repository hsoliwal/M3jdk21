// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite.atom;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Java21 test compiler: source and bytecode stay in memory; each result has isolated static state. */
final class MemJava {
    private MemJava() { }

    static ClassLoader compile(Map<String, String> project) {
        if (project.isEmpty()) throw new IllegalArgumentException("EMPTY_PROJECT");
        var compiler = Objects.requireNonNull(
                ToolProvider.getSystemJavaCompiler(), "FULL_JDK_REQUIRED");
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (var standard = compiler.getStandardFileManager(
                diagnostics, Locale.ROOT, StandardCharsets.UTF_8);
                var files = new ByteFiles(standard)) {
            List<Source> sources = project.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> new Source(entry.getKey(), entry.getValue()))
                    .toList();
            var console = new StringWriter();
            boolean compiled = compiler.getTask(console, files, diagnostics,
                    List.of("--release", "21", "-proc:none", "-Xlint:all", "-Werror",
                            "--class-path", "", "--source-path", ""),
                    null, sources).call();
            if (!compiled) {
                throw new IllegalArgumentException(
                        "JAVA21_COMPILE_REFUSED\n" + diagnostics.getDiagnostics() + "\n" + console);
            }
            Map<String, byte[]> outputs = new LinkedHashMap<>();
            files.outputs.forEach((name, output) -> outputs.put(name, output.bytes.toByteArray()));
            Map<String, byte[]> frozen = Map.copyOf(outputs);
            return new ClassLoader(ClassLoader.getPlatformClassLoader()) {
                @Override
                protected Class<?> findClass(String name) throws ClassNotFoundException {
                    byte[] bytes = frozen.get(name);
                    if (bytes == null) throw new ClassNotFoundException(name);
                    return defineClass(name, bytes, 0, bytes.length);
                }
            };
        } catch (IOException failure) {
            throw new IllegalStateException("MEMORY_COMPILER_IO", failure);
        }
    }

    private static final class Source extends SimpleJavaFileObject {
        private final String text;

        private Source(String path, String text) {
            super(URI.create("string:///" + path), Kind.SOURCE);
            this.text = Objects.requireNonNull(text, "text");
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return text;
        }
    }

    private static final class ByteFile extends SimpleJavaFileObject {
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        private ByteFile(String name, Kind kind) {
            super(URI.create("memory:///" + name.replace('.', '/') + kind.extension), kind);
        }

        @Override
        public OutputStream openOutputStream() {
            return bytes;
        }
    }

    private static final class ByteFiles extends ForwardingJavaFileManager<StandardJavaFileManager> {
        private final Map<String, ByteFile> outputs = new LinkedHashMap<>();

        private ByteFiles(StandardJavaFileManager standard) {
            super(standard);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(
                JavaFileManager.Location location,
                String name,
                JavaFileObject.Kind kind,
                FileObject sibling) {
            ByteFile output = new ByteFile(name, kind);
            if (outputs.putIfAbsent(name, output) != null) {
                throw new IllegalStateException("DUPLICATE_CLASS_OUTPUT:" + name);
            }
            return output;
        }
    }
}
