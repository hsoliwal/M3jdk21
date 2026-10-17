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

    public static String emit(String json, String successorSha256) {
        Objects.requireNonNull(json, "donation json");
        String afterHash = newHashLine(successorSha256);
        if (count(json, NEW_PATH) == 1 && count(json, afterHash) == 1) {
            return json; // exactly pinned fixed point, later verified by owner SHA.
        }
        if (!ORIGINAL_GIT_BLOB.equals(gitBlob(json))) {
            throw new IllegalStateException("historical donation JSON Git blob drift");
        }
        if (count(json, OLD_PATH) != 1 || count(json, OLD_SHA) != 1) {
            throw new IllegalStateException("ambiguous top-level donation receipt");
        }
        String output = json.replace(OLD_PATH, NEW_PATH).replace(OLD_SHA, afterHash);
        if (count(output, NEW_PATH) != 1 || count(output, afterHash) != 1
                || count(output, OLD_PATH) != 0) {
            throw new IllegalStateException("receipt postimage is ambiguous");
        }
        return output;
    }
}
