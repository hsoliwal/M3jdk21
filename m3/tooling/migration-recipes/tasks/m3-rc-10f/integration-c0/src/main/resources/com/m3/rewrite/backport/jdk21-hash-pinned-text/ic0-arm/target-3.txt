// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** In-memory Java 21 compiler/class-loader used by A3 mastery fan-out. */
final class A3MemoryCompiler {
    private A3MemoryCompiler() {}

    static Class<?> compile(String className, String source) {
        String checkedName = text(className, "className");
        String checkedSource = Objects.requireNonNull(source, "source");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("A3 mastery requires a full JDK compiler");
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager standard =
                        compiler.getStandardFileManager(diagnostics, null, null);
                MemoryFileManager files = new MemoryFileManager(standard)) {
            JavaFileObject unit = new SourceObject(checkedName, checkedSource);
            List<String> options =
                    List.of("--release", "21", "-Xlint:all", "-Werror", "-proc:none");
            Boolean passed =
                    compiler.getTask(
                                    null,
                                    files,
                                    diagnostics,
                                    options,
                                    null,
                                    List.of(unit))
                            .call();
            if (!Boolean.TRUE.equals(passed)) {
                throw new IllegalStateException(render(diagnostics));
            }
            return new MemoryLoader(files.bytecode()).loadClass(checkedName);
        } catch (IOException | ClassNotFoundException failure) {
            throw new IllegalStateException("A3 in-memory compilation failed", failure);
        }
    }

    private static String render(
            DiagnosticCollector<JavaFileObject> diagnostics) {
        StringBuilder out = new StringBuilder("A3 javac rejected mastery source");
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            out.append('\n')
                    .append(diagnostic.getKind())
                    .append(" line=")
                    .append(diagnostic.getLineNumber())
                    .append(" column=")
                    .append(diagnostic.getColumnNumber())
                    .append(' ')
                    .append(diagnostic.getMessage(null));
        }
        return out.toString();
    }

    private static String text(String value, String field) {
        String checked = Objects.toString(value, "").strip();
        if (checked.isEmpty()
                || checked.indexOf('\0') >= 0
                || checked.indexOf('\n') >= 0
                || checked.indexOf('\r') >= 0
                || checked.indexOf('\t') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static final class SourceObject extends SimpleJavaFileObject {
        private final String source;

        private SourceObject(String className, String source) {
            super(
                    URI.create(
                            "string:///"
                                    + className.replace('.', '/')
                                    + JavaFileObject.Kind.SOURCE.extension),
                    JavaFileObject.Kind.SOURCE);
            this.source = source;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }

    private static final class ByteObject extends SimpleJavaFileObject {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        private ByteObject(String className, Kind kind) {
            super(
                    URI.create(
                            "bytes:///"
                                    + className.replace('.', '/')
                                    + kind.extension),
                    kind);
        }

        @Override
        public OutputStream openOutputStream() {
            return out;
        }

        byte[] bytes() {
            return out.toByteArray();
        }
    }

    private static final class MemoryFileManager
            extends ForwardingJavaFileManager<StandardJavaFileManager> {
        private final Map<String, ByteObject> output = new LinkedHashMap<>();

        private MemoryFileManager(StandardJavaFileManager fileManager) {
            super(fileManager);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(
                JavaFileManager.Location location,
                String className,
                JavaFileObject.Kind kind,
                FileObject sibling) {
            ByteObject file = new ByteObject(className, kind);
            output.put(className, file);
            return file;
        }

        Map<String, byte[]> bytecode() {
            LinkedHashMap<String, byte[]> bytes = new LinkedHashMap<>();
            output.forEach((name, file) -> bytes.put(name, file.bytes()));
            return Map.copyOf(bytes);
        }
    }

    private static final class MemoryLoader extends ClassLoader {
        private final Map<String, byte[]> bytecode;

        private MemoryLoader(Map<String, byte[]> bytecode) {
            super(A3MemoryCompiler.class.getClassLoader());
            this.bytecode = Map.copyOf(bytecode);
        }

        @Override
        protected Class<?> findClass(String name)
                throws ClassNotFoundException {
            byte[] bytes = bytecode.get(name);
            if (bytes == null) {
                return super.findClass(name);
            }
            return defineClass(name, bytes, 0, bytes.length);
        }
    }
}
