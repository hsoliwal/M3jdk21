// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.ast.partial;

import com.synexia.mindex.MIndexProgressMonitor;
import java.util.Objects;

/**
 * Master partial-AST coordinator.
 *
 * <p>It owns no parser and no semantic rules. It enforces exact cache identity, monotonic
 * escalation, immutable projection publication, cancellation and pressure-aware retention across
 * parser/compiler adapters.</p>
 */
public final class MIndexPartialAstMaster {
    private final MIndexPartialAstCache cache;

    public MIndexPartialAstMaster(MIndexPartialAstCache cache) {
        this.cache = Objects.requireNonNull(cache, "cache");
    }

    public MIndexPartialAstCache cache() {
        return cache;
    }

    public MIndexPartialAstProjection resolve(
            MIndexPartialAstKey initial,
            MIndexPartialAstAdapter adapter,
            MIndexProgressMonitor monitor) {
        Objects.requireNonNull(initial, "initial");
        Objects.requireNonNull(adapter, "adapter");
        MIndexProgressMonitor actual =
                monitor == null ? MIndexProgressMonitor.none() : monitor;

        actual.begin("resolve partial AST", MIndexPartialAstScope.values().length);
        try {
            MIndexPartialAstKey current = initial;
            int attempts = 0;
            while (true) {
                actual.checkCancelled();
                if (++attempts > MIndexPartialAstScope.values().length) {
                    throw new IllegalStateException("partial AST escalation exceeded scope lattice");
                }

                MIndexPartialAstProjection cached = cache.get(current);
                if (cached != null) return cached;

                MIndexPartialAstAttempt attempt =
                        Objects.requireNonNull(adapter.parse(current, actual), "adapter result");
                actual.worked(1);

                if (attempt instanceof MIndexPartialAstAttempt.Success success) {
                    MIndexPartialAstProjection projection = success.projection();
                    if (!projection.key().equals(current)) {
                        throw new IllegalStateException(
                                "partial AST adapter returned projection for a different request");
                    }
                    cache.put(projection);
                    return projection;
                }
                if (attempt instanceof MIndexPartialAstAttempt.Escalate escalation) {
                    MIndexPartialAstScope required = escalation.requiredScope();
                    if (!required.broaderThan(current.scope())) {
                        throw new IllegalStateException(
                                "partial AST escalation must be monotonic: "
                                        + current.scope() + " -> " + required);
                    }
                    current = current.withScope(required);
                    continue;
                }
                if (attempt instanceof MIndexPartialAstAttempt.Reject rejection) {
                    throw new IllegalArgumentException(
                            "partial AST request rejected: " + rejection.reason());
                }
                throw new IllegalStateException("unknown partial AST adapter result");
            }
        } finally {
            actual.done();
        }
    }
}
