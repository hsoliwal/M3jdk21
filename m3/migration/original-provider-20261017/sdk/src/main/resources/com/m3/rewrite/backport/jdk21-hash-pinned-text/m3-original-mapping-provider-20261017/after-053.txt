// SPDX-License-Identifier: Apache-2.0
package jdk.internal.mindex;

import java.util.Objects;

/**
 * Caller-declared release provenance; spelling references use the existing JDK String owner.
 * Adapted from Synexia MIndexLibraryRelease; see m3/tooling/vi/README.md for pinned lineage. Exact raw labels survive comparison-equal versions.
 * This descriptor performs no resolution, download, signature verification or class loading.
 * A content digest is a candidate seal, not proof of a loaded class's definition bytes.
 */
public record M3Release(
        String origin, String group, String artifact,
        String rawVersion, String extension, String classifier,
        String contentSha256) {
    public M3Release {
        require(origin, "origin"); require(group, "group"); require(artifact, "artifact");
        require(rawVersion, "rawVersion"); require(extension, "extension");
        Objects.requireNonNull(classifier, "classifier");
        Objects.requireNonNull(contentSha256, "contentSha256");
        if (contentSha256.length() != 64) throw new IllegalArgumentException("SHA-256 length");
        for (int i = 0; i < contentSha256.length(); i++) {
            char c = contentSha256.charAt(i);
            if (!(c >= '0' && c <= '9' || c >= 'a' && c <= 'f')) {
                throw new IllegalArgumentException("lowercase SHA-256 required");
            }
        }
    }

    private static void require(String value, String name) {
        if (Objects.requireNonNull(value, name).length() == 0) {
            throw new IllegalArgumentException(name);
        }
    }
}
