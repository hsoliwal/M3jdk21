// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

/** Reusable in-memory compiler/runtime laboratory for deterministic M3 recipe mastery tests. */
final class M3RecipeMasteryLab {
    private static final int MAX_CYCLES = 8;
    private static final int[][] VECTORS = {
        {0, 0},
        {1, 2},
        {-1, 7},
        {Integer.MAX_VALUE, 1},
        {Integer.MIN_VALUE, -1},
        {0x55555555, 0x33333333}
    };

    record Convergence(Map<String, String> sources, int cycles) {
        Convergence {
            sources = Map.copyOf(sources);
            if (cycles < 0 || cycles > MAX_CYCLES) {
                throw new IllegalArgumentException("cycles");
            }
        }
    }

    record Outcome(List<Integer> calls, int regexScore) {
        Outcome {
            calls = List.copyOf(calls);
        }
    }

    private M3RecipeMasteryLab() {}

    static Map<String, String> corpus() {
        LinkedHashMap<String, String> sources = new LinkedHashMap<>();
        List<String> expressions =
                List.of(
                        "(a + b) * 31",
                        "(a ^ b) + (a << 2)",
                        "~a + (b * 7)",
                        "(a - b) ^ (b >>> 1)",
                        "((a | b) - (a & b)) * 3",
                        "-(a + (b << 1))");
        int ordinal = 0;
        for (String expression : expressions) {
            for (int decoy = 0; decoy < 4; decoy++) {
                String name = "F" + String.format("%03d", ordinal++);
                String path = "src/main/java/mastery/" + name + ".java";
                sources.put(path, eligibleSource(name, expression, decoy));
            }
        }

        sources.put(
                "src/main/java/mastery/RejectDivision.java",
                rejectedSource("RejectDivision", "return a / (b | 1);", "private static int target"));
        sources.put(
                "src/main/java/mastery/RejectCall.java",
                """
                package mastery;
                final class RejectCall {
                    static int call(int a, int b) { return target(a, b); }
                    private static int target(int a, int b) { return Integer.rotateLeft(a, b); }
                }
                """);
        sources.put(
                "src/main/java/mastery/RejectExtraStatement.java",
                """
                package mastery;
                final class RejectExtraStatement {
                    static int call(int a, int b) { return target(a, b); }
                    private static int target(int a, int b) {
                        int c = a + b;
                        return c * 31;
                    }
                }
                """);
        sources.put(
                "src/main/java/mastery/RejectVisibility.java",
                rejectedSource(
                        "RejectVisibility",
                        "return (a + b) * 31;",
                        "static int target"));
        return Map.copyOf(sources);
    }

    static Convergence converge(List<Recipe> schedule, Map<String, String> initial) {
        Objects.requireNonNull(schedule, "schedule");
        Map<String, String> current = new LinkedHashMap<>(initial);
        for (int cycle = 1; cycle <= MAX_CYCLES; cycle++) {
            boolean changed = false;
            for (Recipe recipe : schedule) {
                Map<String, String> after = applyOnce(recipe, current);
                if (!after.equals(current)) {
                    current = after;
                    changed = true;
                }
            }
            if (!changed) {
                return new Convergence(current, cycle - 1);
            }
        }
        throw new AssertionError("recipe schedule did not converge within " + MAX_CYCLES + " cycles");
    }

    static Map<String, String> applyCanonical(Recipe recipe, Map<String, String> initial) {
        return applyOnce(recipe, initial);
    }

    static Map<String, Outcome> execute(Map<String, String> sources) {
        Map<String, byte[]> bytecode = compile(sources);
        MemoryLoader loader = new MemoryLoader(bytecode);
        TreeMap<String, Outcome> outcomes = new TreeMap<>();
        for (String path : sources.keySet()) {
            String simple = Path.of(path).getFileName().toString().replace(".java", "");
            String className = "mastery." + simple;
            try {
                Class<?> type = loader.loadClass(className);
                var call = type.getDeclaredMethod("call", int.class, int.class);
                call.setAccessible(true);
                ArrayList<Integer> calls = new ArrayList<>(VECTORS.length);
                for (int[] vector : VECTORS) {
                    calls.add((Integer) call.invoke(null, vector[0], vector[1]));
                }
                int regexScore = 0;
                try {
                    var regex = type.getDeclaredMethod("regexScore");
                    regex.setAccessible(true);
                    regexScore = (Integer) regex.invoke(null);
                } catch (NoSuchMethodException ignored) {
                    // Rejection fixtures intentionally do not need regex decoys.
                }
                outcomes.put(className, new Outcome(calls, regexScore));
            } catch (ReflectiveOperationException failure) {
                throw new AssertionError("cannot execute " + className, failure);
            }
        }
        return Map.copyOf(outcomes);
    }

    static int markerCount(Map<String, String> sources, String marker) {
        int count = 0;
        for (String source : sources.values()) {
            int from = 0;
            while ((from = source.indexOf(marker, from)) >= 0) {
                count++;
                from += marker.length();
            }
        }
        return count;
    }

    private static Map<String, String> applyOnce(Recipe recipe, Map<String, String> current) {
        var context =
                new InMemoryExecutionContext(
                        failure -> {
                            throw new AssertionError(failure);
                        });
        ArrayList<Parser.Input> inputs = new ArrayList<>(current.size());
        current.forEach(
                (path, source) ->
                        inputs.add(Parser.Input.fromString(Path.of(path), source)));
        List<SourceFile> parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(inputs, null, context)
                        .toList();

        var result = recipe.run(new InMemoryLargeSourceSet(parsed), context, 8);
        LinkedHashMap<String, String> next = new LinkedHashMap<>(current);
        result.getChangeset()
                .getAllResults()
                .forEach(
                        change -> {
                            SourceFile after = change.getAfter();
                            if (after == null) {
                                throw new AssertionError("mastery recipe deleted source");
                            }
                            next.put(
                                    after.getSourcePath()
                                            .toString()
                                            .replace('\\', '/'),
                                    after.printAll());
                        });
        return Map.copyOf(next);
    }

    private static Map<String, byte[]> compile(Map<String, String> sources) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new AssertionError("mastery lab requires a full JDK compiler");
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager standard =
                        compiler.getStandardFileManager(diagnostics, null, null);
                MemoryFileManager files = new MemoryFileManager(standard)) {
            ArrayList<JavaFileObject> units = new ArrayList<>(sources.size());
            sources.forEach(
                    (path, source) ->
                            units.add(
                                    new SourceObject(
                                            "mastery."
                                                    + Path.of(path)
                                                            .getFileName()
                                                            .toString()
                                                            .replace(".java", ""),
                                            source)));
            List<String> options =
                    List.of("--release", "21", "-Xlint:all", "-Werror", "-proc:none");
            Boolean passed =
                    compiler.getTask(null, files, diagnostics, options, null, units).call();
            if (!Boolean.TRUE.equals(passed)) {
                throw new AssertionError(renderDiagnostics(diagnostics));
            }
            return files.bytecode();
        } catch (IOException failure) {
            throw new AssertionError("in-memory compiler failed", failure);
        }
    }

    private static String renderDiagnostics(DiagnosticCollector<JavaFileObject> diagnostics) {
        StringBuilder out = new StringBuilder("Java compiler rejected mastery corpus");
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            out.append('\n')
                    .append(diagnostic.getKind())
                    .append(" line=")
                    .append(diagnostic.getLineNumber())
                    .append(' ')
                    .append(diagnostic.getMessage(null));
        }
        return out.toString();
    }

    private static String eligibleSource(String name, String expression, int decoy) {
        String comment =
                switch (decoy) {
                    case 0 -> "/* private static int ghost(int a,int b){ return (a+b)*31; } */";
                    case 1 -> "// return (a + b) * 31; private static int ghost(int a, int b)";
                    case 2 -> "/** regex: private\\s+static\\s+int\\s+ghost */";
                    default -> "/* M3-IOP: FAKE_MARKER; int m3$pureIntAtom = 42; */";
                };
        return """
                package mastery;
                final class %s {
                    %s
                    private static final String CODE =
                            "private static int ghost(int a,int b){return (a+b)*31;}";
                    private static final String RX =
                            "(?s)private\\\\s+static\\\\s+int\\\\s+ghost.*return";
                    private static final String TEXT = """
                            private static int ghost(int a, int b) {
                                return (a + b) * 31;
                            }
                            😀 surrogate-safe text
                            """;

                    static int call(int a, int b) { return target(a, b); }

                    static int regexScore() {
                        int score = java.util.regex.Pattern.compile(RX).matcher(TEXT).find() ? 1 : 0;
                        score += java.util.regex.Pattern.compile("m3\\\\$pureIntAtom")
                                .matcher(CODE).find() ? 2 : 0;
                        return score;
                    }

                    private static int target(int a, int b) {
                        return %s;
                    }
                }
                """
                .formatted(name, comment, expression);
    }

    private static String rejectedSource(String name, String statement, String declaration) {
        return """
                package mastery;
                final class %s {
                    static int call(int a, int b) { return target(a, b); }
                    %s(int a, int b) { %s }
                }
                """
                .formatted(name, declaration, statement);
    }

    private static final class SourceObject extends SimpleJavaFileObject {
        private final String source;

        private SourceObject(String className, String source) {
            super(
                    URI.create(
                            "string:///"
                                    + className.replace('.', '/')
                                    + JavaFileObject.Kind.SOURCE.extension),
                    JavaFileObject.Kind.SOURCE);
            this.source = source;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }

    private static final class ByteObject extends SimpleJavaFileObject {
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        private ByteObject(String className, Kind kind) {
            super(
                    URI.create(
                            "bytes:///"
                                    + className.replace('.', '/')
                                    + kind.extension),
                    kind);
        }

        @Override
        public OutputStream openOutputStream() {
            return bytes;
        }

        byte[] bytes() {
            return bytes.toByteArray();
        }
    }

    private static final class MemoryFileManager
            extends ForwardingJavaFileManager<StandardJavaFileManager> {
        private final Map<String, ByteObject> output = new LinkedHashMap<>();

        private MemoryFileManager(StandardJavaFileManager fileManager) {
            super(fileManager);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(
                JavaFileManager.Location location,
                String className,
                JavaFileObject.Kind kind,
                FileObject sibling) {
            ByteObject file = new ByteObject(className, kind);
            output.put(className, file);
            return file;
        }

        Map<String, byte[]> bytecode() {
            LinkedHashMap<String, byte[]> result = new LinkedHashMap<>();
            output.forEach((name, file) -> result.put(name, file.bytes()));
            return Map.copyOf(result);
        }
    }

    private static final class MemoryLoader extends ClassLoader {
        private final Map<String, byte[]> bytecode;

        private MemoryLoader(Map<String, byte[]> bytecode) {
            super(M3RecipeMasteryLab.class.getClassLoader());
            this.bytecode = Map.copyOf(bytecode);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] bytes = bytecode.get(name);
            if (bytes == null) {
                return super.findClass(name);
            }
            return defineClass(name, bytes, 0, bytes.length);
        }
    }
}
