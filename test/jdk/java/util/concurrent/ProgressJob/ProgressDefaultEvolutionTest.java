/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */

/*
 * @test
 * @summary Stock Java 21 task classfiles retain dispatch through progress defaults and the optional ProgressJob base
 * @modules java.compiler jdk.compiler
 * @run main/othervm ProgressDefaultEvolutionTest
 * @run main/othervm -Xint ProgressDefaultEvolutionTest
 */

import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.MonitoredCallable;
import java.util.concurrent.MonitoredRunnable;
import java.util.concurrent.ProgressJob;
import java.util.concurrent.ProgressMonitor;
import java.util.concurrent.ProgressMonitors;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

public class ProgressDefaultEvolutionTest {
    private static final Object RESULT = new Object();
    private static int assertions;

    private static final Map<String, String> LEGACY_SOURCES = Map.of(
            "legacy.progress.LegacyDual", """
                    package legacy.progress;
                    public final class LegacyDual implements Runnable,
                            java.util.concurrent.Callable<Object> {
                        private final Runnable body;
                        private final java.util.concurrent.Callable<Object> value;
                        public LegacyDual(Runnable body,
                                java.util.concurrent.Callable<Object> value) {
                            this.body = body;
                            this.value = value;
                        }
                        @Override public void run() { body.run(); }
                        @Override public Object call() throws Exception { return value.call(); }
                        public int run(int value) { return value + 1; }
                        public int call(int value) { return value + 2; }
                    }
                    """,
            "legacy.progress.LegacyClients", """
                    package legacy.progress;
                    public final class LegacyClients {
                        public static Runnable runnable(Runnable body) { return body::run; }
                        public static java.util.concurrent.Callable<Object> callable(
                                java.util.concurrent.Callable<Object> body) { return body::call; }
                        public static Object invoke(java.util.concurrent.Callable<?> task)
                                throws Exception { return task.call(); }
                        public static int overloads(LegacyDual task) {
                            java.util.function.IntUnaryOperator run = task::run;
                            java.util.function.IntUnaryOperator call = task::call;
                            return run.applyAsInt(7) + call.applyAsInt(11);
                        }
                    }
                    """,
            "legacy.progress.LegacyStringCallable", """
                    package legacy.progress;
                    public final class LegacyStringCallable
                            implements java.util.concurrent.Callable<String> {
                        private final String result;
                        public LegacyStringCallable(String result) { this.result = result; }
                        @Override public String call() { return result; }
                    }
                    """);

    // Exact classfiles produced by stock Temurin javac 21.0.13+9 from LEGACY_SOURCES
    // with --release 21 -Xlint:all -Werror -proc:none. Their canonical recipe proof
    // independently regenerates these bytes. A current M3JDK --release 21 compile
    // cannot stand in for an independently frozen stock-JDK binary baseline.
    private static final Map<String, String> STOCK_CLASSFILES = Map.of(
            "legacy.progress.LegacyClients",
                    "yv66vgAAAEEAVQoAAgADBwAEDAAFAAYBABBqYXZhL2xhbmcvT2JqZWN0AQAGPGluaXQ+AQADKClWCgAIAAkHAAoMAAsADAEAEWph" +
                    "dmEvdXRpbC9PYmplY3RzAQAOcmVxdWlyZU5vbk51bGwBACYoTGphdmEvbGFuZy9PYmplY3Q7KUxqYXZhL2xhbmcvT2JqZWN0OxIA" +
                    "AAAODAAPABABAANydW4BACooTGphdmEvbGFuZy9SdW5uYWJsZTspTGphdmEvbGFuZy9SdW5uYWJsZTsSAAEAEgwAEwAUAQAEY2Fs" +
                    "bAEAQChMamF2YS91dGlsL2NvbmN1cnJlbnQvQ2FsbGFibGU7KUxqYXZhL3V0aWwvY29uY3VycmVudC9DYWxsYWJsZTsLABYAFwcA" +
                    "GAwAEwAZAQAdamF2YS91dGlsL2NvbmN1cnJlbnQvQ2FsbGFibGUBABQoKUxqYXZhL2xhbmcvT2JqZWN0OxIAAgAbDAAcAB0BAAph" +
                    "cHBseUFzSW50AQBDKExsZWdhY3kvcHJvZ3Jlc3MvTGVnYWN5RHVhbDspTGphdmEvdXRpbC9mdW5jdGlvbi9JbnRVbmFyeU9wZXJh" +
                    "dG9yOxIAAwAbCwAgACEHACIMABwAIwEAI2phdmEvdXRpbC9mdW5jdGlvbi9JbnRVbmFyeU9wZXJhdG9yAQAEKEkpSQcAJQEAHWxl" +
                    "Z2FjeS9wcm9ncmVzcy9MZWdhY3lDbGllbnRzAQAEQ29kZQEAD0xpbmVOdW1iZXJUYWJsZQEACHJ1bm5hYmxlAQAIY2FsbGFibGUB" +
                    "AAlTaWduYXR1cmUBAGgoTGphdmEvdXRpbC9jb25jdXJyZW50L0NhbGxhYmxlPExqYXZhL2xhbmcvT2JqZWN0Oz47KUxqYXZhL3V0" +
                    "aWwvY29uY3VycmVudC9DYWxsYWJsZTxMamF2YS9sYW5nL09iamVjdDs+OwEABmludm9rZQEAMyhMamF2YS91dGlsL2NvbmN1cnJl" +
                    "bnQvQ2FsbGFibGU7KUxqYXZhL2xhbmcvT2JqZWN0OwEACkV4Y2VwdGlvbnMHADABABNqYXZhL2xhbmcvRXhjZXB0aW9uAQA2KExq" +
                    "YXZhL3V0aWwvY29uY3VycmVudC9DYWxsYWJsZTwqPjspTGphdmEvbGFuZy9PYmplY3Q7AQAJb3ZlcmxvYWRzAQAfKExsZWdhY3kv" +
                    "cHJvZ3Jlc3MvTGVnYWN5RHVhbDspSQEAClNvdXJjZUZpbGUBABJMZWdhY3lDbGllbnRzLmphdmEBABBCb290c3RyYXBNZXRob2Rz" +
                    "EAAGDwkAOQsAOgA7BwA8DAAPAAYBABJqYXZhL2xhbmcvUnVubmFibGUQABkPCQAVEAAjDwUAQQoAQgBDBwBEDAAPACMBABpsZWdh" +
                    "Y3kvcHJvZ3Jlc3MvTGVnYWN5RHVhbA8FAEYKAEIARwwAEwAjDwYASQoASgBLBwBMDABNAE4BACJqYXZhL2xhbmcvaW52b2tlL0xh" +
                    "bWJkYU1ldGFmYWN0b3J5AQALbWV0YWZhY3RvcnkBAMwoTGphdmEvbGFuZy9pbnZva2UvTWV0aG9kSGFuZGxlcyRMb29rdXA7TGph" +
                    "dmEvbGFuZy9TdHJpbmc7TGphdmEvbGFuZy9pbnZva2UvTWV0aG9kVHlwZTtMamF2YS9sYW5nL2ludm9rZS9NZXRob2RUeXBlO0xq" +
                    "YXZhL2xhbmcvaW52b2tlL01ldGhvZEhhbmRsZTtMamF2YS9sYW5nL2ludm9rZS9NZXRob2RUeXBlOylMamF2YS9sYW5nL2ludm9r" +
                    "ZS9DYWxsU2l0ZTsBAAxJbm5lckNsYXNzZXMHAFEBACVqYXZhL2xhbmcvaW52b2tlL01ldGhvZEhhbmRsZXMkTG9va3VwBwBTAQAe" +
                    "amF2YS9sYW5nL2ludm9rZS9NZXRob2RIYW5kbGVzAQAGTG9va3VwADEAJAACAAAAAAAFAAEABQAGAAEAJgAAAB0AAQABAAAABSq3" +
                    "AAGxAAAAAQAnAAAABgABAAAAAgAJACgAEAABACYAAAAkAAIAAQAAAAwqWbgAB1e6AA0AALAAAAABACcAAAAGAAEAAAADAAkAKQAU" +
                    "AAIAJgAAACQAAgABAAAADCpZuAAHV7oAEQAAsAAAAAEAJwAAAAYAAQAAAAUAKgAAAAIAKwAJACwALQADACYAAAAfAAEAAQAAAAcq" +
                    "uQAVAQCwAAAAAQAnAAAABgABAAAABwAuAAAABAABAC8AKgAAAAIAMQAJADIAMwABACYAAABKAAMAAwAAACoqWbgAB1e6ABoAAEwq" +
                    "WbgAB1e6AB4AAE0rEAe5AB8CACwQC7kAHwIAYKwAAAABACcAAAAOAAMAAAAJAAwACgAYAAsAAwA0AAAAAgA1ADYAAAAqAAQASAAD" +
                    "ADcAOAA3AEgAAwA9AD4APQBIAAMAPwBAAD8ASAADAD8ARQA/AE8AAAAKAAEAUABSAFQAGQ==",
            "legacy.progress.LegacyDual",
                    "yv66vgAAAEEAKQoAAgADBwAEDAAFAAYBABBqYXZhL2xhbmcvT2JqZWN0AQAGPGluaXQ+AQADKClWCQAIAAkHAAoMAAsADAEAGmxl" +
                    "Z2FjeS9wcm9ncmVzcy9MZWdhY3lEdWFsAQAEYm9keQEAFExqYXZhL2xhbmcvUnVubmFibGU7CQAIAA4MAA8AEAEABXZhbHVlAQAf" +
                    "TGphdmEvdXRpbC9jb25jdXJyZW50L0NhbGxhYmxlOwsAEgATBwAUDAAVAAYBABJqYXZhL2xhbmcvUnVubmFibGUBAANydW4LABcA" +
                    "GAcAGQwAGgAbAQAdamF2YS91dGlsL2NvbmN1cnJlbnQvQ2FsbGFibGUBAARjYWxsAQAUKClMamF2YS9sYW5nL09iamVjdDsBAAlT" +
                    "aWduYXR1cmUBADNMamF2YS91dGlsL2NvbmN1cnJlbnQvQ2FsbGFibGU8TGphdmEvbGFuZy9PYmplY3Q7PjsBADYoTGphdmEvbGFu" +
                    "Zy9SdW5uYWJsZTtMamF2YS91dGlsL2NvbmN1cnJlbnQvQ2FsbGFibGU7KVYBAARDb2RlAQAPTGluZU51bWJlclRhYmxlAQBKKExq" +
                    "YXZhL2xhbmcvUnVubmFibGU7TGphdmEvdXRpbC9jb25jdXJyZW50L0NhbGxhYmxlPExqYXZhL2xhbmcvT2JqZWN0Oz47KVYBAApF" +
                    "eGNlcHRpb25zBwAkAQATamF2YS9sYW5nL0V4Y2VwdGlvbgEABChJKUkBAFlMamF2YS9sYW5nL09iamVjdDtMamF2YS9sYW5nL1J1" +
                    "bm5hYmxlO0xqYXZhL3V0aWwvY29uY3VycmVudC9DYWxsYWJsZTxMamF2YS9sYW5nL09iamVjdDs+OwEAClNvdXJjZUZpbGUBAA9M" +
                    "ZWdhY3lEdWFsLmphdmEAMQAIAAIAAgASABcAAgASAAsADAAAABIADwAQAAEAHAAAAAIAHQAFAAEABQAeAAIAHwAAADMAAgADAAAA" +
                    "Dyq3AAEqK7UAByostQANsQAAAAEAIAAAABIABAAAAAcABAAIAAkACQAOAAoAHAAAAAIAIQABABUABgABAB8AAAAiAAEAAQAAAAoq" +
                    "tAAHuQARAQCxAAAAAQAgAAAABgABAAAACwABABoAGwACAB8AAAAiAAEAAQAAAAoqtAANuQAWAQCwAAAAAQAgAAAABgABAAAADAAi" +
                    "AAAABAABACMAAQAVACUAAQAfAAAAHAACAAIAAAAEGwRgrAAAAAEAIAAAAAYAAQAAAA0AAQAaACUAAQAfAAAAHAACAAIAAAAEGwVg" +
                    "rAAAAAEAIAAAAAYAAQAAAA4AAgAcAAAAAgAmACcAAAACACg=",
            "legacy.progress.LegacyStringCallable",
                    "yv66vgAAAEEAHgoAAgADBwAEDAAFAAYBABBqYXZhL2xhbmcvT2JqZWN0AQAGPGluaXQ+AQADKClWCQAIAAkHAAoMAAsADAEAJGxl" +
                    "Z2FjeS9wcm9ncmVzcy9MZWdhY3lTdHJpbmdDYWxsYWJsZQEABnJlc3VsdAEAEkxqYXZhL2xhbmcvU3RyaW5nOwoACAAODAAPABAB" +
                    "AARjYWxsAQAUKClMamF2YS9sYW5nL1N0cmluZzsHABIBAB1qYXZhL3V0aWwvY29uY3VycmVudC9DYWxsYWJsZQEAFShMamF2YS9s" +
                    "YW5nL1N0cmluZzspVgEABENvZGUBAA9MaW5lTnVtYmVyVGFibGUBABQoKUxqYXZhL2xhbmcvT2JqZWN0OwEACkV4Y2VwdGlvbnMH" +
                    "ABkBABNqYXZhL2xhbmcvRXhjZXB0aW9uAQAJU2lnbmF0dXJlAQBFTGphdmEvbGFuZy9PYmplY3Q7TGphdmEvdXRpbC9jb25jdXJy" +
                    "ZW50L0NhbGxhYmxlPExqYXZhL2xhbmcvU3RyaW5nOz47AQAKU291cmNlRmlsZQEAGUxlZ2FjeVN0cmluZ0NhbGxhYmxlLmphdmEA" +
                    "MQAIAAIAAQARAAEAEgALAAwAAAADAAEABQATAAEAFAAAACIAAgACAAAACiq3AAEqK7UAB7EAAAABABUAAAAGAAEAAAAFAAEADwAQ" +
                    "AAEAFAAAAB0AAQABAAAABSq0AAewAAAAAQAVAAAABgABAAAABhBBAA8AFgACABQAAAAdAAEAAQAAAAUqtgANsAAAAAEAFQAAAAYA" +
                    "AQAAAAIAFwAAAAQAAQAYAAIAGgAAAAIAGwAcAAAAAgAd");

    private static final Map<String, String> STOCK_SHA256 = Map.of(
            "legacy.progress.LegacyClients", "62bc873e73601cf22ecbe170f9de7ccdd9ad7ceca13b5970e52fc42461f4fbaf",
            "legacy.progress.LegacyDual", "0761065541464b1920220c37ad3af714a28a0b8ebc8a68ab3e46b2cd06a4a8f4",
            "legacy.progress.LegacyStringCallable", "4b2b40de95234b935bf5a19a3320a1fb3a3acd7ef458c17ab54e785599080edc");

    private static final String NEW_API_PROBE = """
            public class NewApiProbe {
                void invoke(Runnable runnable, java.util.concurrent.Callable<?> callable)
                        throws Exception {
                    runnable.run(null);
                    callable.call(null);
                }
            }
            """;

    @FunctionalInterface
    private interface Action {
        void run() throws Exception;
    }

    private static final class Probe implements ProgressMonitor {
        final List<String> events = new ArrayList<>();
        final CancellationException cancellation = new CancellationException("probe");
        int checks;
        int cancelAt;
        int completions;

        @Override public void checkCancelled() {
            require(ProgressMonitors.current() == this, "check sees borrowed monitor");
            events.add("check");
            if (++checks == cancelAt) throw cancellation;
            ProgressMonitor.super.checkCancelled();
        }

        @Override public void done() { completions++; }
    }

    private record Compilation(boolean success, List<String> diagnostics, String output) { }

    public static void main(String[] args) throws Exception {
        require(!Thread.currentThread().isInterrupted(), "uninterrupted test entry");
        Path root = Files.createTempDirectory(Path.of("."), "progress-default-evolution-");
        Path stock = root.resolve("stock-classes");
        writeStockClients(stock);
        Map<String, String> frozen = classHashes(stock);
        require(frozen.size() == 3, "freeze all three stock client classfiles");

        Path runtimeApi = root.resolve("runtime-api");
        copyRuntimeApi(runtimeApi);
        List<String> actualApi = List.of("-source", "21", "-target", "21",
                "--patch-module", "java.base=" + runtimeApi.toAbsolutePath());
        Compilation present = compile(root.resolve("current-probe-source"),
                root.resolve("current-probe-classes"), Map.of("NewApiProbe", NEW_API_PROBE),
                actualApi);
        require(present.success(), "exact runtime API supplies progress overloads: " + present);

        signatures();
        exercise(stock);
        require(classHashes(stock).equals(frozen), "all old classfiles remain byte-identical");

        Path recompiled = root.resolve("recompiled-classes");
        Compilation unchanged = compile(root.resolve("recompiled-source"), recompiled,
                LEGACY_SOURCES, actualApi);
        require(unchanged.success(), "unchanged old source compiles against runtime API: " + unchanged);
        exercise(recompiled);
        require(classHashes(stock).equals(frozen), "source recompilation never replaces old classfiles");
        System.out.println("PROGRESS_DEFAULT_EVOLUTION_PASS oldClassfiles=" + frozen.size()
                + " assertions=" + assertions);
    }

    private static void writeStockClients(Path root) throws Exception {
        require(STOCK_CLASSFILES.keySet().equals(LEGACY_SOURCES.keySet()),
                "frozen binaries and unchanged source identify the same clients");
        require(STOCK_SHA256.keySet().equals(STOCK_CLASSFILES.keySet()),
                "every frozen binary has an independent expected digest");
        for (var entry : new TreeMap<>(STOCK_CLASSFILES).entrySet()) {
            byte[] bytes = Base64.getDecoder().decode(entry.getValue());
            require(bytes.length > 8 && ((bytes[6] & 255) << 8 | bytes[7] & 255) == 65,
                    "frozen classfile uses Java 21 version: " + entry.getKey());
            require(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))
                    .equals(STOCK_SHA256.get(entry.getKey())),
                    "frozen stock classfile digest: " + entry.getKey());
            Path path = root.resolve(entry.getKey().replace('.', '/') + ".class");
            Files.createDirectories(path.getParent());
            Files.write(path, bytes);
        }
    }

    private static void signatures() throws Exception {
        for (Class<?> type : List.of(Runnable.class, Callable.class,
                MonitoredRunnable.class, MonitoredCallable.class)) {
            long count = Arrays.stream(type.getMethods())
                    .filter(method -> Modifier.isAbstract(method.getModifiers())).count();
            require(count == 1, "single abstract contract retained for " + type.getName());
        }
        require(Runnable.class.getMethod("run", ProgressMonitor.class).isDefault(),
                "Runnable progress overload is a default");
        require(Callable.class.getMethod("call", ProgressMonitor.class).isDefault(),
                "Callable progress overload is a default");
        require(Modifier.isAbstract(ProgressJob.class.getModifiers()), "ProgressJob stays optional base");
    }

    private static void exercise(Path clients) throws Exception {
        ProgressMonitor previous = ProgressMonitors.current();
        try (URLClassLoader loader = new URLClassLoader(
                new java.net.URL[] {clients.toUri().toURL()},
                ProgressDefaultEvolutionTest.class.getClassLoader())) {
            Class<?> dualType = loader.loadClass("legacy.progress.LegacyDual");
            Class<?> clientType = loader.loadClass("legacy.progress.LegacyClients");
            require(dualType.getClassLoader() == loader, "load frozen implementation from its own directory");
            Probe outer = new Probe();
            final ProgressMonitors.Scope scope = ProgressMonitors.bind(outer);
            try (scope) {
                oldAndNewDispatch(dualType, clientType, outer);
                oldLambdas(clientType, outer);
                failures(dualType, outer);
                genericBridge(loader, outer);
                skeletalBase(clientType, outer);
            }
            require(ProgressMonitors.current() == previous, "restore caller scope after clients");
            require(outer.completions == 0, "old clients never complete borrowed outer monitor");
        }
    }

    private static Object dual(Class<?> type, Runnable runnable, Callable<Object> callable)
            throws Exception {
        return type.getConstructor(Runnable.class, Callable.class).newInstance(runnable, callable);
    }

    private static void oldAndNewDispatch(Class<?> dualType, Class<?> clientType, Probe outer)
            throws Exception {
        Probe supplied = new Probe();
        Object implementation = dual(dualType,
                () -> supplied.events.add("run"),
                () -> { supplied.events.add("call"); return RESULT; });
        Runnable runnable = (Runnable) implementation;
        Callable<?> callable = (Callable<?>) implementation;

        require(runnable.progressMonitor() == outer,
                "old dual implementation inherits one unambiguous ambient accessor");
        runnable.run();
        require(callable.call() == RESULT, "old no-argument call retains result identity");
        events(supplied, "run", "call");
        require(supplied.checks == 0, "old operations acquire no implicit cancellation checks");

        supplied.events.clear();
        runnable.run(supplied);
        events(supplied, "check", "run", "check");
        require(ProgressMonitors.current() == outer, "new Runnable overload restores old outer scope");
        supplied.events.clear();
        require(callable.call(supplied) == RESULT, "new Callable overload keeps original result");
        events(supplied, "check", "call", "check");
        require(ProgressMonitors.current() == outer, "new Callable overload restores old outer scope");
        require(supplied.completions == 0, "new defaults borrow monitor lifecycle");

        require(clientType.getMethod("overloads", dualType).invoke(null, implementation).equals(21),
                "old bound references retain int overload selection");
    }

    private static void oldLambdas(Class<?> clients, Probe outer) throws Exception {
        Probe supplied = new Probe();
        Runnable body = () -> {
            require(ProgressMonitors.current() == supplied, "old Runnable lambda receives supplied scope");
            supplied.events.add("lambda-run");
        };
        Runnable runnable = (Runnable) clients.getMethod("runnable", Runnable.class).invoke(null, body);
        runnable.run(supplied);
        events(supplied, "check", "lambda-run", "check");
        supplied.events.clear();
        Callable<Object> bodyCall = () -> {
            require(ProgressMonitors.current() == supplied, "old Callable lambda receives supplied scope");
            supplied.events.add("lambda-call");
            return RESULT;
        };
        Callable<?> callable = (Callable<?>) clients.getMethod("callable", Callable.class)
                .invoke(null, bodyCall);
        require(callable.call(supplied) == RESULT, "old lambda result identity");
        events(supplied, "check", "lambda-call", "check");
        require(ProgressMonitors.current() == outer, "old lambdas restore outer scope");
        require(supplied.completions == 0, "old lambdas retain borrowed ownership");
    }

    private static void failures(Class<?> dualType, Probe outer) throws Exception {
        Probe supplied = new Probe();
        Exception checked = new Exception("original checked failure");
        RuntimeException unchecked = new IllegalStateException("original unchecked failure");
        Object implementation = dual(dualType,
                () -> { supplied.events.add("run"); throw unchecked; },
                () -> { supplied.events.add("call"); throw checked; });
        Runnable runnable = (Runnable) implementation;
        Callable<?> callable = (Callable<?>) implementation;
        require(expect(RuntimeException.class, () -> runnable.run(supplied)) == unchecked,
                "old Runnable failure identity");
        events(supplied, "check", "run");
        supplied.events.clear();
        require(expect(Exception.class, () -> callable.call(supplied)) == checked,
                "old Callable checked failure identity");
        events(supplied, "check", "call");
        require(ProgressMonitors.current() == outer, "old failures restore outer scope");

        supplied.events.clear();
        supplied.cancelAt = supplied.checks + 1;
        require(expect(CancellationException.class, () -> runnable.run(supplied)) == supplied.cancellation,
                "pre-cancellation identity through old implementation");
        events(supplied, "check");
        supplied.events.clear();
        expect(NullPointerException.class, () -> callable.call(null));
        require(supplied.events.isEmpty(), "null monitor rejects before invoking old body");
        require(ProgressMonitors.current() == outer, "validation and cancellation restore outer scope");
        require(supplied.completions == 0, "failure paths never complete borrowed monitor");
    }

    private static void genericBridge(ClassLoader loader, Probe outer) throws Exception {
        Class<?> type = loader.loadClass("legacy.progress.LegacyStringCallable");
        require(Arrays.stream(type.getDeclaredMethods()).anyMatch(method -> method.isBridge()),
                "stock compiler emitted covariant Callable bridge");
        String value = new String(new char[] {'v', 'a', 'l', 'u', 'e'});
        Callable<?> task = (Callable<?>) type.getConstructor(String.class).newInstance(value);
        Probe supplied = new Probe();
        require(task.call(supplied) == value, "new default dispatches through old bridge unchanged");
        events(supplied, "check", "check");
        require(ProgressMonitors.current() == outer, "bridge dispatch restores outer scope");
    }

    private static void skeletalBase(Class<?> clients, Probe outer) throws Exception {
        Probe supplied = new Probe();
        ProgressJob<Object> job = new ProgressJob<>() {
            @Override protected Object run(ProgressMonitor monitor) {
                require(monitor == supplied, "protected hook receives selected monitor");
                require(ProgressMonitors.current() == supplied, "protected hook sees same lexical scope");
                supplied.events.add("hook");
                return RESULT;
            }
        };
        final ProgressMonitors.Scope scope = ProgressMonitors.bind(supplied);
        try (scope) {
            require(invokeOldCallable(clients, job) == RESULT,
                    "stock-compiled Callable client can use optional skeletal base");
        }
        events(supplied, "check", "hook", "check");
        supplied.events.clear();
        require(job.call(supplied) == RESULT, "inherited overload preserves protected hook result");
        events(supplied, "check", "check", "hook", "check", "check");
        require(ProgressMonitors.current() == outer, "skeletal template restores outer scope");
        require(supplied.completions == 0, "skeletal template borrows lifecycle");

        Exception marker = new Exception("protected hook failure");
        ProgressJob<Object> failing = new ProgressJob<>() {
            @Override protected Object run(ProgressMonitor monitor) throws Exception { throw marker; }
        };
        require(expect(Exception.class, () -> invokeOldCallable(clients, failing)) == marker,
                "old Callable client preserves protected-hook checked failure identity");
    }

    private static Object invokeOldCallable(Class<?> clients, Callable<?> task) throws Exception {
        try {
            return clients.getMethod("invoke", Callable.class).invoke(null, task);
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof Exception exception) throw exception;
            if (cause instanceof Error error) throw error;
            throw new AssertionError(cause);
        }
    }

    private static Compilation compile(Path sources, Path output, Map<String, String> source,
            List<String> platform) throws Exception {
        Files.createDirectories(output);
        List<Path> paths = new ArrayList<>();
        for (var entry : new TreeMap<>(source).entrySet()) {
            Path path = sources.resolve(entry.getKey().replace('.', '/') + ".java");
            Files.createDirectories(path.getParent());
            Files.writeString(path, entry.getValue(), StandardCharsets.UTF_8);
            paths.add(path);
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        require(compiler != null, "compiler is available");
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StringWriter text = new StringWriter();
        List<String> options = new ArrayList<>(platform);
        options.addAll(List.of("-Xlint:all", "-Werror", "-proc:none", "-d", output.toString()));
        boolean success;
        try (var manager = compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
            success = compiler.getTask(text, manager, diagnostics, options, null,
                    manager.getJavaFileObjectsFromPaths(paths)).call();
        }
        return new Compilation(success,
                diagnostics.getDiagnostics().stream().map(item -> item.getCode()).toList(),
                text.toString());
    }

    private static void copyRuntimeApi(Path root) throws Exception {
        for (Class<?> type : List.of(Runnable.class, Callable.class, ProgressMonitor.class,
                ProgressMonitors.class, MonitoredRunnable.class, MonitoredCallable.class,
                ProgressJob.class)) {
            String name = type.getName().replace('.', '/') + ".class";
            Path path = root.resolve(name);
            Files.createDirectories(path.getParent());
            try (InputStream input = type.getResourceAsStream("/" + name)) {
                require(input != null, "actual runtime class bytes available: " + name);
                Files.write(path, input.readAllBytes());
            }
        }
    }

    private static Map<String, String> classHashes(Path root) throws Exception {
        Map<String, String> result = new TreeMap<>();
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".class")).sorted().toList()) {
                result.put(root.relativize(path).toString(), HexFormat.of().formatHex(
                        MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))));
            }
        }
        return Map.copyOf(result);
    }

    private static <T extends Throwable> T expect(Class<T> type, Action action) throws Exception {
        try {
            action.run();
        } catch (Throwable failure) {
            require(type.isInstance(failure), "expected " + type.getName() + ": " + failure);
            return type.cast(failure);
        }
        throw new AssertionError("expected " + type.getName());
    }

    private static void events(Probe probe, String... expected) {
        require(probe.events.equals(List.of(expected)),
                "callback order expected " + List.of(expected) + ", got " + probe.events);
    }

    private static void require(boolean condition, String description) {
        assertions++;
        if (!condition) throw new AssertionError(description);
    }
}
