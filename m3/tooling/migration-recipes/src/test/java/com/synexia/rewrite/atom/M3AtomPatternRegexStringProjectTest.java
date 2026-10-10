// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite.atom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.Assertions;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.TypeValidation;

/**
 * One in-memory multipass project joining atomization, patternization and regex/String behavior.
 *
 * <p>This is deliberately a receiver-side proof: it exercises the existing Synexia-owned
 * recipes and adds no production recipe or third-party implementation.</p>
 */
final class M3AtomPatternRegexStringProjectTest {
    private static final String FIXTURE = "lab/Fixture.java";

    @Test
    void both_pass_orders_converge_and_preserve_regex_behavior() throws Exception {
        Map<String, String> original = project();
        ClassLoader baseline = MemJava.compile(original);

        Map<String, String> atomThenPattern = apply(
                new M3PatternizePureIntAtomRecipe(),
                apply(new M3AtomizePureIntReturnRecipe(), original));
        Map<String, String> patternThenAtom = apply(
                new M3AtomizePureIntReturnRecipe(),
                apply(new M3PatternizePureIntAtomRecipe(), original));

        Recipe convergence = new M3PureIntConvergenceRecipe();
        Map<String, String> canonical = apply(convergence, atomThenPattern);
        Map<String, String> alternate = apply(convergence, patternThenAtom);

        assertEquals(canonical, alternate, "multipass order must converge");
        assertEquals(canonical, apply(convergence, canonical), "normal form must be fixed");
        assertEquals(original.keySet(), canonical.keySet(), "recipes are file-local");
        assertTrue(canonical.get(FIXTURE).contains("M3-IOP: PURE_INT_EXPRESSION"));
        assertTrue(canonical.get(FIXTURE).contains("M3-ATOM: m3$pureIntAtom;"));
        assertTrue(canonical.get(FIXTURE).contains("M3-ATOM: m3$pureIntAtom; Pattern/IOP:"));
        assertEquals(surface(baseline, "lab.Fixture"), surface(MemJava.compile(canonical), "lab.Fixture"));
        assertEquals(surface(baseline, "lab.RegexConsumer"),
                surface(MemJava.compile(canonical), "lab.RegexConsumer"));

        ClassLoader candidate = MemJava.compile(canonical);
        for (String input : List.of("", "a", "baaac", "😀aa", "bbb-aa")) {
            assertEquals(invoke(baseline, 7, -3, input),
                    invoke(candidate, 7, -3, input), input);
        }
    }

    @Test
    void compiler_and_behavior_oracles_reject_a_deliberate_mutant() throws Exception {
        Map<String, String> original = project();
        ClassLoader baseline = MemJava.compile(original);
        Map<String, String> mutant = new TreeMap<>(original);
        mutant.put(FIXTURE, mutant.get(FIXTURE).replace(
                "return a + b;", "return a - b;"));
        ClassLoader changed = MemJava.compile(mutant);
        assertNotEquals(invoke(baseline, 9, 4, "baaac"),
                invoke(changed, 9, 4, "baaac"));
    }

    private static String invoke(ClassLoader loader, int a, int b, String input)
            throws Exception {
        Method run = loader.loadClass("lab.RegexConsumer")
                .getMethod("run", int.class, int.class, String.class);
        return (String) run.invoke(null, a, b, input);
    }

    private static List<String> surface(ClassLoader loader, String name) throws Exception {
        return java.util.Arrays.stream(loader.loadClass(name).getDeclaredMethods())
                .map(method -> method.getName() + java.util.Arrays.toString(method.getParameterTypes())
                        + method.getReturnType().getName())
                .sorted()
                .toList();
    }

    private static Map<String, String> apply(Recipe recipe, Map<String, String> project) {
        ExecutionContext context = new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
        List<Parser.Input> inputs = project.entrySet().stream()
                .map(entry -> Parser.Input.fromString(Path.of(entry.getKey()), entry.getValue()))
                .toList();
        List<SourceFile> parsed;
        try (var stream = JavaParser.fromJavaVersion().build().parseInputs(inputs, null, context)) {
            parsed = stream.toList();
        }
        assertEquals(project.size(), parsed.size());
        parsed.forEach(file -> {
            assertTrue(file instanceof J.CompilationUnit);
            Assertions.validateTypes(file, TypeValidation.all());
        });
        var run = recipe.run(new InMemoryLargeSourceSet(parsed), context, 1);
        Map<String, String> result = new TreeMap<>(project);
        run.getChangeset().getAllResults().forEach(change -> {
            assertTrue(change.getBefore() != null && change.getAfter() != null);
            String path = change.getAfter().getSourcePath().toString();
            assertTrue(project.containsKey(path), path);
            result.put(path, change.getAfter().printAll());
        });
        return Map.copyOf(result);
    }

    private static Map<String, String> project() {
        return Map.of(
                FIXTURE, """
                        package lab;
                        public final class Fixture {
                            private Fixture() {}
                            private static int pure(int a, int b) { return a + b; }
                            public static int combine(int a, int b) { return pure(a, b); }
                        }
                        """,
                "lab/RegexConsumer.java", """
                        package lab;
                        import java.util.regex.Matcher;
                        import java.util.regex.Pattern;
                        public final class RegexConsumer {
                            private RegexConsumer() {}
                            public static String run(int a, int b, String input) {
                                Pattern pattern = Pattern.compile("a+|b+");
                                Matcher matcher = pattern.matcher(input);
                                String first = matcher.find() ? matcher.group() : "-";
                                return Fixture.combine(a, b) + ":" +
                                        pattern.matcher(input).replaceAll("X") + ":" + first;
                            }
                        }
                        """,
                "lab/Controls.java", """
                        package lab;
                        public final class Controls {
                            private static int state;
                            private Controls() {}
                            private static int field(int a, int b) { return state + a + b; }
                            private static int sideEffect(int a, int b) { return state++ + a + b; }
                            public static int unchanged(int a, int b) {
                                return field(a, b) + sideEffect(a, b);
                            }
                        }
                        """);
    }
}