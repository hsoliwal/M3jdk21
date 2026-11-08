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
package jdk.internal.m3.collections;

import java.util.ArrayList;
import java.util.List;

/**
 * Built-in policy inventory; each score vector is heuristic and versioned by the ID.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/BuiltinProviders.java}
 * blob {@code c4a1c920ae756177149a0b2662a99b90353fd822} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 2 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class M3BuiltinProviders implements M3ShapesCollectionProvider {
    private final M3CollectionCandidate candidate;
    private final String storage;
    private M3BuiltinProviders(M3CollectionRequirements.Kind kind, String storage,
                             M3CollectionRequirements.Order order, M3CandidateProfile profile) {
        this.storage = storage;
        String shape = switch (kind) {
            case MAP -> "map";
            case SET -> "set";
            case SEQUENCE -> "sequence";
            case LINKED_SEQUENCE -> "linked";
            default -> throw new IllegalArgumentException("not a standard shape");
        };
        candidate = new M3CollectionCandidate("m3." + shape + "." + storage + ".v1", kind, order,
                M3CollectionRequirements.Concurrency.SINGLE_THREAD, true, true, true, 1 << 28, profile);
    }
    static List<M3ShapesCollectionProvider> all() {
        var result = new ArrayList<M3ShapesCollectionProvider>();
        for (var kind : List.of(M3CollectionRequirements.Kind.MAP, M3CollectionRequirements.Kind.SET)) {
            result.add(new M3BuiltinProviders(kind, "scan", M3CollectionRequirements.Order.INSERTION,
                    new M3CandidateProfile(M3CandidateProfile.SizeBand.SMALL, 150, 300, 1000, 1000, 200)));
            result.add(new M3BuiltinProviders(kind, "flat-hash", M3CollectionRequirements.Order.UNSPECIFIED,
                    new M3CandidateProfile(M3CandidateProfile.SizeBand.GENERAL, 1000, 900, 650, 700, 250)));
            result.add(new M3BuiltinProviders(kind, "segmented-hash", M3CollectionRequirements.Order.UNSPECIFIED,
                    new M3CandidateProfile(M3CandidateProfile.SizeBand.LARGE, 750, 750, 500, 450, 1000)));
        }
        result.add(new M3BuiltinProviders(M3CollectionRequirements.Kind.SEQUENCE, "flat", M3CollectionRequirements.Order.POSITION,
                new M3CandidateProfile(M3CandidateProfile.SizeBand.GENERAL, 1000, 700, 1000, 900, 200)));
        result.add(new M3BuiltinProviders(M3CollectionRequirements.Kind.SEQUENCE, "segmented", M3CollectionRequirements.Order.POSITION,
                new M3CandidateProfile(M3CandidateProfile.SizeBand.LARGE, 800, 650, 800, 500, 1000)));
        result.add(new M3BuiltinProviders(M3CollectionRequirements.Kind.LINKED_SEQUENCE, "segmented", M3CollectionRequirements.Order.POSITION,
                new M3CandidateProfile(M3CandidateProfile.SizeBand.LARGE, 750, 900, 500, 350, 1000)));
        return List.copyOf(result);
    }
    @Override public M3CollectionCandidate candidate() { return candidate; }

    @Override public int aspects() {
        int mask = candidate.aspects();
        return switch (storage) {
            case "scan", "flat-hash" -> mask | M3CollectionAspectMask.LAZY_STORAGE;
            case "segmented-hash" -> mask
                    | M3CollectionAspectMask.LAZY_STORAGE
                    | M3CollectionAspectMask.NO_PAYLOAD_COPY_ON_GROWTH;
            case "segmented" -> mask
                    | M3CollectionAspectMask.LAZY_STORAGE
                    | M3CollectionAspectMask.NO_PAYLOAD_COPY_ON_GROWTH
                    | (candidate.kind() == M3CollectionRequirements.Kind.LINKED_SEQUENCE
                            ? M3CollectionAspectMask.STABLE_HANDLES : 0);
            case "flat" -> mask;
            default -> throw new AssertionError("unknown built-in storage: " + storage);
        };
    }

    private <K, V> M3KeyStore<K, V> keyStore(M3ShapesCollectionRequest r, boolean values) {
        return switch (storage) {
            case "scan" -> new M3ScanKeyStore<>(r.required(), values);
            case "flat-hash" -> new M3FlatHashKeyStore<>(r.required(), values);
            case "segmented-hash" -> new M3SegmentedHashKeyStore<>(r.required(), values);
            default -> throw new UnsupportedOperationException("not a key store");
        };
    }
    @Override public <K, V> M3ShapeMap<K, V> map(M3ShapesCollectionRequest r) {
        return new M3StoreShapeMap<>(keyStore(r, true));
    }
    @Override public <E> M3ShapeSet<E> set(M3ShapesCollectionRequest r) {
        return new M3StoreShapeSet<>(this.<E, Object>keyStore(r, false));
    }
    @Override public <E> M3ShapeSequence<E> sequence(M3ShapesCollectionRequest r) {
        return storage.equals("flat") ? new M3FlatShapeSequence<>(r.required()) : new M3SegmentedShapeSequence<>(r.required());
    }
    @Override public <E> M3ShapeLinkedSequence<E> linkedSequence(M3ShapesCollectionRequest r) {
        return new M3SegmentedShapeLinkedSequence<>(r.required());
    }
}
