/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * @test
 * @summary Preserve optional resolution failures and hierarchical qualifiers
 * @modules jdk.m3.paradigm/jdk.internal.m3.paradigm.context
 * @compile ContainerResolutionProbe.java
 * @run main m3.tests.context.ContainerResolutionProbe
 */
package m3.tests.context;

import jdk.internal.m3.paradigm.context.AbstractContainer;
import jdk.internal.m3.paradigm.context.IContainer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/** Contract regressions for optional resolution and hierarchical qualifiers. */
public class ContainerResolutionProbe {
    private static class Container extends AbstractContainer {
        Container() {}
        Container(IContainer parent) {
            super(parent);
        }
    }

    void directAndInheritedAbsenceRemainOptional() {
        Container root = new Container();
        IContainer child = root.createChild().createChild();
        assertTrue(root.tryGet(String.class).isEmpty());
        assertTrue(child.tryGet(String.class).isEmpty());
        assertEquals("No binding for type: java.lang.String",
                assertThrows(IllegalArgumentException.class,
                        () -> child.get(String.class)).getMessage());
        assertEquals("No binding for type: java.lang.String with qualifier: absent",
                assertThrows(IllegalArgumentException.class,
                        () -> child.get(String.class, "absent")).getMessage());
    }

    void providerIllegalArgumentExceptionIsNotAbsence() {
        Container container = new Container();
        IllegalArgumentException failure = new IllegalArgumentException("provider failure");
        AtomicInteger calls = new AtomicInteger();
        container.bind(String.class, () -> {
            calls.incrementAndGet();
            throw failure;
        });
        assertSame(failure, assertThrows(IllegalArgumentException.class,
                () -> container.tryGet(String.class)));
        assertEquals(1, calls.get());
    }

    void parentProviderFailurePropagatesThroughChildren() {
        Container parent = new Container();
        IllegalArgumentException failure = new IllegalArgumentException("parent provider");
        parent.bind(String.class, () -> { throw failure; });
        IContainer child = parent.createChild().createChild();
        assertSame(failure, assertThrows(IllegalArgumentException.class,
                () -> child.tryGet(String.class)));
    }

    void nestedMissingDependencyRemainsTheSameFailure() {
        Container missingDependency = new Container();
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> missingDependency.get(Integer.class));
        Container container = new Container();
        container.bind(String.class, () -> { throw failure; });
        assertSame(failure, assertThrows(IllegalArgumentException.class,
                () -> container.tryGet(String.class)));
        assertSame(failure, assertThrows(IllegalArgumentException.class,
                () -> container.tryGet(String.class)));
    }

    void missingSameTypeInAnotherProviderContainerIsStillFailure() {
        Container missingDependency = new Container();
        Container container = new Container();
        container.bind(String.class, () -> missingDependency.get(String.class));
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> container.tryGet(String.class));
        assertEquals("No binding for type: java.lang.String", failure.getMessage());
    }

    void missingQualifiedDependencyInsideProviderIsNotAbsence() {
        Container missingDependency = new Container();
        Container container = new Container();
        container.bind(String.class, () -> missingDependency.get(String.class, "named"));
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> container.tryGet(String.class));
        assertEquals("No binding for type: java.lang.String with qualifier: named",
                failure.getMessage());
    }

    void customResolutionFailureIsNotAbsence() {
        IllegalArgumentException failure = new IllegalArgumentException("custom hook");
        Container container = new Container() {
            @Override
            protected <T> Optional<T> doGet(Class<T> type) {
                throw failure;
            }
        };
        assertSame(failure, assertThrows(IllegalArgumentException.class,
                () -> container.tryGet(String.class)));
    }

    void missingDependencyInsideCustomResolutionIsNotAbsence() {
        Container missingDependency = new Container();
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> missingDependency.get(Integer.class));
        Container container = new Container() {
            @Override
            protected <T> Optional<T> doGet(Class<T> type) {
                throw failure;
            }
        };
        assertSame(failure, assertThrows(IllegalArgumentException.class,
                () -> container.tryGet(String.class)));
    }

    void missingDependencyInsideResolutionNotificationIsNotAbsence() {
        Container missingDependency = new Container();
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> missingDependency.get(Integer.class));
        Container container = new Container() {
            @Override
            protected <T> void onResolved(Class<T> type, T value) {
                throw failure;
            }
        };
        container.bindInstance(String.class, "value");
        assertSame(failure, assertThrows(IllegalArgumentException.class,
                () -> container.tryGet(String.class)));
    }

    void qualifiedResolutionMarksNestedFailureBeforeItEscapes() {
        Container missingDependency = new Container();
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> missingDependency.get(Integer.class));
        Container qualified = new Container();
        qualified.bind(String.class, "named", () -> { throw failure; });
        assertSame(failure, assertThrows(IllegalArgumentException.class,
                () -> qualified.get(String.class, "named")));

        // Reusing the same exception cannot turn it back into an optional absence.
        Container override = new Container() {
            @Override
            public <T> T get(Class<T> type) {
                throw failure;
            }
        };
        assertSame(failure, assertThrows(IllegalArgumentException.class,
                () -> override.tryGet(String.class)));
    }

    void qualifiedHooksMarkNestedFailures() {
        Container missingDependency = new Container();
        IllegalArgumentException customFailure = assertThrows(IllegalArgumentException.class,
                () -> missingDependency.get(Integer.class));
        Container custom = new Container() {
            @Override
            protected <T> Optional<T> doGet(Class<T> type, String qualifier) {
                throw customFailure;
            }
        };
        assertSame(customFailure, assertThrows(IllegalArgumentException.class,
                () -> custom.get(String.class, "q")));

        IllegalArgumentException notificationFailure = assertThrows(
                IllegalArgumentException.class, () -> missingDependency.get(Long.class));
        Container notification = new Container() {
            @Override
            protected <T> void onResolved(Class<T> type, T value) {
                throw notificationFailure;
            }
        };
        notification.bind(String.class, "q", () -> "value");
        assertSame(notificationFailure, assertThrows(IllegalArgumentException.class,
                () -> notification.get(String.class, "q")));
        Container outer = new Container();
        outer.bind(String.class, () -> { throw notificationFailure; });
        assertSame(notificationFailure, assertThrows(IllegalArgumentException.class,
                () -> outer.tryGet(String.class)));
    }

    void optionalLookupRetainsPublicGetOverrideDispatch() {
        AtomicInteger calls = new AtomicInteger();
        Container container = new Container() {
            @Override
            public <T> T get(Class<T> type) {
                calls.incrementAndGet();
                return type.cast("override");
            }
        };
        assertEquals(Optional.of("override"), container.tryGet(String.class));
        assertEquals(1, calls.get());
    }

    void publicGetOverrideFailuresPropagateUnchanged() {
        IllegalArgumentException failure = new IllegalArgumentException("override");
        Container container = new Container() {
            @Override
            public <T> T get(Class<T> type) {
                throw failure;
            }
        };
        assertSame(failure, assertThrows(IllegalArgumentException.class,
                () -> container.tryGet(String.class)));
    }

    void supplierAndHookOrderRemainsStableAndDemandDriven() {
        List<String> events = new ArrayList<>();
        AtomicInteger generations = new AtomicInteger();
        Container container = new Container() {
            @Override
            protected <T> Optional<T> doGet(Class<T> type) {
                events.add("lookup");
                return Optional.empty();
            }

            @Override
            protected <T> void onResolved(Class<T> type, T value) {
                events.add("resolved");
            }

            @Override
            protected <T> void doBind(Class<T> type, Supplier<T> supplier) {
                events.add("binding");
            }

            @Override
            protected <T> void onBound(Class<T> type) {
                events.add("bound");
            }
        };
        container.bind(String.class, () -> {
            events.add("provider");
            return "generation-" + generations.incrementAndGet();
        });
        assertEquals(List.of("binding", "bound"), events);
        events.clear();
        assertEquals(Optional.of("generation-1"), container.tryGet(String.class));
        assertEquals(List.of("lookup", "provider", "resolved"), events);
        events.clear();
        assertEquals("generation-2", container.get(String.class));
        assertEquals(List.of("lookup", "provider", "resolved"), events);
    }

    void customValuePrecedesBindingsAndNotifiesOnce() {
        List<String> events = new ArrayList<>();
        Container container = new Container() {
            @Override
            protected <T> Optional<T> doGet(Class<T> type) {
                events.add("custom");
                return Optional.of(type.cast("custom-value"));
            }

            @Override
            protected <T> void onResolved(Class<T> type, T value) {
                events.add("resolved");
            }
        };
        container.bind(String.class, () -> {
            events.add("provider");
            return "provider-value";
        });
        assertEquals(Optional.of("custom-value"), container.tryGet(String.class));
        assertEquals(List.of("custom", "resolved"), events);
    }

    void qualifiedValuesRemainExactAndSeparateFromUnqualifiedValues() {
        Container parent = new Container();
        parent.bindInstance(String.class, "plain");
        parent.bind(String.class, "", () -> "empty");
        parent.bind(String.class, " spaced ", () -> "spaced");
        parent.bind(String.class, "\u03bb", () -> "unicode");
        IContainer child = parent.createChild();
        child.bindInstance(String.class, "local");
        assertEquals("local", child.get(String.class));
        assertEquals("empty", child.get(String.class, ""));
        assertEquals("spaced", child.get(String.class, " spaced "));
        assertEquals("unicode", child.get(String.class, "\u03bb"));
        assertThrows(IllegalArgumentException.class, () -> child.get(String.class, "spaced"));
        assertThrows(IllegalArgumentException.class, () -> child.get(String.class, "missing"));
    }

    void defaultChildPreservesParentQualifiedDispatchDespiteLocalShadow() {
        IContainer parent = new IContainer() {
            @Override
            public <T> T get(Class<T> type) {
                return type.cast("parent-plain");
            }

            @Override
            public <T> T get(Class<T> type, String qualifier) {
                return type.cast(qualifier == null ? "null-policy" : "qualified:" + qualifier);
            }
        };
        IContainer child = parent.createChild().createChild();
        child.bindInstance(String.class, "child-plain");
        assertEquals("child-plain", child.get(String.class));
        assertEquals("qualified: key ", child.get(String.class, " key "));
        assertEquals("null-policy", child.get(String.class, null));
    }

    void defaultChildUsesParentsOptionalResolutionPolicy() {
        AtomicInteger calls = new AtomicInteger();
        IContainer parent = new IContainer() {
            @Override
            public <T> T get(Class<T> type) {
                throw new AssertionError("optional policy must be delegated");
            }

            @Override
            public <T> Optional<T> tryGet(Class<T> type) {
                calls.incrementAndGet();
                return Optional.empty();
            }
        };
        IContainer child = parent.createChild().createChild();
        assertTrue(child.tryGet(String.class).isEmpty());
        assertEquals(1, calls.get());
        child.bindInstance(String.class, "local");
        assertEquals(Optional.of("local"), child.tryGet(String.class));
        assertEquals(1, calls.get());
    }

    void defaultChildBindingKeepsItsExistingEagerSnapshotContract() {
        IContainer parent = new IContainer() {
            @Override
            public <T> T get(Class<T> type) {
                throw new IllegalArgumentException("missing");
            }
        };
        IContainer child = parent.createChild();
        AtomicInteger calls = new AtomicInteger();
        child.bind(String.class, () -> "value-" + calls.incrementAndGet());
        assertEquals(1, calls.get());
        assertEquals("value-1", child.get(String.class));
        assertEquals(Optional.of("value-1"), child.tryGet(String.class));
        assertEquals(1, calls.get());
    }

    void abstractChildKeepsLazyProvidersAndBoundInstanceIdentity() {
        IContainer child = new Container().createChild();
        AtomicInteger calls = new AtomicInteger();
        child.bind(Object.class, () -> {
            calls.incrementAndGet();
            return new Object();
        });
        assertEquals(0, calls.get());
        Object first = child.get(Object.class);
        assertNotSame(first, child.get(Object.class));
        assertEquals(2, calls.get());
        child.bindInstance(Object.class, first);
        assertSame(first, child.get(Object.class));
        assertSame(first, child.tryGet(Object.class).orElseThrow());
    }

    void nullProviderResultAndNonArgumentFailuresKeepTheirContracts() {
        Container container = new Container();
        container.bind(String.class, () -> null);
        assertNull(container.get(String.class));
        assertThrows(NullPointerException.class, () -> container.tryGet(String.class));
        IllegalStateException state = new IllegalStateException("state");
        container.bind(String.class, () -> { throw state; });
        assertSame(state, assertThrows(IllegalStateException.class,
                () -> container.tryGet(String.class)));
        AssertionError error = new AssertionError("fatal");
        container.bind(String.class, () -> { throw error; });
        assertSame(error, assertThrows(AssertionError.class,
                () -> container.tryGet(String.class)));
    }

    void existingNullKeyAndBindingValidationRemain() {
        Container container = new Container();
        AtomicInteger calls = new AtomicInteger();
        Supplier<String> supplier = () -> {
            calls.incrementAndGet();
            return "value";
        };
        assertThrows(NullPointerException.class, () -> container.get(null));
        assertThrows(NullPointerException.class, () -> container.tryGet(null));
        assertThrows(NullPointerException.class, () -> container.get(String.class, null));
        assertThrows(NullPointerException.class, () -> container.bind(null, supplier));
        assertThrows(NullPointerException.class, () -> container.bind(String.class, null));
        assertThrows(NullPointerException.class,
                () -> container.bind(String.class, null, supplier));
        assertEquals(0, calls.get());
    }

    void arbitraryInterfaceDefaultsKeepLegacyBehavior() {
        IllegalArgumentException failure = new IllegalArgumentException("legacy absence");
        IContainer legacy = new IContainer() {
            @Override
            public <T> T get(Class<T> type) {
                throw failure;
            }
        };
        assertFalse(legacy.tryGet(String.class).isPresent());
        assertFalse(legacy.createChild().tryGet(String.class).isPresent());
        IContainer unqualified = new IContainer() {
            @Override
            public <T> T get(Class<T> type) {
                return type.cast("legacy");
            }
        };
        assertEquals("legacy", unqualified.get(String.class, "ignored"));
        assertEquals("legacy", unqualified.get(String.class, null));
    }

    void unknownParentArgumentFailureIsNotGuessedToBeAbsence() {
        IllegalArgumentException failure = new IllegalArgumentException("ambiguous parent");
        IContainer legacy = new IContainer() {
            @Override
            public <T> T get(Class<T> type) {
                throw failure;
            }
        };
        Container child = new Container(legacy);
        assertSame(failure, assertThrows(IllegalArgumentException.class,
                () -> child.tryGet(String.class)));
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Throwable;
    }

    private static <T extends Throwable> T assertThrows(Class<T> expected, CheckedAction action) {
        try {
            action.run();
        } catch (Throwable failure) {
            if (expected.isInstance(failure)) {
                return expected.cast(failure);
            }
            throw new AssertionError("Expected " + expected.getName() + ", caught " + failure, failure);
        }
        throw new AssertionError("Expected " + expected.getName() + ", but no exception was thrown");
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
    }

    private static void assertSame(Object expected, Object actual) {
        if (expected != actual) {
            throw new AssertionError("Expected the identical object");
        }
    }

    private static void assertNotSame(Object first, Object second) {
        if (first == second) {
            throw new AssertionError("Expected distinct objects");
        }
    }

    private static void assertTrue(boolean condition) {
        if (!condition) {
            throw new AssertionError("Expected true");
        }
    }

    private static void assertFalse(boolean condition) {
        assertTrue(!condition);
    }

    private static void assertNull(Object value) {
        assertSame(null, value);
    }

    /** Runs the same product contracts without requiring a JUnit launcher. */
    public static void main(String[] args) {
        ContainerResolutionProbe probe = new ContainerResolutionProbe();
        String[] names = {
            "directAndInheritedAbsenceRemainOptional",
            "providerIllegalArgumentExceptionIsNotAbsence",
            "parentProviderFailurePropagatesThroughChildren",
            "nestedMissingDependencyRemainsTheSameFailure",
            "missingSameTypeInAnotherProviderContainerIsStillFailure",
            "missingQualifiedDependencyInsideProviderIsNotAbsence",
            "customResolutionFailureIsNotAbsence",
            "missingDependencyInsideCustomResolutionIsNotAbsence",
            "missingDependencyInsideResolutionNotificationIsNotAbsence",
            "qualifiedResolutionMarksNestedFailureBeforeItEscapes",
            "qualifiedHooksMarkNestedFailures",
            "optionalLookupRetainsPublicGetOverrideDispatch",
            "publicGetOverrideFailuresPropagateUnchanged",
            "supplierAndHookOrderRemainsStableAndDemandDriven",
            "customValuePrecedesBindingsAndNotifiesOnce",
            "qualifiedValuesRemainExactAndSeparateFromUnqualifiedValues",
            "defaultChildPreservesParentQualifiedDispatchDespiteLocalShadow",
            "defaultChildUsesParentsOptionalResolutionPolicy",
            "defaultChildBindingKeepsItsExistingEagerSnapshotContract",
            "abstractChildKeepsLazyProvidersAndBoundInstanceIdentity",
            "nullProviderResultAndNonArgumentFailuresKeepTheirContracts",
            "existingNullKeyAndBindingValidationRemain",
            "arbitraryInterfaceDefaultsKeepLegacyBehavior",
            "unknownParentArgumentFailureIsNotGuessedToBeAbsence"
        };
        Runnable[] cases = {
            probe::directAndInheritedAbsenceRemainOptional,
            probe::providerIllegalArgumentExceptionIsNotAbsence,
            probe::parentProviderFailurePropagatesThroughChildren,
            probe::nestedMissingDependencyRemainsTheSameFailure,
            probe::missingSameTypeInAnotherProviderContainerIsStillFailure,
            probe::missingQualifiedDependencyInsideProviderIsNotAbsence,
            probe::customResolutionFailureIsNotAbsence,
            probe::missingDependencyInsideCustomResolutionIsNotAbsence,
            probe::missingDependencyInsideResolutionNotificationIsNotAbsence,
            probe::qualifiedResolutionMarksNestedFailureBeforeItEscapes,
            probe::qualifiedHooksMarkNestedFailures,
            probe::optionalLookupRetainsPublicGetOverrideDispatch,
            probe::publicGetOverrideFailuresPropagateUnchanged,
            probe::supplierAndHookOrderRemainsStableAndDemandDriven,
            probe::customValuePrecedesBindingsAndNotifiesOnce,
            probe::qualifiedValuesRemainExactAndSeparateFromUnqualifiedValues,
            probe::defaultChildPreservesParentQualifiedDispatchDespiteLocalShadow,
            probe::defaultChildUsesParentsOptionalResolutionPolicy,
            probe::defaultChildBindingKeepsItsExistingEagerSnapshotContract,
            probe::abstractChildKeepsLazyProvidersAndBoundInstanceIdentity,
            probe::nullProviderResultAndNonArgumentFailuresKeepTheirContracts,
            probe::existingNullKeyAndBindingValidationRemain,
            probe::arbitraryInterfaceDefaultsKeepLegacyBehavior,
            probe::unknownParentArgumentFailureIsNotGuessedToBeAbsence
        };
        AssertionError aggregate = new AssertionError("Container resolution contracts failed");
        int passed = 0;
        for (int index = 0; index < cases.length; index++) {
            try {
                cases[index].run();
                passed++;
                System.out.println("PASS " + names[index]);
            } catch (Throwable failure) {
                System.out.println("FAIL " + names[index] + ": " + failure);
                aggregate.addSuppressed(new AssertionError(names[index], failure));
            }
        }
        System.out.println("Container resolution contracts: " + passed + "/" + cases.length);
        if (passed != cases.length) {
            throw aggregate;
        }
    }
}
