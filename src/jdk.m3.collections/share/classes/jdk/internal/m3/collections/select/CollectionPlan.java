/* *****************************************************************************
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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Immutable selection receipt. Planning never constructs the underlying collection.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/selection/CollectionPlan.java}
 * blob {@code 4320504b911a58957704764debc56c28b5545d53} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class CollectionPlan<T> {
    public record Candidate(String providerId, String modelVersion,
            CollectionCapabilities capabilities, CollectionEstimate estimate,
            int memoryFit, int operationFit, int allocationFit, int score) {}

    public record Rejection(String providerId, String modelVersion,
            CollectionCapabilities capabilities, List<String> reasons) {
        public Rejection { reasons = List.copyOf(reasons); }
    }

    private final CollectionRequest<T> request;
    private final List<Candidate> ranked;
    private final List<Rejection> rejected;
    private final List<CollectionProvider<?>> providers;
    private final String receipt;

    CollectionPlan(CollectionRequest<T> request, List<Candidate> ranked,
            List<Rejection> rejected, List<CollectionProvider<?>> providers) {
        this.request = request;
        this.ranked = List.copyOf(ranked);
        this.rejected = List.copyOf(rejected);
        this.providers = List.copyOf(providers);
        this.receipt = digest();
    }

    public CollectionRequest<T> request() { return request; }
    public List<Candidate> ranked() { return ranked; }
    public List<Rejection> rejected() { return rejected; }
    public String receipt() { return receipt; }
    public Optional<Candidate> selected() { return ranked.stream().findFirst(); }

    public T create() {
        if (ranked.isEmpty()) {
            throw new IllegalStateException("No eligible provider for " + request.family().id()
                    + ": " + rejected);
        }
        return create(ranked.get(0).providerId());
    }

    /** Explicit override among eligible providers, useful for controlled comparison/calibration. */
    public T create(String providerId) {
        for (CollectionProvider<?> provider : providers) {
            if (provider.id().equals(providerId)) {
                return request.family().type().cast(provider.create(request));
            }
        }
        throw new IllegalArgumentException("Provider is not eligible: " + providerId);
    }

    private String digest() {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(bytes)) {
                field(out, "m3-collection-plan-v1");
                field(out, request.family().id());
                field(out, request.family().type().getName());
                out.writeInt(request.expectedSize());
                var required = request.requirements();
                field(out, required.order().name());
                out.writeBoolean(required.concurrent());
                out.writeBoolean(required.stableHandles());
                out.writeBoolean(required.lazyStorage());
                out.writeBoolean(required.noPayloadCopyOnGrowth());
                out.writeInt(required.blockSize());
                var work = request.workload();
                out.writeInt(work.lookups()); out.writeInt(work.inserts());
                out.writeInt(work.removes()); out.writeInt(work.scans());
                out.writeInt(work.indexedAccess()); out.writeInt(work.emptyPermille());
                var preferences = request.preferences();
                out.writeInt(preferences.memory()); out.writeInt(preferences.operations());
                out.writeInt(preferences.allocation()); out.writeLong(preferences.payloadTargetBytes());
                out.writeInt(ranked.size());
                for (Candidate candidate : ranked) {
                    field(out, candidate.providerId()); field(out, candidate.modelVersion());
                    capability(out, candidate.capabilities());
                    var estimate = candidate.estimate();
                    out.writeLong(estimate.initialPayloadBytes()); out.writeLong(estimate.expectedPayloadBytes());
                    out.writeInt(estimate.lookupFit()); out.writeInt(estimate.insertFit());
                    out.writeInt(estimate.removeFit()); out.writeInt(estimate.scanFit());
                    out.writeInt(estimate.indexedAccessFit()); out.writeInt(estimate.allocationFit());
                    field(out, estimate.assumptions());
                    out.writeInt(candidate.memoryFit()); out.writeInt(candidate.operationFit());
                    out.writeInt(candidate.allocationFit()); out.writeInt(candidate.score());
                }
                out.writeInt(rejected.size());
                for (Rejection rejection : rejected) {
                    field(out, rejection.providerId()); field(out, rejection.modelVersion());
                    capability(out, rejection.capabilities());
                    out.writeInt(rejection.reasons().size());
                    for (String reason : rejection.reasons()) { field(out, reason); }
                }
            }
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(bytes.toByteArray()));
        } catch (IOException | NoSuchAlgorithmException failure) {
            throw new IllegalStateException("Cannot build selection receipt", failure);
        }
    }

    private static void field(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static void capability(DataOutputStream out, CollectionCapabilities value) throws IOException {
        field(out, value.order().name());
        out.writeBoolean(value.concurrent()); out.writeBoolean(value.stableHandles());
        out.writeBoolean(value.lazyStorage()); out.writeBoolean(value.noPayloadCopyOnGrowth());
        out.writeInt(value.maximumExpectedSize()); out.writeInt(value.minimumBlockSize());
        out.writeInt(value.maximumBlockSize()); out.writeInt(value.defaultBlockSize());
    }
}
