// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.atom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.rewrite.M3Java21ConvergenceRecipe;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

/** Real recipes, independently compiled immutable oracles, bounded permutations and counterexamples. */
final class M3RecipeLabTest {
    private static final Class<?>[] THREE_INTS = {int.class, int.class, int.class};
    private static final Class<?>[] TWO_INTS = {int.class, int.class};

    @TestFactory
    Stream<DynamicTest> cartesianProjects() {
        return IntStream.range(0, 4).mapToObj(batch -> DynamicTest.dynamicTest(
                "operators-unary-parentheses-names-" + batch, () -> {
                    int start = batch * 36;
                    int end = start + 36;
                    Map<String, String> original = IntCases.project(start, end);
                    var baseline = MemoryProject.compile(original);
                    ProjectCheck check = project -> compare(
                            baseline, MemoryProject.compile(project), start, end);
                    Run run = converge(original, M3Java21ConvergenceRecipe::new, 4, check);
                    assertEquals(2, run.passes());
                    assertNotEquals(original.get(IntCases.PATH), run.source().get(IntCases.PATH));
                    assertEquals(original.get(IntCases.NOISE_PATH), run.source().get(IntCases.NOISE_PATH));
                    assertEquals(original, IntCases.project(start, end), "immutable baseline");
                    assertEquals(36, occurrences(run.source().get(IntCases.PATH), "M3-ATOM:"));
                    assertEquals(36, occurrences(run.source().get(IntCases.PATH), "M3-IOP:"));
                    assertEquals(36, atomCount(run.source()));
                    assertEquals(1, converge(run.source(), M3Java21ConvergenceRecipe::new, 2, check).passes());
                }));
    }

    @Test
    void inputFileOrderDoesNotChangeConvergence() throws Exception {
        Map<String, String> original = IntCases.project(0, 2);
        var baseline = MemoryProject.compile(original);
        ProjectCheck check = project -> compare(baseline, MemoryProject.compile(project), 0, 2);
        var forward = new LinkedHashMap<String, String>();
        original.keySet().stream().sorted().forEach(key -> forward.put(key, original.get(key)));
        var backward = new LinkedHashMap<String, String>();
        original.keySet().stream().sorted(Collections.reverseOrder())
                .forEach(key -> backward.put(key, original.get(key)));
        assertEquals(converge(forward, M3Java21ConvergenceRecipe::new, 4, check).source(),
                converge(backward, M3Java21ConvergenceRecipe::new, 4, check).source());
    }

    @Test
    void compilerRejectsInvalidOriginalRatherThanChangingTheOracle() {
        var invalid = Map.of("lab/Broken.java", "package lab; class Broken { int f() { return missing; } }");
        AssertionError failure = assertThrows(AssertionError.class, () -> MemoryProject.compile(invalid));
        assertTrue(failure.getMessage().contains("JAVA21_COMPILE"));
        assertTrue(failure.getMessage().contains("missing"));
        assertTrue(invalid.get("lab/Broken.java").contains("return missing;"));
    }

    @Test
    void onePassBudgetIsNotAProofOfConvergence() {
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> converge(IntCases.project(0, 1), M3PureIntConvergenceRecipe::new,
                        1, MemoryProject::compile));
        assertEquals("PASS_LIMIT:1", failure.getMessage());
        assertThrows(IllegalArgumentException.class,
                () -> converge(IntCases.project(0, 1), M3PureIntConvergenceRecipe::new,
                        0, MemoryProject::compile));
    }

    @Test
    void recurringSourceStateIsAnOscillationNotConvergence() {
        var source = Map.of("lab/Toggle.java", "package lab; class Toggle { int value() { return 1; } }");
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> converge(source, Toggle::new, 4, MemoryProject::compile));
        assertEquals("OSCILLATION:2", failure.getMessage());
    }

    @Test
    void behavioralOracleKillsWrongArithmeticCandidate() throws Exception {
        Map<String, String> original = IntCases.project(0, 1);
        var baseline = MemoryProject.compile(original);
        var mutant = new TreeMap<>(original);
        mutant.put(IntCases.PATH, original.get(IntCases.PATH).replace("return a + b;", "return a - b;"));
        assertNotEquals(original, mutant, "mutant must actually differ");
        var wrong = MemoryProject.compile(mutant);
        assertThrows(AssertionError.class, () -> compare(baseline, wrong, 0, 1));
    }

    @Test
    void behavioralOracleKillsCallbackOrderCandidate() throws Exception {
        Map<String, String> original = IntCases.project(0, 1);
        var baseline = MemoryProject.compile(original);
        var mutant = new TreeMap<>(original);
        mutant.put(IntCases.NOISE_PATH, original.get(IntCases.NOISE_PATH).replace(
                "left.getAsInt() + right.getAsInt()", "right.getAsInt() + left.getAsInt()"));
        assertNotEquals(original, mutant, "mutant must actually differ");
        var wrong = MemoryProject.compile(mutant);
        assertThrows(AssertionError.class, () -> compare(baseline, wrong, 0, 1));
        assertNotEquals(baseline.invoke("lab.Noise", "witness", TWO_INTS, 0, 1),
                wrong.invoke("lab.Noise", "witness", TWO_INTS, 0, 1));
    }

    @Test
    void quotedMarkerExamplesDoNotSuppressRealPatternAndDocumentation() throws Exception {
        String source = """
                package lab;
                public final class Markers {
                    private Markers() {}
                    /** Historical example: M3-ATOM: m3$pureIntAtom; Pattern/IOP: PURE_INT_EXPRESSION. */
                    private static int calc(int a, int b) {
                        /* Historical example: M3-IOP: PURE_INT_EXPRESSION should not certify a role. */
                        int m3$pureIntAtom = a + b;
                        return m3$pureIntAtom;
                    }
                    public static int eval(int a, int b) { return calc(a,b); }
                }
                """;
        Run result = converge(Map.of("lab/Markers.java", source),
                M3PureIntConvergenceRecipe::new, 4, MemoryProject::compile);
        String after = result.source().get("lab/Markers.java");
        assertEquals(2, occurrences(after, "M3-IOP:"), "retain example AND add the actual role");
        assertEquals(2, occurrences(after, "M3-ATOM:"), "retain example AND add actual semantic memory");
        assertTrue(after.contains("Historical example:"));
        assertEquals(1, converge(result.source(), M3PureIntConvergenceRecipe::new,
                2, MemoryProject::compile).passes());
    }

    private static void compare(MemoryProject.Image baseline, MemoryProject.Image candidate,
            int start, int end) throws ReflectiveOperationException {
        assertEquals(baseline.surface(), candidate.surface(), "public/protected surface");
        assertNotEquals(baseline.loadClass(IntCases.OWNER), candidate.loadClass(IntCases.OWNER),
                "candidate and immutable parent require independent class identity");
        for (int id = start; id < end; id++) {
            for (int[] pair : IntCases.inputs()) {
                var expected = baseline.invoke(IntCases.OWNER, "eval", THREE_INTS, id, pair[0], pair[1]);
                assertEquals(IntCases.expected(id, pair[0], pair[1]), expected.value(), "independent arithmetic oracle");
                assertEquals(expected, candidate.invoke(IntCases.OWNER, "eval", THREE_INTS, id, pair[0], pair[1]),
                        "original/candidate case " + id);
            }
        }
        for (int left : new int[] {-1, 0, 1}) for (int right : new int[] {-1, 0, 1}) {
            assertEquals(baseline.invoke("lab.Noise", "witness", TWO_INTS, left, right),
                    candidate.invoke("lab.Noise", "witness", TWO_INTS, left, right), "callback trace");
            assertEquals(baseline.invoke("lab.Noise", "divide", TWO_INTS, left, right),
                    candidate.invoke("lab.Noise", "divide", TWO_INTS, left, right), "exception contract");
        }
    }

    /** Test controller only: every mutation is performed by the real OpenRewrite recipe scheduler. */
    static Run converge(Map<String, String> original, Supplier<Recipe> factory,
            int budget, ProjectCheck check) throws Exception {
        if (budget < 1 || budget > 8) throw new IllegalArgumentException("pass budget");
        Map<String, String> current = Collections.unmodifiableMap(new LinkedHashMap<>(original));
        Set<Map<String, String>> visited = new HashSet<>();
        visited.add(current);
        for (int pass = 1; pass <= budget; pass++) {
            var context = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
            List<SourceFile> inputs = parse(current, context);
            var result = factory.get().run(new InMemoryLargeSourceSet(inputs), context, 1);
            var changes = result.getChangeset().getAllResults();
            if (changes.isEmpty()) return new Run(current, pass);
            Map<String, String> next = new TreeMap<>(current);
            for (var change : changes) {
                SourceFile before = change.getBefore();
                SourceFile after = change.getAfter();
                assertNotNull(before, "FILE recipe may not create a file");
                assertNotNull(after, "FILE recipe may not delete a file");
                assertInstanceOf(J.CompilationUnit.class, after);
                assertEquals(before.getSourcePath(), after.getSourcePath(), "FILE path lock");
                assertEquals(before.getId(), after.getId(), "source identity lock");
                String path = after.getSourcePath().toString().replace('\\', '/');
                assertTrue(current.containsKey(path));
                next.put(path, after.printAll());
            }
            check.verify(Collections.unmodifiableMap(next));
            if (!visited.add(next)) throw new IllegalStateException("OSCILLATION:" + pass);
            current = Collections.unmodifiableMap(next);
        }
        throw new IllegalStateException("PASS_LIMIT:" + budget);
    }

    private static List<SourceFile> parse(Map<String, String> source, ExecutionContext context) {
        List<Parser.Input> inputs = new ArrayList<>();
        source.forEach((path, text) -> inputs.add(Parser.Input.fromString(Path.of(path), text)));
        List<SourceFile> parsed = JavaParser.fromJavaVersion().build().parseInputs(inputs, null, context).toList();
        assertEquals(source.size(), parsed.size());
        for (SourceFile file : parsed) assertInstanceOf(J.CompilationUnit.class, file);
        return parsed;
    }

    private static int atomCount(Map<String, String> source) {
        int[] count = {0};
        var context = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        for (SourceFile file : parse(source, context)) {
            new JavaIsoVisitor<int[]>() {
                @Override
                public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, int[] n) {
                    if (M3PureIntAtomEligibility.atomized(method)) n[0]++;
                    return super.visitMethodDeclaration(method, n);
                }
            }.visit(file, count);
        }
        return count[0];
    }

    private static int occurrences(String value, String needle) {
        int count = 0;
        for (int index = 0; (index = value.indexOf(needle, index)) >= 0; index += needle.length()) count++;
        return count;
    }

    @FunctionalInterface
    interface ProjectCheck {
        void verify(Map<String, String> source) throws Exception;
    }

    record Run(Map<String, String> source, int passes) {}

    private static final class Toggle extends Recipe {
        @Override public String getDisplayName() { return "Test-only oscillation"; }
        @Override public String getDescription() { return "Deliberately nonconvergent laboratory control."; }
        @Override public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new JavaIsoVisitor<ExecutionContext>() {
                @Override public J.Literal visitLiteral(J.Literal literal, ExecutionContext context) {
                    if (!(literal.getValue() instanceof Integer value)) return literal;
                    int replacement = value == 1 ? 2 : 1;
                    return literal.withValue(replacement).withValueSource(Integer.toString(replacement));
                }
            };
        }
    }
}
