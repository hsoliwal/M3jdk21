// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

/** Installs the exact reviewed Nebula A3 explicit-file materializer and its JUnit proof. */
public final class NebulaM3A3FileMaterializerRecipe
        extends ScanningRecipe<NebulaM3A3FileMaterializerRecipe.State> {
    private static final String ANCHOR =
            "src/main/java/org/eclipse/nebula/m3/rewrite/NebulaM3Java21ConvergenceRecipe.java";
    private static final String ROOT =
            "/org/eclipse/nebula/m3/rewrite/convergence/a3-file-materializer/";

    private static final Map<String, Target> TARGETS =
            Map.of(
                    "src/main/java/org/eclipse/nebula/m3/NebulaM3A3Apply.java",
                    new Target(
                            "NebulaM3A3Apply.java.txt",
                            "8e7a5f1cd249f9d6123ca53ae82eda277d2c2f2eb56b045b0836b12e4a5fd0f5"),
                    "src/test/java/org/eclipse/nebula/m3/NebulaM3A3ApplyTest.java",
                    new Target(
                            "NebulaM3A3ApplyTest.java.txt",
                            "4ef09a241551ae5a20cefdc9d1c3dee830b7f0826b2f75ce9e222c4de9467f12"));

    static final class State {
        boolean anchorSeen;
        final Map<String, String> seen = new LinkedHashMap<>();
        final List<String> conflicts = new ArrayList<>();
    }

    private record Target(String resource, String sha256) {
        private Target {
            resource = Objects.requireNonNull(resource, "resource");
            sha256 = Objects.requireNonNull(sha256, "sha256");
            if (!sha256.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("sha256");
            }
        }
    }

    @Override
    public String getDisplayName() {
        return "Install Nebula M3 A3 explicit-file materializer";
    }

    @Override
    public String getDescription() {
        return "Adds the reviewed no-root-write per-file convergence runner and proof under the "
                + "existing Nebula recipe-first module.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "nebula",
                "a3",
                "recipe-first",
                "candidate-only",
                "no-root-write",
                "file-local");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public State getInitialValue(ExecutionContext context) {
        for (Target target : TARGETS.values()) {
            String body = resource(target.resource());
            if (!target.sha256().equals(sha256(body))) {
                throw new IllegalStateException(
                        "Nebula A3 materializer resource drift: " + target.resource());
            }
        }
        return new State();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(State state) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext context) {
                if (!(tree instanceof SourceFile source)) {
                    return tree;
                }
                stopAfterPreVisit();
                String path = normalized(source.getSourcePath());
                synchronized (state) {
                    if (ANCHOR.equals(path)) {
                        state.anchorSeen = true;
                    }
                    Target target = TARGETS.get(path);
                    if (target == null) {
                        return tree;
                    }
                    if (!(source instanceof J.CompilationUnit)) {
                        state.conflicts.add("target is not Java: " + path);
                    }
                    String current = sha256(source.printAll());
                    if (state.seen.putIfAbsent(path, current) != null) {
                        state.conflicts.add("duplicate target: " + path);
                    }
                    if (!target.sha256().equals(current)) {
                        state.conflicts.add("occupied target drift: " + path);
                    }
                }
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(
            State state,
            Collection<SourceFile> generatedInThisCycle,
            ExecutionContext context) {
        synchronized (state) {
            if (!state.anchorSeen) {
                return List.of();
            }
            if (!state.conflicts.isEmpty()) {
                throw new IllegalStateException(String.join("; ", state.conflicts));
            }

            List<SourceFile> generated = new ArrayList<>();
            TARGETS.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .filter(entry -> !state.seen.containsKey(entry.getKey()))
                    .map(entry -> parse(entry.getKey(), entry.getValue(), context))
                    .forEach(generated::add);
            return List.copyOf(generated);
        }
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(State state) {
        return new TreeVisitor<Tree, ExecutionContext>() {};
    }

    public boolean sourceMutationAuthority() {
        return false;
    }

    public boolean promotionAuthority() {
        return false;
    }

    private static SourceFile parse(
            String path,
            Target target,
            ExecutionContext context) {
        String body = resource(target.resource());
        List<SourceFile> parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(
                                List.of(
                                        Parser.Input.fromString(
                                                Path.of(path),
                                                body)),
                                null,
                                context)
                        .toList();
        if (parsed.size() != 1
                || !(parsed.getFirst() instanceof J.CompilationUnit)
                || !body.equals(parsed.getFirst().printAll())) {
            throw new IllegalStateException(
                    "Nebula A3 materializer Java roundtrip failed: " + path);
        }
        return parsed.getFirst().withSourcePath(Path.of(path));
    }

    private static String resource(String name) {
        try (InputStream input =
                NebulaM3A3FileMaterializerRecipe.class.getResourceAsStream(
                        ROOT + name)) {
            if (input == null) {
                throw new IllegalStateException(
                        "missing Nebula A3 materializer resource: " + name);
            }
            return new String(
                    input.readAllBytes(),
                    StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "cannot read Nebula A3 materializer resource",
                    failure);
        }
    }

    private static String sha256(String value) {
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

    private static String normalized(Path path) {
        String value = path.normalize().toString().replace('\\', '/');
        String prefix = "m3/recipe-first/";
        return value.startsWith(prefix) ? value.substring(prefix.length()) : value;
    }
}
