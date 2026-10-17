// SPDX-License-Identifier: Apache-2.0
package com.synexia.m3.repair;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/** Exact top-level JSON receipt retarget; preserve all historical nested receipts. */
public final class M3DonationPointerRecipe {
    private M3DonationPointerRecipe() { }

    private static final String ORIGINAL_GIT_BLOB =
            "e2cf4b64c119c74c02cf57884885a0e542153aaf";
    private static final String OLD_PATH =
            "  \"installed_source_manifest\": \"qualification/installed-source-enum-set-20261014.tsv\",";
    private static final String NEW_PATH =
            "  \"installed_source_manifest\": \"qualification/installed-source-20261017.tsv\",";
    private static final String OLD_SHA =
            "  \"installed_source_manifest_sha256\": \"90e6e4adcc6a9224b6ca7f85b5d01b950ed8c368fcc660b04ce56fda59eae689\",";
    private static final String HASH_PREFIX =
            "  \"installed_source_manifest_sha256\": \"";
    // An earlier exact source epoch has a second ROOT declaration of the same
    // JSON key; Python json.loads takes the last and ignores our new pointer.
    // Keep its value and history under a unique, non-current provenance key.
    private static final String HISTORICAL_DUPLICATE =
            "  \"installed_source_manifest\": \"qualification/installed-source-enum-set-20261008.tsv\",";
    private static final String HISTORICAL_DISTINCT =
            "  \"historical_installed_source_manifest_20261008\": "
                    + "\"qualification/installed-source-enum-set-20261008.tsv\",";
    private static final String ROOT_MANIFEST_KEY = "  \"installed_source_manifest\": ";

    public static String gitBlob(String content) {
        try {
            byte[] payload = content.getBytes(StandardCharsets.UTF_8);
            byte[] prefix = ("blob " + payload.length + "\0").getBytes(StandardCharsets.US_ASCII);
            var sha = MessageDigest.getInstance("SHA-1");
            sha.update(prefix);
            sha.update(payload);
            return HexFormat.of().formatHex(sha.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static String newHashLine(String successorSha256) {
        if (!successorSha256.matches("[0-9a-f]{64}")) {
            throw new IllegalStateException("invalid successor SHA256");
        }
        return HASH_PREFIX + successorSha256 + "\",";
    }

    private static int count(String content, String exactLine) {
        return content.split(java.util.regex.Pattern.quote(exactLine), -1).length - 1;
    }

    public static String emit(String current, String successorSha256, String pinnedOriginal) {
        Objects.requireNonNull(current, "donation json");
        Objects.requireNonNull(pinnedOriginal, "reviewed original json");
        if (!ORIGINAL_GIT_BLOB.equals(gitBlob(pinnedOriginal))) {
            throw new IllegalStateException("original donation JSON Git blob drift");
        }
        String afterHash = newHashLine(successorSha256);
        if (count(pinnedOriginal, OLD_PATH) != 1 || count(pinnedOriginal, OLD_SHA) != 1
                || count(pinnedOriginal, HISTORICAL_DUPLICATE) != 1
                || count(pinnedOriginal, ROOT_MANIFEST_KEY) != 2
                || count(pinnedOriginal, HISTORICAL_DISTINCT) != 0) {
            throw new IllegalStateException("unexpected historical root manifest inventory");
        }
        // V1 was a source-sealed candidate installed before detecting the duplicate:
        // recognize it exactly, but never mistake it for the qualified fixed point.
        String priorCandidate = pinnedOriginal.replace(OLD_PATH, NEW_PATH)
                .replace(OLD_SHA, afterHash);
        String postimage = priorCandidate.replace(HISTORICAL_DUPLICATE, HISTORICAL_DISTINCT);
        if (count(postimage, NEW_PATH) != 1 || count(postimage, afterHash) != 1
                || count(postimage, OLD_PATH) != 0
                || count(postimage, HISTORICAL_DUPLICATE) != 0
                || count(postimage, HISTORICAL_DISTINCT) != 1
                || count(postimage, ROOT_MANIFEST_KEY) != 1) {
            throw new IllegalStateException("successor root manifest is not unique");
        }
        if (!current.equals(pinnedOriginal) && !current.equals(priorCandidate)
                && !current.equals(postimage)) {
            throw new IllegalStateException("donation JSON source drift / collision");
        }
        return postimage;
    }
}
