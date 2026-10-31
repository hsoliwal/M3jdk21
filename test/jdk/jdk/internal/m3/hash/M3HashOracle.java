/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.tools.ToolProvider;

/**
 * The oracle of the jdk.internal.m3.hash tests: the vendored zero-allocation-hashing 0.16
 * (m3/vendor/third_party/zero-allocation-hashing), compiled unchanged. {@code -Dm3.hash.oracle=<classes dir>} names a
 * prebuilt copy; otherwise the donor sources are looked up above {@code test.src} and compiled once into a temporary
 * directory (the test needs jdk.compiler; the donor needs jdk.unsupported and java.base/sun.nio.ch).
 */
final class M3HashOracle {
    static final String PKG = "net.openhft.hashing.";
    private static final String ORACLE_PROPERTY = "m3.hash.oracle";
    private static final String TEST_SRC_PROPERTY = "test.src";
    private static final String HERE = ".";
    private static final String JAVA = ".java";
    private static final String TEMP_PREFIX = "m3-hash-oracle";
    private static final String DONOR = "m3/vendor/third_party/zero-allocation-hashing/src/main/java";
    private static final String ANNOTATIONS = "m3/vendor/third_party/annotations/src/main/java";
    private static final String JSR305 = "m3/vendor/third_party/jsr305/src/main/java";
    private static final List<String> JAVAC_OPTIONS = List.of("-proc:none", "-nowarn", "-XDsuppressNotes",
            "--add-exports", "java.base/sun.nio.ch=ALL-UNNAMED");
    private static final int JAVAC_OK = 0;
    private static final ClassLoader LOADER = load();

    private M3HashOracle() {}

    static Class<?> type(String simple) {
        try {
            return Class.forName(PKG + simple, true, LOADER);
        } catch (ClassNotFoundException e) {
            throw new AssertionError("oracle type missing: " + simple, e);
        }
    }

    /** A public static method of an oracle type, called with the given arguments. */
    static Object call(String type, String name, Class<?>[] params, Object... args) {
        try {
            Class<?> c = type(type);
            Class<?> ret = c.getMethod(name, params).getReturnType();
            return MethodHandles.publicLookup().findStatic(c, name, MethodType.methodType(ret, params))
                    .invokeWithArguments(args);
        } catch (Throwable e) {
            throw new AssertionError("oracle call failed: " + type + "." + name, e);
        }
    }

    /** A public instance method of an oracle type as a handle of type (Object, params...)ret. */
    static MethodHandle virtual(String type, String name, Class<?> ret, Class<?>... params) {
        try {
            MethodHandle mh = MethodHandles.publicLookup()
                    .findVirtual(type(type), name, MethodType.methodType(ret, params));
            return mh.asType(mh.type().changeParameterType(0, Object.class));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("oracle method missing: " + type + "." + name, e);
        }
    }

    private static ClassLoader load() {
        try {
            String prebuilt = System.getProperty(ORACLE_PROPERTY);
            Path classes = prebuilt != null ? Path.of(prebuilt) : compile(repositoryRoot());
            return new URLClassLoader(new URL[] {classes.toUri().toURL()}, M3HashOracle.class.getClassLoader());
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static Path repositoryRoot() {
        for (Path p = Path.of(System.getProperty(TEST_SRC_PROPERTY, HERE)).toAbsolutePath(); p != null; p = p.getParent()) {
            if (Files.isDirectory(p.resolve(DONOR))) {
                return p;
            }
        }
        throw new IllegalStateException(DONOR + " not found above " + TEST_SRC_PROPERTY + "; set -D" + ORACLE_PROPERTY);
    }

    private static Path compile(Path root) throws IOException {
        Path out = Files.createTempDirectory(TEMP_PREFIX);
        List<String> args = new ArrayList<>(JAVAC_OPTIONS);
        args.addAll(List.of("-sourcepath", root.resolve(ANNOTATIONS) + File.pathSeparator + root.resolve(JSR305),
                "-d", out.toString()));
        try (Stream<Path> files = Files.walk(root.resolve(DONOR))) {
            files.map(Path::toString).filter(p -> p.endsWith(JAVA)).sorted().forEach(args::add);
        }
        int rc = ToolProvider.getSystemJavaCompiler().run(null, null, null, args.toArray(String[]::new));
        if (rc != JAVAC_OK) {
            throw new IllegalStateException("javac of the vendored donor failed: exit " + rc);
        }
        return out;
    }
}
