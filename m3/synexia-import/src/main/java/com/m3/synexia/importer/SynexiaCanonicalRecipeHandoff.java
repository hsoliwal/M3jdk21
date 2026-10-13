// SPDX-License-Identifier: Apache-2.0
package com.m3.synexia.importer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Source-pin receipt for current Synexia-canonical recipe/policy assets.
 *
 * <p>This is deliberately weaker than {@link SynexiaImportManifest}: it records exact upstream Git
 * custody but grants no automatic materialization or target promotion authority. A later receiver
 * must bind exact bytes/SHA-256 through the normal Synexia import manifest before copying anything.
 */
public record SynexiaCanonicalRecipeHandoff(
        String sourceRepository,
        String sourceRevision,
        List<Entry> entries) {

    public static final String SCHEMA = "M3JDK21_SYNEXIA_RECIPE_HANDOFF_V1";
    public static final String SOURCE_REPOSITORY = "hsoliwal/com.synexia";
    public static final String HEADER =
            "schema\tsource_repository\tsource_revision\tsource_path\tsource_git_blob\t"
                    + "asset_kind\tcanonical_owner\ttarget_disposition\tlicense";

    public enum AssetKind {
        POLICY,
        MACHINE_LEDGER,
        OPENREWRITE_RECIPE
    }

    public enum TargetDisposition {
        SOURCE_PIN_ONLY,
        THIN_RECEIVER_ONLY
    }

    public SynexiaCanonicalRecipeHandoff {
        sourceRepository = token(sourceRepository, "sourceRepository");
        if (!SOURCE_REPOSITORY.equals(sourceRepository)) {
            throw new IllegalArgumentException("unexpected Synexia source repository");
        }
        sourceRevision = commit(sourceRevision);
        entries = Objects.requireNonNull(entries, "entries").stream()
                .map(entry -> Objects.requireNonNull(entry, "entry"))
                .sorted(Comparator.comparing(Entry::sourcePath))
                .toList();
        if (entries.isEmpty()) throw new IllegalArgumentException("empty recipe handoff");
        for (int index = 1; index < entries.size(); index++) {
            if (entries.get(index - 1).sourcePath().equals(entries.get(index).sourcePath())) {
                throw new IllegalArgumentException(
                        "duplicate Synexia source path: " + entries.get(index).sourcePath());
            }
        }
    }

    public static SynexiaCanonicalRecipeHandoff parse(String tsv) {
        List<String> lines = Objects.requireNonNull(tsv, "tsv").lines().toList();
        if (lines.isEmpty() || !HEADER.equals(lines.getFirst())) {
            throw new IllegalArgumentException("invalid recipe handoff header");
        }

        String repository = null;
        String revision = null;
        ArrayList<Entry> entries = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] cells = line.split("\t", -1);
            if (cells.length != 9) {
                throw new IllegalArgumentException("invalid recipe handoff row");
            }
            if (!SCHEMA.equals(cells[0])) {
                throw new IllegalArgumentException("unexpected recipe handoff schema");
            }
            if (repository == null) repository = cells[1];
            if (revision == null) revision = cells[2];
            if (!Objects.equals(repository, cells[1]) || !Objects.equals(revision, cells[2])) {
                throw new IllegalArgumentException("recipe handoff source drift");
            }
            entries.add(new Entry(
                    cells[3],
                    cells[4],
                    AssetKind.valueOf(cells[5]),
                    cells[6],
                    TargetDisposition.valueOf(cells[7]),
                    cells[8]));
        }
        if (repository == null || revision == null) {
            throw new IllegalArgumentException("recipe handoff has no entries");
        }
        return new SynexiaCanonicalRecipeHandoff(repository, revision, entries);
    }

    public boolean materializationAuthority() {
        return false;
    }

    public boolean targetPromotionAuthority() {
        return false;
    }

    public record Entry(
            String sourcePath,
            String sourceGitBlob,
            AssetKind assetKind,
            String canonicalOwner,
            TargetDisposition targetDisposition,
            String license) {
        public Entry {
            sourcePath = relative(sourcePath, "sourcePath");
            sourceGitBlob = gitObject(sourceGitBlob, "sourceGitBlob");
            assetKind = Objects.requireNonNull(assetKind, "assetKind");
            canonicalOwner = token(canonicalOwner, "canonicalOwner");
            targetDisposition = Objects.requireNonNull(targetDisposition, "targetDisposition");
            license = token(license, "license");
            if (!"Apache-2.0".equals(license)) {
                throw new IllegalArgumentException(
                        "automatic Synexia recipe handoff requires Apache-2.0");
            }
            if (assetKind == AssetKind.OPENREWRITE_RECIPE
                    && !canonicalOwner.startsWith("com.synexia.rewrite.")) {
                throw new IllegalArgumentException(
                        "OpenRewrite recipe must remain Synexia-owned");
            }
        }

        public boolean reusableEvolutionAllowedInTarget() {
            return false;
        }
    }

    private static String commit(String value) {
        String checked = token(value, "sourceRevision").toLowerCase(Locale.ROOT);
        if (!(checked.matches("[0-9a-f]{40}") || checked.matches("[0-9a-f]{64}"))) {
            throw new IllegalArgumentException("sourceRevision");
        }
        return checked;
    }

    private static String gitObject(String value, String field) {
        String checked = token(value, field).toLowerCase(Locale.ROOT);
        if (!checked.matches("[0-9a-f]{40}")) throw new IllegalArgumentException(field);
        return checked;
    }

    private static String relative(String value, String field) {
        String checked = token(value, field).replace('\\', '/');
        if (checked.startsWith("/")
                || checked.equals("..")
                || checked.startsWith("../")
                || checked.contains("/../")
                || checked.endsWith("/..")
                || checked.contains("//")) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String token(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
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
