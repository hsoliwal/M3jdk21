// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.vendor;

import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

/** The consumer-switch rows, in the schema of {@code switch_consumers.py}'s {@code CONSUMER_SWITCH.tsv}. */
public final class M3VendoredPinRows extends DataTable<M3VendoredPinRows.Row> {

    /** TSV header, byte-identical to the oracle's. */
    public static final String HEADER = "pom\tgroup\tartifact\tdeclared\teffective_or_new\taction\tresolved";

    public M3VendoredPinRows(Recipe recipe) {
        super(recipe, "Vendored consumer pins", "One row per dependency entry the consumer-pin switch decided on.");
    }

    /** One row; empty strings where the oracle pads. */
    public record Row(
            @Column(displayName = "pom", description = "Pom path relative to the repository root.") String pom,
            @Column(displayName = "group", description = "groupId (empty for plugin dependencies).") String group,
            @Column(displayName = "artifact", description = "artifactId.") String artifact,
            @Column(displayName = "declared", description = "Declared version text, stripped.") String declared,
            @Column(displayName = "effective_or_new", description = "New version when switched, else the effective one.") String effectiveOrNew,
            @Column(displayName = "action", description = "Decision.") String action,
            @Column(displayName = "resolved", description = "Effective version after property resolution.") String resolved) {

        public String tsv() {
            return String.join("\t", pom, group, artifact, declared, effectiveOrNew, action, resolved);
        }
    }
}
