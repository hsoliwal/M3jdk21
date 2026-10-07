// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.atom;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.IntBinaryOperator;
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

/** Frozen-oracle, in-memory multi-file regression laboratory for the actual Java21 recipe owners. */
final class M3IntRecipeProjectTest {
    private static final String FIXTURE = "lab/Fixture.java";
    private static final String[] OPERATORS = {"+", "-", "*", "<<", ">>", ">>>", "&", "|", "^"};
    private static final IntBinaryOperator[] ORACLES = {
        (a, b) -> a + b, (a, b) -> a - b, (a, b) -> a * b,
        (a, b) -> a << b, (a, b) -> a >> b, (a, b) -> a >>> b,
        (a, b) -> a & b, (a, b) -> a | b, (a, b) -> a ^ b
    };
    private static final int VARIANTS = 6;
    private static final int[][] INPUTS = inputs();

    @Test
    void actualRecipesConvergeAcrossBothOrdersWithEveryIntermediateComparedToFrozenOriginal()
            throws Exception {
        Map<String, String> original = project();
        Map<String, String> sealed = Map.copyOf(original);
        ClassLoader baseline = MemJava.compile(original);
        assertEquivalent(baseline, baseline);
        Map<String, String> canonical = converge(original, baseline, false, 3);
        Map<String, String> alternate = converge(original, baseline, true, 3);
        assertEquals(canonical, alternate, "admitted orderings must have one normal form");
        assertEquals(sealed, original, "the original must not become its own moving oracle");
        assertNotEquals(original.get(FIXTURE), canonical.get(FIXTURE));
        for (var entry : original.entrySet()) {
            if (!entry.getKey().equals(FIXTURE)) {
                assertEquals(entry.getValue(), canonical.get(entry.getKey()), entry.getKey());
            }
        }
        String result = canonical.get(FIXTURE);
        assertEquals(54, occurrences(result, "int m3$pureIntAtom ="));
        assertEquals(54, occurrences(result, "M3-IOP: PURE_INT_EXPRESSION"));
        assertEquals(54, occurrences(result, "M3-ATOM: m3$pureIntAtom; Pattern/IOP:"));
        assertEquals(canonical, apply(new M3PureIntConvergenceRecipe(), canonical));
        writeReceipt(original, canonical);
    }

    @Test
    void projectEnumerationOrderDoesNotChangeTheCompleteCompositeResult() {
        Map<String, String> original = project();
        Map<String, String> reversed = new LinkedHashMap<>();
        original.entrySet().stream().sorted(Map.Entry.<String, String>comparingByKey().reversed())
                .forEach(entry -> reversed.put(entry.getKey(), entry.getValue()));
        assertEquals(apply(new M3PureIntConvergenceRecipe(), original),
                apply(new M3PureIntConvergenceRecipe(), reversed));
    }

    @Test
    void oneSweepBudgetIsNotAReplayWitness() throws Exception {
        Map<String, String> original = project();
        var failure = assertThrows(IllegalStateException.class,
                () -> converge(original, MemJava.compile(original), false, 1));
        assertEquals("PASS_LIMIT", failure.getMessage());
    }

    @Test
    void compilerAndDifferentialOracleRejectDeliberatelyBadCandidates() throws Exception {
        var compilation = assertThrows(IllegalArgumentException.class,
                () -> MemJava.compile(Map.of("lab/Broken.java", "package lab; class Broken {")));
        assertTrue(compilation.getMessage().contains("JAVA21_COMPILE_REFUSED"));
        Map<String, String> original = project();
        Map<String, String> bad = new TreeMap<>(original);
        String mutated = bad.get(FIXTURE).replace("return a + b;", "return a - b;");
        assertNotEquals(bad.get(FIXTURE), mutated, "the curated mutant must actually change code");
        bad.put(FIXTURE, mutated);
        ClassLoader baseline = MemJava.compile(original);
        ClassLoader mutant = MemJava.compile(bad);
        assertThrows(AssertionError.class, () -> assertEquivalent(baseline, mutant));
        assertThrows(ClassNotFoundException.class, () -> baseline.loadClass("lab.NotGenerated"));
        assertThrows(IllegalArgumentException.class, () -> MemJava.compile(Map.of()));
    }

    @Test
    void stateExceptionsAliasesAndLexicalLookalikesStayOutsideThePureLeafDomain() {
        Map<String, String> controls = Map.of("lab/Controls.java", controls());
        assertEquals(controls, apply(new M3PureIntConvergenceRecipe(), controls));
        String text = controls.get("lab/Controls.java");
        assertTrue(text.contains("private static int mutation"));
        assertTrue(text.contains("M3-IOP: PURE_INT_EXPRESSION"));
        assertFalse(text.contains("int m3$pureIntAtom ="));
    }

    @Test
    void arrayDeclaratorsAndVarargsAreNotScalarIntParameters() {
        for (String parameters : List.of("int b, int unused[]", "int b, int... unused",
                "int b, int[] unused", "int b, Integer unused")) {
            String source = "package lab; final class Shapes { private static int f("
                    + parameters + ") { return b + 1; } }\n";
            Map<String, String> project = Map.of("lab/Shapes.java", source);
            MemJava.compile(project);
            assertEquals(project, apply(new M3PureIntConvergenceRecipe(), project), parameters);
        }
    }

    private static Map<String, String> converge(Map<String, String> original,
            ClassLoader baseline, boolean patternFirst, int budget) throws Exception {
        Map<String, String> current = original;
        for (int pass = 0; pass < budget; pass++) {
            Map<String, String> prior = current;
            boolean anyChange = false;
            List<Recipe> recipes = new ArrayList<>(new M3PureIntConvergenceRecipe().getRecipeList());
            if (patternFirst) java.util.Collections.swap(recipes, 0, 1);
            for (Recipe recipe : recipes) {
                Map<String, String> candidate = apply(recipe, current);
                anyChange |= !candidate.equals(current);
                assertEquivalent(baseline, MemJava.compile(candidate));
                current = candidate;
            }
            if (!anyChange) return current;
            if (prior.equals(current)) throw new IllegalStateException("CANCELLING_CHANGES");
        }
        throw new IllegalStateException("PASS_LIMIT");
    }

    private static Map<String, String> apply(Recipe recipe, Map<String, String> project) {
        var context = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        List<Parser.Input> inputs = project.entrySet().stream()
                .map(entry -> Parser.Input.fromString(Path.of(entry.getKey()), entry.getValue()))
                .toList();
        List<SourceFile> parsed;
        try (var stream = JavaParser.fromJavaVersion().build().parseInputs(inputs, null, context)) {
            parsed = stream.toList();
        }
        assertEquals(project.size(), parsed.size());
        parsed.forEach(file -> {
            assertTrue(file instanceof J.CompilationUnit, file.getSourcePath().toString());
            Assertions.validateTypes(file, TypeValidation.all());
            assertEquals(project.get(file.getSourcePath().toString()), file.printAll());
        });
        var run = recipe.run(new InMemoryLargeSourceSet(parsed), context, 1);
        Map<String, String> result = new TreeMap<>(project);
        run.getChangeset().getAllResults().forEach(change -> {
            assertTrue(change.getBefore() != null && change.getAfter() != null,
                    "FILE recipes cannot create or delete owners");
            SourceFile after = change.getAfter();
            assertEquals(change.getBefore().getSourcePath(), after.getSourcePath());
            Assertions.validateTypes(after, TypeValidation.all());
            String path = after.getSourcePath().toString();
            assertTrue(project.containsKey(path), path);
            result.put(path, after.printAll());
        });
        return Map.copyOf(result);
    }

    private static void assertEquivalent(ClassLoader baseline, ClassLoader candidate) throws Exception {
        Class<?> before = baseline.loadClass("lab.Probe");
        Class<?> after = candidate.loadClass("lab.Probe");
        var left = before.getMethod("run", int.class, int.class);
        var right = after.getMethod("run", int.class, int.class);
        var leftTrace = baseline.loadClass("lab.Controls").getMethod("trace", int.class, int.class);
        var rightTrace = candidate.loadClass("lab.Controls").getMethod("trace", int.class, int.class);
        for (int[] pair : INPUTS) {
            int[] expected = (int[]) left.invoke(null, pair[0], pair[1]);
            assertArrayEquals(expectedValues(pair[0], pair[1]), expected, "independent numeric model");
            assertArrayEquals(expected, (int[]) right.invoke(null, pair[0], pair[1]));
            assertEquals(leftTrace.invoke(null, pair[0], pair[1]),
                    rightTrace.invoke(null, pair[0], pair[1]), "effect/exception/callback trace");
        }
        for (String name : List.of("lab.Fixture", "lab.Probe", "lab.Controls")) {
            assertEquals(surface(baseline.loadClass(name)), surface(candidate.loadClass(name)), name);
        }
    }

    private static List<String> surface(Class<?> type) {
        List<String> members = new ArrayList<>();
        Arrays.stream(type.getDeclaredMethods()).forEach(method -> members.add(
                "method " + Modifier.toString(method.getModifiers()) + " " + method.getName()
                        + " " + method.getReturnType().descriptorString()
                        + Arrays.stream(method.getParameterTypes()).map(Class::descriptorString).toList()));
        Arrays.stream(type.getDeclaredFields()).forEach(field -> members.add(
                "field " + Modifier.toString(field.getModifiers()) + " " + field.getName()
                        + " " + field.getType().descriptorString()));
        Arrays.stream(type.getDeclaredConstructors()).forEach(constructor -> members.add(
                "constructor " + Modifier.toString(constructor.getModifiers())
                        + Arrays.stream(constructor.getParameterTypes()).map(Class::descriptorString).toList()));
        return members.stream().sorted().toList();
    }

    private static Map<String, String> project() {
        StringBuilder methods = new StringBuilder();
        List<String> calls = new ArrayList<>();
        for (int operator = 0; operator < OPERATORS.length; operator++) {
            for (int variant = 0; variant < VARIANTS; variant++) {
                String name = "f" + (operator * VARIANTS + variant);
                methods.append("  private static int ").append(name)
                        .append("(int a, int b) { return ")
                        .append(expression(OPERATORS[operator], variant)).append("; }\n");
                calls.add(name + "(a, b)");
            }
        }
        String fixture = "package lab;\npublic final class Fixture {\n"
                + "  private Fixture() {}\n"
                + "  public static int[] evaluate(int a, int b) { return new int[]{"
                + String.join(",", calls) + "}; }\n" + methods + "}\n";
        return Map.of(FIXTURE, fixture,
                "lab/Probe.java", """
                package lab;
                public final class Probe {
                    private Probe() {}
                    public static int[] run(int a, int b) {
                        java.util.function.IntBinaryOperator add = (x, y) -> x + y;
                        if (add.applyAsInt(a, b) == 0) return Fixture.evaluate(a, b);
                        return Fixture.evaluate(a, b);
                    }
                }
                """, "lab/Controls.java", controls());
    }

    private static String controls() {
        return """
                package lab;
                public final class Controls {
                    private static int state;
                    private static final StringBuilder calls = new StringBuilder();
                    // class Strategy { native int dijkstra(); } M3-IOP: PURE_INT_EXPRESSION
                    private static final String FAKE = "return a + b; /* native while */";
                    private static final String BLOCK =""" + "\"\"\"\n"
                + "private static int fake(int a, int b) { return a + b; }\n"
                + "M3-IOP: PURE_INT_EXPRESSION // \"escaped\" class Factory\n"
                + "\"\"\";\n" + """
                    private Controls() {}
                    private static int divide(int a, int b) { return a / b; }
                    private static int modulo(int a, int b) { return a % b; }
                    private static int mutation(int a, int b) { return a++ + b; }
                    private static int field(int a, int b) { return state + a + b; }
                    private static int reserved(int m3$pureIntAtom, int b) { return m3$pureIntAtom + b; }
                    private static int boxed(Integer a, int b) { return a + b; }
                    private static int cast(long a, int b) { return (int) a + b; }
                    private static int choose(int a, int b) { return a > b ? a : b; }
                    private static int access(int[] a, int b) { return a[b]; }
                    private static int callback(int a, int b) {
                        java.util.function.IntSupplier left = () -> { calls.append('L'); return state++ + a; };
                        java.util.function.IntSupplier right = () -> { calls.append('R'); return state + b; };
                        return left.getAsInt() + right.getAsInt();
                    }
                    private static int nested(int a, int b) {
                        int sum = 0;
                        for (int i = 0; i < 3; i++) {
                            if (a > i) { for (int j = 0; j < 2; j++) sum += b; }
                        }
                        return sum;
                    }
                    public static String trace(int a, int b) {
                        state = 7;
                        calls.setLength(0);
                        int v = callback(a, b) ^ mutation(a, b) ^ field(a, b)
                                ^ reserved(a, b) ^ cast(a, b) ^ choose(a, b) ^ nested(a, b);
                        try { v ^= divide(a, b) ^ modulo(a, b); }
                        catch (ArithmeticException expected) { calls.append('A'); }
                        try { v ^= boxed(null, b); }
                        catch (NullPointerException expected) { calls.append('N'); }
                        try { v ^= access(new int[]{a}, b); }
                        catch (ArrayIndexOutOfBoundsException expected) { calls.append('B'); }
                        return v + ":" + state + ":" + calls + ":" + FAKE + BLOCK;
                    }
                }
                """;
    }

    private static String expression(String operator, int variant) {
        String plain = "a " + operator + " b";
        return switch (variant) {
            case 0 -> plain;
            case 1 -> "(-a) " + operator + " (+b)";
            case 2 -> "(~a) " + operator + " (-(-b))";
            case 3 -> "((a) " + operator + " (b))";
            case 4 -> "(" + plain + ") ^ (a + b)";
            case 5 -> "((" + plain + ") * 31) + ~(-(+a))";
            default -> throw new IllegalArgumentException("variant");
        };
    }

    private static int[] expectedValues(int a, int b) {
        int[] values = new int[OPERATORS.length * VARIANTS];
        for (int i = 0; i < ORACLES.length; i++) {
            IntBinaryOperator op = ORACLES[i];
            int plain = op.applyAsInt(a, b);
            values[i * VARIANTS] = plain;
            values[i * VARIANTS + 1] = op.applyAsInt(-a, +b);
            values[i * VARIANTS + 2] = op.applyAsInt(~a, -(-b));
            values[i * VARIANTS + 3] = plain;
            values[i * VARIANTS + 4] = plain ^ (a + b);
            values[i * VARIANTS + 5] = plain * 31 + ~(-(+a));
        }
        return values;
    }

    private static int[][] inputs() {
        int[] edges = {Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -65536, -33, -32, -1,
                0, 1, 2, 31, 32, 33, 255, 65536, Integer.MAX_VALUE};
        List<int[]> cases = new ArrayList<>();
        for (int a : edges) for (int b : edges) cases.add(new int[]{a, b});
        Random random = new Random(0x4d334a444b21L);
        for (int i = 0; i < 128; i++) cases.add(new int[]{random.nextInt(), random.nextInt()});
        return cases.toArray(int[][]::new);
    }

    private static int occurrences(String text, String needle) {
        return (text.length() - text.replace(needle, "").length()) / needle.length();
    }

    private static void writeReceipt(Map<String, String> before, Map<String, String> after)
            throws Exception {
        Path report = Path.of("target", "m3-recipe-lab", "PROJECT.tsv");
        Files.createDirectories(report.getParent());
        StringBuilder content = new StringBuilder("path\tbefore_sha256\tafter_sha256\n");
        for (String path : new TreeMap<>(before).keySet()) {
            content.append(path).append('\t').append(hash(before.get(path)))
                    .append('\t').append(hash(after.get(path))).append('\n');
        }
        Files.writeString(report, content, StandardCharsets.UTF_8);
        Files.writeString(report.resolveSibling("COUNTS.tsv"),
                "expression_variants\tinput_pairs\torders\tproof\n54\t" + INPUTS.length
                        + "\t2\tcompiler_behavior_surface_iop_fixed_point\n", StandardCharsets.UTF_8);
    }

    private static String hash(String source) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(source.getBytes(StandardCharsets.UTF_8)));
    }
}
