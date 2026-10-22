// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3;

import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

/** Ordered challenge and GitHub donor review evidence for one Nebula source file/category. */
public final class NebulaM3FastSearchReviewTable
        extends DataTable<NebulaM3FastSearchReviewTable.Row> {

    public NebulaM3FastSearchReviewTable(Recipe recipe) {
        super(
                recipe,
                "Nebula M3 fast-search review ledger",
                "Read-only ordered LeetCode/HackerRank/GeeksforGeeks/GitHub donor review rows.");
    }

    public static final class Row {
        @Column(displayName = "Source path", description = "Repository-relative Java source path.")
        private final String sourcePath;

        @Column(displayName = "Category", description = "Candidate fast-search category.")
        private final String category;

        @Column(displayName = "Pass order", description = "Deterministic serial review ordinal.")
        private final int passOrder;

        @Column(displayName = "Evidence source", description = "Challenge platform or GitHub donor.")
        private final String evidenceSource;

        @Column(displayName = "Reference", description = "Reference URL for this review pass.")
        private final String reference;

        @Column(displayName = "GitHub donor", description = "Pinned GitHub donor repository when applicable.")
        private final String githubDonor;

        @Column(displayName = "Revision", description = "Pinned donor revision when applicable.")
        private final String revision;

        @Column(displayName = "License", description = "Reviewed donor license when applicable.")
        private final String license;

        @Column(displayName = "Disposition", description = "Reference-only or donor-mechanics disposition.")
        private final String disposition;

        @Column(displayName = "Next action", description = "Exactly one deterministic next review action.")
        private final String nextAction;

        @Column(displayName = "Authority", description = "Always READ_ONLY_EVIDENCE.")
        private final String authority;

        Row(
                String sourcePath,
                NebulaM3FastSearchReviewPolicy.ReviewPass pass) {
            this.sourcePath = sourcePath;
            this.category = pass.category().name();
            this.passOrder = pass.passOrder();
            this.evidenceSource = pass.source().name();
            this.reference = pass.reference();
            this.githubDonor = pass.donorRepository();
            this.revision = pass.revision();
            this.license = pass.license();
            this.disposition = pass.disposition();
            this.nextAction = pass.nextAction();
            this.authority = "READ_ONLY_EVIDENCE";
        }

        public String getSourcePath() {
            return sourcePath;
        }

        public String getCategory() {
            return category;
        }

        public int getPassOrder() {
            return passOrder;
        }

        public String getEvidenceSource() {
            return evidenceSource;
        }

        public String getReference() {
            return reference;
        }

        public String getGithubDonor() {
            return githubDonor;
        }

        public String getRevision() {
            return revision;
        }

        public String getLicense() {
            return license;
        }

        public String getDisposition() {
            return disposition;
        }

        public String getNextAction() {
            return nextAction;
        }

        public String getAuthority() {
            return authority;
        }
    }
}
