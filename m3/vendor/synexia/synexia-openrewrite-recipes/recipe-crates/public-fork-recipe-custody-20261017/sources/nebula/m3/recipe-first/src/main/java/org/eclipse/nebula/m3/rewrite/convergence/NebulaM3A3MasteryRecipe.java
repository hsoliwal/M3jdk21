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

/**
 * Installs the generated compiler/runtime-driven Nebula A3 Atomize/Patternize mastery laboratory.
 *
 * <p>This recipe owns only additive M3 tooling/test paths. Existing occupied paths must be the
 * exact reviewed postimage or the recipe fails closed. It never targets Nebula widget/product
 * source and grants no source-mutation or promotion authority.</p>
 */
public final class NebulaM3A3MasteryRecipe
        extends ScanningRecipe<NebulaM3A3MasteryRecipe.State> {
    private static final String ANCHOR =
            "src/main/java/org/eclipse/nebula/m3/rewrite/NebulaM3Java21ConvergenceRecipe.java";
    private static final String ROOT =
            "/org/eclipse/nebula/m3/rewrite/convergence/a3-mastery/";

    private static final Map<String, Target> TARGETS =
            Map.of(
                    "src/main/java/org/eclipse/nebula/m3/NebulaM3A3Cases.java",
                    new Target(
                            "NebulaM3A3Cases.java.txt",
                            "54404a4d50a8d8cfb0b3cff1e69ae40e6427fb7f"),
                    "src/main/java/org/eclipse/nebula/m3/NebulaM3A3Lab.java",
                    new Target(
                            "NebulaM3A3Lab.java.txt",
                            "099de04b1612c30726d0bd2b6bbeb2ad7e065bfb"),
                    "src/test/java/org/eclipse/nebula/m3/NebulaM3A3LabTest.java",
                    new Target(
                            "NebulaM3A3LabTest.java.txt",
                            "b9d449a1c15b99e52c1985000e6181569372f97c"));

    static final class State {
        boolean anchorSeen;
        final Map<String, String> seen = new LinkedHashMap<>();
        final List<String> conflicts = new ArrayList<>();
    }

    private record Target(String resource, String gitBlob) {
        private Target {
            resource = Objects.requireNonNull(resource, "resource");
            gitBlob = Objects.requireNonNull(gitBlob, "gitBlob");
            if (!gitBlob.matches("[0-9a-f]{40}")) {
                throw new IllegalArgumentException("gitBlob");
            }
        }
    }

    @Override
    public String getDisplayName() {
        return "Install Nebula M3 A3 mastery laboratory";
    }

    @Override
    public String getDescription() {
        return "Installs the reviewed generated Java-21 Atomize/Patternize compiler/runtime mastery "
                + "lab under the existing Nebula recipe-first module.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "nebula",
                "a3",
                "atomization",
                "patternization",
                "compiler",
                "junit",
                "recipe-first",
                "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public State getInitialValue(ExecutionContext context) {
        TARGETS.forEach(
                (path, target) -> {
                    String body = resource(target.resource());
                    if (!target.gitBlob().equals(gitBlob(body))) {
                        throw new IllegalStateException(
                                "Nebula A3 mastery resource drift: " + path);
                    }
                });
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
                    String current = gitBlob(source.printAll());
                    if (!(source instanceof J.CompilationUnit)) {
                        state.conflicts.add("mastery target is not Java: " + path);
                    }
                    if (state.seen.putIfAbsent(path, current) != null) {
                        state.conflicts.add("duplicate mastery target: " + path);
                    }
                    if (!target.gitBlob().equals(current)) {
                        state.conflicts.add("occupied mastery target drift: " + path);
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

            ArrayList<SourceFile> generated = new ArrayList<>();
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

    public boolean productSourceMutationAuthority() {
        return false;
    }

    public boolean donorSourceCopyAuthority() {
        return false;
    }

    public boolean promotionAuthority() {
        return false;
    }

    static Map<String, String> reviewedTargets() {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        TARGETS.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> result.put(entry.getKey(), entry.getValue().gitBlob()));
        return Map.copyOf(result);
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
                                                Path.of(path), body)),
                                null,
                                context)
                        .toList();
        if (parsed.size() != 1
                || !(parsed.getFirst() instanceof J.CompilationUnit)
                || !body.equals(parsed.getFirst().printAll())) {
            throw new IllegalStateException(
                    "Nebula A3 mastery Java roundtrip failed: " + path);
        }
        return parsed.getFirst().withSourcePath(Path.of(path));
    }

    private static String resource(String name) {
        try (InputStream input =
                NebulaM3A3MasteryRecipe.class.getResourceAsStream(ROOT + name)) {
            if (input == null) {
                throw new IllegalStateException(
                        "missing Nebula A3 mastery resource: " + name);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "cannot read Nebula A3 mastery resource", failure);
        }
    }

    private static String normalized(Path path) {
        String value = path.normalize().toString().replace('\\', '/');
        String prefix = "m3/recipe-first/";
        return value.startsWith(prefix) ? value.substring(prefix.length()) : value;
    }

    private static String gitBlob(String value) {
        byte[] bytes = Objects.requireNonNull(value, "value").getBytes(StandardCharsets.UTF_8);
        byte[] prefix = ("blob " + bytes.length + "\0").getBytes(StandardCharsets.UTF_8);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(prefix);
            digest.update(bytes);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }
}
