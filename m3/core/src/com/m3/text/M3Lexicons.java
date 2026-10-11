/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.util.List;
import java.util.Objects;

/**
 * Stable M3JDK facade for Synexia's typed lexicon receivers.
 *
 * <p>The facade owns no corpus bytes and does not create a second interner. It
 * exposes the immutable proper-name instance projection while leaving lexical
 * token identity and payload admission to their existing owners.</p>
 */
public final class M3Lexicons {
    private final M3InstanceIndexPrecompute properNames;

    public M3Lexicons(M3InstanceIndexPrecompute properNames) {
        this.properNames = Objects.requireNonNull(properNames, "properNames");
    }

    public M3InstanceIndexPrecompute properNames() {
        return properNames;
    }

    public int properNameCount() {
        return properNames.size();
    }

    public M3InstanceIndexPrecompute.InstanceRecord findProperName(String name) {
        return properNames.findByName(name);
    }

    public List<M3InstanceIndexPrecompute.InstanceRecord> properNamesOf(long conceptX) {
        return properNames.instancesOf(conceptX);
    }

    public boolean appliesTo(String source, String revision, String fingerprint,
                             String normalization) {
        return properNames.appliesTo(source, revision, fingerprint, normalization);
    }
}
