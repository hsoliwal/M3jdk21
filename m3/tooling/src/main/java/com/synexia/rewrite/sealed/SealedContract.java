// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite.sealed;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** A complete declared observation envelope; equality is a requirement, NOT behavioral proof. */
public final class SealedContract {
    public enum Facet {
        TYPE_SURFACE, VALUES, EFFECTS, EXCEPTIONS, STATE, ORDERING, CONCURRENCY,
        SERIALIZATION, PROTOCOL, RESOURCES, NONFUNCTIONAL
    }
    private final Map<Facet, String> facets;
    private final String root;
    public SealedContract(Map<Facet, String> observations) {
        EnumMap<Facet, String> copy = new EnumMap<>(Facet.class);
        for (Facet facet : Facet.values()) {
            String value = Objects.requireNonNull(observations.get(facet), "MISSING_CONTRACT_FACET:" + facet);
            if (value.isBlank() || value.length() > 16384) throw new IllegalArgumentException("INVALID_FACET:" + facet);
            SealHash.utf8(value); copy.put(facet, value);
        }
        if (observations.size() != copy.size()) throw new IllegalArgumentException("UNKNOWN_FACET");
        facets = Collections.unmodifiableMap(copy);
        StringBuilder material = new StringBuilder("SEALED-CONTRACT/1\n");
        copy.forEach((key, value) -> material.append(SealHash.frame(key.name(), value)).append('\n'));
        root = SealHash.text(material.toString());
    }
    public Map<Facet, String> facets() { return facets; }
    public String root() { return root; }
    public boolean sameObservations(SealedContract other) { return facets.equals(other.facets); }
    /** Private helper extraction is opt-in because reflection, frames and resource limits can observe it. */
    public boolean permitsPrivateHelpers() {
        return facets.get(Facet.NONFUNCTIONAL).lines().anyMatch("private-helper-shape=UNOBSERVED"::equals)
                && facets.get(Facet.EXCEPTIONS).lines().anyMatch("stack-frames=UNOBSERVED"::equals)
                && facets.get(Facet.RESOURCES).lines().anyMatch("extra-call-depth=ALLOWED"::equals);
    }
}
