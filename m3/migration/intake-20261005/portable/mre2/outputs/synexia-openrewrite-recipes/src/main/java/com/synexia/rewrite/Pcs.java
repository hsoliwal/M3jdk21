// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ParseExceptionResult;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.tree.J;
import org.openrewrite.tree.ParseError;

/**
 * Provider Callback Superset: one candidate barrier for the retained and bounded-tree crates.
 *
 * <p>The existing recipes perform every transformation. This task-specific composition stages
 * their actual scheduler results privately and publishes only the complete admitted union.
 */
public final class Pcs extends ScanningRecipe<Pcs.Inventory> {
    public static final String CRATE_NAME = "pcs-v1";
    private static final String RECONCILED_CRATE = "pcs-reconciled-v1";
    private static final String RECONCILED_MANIFEST =
            "/com/synexia/rewrite/pcs-reconciled-v1/target-manifest.tsv";
    private final boolean reconciled;
    private static final String RESOURCE_ROOT = "/com/synexia/rewrite/hash-pinned-java/";
    private static final String MANIFEST = "/com/synexia/rewrite/pcs-v1/target-manifest.tsv";
    private static final List<String> ADDITIONAL_PATHS = List.of(
            "synexia-primitives/src/main/java/com/synexia/primitives/BoundedProviderBackedPrimitiveLongTree.java",
            "synexia-primitives/src/main/java/com/synexia/primitives/BoundedProviderBackedPrimitiveTree.java",
            "synexia-primitives/src/main/java/com/synexia/primitives/BoundedVersionedProviderBackedPrimitiveLongTree.java",
            "synexia-primitives/src/test/java/com/synexia/primitives/PcsProbe.java",
            "synexia-primitives/src/test/java/com/synexia/primitives/PcsTest.java",
            "synexia-primitives/src/test/java/com/synexia/primitives/ProviderResidencySupersetTest.java");

    private record Target(String path, String before, String after, boolean retained) { }

    /** Retains the published pcs-v1 entry point and its exact resource contract. */
    public Pcs() {
        this(false);
    }

    private Pcs(boolean reconciled) {
        this.reconciled = reconciled;
    }

    /** Closed current-snapshot configuration; no caller-selected crate is admitted. */
    static Pcs reconciled() {
        return new Pcs(true);
    }

    /** Invocation-local source inventory and fully staged outputs; never shared between runs. */
    public static final class Inventory {
        private final Map<String, Target> targets;
        private final boolean reconciled;
        private final Map<String, SourceFile> seen = new TreeMap<>();
        private final Map<String, String> observed = new TreeMap<>();
        private final List<String> conflicts = new ArrayList<>();
        private Map<String, SourceFile> candidates;
        private IllegalStateException failure;

        private Inventory(Map<String, Target> targets, boolean reconciled) {
            this.targets = targets;
            this.reconciled = reconciled;
        }
    }

    @Override
    public String getDisplayName() {
        return reconciled ? "Reconcile Pcr provider callback and bounded tree candidates"
                : "Compose Pcs provider callback and bounded tree candidates";
    }

    @Override
    public String getDescription() {
        if (reconciled) {
            return "Admit the twenty current provider targets, retain landed repairs, "
                    + "and publish the complete reconciled candidate through the shared Pcs envelope.";
        }
        return "Admit the twenty exact repository targets, privately run the retained callback "
                + "recipe and bounded-tree snapshot crate, and publish only their complete verified candidate.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of("synexia", "m3", "openrewrite", "recipe-first", "candidate-only",
                reconciled ? "m3-capability:pcr" : "m3-capability:pcs");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public boolean causesAnotherCycle() {
        return false;
    }

    @Override
    public Inventory getInitialValue(ExecutionContext context) {
        return new Inventory(targets(reconciled), reconciled);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Inventory inventory) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(Tree tree, ExecutionContext context) {
                if (tree instanceof SourceFile source) {
                    synchronized (inventory) {
                        String rawPath = source.getSourcePath().toString();
                        String canonical = admissionPath(source.getSourcePath());
                        if (inventory.targets.containsKey(canonical) && !rawPath.equals(canonical)) {
                            inventory.conflicts.add("target path alias refused: " + rawPath);
                            return tree;
                        }
                        Target target = inventory.targets.get(rawPath);
                        if (target == null) return tree;
                        if (inventory.seen.putIfAbsent(rawPath, source) != null) {
                            inventory.conflicts.add("duplicate target: " + rawPath);
                        }
                        try {
                            String hash = hash(sourceText(source));
                            inventory.observed.putIfAbsent(rawPath, hash);
                            requireInput(source, target, hash);
                        } catch (IllegalStateException failure) {
                            inventory.conflicts.add(failure.getMessage());
                        }
                    }
                }
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(Inventory inventory, ExecutionContext context) {
        prepare(inventory);
        List<SourceFile> generated = new ArrayList<>();
        for (String path : new TreeMap<>(inventory.targets).keySet()) {
            if (!inventory.seen.containsKey(path)) generated.add(inventory.candidates.get(path));
        }
        return List.copyOf(generated);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Inventory inventory) {
        requireAdmissible(inventory);
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(Tree tree, ExecutionContext context) {
                if (tree instanceof SourceFile source) {
                    prepare(inventory);
                    String rawPath = source.getSourcePath().toString();
                    String canonical = admissionPath(source.getSourcePath());
                    if (inventory.targets.containsKey(canonical) && !rawPath.equals(canonical)) {
                        throw new IllegalStateException("target path alias after scan: " + rawPath);
                    }
                    Target target = inventory.targets.get(rawPath);
                    if (target == null) return tree;
                    String current = hash(sourceText(source));
                    requireInput(source, target, current);
                    String scanned = inventory.observed.get(rawPath);
                    if (scanned == null) {
                        if (!"ABSENT".equals(target.before()) || !target.after().equals(current)) {
                            throw new IllegalStateException("target appeared after scan: " + rawPath);
                        }
                    } else if (!scanned.equals(current)
                            || !source.getId().equals(inventory.seen.get(rawPath).getId())
                            || (source instanceof ParseError)
                                    != (inventory.seen.get(rawPath) instanceof ParseError)) {
                        throw new IllegalStateException("target changed after scan: " + rawPath);
                    }
                    if (target.after().equals(current)) return tree;
                    SourceFile replacement = inventory.candidates.get(rawPath).withId(source.getId());
                    replacement = replacement.withSourcePath(source.getSourcePath());
                    replacement = replacement.withFileAttributes(source.getFileAttributes());
                    replacement = replacement.withCharset(source.getCharset());
                    replacement = replacement.withCharsetBomMarked(source.isCharsetBomMarked());
                    replacement = replacement.withMarkers(source instanceof ParseError
                            ? source.getMarkers().removeByType(ParseExceptionResult.class)
                            : source.getMarkers());
                    return replacement.withChecksum(null);
                }
                return tree;
            }
        };
    }

    public List<String> targetPaths() {
        return List.copyOf(new TreeMap<>(targets(reconciled)).keySet());
    }

    public Map<String, String> preimageManifest() {
        Map<String, String> manifest = new TreeMap<>();
        targets(reconciled).forEach((path, target) -> manifest.put(path, target.before()));
        return Map.copyOf(manifest);
    }

    public Map<String, String> postimageManifest() {
        Map<String, String> manifest = new TreeMap<>();
        targets(reconciled).forEach((path, target) -> manifest.put(path, target.after()));
        return Map.copyOf(manifest);
    }

    public M3EditScope requiredScope() {
        return M3ScopeInference.inferJavaTargets(targetPaths());
    }

    private static void prepare(Inventory inventory) {
        synchronized (inventory) {
            requireAdmissible(inventory);
            if (inventory.failure != null) throw inventory.failure;
            revalidate(inventory);
            if (inventory.candidates != null) return;
            try {
                Map<String, SourceFile> staged = new TreeMap<>();
                List<Boolean> components = inventory.reconciled ? List.of(true) : List.of(true, false);
                for (boolean retained : components) {
                    List<SourceFile> inputs = new ArrayList<>();
                    Set<String> allowed = new HashSet<>();
                    for (Target target : new TreeMap<>(inventory.targets).values()) {
                        if (target.retained() != retained) continue;
                        allowed.add(target.path());
                        SourceFile source = inventory.seen.get(target.path());
                        if (source != null) inputs.add(source);
                    }
                    Recipe component = inventory.reconciled
                            ? new M3HashPinnedJavaSnapshotRecipe(RECONCILED_CRATE)
                            : retained ? new M3ProviderCallbackSupersetRecipe()
                                    : new M3HashPinnedJavaSnapshotRecipe(CRATE_NAME);
                    Map<String, SourceFile> outputs = runIsolated(component, inputs, allowed);
                    if (!outputs.keySet().equals(allowed)) {
                        throw new IllegalStateException("incomplete Pcs component output: " + component.getName());
                    }
                    for (var entry : outputs.entrySet()) {
                        Target target = inventory.targets.get(entry.getKey());
                        SourceFile output = entry.getValue();
                        if (target == null || target.retained() != retained
                                || !(output instanceof J.CompilationUnit)
                                || !target.after().equals(hash(sourceText(output)))) {
                            throw new IllegalStateException("final Pcs target hash/type drift: " + entry.getKey());
                        }
                        requireInput(output, target, target.after());
                        if (staged.putIfAbsent(entry.getKey(), output) != null) {
                            throw new IllegalStateException("overlapping Pcs component output: " + entry.getKey());
                        }
                    }
                }
                revalidate(inventory);
                if (!staged.keySet().equals(inventory.targets.keySet())) {
                    throw new IllegalStateException("incomplete Pcs candidate");
                }
                inventory.candidates = Map.copyOf(staged);
            } catch (RuntimeException failure) {
                inventory.failure = new IllegalStateException("Pcs preparation refused", failure);
                throw inventory.failure;
            }
        }
    }

    /** Recheck the actual immutable source objects captured by the complete scanner pass. */
    private static void revalidate(Inventory inventory) {
        for (var entry : inventory.seen.entrySet()) {
            SourceFile source = entry.getValue();
            String current = hash(sourceText(source));
            if (!entry.getKey().equals(exactPath(source))
                    || !current.equals(inventory.observed.get(entry.getKey()))) {
                throw new IllegalStateException("Pcs source changed after scan: " + entry.getKey());
            }
            requireInput(source, inventory.targets.get(entry.getKey()), current);
        }
    }

    /** Actual scheduler results stay private until the outer preparation barrier succeeds. */
    private static Map<String, SourceFile> runIsolated(
            Recipe recipe, List<SourceFile> inputs, Set<String> allowed) {
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
        var context = new InMemoryExecutionContext(failures::add);
        List<Result> results;
        try {
            results = recipe.run(new InMemoryLargeSourceSet(inputs), context, 1)
                    .getChangeset().getAllResults();
        } catch (RuntimeException failure) {
            failures.add(failure);
            results = List.of();
        }
        if (!failures.isEmpty()) {
            IllegalStateException refusal = new IllegalStateException(
                    "inner recipe refused: " + recipe.getName(), failures.getFirst());
            for (Throwable failure : failures) {
                if (failure != failures.getFirst()) refusal.addSuppressed(failure);
            }
            throw refusal;
        }

        Map<String, SourceFile> outputs = new TreeMap<>();
        for (SourceFile input : inputs) {
            String path = exactPath(input);
            if (!allowed.contains(path) || outputs.putIfAbsent(path, input) != null) {
                throw new IllegalStateException("invalid inner source set: " + path);
            }
        }
        Set<String> changed = new HashSet<>();
        for (Result result : results) {
            SourceFile before = result.getBefore();
            SourceFile after = result.getAfter();
            if (after == null) throw new IllegalStateException("inner deletion refused");
            String path = exactPath(after);
            if (!allowed.contains(path) || !changed.add(path)) {
                throw new IllegalStateException("unexpected inner output: " + path);
            }
            SourceFile original = outputs.get(path);
            if (before == null) {
                if (original != null) throw new IllegalStateException("inner generation collision: " + path);
            } else if (!path.equals(exactPath(before)) || original == null
                    || !before.getId().equals(original.getId())
                    || !sourceText(before).equals(sourceText(original))
                    || !before.getCharset().equals(original.getCharset())
                    || before.isCharsetBomMarked() != original.isCharsetBomMarked()) {
                throw new IllegalStateException("inner rename/preimage change refused: " + path);
            }
            outputs.put(path, after);
        }
        return Map.copyOf(outputs);
    }

    private static void requireAdmissible(Inventory inventory) {
        synchronized (inventory) {
            if (!inventory.conflicts.isEmpty()) {
                throw new IllegalStateException(String.join("; ", inventory.conflicts));
            }
            for (Target target : new TreeMap<>(inventory.targets).values()) {
                if (!"ABSENT".equals(target.before()) && !inventory.seen.containsKey(target.path())) {
                    throw new IllegalStateException("required source missing: " + target.path());
                }
            }
        }
    }

    private static void requireInput(SourceFile source, Target target, String hash) {
        boolean malformedVersion = target.path().equals(M3VersionedProviderSyntaxRepairRecipe.TARGET)
                && M3VersionedProviderSyntaxRepairRecipe.BEFORE.equals(hash) && source instanceof ParseError;
        if (!(source instanceof J.CompilationUnit) && !malformedVersion) {
            throw new IllegalStateException("target is not an admitted Java source: " + target.path());
        }
        if (!StandardCharsets.UTF_8.equals(source.getCharset()) || source.isCharsetBomMarked()
                || sourceText(source).startsWith("\ufeff")) {
            throw new IllegalStateException("target requires UTF-8 without BOM: " + target.path());
        }
        if (!target.before().equals(hash) && !target.after().equals(hash)) {
            throw new IllegalStateException("source drift: " + target.path());
        }
    }

    private static Map<String, Target> targets(boolean reconciled) {
        if (reconciled) return reconciledTargets();
        var retained = new M3ProviderCallbackSupersetRecipe();
        var extension = new M3HashPinnedJavaSnapshotRecipe(CRATE_NAME);
        List<String> retainedPaths = retained.targetPaths();
        Map<String, String> retainedBefore = retained.preimageManifest();
        Map<String, String> retainedAfter = retained.postimageManifest();
        List<String> additionalPaths = extension.targetPaths();
        Map<String, String> additionalBefore = extension.preimageManifest();
        if (retainedPaths.size() != 14 || !additionalPaths.equals(ADDITIONAL_PATHS)
                || !retainedBefore.keySet().equals(Set.copyOf(retainedPaths))
                || !retainedAfter.keySet().equals(Set.copyOf(retainedPaths))
                || !Collections.disjoint(retainedPaths, additionalPaths)) {
            throw new IllegalStateException("Pcs component scope drift or overlap");
        }

        Map<String, Target> targets = new TreeMap<>();
        for (String path : retainedPaths) {
            targets.put(path, new Target(path, retainedBefore.get(path), retainedAfter.get(path), true));
        }
        Map<String, List<String>> snapshotRows = new TreeMap<>();
        for (String line : resource(RESOURCE_ROOT + CRATE_NAME + "/manifest.tsv").lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] cells = line.split("\t", -1);
            if (cells.length != 4 || !cells[1].equals(additionalBefore.get(cells[0]))
                    || !cells[2].matches("[0-9a-f]{64}")
                    || snapshotRows.putIfAbsent(cells[0], List.of(cells)) != null) {
                throw new IllegalStateException("Pcs snapshot manifest changed after engine admission");
            }
        }
        if (!List.copyOf(snapshotRows.keySet()).equals(additionalPaths)) {
            throw new IllegalStateException("Pcs snapshot target set changed after engine admission");
        }

        List<String> provenance = resource(MANIFEST).lines().toList();
        if (provenance.size() != ADDITIONAL_PATHS.size() + 1
                || !provenance.getFirst().equals(
                        "path\tbefore_git_blob\tbefore_sha256\tafter_sha256\tcrate\ttemplate")) {
            throw new IllegalStateException("Pcs provenance manifest scope/header drift");
        }
        String previous = "";
        for (String line : provenance.subList(1, provenance.size())) {
            String[] cells = line.split("\t", -1);
            List<String> snapshot = cells.length == 6 ? snapshotRows.get(cells[0]) : null;
            if (snapshot == null || previous.compareTo(cells[0]) >= 0
                    || !cells[2].equals(snapshot.get(1)) || !cells[3].equals(snapshot.get(2))
                    || !CRATE_NAME.equals(cells[4]) || !cells[5].equals(snapshot.get(3))
                    || !("ABSENT".equals(cells[2]) ? "ABSENT".equals(cells[1])
                            : cells[1].matches("[0-9a-f]{40}"))) {
                throw new IllegalStateException("Pcs provenance disagrees with the admitted snapshot");
            }
            previous = cells[0];
            Target target = new Target(cells[0], cells[2], cells[3], false);
            if (targets.putIfAbsent(target.path(), target) != null) {
                throw new IllegalStateException("duplicate Pcs target: " + target.path());
            }
        }
        List<String> union = List.copyOf(targets.keySet());
        if (targets.size() != 20
                || retained.requiredScope() != M3ScopeInference.inferJavaTargets(retainedPaths)
                || extension.requiredScope() != M3ScopeInference.inferJavaTargets(additionalPaths)
                || M3ScopeInference.inferJavaTargets(union)
                        != retained.requiredScope().promote(extension.requiredScope())) {
            throw new IllegalStateException("Pcs union scope drift");
        }
        return Map.copyOf(targets);
    }

    /** Binds the current flat crate to the same exact scope as the retained Pcs capability. */
    private static Map<String, Target> reconciledTargets() {
        Map<String, Target> retained = targets(false);
        var snapshot = new M3HashPinnedJavaSnapshotRecipe(RECONCILED_CRATE);
        List<String> paths = snapshot.targetPaths();
        Map<String, String> before = snapshot.preimageManifest();
        if (paths.size() != 20 || !paths.equals(List.copyOf(new TreeMap<>(retained).keySet()))
                || !before.keySet().equals(retained.keySet())) {
            throw new IllegalStateException("Pcr scope drift or overlap");
        }
        Map<String, List<String>> rows = new TreeMap<>();
        for (String line : resource(RESOURCE_ROOT + RECONCILED_CRATE + "/manifest.tsv").lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] cells = line.split("\t", -1);
            if (cells.length != 4 || !cells[1].equals(before.get(cells[0]))
                    || !cells[2].matches("[0-9a-f]{64}")
                    || rows.putIfAbsent(cells[0], List.of(cells)) != null) {
                throw new IllegalStateException("Pcr snapshot manifest changed after engine admission");
            }
        }
        if (!List.copyOf(rows.keySet()).equals(paths)) {
            throw new IllegalStateException("Pcr snapshot target set changed after engine admission");
        }
        List<String> provenance = resource(RECONCILED_MANIFEST).lines().toList();
        if (provenance.size() != paths.size() + 1
                || !provenance.getFirst().equals(
                        "path\tbefore_git_blob\tbefore_sha256\tafter_sha256\tcrate\ttemplate")) {
            throw new IllegalStateException("Pcr provenance manifest scope/header drift");
        }
        Map<String, Target> targets = new TreeMap<>();
        String previous = "";
        for (String line : provenance.subList(1, provenance.size())) {
            String[] cells = line.split("\t", -1);
            List<String> row = cells.length == 6 ? rows.get(cells[0]) : null;
            if (row == null || previous.compareTo(cells[0]) >= 0
                    || !cells[2].equals(row.get(1)) || !cells[3].equals(row.get(2))
                    || !RECONCILED_CRATE.equals(cells[4]) || !cells[5].equals(row.get(3))
                    || !("ABSENT".equals(cells[2]) ? "ABSENT".equals(cells[1])
                            : cells[1].matches("[0-9a-f]{40}"))) {
                throw new IllegalStateException("Pcr provenance disagrees with the admitted snapshot");
            }
            previous = cells[0];
            Target target = new Target(cells[0], cells[2], cells[3], true);
            if (targets.putIfAbsent(target.path(), target) != null) {
                throw new IllegalStateException("duplicate Pcr target: " + target.path());
            }
        }
        if (!targets.keySet().equals(retained.keySet())
                || snapshot.requiredScope() != M3ScopeInference.inferJavaTargets(paths)) {
            throw new IllegalStateException("Pcr union scope drift");
        }
        return Map.copyOf(targets);
    }

    private static String exactPath(SourceFile source) {
        String raw = source.getSourcePath().toString();
        if (!raw.equals(canonical(source.getSourcePath())) || source.getSourcePath().isAbsolute()) {
            throw new IllegalStateException("inner source path alias refused: " + raw);
        }
        return raw;
    }

    private static String admissionPath(Path path) {
        String normalized = canonical(path);
        return path.isAbsolute() && normalized.startsWith("/")
                ? normalized.substring(1) : normalized;
    }

    private static String canonical(Path path) {
        return Path.of(path.toString().replace('\\', '/')).normalize().toString().replace('\\', '/');
    }

    private static String sourceText(SourceFile source) {
        return source instanceof ParseError error ? error.getText() : source.printAll();
    }

    private static String resource(String name) {
        try (var stream = Pcs.class.getResourceAsStream(name)) {
            if (stream == null) throw new IllegalStateException("missing Pcs manifest: " + name);
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(stream.readAllBytes())).toString();
        } catch (IOException failure) {
            throw new IllegalStateException("cannot read Pcs manifest", failure);
        }
    }

    private static String hash(String value) {
        try {
            ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(value));
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(encoded);
            return HexFormat.of().formatHex(digest.digest());
        } catch (CharacterCodingException failure) {
            throw new IllegalStateException("source is not losslessly encodable as UTF-8", failure);
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }
}
