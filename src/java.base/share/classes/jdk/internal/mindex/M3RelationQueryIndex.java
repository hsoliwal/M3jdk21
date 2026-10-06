// SPDX-License-Identifier: Apache-2.0
package jdk.internal.mindex;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable internal bundle for one concrete relation-query implementation.
 *
 * <p>The bundle composes direct CSR relations with a bounded transitive-closure image and, for the
 * ATTACK lane only, odd/even parity reachability. It does not define "reasoning" as a domain and it
 * does not replace the source {@link M3ReasoningGraph} as relation authority.</p>
 */
public record M3RelationQueryIndex(
        M3ReasoningGraph graph,
        M3ReasoningRelationKind relationKind,
        M3ReasoningClosure closure,
        Optional<M3AttackParityIndex> attackParity) {

    public M3RelationQueryIndex {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(relationKind, "relationKind");
        Objects.requireNonNull(closure, "closure");
        attackParity = Objects.requireNonNull(attackParity, "attackParity");

        if (closure.graph() != graph || closure.kind() != relationKind) {
            throw new IllegalArgumentException("closure must share graph and relation kind");
        }
        boolean attack = relationKind == M3ReasoningRelationKind.ATTACK;
        if (attack != attackParity.isPresent()) {
            throw new IllegalArgumentException(
                    "attack parity exists exactly for ATTACK relation bundles");
        }
        attackParity.ifPresent(
                parity -> {
                    if (parity.graph() != graph) {
                        throw new IllegalArgumentException(
                                "attack parity must share source graph");
                    }
                });
    }

    public static M3RelationQueryIndex compile(
            M3ReasoningGraph graph, M3ReasoningRelationKind relationKind) {
        return compile(
                graph,
                relationKind,
                M3ReasoningClosure.Budget.DEFAULT,
                M3AttackParityIndex.Budget.DEFAULT);
    }

    public static M3RelationQueryIndex compile(
            M3ReasoningGraph graph,
            M3ReasoningRelationKind relationKind,
            M3ReasoningClosure.Budget closureBudget,
            M3AttackParityIndex.Budget parityBudget) {
        M3ReasoningGraph checkedGraph = Objects.requireNonNull(graph, "graph");
        M3ReasoningRelationKind checkedKind =
                Objects.requireNonNull(relationKind, "relationKind");
        M3ReasoningClosure closure =
                M3ReasoningClosure.compile(
                        checkedGraph,
                        checkedKind,
                        Objects.requireNonNull(closureBudget, "closureBudget"));
        Optional<M3AttackParityIndex> parity =
                checkedKind == M3ReasoningRelationKind.ATTACK
                        ? Optional.of(
                                M3AttackParityIndex.compile(
                                        checkedGraph,
                                        Objects.requireNonNull(
                                                parityBudget, "parityBudget")))
                        : Optional.empty();
        return new M3RelationQueryIndex(checkedGraph, checkedKind, closure, parity);
    }

    public boolean directlyRelated(long sourceId, long targetId) {
        return graph.hasRelation(sourceId, relationKind, targetId);
    }

    public boolean reachable(long sourceId, long targetId) {
        return closure.reachable(sourceId, targetId);
    }

    public long[] reachableFrom(long sourceId) {
        return closure.reachableFrom(sourceId);
    }

    public long retainedPrimitiveBytes() {
        long bytes = closure.retainedPrimitiveBytes();
        if (attackParity.isPresent()) {
            bytes =
                    Math.addExact(
                            bytes,
                            attackParity.orElseThrow().retainedPrimitiveBytes());
        }
        return bytes;
    }
}
