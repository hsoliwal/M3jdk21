// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

/**
 * Filesystem execution shell for a verified classpath Synexia handoff crate.
 *
 * <p>The existing OpenRewrite guard and hash-pinned Java/text recipes remain the transformation
 * authority. This class only loads their exact target preimages, runs those recipes in memory,
 * preflights every resulting write, and atomically materializes the verified postimages.</p>
 */
final class SynexiaHandoffMaterializer {
    private static final String JAVA_ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned/";
    private static final String TEXT_ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned-text/";
    private static final int MAX_TARGETS = 4096;
    private static final long MAX_TARGET_BYTES = 64L * 1024L * 1024L;

    enum Kind { JAVA, TEXT }

    record Receipt(
            String crateName,
            String sourceRevision,
            String packetRoot,
            int targets,
            int changedTargets) {}

    private record Target(Kind kind, String path) {}
    private record Snapshot(Target target, Path file, String beforeText, Set<PosixFilePermission> permissions) {}
    private record Mutation(Snapshot snapshot, String afterText) {}
    private record Plan(SynexiaHandoffPacket.Verified verified, List<Target> targets, List<Mutation> mutations) {}

    private SynexiaHandoffMaterializer() {}

    static Receipt inspect(Path repositoryRoot, String crateName) {
        Plan plan = plan(repositoryRoot, crateName);
        return receipt(plan);
    }

    static Receipt apply(Path repositoryRoot, String crateName) {
        Plan plan = plan(repositoryRoot, crateName);
        for (Mutation mutation : plan.mutations()) {
            write(mutation);
        }
        return receipt(plan);
    }

    private static Receipt receipt(Plan plan) {
        return new Receipt(
                plan.verified().crateName(),
                plan.verified().sourceRevision(),
                plan.verified().packetRoot(),
                plan.targets().size(),
                plan.mutations().size());
    }

    private static Plan plan(Path suppliedRoot, String crateName) {
        Path root = realDirectory(suppliedRoot);
        SynexiaHandoffPacket.Verified verified =
                SynexiaHandoffPacket.verify(SynexiaHandoffPacket.checkedCrate(crateName));
        List<Target> targets = targets(verified.crateName());
        Map<String, Snapshot> snapshots = snapshot(root, targets);

        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        // Execute the provenance gate explicitly before any receiver can nominate a source change.
        String guardedRoot =
                new M3SynexiaHandoffGuardRecipe(verified.crateName()).packetRoot();
        if (!guardedRoot.equals(verified.packetRoot())) {
            throw new IllegalStateException("Synexia handoff guard root changed");
        }

        List<Mutation> mutations = new ArrayList<>();
        List<Target> javaTargets = targets.stream().filter(t -> t.kind() == Kind.JAVA).toList();
        if (!javaTargets.isEmpty()) {
            List<SourceFile> sources = javaSources(javaTargets, snapshots, context);
            List<Result> results =
                    new M3Jdk21HashPinnedSnapshotRecipe(verified.crateName())
                            .run(new InMemoryLargeSourceSet(sources), context)
                            .getChangeset()
                            .getAllResults();
            collect(results, snapshots, Kind.JAVA, mutations);
        }

        List<Target> textTargets = targets.stream().filter(t -> t.kind() == Kind.TEXT).toList();
        if (!textTargets.isEmpty()) {
            List<SourceFile> sources = textSources(textTargets, snapshots);
            List<Result> results =
                    new M3Jdk21HashPinnedTextSnapshotRecipe(verified.crateName())
                            .run(new InMemoryLargeSourceSet(sources), context)
                            .getChangeset()
                            .getAllResults();
            collect(results, snapshots, Kind.TEXT, mutations);
        }
        if (!errors.isEmpty()) {
            IllegalStateException failure =
                    new IllegalStateException("Synexia receiver reported OpenRewrite errors");
            errors.forEach(failure::addSuppressed);
            throw failure;
        }

        mutations.sort(Comparator.comparing(m -> m.snapshot().target().path()));
        return new Plan(verified, targets, List.copyOf(mutations));
    }

    private static List<Target> targets(String crate) {
        List<Target> result = new ArrayList<>();
        Set<String> paths = new HashSet<>();
        loadManifest(JAVA_ROOT, crate, Kind.JAVA, result, paths);
        loadManifest(TEXT_ROOT, crate, Kind.TEXT, result, paths);
        if (result.isEmpty() || result.size() > MAX_TARGETS) {
            throw new IllegalStateException("Synexia handoff materialization target budget");
        }
        result.sort(Comparator.comparing(Target::path));
        return List.copyOf(result);
    }

    private static void loadManifest(
            String root,
            String crate,
            Kind kind,
            List<Target> targets,
            Set<String> paths) {
        String manifest = resourceOrNull(root + crate + "/manifest.tsv");
        if (manifest == null) return;
        for (String line : manifest.lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] cells = line.split("\t", -1);
            if (cells.length != 4
                    || !paths.add(cells[0])
                    || (kind == Kind.JAVA
                            ? !M3Jdk21HandoffPaths.javaSource(cells[0])
                            : !M3Jdk21HashPinnedTextSnapshotRecipe.jdkTextPath(cells[0]))) {
                throw new IllegalStateException("invalid Synexia receiver manifest target");
            }
            targets.add(new Target(kind, cells[0]));
        }
    }

    private static Map<String, Snapshot> snapshot(Path root, List<Target> targets) {
        Map<String, Snapshot> result = new HashMap<>();
        for (Target target : targets) {
            Path file = root.resolve(target.path()).normalize();
            if (!file.startsWith(root)) {
                throw new IllegalStateException("Synexia target escapes repository: " + target.path());
            }
            safeAncestors(root, file.getParent());
            String text = null;
            Set<PosixFilePermission> permissions = Set.of();
            if (Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
                if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                        || Files.isSymbolicLink(file)) {
                    throw new IllegalStateException("unsafe Synexia target: " + target.path());
                }
                text = read(file, target.path());
                permissions = permissions(file);
            }
            result.put(target.path(), new Snapshot(target, file, text, permissions));
        }
        return Map.copyOf(result);
    }

    private static List<SourceFile> javaSources(
            List<Target> targets,
            Map<String, Snapshot> snapshots,
            InMemoryExecutionContext context) {
        List<Parser.Input> inputs = new ArrayList<>();
        for (Target target : targets) {
            Snapshot snapshot = snapshots.get(target.path());
            if (snapshot.beforeText() != null) {
                inputs.add(Parser.Input.fromString(Path.of(target.path()), snapshot.beforeText()));
            }
        }
        if (inputs.isEmpty()) return List.of();
        return JavaParser.fromJavaVersion().build().parseInputs(inputs, null, context).toList();
    }

    private static List<SourceFile> textSources(
            List<Target> targets, Map<String, Snapshot> snapshots) {
        List<SourceFile> sources = new ArrayList<>();
        for (Target target : targets) {
            Snapshot snapshot = snapshots.get(target.path());
            if (snapshot.beforeText() != null) {
                sources.add(
                        PlainText.builder()
                                .sourcePath(Path.of(target.path()))
                                .text(snapshot.beforeText())
                                .build());
            }
        }
        return List.copyOf(sources);
    }

    private static void collect(
            List<Result> results,
            Map<String, Snapshot> snapshots,
            Kind kind,
            List<Mutation> mutations) {
        Set<String> seen = new HashSet<>();
        for (Result result : results) {
            SourceFile after = result.getAfter();
            if (after == null) {
                throw new IllegalStateException("Synexia handoff receiver attempted a deletion");
            }
            String path = after.getSourcePath().normalize().toString().replace('\\', '/');
            Snapshot snapshot = snapshots.get(path);
            if (snapshot == null
                    || snapshot.target().kind() != kind
                    || !seen.add(path)) {
                throw new IllegalStateException(
                        "Synexia handoff receiver changed an unexpected target: " + path);
            }
            mutations.add(new Mutation(snapshot, after.printAll()));
        }
    }

    private static void write(Mutation mutation) {
        Snapshot snapshot = mutation.snapshot();
        Path target = snapshot.file();
        // Recheck immediately before each write. The bridge requires an exclusive checkout, and
        // refuses observable concurrent drift rather than overwriting it.
        if (snapshot.beforeText() == null) {
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalStateException(
                        "Synexia target appeared after preflight: " + snapshot.target().path());
            }
        } else {
            if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)
                    || Files.isSymbolicLink(target)
                    || !snapshot.beforeText().equals(read(target, snapshot.target().path()))) {
                throw new IllegalStateException(
                        "Synexia target changed after preflight: " + snapshot.target().path());
            }
        }

        try {
            Files.createDirectories(target.getParent());
            Path temp =
                    Files.createTempFile(target.getParent(), ".m3-synexia-receiver-", ".tmp");
            try {
                Files.writeString(temp, mutation.afterText(), StandardCharsets.UTF_8);
                if (!snapshot.permissions().isEmpty()
                        && Files.getFileAttributeView(temp, PosixFileAttributeView.class) != null) {
                    Files.setPosixFilePermissions(temp, snapshot.permissions());
                }
                try {
                    Files.move(
                            temp,
                            target,
                            StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException unsupported) {
                    throw new IllegalStateException(
                            "atomic Synexia product-source move unsupported: "
                                    + snapshot.target().path(),
                            unsupported);
                }
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "cannot materialize Synexia receiver target: " + snapshot.target().path(),
                    failure);
        }
    }

    private static Path realDirectory(Path supplied) {
        Path root = Objects.requireNonNull(supplied, "repositoryRoot").toAbsolutePath().normalize();
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(root)) {
            throw new IllegalArgumentException("repositoryRoot must be a real directory");
        }
        return root;
    }

    private static void safeAncestors(Path root, Path parent) {
        if (parent == null || !parent.startsWith(root)) {
            throw new IllegalStateException("Synexia target parent escapes repository");
        }
        Path current = root;
        for (Path part : root.relativize(parent)) {
            current = current.resolve(part);
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS)
                    && (!Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)
                            || Files.isSymbolicLink(current))) {
                throw new IllegalStateException("unsafe Synexia target ancestor: " + current);
            }
        }
    }

    private static String read(Path file, String label) {
        try {
            long size = Files.size(file);
            if (size > MAX_TARGET_BYTES) {
                throw new IllegalStateException("Synexia target file budget: " + label);
            }
            return StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(Files.readAllBytes(file)))
                    .toString();
        } catch (IOException failure) {
            throw new IllegalStateException("cannot read Synexia target: " + label, failure);
        }
    }

    private static Set<PosixFilePermission> permissions(Path file) {
        try {
            if (Files.getFileAttributeView(file, PosixFileAttributeView.class) == null) {
                return Set.of();
            }
            return Set.copyOf(Files.getPosixFilePermissions(file, LinkOption.NOFOLLOW_LINKS));
        } catch (IOException failure) {
            throw new IllegalStateException("cannot read Synexia target permissions: " + file, failure);
        }
    }

    private static String resourceOrNull(String name) {
        try (var stream = SynexiaHandoffMaterializer.class.getResourceAsStream(name)) {
            if (stream == null) return null;
            return StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(stream.readAllBytes()))
                    .toString();
        } catch (IOException failure) {
            throw new IllegalStateException("cannot read Synexia receiver manifest", failure);
        }
    }
}
