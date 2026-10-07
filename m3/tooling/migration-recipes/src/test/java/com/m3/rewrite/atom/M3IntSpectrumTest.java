// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite.atom;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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

/** Qualification of real recipe permutations against frozen behavior and contract oracles. */
final class M3IntSpectrumTest {
    private static final int[][] ORDERS = {{0, 1, 2}, {0, 2, 1}, {1, 0, 2},
            {1, 2, 0}, {2, 0, 1}, {2, 1, 0}};

    @Test
    void allSixOrdersPreserveEveryIntermediateAndReachOneNormalForm() throws Exception {
        Map<String, String> original = SpectrumCases.project();
        String frozen = seal(original);
        ClassLoader baseline = MemJava.compile(original);
        equivalent(baseline, baseline);
        Map<String, String> canonical = null;
        StringBuilder receipt = new StringBuilder("order\tsweep\tleaf\tbefore_sha256\tafter_sha256\tproof\n");
        int checks = 0;
        for (int[] order : ORDERS) {
            Map<String, String> current = original;
            boolean converged = false;
            for (int sweep = 0; sweep < 4; sweep++) {
                boolean changed = false;
                for (int leaf : order) {
                    Recipe recipe = new M3PureIntConvergenceRecipe().getRecipeList().get(leaf);
                    String before = seal(current);
                    Map<String, String> candidate = apply(recipe, current);
                    equivalent(baseline, MemJava.compile(candidate));
                    checks++;
                    assertEquals(original.get("spectrum/Effects.java"), candidate.get("spectrum/Effects.java"));
                    assertEquals(frozen, seal(original), "immutable original cannot become a moving oracle");
                    String after = seal(candidate);
                    changed |= !candidate.equals(current);
                    receipt.append(Arrays.toString(order)).append('\t').append(sweep).append('\t')
                            .append(leaf).append('\t').append(before).append('\t').append(after)
                            .append("\tcompiler_runtime_surface\n");
                    current = candidate;
                }
                if (!changed) { converged = true; break; }
            }
            assertTrue(converged, "PASS_LIMIT:" + Arrays.toString(order));
            assertNotEquals(original, current, "all-no-op recipes cannot qualify the positive corpus");
            assertEquals(SpectrumCases.EXPRESSIONS, current.values().stream()
                    .mapToInt(s -> occurrences(s, "int m3$pureIntAtom =")).sum());
            if (canonical == null) canonical = current;
            else assertEquals(canonical, current, "recipe order must not change the normal form");
            assertEquals(current, apply(new M3PureIntConvergenceRecipe(), current));
        }
        Path out = Path.of("target/m3-spectrum");
        Files.createDirectories(out);
        Files.writeString(out.resolve("PERMUTATIONS.tsv"), receipt, StandardCharsets.UTF_8);
        Files.writeString(out.resolve("COUNTS.tsv"),
                "expressions\tinput_pairs\torders\tintermediate_checks\toriginal_sha256\tcanonical_sha256\n"
                + SpectrumCases.EXPRESSIONS + "\t225\t6\t" + checks + "\t" + frozen + "\t"
                + seal(canonical) + "\n", StandardCharsets.UTF_8);
    }

    @Test
    void compilerAndFrozenOracleKillCuratedBadCandidates() throws Exception {
        Map<String, String> original = SpectrumCases.project();
        Map<String, String> mutant = new TreeMap<>(original);
        String path = "spectrum/Shape0.java";
        String before = mutant.get(path);
        String after = before.replace("return (a + b) + (a ^ b);", "return (a - b) + (a ^ b);");
        assertNotEquals(before, after);
        mutant.put(path, after);
        ClassLoader baseline = MemJava.compile(original);
        ClassLoader changed = MemJava.compile(mutant);
        assertThrows(AssertionError.class, () -> equivalent(baseline, changed));
        assertThrows(IllegalArgumentException.class,
                () -> MemJava.compile(Map.of("spectrum/Broken.java", "package spectrum; class Broken {")));
        Map<String, String> apiMutation = new TreeMap<>(original);
        apiMutation.put(path, before.replace("public final class Shape0", "public class Shape0"));
        ClassLoader changedApi = MemJava.compile(apiMutation);
        assertThrows(AssertionError.class, () -> equivalent(baseline, changedApi));
    }

    @Test
    void standaloneOpaqueSourceRemainsByteIdentical() {
        Map<String, String> source = Map.of("spectrum/Effects.java", SpectrumCases.controls());
        MemJava.compile(source);
        assertEquals(source, apply(new M3PureIntConvergenceRecipe(), source));
    }

    @Test
    void atomizationRetainsInlineSemanticMemory() {
        String source = """
                package spectrum;
                final class Comments {
                    private static int value(int a, int b) /* BODY_CONTRACT */ {
                        // ENTRY_CONTRACT: arguments are machine integers.
                        return /* OPERAND_CONTRACT */ a /* LEFT_CONTRACT */ + /* RIGHT_CONTRACT */ b /* TAIL_CONTRACT */;
                        // EXIT_CONTRACT: no state has changed.
                    }
                }
                """;
        for (String variant : List.of(source, source.replace("/* TAIL_CONTRACT */;", "// TAIL_CONTRACT\n        ;"))) {
            Map<String, String> original = Map.of("spectrum/Comments.java", variant);
            Map<String, String> candidate = apply(new M3AtomizePureIntReturnRecipe(), original);
            MemJava.compile(candidate);
            String after = candidate.get("spectrum/Comments.java");
            for (String marker : List.of("BODY_CONTRACT", "ENTRY_CONTRACT", "OPERAND_CONTRACT",
                    "LEFT_CONTRACT", "RIGHT_CONTRACT", "TAIL_CONTRACT", "EXIT_CONTRACT")) {
                assertEquals(1, occurrences(after, marker), marker);
            }
            Map<String, String> completed = apply(new M3PureIntConvergenceRecipe(), candidate);
            assertEquals(completed, apply(new M3PureIntConvergenceRecipe(), completed));
        }
    }

    static Map<String, String> apply(Recipe recipe, Map<String, String> project) {
        var context = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        List<Parser.Input> inputs = new TreeMap<>(project).entrySet().stream()
                .map(e -> Parser.Input.fromString(Path.of(e.getKey()), e.getValue())).toList();
        List<SourceFile> parsed;
        try (var stream = JavaParser.fromJavaVersion().build().parseInputs(inputs, null, context)) {
            parsed = stream.toList();
        }
        assertEquals(project.size(), parsed.size());
        for (SourceFile file : parsed) {
            assertTrue(file instanceof J.CompilationUnit);
            Assertions.validateTypes(file, TypeValidation.all());
            assertEquals(project.get(file.getSourcePath().toString()), file.printAll());
        }
        var run = recipe.run(new InMemoryLargeSourceSet(parsed), context, 1);
        Map<String, String> result = new TreeMap<>(project);
        run.getChangeset().getAllResults().forEach(change -> {
            assertTrue(change.getBefore() != null && change.getAfter() != null);
            assertEquals(change.getBefore().getSourcePath(), change.getAfter().getSourcePath());
            Assertions.validateTypes(change.getAfter(), TypeValidation.all());
            String path = change.getAfter().getSourcePath().toString();
            assertTrue(project.containsKey(path));
            result.put(path, change.getAfter().printAll());
        });
        return Map.copyOf(result);
    }

    private static void equivalent(ClassLoader baseline, ClassLoader candidate) throws Exception {
        for (int variant = 0; variant < 4; variant++) {
            String name = "spectrum.Shape" + variant;
            Class<?> left = baseline.loadClass(name);
            Class<?> right = candidate.loadClass(name);
            assertEquals(surface(left), surface(right), "class and member surface: " + name);
            var l = left.getMethod("values", int.class, int.class);
            var r = right.getMethod("values", int.class, int.class);
            for (int a : SpectrumCases.EDGES) for (int b : SpectrumCases.EDGES) {
                int[] expected = (int[]) l.invoke(null, a, b);
                assertArrayEquals(SpectrumCases.expected(variant, a, b), expected, "independent oracle");
                assertArrayEquals(expected, (int[]) r.invoke(null, a, b));
            }
        }
        var l = baseline.loadClass("spectrum.Effects").getMethod("trace", int.class, int.class);
        var r = candidate.loadClass("spectrum.Effects").getMethod("trace", int.class, int.class);
        assertEquals(surface(l.getDeclaringClass()), surface(r.getDeclaringClass()));
        for (int a : SpectrumCases.EDGES) for (int b : SpectrumCases.EDGES) {
            assertEquals(l.invoke(null, a, b), r.invoke(null, a, b), "effects and exception trace");
        }
    }

    private static List<String> surface(Class<?> type) {
        List<String> members = new ArrayList<>();
        members.add("class " + type.getName() + " " + Modifier.toString(type.getModifiers())
                + " " + type.getGenericSuperclass() + " " + Arrays.toString(type.getGenericInterfaces())
                + " " + Arrays.toString(type.getTypeParameters()) + " " + Arrays.toString(type.getDeclaredAnnotations()));
        Arrays.stream(type.getDeclaredMethods()).forEach(m -> members.add("method " + m.toGenericString()
                + " " + Arrays.toString(m.getDeclaredAnnotations()) + " " + Arrays.toString(m.getExceptionTypes())));
        Arrays.stream(type.getDeclaredFields()).forEach(f -> members.add("field " + f.toGenericString()
                + " " + Arrays.toString(f.getDeclaredAnnotations())));
        Arrays.stream(type.getDeclaredConstructors()).forEach(c -> members.add("constructor " + c.toGenericString()
                + " " + Arrays.toString(c.getDeclaredAnnotations()) + " " + Arrays.toString(c.getExceptionTypes())));
        return members.stream().sorted().toList();
    }

    private static String seal(Map<String, String> sources) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (var entry : new TreeMap<>(sources).entrySet()) {
            byte[] path = entry.getKey().getBytes(StandardCharsets.UTF_8);
            byte[] text = entry.getValue().getBytes(StandardCharsets.UTF_8);
            digest.update(Integer.toString(path.length).getBytes(StandardCharsets.US_ASCII));
            digest.update((byte) ':'); digest.update(path);
            digest.update(Integer.toString(text.length).getBytes(StandardCharsets.US_ASCII));
            digest.update((byte) ':'); digest.update(text);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static int occurrences(String text, String token) {
        return (text.length() - text.replace(token, "").length()) / token.length();
    }
}
