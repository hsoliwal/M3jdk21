// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

/** Source-sealed installer for Nebula's official declarative OpenRewrite convergence control. */
public final class NebulaM3DeclarativeConvergenceControlRecipe
        extends ScanningRecipe<NebulaM3DeclarativeConvergenceControlRecipe.State> {
    private static final String ABSENT = "ABSENT";
    private static final String ROOT =
            "/org/eclipse/nebula/m3/rewrite/exact/declarative-convergence/";

    private static final Map<String, Target> TARGETS =
            Map.of(
                    "src/main/java/org/eclipse/nebula/m3/rewrite/NebulaM3ConvergenceCatalog.java",
                    new Target(
                            ABSENT,
                            "edda96e75f11537490ddfe05197e20cc55e1dfad",
                            "NebulaM3ConvergenceCatalog.java.after.txt"),
                    "src/main/java/org/eclipse/nebula/m3/rewrite/convergence/NebulaM3FileConvergenceRecipeDag.java",
                    new Target(
                            "04c543bc956eb5d68d45f993fcb75337a689e608",
                            "cb37b38e5aa1587b7bc32b0204844374723202b1",
                            "NebulaM3FileConvergenceRecipeDag.java.after.txt"),
                    "src/test/java/org/eclipse/nebula/m3/rewrite/NebulaM3DeclarativeConvergenceTest.java",
                    new Target(
                            ABSENT,
                            "89f2de01b1d600445e79f1143667fe39881e1629",
                            "NebulaM3DeclarativeConvergenceTest.java.after.txt"));

    private record Target(String beforeGitBlob, String afterGitBlob, String afterResource) {}

    public static final class State {
        private final Map<String, String> seen = new LinkedHashMap<>();
        private String refusal;
    }

    @Override
    public String getDisplayName() {
        return "Install Nebula declarative convergence control";
    }

    @Override
    public String getDescription() {
        return "Replays the exact reviewed managed-Environment catalog, DAG activation and "
                + "declarative-resource proof without changing Nebula widget/public contracts.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "nebula",
                "openrewrite",
                "recipe-first",
                "file-local",
                "candidate-only",
                "contract-preserving");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public State getInitialValue(ExecutionContext context) {
        TARGETS.forEach(
                (path, target) -> {
                    String after = resource(target.afterResource());
                    if (!target.afterGitBlob().equals(gitBlob(after))) {
                        throw new IllegalStateException(
                                "Nebula declarative convergence resource drift: " + path);
                    }
                });
        return new State();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(State state) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext context) {
                if (!(tree instanceof SourceFile source)) return tree;
                stopAfterPreVisit();
                String path = normalized(source.getSourcePath());
                Target target = TARGETS.get(path);
                if (target == null) return tree;
                if (!(source instanceof J.CompilationUnit)) {
                    state.refusal = "target is not Java: " + path;
                    return tree;
                }
                String hash = gitBlob(source.printAll());
                if (state.seen.putIfAbsent(path, hash) != null) {
                    state.refusal = "duplicate target: " + path;
                } else if (!hash.equals(target.beforeGitBlob())
                        && !hash.equals(target.afterGitBlob())) {
                    state.refusal = "source drift: " + path;
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
        requireAdmissible(state);
        return TARGETS.entrySet().stream()
                .filter(entry -> ABSENT.equals(entry.getValue().beforeGitBlob()))
                .filter(entry -> !state.seen.containsKey(entry.getKey()))
                .map(entry -> parse(entry.getKey(), entry.getValue(), context))
                .toList();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(State state) {
        requireAdmissible(state);
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext context) {
                if (!(tree instanceof SourceFile source)) return tree;
                stopAfterPreVisit();
                String path = normalized(source.getSourcePath());
                Target target = TARGETS.get(path);
                if (target == null) return tree;

                String current = gitBlob(source.printAll());
                if (current.equals(target.afterGitBlob())) return tree;
                if (!current.equals(target.beforeGitBlob())) {
                    throw new IllegalStateException(
                            "Nebula declarative convergence target moved after scan: " + path);
                }

                SourceFile parsed = parse(path, target, context);
                return parsed.withId(source.getId())
                        .withSourcePath(source.getSourcePath())
                        .withMarkers(source.getMarkers())
                        .withFileAttributes(source.getFileAttributes())
                        .withCharset(source.getCharset())
                        .withCharsetBomMarked(source.isCharsetBomMarked())
                        .withChecksum(null);
            }
        };
    }

    private static void requireAdmissible(State state) {
        if (state.refusal != null) {
            throw new IllegalStateException(state.refusal);
        }
        TARGETS.forEach(
                (path, target) -> {
                    if (!ABSENT.equals(target.beforeGitBlob())
                            && !state.seen.containsKey(path)) {
                        throw new IllegalStateException(
                                "required Nebula declarative convergence source missing: " + path);
                    }
                });
    }

    private static SourceFile parse(
            String path, Target target, ExecutionContext context) {
        String body = resource(target.afterResource());
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
                    "Nebula declarative convergence Java roundtrip failed: " + path);
        }
        return parsed.getFirst();
    }

    private static String resource(String name) {
        try (InputStream input =
                NebulaM3DeclarativeConvergenceControlRecipe.class.getResourceAsStream(
                        ROOT + name)) {
            if (input == null) {
                throw new IllegalStateException(
                        "missing Nebula declarative convergence resource: " + name);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "cannot read Nebula declarative convergence resource", failure);
        }
    }

    static String gitBlob(String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
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

    private static String normalized(Path path) {
        String value = path.normalize().toString().replace('\\', '/');
        String prefix = "m3/recipe-first/";
        return value.startsWith(prefix) ? value.substring(prefix.length()) : value;
    }
}
