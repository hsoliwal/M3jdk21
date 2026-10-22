// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3;

import java.util.Objects;

/**
 * Thin authority-free Nebula binding to the canonical Synexia Apache-2.0 handoff manifest.
 *
 * <p>The source-side custody implementation remains owned by hsoliwal/com.synexia. This binding
 * cannot apply source changes or relicense Nebula. While the pinned Synexia PR is unmerged it is
 * qualification evidence only.</p>
 */
public final class NebulaM3ApacheHandoffBinding {
    public static final String SCHEMA = "M3_NEBULA_APACHE_HANDOFF_BINDING_V1";
    public static final String UPSTREAM_REPOSITORY = "hsoliwal/com.synexia";
    public static final String UPSTREAM_BRANCH = "m3/apache-handoff-custody-20261007";
    public static final String UPSTREAM_COMMIT =
            "25489c202af43863bda2ae23b67542c363e92450";
    public static final int UPSTREAM_PR = 9642;
    public static final String MANIFEST_PATH = ".m3/apache-handoff.tsv";
    public static final String MANIFEST_SHA256 =
            "98274241cc6a0655e600d5fb5c58d70170aca0111770f95c14609041a1f1289a";
    public static final String SOURCE_LICENSE = "Apache-2.0";
    public static final String COPYRIGHT_NOTICE =
            "Copyright 2026 Hitesh Soliwal and contributors";
    public static final String CANONICAL_OWNER = "synexia-m3-recipe";
    public static final String DELIVERY_STATE = "SOURCE_MERGED_PROOF_PENDING";
    public static final String TARGET_ROLE = "QUALIFICATION_INPUT_ONLY";
    public static final String NEBULA_RETAINED_LICENSE = "EPL-2.0";

    private NebulaM3ApacheHandoffBinding() {}

    public static boolean automaticApplication() {
        return false;
    }

    public static boolean targetRelicenseAuthority() {
        return false;
    }

    public static boolean sourceMutationAuthority() {
        return false;
    }

    public static boolean promotionAuthority() {
        return false;
    }

    public static void requireExact(
            String repository,
            String commit,
            String manifestSha256,
            String sourceLicense,
            String retainedLicense) {
        if (!UPSTREAM_REPOSITORY.equals(Objects.requireNonNull(repository, "repository"))
                || !UPSTREAM_COMMIT.equals(Objects.requireNonNull(commit, "commit"))
                || !MANIFEST_SHA256.equals(
                        Objects.requireNonNull(manifestSha256, "manifestSha256"))
                || !SOURCE_LICENSE.equals(Objects.requireNonNull(sourceLicense, "sourceLicense"))
                || !NEBULA_RETAINED_LICENSE.equals(
                        Objects.requireNonNull(retainedLicense, "retainedLicense"))) {
            throw new IllegalArgumentException("Nebula Synexia Apache handoff binding drift");
        }
    }

    public static String tsv() {
        return "schema\tupstream_repository\tupstream_branch\tupstream_commit\tupstream_pr"
                + "\tmanifest_path\tmanifest_sha256\tlicense\tcopyright_notice\tcanonical_owner"
                + "\tdelivery_state\ttarget_role\tautomatic_application"
                + "\ttarget_relicense_authority\tnebula_retained_license\n"
                + SCHEMA
                + "\t" + UPSTREAM_REPOSITORY
                + "\t" + UPSTREAM_BRANCH
                + "\t" + UPSTREAM_COMMIT
                + "\t" + UPSTREAM_PR
                + "\t" + MANIFEST_PATH
                + "\t" + MANIFEST_SHA256
                + "\t" + SOURCE_LICENSE
                + "\t" + COPYRIGHT_NOTICE
                + "\t" + CANONICAL_OWNER
                + "\t" + DELIVERY_STATE
                + "\t" + TARGET_ROLE
                + "\tfalse\tfalse\t"
                + NEBULA_RETAINED_LICENSE
                + "\n";
    }
}
