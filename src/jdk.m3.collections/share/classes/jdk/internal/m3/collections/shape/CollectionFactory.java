/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections.shape;

import jdk.internal.m3.collections.CollectionAspectMask;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Hard admission then deterministic fuzzy ranking over an immutable provider registry.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/CollectionFactory.java}
 * blob {@code 2db60226feef38148292e1462a803e17cb566e03} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections shape factory batch (v6.2, NOT_HUB): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class CollectionFactory {
    public static final String POLICY = "m3-shapes-integer-fuzzy-v1";
    public static final String ASPECT_POLICY = "m3-shapes-aspect-mask-v1";

    private final List<Registration> registrations;
    private final String registryDigest;
    private final String aspectRegistryDigest;

    private record Registration(
            CollectionProvider provider, CollectionCandidate candidate, int aspects) { }

    private CollectionFactory(Collection<? extends CollectionProvider> providers) {
        var entries = new ArrayList<Registration>();
        for (CollectionProvider provider : providers) {
            Objects.requireNonNull(provider, "provider");
            CollectionCandidate candidate =
                    Objects.requireNonNull(provider.candidate(), "candidate");
            int aspects = provider.aspects();
            CollectionAspectMask.requireKnown(aspects);
            if (!CollectionAspectMask.supports(aspects, candidate.aspects())) {
                throw new IllegalArgumentException(
                        "provider aspects omit declared candidate semantics: " + candidate.id());
            }
            entries.add(new Registration(provider, candidate, aspects));
        }
        entries.sort(Comparator.comparing(entry -> entry.candidate().id()));

        StringBuilder legacy = new StringBuilder(POLICY).append('\n');
        String previous = null;
        for (Registration entry : entries) {
            if (entry.candidate().id().equals(previous)) {
                throw new IllegalArgumentException("duplicate provider id: " + previous);
            }
            previous = entry.candidate().id();
            legacy.append(SelectionDigest.candidate(entry.candidate())).append('\n');
        }

        StringBuilder aspects = new StringBuilder(ASPECT_POLICY).append('\n');
        for (Registration entry : entries) {
            aspects.append(SelectionDigest.candidate(entry.candidate()))
                    .append("|0x")
                    .append(Integer.toUnsignedString(entry.aspects(), 16))
                    .append('\n');
        }

        registrations = List.copyOf(entries);
        registryDigest = SelectionDigest.sha256(legacy.toString());
        aspectRegistryDigest = SelectionDigest.sha256(aspects.toString());
    }

    /** Frozen version-1 registry with the original nine native implementations. */
    public static CollectionFactory standard() { return StandardHolder.INSTANCE; }
    private static final class StandardHolder {
        private static final CollectionFactory INSTANCE =
                new CollectionFactory(BuiltinProviders.all());
    }

    /** Opt-in registry with the original nine and eight additional workload providers. */
    public static CollectionFactory expanded() { return ExpandedHolder.INSTANCE; }
    private static final class ExpandedHolder {
        private static final CollectionFactory INSTANCE =
                new CollectionFactory(WorkloadProviders.all());
    }

    /** Opt-in 21-provider registry including prefix and stable comparator-ranked reductions. */
    public static CollectionFactory reductions() { return ReductionHolder.INSTANCE; }
    private static final class ReductionHolder {
        private static final CollectionFactory INSTANCE =
                new CollectionFactory(ReductionProviders.all());
    }

    /** Opt-in 25-provider registry adding stable ordered slots, indexed sequences and ID heaps. */
    public static CollectionFactory indexed() { return IndexedHolder.INSTANCE; }
    private static final class IndexedHolder {
        private static final CollectionFactory INSTANCE =
                new CollectionFactory(IndexedProviders.all());
    }

    /** Opt-in 27-provider registry including scan and heap queues with primitive live handles. */
    public static CollectionFactory addressable() { return AddressableHolder.INSTANCE; }
    private static final class AddressableHolder {
        private static final CollectionFactory INSTANCE =
                new CollectionFactory(AddressableProviders.all());
    }

    /** Explicit trusted registry, with order-independent descriptors. */
    public static CollectionFactory ofProviders(
            Collection<? extends CollectionProvider> providers) {
        return new CollectionFactory(Objects.requireNonNull(providers, "providers"));
    }

    /** Add one provider without mutating this registry. Duplicate IDs are rejected. */
    public CollectionFactory with(CollectionProvider provider) {
        var providers = new ArrayList<CollectionProvider>();
        for (Registration entry : registrations) {
            providers.add(entry.provider());
        }
        providers.add(provider);
        return new CollectionFactory(providers);
    }

    /** Immutable capability inventory in stable ID order. */
    public List<CollectionCandidate> candidates() {
        return registrations.stream().map(Registration::candidate).toList();
    }

    /** Report-only automatic selection; unsupported semantics fail closed. */
    public CollectionSelection select(CollectionRequest request) {
        return select(request, null);
    }

    /** Select one ID explicitly, still enforcing hard admission; null means fuzzy ranking. */
    public CollectionSelection select(CollectionRequest request, String explicitId) {
        Objects.requireNonNull(request, "request");
        var decisions = decisions(request, 0);
        CandidateDecision winner = winner(decisions, explicitId);
        String text = legacyReceipt(request, explicitId, winner, decisions);
        return new CollectionSelection(
                POLICY,
                request,
                registryDigest,
                winner.id(),
                decisions,
                text,
                SelectionDigest.sha256(text));
    }

    /** Select directly from the compact semantic/residency aspect request. */
    public CollectionSelection select(CollectionAspectRequest request) {
        return select(request, null);
    }

    /** Explicit provider override, still requiring every compact policy aspect. */
    public CollectionSelection select(CollectionAspectRequest request, String explicitId) {
        Objects.requireNonNull(request, "request");
        CollectionRequest semantic = request.semanticRequest();
        var decisions = decisions(semantic, request.policyAspects());
        CandidateDecision winner = winner(decisions, explicitId);
        String text = aspectReceipt(request, semantic, explicitId, winner, decisions);
        return new CollectionSelection(
                ASPECT_POLICY,
                semantic,
                aspectRegistryDigest,
                winner.id(),
                decisions,
                text,
                SelectionDigest.sha256(text));
    }

    private List<CandidateDecision> decisions(CollectionRequest request, int policyAspects) {
        var decisions = new ArrayList<CandidateDecision>();
        for (Registration entry : registrations) {
            var reasons = new ArrayList<>(rejectionReasons(entry.candidate(), request.required()));
            int missing = CollectionAspectMask.missing(entry.aspects(), policyAspects);
            if (missing != 0) {
                reasons.add("ASPECTS:0x" + Integer.toUnsignedString(missing, 16));
            }
            decisions.add(new CandidateDecision(
                    entry.candidate().id(),
                    reasons.isEmpty(),
                    reasons.isEmpty()
                            ? entry.candidate().profile().score(request.preferred())
                            : -1,
                    reasons));
        }
        decisions.sort(Comparator.comparing(CandidateDecision::admitted).reversed()
                .thenComparing(Comparator.comparingLong(CandidateDecision::score).reversed())
                .thenComparing(CandidateDecision::id));
        return List.copyOf(decisions);
    }

    private static CandidateDecision winner(
            List<CandidateDecision> decisions, String explicitId) {
        for (CandidateDecision decision : decisions) {
            if (decision.admitted()
                    && (explicitId == null || explicitId.equals(decision.id()))) {
                return decision;
            }
        }
        throw new IllegalArgumentException("no admitted collection: " + decisions);
    }

    private String legacyReceipt(
            CollectionRequest request,
            String explicitId,
            CandidateDecision winner,
            List<CandidateDecision> decisions) {
        StringBuilder text = new StringBuilder(POLICY).append('\n')
                .append(SelectionDigest.request(request)).append('\n')
                .append(registryDigest).append('\n')
                .append(explicitId == null ? "AUTO" : "EXPLICIT:" + explicitId).append('\n')
                .append(winner.id()).append('\n');
        appendDecisions(text, decisions);
        return text.toString();
    }

    private String aspectReceipt(
            CollectionAspectRequest request,
            CollectionRequest semantic,
            String explicitId,
            CandidateDecision winner,
            List<CandidateDecision> decisions) {
        StringBuilder text = new StringBuilder(ASPECT_POLICY).append('\n')
                .append(SelectionDigest.request(semantic)).append('\n')
                .append(aspectRegistryDigest).append('\n')
                .append("ASPECTS:0x")
                .append(Integer.toUnsignedString(request.aspects(), 16)).append('\n')
                .append("MAXIMUM_SIZE:").append(request.maximumSize()).append('\n')
                .append(explicitId == null ? "AUTO" : "EXPLICIT:" + explicitId).append('\n')
                .append(winner.id()).append('\n');
        appendDecisions(text, decisions);
        return text.toString();
    }

    private static void appendDecisions(
            StringBuilder text, List<CandidateDecision> decisions) {
        for (CandidateDecision decision : decisions) {
            text.append(decision.id()).append('|')
                    .append(decision.admitted()).append('|')
                    .append(decision.score()).append('|')
                    .append(String.join(",", decision.reasons())).append('\n');
        }
    }

    private static List<String> rejectionReasons(
            CollectionCandidate candidate, CollectionRequirements required) {
        var reasons = new ArrayList<String>();
        int missing = candidate.missingAspects(required);
        if ((missing & CollectionAspectMask.KIND_MASK) != 0) {
            reasons.add("SHAPE");
        }
        if ((missing & CollectionAspectMask.ORDER_MASK) != 0) {
            reasons.add("ORDER");
        }
        if ((missing & CollectionAspectMask.CONCURRENCY_MASK) != 0) {
            reasons.add("CONCURRENCY");
        }
        if ((missing & CollectionAspectMask.EQUALITY_VALUE) != 0) {
            reasons.add("VALUE_EQUALITY");
        }
        if ((missing & CollectionAspectMask.EQUALITY_IDENTITY) != 0) {
            reasons.add("IDENTITY_EQUALITY");
        }
        if ((missing & CollectionAspectMask.NULLABLE) != 0) {
            reasons.add("NULL_POLICY");
        }
        if (required.maximumSize() > candidate.maximumSize()) {
            reasons.add("MAXIMUM_SIZE");
        }
        return reasons;
    }

    private CollectionProvider provider(CollectionSelection selection) {
        for (Registration entry : registrations) {
            if (entry.candidate().id().equals(selection.selectedId())) {
                return entry.provider();
            }
        }
        throw new AssertionError("selected provider disappeared");
    }

    private CollectionSelection typed(
            CollectionRequest request, String id, CollectionRequirements.Kind kind) {
        Objects.requireNonNull(request, "request");
        if (request.required().kind() != kind) {
            throw new IllegalArgumentException("wrong factory method for shape");
        }
        return select(request, id);
    }

    private CollectionSelection typed(
            CollectionAspectRequest request, String id, CollectionRequirements.Kind kind) {
        Objects.requireNonNull(request, "request");
        CollectionRequest semantic = request.semanticRequest();
        if (semantic.required().kind() != kind) {
            throw new IllegalArgumentException("wrong factory method for shape");
        }
        return select(request, id);
    }

    private static <T extends ShapeContainer> SelectedCollection<T> result(
            T value, CollectionSelection selected) {
        Objects.requireNonNull(value, "provider returned null");
        if (!value.isEmpty()) {
            throw new IllegalStateException("provider returned nonempty collection");
        }
        return new SelectedCollection<>(value, selected);
    }

    public <E> SelectedCollection<ShapeAddressablePriorityQueue<E>> addressablePriorityQueue(
            CollectionRequest request) {
        return addressablePriorityQueue(request, null);
    }

    public <E> SelectedCollection<ShapeAddressablePriorityQueue<E>> addressablePriorityQueue(
            CollectionRequest request, String id) {
        var selected =
                typed(request, id, CollectionRequirements.Kind.ADDRESSABLE_PRIORITY_QUEUE);
        return result(provider(selected).<E>addressablePriorityQueue(request), selected);
    }

    public <E> SelectedCollection<ShapeAddressablePriorityQueue<E>> addressablePriorityQueue(
            CollectionAspectRequest request) {
        CollectionRequest semantic = request.semanticRequest();
        var selected =
                typed(request, null, CollectionRequirements.Kind.ADDRESSABLE_PRIORITY_QUEUE);
        return result(provider(selected).<E>addressablePriorityQueue(semantic), selected);
    }

    public <K, V> SelectedCollection<ShapeMap<K, V>> map(CollectionRequest request) {
        return map(request, null);
    }

    public <K, V> SelectedCollection<ShapeMap<K, V>> map(
            CollectionRequest request, String id) {
        var selected = typed(request, id, CollectionRequirements.Kind.MAP);
        return result(provider(selected).<K, V>map(request), selected);
    }

    public <K, V> SelectedCollection<ShapeMap<K, V>> map(
            CollectionAspectRequest request) {
        CollectionRequest semantic = request.semanticRequest();
        var selected = typed(request, null, CollectionRequirements.Kind.MAP);
        return result(provider(selected).<K, V>map(semantic), selected);
    }

    public <E> SelectedCollection<ShapeSet<E>> set(CollectionRequest request) {
        return set(request, null);
    }

    public <E> SelectedCollection<ShapeSet<E>> set(
            CollectionRequest request, String id) {
        var selected = typed(request, id, CollectionRequirements.Kind.SET);
        return result(provider(selected).<E>set(request), selected);
    }

    public <E> SelectedCollection<ShapeSet<E>> set(
            CollectionAspectRequest request) {
        CollectionRequest semantic = request.semanticRequest();
        var selected = typed(request, null, CollectionRequirements.Kind.SET);
        return result(provider(selected).<E>set(semantic), selected);
    }

    public <E> SelectedCollection<ShapeSequence<E>> sequence(CollectionRequest request) {
        return sequence(request, null);
    }

    public <E> SelectedCollection<ShapeSequence<E>> sequence(
            CollectionRequest request, String id) {
        var selected = typed(request, id, CollectionRequirements.Kind.SEQUENCE);
        return result(provider(selected).<E>sequence(request), selected);
    }

    public <E> SelectedCollection<ShapeSequence<E>> sequence(
            CollectionAspectRequest request) {
        CollectionRequest semantic = request.semanticRequest();
        var selected = typed(request, null, CollectionRequirements.Kind.SEQUENCE);
        return result(provider(selected).<E>sequence(semantic), selected);
    }

    public <E> SelectedCollection<ShapeLinkedSequence<E>> linkedSequence(
            CollectionRequest request) {
        return linkedSequence(request, null);
    }

    public <E> SelectedCollection<ShapeLinkedSequence<E>> linkedSequence(
            CollectionRequest request, String id) {
        var selected = typed(request, id, CollectionRequirements.Kind.LINKED_SEQUENCE);
        return result(provider(selected).<E>linkedSequence(request), selected);
    }

    public <E> SelectedCollection<ShapeLinkedSequence<E>> linkedSequence(
            CollectionAspectRequest request) {
        CollectionRequest semantic = request.semanticRequest();
        var selected = typed(request, null, CollectionRequirements.Kind.LINKED_SEQUENCE);
        return result(provider(selected).<E>linkedSequence(semantic), selected);
    }

    public <E> SelectedCollection<ShapeDeque<E>> deque(CollectionRequest request) {
        return deque(request, null);
    }

    public <E> SelectedCollection<ShapeDeque<E>> deque(
            CollectionRequest request, String id) {
        var selected = typed(request, id, CollectionRequirements.Kind.DEQUE);
        return result(provider(selected).<E>deque(request), selected);
    }

    public <E> SelectedCollection<ShapeDeque<E>> deque(
            CollectionAspectRequest request) {
        CollectionRequest semantic = request.semanticRequest();
        var selected = typed(request, null, CollectionRequirements.Kind.DEQUE);
        return result(provider(selected).<E>deque(semantic), selected);
    }

    public <E> SelectedCollection<ShapeWindow<E>> window(CollectionRequest request) {
        return window(request, null);
    }

    public <E> SelectedCollection<ShapeWindow<E>> window(
            CollectionRequest request, String id) {
        var selected = typed(request, id, CollectionRequirements.Kind.WINDOW);
        return result(provider(selected).<E>window(request), selected);
    }

    public <E> SelectedCollection<ShapeWindow<E>> window(
            CollectionAspectRequest request) {
        CollectionRequest semantic = request.semanticRequest();
        var selected = typed(request, null, CollectionRequirements.Kind.WINDOW);
        return result(provider(selected).<E>window(semantic), selected);
    }

    public <E> SelectedCollection<ShapePriorityQueue<E>> priorityQueue(
            CollectionRequest request) {
        return priorityQueue(request, null);
    }

    public <E> SelectedCollection<ShapePriorityQueue<E>> priorityQueue(
            CollectionRequest request, String id) {
        var selected = typed(request, id, CollectionRequirements.Kind.PRIORITY_QUEUE);
        return result(provider(selected).<E>priorityQueue(request), selected);
    }

    public <E> SelectedCollection<ShapePriorityQueue<E>> priorityQueue(
            CollectionAspectRequest request) {
        CollectionRequest semantic = request.semanticRequest();
        var selected = typed(request, null, CollectionRequirements.Kind.PRIORITY_QUEUE);
        return result(provider(selected).<E>priorityQueue(semantic), selected);
    }

    public <E> SelectedCollection<ShapeTopK<E>> topK(CollectionRequest request) {
        return topK(request, null);
    }

    public <E> SelectedCollection<ShapeTopK<E>> topK(
            CollectionRequest request, String id) {
        var selected = typed(request, id, CollectionRequirements.Kind.TOP_K);
        return result(provider(selected).<E>topK(request), selected);
    }

    public <E> SelectedCollection<ShapeTopK<E>> topK(
            CollectionAspectRequest request) {
        CollectionRequest semantic = request.semanticRequest();
        var selected = typed(request, null, CollectionRequirements.Kind.TOP_K);
        return result(provider(selected).<E>topK(semantic), selected);
    }

    public <E> SelectedCollection<ShapePrefixSet<E>> prefixSet(CollectionRequest request) {
        return prefixSet(request, null);
    }

    public <E> SelectedCollection<ShapePrefixSet<E>> prefixSet(
            CollectionRequest request, String id) {
        var selected = typed(request, id, CollectionRequirements.Kind.PREFIX_SET);
        return result(provider(selected).<E>prefixSet(request), selected);
    }

    public <E> SelectedCollection<ShapePrefixSet<E>> prefixSet(
            CollectionAspectRequest request) {
        CollectionRequest semantic = request.semanticRequest();
        var selected = typed(request, null, CollectionRequirements.Kind.PREFIX_SET);
        return result(provider(selected).<E>prefixSet(semantic), selected);
    }

    public <E> SelectedCollection<ShapeRankedSelection<E>> rankedSelection(
            CollectionRequest request, Comparator<? super E> comparator) {
        return rankedSelection(request, comparator, null);
    }

    public <E> SelectedCollection<ShapeRankedSelection<E>> rankedSelection(
            CollectionRequest request, Comparator<? super E> comparator, String id) {
        Objects.requireNonNull(comparator, "comparator");
        var selected = typed(request, id, CollectionRequirements.Kind.RANKED_SELECTION);
        return result(provider(selected).<E>rankedSelection(request, comparator), selected);
    }

    public <E> SelectedCollection<ShapeRankedSelection<E>> rankedSelection(
            CollectionAspectRequest request, Comparator<? super E> comparator) {
        Objects.requireNonNull(comparator, "comparator");
        CollectionRequest semantic = request.semanticRequest();
        var selected = typed(request, null, CollectionRequirements.Kind.RANKED_SELECTION);
        return result(provider(selected).<E>rankedSelection(semantic, comparator), selected);
    }
}
