// SPDX-License-Identifier: Apache-2.0
package com.m3.synexia.importer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Fail-closed M3JDK21 receiver policy for Synexia-canonical recipe ownership.
 *
 * <p>The canonical ownership decision is imported from Synexia. This class does not invent or
 * widen ownership. It only applies the pinned borrowing ledger to the current target recipe tree.
 */
public final class SynexiaRecipeOwnershipPolicy {
    public static final String EXPECTED_SCHEMA = "M3_JDK21_SYNEXIA_BORROWING_V1";
    public static final String EXPECTED_TARGET = "hsoliwal/M3jdk21";
    public static final String RECIPE_SOURCE_ROOT =
            "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/";

    public enum Disposition {
        SYNEXIA_CANONICAL(false),
        TARGET_ADAPTER_ONLY(false),
        JDK_TARGET_SPECIFIC(true);

        private final boolean reusableEvolutionAllowed;

        Disposition(boolean reusableEvolutionAllowed) {
            this.reusableEvolutionAllowed = reusableEvolutionAllowed;
        }

        /**
         * Whether this target class/family may grow new reusable implementation logic locally.
         *
         * <p>Target adapters may evolve only as thin adapters; reusable mechanics must move to
         * Synexia first. Therefore only JDK-target-specific owners return true here.
         */
        public boolean reusableEvolutionAllowed() {
            return reusableEvolutionAllowed;
        }
    }

    public record Rule(
            String targetOwner,
            Disposition disposition,
            String canonicalSynexiaOwner,
            String license,
            String rationale) {
        public Rule {
            targetOwner = token(targetOwner, "targetOwner");
            disposition = Objects.requireNonNull(disposition, "disposition");
            canonicalSynexiaOwner = token(canonicalSynexiaOwner, "canonicalSynexiaOwner");
            license = token(license, "license");
            rationale = token(rationale, "rationale");
            if (!"Apache-2.0".equals(license)) {
                throw new IllegalArgumentException("automatic borrowing rule must be Apache-2.0");
            }
        }

        public boolean wildcard() {
            return targetOwner.endsWith("*");
        }

        public String prefix() {
            return wildcard()
                    ? targetOwner.substring(0, targetOwner.length() - 1)
                    : targetOwner;
        }

        public boolean matches(String owner) {
            return wildcard() ? owner.startsWith(prefix()) : owner.equals(targetOwner);
        }
    }

    public record Classification(
            String targetOwner,
            Disposition disposition,
            String canonicalSynexiaOwner,
            String rationale) {
        public Classification {
            targetOwner = token(targetOwner, "targetOwner");
            disposition = Objects.requireNonNull(disposition, "disposition");
            canonicalSynexiaOwner = token(canonicalSynexiaOwner, "canonicalSynexiaOwner");
            rationale = token(rationale, "rationale");
        }

        public boolean reusableEvolutionAllowed() {
            return disposition.reusableEvolutionAllowed();
        }

        public boolean thinAdapterOnly() {
            return disposition == Disposition.TARGET_ADAPTER_ONLY;
        }

        public boolean synexiaCanonicalResidue() {
            return disposition == Disposition.SYNEXIA_CANONICAL;
        }
    }

    public record AuditRow(String sourcePath, String targetOwner, Classification classification) {
        public AuditRow {
            sourcePath = relative(sourcePath, "sourcePath");
            targetOwner = token(targetOwner, "targetOwner");
            classification = Objects.requireNonNull(classification, "classification");
        }
    }

    private final List<Rule> exactRules;
    private final List<Rule> wildcardRules;

    private SynexiaRecipeOwnershipPolicy(List<Rule> rules) {
        List<Rule> exact = new ArrayList<>();
        List<Rule> wildcard = new ArrayList<>();
        for (Rule rule : rules) {
            (rule.wildcard() ? wildcard : exact).add(rule);
        }
        wildcard.sort(
                Comparator.comparingInt((Rule rule) -> rule.prefix().length())
                        .reversed()
                        .thenComparing(Rule::targetOwner));
        exact.sort(Comparator.comparing(Rule::targetOwner));
        exactRules = List.copyOf(exact);
        wildcardRules = List.copyOf(wildcard);
    }

    public static SynexiaRecipeOwnershipPolicy parse(String tsv) {
        Objects.requireNonNull(tsv, "tsv");
        List<String> lines = tsv.lines().toList();
        if (lines.isEmpty()) throw new IllegalArgumentException("empty borrowing ledger");

        String expectedHeader =
                "schema\ttarget_repository\ttarget_owner\tdisposition\t"
                        + "canonical_synexia_owner\tlicense\trationale";
        if (!expectedHeader.equals(lines.getFirst())) {
            throw new IllegalArgumentException("unexpected borrowing ledger header");
        }

        ArrayList<Rule> rules = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            if (line.isBlank()) continue;
            String[] cells = line.split("\t", -1);
            if (cells.length != 7) {
                throw new IllegalArgumentException("invalid borrowing ledger row");
            }
            if (!EXPECTED_SCHEMA.equals(cells[0])) {
                throw new IllegalArgumentException("unexpected borrowing ledger schema");
            }
            if (!EXPECTED_TARGET.equals(cells[1])) {
                throw new IllegalArgumentException("unexpected borrowing ledger target");
            }
            Disposition disposition;
            try {
                disposition = Disposition.valueOf(cells[3]);
            } catch (IllegalArgumentException invalid) {
                throw new IllegalArgumentException(
                        "unsupported borrowing disposition: " + cells[3],
                        invalid);
            }
            rules.add(
                    new Rule(
                            cells[2],
                            disposition,
                            cells[4],
                            cells[5],
                            cells[6]));
        }
        if (rules.isEmpty()) throw new IllegalArgumentException("borrowing ledger has no rules");
        return new SynexiaRecipeOwnershipPolicy(rules);
    }

    public static SynexiaRecipeOwnershipPolicy load(Path ledger) throws IOException {
        Path path = Objects.requireNonNull(ledger, "ledger").toAbsolutePath().normalize();
        if (Files.isSymbolicLink(path) || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException("borrowing ledger must be a regular file");
        }
        return parse(Files.readString(path));
    }

    public Classification require(String targetOwner) {
        String owner = token(targetOwner, "targetOwner");
        for (Rule rule : exactRules) {
            if (rule.matches(owner)) return classification(owner, rule);
        }
        for (Rule rule : wildcardRules) {
            if (rule.matches(owner)) return classification(owner, rule);
        }
        throw new IllegalArgumentException("unclassified M3JDK21 recipe owner: " + owner);
    }

    public void requireReusableEvolutionAllowed(String targetOwner) {
        Classification classification = require(targetOwner);
        if (!classification.reusableEvolutionAllowed()) {
            throw new IllegalStateException(
                    "reusable recipe logic must evolve in Synexia first: "
                            + targetOwner
                            + " -> "
                            + classification.canonicalSynexiaOwner());
        }
    }

    public List<AuditRow> auditRecipeTree(Path repositoryRoot) throws IOException {
        Path root = Objects.requireNonNull(repositoryRoot, "repositoryRoot")
                .toAbsolutePath()
                .normalize();
        if (Files.isSymbolicLink(root) || !Files.isDirectory(root)) {
            throw new IllegalArgumentException("repositoryRoot");
        }
        Path sourceRoot = root.resolve(RECIPE_SOURCE_ROOT).normalize();
        if (!sourceRoot.startsWith(root)
                || Files.isSymbolicLink(sourceRoot)
                || !Files.isDirectory(sourceRoot)) {
            throw new IllegalStateException("M3JDK21 recipe source root missing");
        }

        ArrayList<AuditRow> rows = new ArrayList<>();
        try (var paths = Files.walk(sourceRoot)) {
            for (Path file : paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .sorted()
                    .toList()) {
                if (Files.isSymbolicLink(file)) {
                    throw new IllegalStateException("recipe tree contains symbolic link");
                }
                String sourcePath = normalized(root.relativize(file));
                String owner = ownerFromPath(sourcePath);
                rows.add(new AuditRow(sourcePath, owner, require(owner)));
            }
        }
        if (rows.isEmpty()) throw new IllegalStateException("M3JDK21 recipe tree is empty");
        return List.copyOf(rows);
    }

    public static String ownerFromPath(String sourcePath) {
        String path = relative(sourcePath, "sourcePath");
        if (!path.startsWith(RECIPE_SOURCE_ROOT) || !path.endsWith(".java")) {
            throw new IllegalArgumentException("not an M3JDK21 recipe source: " + sourcePath);
        }
        String relative = path.substring(RECIPE_SOURCE_ROOT.length(), path.length() - ".java".length());
        if (relative.isBlank()) throw new IllegalArgumentException("recipe class path is empty");
        return "com.m3.rewrite." + relative.replace('/', '.');
    }

    private static Classification classification(String owner, Rule rule) {
        return new Classification(
                owner,
                rule.disposition(),
                rule.canonicalSynexiaOwner(),
                rule.rationale());
    }

    private static String normalized(Path path) {
        return path.normalize().toString().replace('\\', '/');
    }

    private static String relative(String value, String field) {
        String checked = token(value, field).replace('\\', '/');
        if (checked.startsWith("/")
                || checked.contains("//")
                || checked.equals("..")
                || checked.startsWith("../")
                || checked.contains("/../")
                || checked.endsWith("/..")) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String token(String value, String field) {
        String checked = Objects.toString(value, "").strip();
        if (checked.isEmpty()
                || checked.indexOf('\0') >= 0
                || checked.indexOf('\t') >= 0
                || checked.indexOf('\r') >= 0
                || checked.indexOf('\n') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }
}
