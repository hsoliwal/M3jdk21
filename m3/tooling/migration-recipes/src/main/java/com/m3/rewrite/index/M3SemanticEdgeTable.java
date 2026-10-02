// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.index;

import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

/** Ordered composition edges between semantic nodes. */
public final class M3SemanticEdgeTable extends DataTable<M3SemanticEdgeTable.Row> {
    public M3SemanticEdgeTable(Recipe recipe) {
        super(
                recipe,
                "M3 semantic edges",
                "Ordered child composition used to reconstruct normalized logic from ATOM to REPOSITORY.");
    }

    public static final class Row {
        @Column(displayName = "Parent ID", description = "Parent semantic node.")
        private final String parentId;
        @Column(displayName = "Child ID", description = "Child semantic node.")
        private final String childId;
        @Column(displayName = "Role", description = "Composition role of the child.")
        private final String role;
        @Column(displayName = "Ordinal", description = "Stable child order within the role.")
        private final int ordinal;

        public Row(String parentId, String childId, String role, int ordinal) {
            this.parentId = parentId;
            this.childId = childId;
            this.role = role;
            this.ordinal = ordinal;
        }

        public String parentId() { return parentId; }
        public String childId() { return childId; }
        public String role() { return role; }
        public int ordinal() { return ordinal; }
    }
}
