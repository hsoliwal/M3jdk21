/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */

/*
 * @test
 * @summary Preserve Object adaptation incompatibility witnesses and ordinary Runnable/Callable contracts
 * @run main/othervm/timeout=300 M3RuntimeCompatibilityHold
 */

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A passing witness means the documented compatibility boundary was detected.
 * Supply -Dm3.stock.jdk with an unmodified Java 21 JDK. No legacy class is compiled
 * with the candidate compiler before its frozen binary dispatch is observed.
 */
public class M3RuntimeCompatibilityHold {
    private static int checks;
    private static final String LEGACY_INTERFACE = """
            public interface LegacyAdapter {
                default <T> T toAdapter(Class<T> type) {
                    return type.cast("legacy-interface-default");
                }
            }
            """;
    private static final String LEGACY_DEFAULT_PROBE = """
            public final class LegacyDefaultProbe implements LegacyAdapter {
                public static void main(String[] args) {
                    LegacyAdapter receiver = new LegacyDefaultProbe();
                    Object result = receiver.toAdapter(Object.class);
                    if (args[0].equals("legacy")) {
                        if (!"legacy-interface-default".equals(result)) throw new AssertionError(result);
                        System.out.println("LEGACY_INTERFACE_DEFAULT");
                    } else {
                        if (result != receiver) throw new AssertionError(result);
                        System.out.println("OBJECT_METHOD_PREEMPTED_INTERFACE_DEFAULT");
                    }
                }
            }
            """;
    private static final String OPTIONAL_ADAPTER = """
            public final class OptionalAdapter {
                public <T> java.util.Optional<T> toAdapter(Class<T> type) {
                    return java.util.Optional.empty();
                }
            }
            """;
    private static final String OPTIONAL_PROBE = """
            public final class OptionalAdapterProbe {
                public static void main(String[] args) {
                    if (!new OptionalAdapter().toAdapter(String.class).isEmpty()) {
                        throw new AssertionError("legacy Optional result changed");
                    }
                    System.out.println("LEGACY_OPTIONAL_BINARY_PRESERVED");
                }
            }
            """;
    private static final String DUAL_PROBE = """
            import java.util.concurrent.*;
            public final class LegacyDualProbe implements Runnable, Callable<Integer> {
                int runs;
                public void run() { runs++; }
                public Integer call() { return 41; }
                public static void main(String[] args) throws Exception {
                    LegacyDualProbe task = new LegacyDualProbe();
                    ((Runnable) task).run();
                    if (task.runs != 1 || ((Callable<Integer>) task).call() != 41) {
                        throw new AssertionError("direct legacy dispatch changed");
                    }
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    try {
                        if (executor.submit((Callable<Integer>) task).get() != 41) {
                            throw new AssertionError("Callable result changed");
                        }
                        if (executor.submit((Runnable) task).get() != null || task.runs != 2) {
                            throw new AssertionError("Runnable result changed");
                        }
                        Exception expected = new Exception("checked-body");
                        Future<Integer> failed = executor.submit((Callable<Integer>) () -> { throw expected; });
                        try { failed.get(); throw new AssertionError("missing checked failure"); }
                        catch (ExecutionException observed) {
                            if (observed.getCause() != expected) throw new AssertionError("cause identity changed");
                        }
                    } finally {
                        executor.shutdownNow();
                        if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                            throw new AssertionError("executor did not terminate");
                        }
                    }
                    System.out.println("LEGACY_RUNNABLE_CALLABLE_PRESERVED");
                }
            }
            """;

    private record Result(int exit, String output) { }

    public static void main(String[] args) throws Exception {
        String stockProperty = System.getProperty("m3.stock.jdk");
        if (stockProperty == null || stockProperty.isBlank()) {
            throw new IllegalArgumentException("m3.stock.jdk must identify an unmodified Java 21 JDK");
        }
        Path stock = Path.of(stockProperty).toRealPath();
        Path current = Path.of(System.getProperty("java.home")).toRealPath();
        Path output = args.length == 0
                ? Files.createTempDirectory(Path.of(".").toAbsolutePath(), "m3-compatibility-hold-")
                : Files.createDirectory(Path.of(args[0]).toAbsolutePath());
        Result stockVersion = command(output, "stock-javac-version", executable(stock, "javac"), "-version");
        require(stockVersion.exit == 0 && stockVersion.output.startsWith("javac 21."), "stock compiler must be Java 21");
        Result stockObject = command(output, "stock-object-api", executable(stock, "javap"), "-p", "java.lang.Object");
        require(stockObject.exit == 0 && !stockObject.output.contains("toAdapter("), "baseline Object already has adaptation");
        require(Runtime.version().feature() == 21, "candidate runtime must be Java 21");
        directObjectContract();

        Path sources = Files.createDirectory(output.resolve("sources"));
        Path legacy = Files.createDirectory(output.resolve("legacy-classes"));
        Path currentClasses = Files.createDirectory(output.resolve("current-classes"));
        Path legacyInterface = source(sources, "LegacyAdapter.java", LEGACY_INTERFACE);
        Path legacyDefault = source(sources, "LegacyDefaultProbe.java", LEGACY_DEFAULT_PROBE);
        Path optional = source(sources, "OptionalAdapter.java", OPTIONAL_ADAPTER);
        Path optionalProbe = source(sources, "OptionalAdapterProbe.java", OPTIONAL_PROBE);
        Path dual = source(sources, "LegacyDualProbe.java", DUAL_PROBE);
        Result compiled = command(output, "legacy-compile", executable(stock, "javac"),
                "--release", "21", "-proc:none", "-Xlint:all", "-Werror", "-d", legacy.toString(),
                legacyInterface.toString(), legacyDefault.toString(), optional.toString(),
                optionalProbe.toString(), dual.toString());
        require(compiled.exit == 0, "stock legacy compilation failed: " + compiled.output);
        String frozen = classDigest(legacy);
        Result bytecode = command(output, "legacy-default-bytecode", executable(stock, "javap"),
                "-classpath", legacy.toString(), "-c", "LegacyDefaultProbe");
        require(bytecode.exit == 0 && bytecode.output.lines().anyMatch(line ->
                line.contains("invokeinterface") && line.contains("LegacyAdapter.toAdapter")),
                "baseline probe must exercise the original invokeinterface call site");

        expectRun(output, "legacy-default-stock", stock, legacy, "LegacyDefaultProbe", "legacy", "LEGACY_INTERFACE_DEFAULT");
        expectRun(output, "legacy-default-current", current, legacy, "LegacyDefaultProbe", "receiver",
                "OBJECT_METHOD_PREEMPTED_INTERFACE_DEFAULT");
        expectRun(output, "legacy-optional-stock", stock, legacy, "OptionalAdapterProbe", "", "LEGACY_OPTIONAL_BINARY_PRESERVED");
        expectRun(output, "legacy-optional-current", current, legacy, "OptionalAdapterProbe", "", "LEGACY_OPTIONAL_BINARY_PRESERVED");
        expectRun(output, "legacy-dual-stock", stock, legacy, "LegacyDualProbe", "", "LEGACY_RUNNABLE_CALLABLE_PRESERVED");
        expectRun(output, "legacy-dual-current", current, legacy, "LegacyDualProbe", "", "LEGACY_RUNNABLE_CALLABLE_PRESERVED");

        // Deliberately omit --release: the candidate compiler must inspect the actual candidate Object API.
        Result defaultSource = command(output, "default-source-current", executable(current, "javac"),
                "-proc:none", "-XDrawDiagnostics", "-d", currentClasses.toString(), legacyInterface.toString());
        require(defaultSource.exit != 0 && defaultSource.output.contains("compiler.err.default.overrides.object.member"),
                "expected Object/interface-default source incompatibility was not observed: " + defaultSource.output);
        Result optionalSource = command(output, "optional-source-current", executable(current, "javac"),
                "-proc:none", "-XDrawDiagnostics", "-d", currentClasses.toString(), optional.toString());
        require(optionalSource.exit != 0 && optionalSource.output.contains("compiler.err.override.incompatible.ret"),
                "expected Optional return-type source incompatibility was not observed: " + optionalSource.output);
        Result dualSource = command(output, "dual-source-current", executable(current, "javac"),
                "-proc:none", "-Xlint:all", "-Werror", "-d", currentClasses.toString(), dual.toString());
        require(dualSource.exit == 0, "current dual Runnable/Callable source must remain valid: " + dualSource.output);
        expectRun(output, "current-dual-current", current, currentClasses, "LegacyDualProbe", "", "LEGACY_RUNNABLE_CALLABLE_PRESERVED");
        require(frozen.equals(classDigest(legacy)), "legacy class bytes changed during candidate observations");
        Files.writeString(output.resolve("compatibility-receipt.tsv"),
                "gate\tresult\n"
                + "universal_no_break\tFAIL_COUNTEREXAMPLES_OBSERVED\n"
                + "legacy_interface_default_dispatch\tFAIL_OBSERVED\n"
                + "interface_default_source\tFAIL_OBSERVED\n"
                + "optional_return_source\tFAIL_OBSERVED\n"
                + "optional_return_binary\tPASS\n"
                + "Runnable_Callable_source_and_binary\tPASS\n"
                + "legacy_class_tree_sha256\t" + frozen + "\n"
                + "checks\t" + checks + "\n", StandardCharsets.UTF_8);
        System.out.println("M3_RUNTIME_COMPATIBILITY_HOLD_PASS checks=" + checks
                + " universal_no_break=FAIL_COUNTEREXAMPLES_OBSERVED evidence=" + output);
    }

    private static void directObjectContract() throws Exception {
        Method adapter = Object.class.getDeclaredMethod("toAdapter", Class.class);
        require(Modifier.isPublic(adapter.getModifiers()) && !Modifier.isFinal(adapter.getModifiers()),
                "Object adaptation must retain its current overridable public entry point");
        Object receiver = new Object();
        require(adapter.invoke(receiver, Object.class) == receiver, "assignable adaptation must return self");
        require(adapter.invoke(receiver, String.class) == null, "unsupported adaptation must return null");
        require(adapter.invoke(receiver, int.class) == null, "primitive Class must not match an Object");
        try {
            adapter.invoke(receiver, (Object) null);
            throw new AssertionError("null Class was accepted");
        } catch (InvocationTargetException failure) {
            require(failure.getCause() instanceof NullPointerException, "null Class must retain NPE");
        }
        long virtual = java.util.Arrays.stream(Object.class.getDeclaredMethods())
                .filter(method -> !Modifier.isStatic(method.getModifiers())
                        && !Modifier.isPrivate(method.getModifiers())
                        && !Modifier.isFinal(method.getModifiers())).count();
        require(virtual == 6, "Object virtual-method inventory no longer matches the sealed VM repair");
    }

    private static Path source(Path root, String name, String contents) throws IOException {
        return Files.writeString(root.resolve(name), contents, StandardCharsets.UTF_8);
    }

    private static String executable(Path jdk, String name) {
        return jdk.resolve("bin").resolve(name + (java.io.File.separatorChar == '\\' ? ".exe" : "")).toString();
    }

    private static Result command(Path output, String name, String... arguments) throws Exception {
        Path log = output.resolve(name + ".log");
        Process process = new ProcessBuilder(arguments).redirectErrorStream(true)
                .redirectOutput(log.toFile()).start();
        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new AssertionError("command timed out: " + name);
        }
        return new Result(process.exitValue(), Files.readString(log, StandardCharsets.UTF_8));
    }

    private static void expectRun(Path output, String name, Path jdk, Path classes,
                                  String main, String argument, String marker) throws Exception {
        Result result = command(output, name, executable(jdk, "java"), "-Xcheck:jni", "-cp",
                classes.toString(), main, argument);
        require(result.exit == 0 && result.output.contains(marker), name + " failed: " + result.output);
    }

    private static String classDigest(Path root) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        List<Path> paths;
        try (var files = Files.walk(root)) {
            paths = files.filter(Files::isRegularFile).sorted().toList();
        }
        for (Path path : paths) {
            digest.update(root.relativize(path).toString().replace('\\', '/').getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(Files.readAllBytes(path));
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
