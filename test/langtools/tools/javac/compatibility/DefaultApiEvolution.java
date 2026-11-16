/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only
 *
 * @test
 * @summary Separate source, binary and dispatch checks for additive API evolution
 * @modules java.compiler jdk.compiler
 * @run main/othervm DefaultApiEvolution
 */

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Finite language/runtime regressions; this is not an all-client compatibility proof. */
public class DefaultApiEvolution {
    private static final JavaCompiler COMPILER = ToolProvider.getSystemJavaCompiler();
    private static int compilations;
    private static int refusals;
    private static int invocations;
    private static int checks;
    private final Path root;

    private DefaultApiEvolution(Path root) {
        this.root = root;
    }

    public static void main(String[] args) throws Exception {
        if (COMPILER == null) {
            throw new AssertionError("A JDK compiler is required; no test is skipped");
        }
        Path root = Files.createTempDirectory(Path.of("."), "default-api-evolution-");
        try {
            DefaultApiEvolution test = new DefaultApiEvolution(root);
            test.compatibleAdditions();
            test.nullOverload();
            test.conflictingDefaults();
            test.abstractObligation();
            test.abstractSuperclassPrecedence();
            System.out.printf("PASS: %d compilations, %d expected refusals, "
                    + "%d runtime invocations, %d checks%n",
                    compilations, refusals, invocations, checks);
        } finally {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
    }

    private void compatibleAdditions() throws Exception {
        Map<String, String> before = Map.of(
                "p.Operation", """
                    package p;
                    @FunctionalInterface public interface Operation {
                        String apply(String value);
                    }
                    """,
                "p.Support", """
                    package p;
                    public abstract class Support implements Operation {
                        public abstract String apply(String value);
                    }
                    """,
                "p.Provider", """
                    package p;
                    public class Provider extends Support {
                        public String apply(String value) { return "provider:" + value; }
                    }
                    """,
                "p.ConcreteSupport", """
                    package p;
                    public abstract class ConcreteSupport implements Operation {
                        public String apply() { return "class"; }
                    }
                    """,
                "p.Child", """
                    package p;
                    public class Child extends ConcreteSupport {
                        public String apply(String value) { return "child:" + value; }
                    }
                    """,
                "p.Client", """
                    package p;
                    public class Client {
                        public static String run() {
                            Operation op = new Provider();
                            Operation lambda = v -> "lambda:" + v;
                            java.util.function.Function<String, String> method = op::apply;
                            return op.apply(null) + "|" + lambda.apply("x")
                                    + "|" + method.apply("y");
                        }
                    }
                    """);
        Path old = compile("safe-old", before, null);
        String frozen = digest(old);
        String expected = "provider:null|lambda:x|provider:y";
        equal(expected, invoke("p.Client", old), "baseline behavior");
        Path api = compile("safe-api", Map.of(
                "p.Operation", """
                    package p;
                    @FunctionalInterface public interface Operation {
                        String apply(String value);
                        default String apply() { return apply("default"); }
                        default String apply(String value, int times) {
                            return apply(value).repeat(times);
                        }
                    }
                    """,
                "p.Support", """
                    package p;
                    public abstract class Support implements Operation {
                        public abstract String apply(String value);
                        public String prefixed(String value) { return "[" + apply(value) + "]"; }
                    }
                    """), null);
        equal(expected, invoke("p.Client", api, old), "unchanged client/provider binaries");
        Map<String, String> clients = new TreeMap<>(before);
        clients.remove("p.Operation");
        clients.remove("p.Support");
        Path rebuilt = compile("safe-recompiled", clients, classPath(api));
        equal(expected, invoke("p.Client", rebuilt, api), "recompiled lambda/method reference");
        Path caller = compile("safe-new-client", Map.of("p.NewClient", """
                package p;
                public class NewClient {
                    public static String run() {
                        Operation op = new Provider();
                        return op.apply() + "|" + op.apply("z", 2)
                                + "|" + new Provider().prefixed("b")
                                + "|" + ((Operation) new Child()).apply();
                    }
                }
                """), classPath(api, old));
        equal("provider:default|provider:zprovider:z|[provider:b]|class",
                invoke("p.NewClient", caller, api, old),
                "new operations dispatch into old providers; concrete superclass wins");
        equal(frozen, digest(old), "baseline class files remain untouched");
    }

    private void nullOverload() throws Exception {
        String client = """
                package n;
                public class Client {
                    public static int run() { return ((Input) new Provider()).read(null); }
                }
                """;
        Path old = compile("null-old", Map.of(
                "n.Input", "package n; public interface Input { int read(java.nio.CharBuffer b); }",
                "n.Provider", """
                    package n;
                    public class Provider implements Input {
                        public int read(java.nio.CharBuffer b) { return b == null ? 7 : 0; }
                    }
                    """, "n.Client", client), null);
        equal(7, invoke("n.Client", old), "baseline null call");
        Path api = compile("null-api", Map.of("n.Input", """
                package n;
                public interface Input {
                    int read(java.nio.CharBuffer b);
                    default int read(char[] b) { return 9; }
                }
                """), null);
        equal(7, invoke("n.Client", api, old), "old binary retains CharBuffer overload");
        reject("null-recompile", Map.of("n.Client", client), classPath(api, old),
                "compiler.err.ref.ambiguous");
    }

    private void conflictingDefaults() throws Exception {
        Map<String, String> before = Map.of(
                "d.A", "package d; public interface A { default String value() { return \"a\"; } }",
                "d.B", "package d; public interface B { }",
                "d.Both", "package d; public interface Both extends A, B { }",
                "d.Provider", "package d; public class Provider implements Both { }",
                "d.Client", """
                    package d;
                    public class Client {
                        public static String run() { return new Provider().value(); }
                    }
                    """);
        Path old = compile("diamond-old", before, null);
        equal("a", invoke("d.Client", old), "baseline default selection");
        Path api = compile("diamond-api", Map.of("d.B",
                "package d; public interface B { default String value() { return \"b\"; } }"), null);
        throwsAtInvocation(IncompatibleClassChangeError.class, "d.Client", api, old);
        reject("diamond-recompile", Map.of("d.Both", before.get("d.Both")),
                classPath(api, old), "compiler.err.types.incompatible");
    }

    private void abstractObligation() throws Exception {
        String provider = "package a; public class Provider extends Base { }";
        Path old = compile("abstract-old", Map.of(
                "a.Base", "package a; public abstract class Base { public String old() { return \"old\"; } }",
                "a.Provider", provider,
                "a.Client", "package a; public class Client { public static String run() { return new Provider().old(); } }"), null);
        equal("old", invoke("a.Client", old), "baseline abstract-base call");
        Path api = compile("abstract-api", Map.of("a.Base", """
                package a;
                public abstract class Base {
                    public String old() { return "old"; }
                    public abstract String required();
                }
                """), null);
        equal("old", invoke("a.Client", api, old), "unchanged call does not prove new obligation safe");
        reject("abstract-recompile", Map.of("a.Provider", provider), classPath(api),
                "compiler.err.does.not.override.abstract");
        Path caller = compile("abstract-caller", Map.of("a.NewClient", """
                package a;
                public class NewClient {
                    public static String run() { return ((Base) new Provider()).required(); }
                }
                """), classPath(api, old));
        throwsAtInvocation(AbstractMethodError.class, "a.NewClient", caller, api, old);
    }

    private void abstractSuperclassPrecedence() throws Exception {
        reject("abstract-precedence", Map.of("BlockedDefault", """
                interface Contract { default void work() { } }
                abstract class Parent { public abstract void work(); }
                class BlockedDefault extends Parent implements Contract { }
                """), null, "compiler.err.does.not.override.abstract");
    }

    private Path compile(String name, Map<String, String> sources, String classPath)
            throws IOException {
        Path output = root.resolve(name);
        Compilation result = compileInto(output, sources, classPath);
        check(result.success(), name + " must compile: " + result.errors());
        return output;
    }

    private void reject(String name, Map<String, String> sources, String classPath,
                        String errorCode) throws IOException {
        Compilation result = compileInto(root.resolve(name), sources, classPath);
        check(!result.success(), name + " unexpectedly compiled");
        check(result.errors().contains(errorCode), name + " failed for a different reason: " + result.errors());
        refusals++;
    }

    private record Compilation(boolean success, List<String> errors) { }

    private Compilation compileInto(Path output, Map<String, String> sources, String classPath)
            throws IOException {
        Files.createDirectories(output);
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        List<String> options = List.of("--release", "21", "-proc:none", "-Xlint:all", "-Werror",
                "-d", output.toString(), "--class-path", classPath == null ? output.toString() : classPath,
                "--source-path", output.toString());
        List<JavaFileObject> units = new ArrayList<>();
        new TreeMap<>(sources).forEach((name, text) -> units.add(new SimpleJavaFileObject(
                URI.create("string:///" + name.replace('.', '/') + ".java"), JavaFileObject.Kind.SOURCE) {
            @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) { return text; }
        }));
        try (StandardJavaFileManager files = COMPILER.getStandardFileManager(
                diagnostics, null, StandardCharsets.UTF_8)) {
            boolean success = COMPILER.getTask(null, files, diagnostics, options, null, units).call();
            compilations++;
            return new Compilation(success, diagnostics.getDiagnostics().stream()
                    .filter(d -> d.getKind() == Diagnostic.Kind.ERROR).map(Diagnostic::getCode).toList());
        }
    }

    private static String classPath(Path... roots) {
        return java.util.Arrays.stream(roots).map(Path::toString)
                .collect(Collectors.joining(java.io.File.pathSeparator));
    }

    private static Object invoke(String name, Path... roots) throws Exception {
        URL[] urls = new URL[roots.length];
        for (int i = 0; i < roots.length; i++) urls[i] = roots[i].toUri().toURL();
        try (URLClassLoader loader = new URLClassLoader(urls, ClassLoader.getPlatformClassLoader())) {
            invocations++;
            return loader.loadClass(name).getMethod("run").invoke(null);
        }
    }

    private static void throwsAtInvocation(Class<? extends Error> expected, String name,
                                          Path... roots) throws Exception {
        try {
            invoke(name, roots);
            throw new AssertionError("Expected invocation failure: " + expected.getName());
        } catch (InvocationTargetException failure) {
            check(expected == failure.getCause().getClass(), "Wrong invocation failure: " + failure.getCause());
        }
    }

    private static String digest(Path root) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var files = Files.walk(root)) {
            for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
                digest.update(root.relativize(file).toString().getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                digest.update(Files.readAllBytes(file));
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void equal(Object expected, Object actual, String message) {
        check(expected.equals(actual), message + ": expected " + expected + ", actual " + actual);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
