// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections.shape;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jdk.internal.m3.collections.CollectionAspectMask;
import jdk.internal.m3.collections.select.CollectionCapabilities;
import jdk.internal.m3.collections.select.CollectionRequest;
import org.junit.jupiter.api.Test;

final class CollectionAspectMaskTest {
    private static final CandidateProfile PROFILE =
            new CandidateProfile(CandidateProfile.SizeBand.GENERAL, 500, 500, 500, 500, 500);

    @Test
    void shapeRequirementsRoundTripAllCurrentSemanticCombinations() {
        long cases = 0;
        for (CollectionRequirements.Kind kind : CollectionRequirements.Kind.values()) {
            for (CollectionRequirements.Order order : CollectionRequirements.Order.values()) {
                for (CollectionRequirements.Equality equality : CollectionRequirements.Equality.values()) {
                    for (CollectionRequirements.Concurrency concurrency
                            : CollectionRequirements.Concurrency.values()) {
                        for (boolean nullable : new boolean[] {false, true}) {
                            var required = new CollectionRequirements(
                                    kind, order, equality, concurrency, nullable, 4096);
                            int mask = required.aspects();
                            CollectionAspectMask.requireShapeRequirementWord(mask);
                            assertEquals(
                                    required,
                                    new CollectionRequirements(mask, 4096));
                            cases++;
                        }
                    }
                }
            }
        }
        assertEquals(352L, cases);
    }

    @Test
    void shapeMaskAdmissionMatchesLegacyFieldRulesExhaustively() {
        long cases = 0;
        for (CollectionRequirements.Kind requiredKind : CollectionRequirements.Kind.values()) {
            for (CollectionRequirements.Order requiredOrder : CollectionRequirements.Order.values()) {
                for (CollectionRequirements.Equality requiredEquality
                        : CollectionRequirements.Equality.values()) {
                    for (CollectionRequirements.Concurrency requiredConcurrency
                            : CollectionRequirements.Concurrency.values()) {
                        for (boolean requireNull : new boolean[] {false, true}) {
                            var required = new CollectionRequirements(
                                    requiredKind,
                                    requiredOrder,
                                    requiredEquality,
                                    requiredConcurrency,
                                    requireNull,
                                    4096);
                            for (CollectionRequirements.Kind candidateKind
                                    : CollectionRequirements.Kind.values()) {
                                for (CollectionRequirements.Order candidateOrder
                                        : CollectionRequirements.Order.values()) {
                                    for (CollectionRequirements.Concurrency candidateConcurrency
                                            : CollectionRequirements.Concurrency.values()) {
                                        for (boolean valueEquality : new boolean[] {false, true}) {
                                            for (boolean identityEquality : new boolean[] {false, true}) {
                                                for (boolean nullable : new boolean[] {false, true}) {
                                                    var candidate = new CollectionCandidate(
                                                            "mask-test",
                                                            candidateKind,
                                                            candidateOrder,
                                                            candidateConcurrency,
                                                            valueEquality,
                                                            identityEquality,
                                                            nullable,
                                                            4096,
                                                            PROFILE);
                                                    boolean legacy =
                                                            candidateKind == requiredKind
                                                            && (requiredOrder
                                                                            == CollectionRequirements.Order.UNSPECIFIED
                                                                    || candidateOrder == requiredOrder)
                                                            && candidateConcurrency == requiredConcurrency
                                                            && (requiredEquality
                                                                            != CollectionRequirements.Equality.VALUE
                                                                    || valueEquality)
                                                            && (requiredEquality
                                                                            != CollectionRequirements.Equality.IDENTITY
                                                                    || identityEquality)
                                                            && (!requireNull || nullable);
                                                    boolean masked =
                                                            CollectionAspectMask.supports(
                                                                    candidate.aspects(),
                                                                    required.aspects());
                                                    assertEquals(legacy, masked);
                                                    cases++;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        assertEquals(247_808L, cases);
    }

    @Test
    void selectionMaskAdmissionMatchesLegacyCapabilityRulesExhaustively() {
        long cases = 0;
        for (CollectionRequest.Order requiredOrder : CollectionRequest.Order.values()) {
            for (boolean requireConcurrent : new boolean[] {false, true}) {
                for (boolean requireStable : new boolean[] {false, true}) {
                    for (boolean requireLazy : new boolean[] {false, true}) {
                        for (boolean requireNoCopy : new boolean[] {false, true}) {
                            var required = new CollectionRequest.Requirements(
                                    requiredOrder,
                                    requireConcurrent,
                                    requireStable,
                                    requireLazy,
                                    requireNoCopy,
                                    0);
                            for (CollectionRequest.Order providedOrder : CollectionRequest.Order.values()) {
                                for (boolean concurrent : new boolean[] {false, true}) {
                                    for (boolean stable : new boolean[] {false, true}) {
                                        for (boolean lazy : new boolean[] {false, true}) {
                                            for (boolean noCopy : new boolean[] {false, true}) {
                                                var provided = new CollectionCapabilities(
                                                        providedOrder,
                                                        concurrent,
                                                        stable,
                                                        lazy,
                                                        noCopy,
                                                        1 << 28,
                                                        0,
                                                        0,
                                                        0);
                                                boolean legacy =
                                                        (requiredOrder == CollectionRequest.Order.ANY
                                                                        || requiredOrder == providedOrder)
                                                                && (!requireConcurrent || concurrent)
                                                                && (!requireStable || stable)
                                                                && (!requireLazy || lazy)
                                                                && (!requireNoCopy || noCopy);
                                                boolean masked =
                                                        CollectionAspectMask.supports(
                                                                provided.aspects(),
                                                                required.aspects());
                                                assertEquals(legacy, masked);
                                                cases++;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        assertEquals(9_216L, cases);
    }

    @Test
    void residencyDependenciesAndCompactRequestFailClosedOrRoundTrip() {
        assertThrows(
                IllegalArgumentException.class,
                () -> CollectionAspectMask.requireKnown(CollectionAspectMask.SQLITE_COLD));
        assertThrows(
                IllegalArgumentException.class,
                () -> CollectionAspectMask.requireKnown(CollectionAspectMask.PRESSURE_ADAPTIVE));
        assertThrows(
                IllegalArgumentException.class,
                () -> CollectionAspectMask.requireKnown(1 << 31));

        int semantic = CollectionAspectMask.KIND_MAP
                | CollectionAspectMask.ORDER_INSERTION
                | CollectionAspectMask.EQUALITY_VALUE
                | CollectionAspectMask.CONCURRENCY_SINGLE_THREAD
                | CollectionAspectMask.NULLABLE;
        int valid = semantic
                | CollectionAspectMask.PAGEABLE
                | CollectionAspectMask.PRESSURE_ADAPTIVE
                | CollectionAspectMask.SQLITE_COLD;
        CollectionAspectMask.requireShapeRequirementWord(valid);
        assertTrue(CollectionAspectMask.has(valid, CollectionAspectMask.SQLITE_COLD));

        var request = new CollectionAspectRequest(valid, 32, 4096);
        assertTrue(request.pageable());
        assertTrue(request.sqliteCold());
        assertTrue(request.pressureAdaptive());
        assertEquals(
                CollectionAspectMask.PAGEABLE
                        | CollectionAspectMask.PRESSURE_ADAPTIVE
                        | CollectionAspectMask.SQLITE_COLD,
                request.policyAspects());
        assertEquals(request.semanticAspects(), request.semanticRequest().required().aspects());
    }


    @Test
    void aspectConstructorSelectsStableSegmentedOwnerAndRefusesUnimplementedSwapResidency() {
        int linked = CollectionAspectMask.KIND_LINKED_SEQUENCE
                | CollectionAspectMask.ORDER_POSITION
                | CollectionAspectMask.EQUALITY_VALUE
                | CollectionAspectMask.CONCURRENCY_SINGLE_THREAD
                | CollectionAspectMask.STABLE_HANDLES
                | CollectionAspectMask.LAZY_STORAGE
                | CollectionAspectMask.NO_PAYLOAD_COPY_ON_GROWTH;
        var linkedRequest = new CollectionAspectRequest(linked, 64, 4096);
        var selected = CollectionFactory.standard().<String>linkedSequence(linkedRequest);
        assertEquals("m3.linked.segmented.v1", selected.selection().selectedId());
        assertEquals(CollectionFactory.ASPECT_POLICY, selected.selection().policy());
        assertTrue(selected.selection().canonicalText().contains("ASPECTS:0x"));

        int pageableMap = CollectionAspectMask.KIND_MAP
                | CollectionAspectMask.EQUALITY_VALUE
                | CollectionAspectMask.CONCURRENCY_SINGLE_THREAD
                | CollectionAspectMask.PAGEABLE
                | CollectionAspectMask.SQLITE_COLD
                | CollectionAspectMask.PRESSURE_ADAPTIVE;
        var coldRequest = new CollectionAspectRequest(pageableMap, 64, 4096);
        assertThrows(
                IllegalArgumentException.class,
                () -> CollectionFactory.standard().map(coldRequest));
    }

    @Test
    void legacyRequirementsRejectPolicyBitsInsteadOfDroppingThem() {
        int full = CollectionAspectMask.KIND_MAP
                | CollectionAspectMask.EQUALITY_VALUE
                | CollectionAspectMask.CONCURRENCY_SINGLE_THREAD
                | CollectionAspectMask.PAGEABLE;
        assertThrows(
                IllegalArgumentException.class,
                () -> new CollectionRequirements(full, 4096));
    }

    @Test
    void physicalLookupStrategyIsNotForcedByTheConstructorMask() {
        int request = CollectionAspectMask.KIND_MAP
                | CollectionAspectMask.EQUALITY_VALUE
                | CollectionAspectMask.CONCURRENCY_SINGLE_THREAD;
        CollectionAspectMask.requireShapeRequirementWord(request);
        assertFalse(CollectionAspectMask.has(request, CollectionAspectMask.PAGEABLE));
        assertEquals(0, request & CollectionAspectMask.RESIDENCY_MASK);
    }

}
