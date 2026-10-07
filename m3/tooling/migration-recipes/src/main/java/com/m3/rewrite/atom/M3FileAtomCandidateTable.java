// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.atom;

import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

/** Structured FILE-scope atomization candidates emitted without mutating product source. */
public final class M3FileAtomCandidateTable extends DataTable<M3FileAtomCandidateTable.Row> {
    public M3FileAtomCandidateTable(Recipe recipe) {
        super(
                recipe,
                "M3 FILE atomization candidates",
                "Contract-preserving FILE-scope leaves eligible for a tested M3 OpenRewrite recipe.");
    }

    /** One mechanically admitted FILE-scope candidate. */
    public static final class Row {
        @Column(displayName = "Source path", description = "Repository-relative Java source path.")
        private final String sourcePath;

        @Column(displayName = "Method", description = "Method containing the admitted semantic leaf.")
        private final String methodName;

        @Column(displayName = "Scope", description = "Narrowest mutation authority required.")
        private final String scope;

        @Column(displayName = "Contract mode", description = "Contract preservation mode.")
        private final String contractMode;

        @Column(displayName = "Pattern role", description = "IOP/pattern role assigned to the atom.")
        private final String patternRole;

        @Column(displayName = "Recipe", description = "Exact recipe class able to transform the candidate.")
        private final String recipeClass;

        public Row(
                String sourcePath,
                String methodName,
                String scope,
                String contractMode,
                String patternRole,
                String recipeClass) {
            this.sourcePath = sourcePath;
            this.methodName = methodName;
            this.scope = scope;
            this.contractMode = contractMode;
            this.patternRole = patternRole;
            this.recipeClass = recipeClass;
        }

        public String sourcePath() {
            return sourcePath;
        }

        public String methodName() {
            return methodName;
        }

        public String scope() {
            return scope;
        }

        public String contractMode() {
            return contractMode;
        }

        public String patternRole() {
            return patternRole;
        }

        public String recipeClass() {
            return recipeClass;
        }
    }
}
