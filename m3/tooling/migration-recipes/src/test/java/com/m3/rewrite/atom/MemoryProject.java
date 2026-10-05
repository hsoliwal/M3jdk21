// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.atom;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
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

/** Test-only JavaCompiler project: immutable sources, in-memory output, isolated class identity. */
final class MemoryProject {
    private MemoryProject() {}

    static Image compile(Map<String, String> source) throws IOException {
        var compiler = Objects.requireNonNull(ToolProvider.getSystemJavaCompiler(), "full JDK required");
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        var output = new LinkedHashMap<String, ByteCode>();
        try (StandardJavaFileManager standard = compiler.getStandardFileManager(
                        diagnostics, Locale.ROOT, StandardCharsets.UTF_8);
                var files = new ForwardingJavaFileManager<StandardJavaFileManager>(standard) {
                    @Override
                    public JavaFileObject getJavaFileForOutput(
                            JavaFileManager.Location location, String name,
                            JavaFileObject.Kind kind, FileObject sibling) {
                        if (kind != JavaFileObject.Kind.CLASS || output.containsKey(name)) {
                            throw new IllegalStateException("unexpected compiler output: " + name);
                        }
                        ByteCode code = new ByteCode(name);
                        output.put(name, code);
                        return code;
                    }
                }) {
            List<JavaFileObject> inputs = source.entrySet().stream()
                    .<JavaFileObject>map(entry -> new Source(entry.getKey(), entry.getValue()))
                    .toList();
            var text = new StringWriter();
            boolean success = Boolean.TRUE.equals(compiler.getTask(
                    text, files, diagnostics,
                    List.of("--release", "21", "-proc:none", "-g:none", "-Xlint:all", "-Werror"),
                    null, inputs).call());
            if (!success) {
                throw new AssertionError("JAVA21_COMPILE: " + diagnostics.getDiagnostics() + text);
            }
        }
        Map<String, byte[]> bytes = new LinkedHashMap<>();
        output.forEach((name, code) -> bytes.put(name, code.bytes.toByteArray()));
        return new Image(bytes);
    }

    static final class Image extends ClassLoader {
        private final Map<String, byte[]> bytecode;

        Image(Map<String, byte[]> bytecode) {
            super(ClassLoader.getPlatformClassLoader());
            this.bytecode = Collections.unmodifiableMap(new LinkedHashMap<>(bytecode));
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] bytes = bytecode.get(name);
            if (bytes == null) throw new ClassNotFoundException(name);
            return defineClass(name, bytes, 0, bytes.length);
        }

        Outcome invoke(String owner, String name, Class<?>[] types, Object... arguments)
                throws ReflectiveOperationException {
            try {
                return new Outcome(loadClass(owner).getMethod(name, types).invoke(null, arguments), "", "");
            } catch (InvocationTargetException exception) {
                Throwable cause = exception.getCause();
                return new Outcome(null, cause.getClass().getName(), cause.getMessage());
            }
        }

        List<String> surface() throws ClassNotFoundException {
            List<String> members = new ArrayList<>();
            for (String name : bytecode.keySet()) {
                Class<?> type = loadClass(name);
                if (visible(type.getModifiers())) {
                    members.add(type.getName() + ":" + type.getModifiers());
                }
                for (var method : type.getDeclaredMethods()) {
                    if (visible(method.getModifiers())) members.add(method.toGenericString());
                }
                for (var field : type.getDeclaredFields()) {
                    if (visible(field.getModifiers())) members.add(field.toGenericString());
                }
                for (var constructor : type.getDeclaredConstructors()) {
                    if (visible(constructor.getModifiers())) members.add(constructor.toGenericString());
                }
            }
            return members.stream().sorted().toList();
        }

        private static boolean visible(int modifiers) {
            return Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers);
        }
    }

    record Outcome(Object value, String exceptionType, String exceptionMessage) {}

    private static final class Source extends SimpleJavaFileObject {
        private final String text;

        Source(String path, String text) {
            super(sourceUri(path), Kind.SOURCE);
            this.text = Objects.requireNonNull(text, "source");
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return text;
        }
    }

    private static URI sourceUri(String path) {
        if (!path.matches("[A-Za-z_$][A-Za-z0-9_$/]*\\.java")
                || path.contains("//")) {
            throw new IllegalArgumentException("unsafe fixture path: " + path);
        }
        return URI.create("mem:///" + path);
    }

    private static final class ByteCode extends SimpleJavaFileObject {
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        ByteCode(String name) {
            super(URI.create("mem:///" + name.replace('.', '/') + Kind.CLASS.extension), Kind.CLASS);
        }

        @Override
        public OutputStream openOutputStream() {
            return bytes;
        }
    }
}
