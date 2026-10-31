// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.Objects;

/** Exact source observation plus parser/index/precompute fact receipts from the same stream pass. */
public record M3FileIndexedSnapshot(M3FileSnapshot source, M3FileFacts facts) {
    public M3FileIndexedSnapshot {
        source = Objects.requireNonNull(source, "source");
        facts = Objects.requireNonNull(facts, "facts");
        if (!source.hasContentHash()) {
            throw new IllegalArgumentException("indexed snapshot requires exact source content hash");
        }
    }

    public M3FileIndexedDelta deltaTo(M3FileIndexedSnapshot next) {
        M3FileIndexedSnapshot after = Objects.requireNonNull(next, "next");
        return new M3FileIndexedDelta(
                source.deltaTo(after.source),
                facts.deltaTo(after.facts));
    }
}
