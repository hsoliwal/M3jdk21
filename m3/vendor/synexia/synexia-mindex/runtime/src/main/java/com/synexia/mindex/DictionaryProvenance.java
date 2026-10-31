// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Immutable source/version/license/pipeline/hash lineage for a compiled table. */
public record DictionaryProvenance(
        String sourceDataset,
        String sourceVersion,
        String license,
        String transformationPipeline,
        String sourceSha256) {
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");

    public DictionaryProvenance {
        sourceDataset = requireText(sourceDataset, "sourceDataset");
        sourceVersion = requireText(sourceVersion, "sourceVersion");
        license = requireText(license, "license");
        transformationPipeline = requireText(transformationPipeline, "transformationPipeline");
        sourceSha256 = requireText(sourceSha256, "sourceSha256").toLowerCase(Locale.ROOT);
        if (!SHA256.matcher(sourceSha256).matches()) {
            throw new IllegalArgumentException("sourceSha256 must be 64 hex digits");
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        String normalized = value.strip();
        if (normalized.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return normalized;
    }
}
