// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * JDK target compiler adapted from Synexia's all-module public-code polish workspace.
 *
 * <p>The model is deliberately candidate-only. Java-bearing source modules receive one serial
 * FILE-local OpenRewrite dry-run lane; native-bearing modules additionally require native/JNI
 * verification. Test families and build metadata remain verification-only targets. Every A3
 * inventory row is covered exactly once by a source-module or test-family target.</p>
 */
public final class A3Polish {
    public static final String RECIPE = "com.m3.a3.SynexiaPublicPolish";

    public enum Kind {
        OWNED_CODE,
        OWNED_MIXED_NATIVE,
        NATIVE_ONLY,
        RESOURCE_ONLY,
        TEST_SUITE,
        BUILD_METADATA
    }

    public enum Action {
        DRY_RUN_CANDIDATE,
        VERIFY_ONLY
    }

    public record Target(
            int ordinal,
            String target,
            String family,
            Kind kind,
            Action action,
            String recipe,
            boolean nativeGate,
            String scope,
            int files,
            int javaFiles,
            int nativeFiles,
            String root) {
        public Target {
            if (ordinal < 1 || files < 0 || javaFiles < 0 || nativeFiles < 0) {
                throw new IllegalArgumentException("invalid A3 polish counts");
            }
            target = text(target, "target");
            family = text(family, "family");
            kind = Objects.requireNonNull(kind, "kind");
            action = Objects.requireNonNull(action, "action");
            recipe = text(recipe, "recipe");
            scope = text(scope, "scope");
            if (javaFiles + nativeFiles > files) {
                throw new IllegalArgumentException("A3 polish file counts");
            }
            if ((action == Action.DRY_RUN_CANDIDATE) != (javaFiles > 0)) {
                throw new IllegalArgumentException("A3 polish candidate/java mismatch");
            }
            if ((action == Action.DRY_RUN_CANDIDATE) != RECIPE.equals(recipe)) {
                throw new IllegalArgumentException("A3 polish recipe/action mismatch");
            }
            if (nativeGate != (nativeFiles > 0)) {
                throw new IllegalArgumentException("A3 polish native gate mismatch");
            }
            String expected =
                    A3Fs.sha(
                            String.join(
                                    "\n",
                                    "A3-PUBLIC-POLISH-TARGET/1",
                                    Integer.toString(ordinal),
                                    target,
                                    family,
                                    kind.name(),
                                    action.name(),
                                    recipe,
                                    Boolean.toString(nativeGate),
                                    scope,
                                    Integer.toString(files),
                                    Integer.toString(javaFiles),
                                    Integer.toString(nativeFiles)));
            root = root == null || root.isBlank() ? expected : sha(root, "root");
            if (!expected.equals(root)) {
                throw new IllegalArgumentException("A3 polish target root mismatch");
            }
        }
    }

    public record Workspace(
            int inventoryFiles,
            int coveredInventoryFiles,
            int targetCount,
            int candidateTargets,
            int nativeGateTargets,
            String inventoryRoot,
            String synexiaRoot,
            String targetRoot,
            List<Target> targets) {
        public Workspace {
            if (inventoryFiles < 0
                    || inventoryFiles != coveredInventoryFiles
                    || targetCount < 1
                    || targetCount != Objects.requireNonNull(targets, "targets").size()
                    || candidateTargets < 1
                    || nativeGateTargets < 0) {
                throw new IllegalArgumentException("incomplete A3 polish workspace");
            }
            inventoryRoot = sha(inventoryRoot, "inventoryRoot");
            synexiaRoot = sha(synexiaRoot, "synexiaRoot");
            targetRoot = sha(targetRoot, "targetRoot");
            targets = List.copyOf(targets);
        }

        public boolean sourceMutationAuthority() {
            return false;
        }

        public boolean promotionAuthority() {
            return false;
        }
    }

    private A3Polish() {}

    public static Workspace compile(Path root) throws IOException {
        Path checkedRoot = A3Fs.root(root);
        List<A3Inv.Row> inventory = A3Inv.scan(checkedRoot);
        A3Synexia.Report synexia = A3Synexia.verify(checkedRoot);
        boolean buildMetadata =
                Files.isRegularFile(checkedRoot.resolve("configure"))
                        && Files.isRegularFile(checkedRoot.resolve("Makefile"))
                        && Files.isDirectory(checkedRoot.resolve("make"));
        return compile(inventory, synexia.root(), buildMetadata);
    }

    static Workspace compile(
            List<A3Inv.Row> inventory,
            String synexiaRoot,
            boolean buildMetadata) {
        List<A3Inv.Row> rows = List.copyOf(Objects.requireNonNull(inventory, "inventory"));
        Map<String, List<A3Inv.Row>> source = new TreeMap<>();
        Map<String, List<A3Inv.Row>> tests = new TreeMap<>();
        for (A3Inv.Row row : rows) {
            Map<String, List<A3Inv.Row>> owner =
                    "src".equals(row.tree()) ? source : tests;
            owner.computeIfAbsent(row.module(), ignored -> new ArrayList<>()).add(row);
        }

        ArrayList<Target> targets = new ArrayList<>();
        int ordinal = 1;
        for (Map.Entry<String, List<A3Inv.Row>> entry : source.entrySet()) {
            targets.add(sourceTarget(ordinal++, entry.getKey(), entry.getValue()));
        }
        for (Map.Entry<String, List<A3Inv.Row>> entry : tests.entrySet()) {
            targets.add(testTarget(ordinal++, entry.getKey(), entry.getValue()));
        }
        if (buildMetadata) {
            targets.add(
                    new Target(
                            ordinal,
                            "openjdk-build",
                            "OPENJDK_BUILD",
                            Kind.BUILD_METADATA,
                            Action.VERIFY_ONLY,
                            "NONE",
                            false,
                            "REPOSITORY",
                            3,
                            0,
                            0,
                            ""));
        }

        int covered =
                targets.stream()
                        .filter(target -> target.kind() != Kind.BUILD_METADATA)
                        .mapToInt(Target::files)
                        .sum();
        if (covered != rows.size()) {
            throw new IllegalStateException(
                    "A3 polish inventory coverage drift: inventory="
                            + rows.size()
                            + " covered="
                            + covered);
        }

        targets.sort(Comparator.comparingInt(Target::ordinal));
        String inventoryRoot = inventoryRoot(rows);
        String targetRoot =
                A3Fs.sha(
                        "A3-PUBLIC-POLISH-WORKSPACE/1\n"
                                + targets.stream()
                                        .map(Target::root)
                                        .reduce("", (left, right) -> left + right + "\n"));
        int candidates =
                Math.toIntExact(
                        targets.stream()
                                .filter(target -> target.action() == Action.DRY_RUN_CANDIDATE)
                                .count());
        int nativeGates =
                Math.toIntExact(targets.stream().filter(Target::nativeGate).count());
        return new Workspace(
                rows.size(),
                covered,
                targets.size(),
                candidates,
                nativeGates,
                inventoryRoot,
                synexiaRoot,
                targetRoot,
                List.copyOf(targets));
    }

    public static Workspace write(Path root, Path out) throws IOException {
        Path checkedRoot = A3Fs.root(root);
        Path checkedOut = A3Fs.out(checkedRoot, out);
        Workspace workspace = compile(checkedRoot);

        String source =
                "field\tvalue\n"
                        + "inventory_files\t"
                        + workspace.inventoryFiles()
                        + "\ncovered_inventory_files\t"
                        + workspace.coveredInventoryFiles()
                        + "\ntarget_count\t"
                        + workspace.targetCount()
                        + "\ncandidate_targets\t"
                        + workspace.candidateTargets()
                        + "\nnative_gate_targets\t"
                        + workspace.nativeGateTargets()
                        + "\ninventory_root\t"
                        + workspace.inventoryRoot()
                        + "\nsynexia_root\t"
                        + workspace.synexiaRoot()
                        + "\ntarget_root\t"
                        + workspace.targetRoot()
                        + "\nrecipe\t"
                        + RECIPE
                        + "\nsource_mutation_authority\t"
                        + workspace.sourceMutationAuthority()
                        + "\npromotion_authority\t"
                        + workspace.promotionAuthority()
                        + "\n";
        A3Fs.write(checkedRoot, checkedOut.resolve("SOURCE.tsv"), source);

        StringBuilder targets =
                new StringBuilder(
                        "ordinal\ttarget\tfamily\tkind\taction\trecipe\tnative_gate\tscope"
                                + "\tfiles\tjava_files\tnative_files\troot\n");
        for (Target target : workspace.targets()) {
            targets.append(target.ordinal())
                    .append('\t')
                    .append(A3Fs.cell(target.target()))
                    .append('\t')
                    .append(target.family())
                    .append('\t')
                    .append(target.kind())
                    .append('\t')
                    .append(target.action())
                    .append('\t')
                    .append(target.recipe())
                    .append('\t')
                    .append(target.nativeGate())
                    .append('\t')
                    .append(target.scope())
                    .append('\t')
                    .append(target.files())
                    .append('\t')
                    .append(target.javaFiles())
                    .append('\t')
                    .append(target.nativeFiles())
                    .append('\t')
                    .append(target.root())
                    .append('\n');
        }
        A3Fs.write(checkedRoot, checkedOut.resolve("TARGETS.tsv"), targets.toString());
        return workspace;
    }

    private static Target sourceTarget(
            int ordinal,
            String module,
            List<A3Inv.Row> rows) {
        int javaFiles = count(rows, A3Inv.Kind.JAVA);
        int nativeFiles = count(rows, A3Inv.Kind.NATIVE);
        Kind kind;
        if (javaFiles > 0 && nativeFiles > 0) {
            kind = Kind.OWNED_MIXED_NATIVE;
        } else if (javaFiles > 0) {
            kind = Kind.OWNED_CODE;
        } else if (nativeFiles > 0) {
            kind = Kind.NATIVE_ONLY;
        } else {
            kind = Kind.RESOURCE_ONLY;
        }
        Action action =
                javaFiles > 0 ? Action.DRY_RUN_CANDIDATE : Action.VERIFY_ONLY;
        return new Target(
                ordinal,
                module,
                "OPENJDK_SOURCE",
                kind,
                action,
                javaFiles > 0 ? RECIPE : "NONE",
                nativeFiles > 0,
                "MODULE",
                rows.size(),
                javaFiles,
                nativeFiles,
                "");
    }

    private static Target testTarget(
            int ordinal,
            String family,
            List<A3Inv.Row> rows) {
        int javaFiles = count(rows, A3Inv.Kind.JAVA);
        int nativeFiles = count(rows, A3Inv.Kind.NATIVE);
        Action action =
                javaFiles > 0 ? Action.DRY_RUN_CANDIDATE : Action.VERIFY_ONLY;
        return new Target(
                ordinal,
                family,
                "OPENJDK_TEST",
                Kind.TEST_SUITE,
                action,
                javaFiles > 0 ? RECIPE : "NONE",
                nativeFiles > 0,
                "MODULE",
                rows.size(),
                javaFiles,
                nativeFiles,
                "");
    }

    private static int count(List<A3Inv.Row> rows, A3Inv.Kind kind) {
        return Math.toIntExact(rows.stream().filter(row -> row.kind() == kind).count());
    }

    private static String inventoryRoot(List<A3Inv.Row> rows) {
        StringBuilder material = new StringBuilder("A3-INVENTORY-ROOT/1\n");
        rows.stream()
                .sorted(Comparator.comparing(A3Inv.Row::path))
                .forEach(
                        row ->
                                material.append(row.path())
                                        .append('\t')
                                        .append(row.sha256())
                                        .append('\n'));
        return A3Fs.sha(material.toString());
    }

    private static String sha(String value, String field) {
        String checked = text(value, field);
        if (!checked.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field);
        }
        return checked;
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
}
