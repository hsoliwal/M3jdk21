// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import com.m3.rewrite.scope.M3EditScope;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.text.PlainText;

/**
 * Exact source-sealed replay for the 85-JEP authority/planner reconciliation.
 *
 * <p>The recipe owns only M3 control-plane text. It does not mutate OpenJDK product source.
 * Existing targets must match the exact parent preimage or reviewed postimage; additive targets
 * must be absent or already equal the reviewed postimage. Any third state fails closed.</p>
 */
public final class M3JepUniverseAuthorityReconciliationRecipe
        extends ScanningRecipe<M3JepUniverseAuthorityReconciliationRecipe.Inventory> {
    private static final String ROOT =
            "/com/m3/rewrite/backport/jep-universe-authority-reconciliation/";

    private record Target(
            String path, String beforeResource, String afterResource, boolean absentBefore) {}

    private static final List<Target> TARGETS = List.of(
            existing(".github/workflows/m3-jdk21-recipe-crates.yml",
                    "00-m3-jdk21-recipe-crates.yml.txt"),
            additive(".github/workflows/m3-jep-universe-file-atoms.yml",
                    "01-m3-jep-universe-file-atoms.yml.txt"),
            existing("m3/backports/README.md", "02-backports-README.md.txt"),
            additive("m3/backports/RELEASE_DONOR_REFS.tsv", "03-RELEASE_DONOR_REFS.tsv.txt"),
            existing("m3/backports/file_delta_inventory.py", "04-file_delta_inventory.py.txt"),
            existing("m3/backports/generate_recipe_crates.py", "05-generate_recipe_crates.py.txt"),
            additive("m3/backports/jep_seed_packet_planner.py",
                    "06-jep_seed_packet_planner.py.txt"),
            additive("m3/backports/jep_upstream_inventory.py",
                    "07-jep_upstream_inventory.py.txt"),
            additive("m3/backports/release_donor_refs.py", "08-release_donor_refs.py.txt"),
            existing("m3/backports/test_file_delta_inventory.py",
                    "09-test_file_delta_inventory.py.txt"),
            existing("m3/backports/test_generate_recipe_crates.py",
                    "10-test_generate_recipe_crates.py.txt"),
            additive("m3/backports/test_jep_seed_packet_planner.py",
                    "11-test_jep_seed_packet_planner.py.txt"),
            additive("m3/backports/test_jep_upstream_inventory.py",
                    "12-test_jep_upstream_inventory.py.txt"),
            additive("m3/backports/test_release_donor_refs.py",
                    "13-test_release_donor_refs.py.txt"),
            additive("m3/backports/recipes/jep-universe-authority-reconciliation/README.md",
                    "14-packet-README.md.txt"),
            additive("m3/backports/recipes/jep-universe-authority-reconciliation/packet.tsv",
                    "15-packet.tsv.txt"),
            additive("m3/backports/recipes/jep-universe-authority-reconciliation/atom-evidence.tsv",
                    "16-atom-evidence.tsv.txt"),
            additive("m3/backports/recipes/jep-universe-authority-reconciliation/COMPOSITION_PLAN.tsv",
                    "17-COMPOSITION_PLAN.tsv.txt"));

    private static final Map<String, Target> BY_PATH = byPath();

    static final class Inventory {
        private final Map<String, State> states = new HashMap<>();

        void observe(Target target, String source) {
            if (states.containsKey(target.path())) {
                throw new IllegalStateException(
                        "duplicate M3 JEP authority target: " + target.path());
            }
            String after = resource(target.afterResource());
            if (source.equals(after)) {
                states.put(target.path(), State.AFTER);
                return;
            }
            if (!target.absentBefore()
                    && source.equals(resource(target.beforeResource()))) {
                states.put(target.path(), State.BEFORE);
                return;
            }
            throw new IllegalStateException(
                    "drifted M3 JEP authority target: " + target.path());
        }

        State state(Target target) {
            return states.get(target.path());
        }

        void requireAdmissible() {
            for (Target target : TARGETS) {
                if (!target.absentBefore() && !states.containsKey(target.path())) {
                    throw new IllegalStateException(
                            "missing M3 JEP authority target: " + target.path());
                }
            }
        }
    }

    private enum State {
        BEFORE,
        AFTER
    }

    @Override
    public String getDisplayName() {
        return "Reconcile the M3JDK21 85-JEP authority and FILE-atom planner";
    }

    @Override
    public String getDescription() {
        return "Replays the reviewed release/donor authority, comparator, FILE recipe generator, "
                + "JEP planners, tests, CI and DAG evidence from exact parent preimages.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "backport",
                "recipe-first",
                "file-atomic",
                "authority",
                "dag",
                "candidate-only",
                "fail-closed");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public Inventory getInitialValue(ExecutionContext context) {
        Objects.requireNonNull(context, "context");
        return new Inventory();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Inventory inventory) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext context) {
                if (tree instanceof SourceFile source) {
                    stopAfterPreVisit();
                    Target target = BY_PATH.get(normalized(source));
                    if (target != null) {
                        if (!(source instanceof PlainText)) {
                            throw new IllegalStateException(
                                    "M3 JEP authority target is not PlainText: "
                                            + normalized(source));
                        }
                        inventory.observe(target, source.printAll());
                    }
                }
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(
            Inventory inventory, ExecutionContext context) {
        inventory.requireAdmissible();
        ArrayList<SourceFile> generated = new ArrayList<>();
        for (Target target : TARGETS) {
            if (target.absentBefore() && inventory.state(target) == null) {
                generated.add(template(target));
            }
        }
        return generated;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Inventory inventory) {
        inventory.requireAdmissible();
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext context) {
                if (!(tree instanceof SourceFile source)) {
                    return tree;
                }
                stopAfterPreVisit();
                Target target = BY_PATH.get(normalized(source));
                if (target == null || inventory.state(target) == State.AFTER) {
                    return tree;
                }
                if (!(source instanceof PlainText) || inventory.state(target) != State.BEFORE) {
                    throw new IllegalStateException(
                            "unadmitted M3 JEP authority edit: " + normalized(source));
                }
                return template(target)
                        .withId(source.getId())
                        .withSourcePath(source.getSourcePath())
                        .withMarkers(source.getMarkers())
                        .withFileAttributes(source.getFileAttributes())
                        .withCharset(source.getCharset())
                        .withCharsetBomMarked(source.isCharsetBomMarked())
                        .withChecksum(null);
            }
        };
    }

    /** Physical target set spans workflow and M3 backport/tooling modules. */
    public M3EditScope declaredScope() {
        return M3EditScope.MULTI_MODULE;
    }

    public List<String> targetPaths() {
        return TARGETS.stream().map(Target::path).toList();
    }

    private static Target existing(String path, String resource) {
        return new Target(path, "before-" + resource, resource, false);
    }

    private static Target additive(String path, String resource) {
        return new Target(path, "", resource, true);
    }

    private static Map<String, Target> byPath() {
        HashMap<String, Target> result = new HashMap<>();
        for (Target target : TARGETS) {
            if (result.put(target.path(), target) != null) {
                throw new IllegalStateException(
                        "duplicate configured M3 JEP authority target: " + target.path());
            }
        }
        return Map.copyOf(result);
    }

    private static PlainText template(Target target) {
        return PlainText.builder()
                .sourcePath(Path.of(target.path()))
                .text(resource(target.afterResource()))
                .build();
    }

    private static String normalized(SourceFile source) {
        return source.getSourcePath().normalize().toString().replace('\\', '/');
    }

    private static String resource(String name) {
        try (var input =
                M3JepUniverseAuthorityReconciliationRecipe.class
                        .getResourceAsStream(ROOT + name)) {
            if (input == null) {
                throw new IllegalStateException(
                        "missing M3 JEP authority recipe resource: " + name);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
