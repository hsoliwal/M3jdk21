/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Owning-module execution of the independent nested-callback contract. */
final class M3CollectionViewsReentrantTest {
    @Test void consumerExhaustionKeepsOuterDelivery() {
        M3CollectionViewsReentrantContract.consumerExhaustion();
    }
    @Test void nestedConsumerInvocationsKeepTheirOwnResults() {
        M3CollectionViewsReentrantContract.nestedConsumers();
    }
    @Test void nestedPredicateInvocationsKeepTheirOwnActions() {
        M3CollectionViewsReentrantContract.nestedPredicates();
    }
    @Test void callbackFailuresRestoreStateAndPreserveIdentity() {
        M3CollectionViewsReentrantContract.callbackFailures();
    }
    @Test void nullAdmissionDoesNotDisturbActiveCallbacks() {
        M3CollectionViewsReentrantContract.nullAdmission();
    }
    @Test void splitChildrenKeepOrderAndIndependentCallbackState() {
        M3CollectionViewsReentrantContract.splitConformance();
    }
    @Test void bulkAndAdvanceFollowTheActualOwner() {
        M3CollectionViewsReentrantContract.bulkConformance();
    }
    @Test void callbacksAreRetainedOnlyDuringTheirInvocation() {
        M3CollectionViewsReentrantContract.callbackLifetime();
    }
}
