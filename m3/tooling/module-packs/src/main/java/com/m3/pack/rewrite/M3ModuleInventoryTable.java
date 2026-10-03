// SPDX-License-Identifier: Apache-2.0
package com.m3.pack.rewrite;

import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

/** Observation table following the existing M3FileAtomCandidateTable pattern. */
public final class M3ModuleInventoryTable extends DataTable<M3ModuleInventoryTable.Row> {
    /** Binds observations to the producing recipe, not to product mutation authority. */
    public M3ModuleInventoryTable(Recipe recipe) {
        super(recipe, "M3 actual module artifacts", "Pinned, explicit Java 21 artifact observations.");
    }

    /** One observed artifact; readiness and demand are deliberately not inferred fields. */
    public record Row(
            @Column(displayName = "Module", description = "Actual explicit module name.") String module,
            @Column(displayName = "Version", description = "Descriptor version, if supplied.") String version,
            @Column(displayName = "SHA-256", description = "Exact archive content identity.") String sha256,
            @Column(displayName = "Class version", description = "Highest effective Java 21 class version.") int classVersion,
            @Column(displayName = "Native entries", description = "Observed native resources, not ABI proof.") String nativeEntries) {}
}
