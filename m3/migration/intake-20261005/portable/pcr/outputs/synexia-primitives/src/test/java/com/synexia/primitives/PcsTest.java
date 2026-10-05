// SPDX-License-Identifier: Apache-2.0
package com.synexia.primitives;

import org.junit.jupiter.api.Test;

/** Behavioral admission regressions shared with the dependency-free runtime probe. */
class PcsTest {
    @Test
    void pagesAdmitColdParentsAndPreserveCompletedBranchSemantics() {
        PcsProbe.pagesAdmitColdParentsAndPreserveCompletedBranchSemantics();
    }

    @Test
    void ensureHonorsPageBudgetsAndZeroWork() {
        PcsProbe.ensureHonorsPageBudgetsAndZeroWork();
    }

    @Test
    void metadataAndEmptyWindowsDoNotMaterializeOrReferenceRows() {
        PcsProbe.metadataAndEmptyWindowsDoNotMaterializeOrReferenceRows();
    }

    @Test
    void successfulExplicitDemandsRetainTheirCounters() {
        PcsProbe.successfulExplicitDemandsRetainTheirCounters();
    }

    @Test
    void directRowFailuresDoNotEvictBeforePublication() {
        PcsProbe.directRowFailuresDoNotEvictBeforePublication();
    }

    @Test
    void pageParentLoadFailuresDoNotBecomeFacadeDemands() {
        PcsProbe.pageParentLoadFailuresDoNotBecomeFacadeDemands();
    }

    @Test
    void afterTouchFailuresSettleEveryExplicitDemand() {
        PcsProbe.afterTouchFailuresSettleEveryExplicitDemand();
    }

    @Test
    void afterTouchFailuresKeepPrimaryWhenAdmissionRefuses() {
        PcsProbe.afterTouchFailuresKeepPrimaryWhenAdmissionRefuses();
    }

    @Test
    void normalExplicitAdmissionRefusalIsCountedOnce() {
        PcsProbe.normalExplicitAdmissionRefusalIsCountedOnce();
    }

    @Test
    void lateLaneAndTargetFailuresSettlePublishedRows() {
        PcsProbe.lateLaneAndTargetFailuresSettlePublishedRows();
    }

    @Test
    void lateArgumentFailureKeepsPrimaryWhenAdmissionRefuses() {
        PcsProbe.lateArgumentFailureKeepsPrimaryWhenAdmissionRefuses();
    }

    @Test
    void hintAndPageFailuresSettlePublishedParents() {
        PcsProbe.hintAndPageFailuresSettlePublishedParents();
    }

    @Test
    void pageFailureIdentityAndDistinctSuppressionSurviveSettlement() {
        PcsProbe.pageFailureIdentityAndDistinctSuppressionSurviveSettlement();
    }

    @Test
    void successfulPageRefusalPreservesCommittedTopologyAndFlags() {
        PcsProbe.successfulPageRefusalPreservesCommittedTopologyAndFlags();
    }

    @Test
    void ensureFailurePreservesEarlierPageAndPinnedVersion() {
        PcsProbe.ensureFailurePreservesEarlierPageAndPinnedVersion();
    }

    @Test
    void cachedCallbackReadsDoNotAdmitTheInFlightParent() {
        PcsProbe.cachedCallbackReadsDoNotAdmitTheInFlightParent();
    }

    @Test
    void mixedInvalidArgumentsRetainDelegatedValidationOrder() {
        PcsProbe.mixedInvalidArgumentsRetainDelegatedValidationOrder();
    }

    @Test
    void offsetRootNavigationAndExpansionSurvivePayloadSettlement() {
        PcsProbe.offsetRootNavigationAndExpansionSurvivePayloadSettlement();
    }

    @Test
    void longIdsAndOpaqueDecreasingCursorsSurviveEviction() {
        PcsProbe.longIdsAndOpaqueDecreasingCursorsSurviveEviction();
    }

    @Test
    void staleVersionRequiresExplicitRefresh() {
        PcsProbe.staleVersionRequiresExplicitRefresh();
    }

    @Test
    void refreshAndVersionLookupFailuresSettlePublishedPayload() {
        PcsProbe.refreshAndVersionLookupFailuresSettlePublishedPayload();
    }

    @Test
    void versionedAdapterDoesNotAcquireTouchObserverSemantics() {
        PcsProbe.versionedAdapterDoesNotAcquireTouchObserverSemantics();
    }

    @Test
    void boundedListUsesTheSealedRowFailureSettlement() {
        PcsProbe.boundedListUsesTheSealedRowFailureSettlement();
    }

    @Test
    void listPinnedRollbackAndValidationPreserveContracts() {
        PcsProbe.listPinnedRollbackAndValidationPreserveContracts();
    }
}
