// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.donor;

import com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Provenance-gated donor inlining wrapper.
 *
 * <p>The donor is a build-time source input, never a runtime dependency. This recipe validates the
 * crate's pinned donor identity/license metadata and delegates mutation to the already sealed
 * hash-pinned Java snapshot recipe. The target template is therefore the inlined M3-owned source,
 * not a reference to the donor package.
 */
public final class M3PinnedDonorInlineRecipe extends Recipe {
    private static final String ROOT = "/com/m3/rewrite/donor-inline/";

    private final String crateName;
    private final Donor donor;

    public M3PinnedDonorInlineRecipe(String crateName) {
        if (crateName == null || !crateName.matches("[a-z0-9][a-z0-9-]{0,79}")) {
            throw new IllegalArgumentException("invalid donor inline crate");
        }
        this.crateName = crateName;
        this.donor = readDonor(crateName);
    }

    @Override
    public String getDisplayName() {
        return "Inline pinned FOSS donor into M3";
    }

    @Override
    public String getDescription() {
        return "Validates pinned donor provenance and license, then delegates exact source creation "
                + "or replacement to the M3 hash-pinned Java snapshot recipe.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "donor",
                "inline",
                "transpile",
                "hash-pinned",
                "recipe-first",
                "candidate-only");
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(new M3HashPinnedJavaSnapshotRecipe(crateName));
    }

    public String crateName() {
        return crateName;
    }

    public Donor donor() {
        return donor;
    }

    public record Donor(
            String capabilityId,
            String repository,
            String revision,
            String sourcePath,
            String sourceSha256,
            String license,
            String targetNamespace,
            String dagId) {
        public Donor {
            capabilityId = token(capabilityId, "capabilityId");
            repository = token(repository, "repository");
            if (!repository.startsWith("https://github.com/")) {
                throw new IllegalArgumentException("GitHub donor repository required");
            }
            revision = token(revision, "revision");
            if (!revision.matches("[0-9a-f]{40}")) {
                throw new IllegalArgumentException("pinned 40-hex donor revision required");
            }
            sourcePath = relativePath(sourcePath, "sourcePath");
            sourceSha256 = token(sourceSha256, "sourceSha256");
            if (!sourceSha256.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("donor source SHA-256 required");
            }
            license = token(license, "license");
            if (!Set.of("Apache-2.0", "MIT", "BSD-2-Clause", "BSD-3-Clause").contains(license)) {
                throw new IllegalArgumentException("donor license is not admitted for inlining");
            }
            targetNamespace = token(targetNamespace, "targetNamespace");
            if (!targetNamespace.startsWith("com.m3.")) {
                throw new IllegalArgumentException("donor must inline into M3 namespace");
            }
            dagId = token(dagId, "dagId");
        }
    }

    private static Donor readDonor(String crateName) {
        String text = resource(crateName, "donor.tsv");
        List<String> rows = text.lines()
                .filter(line -> !line.isBlank() && !line.startsWith("#"))
                .toList();
        if (rows.size() != 2
                || !rows.getFirst().equals(
                        "capability_id\trepository\trevision\tsource_path\tsource_sha256"
                                + "\tlicense\ttarget_namespace\tdag_id")) {
            throw new IllegalStateException("invalid donor inline metadata");
        }
        String[] cells = rows.get(1).split("\t", -1);
        if (cells.length != 8) throw new IllegalStateException("invalid donor inline row");
        return new Donor(
                cells[0], cells[1], cells[2], cells[3], cells[4], cells[5], cells[6], cells[7]);
    }

    private static String resource(String crate, String name) {
        String path = ROOT + crate + "/" + name;
        try (var stream = M3PinnedDonorInlineRecipe.class.getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException("missing donor inline resource: " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException("cannot read donor inline resource", failure);
        }
    }

    public static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(Objects.requireNonNull(text, "text").getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static String relativePath(String value, String field) {
        String checked = token(value, field).replace('\\', '/');
        if (checked.startsWith("/") || checked.contains("../") || checked.contains("/./")) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String token(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty() || checked.indexOf('\0') >= 0) throw new IllegalArgumentException(field);
        return checked;
    }
}
