// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.tools.ToolProvider;
import org.eclipse.nebula.m3.rewrite.atom.NebulaM3AtomizePureIntReturnRecipe;
import org.eclipse.nebula.m3.rewrite.atom.NebulaM3PatternizePureIntAtomRecipe;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.tree.ParseError;

/**
 * Compiler/runtime-driven mastery laboratory for Nebula's retained FILE-local Atomize/Patternize
 * recipes.
 *
 * <p>Generated fixtures mix executable Java with code-looking comments, strings, text blocks and
 * regexes. Every application is reparsed, compiled with Java 21 warnings-as-errors, compared
 * against the original public/protected contract and runtime behavior, and replayed to a textual
 * fixed point. No Nebula widget/product source is an input or output.</p>
 */
public final class NebulaM3A3Lab {
    private static final int MAX_PASSES = 6;
    private static final List<List<String>> SCHEDULES =
            List.of(
                    List.of("A"),
                    List.of("P"),
                    List.of("A", "P"),
                    List.of("P", "A"),
                    List.of("A", "P", "A"),
                    List.of("P", "A", "P"));

    public record Result(
            int fixture,
            String schedule,
            String operationSet,
            String beforeSha256,
            String afterSha256,
            int applications,
            int compiles,
            boolean changed,
            boolean fixedPoint,
            boolean behaviorStable,
            boolean contractStable,
            boolean lexicalDataStable) {
        public Result {
            if (fixture < 0 || applications < 1 || compiles < 1) {
                throw new IllegalArgumentException("invalid mastery result counts");
            }
            schedule = text(schedule, "schedule");
            operationSet = text(operationSet, "operationSet");
            beforeSha256 = sha(beforeSha256, "beforeSha256");
            afterSha256 = sha(afterSha256, "afterSha256");
            if (!fixedPoint || !behaviorStable || !contractStable || !lexicalDataStable) {
                throw new IllegalArgumentException("mastery result did not pass");
            }
        }
    }

    private record Observation(
            List<String> contract,
            List<String> behavior,
            String payload) {}

    private record ResultAndSource(Result result, String source) {}

    private NebulaM3A3Lab() {
        throw new AssertionError("No instances");
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("usage: NebulaM3A3Lab <output-directory>");
        }
        write(Path.of(args[0]));
    }

    public static int fixtureCount() {
        return NebulaM3A3Cases.fixtures().size();
    }

    public static int scheduleCount() {
        return SCHEDULES.size();
    }

    public static List<Result> run() throws Exception {
        ArrayList<Result> results = new ArrayList<>();
        int ordinal = 0;
        for (String fixture : NebulaM3A3Cases.fixtures()) {
            Observation baseline = verify(fixture, ordinal, "baseline", 0);
            NebulaM3A3Cases.Signal signal = NebulaM3A3Cases.signal(fixture);
            Map<String, String> normalForms = new LinkedHashMap<>();
            for (List<String> schedule : SCHEDULES) {
                ResultAndSource outcome =
                        converge(ordinal, fixture, schedule, baseline, signal);
                String prior =
                        normalForms.putIfAbsent(
                                outcome.result().operationSet(), outcome.source());
                if (prior != null && !prior.equals(outcome.source())) {
                    throw new IllegalStateException(
                            "Nebula A3 operation set did not converge: "
                                    + outcome.result().operationSet());
                }
                results.add(outcome.result());
            }
            ordinal++;
        }
        return List.copyOf(results);
    }

    public static List<Result> write(Path output) throws Exception {
        Path out = Objects.requireNonNull(output, "output").toAbsolutePath().normalize();
        Files.createDirectories(out);
        if (Files.isSymbolicLink(out) || !Files.isDirectory(out)) {
            throw new IOException("mastery output is not a regular directory");
        }
        List<Result> results = run();
        StringBuilder tsv =
                new StringBuilder(
                        "fixture\tschedule\toperationSet\tbeforeSha256\tafterSha256"
                                + "\tapplications\tcompiles\tchanged\tfixedPoint"
                                + "\tbehaviorStable\tcontractStable\tlexicalDataStable\n");
        for (Result result : results) {
            tsv.append(result.fixture())
                    .append('\t')
                    .append(cell(result.schedule()))
                    .append('\t')
                    .append(cell(result.operationSet()))
                    .append('\t')
                    .append(result.beforeSha256())
                    .append('\t')
                    .append(result.afterSha256())
                    .append('\t')
                    .append(result.applications())
                    .append('\t')
                    .append(result.compiles())
                    .append('\t')
                    .append(result.changed())
                    .append('\t')
                    .append(result.fixedPoint())
                    .append('\t')
                    .append(result.behaviorStable())
                    .append('\t')
                    .append(result.contractStable())
                    .append('\t')
                    .append(result.lexicalDataStable())
                    .append('\n');
        }
        Files.writeString(out.resolve("results.tsv"), tsv, StandardCharsets.UTF_8);
        return results;
    }

    private static ResultAndSource converge(
            int fixture,
            String original,
            List<String> schedule,
            Observation baseline,
            NebulaM3A3Cases.Signal baselineSignal)
            throws Exception {
        String current = original;
        int applications = 0;
        int compiles = 1;
        boolean changed = false;
        boolean stable = false;

        for (int pass = 0; pass < MAX_PASSES; pass++) {
            String beforePass = current;
            for (String operation : schedule) {
                current = apply(recipe(operation), current);
                applications++;
                Observation observed =
                        verify(
                                current,
                                fixture,
                                String.join("", schedule),
                                applications);
                compiles++;
                requireObservation(baseline, observed);
                NebulaM3A3Cases.requireStableData(
                        baselineSignal, NebulaM3A3Cases.signal(current));
            }
            if (current.equals(beforePass)) {
                stable = true;
                break;
            }
            changed = true;
        }
        if (!stable) {
            throw new IllegalStateException(
                    "Nebula A3 mastery pass budget exhausted: "
                            + String.join(">", schedule));
        }

        String replay = current;
        for (String operation : schedule) {
            replay = apply(recipe(operation), replay);
        }
        if (!current.equals(replay)) {
            throw new IllegalStateException(
                    "Nebula A3 second replay changed source: "
                            + String.join(">", schedule));
        }

        return new ResultAndSource(
                new Result(
                        fixture,
                        String.join(">", schedule),
                        operationSet(schedule),
                        sha256(original),
                        sha256(current),
                        applications,
                        compiles,
                        changed,
                        true,
                        true,
                        true,
                        true),
                current);
    }

    private static Recipe recipe(String operation) {
        return switch (operation) {
            case "A" -> new NebulaM3AtomizePureIntReturnRecipe();
            case "P" -> new NebulaM3PatternizePureIntAtomRecipe();
            default -> throw new IllegalArgumentException("unknown mastery operation");
        };
    }

    private static String apply(Recipe recipe, String source) {
        SourceFile parsed = parse(source);
        InMemoryExecutionContext context =
                new InMemoryExecutionContext(
                        failure -> {
                            throw new IllegalStateException(failure);
                        });
        var run =
                recipe.run(
                        new InMemoryLargeSourceSet(List.of(parsed)),
                        context,
                        4);
        var changes = run.getChangeset().getAllResults();
        if (changes.isEmpty()) {
            return source;
        }
        if (changes.size() != 1 || changes.getFirst().getAfter() == null) {
            throw new IllegalStateException("mastery recipe changed unexpected source set");
        }
        SourceFile after = Objects.requireNonNull(changes.getFirst().getAfter(), "after");
        if (!after.getSourcePath().equals(parsed.getSourcePath())) {
            throw new IllegalStateException("mastery recipe changed source path");
        }
        String rendered = after.printAll();
        if (!parse(rendered).printAll().equals(rendered)) {
            throw new IllegalStateException("mastery postimage did not round-trip");
        }
        return rendered;
    }

    private static SourceFile parse(String source) {
        ArrayList<Throwable> errors = new ArrayList<>();
        InMemoryExecutionContext context = new InMemoryExecutionContext(errors::add);
        List<SourceFile> parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(
                                List.of(
                                        Parser.Input.fromString(
                                                Path.of(
                                                        "org/eclipse/nebula/m3/lab/Subject.java"),
                                                source)),
                                null,
                                context)
                        .toList();
        if (!errors.isEmpty()
                || parsed.size() != 1
                || parsed.getFirst() instanceof ParseError
                || !parsed.getFirst().printAll().equals(source)) {
            throw new IllegalArgumentException("mastery Java parse/print was not lossless");
        }
        return parsed.getFirst();
    }

    private static Observation verify(
            String source,
            int fixture,
            String schedule,
            int pass)
            throws Exception {
        Path directory = Files.createTempDirectory("nebula-m3-a3-");
        try {
            Path java =
                    directory.resolve(
                            "org/eclipse/nebula/m3/lab/Subject.java");
            Files.createDirectories(java.getParent());
            Files.writeString(java, source, StandardCharsets.UTF_8);

            int exit =
                    ToolProvider.getSystemJavaCompiler()
                            .run(
                                    null,
                                    null,
                                    null,
                                    "--release",
                                    "21",
                                    "-Xlint:all",
                                    "-Werror",
                                    "-d",
                                    directory.toString(),
                                    java.toString());
            if (exit != 0) {
                throw new IllegalStateException(
                        "mastery javac failed fixture="
                                + fixture
                                + " schedule="
                                + schedule
                                + " pass="
                                + pass);
            }

            try (URLClassLoader loader =
                    new URLClassLoader(
                            new URL[] {directory.toUri().toURL()}, null)) {
                Class<?> type =
                        loader.loadClass("org.eclipse.nebula.m3.lab.Subject");
                Method probe = type.getMethod("probe", int.class, int.class);
                Method matches = type.getMethod("matches", String.class);
                Method payload = type.getMethod("payload");

                ArrayList<String> behavior = new ArrayList<>();
                for (int[] pair :
                        List.of(
                                new int[] {0, 0},
                                new int[] {1, 2},
                                new int[] {-7, 3},
                                new int[] {Integer.MAX_VALUE, 1})) {
                    behavior.add(
                            pair[0]
                                    + ","
                                    + pair[1]
                                    + "="
                                    + probe.invoke(null, pair[0], pair[1]));
                }
                for (String value :
                        List.of(
                                "return (a+b)*31;",
                                "if (x) { return y; }",
                                "plain text",
                                "a+b?")) {
                    behavior.add(
                            "m:" + value + "=" + matches.invoke(null, value));
                }

                return new Observation(
                        contract(type),
                        List.copyOf(behavior),
                        Objects.toString(payload.invoke(null), ""));
            }
        } finally {
            deleteTree(directory);
        }
    }

    private static List<String> contract(Class<?> type) {
        ArrayList<String> rows = new ArrayList<>();
        for (Method method : type.getDeclaredMethods()) {
            int modifiers = method.getModifiers();
            if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)) {
                rows.add(
                        "M:"
                                + Modifier.toString(modifiers)
                                + ":"
                                + method.getName()
                                + ":"
                                + method.getReturnType().getTypeName()
                                + ":"
                                + List.of(method.getParameterTypes()).stream()
                                        .map(Class::getTypeName)
                                        .toList());
            }
        }
        for (Field field : type.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)) {
                rows.add(
                        "F:"
                                + Modifier.toString(modifiers)
                                + ":"
                                + field.getName()
                                + ":"
                                + field.getType().getTypeName());
            }
        }
        rows.sort(Comparator.naturalOrder());
        return List.copyOf(rows);
    }

    private static void requireObservation(
            Observation expected,
            Observation actual) {
        if (!expected.contract().equals(actual.contract())) {
            throw new IllegalStateException("mastery public/protected contract drift");
        }
        if (!expected.behavior().equals(actual.behavior())) {
            throw new IllegalStateException("mastery behavior drift");
        }
        if (!expected.payload().equals(actual.payload())) {
            throw new IllegalStateException("mastery code-looking payload drift");
        }
    }

    private static String operationSet(List<String> schedule) {
        return schedule.stream()
                .distinct()
                .sorted()
                .reduce((left, right) -> left + "+" + right)
                .orElseThrow();
    }

    private static String cell(String value) {
        return Objects.toString(value, "")
                .replace('\t', ' ')
                .replace('\r', ' ')
                .replace('\n', ' ');
    }

    private static String text(String value, String field) {
        String checked = Objects.toString(value, "").strip();
        if (checked.isEmpty()
                || checked.indexOf('\0') >= 0
                || checked.indexOf('\n') >= 0
                || checked.indexOf('\r') >= 0
                || checked.indexOf('\t') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String sha(String value, String field) {
        String checked = text(value, field);
        if (!checked.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    static String sha256(String value) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(
                                            Objects.requireNonNull(value, "value")
                                                    .getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }

    private static void deleteTree(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(
                            path -> {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (IOException ignored) {
                                    // Temporary compiler output cleanup is best effort.
                                }
                            });
        } catch (IOException ignored) {
            // Temporary compiler output cleanup is best effort.
        }
    }
}
