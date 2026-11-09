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
package jdk.internal.m3.collections.select;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Hard filtering followed by deterministic weighted fuzzy memberships; no runtime auto-tuning.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/selection/FuzzyCollectionFactory.java}
 * blob {@code ae0e57e1264589e79ff3328dca099e6a1c0bb1e9} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections catalogue chain after API_UNION U3 (v6.2): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class FuzzyCollectionFactory {
    private final List<CollectionProvider<?>> providers;

    public FuzzyCollectionFactory(Collection<? extends CollectionProvider<?>> providers) {
        var sorted = new ArrayList<CollectionProvider<?>>(providers);
        sorted.sort(Comparator.comparing(CollectionProvider::id));
        List<CollectionFamily<?>> families = new ArrayList<>();
        String previous = null;
        for (CollectionProvider<?> provider : sorted) {
            if (provider.id().equals(previous)) {
                throw new IllegalArgumentException("Duplicate provider ID: " + previous);
            }
            previous = provider.id();
            families.add(provider.family());
        }
        families.sort(Comparator.comparing(CollectionFamily::id));
        for (int index = 1; index < families.size(); index++) {
            CollectionFamily<?> old = families.get(index - 1);
            CollectionFamily<?> next = families.get(index);
            if (old.id().equals(next.id()) && !old.equals(next)) {
                throw new IllegalArgumentException("Conflicting contract for family " + old.id());
            }
        }
        this.providers = List.copyOf(sorted);
    }

    public static FuzzyCollectionFactory standard() { return StandardCollectionCatalog.factory(); }
    public List<CollectionProvider<?>> providers() { return providers; }

    public FuzzyCollectionFactory withProvider(CollectionProvider<?> provider) {
        var extended = new ArrayList<>(providers);
        extended.add(Objects.requireNonNull(provider, "provider"));
        return new FuzzyCollectionFactory(extended);
    }

    public <T> CollectionPlan<T> select(CollectionRequest<T> request) {
        Objects.requireNonNull(request, "request");
        List<CollectionPlan.Candidate> ranked = new ArrayList<>();
        List<CollectionPlan.Rejection> rejected = new ArrayList<>();
        List<CollectionProvider<?>> eligible = new ArrayList<>();
        for (CollectionProvider<?> provider : providers) {
            if (!provider.family().equals(request.family())) { continue; }
            var capabilities = provider.capabilities();
            List<String> reasons = capabilities.rejections(request);
            if (!reasons.isEmpty()) {
                rejected.add(new CollectionPlan.Rejection(provider.id(), provider.modelVersion(),
                        capabilities, reasons));
                continue;
            }
            // Provider/model failures surface to the caller; they never trigger a silent fallback.
            CollectionEstimate estimate = provider.estimate(request);
            long bytes = estimate.weightedPayloadBytes(request.workload().emptyPermille());
            var preferences = request.preferences();
            BigInteger target = BigInteger.valueOf(preferences.payloadTargetBytes());
            int memory = target.multiply(BigInteger.valueOf(1000))
                    .divide(target.add(BigInteger.valueOf(bytes))).intValueExact();
            int operations = estimate.operationFit(request.workload());
            int allocation = estimate.allocationFit();
            int score = (memory * preferences.memory() + operations * preferences.operations()
                    + allocation * preferences.allocation())
                    / (preferences.memory() + preferences.operations() + preferences.allocation());
            ranked.add(new CollectionPlan.Candidate(provider.id(), provider.modelVersion(),
                    capabilities, estimate, memory, operations, allocation, score));
            eligible.add(provider);
        }
        ranked.sort(Comparator.comparingInt(CollectionPlan.Candidate::score).reversed()
                .thenComparing(CollectionPlan.Candidate::providerId));
        return new CollectionPlan<>(request, ranked, rejected, eligible);
    }
}
