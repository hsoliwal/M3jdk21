/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */

/* @test
 * @summary Build facts share the original M3AST analysis event and remain explicitly partial
 * @modules jdk.compiler/com.sun.source.util
 * @run main M3ASTBuildFactsTest
 */

import com.sun.source.util.JavacTask;
import com.sun.source.util.M3AST;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/*
 * Adapted 2026-10-21 by Synexia (Hitesh Soliwal and contributors) from
 * M3ASTSharedFrontendTest fixture helpers to verify partial build facts,
 * parser seals, shared events and ordinary compiler-output parity.
 */
public class M3ASTBuildFactsTest {
    private static int checks;
    private static Path work;

    public static void main(String[] args) throws Exception {
        work = Files.createTempDirectory("m3-build-facts-");
        sharedFactsAndBytecode();
        sealedParserGeneration();
        failedCompilationStaysPartial();
        valueBoundaries();
        System.out.println("M3_AST_BUILD_FACTS checks=" + checks);
    }

    private static final class Source extends SimpleJavaFileObject {
        String text;
        int reads;

        Source(String name, String text) {
            super(URI.create("string:///" + name + ".java"), Kind.SOURCE);
            this.text = text;
        }

        @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            reads++;
            return new StringBuilder(text);
        }
    }

    private static List<Source> sources(int offset) {
        return List.of(
                new Source("deps/Constants", """
                        package deps;
                        public class Constants {
                            public static final int VALUE = 7;
                        }
                        """),
                new Source("deps/Base", """
                        package deps;
                        public class Base {
                            public int inherited = 3;
                            public static int staticValue() { return 1; }
                            public int instanceValue() { return 2; }
                        }
                        """),
                new Source("sample/Client", """
                        package sample;
                        import java.util.List;
                        import java.util.function.IntSupplier;
                        import static deps.Constants.VALUE;
                        public class Client extends deps.Base {
                            public static class Nested { }
                            public static int answer() {
                                IntSupplier reference = deps.Base::staticValue;
                                class Local { int number() { return VALUE; } }
                                Runnable anonymous = new Runnable() {
                                    @Override public void run() { }
                                };
                                List<String> labels = List.of("phantom.NotAType");
                                java.math.BigInteger[][] matrix = new java.math.BigInteger[1][0];
                                int[] primitives = { 1 };
                                int lengths = matrix.length + primitives.length;
                                Object arrayCopy = matrix.clone();
                                // phantom.CommentType is text, not a symbol reference.
                                return VALUE + %d;
                            }
                            public int member() { return instanceValue() + inherited; }
                        }
                        class Sibling { }
                        """.formatted(offset)));
    }

    private record Compilation(Path output, Map<String, M3AST.BuildFacts> facts,
                               M3AST.Summary summary, boolean successful, int parses) { }

    private static Compilation compile(String name, List<Source> sources, boolean select,
                                       Runnable afterParse) throws Exception {
        Path output = Files.createDirectory(work.resolve(name));
        var compiler = ToolProvider.getSystemJavaCompiler();
        check(compiler != null, "the running image owns a compiler");
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        Map<String, M3AST.BuildFacts> facts = new HashMap<>();
        AtomicInteger parses = new AtomicInteger();
        M3AST.Unit[] escaped = new M3AST.Unit[1];
        try (StandardJavaFileManager files = compiler.getStandardFileManager(diagnostics, null, null)) {
            JavacTask task = (JavacTask) compiler.getTask(null, files, diagnostics,
                    List.of("-g", "-proc:none", "-implicit:none", "-d", output.toString()),
                    null, sources);
            task.addTaskListener(new TaskListener() {
                @Override public void finished(TaskEvent event) {
                    if (event.getKind() == TaskEvent.Kind.PARSE) parses.incrementAndGet();
                }
            });
            M3AST session = select ? M3AST.open(task) : null;
            if (select) {
                session.addConsumer(new M3AST.Consumer() {
                    @Override public void parsed(M3AST.Unit unit) throws Exception {
                        expect(IllegalStateException.class, () -> M3AST.buildFacts(unit));
                        if (afterParse != null) afterParse.run();
                    }

                    @Override public void analyzed(M3AST.Unit unit) throws Exception {
                        M3AST.BuildFacts value = M3AST.buildFacts(unit);
                        check(facts.put(value.analyzedBinaryName(), value) == null,
                                "one fact slice per analyzed top-level type");
                        check(!value.completeDependencyGraph(), "successful analysis cannot certify completeness");
                        check(CompletableFuture.supplyAsync(() -> {
                            try { M3AST.buildFacts(unit); return false; }
                            catch (IllegalStateException expected) { return true; }
                            catch (Exception unexpected) { throw new AssertionError(unexpected); }
                        }).join(), "cross-thread build projection refused");
                        escaped[0] = unit;
                    }
                });
                session.addConsumer(new M3AST.Consumer() {
                    @Override public void analyzed(M3AST.Unit unit) throws Exception {
                        M3AST.BuildFacts value = M3AST.buildFacts(unit);
                        check(value == facts.get(value.analyzedBinaryName()),
                                "consumers share the identical singleton projection result");
                    }
                });
            }
            boolean successful = task.call();
            M3AST.Summary summary = select ? session.summary() : null;
            if (select) {
                check(summary.closed() && summary.completed() && !summary.consumerFailed(),
                        "compilation closes the owning session without consumer failure");
                check(summary.projectionComputations() == summary.analyzedTypes(),
                        "exactly one computation per analyzed event");
                check(summary.projectionHits() == summary.analyzedTypes(),
                        "exactly one shared hit per analyzed event");
                check(summary.sourceReads() == 0, "build facts never reread a source");
                check(summary.parsedUnits() == parses.get() && parses.get() == sources.size(),
                        "the original compiler performs one parse per input");
                check(summary.capturedSources() == sources.size(), "one parser-owned seal per input");
                if (escaped[0] != null)
                    expect(IllegalStateException.class, () -> M3AST.buildFacts(escaped[0]));
                session.close();
            }
            for (Source source : sources) check(source.reads == 1, "one source read by the original parser");
            return new Compilation(output, Map.copyOf(facts), summary, successful, parses.get());
        }
    }

    private static void sharedFactsAndBytecode() throws Exception {
        List<Source> input = sources(0);
        Compilation selected = compile("selected", input, true, null);
        Compilation ordinary = compile("ordinary", sources(0), false, null);
        check(selected.successful() && ordinary.successful(), "both original compiler tasks succeed");
        check(selected.summary().compilerErrors() == 0 && selected.facts().size() == 4,
                "three sources produce four top-level analysis slices");
        M3AST.BuildFacts client = selected.facts().get("sample.Client");
        check(client != null, "client fact slice exists");
        check(client.sourceUri().equals(input.get(2).toUri())
                        && client.sourceLength() == input.get(2).text.length()
                        && client.sourceUtf16Sha256().equals(utf16Sha256(input.get(2).text)),
                "facts carry the exact original parser generation");
        String[] declared = client.declaredBinaryNames();
        check(contains(declared, "sample.Client") && contains(declared, "sample.Client$Nested"),
                "top-level and member declarations use binary names");
        check(Arrays.stream(declared).anyMatch(value -> value.matches("sample\\.Client\\$[0-9]+Local"))
                        && Arrays.stream(declared).anyMatch(value -> value.matches("sample\\.Client\\$[0-9]+")),
                "local and anonymous declarations are captured from their actual symbols");
        check(!contains(declared, "sample.Sibling")
                        && Arrays.equals(selected.facts().get("sample.Sibling").declaredBinaryNames(),
                                         new String[] { "sample.Sibling" }),
                "other top-level events do not leak into this declaration slice");
        String[] referenced = client.referencedBinaryNames();
        check(contains(referenced, "deps.Constants"), "inlined constant retains its declaring-type dependency");
        check(contains(referenced, "deps.Base"), "static, instance and inherited member owners are captured");
        check(contains(referenced, "java.util.List") && contains(referenced, "java.lang.String")
                        && contains(referenced, "java.util.function.IntSupplier")
                        && contains(referenced, "java.lang.Runnable"),
                "attributed type syntax and method-reference types are captured");
        check(contains(referenced, "java.math.BigInteger"),
                "real multi-dimensional array component classes remain referenced");
        for (String sentinel : List.of("int", "void", "Method", "Array", "Bound", "<nulltype>"))
            check(!contains(referenced, sentinel),
                    "javac primitive/method/array sentinel is not a binary type: " + sentinel);
        check(!contains(referenced, "phantom.NotAType") && !contains(referenced, "phantom.CommentType"),
                "text decoys create no symbol dependencies");
        for (String name : declared)
            check(!contains(referenced, name), "same-event declarations are excluded from external references");
        canonical(declared);
        canonical(referenced);
        declared[0] = "mutated.Declaration";
        referenced[0] = "mutated.Reference";
        check(!contains(client.declaredBinaryNames(), declared[0])
                        && !contains(client.referencedBinaryNames(), referenced[0]),
                "returned arrays cannot mutate retained facts");
        Map<String, byte[]> selectedBytes = classes(selected.output());
        Map<String, byte[]> ordinaryBytes = classes(ordinary.output());
        check(selectedBytes.keySet().equals(ordinaryBytes.keySet()), "projection preserves emitted class inventory");
        for (String name : selectedBytes.keySet())
            check(Arrays.equals(selectedBytes.get(name), ordinaryBytes.get(name)),
                    "projection preserves ordinary compiler bytes: " + name);
        check(answer(selected.output(), "sample.Client") == 7, "emitted selected code executes");

        Compilation changed = compile("changed", sources(1), true, null);
        M3AST.BuildFacts next = changed.facts().get("sample.Client");
        check(changed.successful() && next != client
                        && !next.sourceUtf16Sha256().equals(client.sourceUtf16Sha256()),
                "new source generation and compiler task have distinct fact identity");
        check(answer(changed.output(), "sample.Client") == 8, "changed generation emits changed behavior");
    }

    private static void sealedParserGeneration() throws Exception {
        String original = "package sample; public class Sealed { public static int answer() { return 9; } }";
        Source source = new Source("sample/Sealed", original);
        Compilation result = compile("sealed", List.of(source), true,
                () -> source.text = original.replace("return 9", "return 10"));
        M3AST.BuildFacts facts = result.facts().get("sample.Sealed");
        check(result.successful() && facts.sourceLength() == original.length()
                        && facts.sourceUtf16Sha256().equals(utf16Sha256(original)),
                "source mutation after parsing cannot replace the compiler-owned source seal");
        check(answer(result.output(), "sample.Sealed") == 9,
                "facts describe the generation actually compiled, not current source text");
    }

    private static void failedCompilationStaysPartial() throws Exception {
        Compilation broken = compile("broken", List.of(new Source("Broken",
                "class Broken { Missing value; }")), true, null);
        check(!broken.successful() && broken.summary().compilerErrors() > 0,
                "unresolved input remains an unsuccessful original compilation");
        M3AST.BuildFacts facts = broken.facts().get("Broken");
        check(facts != null && facts.hasUnresolvedSymbols() && !facts.completeDependencyGraph(),
                "error-type observation never promotes partial facts to a completeness certificate");
    }

    private static void valueBoundaries() throws Exception {
        URI uri = URI.create("string:///Boundary.java");
        String hash = utf16Sha256("class Boundary {}");
        String[] names = { "Boundary" };
        M3AST.BuildFacts value = new M3AST.BuildFacts(uri, 17, hash, "Boundary",
                names, new String[0], false);
        names[0] = "changed";
        check(contains(value.declaredBinaryNames(), "Boundary") && !value.completeDependencyGraph(),
                "constructor detaches arrays and cannot claim complete coverage");
        expect(IllegalArgumentException.class, () -> new M3AST.BuildFacts(uri, -1, hash,
                "Boundary", new String[0], new String[0], false));
        expect(IllegalArgumentException.class, () -> new M3AST.BuildFacts(uri, 17, "not-a-seal",
                "Boundary", new String[0], new String[0], false));
        expect(IllegalArgumentException.class, () -> new M3AST.BuildFacts(uri, 17, hash,
                "Boundary", new String[] { "Z", "A" }, new String[0], false));
        expect(IllegalArgumentException.class, () -> new M3AST.BuildFacts(uri, 17, hash,
                "Boundary", new String[] { "A", "A" }, new String[0], false));
        expect(NullPointerException.class, () -> M3AST.buildFacts(null));
    }

    private static Map<String, byte[]> classes(Path root) throws IOException {
        Map<String, byte[]> result = new HashMap<>();
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(value -> value.toString().endsWith(".class")).toList())
                result.put(root.relativize(path).toString(), Files.readAllBytes(path));
        }
        return result;
    }

    private static int answer(Path output, String name) throws Exception {
        try (URLClassLoader loader = new URLClassLoader(new URL[] { output.toUri().toURL() },
                                                       ClassLoader.getPlatformClassLoader())) {
            return (Integer) loader.loadClass(name).getMethod("answer").invoke(null);
        }
    }

    private static String utf16Sha256(String text) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (int index = 0; index < text.length(); index++) {
            char value = text.charAt(index);
            digest.update((byte) (value >>> 8));
            digest.update((byte) value);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static boolean contains(String[] values, String name) {
        return Arrays.binarySearch(values, name) >= 0;
    }

    private static void canonical(String[] values) {
        for (int index = 1; index < values.length; index++)
            check(values[index - 1].compareTo(values[index]) < 0, "name lanes are sorted and unique");
    }

    @FunctionalInterface
    private interface Action { void run() throws Exception; }

    private static <T extends Throwable> T expect(Class<T> type, Action action) throws Exception {
        try {
            action.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) {
                checks++;
                return type.cast(failure);
            }
            throw new AssertionError("expected " + type.getName() + ", got " + failure, failure);
        }
        throw new AssertionError("expected " + type.getName());
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        checks++;
    }
}
