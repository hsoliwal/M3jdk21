// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Guarded import of source-generated Synexia bridge resources.
 *
 * <p>This class never writes JDK product source. It installs only verified migration-recipe
 * resources; the existing hash-pinned OpenRewrite receivers remain the only product-source
 * mutation authority.</p>
 */
final class SynexiaHandoffImport {
    record Receipt(
            String crateName,
            String sourceRevision,
            String packetRoot,
            int resourceFiles,
            int changedFiles) {}

    private record Change(Path source, Path target) {}
    private record Plan(SynexiaHandoffPacket.Inspection inspection, List<Change> changes) {}

    private SynexiaHandoffImport() {}

    static Receipt inspect(Path exportRoot, Path targetRepo, String crateName) {
        Plan plan = plan(exportRoot, targetRepo, crateName);
        var verified = plan.inspection().verified();
        return new Receipt(
                verified.crateName(),
                verified.sourceRevision(),
                verified.packetRoot(),
                plan.inspection().resourcePaths().size(),
                plan.changes().size());
    }

    static Receipt apply(Path exportRoot, Path targetRepo, String crateName) {
        Plan plan = plan(exportRoot, targetRepo, crateName);
        for (Change change : plan.changes()) {
            writeAtomic(change);
        }
        var verified = plan.inspection().verified();
        return new Receipt(
                verified.crateName(),
                verified.sourceRevision(),
                verified.packetRoot(),
                plan.inspection().resourcePaths().size(),
                plan.changes().size());
    }

    private static Plan plan(Path exportRoot, Path targetRepo, String crateName) {
        Path sourceRoot = realDirectory(exportRoot, "exportRoot");
        Path repoRoot = realDirectory(targetRepo, "targetRepo");
        SynexiaHandoffPacket.Inspection inspection =
                SynexiaHandoffPacket.inspect(sourceRoot, crateName);
        Set<String> expected = Set.copyOf(inspection.resourcePaths());
        assertExactExportTree(sourceRoot, expected);

        List<Change> changes = new ArrayList<>();
        for (String relative : inspection.resourcePaths()) {
            Path source = sourceRoot.resolve(relative).normalize();
            Path target = repoRoot.resolve(relative).normalize();
            if (!source.startsWith(sourceRoot) || !target.startsWith(repoRoot)) {
                throw new IllegalStateException("handoff path escapes root");
            }
            ensureRegularSource(source);
            ensureSafeAncestors(repoRoot, target.getParent());
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)
                        || Files.isSymbolicLink(target)) {
                    throw new IllegalStateException(
                            "unsafe handoff destination: " + relative);
                }
                if (!sameBytes(source, target)) {
                    throw new IllegalStateException(
                            "handoff destination drift: " + relative);
                }
            } else {
                changes.add(new Change(source, target));
            }
        }
        return new Plan(inspection, List.copyOf(changes));
    }

    private static void assertExactExportTree(Path root, Set<String> expected) {
        Set<String> seen = new HashSet<>();
        try (var paths = Files.walk(root)) {
            for (Path path : paths.toList()) {
                if (path.equals(root)) continue;
                if (Files.isSymbolicLink(path)) {
                    throw new IllegalStateException(
                            "symlink in handoff export: " + root.relativize(path));
                }
                if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) continue;
                if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                    throw new IllegalStateException(
                            "non-file in handoff export: " + root.relativize(path));
                }
                String relative =
                        root.relativize(path).toString().replace('\\', '/');
                if (!expected.contains(relative)) {
                    throw new IllegalStateException(
                            "unexpected handoff export file: " + relative);
                }
                seen.add(relative);
            }
        } catch (IOException failure) {
            throw new IllegalStateException("cannot enumerate handoff export", failure);
        }
        if (!seen.equals(expected)) {
            throw new IllegalStateException("handoff export file set mismatch");
        }
    }

    private static Path realDirectory(Path supplied, String field) {
        Path root = Objects.requireNonNull(supplied, field).toAbsolutePath().normalize();
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(root)) {
            throw new IllegalArgumentException(field + " must be a real directory");
        }
        return root;
    }

    private static void ensureRegularSource(Path source) {
        if (!Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(source)) {
            throw new IllegalStateException("unsafe handoff source: " + source);
        }
    }

    private static void ensureSafeAncestors(Path root, Path parent) {
        if (parent == null || !parent.startsWith(root)) {
            throw new IllegalStateException(
                    "handoff destination parent escapes root");
        }
        Path current = root;
        for (Path part : root.relativize(parent)) {
            current = current.resolve(part);
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS)
                    && (!Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)
                            || Files.isSymbolicLink(current))) {
                throw new IllegalStateException(
                        "unsafe handoff destination ancestor: " + current);
            }
        }
    }

    private static boolean sameBytes(Path left, Path right) {
        try {
            return Files.mismatch(left, right) == -1L;
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "cannot compare handoff destination", failure);
        }
    }

    private static void writeAtomic(Change change) {
        Path target = change.target();
        try {
            Files.createDirectories(target.getParent());
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalStateException(
                        "handoff destination changed after preflight: " + target);
            }
            Path temp =
                    Files.createTempFile(
                            target.getParent(), ".m3-synexia-handoff-", ".tmp");
            try {
                Files.copy(change.source(), temp, StandardCopyOption.REPLACE_EXISTING);
                try {
                    Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException unsupported) {
                    throw new IllegalStateException(
                            "atomic handoff resource move unsupported: " + target,
                            unsupported);
                }
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "cannot install handoff resource: " + target, failure);
        }
    }
}
