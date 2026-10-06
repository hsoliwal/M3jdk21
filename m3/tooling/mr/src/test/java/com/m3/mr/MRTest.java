// SPDX-License-Identifier: Apache-2.0
package com.m3.mr;

import static org.junit.jupiter.api.Assertions.*;

import com.m3.rewrite.backport.M3Jdk21HashPinnedSnapshotRecipe;
import com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

final class MRTest {
    private static final Path ROOT = Path.of(System.getProperty("m3.root")).toAbsolutePath().normalize();
    private static final Path CRATE = Path.of(System.getProperty("m3.crate")).toAbsolutePath().normalize();
    private static final Path OUT = CRATE.resolve("target");
    private static final Path RES = CRATE.resolve("src/main/resources");
    private static final Path JR = RES.resolve("com/m3/rewrite/backport/jdk21-hash-pinned/jdk22-m3-mr");
    private static final Path TR = RES.resolve("com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-mr");
    private static final String PRODUCT = "src/java.base/share/classes/";
    private static final String MATCHER = PRODUCT + "java/util/regex/Matcher.java";
    private static final String PROBE = "test/jdk/java/util/regex/M3RegexReuseTest.java";
    private static final String JAVA = Path.of(System.getProperty("java.home"), "bin/java").toString();
    private static final String JAVAC = Path.of(System.getProperty("java.home"), "bin/javac").toString();
    private static final List<String> CLOSURE = List.of(MATCHER, PRODUCT + "java/util/regex/Pattern.java",
            PRODUCT + "jdk/internal/mindex/M3TQ.java", PRODUCT + "jdk/internal/mindex/M3StringBacking.java");
    private static List<SourceFile> before;
    private static List<SourceFile> after;
    private static String semantic;

    private static Recipe javaRecipe() { return new M3Jdk21HashPinnedSnapshotRecipe("jdk22-m3-mr"); }
    private static Recipe textRecipe() { return new M3Jdk21HashPinnedTextSnapshotRecipe("m3-mr"); }
    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> { throw new IllegalStateException(error); });
    }
    private static List<String[]> rows(Path resources) throws Exception {
        return Files.readAllLines(resources.resolve("manifest.tsv")).stream()
                .filter(line -> !line.isBlank() && !line.startsWith("#"))
                .map(line -> line.split("\t", -1)).toList();
    }
    private static List<SourceFile> apply(Recipe recipe, List<SourceFile> input) {
        Map<Path, SourceFile> result = new LinkedHashMap<>();
        input.forEach(file -> result.put(file.getSourcePath(), file));
        for (var change : recipe.run(new InMemoryLargeSourceSet(input), context(), 1).getChangeset().getAllResults()) {
            if (change.getBefore() != null) result.remove(change.getBefore().getSourcePath());
            if (change.getAfter() != null) result.put(change.getAfter().getSourcePath(), change.getAfter());
        }
        return new ArrayList<>(result.values());
    }
    private static SourceFile java(String path, String source) {
        var parsed = JavaParser.fromJavaVersion().build().parseInputs(
                List.of(Parser.Input.fromString(Path.of(path), source)), null, context()).toList();
        assertEquals(1, parsed.size());
        assertEquals(source, parsed.getFirst().printAll());
        assertInstanceOf(org.openrewrite.java.tree.J.CompilationUnit.class, parsed.getFirst());
        return parsed.getFirst();
    }
    private static Map<String, String> texts(List<SourceFile> input) {
        Map<String, String> result = new TreeMap<>();
        input.forEach(file -> result.put(file.getSourcePath().toString(), file.printAll()));
        return result;
    }

    @BeforeAll static void prepare() throws Exception {
        Files.createDirectories(OUT);
        for (String line : Files.readAllLines(CRATE.resolve("inputs.tsv"))) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] row = line.split("\t");
            assertEquals(row[1], hash(Files.readString(ROOT.resolve(row[0]))), row[0]);
        }
        before = new ArrayList<>();
        for (Path resources : List.of(JR, TR)) {
            for (String[] row : rows(resources)) {
                if (row[1].equals("ABSENT")) continue;
                String content = Files.readString(resources.resolve(row[3] + ".before"));
                assertEquals(row[1], hash(content));
                before.add(resources.equals(JR) ? java(row[0], content)
                        : PlainText.builder().sourcePath(Path.of(row[0])).text(content).build());
            }
        }
        // Run the retained Java and text recipes in both orders. Each Java transition is compiled;
        // these exact-source admissions are not a new source-convergence engine.
        after = apply(textRecipe(), apply(javaRecipe(), before));
        List<SourceFile> reverse = apply(javaRecipe(), apply(textRecipe(), before));
        assertEquals(texts(after), texts(reverse));
        assertEquals(texts(after), texts(apply(textRecipe(), apply(javaRecipe(), after))));
        assertEquals(texts(reverse), texts(apply(javaRecipe(), apply(textRecipe(), reverse))));
        for (Path resources : List.of(JR, TR)) {
            for (String[] row : rows(resources)) {
                assertEquals(row[2], hash(texts(after).get(row[0])));
            }
        }
        Path generated = OUT.resolve("generated");
        for (var file : after) write(generated.resolve(file.getSourcePath()), file.printAll());
        String old = Files.readString(JR.resolve("Matcher.java.txt.before"));
        compile("before", old);
        compile("after", texts(after).get(MATCHER));
        compile("reverse", texts(reverse).get(MATCHER));
        Files.createDirectories(OUT.resolve("probes"));
        execute(List.of(JAVAC, "--release", "21", "-proc:none", "-Xlint:all", "-Werror", "-d",
                OUT.resolve("probes").toString(), generated.resolve(PROBE).toString(),
                ROOT.resolve("test/jdk/java/util/regex/M3RegexLiteralTQTest.java").toString()), "compile-probes", true);
        semantic = execute(runtime(null, "mixed", "M3RegexReuseTest", "semantic"), "stock-semantic", true).strip();
        assertTrue(semantic.startsWith("MR_SEMANTIC\t"));
    }

    @Test void exactMaterializationAndReplay() throws Exception {
        Map<String, String> admitted = texts(after);
        // Complete preflight precedes any writes. Current source may be only a reviewed pre/postimage.
        for (Path resources : List.of(JR, TR)) {
            for (String[] row : rows(resources)) {
                Path target = ROOT.resolve(row[0]);
                if (!Files.exists(target)) assertEquals("ABSENT", row[1], row[0]);
                else assertTrue(List.of(row[1], row[2]).contains(hash(Files.readString(target))), row[0]);
            }
        }
        if (Boolean.getBoolean("mr.materialize")) {
            for (var entry : admitted.entrySet()) {
                Path target = ROOT.resolve(entry.getKey()).normalize();
                assertTrue(target.startsWith(ROOT));
                if (!Files.exists(target) || !Files.readString(target).equals(entry.getValue())) write(target, entry.getValue());
            }
        }
    }

    @Test void driftMissingWrongParserDuplicateAndCollisionRefuse() {
        SourceFile owner = before.stream().filter(f -> f.getSourcePath().toString().equals(MATCHER)).findFirst().orElseThrow();
        var drift = new ArrayList<>(before);
        drift.set(drift.indexOf(owner), java(MATCHER, owner.printAll() + "\n// drift\n"));
        assertThrows(RuntimeException.class, () -> apply(javaRecipe(), drift));
        var missing = new ArrayList<>(before); missing.remove(owner);
        assertThrows(RuntimeException.class, () -> apply(javaRecipe(), missing));
        var wrong = new ArrayList<>(before);
        wrong.set(wrong.indexOf(owner), PlainText.builder().sourcePath(owner.getSourcePath()).text(owner.printAll()).build());
        assertThrows(RuntimeException.class, () -> apply(javaRecipe(), wrong));
        var duplicate = new ArrayList<>(before); duplicate.add(owner.withId(java.util.UUID.randomUUID()));
        assertThrows(RuntimeException.class, () -> apply(javaRecipe(), duplicate));
        var collision = new ArrayList<>(before); collision.add(java(PROBE, "class Unreviewed {}"));
        assertThrows(RuntimeException.class, () -> apply(javaRecipe(), collision));
        var text = new ArrayList<>(before);
        var first = text.stream().filter(PlainText.class::isInstance).findFirst().orElseThrow();
        text.remove(first);
        assertThrows(RuntimeException.class, () -> apply(textRecipe(), text));
        text.add(PlainText.builder().sourcePath(first.getSourcePath()).text(first.printAll() + "drift").build());
        assertThrows(RuntimeException.class, () -> apply(textRecipe(), text));
    }

    @Test void unrelatedInputSurvivesAndTemplateTamperingRefuses() throws Exception {
        var source = java("src/other/Untouched.java", "class Untouched { String value() { return \"a+b?\"; } }");
        var withExtra = new ArrayList<>(before); withExtra.add(source);
        assertEquals(source.printAll(), texts(apply(javaRecipe(), withExtra)).get(source.getSourcePath().toString()));
        Path bad = OUT.resolve("test-classes/com/m3/rewrite/backport/jdk21-hash-pinned/jdk22-m3-mr-bad");
        for (String[] row : rows(JR)) write(bad.resolve(row[3]), Files.readString(JR.resolve(row[3])) + "// drift\n");
        write(bad.resolve("manifest.tsv"), Files.readString(JR.resolve("manifest.tsv")));
        assertThrows(IllegalStateException.class, () -> new M3Jdk21HashPinnedSnapshotRecipe("jdk22-m3-mr-bad").getInitialValue(context()));
    }

    @TestFactory Stream<DynamicTest> semanticsAndReuseAcrossModes() {
        return Stream.of("interpreter", "mixed", "c1", "c2").map(mode -> DynamicTest.dynamicTest(mode, () -> {
            assertEquals(semantic, execute(runtime("before", mode, "M3RegexReuseTest", "semantic"), "before-" + mode, true).strip());
            assertEquals(semantic, execute(runtime("after", mode, "M3RegexReuseTest", "semantic"), "after-" + mode, true).strip());
            if (mode.equals("c1") || mode.equals("c2")) {
                String log = Files.readString(OUT.resolve("jit-after-" + mode + "-semantic.xml"));
                for (String method : List.of("reset ()", "reset (Ljava/lang/CharSequence;)", "search (I)")) {
                    assertTrue(log.lines().anyMatch(line -> line.contains("<nmethod")
                            && line.contains("compiler='" + mode + "'")
                            && line.contains("method='java.util.regex.Matcher " + method)),
                            "changed reset and fact-consuming search compiled by " + mode + ": " + method);
                }
            }
            String reuse = execute(runtime("after", mode, "M3RegexReuseTest", "reuse"), "reuse-" + mode, true);
            assertTrue(reuse.contains("MR_REUSE"));
        }));
    }

    @Test void oldRegressionsApiAndNoNewFields() throws Exception {
        execute(runtime("after", "mixed", "M3RegexLiteralTQTest", null), "original-regression", true);
        String javap = Path.of(System.getProperty("java.home"), "bin/javap").toString();
        for (String type : List.of("java/util/regex/Matcher", "java/util/regex/Pattern")) {
            String left = execute(List.of(javap, "-p", "-s", OUT.resolve("before/" + type + ".class").toString()), "before-api-" + Path.of(type).getFileName(), true);
            String right = execute(List.of(javap, "-p", "-s", OUT.resolve("after/" + type + ".class").toString()), "after-api-" + Path.of(type).getFileName(), true);
            assertEquals(left, right, "all public/private descriptors and fields");
        }
        String jdeps = Path.of(System.getProperty("java.home"), "bin/jdeps").toString();
        assertEquals("Warning: split package: java.util.regex jrt:/java.base " + OUT.resolve("after")
                + "\nafter -> java.base", execute(List.of(jdeps, "-s", OUT.resolve("after").toString()), "jdeps", true).strip());
    }

    @Test void rejectsLostReuseAndStaleInputMutants() throws Exception {
        String baseline = execute(runtime("before", "mixed", "M3RegexReuseTest", "reuse"), "lost-reuse-control", false);
        assertTrue(baseline.contains("plain reset retains exact immutable facts"));
        String original = texts(after).get(MATCHER);
        assertTrue(original.contains("if (input != text) {"));
        compile("stale", original.replace("if (input != text) {", "if (false) {"));
        String stale = execute(runtime("stale", "mixed", "M3RegexReuseTest", "reuse"), "stale-input-control", false);
        assertTrue(stale.contains("equal different input invalidates"));
        String staleSemantics = execute(runtime("stale", "mixed", "M3RegexReuseTest", "semantic"), "stale-semantic-control", false);
        assertTrue(staleSemantics.contains("AssertionError: result"));
    }

    @Test void coldAndWarmAllocationReceipts() throws Exception {
        for (String version : List.of("before", "after")) {
            execute(runtime(version, "mixed", "M3RegexReuseTest", "cost"), version + "-cost", true);
        }
    }

    @Test void sharedMappingRetainsEarlierCustody() throws Exception {
        execute(List.of("python3", CRATE.resolve("test_custody.py").toString(),
                OUT.resolve("generated/m3/tooling/tq-sync/verify-plan.py").toString()),
                "mapping-custody", true);
    }

    @Test void currentStringInvariantsAndMutants() throws Exception {
        execute(List.of("python3", CRATE.resolve("test_invariants.py").toString(),
                OUT.resolve("generated/m3/runtime-integration/check-m3string-invariants.py").toString()),
                "string-invariants", true);
    }

    private static void compile(String name, String matcher) throws Exception {
        Path sources = OUT.resolve(name + "-src");
        List<String> command = new ArrayList<>(List.of(JAVAC, "-source", "21", "-target", "21", "-proc:none",
                "-implicit:none", "-Xlint:all", "-Werror", "--patch-module", "java.base=" + sources,
                "-d", OUT.resolve(name).toString()));
        for (String path : CLOSURE) {
            Path target = sources.resolve(path.substring(PRODUCT.length()));
            write(target, path.equals(MATCHER) ? matcher : Files.readString(ROOT.resolve(path)));
            command.add(target.toString());
        }
        execute(command, "compile-" + name, true);
    }
    private static List<String> runtime(String patch, String mode, String main, String arg) {
        List<String> command = new ArrayList<>(List.of(JAVA, "-Xmx384m", "-Xcheck:jni"));
        if (patch != null) command.addAll(List.of("--patch-module", "java.base=" + OUT.resolve(patch),
                "--add-opens", "java.base/java.util.regex=ALL-UNNAMED"));
        if (mode.equals("interpreter")) command.add("-Xint");
        if (mode.equals("c1")) command.addAll(List.of("-Xbatch", "-XX:TieredStopAtLevel=1"));
        if (mode.equals("c2")) command.addAll(List.of("-Xbatch", "-XX:-TieredCompilation"));
        if (mode.equals("c1") || mode.equals("c2")) command.addAll(List.of("-XX:CompileThreshold=100",
                "-XX:+UnlockDiagnosticVMOptions", "-XX:+LogCompilation",
                "-XX:LogFile=" + OUT.resolve("jit-" + patch + "-" + mode + "-" + arg + ".xml")));
        command.addAll(List.of("-cp", OUT.resolve("probes").toString(), main));
        if (arg != null) command.add(arg);
        return command;
    }
    private static String execute(List<String> command, String log, boolean success) throws Exception {
        Path file = OUT.resolve(log + ".log");
        Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(file.toFile()).start();
        if (!process.waitFor(90, TimeUnit.SECONDS)) {
            process.destroyForcibly(); throw new AssertionError("timeout: " + log);
        }
        String output = Files.readString(file);
        if (success) assertEquals(0, process.exitValue(), log + "\n" + output);
        else assertNotEquals(0, process.exitValue(), log + " negative control unexpectedly passed");
        return output;
    }
    private static void write(Path path, String text) throws Exception {
        Files.createDirectories(path.getParent()); Files.writeString(path, text);
    }
    private static String hash(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
}
