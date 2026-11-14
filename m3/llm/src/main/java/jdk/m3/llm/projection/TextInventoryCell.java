// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.projection;

import java.util.Objects;

/** Immutable exact text evidence. A supplied digest must match the UTF-8 bytes. */
public record TextInventoryCell(CellKey key, String text, String contentSha256) {
    public TextInventoryCell {
        Objects.requireNonNull(key, "key");
        text = EntropyValues.text(text, 1_048_576, true);
        String actual = EntropyValues.digest(text);
        if (contentSha256 != null && !contentSha256.isEmpty() && !contentSha256.equals(actual)) {
            throw new IllegalArgumentException("CONTENT_HASH_MISMATCH");
        }
        contentSha256 = actual;
    }

    public TextInventoryCell(CellKey key, String text) {
        this(key, text, "");
    }
}
