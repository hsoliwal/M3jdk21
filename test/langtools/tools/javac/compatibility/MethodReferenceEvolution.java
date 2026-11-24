/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only
 *
 * @test
 * @summary Different-arity overloads can break exact method-reference selection
 * @modules java.compiler jdk.compiler
 * @run main/othervm MethodReferenceEvolution
 */

import java.io.IOException;
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

/** Finite source-selection regression, separate from old-binary execution. */
public class MethodReferenceEvolution {
    private static final JavaCompiler COMPILER = ToolProvider.getSystemJavaCompiler();
    private static int compilations;
    private static int refusals;
    private static int invocations;
    private static int checks;
    private final Path root;

    private MethodReferenceEvolution(Path root) {
        this.root = root;
    }

    public static void main(String[] args) throws Exception {
        if (COMPILER == null) {
            throw new AssertionError("A JDK compiler is required; no test is skipped");
        }
        Path root = Files.createTempDirectory(Path.of("."), "method-reference-evolution-");
        try {
            MethodReferenceEvolution test = new MethodReferenceEvolution(root);
            test.checkEvolution();
            test.checkConcreteEvolution();
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

    private void checkEvolution() throws Exception {
        String provider = """
                package p;
                public final class Provider implements Monitor {
                    private final StringBuilder trace = new StringBuilder();
                    @Override public void begin(String name, long total) {
                        trace.append("begin:").append(name).append(':').append(total).append(';');
                    }
                    @Override public void worked(long units) {
                        trace.append("worked:").append(units).append(';');
                    }
                    public String trace() { return trace.toString(); }
                }
                """;
        String client = """
                package p;
                import java.util.function.Consumer;
                import java.util.function.Function;
                import java.util.function.BiConsumer;
                import java.util.function.BiFunction;
                public final class Client {
                    @SuppressWarnings("overloads")
                    private static void select(Consumer<Long> action) { action.accept(3L); }
                    @SuppressWarnings("overloads")
                    private static void select(Function<Long, Object> function) {
                        throw new AssertionError("selected Function instead of Consumer");
                    }
                    @SuppressWarnings("overloads")
                    private static void selectBegin(BiConsumer<String, Long> action) {
                        action.accept("legacy", 7L);
                    }
                    @SuppressWarnings("overloads")
                    private static void selectBegin(BiFunction<String, Long, Object> function) {
                        throw new AssertionError("selected BiFunction instead of BiConsumer");
                    }
                    public static String run() {
                        Provider provider = new Provider();
                        Monitor monitor = provider;
                        select(monitor::worked);
                        selectBegin(monitor::begin);
                        return provider.trace();
                    }
                }
                """;
        Map<String, String> legacySources = Map.of("p.Provider", provider, "p.Client", client);
        Path baseline = compile("baseline-api", api(""), null);
        Path legacy = compile("legacy-client", legacySources, classPath(baseline));
        String legacyDigest = digest(legacy);
        String expected = "worked:3;begin:legacy:7;";
        equal(expected, invoke("p.Client", baseline, legacy), "baseline selection and callbacks");

        Path overloaded = compile("overloaded-api", api("""
                default void begin(String task) { begin(task, UNKNOWN_WORK); }
                default void worked() { worked(1L); }
                """), null);
        equal(expected, invoke("p.Client", overloaded, legacy), "old binary with overloads");
        Compilation rejected = compileInto(root.resolve("rejected-client"), legacySources,
                classPath(overloaded));
        check(!rejected.success(), "same-name overloads must reject the unchanged references");
        equal(List.of("compiler.err.ref.ambiguous", "compiler.err.ref.ambiguous"),
                rejected.errors().stream().map(CompileError::code).toList(),
                "both reference expressions must fail solely for ambiguity");
        equal(List.of(lineOf(client, "select(monitor::worked);"),
                        lineOf(client, "selectBegin(monitor::begin);")),
                rejected.errors().stream().map(CompileError::line).sorted().toList(),
                "ambiguities must identify the worked and begin reference expressions");
        check(rejected.errors().stream().allMatch(e -> e.source().endsWith("/p/Client.java")),
                "ambiguities must originate in the unchanged client: " + rejected.errors());
        refusals++;

        Path distinct = compile("distinct-api", api("""
                default void beginUnknownWork(String task) { begin(task, UNKNOWN_WORK); }
                default void workedOneUnit() { worked(1L); }
                """), null);
        equal(expected, invoke("p.Client", distinct, legacy), "old binary with distinct names");
        Path recompiled = compile("recompiled-client", legacySources, classPath(distinct));
        equal(expected, invoke("p.Client", distinct, recompiled),
                "unchanged source retains selection and callbacks with distinct names");
        Path newClient = compile("new-client", Map.of("p.NewClient", """
                package p;
                public final class NewClient {
                    public static String run() {
                        Provider provider = new Provider();
                        Monitor monitor = provider;
                        monitor.beginUnknownWork("new");
                        monitor.workedOneUnit();
                        return provider.trace();
                    }
                }
                """), classPath(distinct, recompiled));
        equal("begin:new:-1;worked:1;", invoke("p.NewClient", distinct, recompiled, newClient),
                "distinct defaults delegate once to the original virtual hooks");
        equal(legacyDigest, digest(legacy), "the original client and provider binaries stay unchanged");
    }


    /** Concrete methods on an abstract base have the same source-selection risk. */
    private void checkConcreteEvolution() throws Exception {
        String provider = """
                package p;
                public final class IoProvider extends IoBase {
                    public static final byte[] BYTES = {1, 2, 3};
                    public static final Object MONITOR = new Object();
                    private final StringBuilder trace = new StringBuilder();
                    @Override public int read(byte[] bytes, int offset, int length, Object monitor) {
                        checkIdentity(bytes, monitor);
                        trace.append("read:").append(offset).append(':').append(length).append(';');
                        return offset + length;
                    }
                    @Override public void write(byte[] bytes, int offset, int length, Object monitor) {
                        checkIdentity(bytes, monitor);
                        trace.append("write:").append(offset).append(':').append(length).append(';');
                    }
                    private static void checkIdentity(byte[] bytes, Object monitor) {
                        if (bytes != BYTES || monitor != MONITOR) throw new AssertionError("identity");
                    }
                    public String trace() { return trace.toString(); }
                }
                """;
        String client = """
                package p;
                @SuppressWarnings("overloads")
                public final class IoClient {
                    interface ReadPrimitive { int run(byte[] b, int o, int n, Object m); }
                    interface ReadBoxed { Integer run(byte[] b, int o, int n, Object m); }
                    interface WriteVoid { void run(byte[] b, int o, int n, Object m); }
                    interface WriteValue { Object run(byte[] b, int o, int n, Object m); }
                    interface UnboundReadPrimitive { int run(IoBase x, byte[] b, int o, int n, Object m); }
                    interface UnboundReadBoxed { Integer run(IoBase x, byte[] b, int o, int n, Object m); }
                    interface UnboundWriteVoid { void run(IoBase x, byte[] b, int o, int n, Object m); }
                    interface UnboundWriteValue { Object run(IoBase x, byte[] b, int o, int n, Object m); }
                    private static void selectRead(ReadPrimitive f) {
                        if (f.run(IoProvider.BYTES, 1, 2, IoProvider.MONITOR) != 3)
                            throw new AssertionError("primitive read result");
                    }
                    private static void selectRead(ReadBoxed f) { throw new AssertionError("boxed"); }
                    private static void selectWrite(WriteVoid f) {
                        f.run(IoProvider.BYTES, 0, 3, IoProvider.MONITOR);
                    }
                    private static void selectWrite(WriteValue f) { throw new AssertionError("value"); }
                    private static void selectUnboundRead(UnboundReadPrimitive f, IoBase io) {
                        if (f.run(io, IoProvider.BYTES, 1, 2, IoProvider.MONITOR) != 3)
                            throw new AssertionError("unbound primitive read result");
                    }
                    private static void selectUnboundRead(UnboundReadBoxed f, IoBase io) {
                        throw new AssertionError("unbound boxed");
                    }
                    private static void selectUnboundWrite(UnboundWriteVoid f, IoBase io) {
                        f.run(io, IoProvider.BYTES, 0, 3, IoProvider.MONITOR);
                    }
                    private static void selectUnboundWrite(UnboundWriteValue f, IoBase io) {
                        throw new AssertionError("unbound value");
                    }
                    public static String run() {
                        IoProvider provider = new IoProvider();
                        IoBase io = provider;
                        selectRead(io::read);
                        selectWrite(io::write);
                        selectUnboundRead(IoBase::read, io);
                        selectUnboundWrite(IoBase::write, io);
                        return provider.trace();
                    }
                }
                """;
        Map<String, String> legacySources = Map.of("p.IoProvider", provider, "p.IoClient", client);
        Path baseline = compile("concrete-baseline-api", concreteApi(""), null);
        Path legacy = compile("concrete-legacy-client", legacySources, classPath(baseline));
        String legacyDigest = digest(legacy);
        String expected = "read:1:2;write:0:3;read:1:2;write:0:3;";
        equal(expected, invoke("p.IoClient", baseline, legacy), "concrete baseline virtual hooks");

        Path overloaded = compile("concrete-overloaded-api", concreteApi("""
                public int read(byte[] bytes, Object monitor) {
                    return read(bytes, 0, bytes.length, monitor);
                }
                public void write(byte[] bytes, Object monitor) {
                    write(bytes, 0, bytes.length, monitor);
                }
                """), null);
        equal(expected, invoke("p.IoClient", overloaded, legacy), "concrete old binary with overloads");
        Compilation rejected = compileInto(root.resolve("concrete-rejected-client"), legacySources,
                classPath(overloaded));
        check(!rejected.success(), "concrete overloads must reject unchanged selector source");
        equal(List.of("compiler.err.ref.ambiguous", "compiler.err.ref.ambiguous",
                        "compiler.err.ref.ambiguous", "compiler.err.ref.ambiguous"),
                rejected.errors().stream().map(CompileError::code).toList(),
                "bound and unbound references fail only for ambiguity");
        equal(List.of(lineOf(client, "selectRead(io::read);"),
                        lineOf(client, "selectWrite(io::write);"),
                        lineOf(client, "selectUnboundRead(IoBase::read, io);"),
                        lineOf(client, "selectUnboundWrite(IoBase::write, io);")),
                rejected.errors().stream().map(CompileError::line).sorted().toList(),
                "diagnostics identify all four unchanged reference expressions");
        check(rejected.errors().stream().allMatch(e -> e.source().endsWith("/p/IoClient.java")),
                "concrete ambiguity diagnostics must belong to the client: " + rejected.errors());
        refusals++;

        // A cast edits the client. Its success is a mitigation, not source compatibility.
        String typedClient = client
                .replace("selectRead(io::read);", "selectRead((ReadPrimitive) io::read);")
                .replace("selectWrite(io::write);", "selectWrite((WriteVoid) io::write);")
                .replace("selectUnboundRead(IoBase::read, io);",
                        "selectUnboundRead((UnboundReadPrimitive) IoBase::read, io);")
                .replace("selectUnboundWrite(IoBase::write, io);",
                        "selectUnboundWrite((UnboundWriteVoid) IoBase::write, io);");
        check(!client.equals(typedClient), "the explicit-target control must change client source");
        Path typed = compile("concrete-typed-client", Map.of("p.IoProvider", provider,
                "p.IoClient", typedClient), classPath(overloaded));
        equal(expected, invoke("p.IoClient", overloaded, typed), "explicit-target control dispatch");

        Path distinct = compile("concrete-distinct-api", concreteApi("""
                public int readWhole(byte[] bytes, Object monitor) {
                    return read(bytes, 0, bytes.length, monitor);
                }
                public void writeWhole(byte[] bytes, Object monitor) {
                    write(bytes, 0, bytes.length, monitor);
                }
                """), null);
        equal(expected, invoke("p.IoClient", distinct, legacy), "concrete old binary with distinct names");
        Path recompiled = compile("concrete-recompiled-client", legacySources, classPath(distinct));
        equal(expected, invoke("p.IoClient", distinct, recompiled),
                "concrete unchanged source preserves all four selector choices");
        Path newClient = compile("concrete-new-client", Map.of("p.NewIoClient", """
                package p;
                public final class NewIoClient {
                    public static String run() {
                        IoProvider provider = new IoProvider();
                        IoBase io = provider;
                        if (io.readWhole(IoProvider.BYTES, IoProvider.MONITOR) != 3)
                            throw new AssertionError("whole-array result");
                        io.writeWhole(IoProvider.BYTES, IoProvider.MONITOR);
                        return provider.trace();
                    }
                }
                """), classPath(distinct, recompiled));
        equal("read:0:3;write:0:3;", invoke("p.NewIoClient", distinct, recompiled, newClient),
                "distinct concrete methods delegate once with original array and monitor identity");
        equal(legacyDigest, digest(legacy), "the original concrete provider/client binaries stay unchanged");
    }

    private static Map<String, String> concreteApi(String additions) {
        return Map.of("p.IoBase", """
                package p;
                public abstract class IoBase {
                    public int read(byte[] bytes, int offset, int length, Object monitor) {
                        throw new AssertionError("existing read override bypassed");
                    }
                    public void write(byte[] bytes, int offset, int length, Object monitor) {
                        throw new AssertionError("existing write override bypassed");
                    }
                """ + additions + "}\n");
    }

    private static Map<String, String> api(String additions) {
        return Map.of("p.Monitor", """
                package p;
                public interface Monitor {
                    long UNKNOWN_WORK = -1L;
                    default void begin(String task, long total) { }
                    default void worked(long units) { }
                """ + additions + "}\n");
    }

    private static long lineOf(String source, String expression) {
        int offset = source.indexOf(expression);
        check(offset >= 0, "missing reference expression: " + expression);
        return 1 + source.substring(0, offset).chars().filter(c -> c == '\n').count();
    }

    private Path compile(String name, Map<String, String> sources, String classPath)
            throws IOException {
        Path output = root.resolve(name);
        Compilation result = compileInto(output, sources, classPath);
        check(result.success(), name + " must compile: " + result.errors());
        return output;
    }

    private record CompileError(String code, String source, long line) { }
    private record Compilation(boolean success, List<CompileError> errors) { }

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
                    .filter(d -> d.getKind() == Diagnostic.Kind.ERROR)
                    .map(d -> new CompileError(d.getCode(),
                            d.getSource() == null ? "" : d.getSource().getName(), d.getLineNumber())).toList());
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
