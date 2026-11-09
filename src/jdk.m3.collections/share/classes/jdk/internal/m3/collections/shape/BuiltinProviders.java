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

import java.util.ArrayList;
import java.util.List;
import jdk.internal.m3.collections.CollectionAspectMask;

/**
 * Built-in policy inventory; each score vector is heuristic and versioned by the ID.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/BuiltinProviders.java}
 * blob {@code c4a1c920ae756177149a0b2662a99b90353fd822} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 2 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class BuiltinProviders implements CollectionProvider {
    private final CollectionCandidate candidate;
    private final String storage;
    private BuiltinProviders(CollectionRequirements.Kind kind, String storage,
                             CollectionRequirements.Order order, CandidateProfile profile) {
        this.storage = storage;
        String shape = switch (kind) {
            case MAP -> "map";
            case SET -> "set";
            case SEQUENCE -> "sequence";
            case LINKED_SEQUENCE -> "linked";
            default -> throw new IllegalArgumentException("not a standard shape");
        };
        candidate = new CollectionCandidate("m3." + shape + "." + storage + ".v1", kind, order,
                CollectionRequirements.Concurrency.SINGLE_THREAD, true, true, true, 1 << 28, profile);
    }
    static List<CollectionProvider> all() {
        var result = new ArrayList<CollectionProvider>();
        for (var kind : List.of(CollectionRequirements.Kind.MAP, CollectionRequirements.Kind.SET)) {
            result.add(new BuiltinProviders(kind, "scan", CollectionRequirements.Order.INSERTION,
                    new CandidateProfile(CandidateProfile.SizeBand.SMALL, 150, 300, 1000, 1000, 200)));
            result.add(new BuiltinProviders(kind, "flat-hash", CollectionRequirements.Order.UNSPECIFIED,
                    new CandidateProfile(CandidateProfile.SizeBand.GENERAL, 1000, 900, 650, 700, 250)));
            result.add(new BuiltinProviders(kind, "segmented-hash", CollectionRequirements.Order.UNSPECIFIED,
                    new CandidateProfile(CandidateProfile.SizeBand.LARGE, 750, 750, 500, 450, 1000)));
        }
        result.add(new BuiltinProviders(CollectionRequirements.Kind.SEQUENCE, "flat", CollectionRequirements.Order.POSITION,
                new CandidateProfile(CandidateProfile.SizeBand.GENERAL, 1000, 700, 1000, 900, 200)));
        result.add(new BuiltinProviders(CollectionRequirements.Kind.SEQUENCE, "segmented", CollectionRequirements.Order.POSITION,
                new CandidateProfile(CandidateProfile.SizeBand.LARGE, 800, 650, 800, 500, 1000)));
        result.add(new BuiltinProviders(CollectionRequirements.Kind.LINKED_SEQUENCE, "segmented", CollectionRequirements.Order.POSITION,
                new CandidateProfile(CandidateProfile.SizeBand.LARGE, 750, 900, 500, 350, 1000)));
        return List.copyOf(result);
    }
    @Override public CollectionCandidate candidate() { return candidate; }

    @Override public int aspects() {
        int mask = candidate.aspects();
        return switch (storage) {
            case "scan", "flat-hash" -> mask | CollectionAspectMask.LAZY_STORAGE;
            case "segmented-hash" -> mask
                    | CollectionAspectMask.LAZY_STORAGE
                    | CollectionAspectMask.NO_PAYLOAD_COPY_ON_GROWTH;
            case "segmented" -> mask
                    | CollectionAspectMask.LAZY_STORAGE
                    | CollectionAspectMask.NO_PAYLOAD_COPY_ON_GROWTH
                    | (candidate.kind() == CollectionRequirements.Kind.LINKED_SEQUENCE
                            ? CollectionAspectMask.STABLE_HANDLES : 0);
            case "flat" -> mask;
            default -> throw new AssertionError("unknown built-in storage: " + storage);
        };
    }

    private <K, V> M3KeyStore<K, V> keyStore(CollectionRequest r, boolean values) {
        return switch (storage) {
            case "scan" -> new ScanKeyStore<>(r.required(), values);
            case "flat-hash" -> new FlatHashKeyStore<>(r.required(), values);
            case "segmented-hash" -> new SegmentedHashKeyStore<>(r.required(), values);
            default -> throw new UnsupportedOperationException("not a key store");
        };
    }
    @Override public <K, V> ShapeMap<K, V> map(CollectionRequest r) {
        return new StoreShapeMap<>(keyStore(r, true));
    }
    @Override public <E> ShapeSet<E> set(CollectionRequest r) {
        return new StoreShapeSet<>(this.<E, Object>keyStore(r, false));
    }
    @Override public <E> ShapeSequence<E> sequence(CollectionRequest r) {
        return storage.equals("flat") ? new FlatShapeSequence<>(r.required()) : new SegmentedShapeSequence<>(r.required());
    }
    @Override public <E> ShapeLinkedSequence<E> linkedSequence(CollectionRequest r) {
        return new SegmentedShapeLinkedSequence<>(r.required());
    }
}
