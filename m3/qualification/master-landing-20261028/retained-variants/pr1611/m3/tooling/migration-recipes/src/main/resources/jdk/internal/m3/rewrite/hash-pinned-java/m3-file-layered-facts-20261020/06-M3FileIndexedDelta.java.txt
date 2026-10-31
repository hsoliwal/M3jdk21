// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.Objects;

/** Downstream delta combining exact source change evidence with named semantic/precompute facts. */
public record M3FileIndexedDelta(M3FileDelta source, M3FileFactDelta facts) {
    public M3FileIndexedDelta {
        source = Objects.requireNonNull(source, "source");
        facts = Objects.requireNonNull(facts, "facts");
    }

    public boolean changed() {
        return source.changed() || !facts.isEmpty();
    }
}
