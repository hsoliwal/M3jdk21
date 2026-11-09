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
package jdk.internal.m3.collections;

import java.util.Objects;
import jdk.internal.m3.collections.select.CollectionCapabilities;
import jdk.internal.m3.collections.shape.CollectionCandidate;
import jdk.internal.m3.collections.shape.CollectionRequirements;

/**
 * Primitive construction-time descriptor for collection semantics and residency eligibility.
 *
 * <p>The mask is metadata only. It owns no collection state and allocates no per-element object.
 * Hard admission is a subset test: {@code (required & ~provided) == 0}.</p>
 *
 * <p>Physical implementation choices such as hash/sorted/direct indexing are deliberately not
 * encoded here. They belong to the selected provider, not to the caller's semantic contract.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/CollectionAspectMask.java}
 * blob {@code ce33dd6e10168fcbaf261a1f948c49fe6ec30674} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class CollectionAspectMask {
    public static final int KIND_MAP = 1 << 0;
    public static final int KIND_SET = 1 << 1;
    public static final int KIND_SEQUENCE = 1 << 2;
    public static final int KIND_LINKED_SEQUENCE = 1 << 3;
    public static final int KIND_DEQUE = 1 << 4;
    public static final int KIND_WINDOW = 1 << 5;
    public static final int KIND_PRIORITY_QUEUE = 1 << 6;
    public static final int KIND_TOP_K = 1 << 7;
    public static final int KIND_PREFIX_SET = 1 << 8;
    public static final int KIND_RANKED_SELECTION = 1 << 9;
    public static final int KIND_ADDRESSABLE_PRIORITY_QUEUE = 1 << 10;

    public static final int ORDER_INSERTION = 1 << 11;
    public static final int ORDER_ACCESS = 1 << 12;
    public static final int ORDER_POSITION = 1 << 13;
    public static final int ORDER_SORTED = 1 << 14;
    public static final int ORDER_MIN_FIRST = 1 << 15;
    public static final int ORDER_MAX_FIRST = 1 << 16;

    public static final int EQUALITY_VALUE = 1 << 17;
    public static final int EQUALITY_IDENTITY = 1 << 18;
    public static final int CONCURRENCY_SINGLE_THREAD = 1 << 19;
    public static final int CONCURRENCY_CONCURRENT = 1 << 20;
    public static final int NULLABLE = 1 << 21;

    public static final int STABLE_HANDLES = 1 << 22;
    public static final int LAZY_STORAGE = 1 << 23;
    public static final int NO_PAYLOAD_COPY_ON_GROWTH = 1 << 24;

    /** Owner is allowed to expose stable page/segment residency boundaries. */
    public static final int PAGEABLE = 1 << 25;
    /** SQLite is an admitted cold implementation for a PAGEABLE owner. */
    public static final int SQLITE_COLD = 1 << 26;
    /** Optional residency may be adjusted by the existing MIndex pressure plane. */
    public static final int PRESSURE_ADAPTIVE = 1 << 27;

    /**
     * Caller requests an ordinary JDK compatibility facade where required API methods may return a
     * fresh Map.Entry projection. This never permits retained Entry/Node storage.
     */
    public static final int JDK_API_COMPAT = 1 << 28;

    /** The logical value is a frozen/snapshot owner rather than a mutable live owner. */
    public static final int IMMUTABLE_SNAPSHOT = 1 << 29;

    public static final int KIND_MASK =
            KIND_MAP | KIND_SET | KIND_SEQUENCE | KIND_LINKED_SEQUENCE | KIND_DEQUE | KIND_WINDOW
                    | KIND_PRIORITY_QUEUE | KIND_TOP_K | KIND_PREFIX_SET | KIND_RANKED_SELECTION
                    | KIND_ADDRESSABLE_PRIORITY_QUEUE;
    public static final int ORDER_MASK =
            ORDER_INSERTION | ORDER_ACCESS | ORDER_POSITION | ORDER_SORTED | ORDER_MIN_FIRST
                    | ORDER_MAX_FIRST;
    public static final int EQUALITY_MASK = EQUALITY_VALUE | EQUALITY_IDENTITY;
    public static final int CONCURRENCY_MASK =
            CONCURRENCY_SINGLE_THREAD | CONCURRENCY_CONCURRENT;
    public static final int SEMANTIC_MASK =
            KIND_MASK | ORDER_MASK | EQUALITY_MASK | CONCURRENCY_MASK | NULLABLE;
    public static final int STORAGE_REQUIREMENT_MASK =
            STABLE_HANDLES | LAZY_STORAGE | NO_PAYLOAD_COPY_ON_GROWTH;
    public static final int RESIDENCY_MASK = PAGEABLE | SQLITE_COLD | PRESSURE_ADAPTIVE;
    public static final int API_MASK = JDK_API_COMPAT | IMMUTABLE_SNAPSHOT;
    public static final int KNOWN_MASK =
            SEMANTIC_MASK | STORAGE_REQUIREMENT_MASK | RESIDENCY_MASK | API_MASK;

    private CollectionAspectMask() { }

    /** True exactly when every required bit is advertised by the provider. */
    public static boolean supports(int provided, int required) {
        requireKnown(provided);
        requireKnown(required);
        return (required & ~provided) == 0;
    }

    /** Missing hard requirements, suitable for diagnostics or refusal. */
    public static int missing(int provided, int required) {
        requireKnown(provided);
        requireKnown(required);
        return required & ~provided;
    }

    public static boolean has(int mask, int aspect) {
        requireKnown(mask);
        if (aspect == 0 || (aspect & ~KNOWN_MASK) != 0) {
            throw new IllegalArgumentException("unknown/empty collection aspect");
        }
        return (mask & aspect) == aspect;
    }

    /** Existing shape hard requirements represented without semantic loss. */
    public static int shapeRequirements(CollectionRequirements requirements) {
        Objects.requireNonNull(requirements, "requirements");
        int mask = kind(requirements.kind())
                | order(requirements.order())
                | equality(requirements.equality())
                | concurrency(requirements.concurrency());
        if (requirements.allowNull()) {
            mask |= NULLABLE;
        }
        requireShapeRequirementWord(mask);
        return mask;
    }

    /** Existing shape provider semantics represented as capability bits. */
    public static int shapeCandidate(CollectionCandidate candidate) {
        Objects.requireNonNull(candidate, "candidate");
        int mask = kind(candidate.kind())
                | order(candidate.order())
                | concurrency(candidate.concurrency());
        if (candidate.valueEquality()) {
            mask |= EQUALITY_VALUE;
        }
        if (candidate.identityEquality()) {
            mask |= EQUALITY_IDENTITY;
        }
        if (candidate.nullable()) {
            mask |= NULLABLE;
        }
        requireKnown(mask);
        return mask;
    }

    /** Existing selection requirements represented without changing their false-means-not-required policy. */
    public static int selectionRequirements(
            jdk.internal.m3.collections.select.CollectionRequest.Requirements requirements) {
        Objects.requireNonNull(requirements, "requirements");
        int mask = selectionOrder(requirements.order());
        if (requirements.concurrent()) {
            mask |= CONCURRENCY_CONCURRENT;
        }
        if (requirements.stableHandles()) {
            mask |= STABLE_HANDLES;
        }
        if (requirements.lazyStorage()) {
            mask |= LAZY_STORAGE;
        }
        if (requirements.noPayloadCopyOnGrowth()) {
            mask |= NO_PAYLOAD_COPY_ON_GROWTH;
        }
        requireKnown(mask);
        return mask;
    }

    /** Existing selection provider promises represented as capability bits. */
    public static int selectionCapabilities(CollectionCapabilities capabilities) {
        Objects.requireNonNull(capabilities, "capabilities");
        int mask = selectionOrder(capabilities.order());
        if (capabilities.concurrent()) {
            mask |= CONCURRENCY_CONCURRENT;
        }
        if (capabilities.stableHandles()) {
            mask |= STABLE_HANDLES;
        }
        if (capabilities.lazyStorage()) {
            mask |= LAZY_STORAGE;
        }
        if (capabilities.noPayloadCopyOnGrowth()) {
            mask |= NO_PAYLOAD_COPY_ON_GROWTH;
        }
        requireKnown(mask);
        return mask;
    }

    public static int kind(CollectionRequirements.Kind kind) {
        return switch (Objects.requireNonNull(kind, "kind")) {
            case MAP -> KIND_MAP;
            case SET -> KIND_SET;
            case SEQUENCE -> KIND_SEQUENCE;
            case LINKED_SEQUENCE -> KIND_LINKED_SEQUENCE;
            case DEQUE -> KIND_DEQUE;
            case WINDOW -> KIND_WINDOW;
            case PRIORITY_QUEUE -> KIND_PRIORITY_QUEUE;
            case TOP_K -> KIND_TOP_K;
            case PREFIX_SET -> KIND_PREFIX_SET;
            case RANKED_SELECTION -> KIND_RANKED_SELECTION;
            case ADDRESSABLE_PRIORITY_QUEUE -> KIND_ADDRESSABLE_PRIORITY_QUEUE;
        };
    }

    public static int order(CollectionRequirements.Order order) {
        return switch (Objects.requireNonNull(order, "order")) {
            case UNSPECIFIED -> 0;
            case INSERTION -> ORDER_INSERTION;
            case POSITION -> ORDER_POSITION;
            case SORTED -> ORDER_SORTED;
        };
    }

    public static int equality(CollectionRequirements.Equality equality) {
        return switch (Objects.requireNonNull(equality, "equality")) {
            case VALUE -> EQUALITY_VALUE;
            case IDENTITY -> EQUALITY_IDENTITY;
        };
    }

    public static int concurrency(CollectionRequirements.Concurrency concurrency) {
        return switch (Objects.requireNonNull(concurrency, "concurrency")) {
            case SINGLE_THREAD -> CONCURRENCY_SINGLE_THREAD;
            case CONCURRENT -> CONCURRENCY_CONCURRENT;
        };
    }

    public static int selectionOrder(
            jdk.internal.m3.collections.select.CollectionRequest.Order order) {
        return switch (Objects.requireNonNull(order, "order")) {
            case ANY -> 0;
            case INSERTION -> ORDER_INSERTION;
            case ACCESS -> ORDER_ACCESS;
            case SORTED -> ORDER_SORTED;
            case MIN_FIRST -> ORDER_MIN_FIRST;
            case MAX_FIRST -> ORDER_MAX_FIRST;
        };
    }

    public static CollectionRequirements.Kind shapeKind(int mask) {
        requireShapeRequirementWord(mask);
        int kind = mask & KIND_MASK;
        if (kind == KIND_MAP) return CollectionRequirements.Kind.MAP;
        if (kind == KIND_SET) return CollectionRequirements.Kind.SET;
        if (kind == KIND_SEQUENCE) return CollectionRequirements.Kind.SEQUENCE;
        if (kind == KIND_LINKED_SEQUENCE) return CollectionRequirements.Kind.LINKED_SEQUENCE;
        if (kind == KIND_DEQUE) return CollectionRequirements.Kind.DEQUE;
        if (kind == KIND_WINDOW) return CollectionRequirements.Kind.WINDOW;
        if (kind == KIND_PRIORITY_QUEUE) return CollectionRequirements.Kind.PRIORITY_QUEUE;
        if (kind == KIND_TOP_K) return CollectionRequirements.Kind.TOP_K;
        if (kind == KIND_PREFIX_SET) return CollectionRequirements.Kind.PREFIX_SET;
        if (kind == KIND_RANKED_SELECTION) return CollectionRequirements.Kind.RANKED_SELECTION;
        return CollectionRequirements.Kind.ADDRESSABLE_PRIORITY_QUEUE;
    }

    public static CollectionRequirements.Order shapeOrder(int mask) {
        requireShapeRequirementWord(mask);
        int order = mask & ORDER_MASK;
        if (order == 0) return CollectionRequirements.Order.UNSPECIFIED;
        if (order == ORDER_INSERTION) return CollectionRequirements.Order.INSERTION;
        if (order == ORDER_POSITION) return CollectionRequirements.Order.POSITION;
        if (order == ORDER_SORTED) return CollectionRequirements.Order.SORTED;
        throw new IllegalArgumentException("order is not supported by shape requirements");
    }

    public static CollectionRequirements.Equality shapeEquality(int mask) {
        requireShapeRequirementWord(mask);
        return (mask & EQUALITY_IDENTITY) != 0
                ? CollectionRequirements.Equality.IDENTITY
                : CollectionRequirements.Equality.VALUE;
    }

    public static CollectionRequirements.Concurrency shapeConcurrency(int mask) {
        requireShapeRequirementWord(mask);
        return (mask & CONCURRENCY_CONCURRENT) != 0
                ? CollectionRequirements.Concurrency.CONCURRENT
                : CollectionRequirements.Concurrency.SINGLE_THREAD;
    }

    /**
     * Validate one shape requirement word.
     *
     * <p>Residency/API bits may coexist with semantics for a higher-level construction request.
     * Shape semantics require exactly one kind/equality/concurrency and at most one order.</p>
     */
    public static void requireShapeRequirementWord(int mask) {
        requireKnown(mask);
        exactlyOne(mask & KIND_MASK, "kind");
        atMostOne(mask & ORDER_MASK, "order");
        exactlyOne(mask & EQUALITY_MASK, "equality");
        exactlyOne(mask & CONCURRENCY_MASK, "concurrency");
        requireResidencyDependencies(mask);
    }

    /** Refuse lossy conversion of storage/residency/API policy into legacy semantic records. */
    public static void requireSemanticOnly(int mask) {
        requireShapeRequirementWord(mask);
        int policy = mask & ~SEMANTIC_MASK;
        if (policy != 0) {
            throw new IllegalArgumentException(
                    "policy aspects require CollectionAspectRequest: 0x"
                            + Integer.toHexString(policy));
        }
    }

    /** Validate generic cross-selector aspect dependencies. */
    public static void requireKnown(int mask) {
        int unknown = mask & ~KNOWN_MASK;
        if (unknown != 0) {
            throw new IllegalArgumentException(
                    "unknown collection aspect bits: 0x" + Integer.toHexString(unknown));
        }
        requireResidencyDependencies(mask);
    }

    private static void requireResidencyDependencies(int mask) {
        if ((mask & SQLITE_COLD) != 0 && (mask & PAGEABLE) == 0) {
            throw new IllegalArgumentException("SQLITE_COLD requires PAGEABLE");
        }
        if ((mask & PRESSURE_ADAPTIVE) != 0 && (mask & PAGEABLE) == 0) {
            throw new IllegalArgumentException("PRESSURE_ADAPTIVE requires PAGEABLE");
        }
    }

    private static void exactlyOne(int value, String label) {
        if (Integer.bitCount(value) != 1) {
            throw new IllegalArgumentException("exactly one " + label + " aspect is required");
        }
    }

    private static void atMostOne(int value, String label) {
        if (Integer.bitCount(value) > 1) {
            throw new IllegalArgumentException("at most one " + label + " aspect is allowed");
        }
    }
}
