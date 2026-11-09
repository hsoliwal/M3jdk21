// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package jdk.m3.collections.sortable;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Real javac annotation-processing, generated-class, and refusal oracle. */
public final class SortableProcessorSelfTest {
    private static int checks;
    private SortableProcessorSelfTest() { }

    public static void main(String[] args) throws Exception {
        M3CompilerFactsContract.main(new String[0]);
        String header = "package demo; import jdk.m3.collections.sortable.Sortable; "
                + "@Sortable(signal=\"absoluteWeight\") public class Person {"
                + "public int age; public java.util.List<String> tags; "
                + "public double absoluteWeight(){return age*0.5;} ";
        String method = "public String describe(){return String.valueOf(age);}";
        String footer = "}";
        try (Compiled a = compile("demo.Person", header + method + footer, true);
                Compiled b = compile("demo.Person", header.replace("public double absoluteWeight(){return age*0.5;} ", "")
                        + method + "public double absoluteWeight(){return age*0.5;}" + footer, true)) {
            check(a.ok && b.ok, "valid compilations: " + a.messages + b.messages);
            check(a.generated.equals(b.generated), "source order must not change structural metadata");
            Class<?> person = a.loader.loadClass("demo.Person");
            Class<?> projection = a.loader.loadClass("demo.PersonM3Sortable");
            Object p = person.getConstructor().newInstance();
            Field age = person.getField("age");
            age.setInt(p, 17);
            Method signal = projection.getMethod("absoluteSignal", person);
            check(((Double) signal.invoke(null, p)) == 8.5, "exact domain-provided double accessor");
            Object values = java.lang.reflect.Array.newInstance(person, 2);
            java.lang.reflect.Array.set(values, 0, p);
            Object q = person.getConstructor().newInstance();
            age.setInt(q, 9);
            java.lang.reflect.Array.set(values, 1, q);
            Method signals = projection.getMethod("absoluteSignals", values.getClass());
            check(Arrays.equals(new double[]{8.5, 4.5}, (double[]) signals.invoke(null, values)),
                    "precompute each signal from the source array exactly once");
            long[][] lanes = (long[][]) projection.getMethod("structuralSignals").invoke(null);
            check(lanes.length == 6, "six source-time structural dimensions");
            check(lanes[0].length == 1 && lanes[1].length == 1, "package and class tokens");
            check(lanes[2].length >= 2 && lanes[3].length >= 2, "field and method tokens");
            check(lanes[4].length >= 2 && lanes[5].length >= 1, "dependency and resolved-call tokens");
            long[][] fresh = (long[][]) projection.getMethod("structuralSignals").invoke(null);
            check(StructuralSignals.weightedDistance(lanes, fresh, new double[]{1,1,1,1,1,1}) == 0.0,
                    "identical structure has zero projected distance");
            M3CompilerFacts compiler = (M3CompilerFacts)
                    projection.getMethod("compilerFacts").invoke(null);
            check(compiler.known(M3CompilerFacts.FIELDS)
                    && compiler.count(M3CompilerFacts.FIELDS) == 2,
                    "two model-declared fields, not runtime reflection");
            check(compiler.known(M3CompilerFacts.METHODS)
                    && compiler.count(M3CompilerFacts.METHODS) == 2,
                    "two compiler-declared methods");
            check(compiler.known(M3CompilerFacts.METHOD_INVOCATIONS)
                    && compiler.count(M3CompilerFacts.METHOD_INVOCATIONS) >= 1,
                    "javac Trees invocation sites source-time");
            check(compiler.known(M3CompilerFacts.DISTINCT_RESOLVED_CALLEE_TOKENS)
                    && compiler.count(M3CompilerFacts.DISTINCT_RESOLVED_CALLEE_TOKENS) >= 1,
                    "resolved-call hints retained as token counts");
            check(compiler.origin(M3CompilerFacts.FIELDS)
                    == M3CompilerFacts.Origin.COMPILER_MODEL,
                    "model provenance retained");
            check(compiler.origin(M3CompilerFacts.METHOD_INVOCATIONS)
                    == M3CompilerFacts.Origin.COMPILER_AST,
                    "AST provenance retained");
            check(!compiler.known(M3CompilerFacts.INCOMING_CALLS)
                    && !compiler.known(M3CompilerFacts.RUNTIME_INVOCATIONS),
                    "unavailable caller graph and runtime frequency never invented as zero");
            int[] changedFacts = compiler.counts();
            changedFacts[M3CompilerFacts.FIELDS] = 123456;
            check(compiler.count(M3CompilerFacts.FIELDS) == 2,
                    "generated primitive fact image is immutable");
            M3MachineHintRules compilerHints = (M3MachineHintRules)
                    projection.getMethod("machinePatternHints").invoke(null);
            check(compilerHints.state(M3MachineHintRules.FIELD_METHOD_RELATION)
                    == M3MachineHintRules.State.CANDIDATE, "javac-generated field/method hint");
            check(compilerHints.state(M3MachineHintRules.INTERFACE_INVOCATION)
                    == M3MachineHintRules.State.NOT_OBSERVED, "no invented interface pattern");
            check(!compilerHints.nativeLoweringAuthority(), "compile-time hints are review-only");
            check(a.generated.contains("MACHINE_HINT_CANDIDATES")
                    && a.generated.contains("machinePatternHints()"),
                    "source companion embeds compile-time literal masks");
            M3CompilerFacts compilerAgain = (M3CompilerFacts)
                    projection.getMethod("compilerFacts").invoke(null);
            check(compilerAgain.count(M3CompilerFacts.FIELDS) == 2,
                    "generated static primitive source data cannot be mutated by exports");
            double[] scoring = new double[M3CompilerFacts.COUNT];
            scoring[M3CompilerFacts.METHODS] = 2.0;
            check(compiler.project(scoring) == 4.0, "domain heuristic projection");
            scoring[M3CompilerFacts.RUNTIME_INVOCATIONS] = 1.0;
            check(Double.isNaN(compiler.project(scoring)),
                    "missing runtime signal refuses false score");
            lanes[0][0] ^= 1;
            check(fresh[0][0] != lanes[0][0], "defensive source-time metadata export");
            check(StructuralSignals.weightedDistance(lanes, fresh, new double[]{1,1,1,1,1,1}) > 0.0,
                    "package change becomes distance evidence");
        }
        try (Compiled c = compile("demo.MetaOnly",
                "package demo; import jdk.m3.collections.sortable.Sortable;"
                + "@Sortable public class MetaOnly {public String name;} ", true)) {
            check(c.ok, "metadata only is accepted: " + c.messages);
            check(!c.generated.contains("absoluteSignal("), "no invented absolute weight");
        }
        String hostileSource = """
                package demo;
                import jdk.m3.collections.sortable.Sortable;
                @Sortable public class Hostile {
                    String fake = "if (x) for (y) throw new Exception();";
                    // if(true) while(false) new Runnable();
                    int branch(int n) {
                        Runnable task = () -> { };
                        for (int i = 0; i < n; i++) {
                            if (i > 0) n--;
                        }
                        try {
                            return n > 1 ? n : 0;
                        } catch (RuntimeException error) {
                            throw error;
                        }
                    }
                    static class Nested {
                        int ignored(int x) { if (x > 0) return java.lang.Math.abs(x); return 0; }
                    }
                }
                """;
        try (Compiled hostile = compile("demo.Hostile", hostileSource, true)) {
            check(hostile.ok, "nested/code-looking source compiles");
            Class<?> companion = hostile.loader.loadClass("demo.HostileM3Sortable");
            M3CompilerFacts facts = (M3CompilerFacts)
                    companion.getMethod("compilerFacts").invoke(null);
            long[][] retainedTokens = (long[][])
                    companion.getMethod("structuralSignals").invoke(null);
            check(retainedTokens[5].length >= 1,
                    "original six-lane call tokens still include nested declarations");
            check(facts.count(M3CompilerFacts.METHOD_INVOCATIONS) == 0
                    && facts.count(M3CompilerFacts.DISTINCT_RESOLVED_CALLEE_TOKENS) == 0,
                    "new type-local invocation counts exclude nested class method bodies");
            M3MachineHintRules sourceHints = (M3MachineHintRules)
                    companion.getMethod("machinePatternHints").invoke(null);
            check(sourceHints.state(M3MachineHintRules.ITERATIVE_INVOCATION)
                    == M3MachineHintRules.State.NOT_OBSERVED,
                    "implicit super call cannot fabricate iterative invocation hint");
            check(sourceHints.state(M3MachineHintRules.LOOP_BRANCH_REVIEW)
                    == M3MachineHintRules.State.CANDIDATE,
                    "real loop and branch produce a candidate-only source hint");
            check(facts.count(M3CompilerFacts.LOOPS) == 1,
                    "one real loop, text/comments ignored");
            check(facts.count(M3CompilerFacts.IF_BRANCHES) == 1,
                    "nested class has separate source ownership");
            check(facts.count(M3CompilerFacts.CONDITIONAL_EXPRESSIONS) == 1,
                    "conditional AST shape retained");
            check(facts.count(M3CompilerFacts.LAMBDAS) == 1,
                    "lambda syntactic signal");
            check(facts.count(M3CompilerFacts.TRY_BLOCKS) == 1
                    && facts.count(M3CompilerFacts.CATCH_BLOCKS) == 1,
                    "exception paths from AST");
            check(facts.count(M3CompilerFacts.RETURN_STATEMENTS) == 1,
                    "nested return statements not stolen by enclosing class");
        }
        // An explicitly written constructor super() remains a real invocation site.
        try (Compiled explicit = compile("demo.ExplicitCtor",
                "package demo; import jdk.m3.collections.sortable.Sortable;"
                + "@Sortable public class ExplicitCtor { public ExplicitCtor() { super(); } }",
                true)) {
            Class<?> companion = explicit.loader.loadClass("demo.ExplicitCtorM3Sortable");
            M3CompilerFacts sourceFacts = (M3CompilerFacts)
                    companion.getMethod("compilerFacts").invoke(null);
            check(sourceFacts.count(M3CompilerFacts.METHOD_INVOCATIONS) == 1,
                    "explicit source super() must remain a source-call fact");
        }
        // Record-component private backing fields are not public signal candidates;
        // their compiler-model accessor is the source-visible numeric member.
        try (Compiled record = compile("demo.SignalRecord",
                "package demo; import jdk.m3.collections.sortable.Sortable;"
                + "@Sortable(signal=\"weight\") public record SignalRecord(double weight) {}", true)) {
            check(record.ok, "numeric record component must compile: " + record.messages);
            Class<?> valueType = record.loader.loadClass("demo.SignalRecord");
            Class<?> projection = record.loader.loadClass("demo.SignalRecordM3Sortable");
            Object value = valueType.getConstructor(double.class).newInstance(7.5);
            check(((Double) projection.getMethod("absoluteSignal", valueType)
                    .invoke(null, value)) == 7.5,
                    "record component uses its public weight() accessor");
            check(record.generated.contains("return value.weight();"),
                    "generated source invokes the record accessor, not its private field");
            Object values = java.lang.reflect.Array.newInstance(valueType, 1);
            java.lang.reflect.Array.set(values, 0, value);
            check(Arrays.equals((double[]) projection.getMethod("absoluteSignals", values.getClass())
                    .invoke(null, values), new double[]{7.5}),
                    "record array precompute preserves source contract");
        }
        expectFail("demo.TextRecord",
                "package demo; import jdk.m3.collections.sortable.Sortable;"
                + "@Sortable(signal=\"weight\") public record TextRecord(String weight) {}",
                "primitive numeric value");
        expectFail("demo.ShadowClass",
                "package demo; import jdk.m3.collections.sortable.Sortable;"
                + "@Sortable(signal=\"weight\") public class ShadowClass {"
                + "private double weight; public double weight(){return weight;}}",
                "accessible instance member");
        String prefix = "package demo; import jdk.m3.collections.sortable.Sortable;";
        expectFail("demo.Missing", prefix + "@Sortable(signal=\"missing\") public class Missing {}",
                "requires exactly one accessible numeric member");
        expectFail("demo.Private", prefix + "@Sortable(signal=\"weight\") "
                + "public class Private {private double weight;}", "accessible instance member");
        expectFail("demo.TextWeight", prefix + "@Sortable(signal=\"weight\") "
                + "public class TextWeight {public String weight;}", "primitive numeric value");
        expectFail("demo.InterfaceWeight", prefix + "@Sortable public interface InterfaceWeight {}",
                "top-level classes and records only");
        check(StructuralSignals.jaccardDistance(new long[0], new long[0]) == 0.0, "empty metric");
        check(StructuralSignals.jaccardDistance(new long[]{1L}, new long[]{2L}) == 1.0,
                "disjoint metric");
        expectInvalid(() -> StructuralSignals.jaccardDistance(new long[]{2,1}, new long[]{1}),
                "unsorted input rejected");
        expectInvalid(() -> StructuralSignals.weightedDistance(new long[6][0], new long[6][0],
                new double[]{0,0,0,0,0,0}), "all-zero weights rejected");
        System.out.println("PASS SortableProcessorSelfTest checks=" + checks);
    }

    private static void expectInvalid(Runnable action, String label) {
        try { action.run(); } catch (IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError(label);
    }

    private static void expectFail(String type, String source, String expected) throws Exception {
        try (Compiled failure = compile(type, source, false)) {
            check(!failure.ok && failure.messages.contains(expected), expected + ": " + failure.messages);
        }
    }

    private static Compiled compile(String type, String code, boolean expectedSuccess) throws Exception {
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        if (javac == null) throw new IllegalStateException("JDK javac required");
        Path root = Files.createTempDirectory("m3-sortable-javac-");
        Path source = root.resolve("src/" + type.replace('.', '/') + ".java");
        Path generated = root.resolve("generated");
        Path classes = root.resolve("classes");
        Files.createDirectories(source.getParent());
        Files.createDirectories(generated);
        Files.createDirectories(classes);
        Files.writeString(source, code, StandardCharsets.UTF_8);
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        boolean ok;
        try (StandardJavaFileManager files = javac.getStandardFileManager(
                diagnostics, null, StandardCharsets.UTF_8)) {
            // Maven exec: processor lives in its own project classloader, not
            // necessarily in java.class.path. Admit its exact class CodeSource.
            var origin = SortableProcessor.class.getProtectionDomain().getCodeSource();
            if (origin == null) throw new IllegalStateException("processor CodeSource unavailable");
            String cp = Path.of(origin.getLocation().toURI())
                    + java.io.File.pathSeparator + System.getProperty("java.class.path");
            List<String> flags = List.of("--release", "21", "-Xlint:all", "-Werror",
                    "-classpath", cp, "-processorpath", cp,
                    "-processor", SortableProcessor.class.getName(),
                    "-s", generated.toString(), "-d", classes.toString());
            ok = Boolean.TRUE.equals(javac.getTask(null, files, diagnostics, flags, null,
                    files.getJavaFileObjects(source.toFile())).call());
        }
        StringBuilder messages = new StringBuilder();
        for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
            messages.append(d.getKind()).append(':').append(d.getMessage(null)).append('\n');
        }
        if (expectedSuccess && !ok) throw new AssertionError(messages.toString());
        Path gen = generated.resolve(type.replace('.', '/') + "M3Sortable.java");
        String generatedText = Files.exists(gen) ? Files.readString(gen) : "";
        URLClassLoader loader = new URLClassLoader(new URL[]{classes.toUri().toURL()},
                SortableProcessorSelfTest.class.getClassLoader());
        return new Compiled(root, loader, ok, generatedText, messages.toString());
    }

    private record Compiled(Path root, URLClassLoader loader, boolean ok,
            String generated, String messages) implements AutoCloseable {
        @Override public void close() throws IOException {
            loader.close();
            try (var stream = Files.walk(root)) {
                for (Path file : stream.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(file);
            }
        }
    }

    private static void check(boolean pass, String label) {
        checks++;
        if (!pass) throw new AssertionError(label);
    }
}
