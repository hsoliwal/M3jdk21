// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.util.Objects;

/**
 * Canonical domain-scoped semantic symbol for MIndex reference families.
 *
 * <p>This record is not a memory address. {@code scope} identifies the existing dictionary,
 * namespace, level, language, region, or feature scope required by a reference family and
 * {@code symbol} identifies the value inside that scope. Existing packed reference encodings
 * remain authoritative and can project into this common form without changing their contracts.</p>
 */
public record MIndexSymbolRef(MIndexReferenceDomain domain, long scope, long symbol)
        implements Comparable<MIndexSymbolRef> {

    public MIndexSymbolRef {
        domain = Objects.requireNonNull(domain, "domain");
        if (scope < 0L) {
            throw new IllegalArgumentException("scope must not be negative: " + scope);
        }
        if (symbol < 0L) {
            throw new IllegalArgumentException("symbol must not be negative: " + symbol);
        }
    }

    public boolean sameDomain(MIndexSymbolRef other) {
        return other != null && domain == other.domain;
    }

    public boolean sameScope(MIndexSymbolRef other) {
        return sameDomain(other) && scope == other.scope;
    }

    @Override
    public int compareTo(MIndexSymbolRef other) {
        MIndexSymbolRef checked = Objects.requireNonNull(other, "other");
        int domainOrder = Integer.compare(domain.ordinal(), checked.domain.ordinal());
        if (domainOrder != 0) {
            return domainOrder;
        }
        int scopeOrder = Long.compare(scope, checked.scope);
        return scopeOrder != 0 ? scopeOrder : Long.compare(symbol, checked.symbol);
    }
}
