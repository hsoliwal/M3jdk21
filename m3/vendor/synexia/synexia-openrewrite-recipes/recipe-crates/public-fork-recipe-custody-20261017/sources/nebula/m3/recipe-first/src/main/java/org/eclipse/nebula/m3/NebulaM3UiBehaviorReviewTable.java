// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3;

import org.eclipse.nebula.m3.rewrite.convergence.NebulaM3UiBehaviorDonorCatalog;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

/** Read-only Java2s UI behavior obligations emitted by the repository review recipe. */
public final class NebulaM3UiBehaviorReviewTable
        extends DataTable<NebulaM3UiBehaviorReviewTable.Row> {

    public NebulaM3UiBehaviorReviewTable(Recipe recipe) {
        super(
                recipe,
                "Nebula M3 UI behavior donor ledger",
                "Read-only SWT/SWT-2D/Swing/Swing-event behavior obligations; never source-copy authority.");
    }

    public static final class Row {
        @Column(displayName = "Source id", description = "Stable UI behavior donor catalogue id.")
        private final String sourceId;

        @Column(displayName = "Reference", description = "Reference-only Java2s catalogue URL.")
        private final String reference;

        @Column(displayName = "Obligations", description = "Comma-separated observable behavior obligations.")
        private final String obligations;

        @Column(displayName = "Authority", description = "Always READ_ONLY_BEHAVIOR_EVIDENCE.")
        private final String authority;

        Row(NebulaM3UiBehaviorDonorCatalog.Source source) {
            this.sourceId = source.id();
            this.reference = source.url();
            this.obligations = String.join(",", source.obligations());
            this.authority = "READ_ONLY_BEHAVIOR_EVIDENCE";
        }

        public String getSourceId() {
            return sourceId;
        }

        public String getReference() {
            return reference;
        }

        public String getObligations() {
            return obligations;
        }

        public String getAuthority() {
            return authority;
        }
    }
}
