// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.atom;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
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
 * Original destination-only receiving fixture using the retained scalar-int owners and MemJava.
 * This bounded tooling proof grants no Synexia export, whole-module, JNI, VM or JDK admission.
 */
final class JccReceivingFixtureTest {
    private static final String MATH = "receiving/Arithmetic.java";
    private static final String OBSERVER = "receiving/Observer.java";
    private static final List<int[]> INPUTS = inputs();
    private static final Map<String, String> ORIGINAL = project();

    private record Schedule(String id, List<Supplier<Recipe>> steps) {}

    private static List<Schedule> schedules() {
        return List.of(
                new Schedule("A", List.of(M3AtomizePureIntReturnRecipe::new)),
                new Schedule("P", List.of(M3PatternizePureIntAtomRecipe::new)),
                new Schedule("AP", List.of(M3AtomizePureIntReturnRecipe::new,
                        M3PatternizePureIntAtomRecipe::new)),
                new Schedule("PA", List.of(M3PatternizePureIntAtomRecipe::new,
                        M3AtomizePureIntReturnRecipe::new)));
    }

    @Test
    void eachScheduleCompilesAndMatchesFrozenOraclesAtEveryIntermediate() throws Exception {
        Map<String, String> sealed = Map.copyOf(ORIGINAL);
        ClassLoader baseline = MemJava.compile(sealed);
        Map<String, Map<String, String>> results = new LinkedHashMap<>();
        for (Schedule schedule : schedules()) {
            Map<String, String> current = sealed;
            boolean firstSweepChanged = false;
            for (Supplier<Recipe> factory : schedule.steps()) {
                Map<String, String> next = apply(factory, current);
                firstSweepChanged |= !next.equals(current);
                verify(baseline, next);
                current = next;
            }
            assertTrue(firstSweepChanged, schedule.id() + " must exercise its admitted leaf");
            Map<String, String> fixed = current;
            // A fresh factory, parser and context at each step must witness a completely quiet sweep.
            for (Supplier<Recipe> factory : schedule.steps()) {
                Map<String, String> next = apply(factory, current);
                verify(baseline, next);
                assertEquals(current, next, schedule.id() + " intermediate second-pass drift");
                current = next;
            }
            assertEquals(fixed, current);
            results.put(schedule.id(), current);
        }
        assertEquals(results.get("AP"), results.get("PA"));
        assertNotEquals(results.get("A"), results.get("P"));
        assertNotEquals(results.get("A"), results.get("AP"), "P must mark the pre-existing atom");
        assertEquals(4, occurrences(results.get("A").get(MATH), "int m3$pureIntAtom ="));
        assertEquals(3, occurrences(results.get("A").get(MATH), "M3-IOP: PURE_INT_EXPRESSION"));
        assertEquals(1, occurrences(results.get("P").get(MATH), "int m3$pureIntAtom ="));
        assertEquals(1, occurrences(results.get("P").get(MATH), "M3-IOP: PURE_INT_EXPRESSION"));
        assertEquals(4, occurrences(results.get("AP").get(MATH), "M3-IOP: PURE_INT_EXPRESSION"));
        assertEquals(sealed, ORIGINAL, "The original must remain the frozen oracle input");
    }

    @Test
    void actualCanonicalCompositeAgreesWithItsDeclaredChildrenAndFreshReplay() throws Exception {
        ClassLoader baseline = MemJava.compile(ORIGINAL);
        List<Recipe> children = new M3PureIntConvergenceRecipe().getRecipeList();
        assertEquals(List.of(M3AtomizePureIntReturnRecipe.class,
                        M3PatternizePureIntAtomRecipe.class, M3DocumentPureIntAtomRecipe.class),
                children.stream().map(Recipe::getClass).toList());
        Map<String, String> current = ORIGINAL;
        for (Supplier<Recipe> factory : List.<Supplier<Recipe>>of(
                M3AtomizePureIntReturnRecipe::new, M3PatternizePureIntAtomRecipe::new,
                M3DocumentPureIntAtomRecipe::new)) {
            current = apply(factory, current);
            verify(baseline, current);
        }
        Map<String, String> composite = apply(M3PureIntConvergenceRecipe::new, ORIGINAL);
        verify(baseline, composite);
        assertEquals(current, composite);
        assertEquals(4, occurrences(composite.get(MATH), "M3-ATOM: m3$pureIntAtom; Pattern/IOP:"));
        Map<String, String> replay = apply(M3PureIntConvergenceRecipe::new, composite);
        verify(baseline, replay);
        assertEquals(composite, replay);
        var ascending = new LinkedHashMap<String, String>();
        var descending = new LinkedHashMap<String, String>();
        ORIGINAL.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> ascending.put(entry.getKey(), entry.getValue()));
        ORIGINAL.entrySet().stream().sorted(Map.Entry.<String, String>comparingByKey().reversed())
                .forEach(entry -> descending.put(entry.getKey(), entry.getValue()));
        assertEquals(List.of(MATH, OBSERVER), new ArrayList<>(ascending.keySet()));
        assertEquals(List.of(OBSERVER, MATH), new ArrayList<>(descending.keySet()));
        assertNotEquals(new ArrayList<>(ascending.keySet()), new ArrayList<>(descending.keySet()));
        Map<String, String> ascendingResult = apply(M3PureIntConvergenceRecipe::new, ascending);
        verify(baseline, ascendingResult);
        Map<String, String> descendingResult = apply(M3PureIntConvergenceRecipe::new, descending);
        verify(baseline, descendingResult);
        assertEquals(composite, ascendingResult);
        assertEquals(ascendingResult, descendingResult);
    }

    @Test
    void opaqueStringObserversAndIndependentClassStateRemainIntact() throws Exception {
        Map<String, String> observerOnly = Map.of(OBSERVER, ORIGINAL.get(OBSERVER));
        assertEquals(observerOnly, apply(M3PureIntConvergenceRecipe::new, observerOnly));
        ClassLoader first = MemJava.compile(observerOnly);
        ClassLoader second = MemJava.compile(observerOnly);
        assertNotSame(first, second);
        var one = first.loadClass("receiving.Observer");
        var two = second.loadClass("receiving.Observer");
        assertNotSame(one, two);
        assertEquals(1, one.getMethod("next").invoke(null));
        assertEquals(2, one.getMethod("next").invoke(null));
        assertEquals(1, two.getMethod("next").invoke(null));
        String[] values = (String[]) one.getMethod("strings", int.class).invoke(null, 0);
        assertNull(values[0]);
        assertEquals("null", values[1]);
        assertNotEquals(values[0], values[1]);
        assertTrue(values[2].contains("private static int fake"));
        assertTrue(values[3].contains("M3-IOP: PURE_INT_EXPRESSION"));
        assertEquals(0, values[4].length());
        assertEquals(4, values[5].length());
        assertEquals(3, values[5].codePointCount(0, values[5].length()));
    }

    @Test
    void compilerAndBothObservationOraclesRejectCuratedMutants() throws Exception {
        ClassLoader baseline = MemJava.compile(ORIGINAL);
        var malformed = assertThrows(IllegalArgumentException.class,
                () -> MemJava.compile(Map.of(MATH, "package receiving; public class Arithmetic {")));
        assertTrue(malformed.getMessage().contains("JAVA21_COMPILE_REFUSED"));
        var numeric = new TreeMap<>(ORIGINAL);
        numeric.put(MATH, ORIGINAL.get(MATH).replace("return a + b;", "return a - b;"));
        assertNotEquals(ORIGINAL, numeric);
        assertThrows(AssertionError.class, () -> verify(baseline, numeric));
        var opaque = new TreeMap<>(ORIGINAL);
        opaque.put(OBSERVER, ORIGINAL.get(OBSERVER).replace("\"null\"", "\"changed\""));
        assertNotEquals(ORIGINAL, opaque);
        // Compile this valid mutation first so the observer rejection is not a compiler failure.
        ClassLoader mutant = MemJava.compile(opaque);
        assertThrows(AssertionError.class, () -> observations(baseline, mutant));
    }

    private static Map<String, String> apply(Supplier<Recipe> factory, Map<String, String> input) {
        Recipe recipe = factory.get();
        assertNotSame(recipe, factory.get(), "Each invocation requires an independent recipe instance");
        var context = new InMemoryExecutionContext(failure -> {
            throw new AssertionError("Destination recipe/parser failure", failure);
        });
        List<Parser.Input> inputs = input.entrySet().stream()
                .map(entry -> Parser.Input.fromString(Path.of(entry.getKey()), entry.getValue())).toList();
        List<SourceFile> sources;
        try (var stream = JavaParser.fromJavaVersion().build().parseInputs(inputs, null, context)) {
            sources = stream.toList();
        }
        assertEquals(input.size(), sources.size());
        for (SourceFile source : sources) {
            assertTrue(source instanceof J.CompilationUnit);
            Assertions.validateTypes(source, TypeValidation.all());
            assertEquals(input.get(source.getSourcePath().toString()), source.printAll());
        }
        var result = new TreeMap<>(input);
        for (var change : recipe.run(new InMemoryLargeSourceSet(sources), context, 1)
                .getChangeset().getAllResults()) {
            assertTrue(change.getBefore() != null && change.getAfter() != null);
            SourceFile after = change.getAfter();
            assertEquals(change.getBefore().getSourcePath(), after.getSourcePath());
            String path = after.getSourcePath().toString();
            assertTrue(input.containsKey(path));
            Assertions.validateTypes(after, TypeValidation.all());
            result.put(path, after.printAll());
        }
        assertEquals(input.keySet(), result.keySet());
        return Map.copyOf(result);
    }

    private static void verify(ClassLoader baseline, Map<String, String> candidate) throws Exception {
        ClassLoader compiled = MemJava.compile(candidate);
        assertNotSame(baseline, compiled);
        observations(baseline, compiled);
        for (String name : List.of("receiving.Arithmetic", "receiving.Observer"))
            assertEquals(surface(baseline.loadClass(name)), surface(compiled.loadClass(name)), name);
        assertEquals(ORIGINAL.get(OBSERVER), candidate.get(OBSERVER), "Opaque observer source must be exact");
    }

    private static void observations(ClassLoader baseline, ClassLoader candidate) throws Exception {
        var beforeMath = baseline.loadClass("receiving.Arithmetic").getMethod("values", int.class, int.class);
        var afterMath = candidate.loadClass("receiving.Arithmetic").getMethod("values", int.class, int.class);
        for (int[] pair : INPUTS) {
            int a = pair[0];
            int b = pair[1];
            int[] independent = {(int) ((long) a + b), Integer.rotateLeft(a, b),
                    (int) ((~(long) a) * 17L + b - 3L), (int) ((long) a - b)};
            int[] frozen = (int[]) beforeMath.invoke(null, a, b);
            assertArrayEquals(independent, frozen);
            assertArrayEquals(frozen, (int[]) afterMath.invoke(null, a, b));
        }
        var beforeStrings = baseline.loadClass("receiving.Observer").getMethod("strings", int.class);
        var afterStrings = candidate.loadClass("receiving.Observer").getMethod("strings", int.class);
        for (int choice : List.of(0, 1, -1))
            assertArrayEquals((String[]) beforeStrings.invoke(null, choice),
                    (String[]) afterStrings.invoke(null, choice));
    }

    private static List<String> surface(Class<?> owner) {
        var entries = new ArrayList<String>();
        for (var method : owner.getDeclaredMethods()) entries.add("method " + method.getName()
                + " " + Modifier.toString(method.getModifiers()) + " " + method.getReturnType().descriptorString()
                + Arrays.stream(method.getParameterTypes()).map(Class::descriptorString).toList());
        for (var field : owner.getDeclaredFields()) entries.add("field " + field.getName()
                + " " + Modifier.toString(field.getModifiers()) + " " + field.getType().descriptorString());
        for (var constructor : owner.getDeclaredConstructors()) entries.add("constructor "
                + Modifier.toString(constructor.getModifiers())
                + Arrays.stream(constructor.getParameterTypes()).map(Class::descriptorString).toList());
        return entries.stream().sorted().toList();
    }

    private static List<int[]> inputs() {
        int[] edges = {Integer.MIN_VALUE, -33, -1, 0, 1, 31, 32, 33, Integer.MAX_VALUE};
        var inputs = new ArrayList<int[]>();
        for (int left : edges) for (int right : edges) inputs.add(new int[]{left, right});
        var random = new Random(0x52454345495645L);
        for (int i = 0; i < 32; i++) inputs.add(new int[]{random.nextInt(), random.nextInt()});
        return List.copyOf(inputs);
    }

    private static int occurrences(String source, String text) {
        return (source.length() - source.replace(text, "").length()) / text.length();
    }

    private static Map<String, String> project() {
        return Map.of(MATH, """
                package receiving;
                public final class Arithmetic {
                    private Arithmetic() {}
                    public static int[] values(int a, int b) {
                        return new int[] {sum(a, b), rotate(a, b), mix(a, b), seeded(a, b)};
                    }
                    private static int sum(int a, int b) { return a + b; }
                    private static int rotate(int a, int b) { return (a << b) | (a >>> -b); }
                    private static int mix(int a, int b) { return (~a * 17) + (b - 3); }
                    private static int seeded(int a, int b) {
                        int m3$pureIntAtom = a - b;
                        return m3$pureIntAtom;
                    }
                }
                """, OBSERVER, """
                package receiving;
                public final class Observer {
                    private static int calls;
                    private static final String CODE = "private static int fake(int a, int b) { return a + b; }";
                    private static final String MARKER = "M3-IOP: PURE_INT_EXPRESSION /* return x+y; */";
                    private Observer() {}
                    public static int next() { return ++calls; }
                    public static String[] strings(int choice) {
                        return new String[] {choice == 0 ? null : "value", "null", CODE, MARKER, "", "A😀Z"};
                    }
                }
                """);
    }
}
