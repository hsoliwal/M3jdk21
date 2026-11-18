/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package m3.tests.context;

import org.junit.jupiter.api.Test;

/** JUnit entry points for the standalone product contract cases. */
class ContainerResolutionContractTest {
    private final ContainerResolutionProbe probe = new ContainerResolutionProbe();

    @Test
    void directAndInheritedAbsenceRemainOptional() {
        probe.directAndInheritedAbsenceRemainOptional();
    }

    @Test
    void providerIllegalArgumentExceptionIsNotAbsence() {
        probe.providerIllegalArgumentExceptionIsNotAbsence();
    }

    @Test
    void parentProviderFailurePropagatesThroughChildren() {
        probe.parentProviderFailurePropagatesThroughChildren();
    }

    @Test
    void nestedMissingDependencyRemainsTheSameFailure() {
        probe.nestedMissingDependencyRemainsTheSameFailure();
    }

    @Test
    void missingSameTypeInAnotherProviderContainerIsStillFailure() {
        probe.missingSameTypeInAnotherProviderContainerIsStillFailure();
    }

    @Test
    void missingQualifiedDependencyInsideProviderIsNotAbsence() {
        probe.missingQualifiedDependencyInsideProviderIsNotAbsence();
    }

    @Test
    void customResolutionFailureIsNotAbsence() {
        probe.customResolutionFailureIsNotAbsence();
    }

    @Test
    void missingDependencyInsideCustomResolutionIsNotAbsence() {
        probe.missingDependencyInsideCustomResolutionIsNotAbsence();
    }

    @Test
    void missingDependencyInsideResolutionNotificationIsNotAbsence() {
        probe.missingDependencyInsideResolutionNotificationIsNotAbsence();
    }

    @Test
    void qualifiedResolutionMarksNestedFailureBeforeItEscapes() {
        probe.qualifiedResolutionMarksNestedFailureBeforeItEscapes();
    }

    @Test
    void qualifiedHooksMarkNestedFailures() {
        probe.qualifiedHooksMarkNestedFailures();
    }

    @Test
    void optionalLookupRetainsPublicGetOverrideDispatch() {
        probe.optionalLookupRetainsPublicGetOverrideDispatch();
    }

    @Test
    void publicGetOverrideFailuresPropagateUnchanged() {
        probe.publicGetOverrideFailuresPropagateUnchanged();
    }

    @Test
    void supplierAndHookOrderRemainsStableAndDemandDriven() {
        probe.supplierAndHookOrderRemainsStableAndDemandDriven();
    }

    @Test
    void customValuePrecedesBindingsAndNotifiesOnce() {
        probe.customValuePrecedesBindingsAndNotifiesOnce();
    }

    @Test
    void qualifiedValuesRemainExactAndSeparateFromUnqualifiedValues() {
        probe.qualifiedValuesRemainExactAndSeparateFromUnqualifiedValues();
    }

    @Test
    void defaultChildPreservesParentQualifiedDispatchDespiteLocalShadow() {
        probe.defaultChildPreservesParentQualifiedDispatchDespiteLocalShadow();
    }

    @Test
    void defaultChildUsesParentsOptionalResolutionPolicy() {
        probe.defaultChildUsesParentsOptionalResolutionPolicy();
    }

    @Test
    void defaultChildBindingKeepsItsExistingEagerSnapshotContract() {
        probe.defaultChildBindingKeepsItsExistingEagerSnapshotContract();
    }

    @Test
    void abstractChildKeepsLazyProvidersAndBoundInstanceIdentity() {
        probe.abstractChildKeepsLazyProvidersAndBoundInstanceIdentity();
    }

    @Test
    void nullProviderResultAndNonArgumentFailuresKeepTheirContracts() {
        probe.nullProviderResultAndNonArgumentFailuresKeepTheirContracts();
    }

    @Test
    void existingNullKeyAndBindingValidationRemain() {
        probe.existingNullKeyAndBindingValidationRemain();
    }

    @Test
    void arbitraryInterfaceDefaultsKeepLegacyBehavior() {
        probe.arbitraryInterfaceDefaultsKeepLegacyBehavior();
    }

    @Test
    void unknownParentArgumentFailureIsNotGuessedToBeAbsence() {
        probe.unknownParentArgumentFailureIsNotGuessedToBeAbsence();
    }
}
