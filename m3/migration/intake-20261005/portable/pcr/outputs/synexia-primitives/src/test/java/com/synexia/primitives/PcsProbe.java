// SPDX-License-Identifier: Apache-2.0
package com.synexia.primitives;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.function.LongConsumer;

/**
 * Standalone behavioral probe for bounded provider admission.
 *
 * <p>Every scenario uses the real collection owners and provider interfaces. The in-memory
 * providers below supply deterministic payloads, pages and deliberate failures; no production
 * dependency is replaced. The same scenarios are individual JUnit tests in {@link PcsTest}.
 * Assertions are explicit and do not depend on the VM's assertion-enable option.
 */
public final class PcsProbe {
    private static final long PARENT = 10L;
    private static final long CHILD = 100L;
    private static final long HIGH_PARENT = 6_000_000_000L;
    private static final long NEXT = -17L;
    private static final long LATER = -41L;
    private static final long END = CursorPagedPrimitiveTreeProvider.END_CURSOR;
    private static final short APP =
            (short) (LazyElementFlags.APPLICATION_0 | LazyElementFlags.APPLICATION_3);
    private static final String ALL_PINNED = "PROVIDER_RESIDENCY_ALL_PINNED";

    private PcsProbe() {}

    public static void main(String[] args) {
        Runnable[] scenarios = {
            PcsProbe::pagesAdmitColdParentsAndPreserveCompletedBranchSemantics,
            PcsProbe::ensureHonorsPageBudgetsAndZeroWork,
            PcsProbe::metadataAndEmptyWindowsDoNotMaterializeOrReferenceRows,
            PcsProbe::successfulExplicitDemandsRetainTheirCounters,
            PcsProbe::directRowFailuresDoNotEvictBeforePublication,
            PcsProbe::pageParentLoadFailuresDoNotBecomeFacadeDemands,
            PcsProbe::afterTouchFailuresSettleEveryExplicitDemand,
            PcsProbe::afterTouchFailuresKeepPrimaryWhenAdmissionRefuses,
            PcsProbe::normalExplicitAdmissionRefusalIsCountedOnce,
            PcsProbe::lateLaneAndTargetFailuresSettlePublishedRows,
            PcsProbe::lateArgumentFailureKeepsPrimaryWhenAdmissionRefuses,
            PcsProbe::hintAndPageFailuresSettlePublishedParents,
            PcsProbe::pageFailureIdentityAndDistinctSuppressionSurviveSettlement,
            PcsProbe::successfulPageRefusalPreservesCommittedTopologyAndFlags,
            PcsProbe::ensureFailurePreservesEarlierPageAndPinnedVersion,
            PcsProbe::cachedCallbackReadsDoNotAdmitTheInFlightParent,
            PcsProbe::mixedInvalidArgumentsRetainDelegatedValidationOrder,
            PcsProbe::offsetRootNavigationAndExpansionSurvivePayloadSettlement,
            PcsProbe::longIdsAndOpaqueDecreasingCursorsSurviveEviction,
            PcsProbe::staleVersionRequiresExplicitRefresh,
            PcsProbe::refreshAndVersionLookupFailuresSettlePublishedPayload,
            PcsProbe::versionedAdapterDoesNotAcquireTouchObserverSemantics,
            PcsProbe::boundedListUsesTheSealedRowFailureSettlement,
            PcsProbe::listPinnedRollbackAndValidationPreserveContracts
        };
        for (Runnable scenario : scenarios) scenario.run();
        System.out.println("PCS_OK scenarios=" + scenarios.length);
    }

    static void pagesAdmitColdParentsAndPreserveCompletedBranchSemantics() {
        for (Kind kind : Kind.values()) {
            Tree t = tree(kind, 1);
            t.model.hint = PagedPrimitiveTreeProvider.ChildHint.NONE;
            ProviderCollectionStats initial = t.stats();
            eq(0L, t.page(PARENT), kind + " empty first page");
            resident(t, PARENT);
            eq(1L, t.model.rowLoads, "one parent load");
            eq(1L, t.model.hints, "one child hint");
            eq(0L, t.model.pageLoads, "NONE never fetches children");
            delta(initial, t.stats(), 0, 0, 0, 0, 0, 0);
            observerCounts(t, 1L, 1L);

            Events complete = t.model.events();
            ProviderCollectionStats before = t.stats();
            eq(0L, t.page(PARENT), "resident completed page");
            sameEvents(complete, t.model.events(), "completed resident must be callback-free");
            eq(before, t.stats(), "completed resident counters");

            eq(0L, t.page(20L), "another cold NONE parent");
            resident(t, 20L);
            cold(t, PARENT);
            check(t.complete(PARENT), "eviction retains completed child state");
            eq(1L, t.stats().policyEvictions(), "first page-induced eviction");

            before = t.stats();
            Events evicted = t.model.events();
            eq(0L, t.ensure(PARENT, 99L, 4), "completed cold ensure has no work");
            sameEvents(evicted, t.model.events(), "ensure does not reload completed cold parent");
            eq(before, t.stats(), "completed cold ensure counters");
            cold(t, PARENT);

            eq(0L, t.page(PARENT), "direct page reloads completed cold parent");
            resident(t, PARENT);
            cold(t, 20L);
            eq(evicted.rowLoads + 1L, t.model.rowLoads, "one completed-parent reload");
            eq(evicted.hints, t.model.hints, "cached hint is retained");
            eq(evicted.pageLoads, t.model.pageLoads, "no repeated child fetch");
            delta(before, t.stats(), 0, 0, 0, 0, 1, 0);
        }
    }

    static void ensureHonorsPageBudgetsAndZeroWork() {
        for (Kind kind : Kind.values()) {
            Tree t = tree(kind, 1);
            Events initial = t.model.events();
            eq(0L, t.ensure(PARENT, 0L, 5), "zero minimum");
            eq(0L, t.ensure(PARENT, 3L, 0), "zero page budget");
            sameEvents(initial, t.model.events(), "zero work does not invoke providers");
            ledger(t, 0);
            ProviderCollectionStats before = t.stats();
            eq(1L, t.ensure(PARENT, 3L, 1), "first budget-limited page");
            resident(t, PARENT);
            eq(t.firstCursor(), t.cursor(PARENT), "first cursor");
            eq(2L, t.ensure(PARENT, 3L, 1), "second budget-limited page");
            eq(3L, t.ensure(PARENT, 3L, 5), "completion");
            check(t.complete(PARENT), "complete branch");
            eq(1L, t.model.rowLoads, "one load across three page calls");
            eq(3L, t.model.pageLoads, "three actual child callbacks");
            observerCounts(t, 3L, 3L);
            delta(before, t.stats(), 0, 0, 0, 0, 0, 0);
            check(t.invalidate(PARENT), "explicit payload invalidation");
            before = t.stats();
            Events complete = t.model.events();
            eq(3L, t.ensure(PARENT, 4L, 5), "complete branch cannot grow through ensure");
            sameEvents(complete, t.model.events(), "completed ensure is callback-free");
            eq(before, t.stats(), "zero-work ensure does not count admission");
            ledger(t, 0);
            if (kind == Kind.VERSIONED) {
                eq(1L, t.versioned().versionStats().versionLookups(), "version pinned once");
                eq(3L, t.versioned().versionStats().childPageLoads(), "version page count");
            }
        }
    }

    static void metadataAndEmptyWindowsDoNotMaterializeOrReferenceRows() {
        for (Kind kind : Kind.values()) {
            Tree t = tree(kind, 2);
            t.model.hint = PagedPrimitiveTreeProvider.ChildHint.NONE;
            t.page(10L);
            t.page(20L);
            t.page(30L);
            check(!referenceBit(t, 20L), "CLOCK has consumed the old resident's second chance");
            long bytes = t.bytes();
            Events events = t.model.events();
            ProviderCollectionStats before = t.stats();
            eq(0L, t.page(20L), "completed resident page");
            eq(0L, t.ensure(20L, 0L, 0), "zero-work ensure");
            eq(0L, t.prefetch(20L, 0L, 0), "empty child window");
            eq(0L, t.flags(900L), "cold flag read");
            check(!t.materialized(900L), "cold presence read");
            check(!t.resident(900L), "cold slot read");
            eq(-1L, t.first(900L), "cold link read");
            eq(0L, t.count(900L), "cold count read");
            eq(bytes, t.bytes(), "metadata reads allocate no payload backing");
            check(!referenceBit(t, 20L), "no-op paging must not grant a CLOCK second chance");
            sameEvents(events, t.model.events(), "metadata and empty windows");
            eq(before, t.stats(), "no-op counters");
            ledger(t, 2);
        }
    }

    static void successfulExplicitDemandsRetainTheirCounters() {
        for (Kind kind : Kind.values()) {
            for (Demand demand : Demand.values()) {
                Tree t = tree(kind, 1);
                invoke(demand, t, 1L);
                resident(t, 1L);
                eq(1L, t.stats().touches(), "cold explicit touch");
                eq(1L, t.stats().loads(), "cold explicit load");
                ProviderCollectionStats before = t.stats();
                invoke(demand, t, 1L);
                delta(before, t.stats(), 1, 1, 0, 0, 0, 0);
                before = t.stats();
                invoke(demand, t, 2L);
                delta(before, t.stats(), 1, 0, 1, 0, 1, 0);
                resident(t, 2L);
                cold(t, 1L);
                eq(2L, t.model.rowLoads, "warm demand must not reload");
                observerCounts(t, 3L, 3L);
            }
        }
    }

    static void directRowFailuresDoNotEvictBeforePublication() {
        for (Kind kind : Kind.values()) {
            for (Demand demand : Demand.values()) {
                for (boolean error : new boolean[] {false, true}) {
                    Tree t = tree(kind, 1);
                    t.touch(1L);
                    Throwable original = problem(error, "row provider");
                    // afterLoad also exercises a transient publication that the row owner rolls back.
                    t.model.fail(error ? Phase.AFTER_LOAD : Phase.LOAD, 2L, original);
                    ProviderCollectionStats before = t.stats();
                    Throwable actual = fails(() -> invoke(demand, t, 2L));
                    providerFailure(original, actual, false);
                    eq(0L, actual.getSuppressed().length, "no admission attempted");
                    resident(t, 1L);
                    cold(t, 2L);
                    check(has(t.flags(2L), LazyElementFlags.FAILED), "row owner records failure");
                    check(!has(t.flags(2L), LazyElementFlags.LOADING), "loading cleared");
                    delta(before, t.stats(), 1, 0, 0, error ? 0 : 1, 0, 0);
                    eq(2L, t.model.rowLoads, "one attempted load per demand");
                    t.model.clearFailure();
                    t.invalidate(2L);
                    t.touch(2L);
                    resident(t, 2L);
                    eq(3L, t.model.rowLoads, "explicit recovery loads once");
                }
            }
        }
    }

    static void pageParentLoadFailuresDoNotBecomeFacadeDemands() {
        for (Kind kind : Kind.values()) {
            for (boolean ensure : new boolean[] {false, true}) {
                Tree t = tree(kind, 1);
                t.touch(1L);
                Throwable original = problem(ensure, "parent load");
                t.model.fail(Phase.LOAD, PARENT, original);
                ProviderCollectionStats before = t.stats();
                Throwable actual = fails(() -> pageDemand(t, ensure));
                providerFailure(original, actual, false);
                eq(before, t.stats(), "page load failure must not invent facade demand counters");
                resident(t, 1L);
                cold(t, PARENT);
                eq(0L, t.model.hints, "load failed before hint");
                eq(0L, t.model.pageLoads, "load failed before page");
                eq(2L, t.model.rowLoads, "one setup and one attempted parent load");
            }
        }
    }

    static void afterTouchFailuresSettleEveryExplicitDemand() {
        for (Kind kind : observedKinds()) {
            for (Demand demand : Demand.values()) {
                for (boolean error : new boolean[] {false, true}) {
                    Tree t = tree(kind, 1);
                    t.touch(1L);
                    t.application(2L, APP);
                    Throwable original = problem(error, "after touch");
                    t.model.fail(Phase.AFTER_TOUCH, 2L, original);
                    ProviderCollectionStats before = t.stats();
                    Throwable actual = fails(() -> invoke(demand, t, 2L));
                    providerFailure(original, actual, false);
                    eq(0L, actual.getSuppressed().length, "successful settlement");
                    resident(t, 2L);
                    cold(t, 1L);
                    eq(APP, application(t.flags(2L)), "application flags");
                    check(!has(t.flags(2L), LazyElementFlags.FAILED), "published row remains usable");
                    delta(before, t.stats(), 1, 0, 0, error ? 0 : 1, 1, 0);
                    eq(2L, t.model.rowLoads, "one delegated load");
                    t.model.clearFailure();
                    before = t.stats();
                    eq(20L, t.get(0, 2L), "warm recovery");
                    eq(2L, t.model.rowLoads, "reconciled payload must not reload");
                    delta(before, t.stats(), 1, 1, 0, 0, 0, 0);
                }
            }
        }
    }

    static void afterTouchFailuresKeepPrimaryWhenAdmissionRefuses() {
        for (Kind kind : observedKinds()) {
            for (Demand demand : Demand.values()) {
                for (boolean error : new boolean[] {false, true}) {
                    Tree t = pinned(kind);
                    Throwable original = problem(error, "notification before admission");
                    t.model.fail(Phase.AFTER_TOUCH, PARENT, original);
                    ProviderCollectionStats before = t.stats();
                    Throwable actual = fails(() -> invoke(demand, t, PARENT));
                    providerFailure(original, actual, false);
                    suppressedAdmission(actual, 0);
                    rolledBackPinnedParent(t);
                    delta(before, t.stats(), 1, 0, 0, error ? 0 : 1, 0, 1);
                    t.model.clearFailure();
                    t.pin(1L, false);
                    check(t.touch(PARENT), "retry after admission rollback reloads");
                    resident(t, PARENT);
                    check(!t.evict(PARENT), "preexisting pin survives recovery");
                }
            }
        }
    }

    static void normalExplicitAdmissionRefusalIsCountedOnce() {
        for (Kind kind : Kind.values()) {
            for (Demand demand : Demand.values()) {
                Tree t = pinned(kind);
                ProviderCollectionStats before = t.stats();
                Throwable actual = fails(() -> invoke(demand, t, PARENT));
                admission(actual);
                eq(0L, actual.getSuppressed().length, "normal refusal is not failed-demand cleanup");
                rolledBackPinnedParent(t);
                delta(before, t.stats(), 1, 0, 1, 0, 0, 1);
                eq(2L, t.model.rowLoads, "normal delegate runs once");
            }
        }
    }

    static void lateLaneAndTargetFailuresSettlePublishedRows() {
        for (Kind kind : Kind.values()) {
            for (BadArgument bad : BadArgument.values()) {
                Tree t = tree(kind, 1);
                t.touch(1L);
                ProviderCollectionStats before = t.stats();
                Throwable actual = fails(() -> invokeBad(bad, t, 2L));
                check(bad.type.isInstance(actual), kind + " " + bad + " exception type");
                eq(0L, actual.getSuppressed().length, "successful late-failure settlement");
                resident(t, 2L);
                cold(t, 1L);
                delta(before, t.stats(), 1, 0, 0, 0, 1, 0);
                eq(2L, t.model.rowLoads, "validation follows exactly one provider load");
                eq(20L, t.get(0, 2L), "settled payload retains its first column");
                eq(102L, t.get(1, 2L), "settled payload retains its second column");
            }
        }
    }

    static void lateArgumentFailureKeepsPrimaryWhenAdmissionRefuses() {
        for (Kind kind : Kind.values()) {
            for (BadArgument bad : BadArgument.values()) {
                Tree t = pinned(kind);
                ProviderCollectionStats before = t.stats();
                Throwable actual = fails(() -> invokeBad(bad, t, PARENT));
                check(bad.type.isInstance(actual), bad + " must remain primary");
                suppressedAdmission(actual, 0);
                rolledBackPinnedParent(t);
                delta(before, t.stats(), 1, 0, 0, 0, 0, 1);
                eq(2L, t.model.rowLoads, "argument failure loads no extra row");
            }
        }
    }

    static void hintAndPageFailuresSettlePublishedParents() {
        for (Kind kind : Kind.values()) {
            for (Phase phase : new Phase[] {Phase.HINT, Phase.PAGE}) {
                for (boolean error : new boolean[] {false, true}) {
                    Tree t = tree(kind, 1);
                    t.touch(1L);
                    Throwable original = problem(error, phase.name());
                    t.model.fail(phase, PARENT, original);
                    ProviderCollectionStats before = t.stats();
                    Throwable actual = fails(() -> pageDemand(t, error));
                    providerFailure(original, actual, false);
                    eq(0L, actual.getSuppressed().length, "settlement succeeds");
                    resident(t, PARENT);
                    cold(t, 1L);
                    eq(0L, t.count(PARENT), "failed page has no published child links");
                    eq(-1L, t.first(PARENT), "failed child scratch is not a link");
                    eq(0L, t.flags(CHILD), "unpublished child stays undiscovered");
                    check(!has(t.flags(PARENT), LazyElementFlags.CHILDREN_LOADING),
                            "child loading flag cleared");
                    delta(before, t.stats(), 0, 0, 0, 0, 1, 0);
                    eq(2L, t.model.rowLoads, "one newly published parent");
                    eq(phase == Phase.PAGE ? 1L : 0L, t.model.pageLoads, "page callback count");
                    if (kind == Kind.VERSIONED) {
                        eq(phase == Phase.PAGE, t.versioned().hasPinnedChildVersion(PARENT),
                                "version is pinned only after reaching page adapter");
                    }
                }
            }
        }
    }

    static void pageFailureIdentityAndDistinctSuppressionSurviveSettlement() {
        for (Kind kind : Kind.values()) {
            for (boolean error : new boolean[] {false, true}) {
                Tree t = pinned(kind);
                Throwable original = error
                        ? new AssertionError("page error")
                        : new ProviderLoadException(PARENT, new IllegalArgumentException("page"));
                Throwable retained = new IllegalStateException("already suppressed");
                original.addSuppressed(retained);
                t.model.fail(Phase.PAGE, PARENT, original);
                ProviderCollectionStats before = t.stats();
                Throwable actual = fails(() -> pageDemand(t, error));
                same(original, actual, "facade must rethrow the identical delegated primary");
                same(retained, actual.getSuppressed()[0], "existing suppression retained");
                suppressedAdmission(actual, 1);
                rolledBackPinnedParent(t);
                delta(before, t.stats(), 0, 0, 0, 0, 0, 1);
                eq(0L, t.count(PARENT), "failed first page has no links");
                eq(0L, t.cursor(PARENT), "failed first page has no cursor advance");
                if (kind == Kind.VERSIONED) {
                    check(t.versioned().hasPinnedChildVersion(PARENT), "version survives rollback");
                    eq(7L, t.versioned().pinnedChildVersionOrDefault(PARENT, 99L), "pinned token");
                }
            }
        }
    }

    static void successfulPageRefusalPreservesCommittedTopologyAndFlags() {
        for (Kind kind : Kind.values()) {
            for (boolean ensure : new boolean[] {false, true}) {
                Tree t = pinned(kind);
                ProviderCollectionStats before = t.stats();
                Throwable actual = fails(() -> pageDemand(t, ensure));
                admission(actual);
                eq(0L, actual.getSuppressed().length, "normal page admission is not caught twice");
                rolledBackPinnedParent(t);
                linkedFirstPage(t);
                delta(before, t.stats(), 0, 0, 0, 0, 0, 1);
                eq(1L, t.model.pageLoads, "delegated page runs once");
                if (kind == Kind.VERSIONED) {
                    check(t.versioned().hasPinnedChildVersion(PARENT), "version pin survives");
                    eq(7L, t.versioned().pinnedChildVersionOrDefault(PARENT, 99L), "version value");
                }
                t.pin(1L, false);
                before = t.stats();
                eq(1L, t.page(PARENT), "resume after admission refusal");
                resident(t, PARENT);
                eq(2L, t.count(PARENT), "old links retained while second page appends");
                eq(CHILD + 1L, t.next(CHILD), "sibling linkage survives payload rollback");
                eq(t.secondCursor(), t.cursor(PARENT), "cursor resumes rather than restarts");
                delta(before, t.stats(), 0, 0, 0, 0, 1, 0);
                eq(3L, t.model.rowLoads, "setup, refused parent, resumed parent");
            }
        }
    }

    static void ensureFailurePreservesEarlierPageAndPinnedVersion() {
        for (Kind kind : Kind.values()) {
            Tree t = pinned(kind);
            ProviderLoadException original =
                    new ProviderLoadException(PARENT, new IllegalStateException("later page"));
            t.model.fail(Phase.PAGE, PARENT, original);
            t.model.failOnPageCall = 2;
            ProviderCollectionStats before = t.stats();
            Throwable actual = fails(() -> t.ensure(PARENT, 3L, 3));
            same(original, actual, "later page primary");
            suppressedAdmission(actual, 0);
            linkedFirstPage(t);
            rolledBackPinnedParent(t);
            eq(2L, t.model.pageLoads, "one successful page then one failed page");
            eq(2L, t.model.rowLoads, "ensure only materializes the parent once");
            check(!has(t.flags(PARENT), LazyElementFlags.CHILDREN_LOADING), "loading cleared");
            delta(before, t.stats(), 0, 0, 0, 0, 0, 1);
            if (kind == Kind.VERSIONED) {
                eq(1L, t.versioned().versionStats().versionLookups(), "one version lookup");
                eq(2L, t.versioned().versionStats().childPageLoads(), "failed page attempt counted");
                eq(7L, t.versioned().pinnedChildVersionOrDefault(PARENT, 99L), "version retained");
            }
            t.model.clearFailure();
            t.pin(1L, false);
            eq(1L, t.page(PARENT), "next explicit page resumes failed cursor");
            eq(2L, t.count(PARENT), "prior successful page retained");
            eq(CHILD + 1L, t.next(CHILD), "resume child");
            resident(t, PARENT);
        }
    }

    static void cachedCallbackReadsDoNotAdmitTheInFlightParent() {
        for (Kind kind : Kind.values()) {
            for (boolean pageHook : new boolean[] {false, true}) {
                Tree t = tree(kind, 1);
                t.touch(1L);
                LongConsumer inspect = id -> {
                    if (id != PARENT) return;
                    check(t.materialized(PARENT), "published parent is readable inside callback");
                    check(!t.resident(PARENT), "outer parent not admitted yet");
                    check(t.resident(1L), "incumbent remains until callback ends");
                    long beforeEvictions = t.stats().policyEvictions();
                    long beforeObserverTouches = t.model.beforeTouches;
                    eq(100L, t.get(0, PARENT), "cached in-flight first column");
                    long[] target = new long[2];
                    t.read(PARENT, target);
                    row(PARENT, target);
                    check(!t.touch(PARENT), "cached in-flight touch is a hit");
                    eq(10L, t.get(0, 1L), "cached incumbent still readable");
                    check(!t.resident(PARENT), "nested cached reads must not admit early");
                    eq(beforeEvictions, t.stats().policyEvictions(), "no eviction inside callback");
                    eq(beforeObserverTouches, t.model.beforeTouches, "cached reads emit no nested observer");
                    expect(IllegalStateException.class, () -> t.touch(20L));
                    expect(IllegalStateException.class, () -> t.page(20L));
                    eq(0L, t.flags(20L), "rejected cold demand publishes no state");
                };
                if (pageHook) t.model.duringPage = inspect;
                else t.model.afterLoad = inspect;
                ProviderCollectionStats before = t.stats();
                eq(1L, t.page(PARENT), "outer page");
                resident(t, PARENT);
                cold(t, 1L);
                delta(before, t.stats(), 5, 4, 0, 0, 1, 0);
                eq(2L, t.model.rowLoads, "only setup and outer parent load");
                observerCounts(t, 2L, 2L);
            }
        }
    }

    static void mixedInvalidArgumentsRetainDelegatedValidationOrder() {
        for (Kind kind : Kind.values()) {
            Tree t = tree(kind, 1);
            ProviderCollectionStats before = t.stats();
            Events events = t.model.events();
            Throwable minimum = fails(() -> t.ensure(-1L, -1L, -1));
            check(minimum instanceof IllegalArgumentException, "negative minimum precedes bad parent");
            eq("minimumLoaded < 0", minimum.getMessage(), "minimum validation order");
            Throwable budget = fails(() -> t.ensure(-1L, 1L, -1));
            eq(kind == Kind.OFFSET ? "maxPageLoads < 0" : "maxPages < 0",
                    budget.getMessage(), "budget validation precedes parent");
            Throwable idFailure = fails(() -> t.flags(-1L));
            Throwable combined = fails(() -> t.get(-9, -1L));
            eq(idFailure.getClass(), combined.getClass(), "logical id precedes bad lane");
            eq(idFailure.getMessage(), combined.getMessage(), "logical-id diagnostic retained");
            combined = fails(() -> t.read(-1L, null));
            eq(idFailure.getClass(), combined.getClass(), "logical id precedes null target");
            eq(idFailure.getMessage(), combined.getMessage(), "logical-id message retained");
            eq(before, t.stats(), "invalid entry ids are rejected before facade touch count");
            sameEvents(events, t.model.events(), "invalid operations invoke no provider");

            t.model.duringLoad = id -> {
                Throwable nested = fails(() -> t.ensure(-1L, -1L, -1));
                eq("minimumLoaded < 0", nested.getMessage(), "callback keeps negative-first order");
                expect(IndexOutOfBoundsException.class, () -> t.page(-1L));
                expect(IllegalStateException.class, () -> t.page(20L));
                eq(0L, t.ensure(20L, 0L, 0), "zero-work ensure is permitted in callback");
                if (kind == Kind.VERSIONED) {
                    Throwable refresh = fails(() -> t.versioned().refreshChildren(-1L));
                    check(refresh instanceof IllegalStateException,
                            "refresh callback guard precedes invalid parent validation");
                    eq("REENTRANT_PROVIDER_CALLBACK", refresh.getMessage(), "refresh guard diagnostic");
                }
            };
            check(t.touch(1L), "outer callback remains healthy");
            resident(t, 1L);
            eq(0L, t.flags(20L), "callback refusals leave cold parent untouched");
            if (kind == Kind.VERSIONED) {
                expect(IndexOutOfBoundsException.class, () -> t.versioned().refreshChildren(-1L));
            }

            t.model.duringLoad = null;
            RuntimeException original = new IllegalArgumentException("provider wins over null target");
            t.model.fail(Phase.LOAD, 2L, original);
            Throwable actual = fails(() -> t.read(2L, null));
            providerFailure(original, actual, false);
            resident(t, 1L);
        }
    }

    static void offsetRootNavigationAndExpansionSurvivePayloadSettlement() {
        Tree t = tree(Kind.OFFSET, 1);
        BoundedProviderBackedPrimitiveTree tree = t.offset();
        eq(2L, tree.loadNextRootPage(), "roots are discovered without payload");
        ledger(t, 0);
        eq(0L, t.model.rowLoads, "root discovery stays lazy");
        eq(1L, tree.loadNextChildPage(PARENT), "first child page admits parent");
        tree.setExpanded(PARENT, true);
        tree.touch(1L);
        tree.pin(1L, true);
        tree.pin(PARENT, true);
        t.application(PARENT, APP);
        long revision = tree.structureRevision();
        ProviderBackedPrimitiveTree.VisibleCursor cursor = tree.visibleLoadedCursor();
        ProviderCollectionStats before = tree.stats();
        Throwable actual = fails(() -> tree.getBits(-1, PARENT));
        check(actual instanceof IndexOutOfBoundsException, "invalid lane remains primary");
        suppressedAdmission(actual, 0);
        rolledBackPinnedParent(t);
        eq(revision, tree.structureRevision(), "payload rollback does not mutate structure revision");
        check(tree.isLoadedRoot(PARENT), "root membership retained");
        check(tree.isExpanded(PARENT), "expansion retained");
        eq(PARENT + 1L, tree.nextLoadedRoot(PARENT), "root sibling retained");
        eq(PARENT, tree.previousLoadedRoot(PARENT + 1L), "reverse root sibling retained");
        eq(CHILD, tree.firstLoadedChild(PARENT), "child link retained");
        eq(CHILD, tree.lastLoadedChild(PARENT), "last child retained");
        eq(-1L, tree.previousLoadedSibling(CHILD), "first child's predecessor");
        eq(PARENT, tree.parentOf(CHILD), "parent link retained");
        eq(1L, tree.nextChildOffset(PARENT), "offset retained");
        long[] visible = new long[3];
        for (int i = 0; i < visible.length; i++) visible[i] = cursor.nextLong();
        check(!cursor.hasNext(), "preexisting loaded cursor remains valid");
        array(new long[] {PARENT, CHILD, PARENT + 1L}, visible, "visible loaded topology");
        delta(before, tree.stats(), 1, 0, 0, 0, 0, 1);
    }

    static void longIdsAndOpaqueDecreasingCursorsSurviveEviction() {
        for (Kind kind : new Kind[] {Kind.CURSOR, Kind.VERSIONED}) {
            Tree t = highTree(kind, 1);
            long parent = t.model.parent;
            long child = t.model.child;
            t.model.version = -123L;
            check(t.capacity() > HIGH_PARENT, "long-id address capacity");
            eq(Long.MIN_VALUE, END, "reserved completion sentinel");
            eq(1L, t.page(parent), "first long-id page");
            eq(NEXT, t.cursor(parent), "negative provider cursor");
            eq(child, t.first(parent), "child id retains high bits");
            eq(parent, t.parent(child), "parent id retains high bits");
            t.touch(parent + 1L);
            cold(t, parent);
            eq(NEXT, t.cursor(parent), "cursor survives payload eviction");
            eq(1L, t.page(parent), "resume negative cursor");
            eq(LATER, t.cursor(parent), "decreasing opaque cursor is valid");
            eq(3L, t.ensure(parent, 3L, 1), "last page");
            eq(END, t.cursor(parent), "completion sentinel");
            eq(child + 1L, t.next(child), "second sibling");
            eq(child + 2L, t.next(child + 1L), "third sibling");
            eq(-1L, t.next(child + 2L), "end of chain");
            check(!t.materialized(child), "child discovery does not load child payload");
            array(new long[] {0L, NEXT, LATER},
                    Arrays.copyOf(t.model.cursors, 3), "provider receives exact opaque cursors");
            resident(t, parent);
            eq(3L, t.model.rowLoads, "parent, competing row, reloaded parent");
            eq(1L, t.stats().touches(), "only explicit competing demand counts");
            eq(1L, t.stats().loads(), "only explicit competing load counts");
            eq(2L, t.stats().policyEvictions(), "two real payload replacements");
            if (kind == Kind.VERSIONED) {
                eq(-123L, t.versioned().pinnedChildVersionOrDefault(parent, 99L),
                        "negative version token retained");
                array(new long[] {-123L, -123L, -123L},
                        Arrays.copyOf(t.model.tokens, 3), "same version on every page");
            }
        }
    }

    static void staleVersionRequiresExplicitRefresh() {
        Tree t = highTree(Kind.VERSIONED, 1);
        long parent = t.model.parent;
        long oldChild = t.model.child;
        BoundedVersionedProviderBackedPrimitiveLongTree tree = t.versioned();
        eq(1L, tree.loadNextChildPage(parent), "v7 first page");
        tree.touch(1L);
        cold(t, parent);
        t.model.version = 8L;
        t.model.rejectStale = true;
        ProviderCollectionStats before = tree.stats();
        Throwable actual = fails(() -> tree.ensureLoadedChildren(parent, 2L, 1));
        same(t.model.stale, actual, "stale page exception identity");
        eq(0L, actual.getSuppressed().length, "stale failure settles successfully");
        resident(t, parent);
        eq(1L, tree.loadedChildCount(parent), "old page retained");
        eq(NEXT, tree.nextChildCursor(parent), "failed cursor retained");
        eq(7L, tree.pinnedChildVersionOrDefault(parent, -1L), "no automatic repin");
        eq(1L, tree.versionStats().versionLookups(), "no hidden version lookup");
        eq(2L, tree.versionStats().childPageLoads(), "stale page attempt counted once");
        delta(before, tree.stats(), 0, 0, 0, 0, 1, 0);

        tree.touch(1L);
        t.model.child = oldChild + 100L;
        before = tree.stats();
        long loads = t.model.rowLoads;
        eq(1L, tree.refreshChildren(parent), "explicit refresh admits a cold parent");
        eq(loads + 1L, t.model.rowLoads, "refresh performs one parent reload");
        resident(t, parent);
        eq(8L, tree.pinnedChildVersionOrDefault(parent, -1L), "refresh pins current version");
        eq(2L, tree.versionStats().versionLookups(), "one fresh version lookup");
        eq(-1L, tree.parentOf(oldChild), "explicit refresh removes old links");
        eq(oldChild + 100L, tree.firstLoadedChild(parent), "new version topology");
        eq(NEXT, tree.nextChildCursor(parent), "new cache lifetime starts at cursor zero");
        delta(before, tree.stats(), 0, 0, 0, 0, 1, 0);
        eq(0L, t.model.beforeTouches, "version adapter does not forward observers");
        eq(0L, t.model.afterTouches, "version adapter does not forward observers");
    }

    static void refreshAndVersionLookupFailuresSettlePublishedPayload() {
        Tree t = tree(Kind.VERSIONED, 1);
        BoundedVersionedProviderBackedPrimitiveLongTree tree = t.versioned();
        tree.loadNextChildPage(PARENT);
        tree.touch(1L);
        tree.pin(1L, true);
        tree.pin(PARENT, true);
        t.application(PARENT, APP);
        t.model.version = 8L;
        t.model.child = 200L;
        ProviderLoadException original =
                new ProviderLoadException(PARENT, new IllegalStateException("new version page"));
        t.model.fail(Phase.PAGE, PARENT, original);
        ProviderCollectionStats before = tree.stats();
        Throwable actual = fails(() -> tree.refreshChildren(PARENT));
        same(original, actual, "refresh rethrows its actual page primary");
        suppressedAdmission(actual, 0);
        rolledBackPinnedParent(t);
        eq(-1L, tree.parentOf(CHILD), "refresh explicitly discarded the old links");
        eq(0L, tree.loadedChildCount(PARENT), "failed refreshed page publishes no links");
        eq(0L, tree.nextChildCursor(PARENT), "fresh failed page keeps initial cursor");
        eq(8L, tree.pinnedChildVersionOrDefault(PARENT, -1L),
                "new version pin survives payload rollback");
        delta(before, tree.stats(), 0, 0, 0, 0, 0, 1);

        Tree lookup = tree(Kind.VERSIONED, 1);
        lookup.touch(1L);
        AssertionError versionError = new AssertionError("version lookup");
        lookup.model.fail(Phase.VERSION, PARENT, versionError);
        before = lookup.stats();
        actual = fails(() -> lookup.versioned().refreshChildren(PARENT));
        same(versionError, actual, "version lookup Error identity");
        resident(lookup, PARENT);
        cold(lookup, 1L);
        check(!lookup.versioned().hasPinnedChildVersion(PARENT), "failed lookup invents no version");
        eq(0L, lookup.versioned().versionStats().versionLookups(), "only successful lookups count");
        eq(0L, lookup.versioned().versionStats().childPageLoads(), "page adapter not reached");
        eq(1L, lookup.model.versionCalls, "one provider version attempt");
        delta(before, lookup.stats(), 0, 0, 0, 0, 1, 0);
    }

    static void versionedAdapterDoesNotAcquireTouchObserverSemantics() {
        Tree t = tree(Kind.VERSIONED, 1);
        t.model.fail(Phase.AFTER_TOUCH, PARENT, new AssertionError("must not forward afterTouch"));
        check(t.touch(PARENT), "versioned direct touch");
        eq(100L, t.get(0, PARENT), "versioned warm read");
        long[] row = new long[2];
        t.read(PARENT, row);
        row(PARENT, row);
        eq(1L, t.page(PARENT), "versioned page");
        eq(0L, t.model.beforeTouches, "beforeTouch is not forwarded");
        eq(0L, t.model.afterTouches, "afterTouch is not forwarded");
        eq(0L, t.model.touchFailures, "onTouchFailure is not forwarded");
        eq(1L, t.model.rowLoads, "payload is loaded once");
        eq(3L, t.stats().touches(), "explicit facade demand count");
        eq(2L, t.stats().cacheHits(), "explicit warm hits");
        eq(1L, t.stats().loads(), "explicit cold load");
        resident(t, PARENT);
    }

    static void boundedListUsesTheSealedRowFailureSettlement() {
        for (ListDemand demand : ListDemand.values()) {
            for (boolean error : new boolean[] {false, true}) {
                Model provider = new Model();
                BoundedProviderBackedPrimitiveList list = list(provider);
                list.touch(1L);
                list.applicationFlags(2L, APP);
                Throwable original = problem(error, "list after touch");
                provider.fail(Phase.AFTER_TOUCH, 2L, original);
                ProviderCollectionStats before = list.stats();
                Throwable actual = fails(() -> invokeList(demand, list, 2L));
                providerFailure(original, actual, false);
                eq(0L, actual.getSuppressed().length, "list settlement succeeds");
                check(list.isResident(2L) && list.isMaterialized(2L), "list published row admitted");
                check(!list.isResident(1L) && !list.isMaterialized(1L), "list incumbent evicted");
                eq(1L, list.residentCount(), "list resident bound");
                eq(1L, list.materializedSize(), "list payload bound");
                eq(APP, list.applicationFlags(2L), "list application bits");
                eq(100L, list.size(), "logical list size is canonical");
                ProviderCollectionStats after = list.stats();
                eq(before.touches() + 1L, after.touches(), "list failed demand counted once");
                eq(before.cacheHits(), after.cacheHits(), "list failure adds no hit");
                eq(before.loads(), after.loads(), "list failure adds no successful demand load");
                eq(before.loadFailures() + (error ? 0L : 1L), after.loadFailures(),
                        "list cold ProviderLoadException accounting");
                eq(before.retries() + (demand == ListDemand.RETRY ? 1L : 0L), after.retries(),
                        "list explicit retry count");
                eq(before.policyEvictions() + 1L, after.policyEvictions(), "actual list eviction");
                eq(before.admissionFailures(), after.admissionFailures(), "no admission refusal");
                eq(2L, provider.rowLoads, "list load count");
                provider.clearFailure();
                eq(20L, list.getBits(0, 2L), "warm list recovery keeps provider value");
                eq(2L, provider.rowLoads, "list recovery does not reload");
            }
        }
    }

    static void listPinnedRollbackAndValidationPreserveContracts() {
        Model provider = new Model();
        BoundedProviderBackedPrimitiveList list = list(provider);
        list.touch(1L);
        list.pin(1L, true);
        list.pin(2L, true);
        list.applicationFlags(2L, APP);
        AssertionError original = new AssertionError("list postpublication error");
        provider.fail(Phase.AFTER_TOUCH, 2L, original);
        Throwable actual = fails(() -> list.readRow(2L, new long[2]));
        same(original, actual, "list Error identity");
        suppressedAdmission(actual, 0);
        check(list.isResident(1L) && list.isMaterialized(1L), "pinned list incumbent retained");
        check(!list.isResident(2L) && !list.isMaterialized(2L), "pre-pinned list miss rolled back");
        eq(APP, list.applicationFlags(2L), "list application flags retained");
        check(has(list.flags(2L), LazyElementFlags.PINNED), "list pin retained");
        check(has(list.flags(2L), LazyElementFlags.INVALIDATED), "list rollback marks stale");
        eq(1L, list.stats().admissionFailures(), "single list admission refusal");
        eq(0L, list.stats().invalidations(), "internal rollback is not an explicit invalidation");

        provider.clearFailure();
        Events events = provider.events();
        ProviderCollectionStats before = list.stats();
        expect(IndexOutOfBoundsException.class, () -> list.readRow(100L, null));
        expect(IndexOutOfBoundsException.class, () -> list.getBits(-1, 100L));
        expect(IndexOutOfBoundsException.class, () -> list.prefetch(9L, 101L));
        list.prefetch(4L, 4L);
        sameEvents(events, provider.events(), "list rejected indices and empty ranges stay cold");
        eq(before, list.stats(), "list validation precedes row demand");
        actual = fails(() -> list.getBits(-1, 2L));
        check(actual instanceof IndexOutOfBoundsException, "late list lane error remains primary");
        suppressedAdmission(actual, 0);
        eq(2L, list.stats().admissionFailures(), "one refusal for the second real publication");
        eq(APP, list.applicationFlags(2L), "list metadata retained through repeated slot refusal");
        list.pin(1L, false);
        check(list.touch(2L), "list recovers after unpin");
        check(list.isResident(2L) && !list.isResident(1L), "list slot reused by logical id");
        eq(APP, list.applicationFlags(2L), "reused slot retains logical row flags");
        check(!list.evict(2L), "original cold-row pin is still effective");
    }

    private static Tree pinned(Kind kind) {
        Tree t = tree(kind, 1);
        t.touch(1L);
        t.pin(1L, true);
        t.pin(PARENT, true);
        t.application(PARENT, APP);
        return t;
    }

    private static void rolledBackPinnedParent(Tree t) {
        resident(t, 1L);
        cold(t, PARENT);
        check(has(t.flags(PARENT), LazyElementFlags.PINNED), "cold parent's pin retained");
        check(has(t.flags(PARENT), LazyElementFlags.INVALIDATED), "rollback marks payload stale");
        eq(APP, application(t.flags(PARENT)), "cold parent's application flags retained");
    }

    private static void linkedFirstPage(Tree t) {
        eq(1L, t.count(PARENT), "first page count");
        eq(CHILD, t.first(PARENT), "first child");
        eq(PARENT, t.parent(CHILD), "child parent");
        eq(-1L, t.next(CHILD), "first page last sibling");
        eq(t.firstCursor(), t.cursor(PARENT), "first page cursor preserved");
        check(has(t.flags(PARENT), LazyElementFlags.CHILDREN_PARTIAL), "partial flag retained");
        check(!t.materialized(CHILD), "topology does not materialize child payload");
    }

    private static void resident(Tree t, long id) {
        check(t.materialized(id), t.kind + " payload resident id=" + id);
        check(t.resident(id), t.kind + " ledger resident id=" + id);
        ledger(t, 1);
    }

    private static void cold(Tree t, long id) {
        check(!t.materialized(id), t.kind + " payload cold id=" + id);
        check(!t.resident(id), t.kind + " ledger cold id=" + id);
    }

    private static void ledger(Tree t, int expected) {
        eq(expected, t.residentCount(), t.kind + " resident count");
        eq(expected, t.materializedSize(), t.kind + " payload count");
        check(t.residentCount() <= t.maximumResident(), "resident bound");
    }

    private static void observerCounts(Tree t, long before, long after) {
        eq(t.kind == Kind.VERSIONED ? 0L : before, t.model.beforeTouches, "beforeTouch count");
        eq(t.kind == Kind.VERSIONED ? 0L : after, t.model.afterTouches, "afterTouch count");
    }

    private static void delta(
            ProviderCollectionStats before, ProviderCollectionStats after,
            long touches, long hits, long loads, long failures, long evictions, long admissions) {
        eq(before.touches() + touches, after.touches(), "facade touches");
        eq(before.cacheHits() + hits, after.cacheHits(), "facade cache hits");
        eq(before.loads() + loads, after.loads(), "facade successful demand loads");
        eq(before.loadFailures() + failures, after.loadFailures(), "facade load failures");
        eq(before.policyEvictions() + evictions, after.policyEvictions(), "real policy evictions");
        eq(before.admissionFailures() + admissions, after.admissionFailures(), "admission failures");
        eq(before.retries(), after.retries(), "no invented retries");
        eq(before.explicitEvictions(), after.explicitEvictions(), "no invented explicit evictions");
        eq(before.invalidations(), after.invalidations(), "no invented explicit invalidations");
        eq(before.materializedPuts(), after.materializedPuts(), "no invented materialized puts");
    }

    private static void pageDemand(Tree t, boolean ensure) {
        if (ensure) t.ensure(PARENT, 1L, 1);
        else t.page(PARENT);
    }

    private static void invoke(Demand demand, Tree t, long id) {
        switch (demand) {
            case TOUCH -> t.touch(id);
            case GET -> eq(id * 10L, t.get(0, id), "demand column");
            case READ -> {
                long[] target = new long[2];
                t.read(id, target);
                row(id, target);
            }
        }
    }

    private static void invokeBad(BadArgument bad, Tree t, long id) {
        switch (bad) {
            case LOW_LANE -> t.get(-1, id);
            case HIGH_LANE -> t.get(2, id);
            case NULL_TARGET -> t.read(id, null);
            case SHORT_TARGET -> {
                long[] target = {987L};
                try {
                    t.read(id, target);
                } finally {
                    eq(987L, target[0], "short target is not partially overwritten");
                }
            }
        }
    }

    private static void invokeList(
            ListDemand demand, BoundedProviderBackedPrimitiveList list, long id) {
        switch (demand) {
            case TOUCH -> list.touch(id);
            case GET -> list.getBits(0, id);
            case READ -> list.readRow(id, new long[2]);
            case SET -> list.setBits(0, id, 999L);
            case RETRY -> list.retry(id);
        }
    }

    private static BoundedProviderBackedPrimitiveList list(Model provider) {
        return new BoundedProviderBackedPrimitiveList(
                100L, 2, 2, 6, 1, provider, PrimitiveKind.LONG, PrimitiveKind.INT);
    }

    private static void row(long id, long[] target) {
        array(new long[] {id * 10L, (int) (id + 100L)}, target, "two payload columns");
    }

    private static Throwable problem(boolean error, String message) {
        return error ? new AssertionError(message) : new IllegalArgumentException(message);
    }

    private static void providerFailure(Throwable original, Throwable actual, boolean page) {
        if (original instanceof Error || (page && original instanceof ProviderLoadException)) {
            same(original, actual, "provider primary identity");
        } else {
            check(actual instanceof ProviderLoadException, "provider Exception is wrapped once");
            same(original, actual.getCause(), "provider cause identity");
        }
    }

    private static void suppressedAdmission(Throwable primary, int preceding) {
        eq(preceding + 1L, primary.getSuppressed().length, "one distinct admission suppression");
        Throwable secondary = primary.getSuppressed()[preceding];
        check(secondary != primary, "never self-suppress");
        admission(secondary);
        eq(0L, secondary.getSuppressed().length, "admission refusal must not recurse");
    }

    private static void admission(Throwable failure) {
        check(failure instanceof IllegalStateException, "admission exception type");
        eq(ALL_PINNED, failure.getMessage(), "admission diagnostic");
    }

    private static Throwable fails(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException | Error failure) {
            return failure;
        }
        throw new AssertionError("expected operation failure");
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        Throwable actual = fails(action);
        check(type.isInstance(actual), "expected " + type.getName() + ", got " + actual);
    }

    private static void sameEvents(Events expected, Events actual, String message) {
        eq(expected, actual, message);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void same(Object expected, Object actual, String message) {
        if (expected != actual) throw new AssertionError(message + ": identity changed");
    }

    private static void eq(long expected, long actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected=" + expected + ", actual=" + actual);
        }
    }

    private static void eq(boolean expected, boolean actual, String message) {
        if (expected != actual) throw new AssertionError(message + ": expected=" + expected);
    }

    private static void eq(Object expected, Object actual, String message) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(message + ": expected=" + expected + ", actual=" + actual);
        }
    }

    private static void array(long[] expected, long[] actual, String message) {
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(message + ": expected=" + Arrays.toString(expected)
                    + ", actual=" + Arrays.toString(actual));
        }
    }

    private static boolean has(short flags, short bit) {
        return LazyElementFlags.has(flags, bit);
    }

    private static short application(short flags) {
        return (short) (flags & LazyElementFlags.APPLICATION_MASK);
    }

    private static Kind[] observedKinds() {
        return new Kind[] {Kind.OFFSET, Kind.CURSOR};
    }

    private enum Kind { OFFSET, CURSOR, VERSIONED }
    private enum Demand { TOUCH, GET, READ }
    private enum ListDemand { TOUCH, GET, READ, SET, RETRY }
    private enum Phase { NONE, LOAD, AFTER_LOAD, AFTER_TOUCH, HINT, PAGE, VERSION }

    private enum BadArgument {
        LOW_LANE(IndexOutOfBoundsException.class),
        HIGH_LANE(IndexOutOfBoundsException.class),
        NULL_TARGET(NullPointerException.class),
        SHORT_TARGET(IllegalArgumentException.class);

        final Class<? extends RuntimeException> type;

        BadArgument(Class<? extends RuntimeException> type) {
            this.type = type;
        }
    }

    private record Events(
            long rowLoads, long beforeTouches, long afterTouches, long hints,
            long pageLoads, long versionCalls, long rootPages, long rootHints) {}

    private static Tree tree(Kind kind, int maximumResident) {
        return new Tree(kind, maximumResident, false);
    }

    private static Tree highTree(Kind kind, int maximumResident) {
        return new Tree(kind, maximumResident, true);
    }

    /** Test-owned dispatch only; all calls reach the shipped facade API. */
    private static final class Tree {
        final Kind kind;
        final Model model = new Model();
        final Object facade;

        Tree(Kind kind, int maximumResident, boolean high) {
            this.kind = kind;
            int root = high ? 13 : 2;
            int directory = high ? 8 : 2;
            int leaf = high ? 12 : 6;
            if (high) {
                model.parent = HIGH_PARENT;
                model.child = HIGH_PARENT + 100L;
            }
            facade = switch (kind) {
                case OFFSET -> new BoundedProviderBackedPrimitiveTree(
                        root, directory, leaf, maximumResident, model, 2,
                        PrimitiveKind.LONG, PrimitiveKind.INT);
                case CURSOR -> new BoundedProviderBackedPrimitiveLongTree(
                        root, directory, leaf, maximumResident, model, 2,
                        PrimitiveKind.LONG, PrimitiveKind.INT);
                case VERSIONED -> new BoundedVersionedProviderBackedPrimitiveLongTree(
                        root, directory, leaf, maximumResident, model, 2,
                        PrimitiveKind.LONG, PrimitiveKind.INT);
            };
        }

        BoundedProviderBackedPrimitiveTree offset() {
            return (BoundedProviderBackedPrimitiveTree) facade;
        }

        BoundedProviderBackedPrimitiveLongTree cursorTree() {
            return (BoundedProviderBackedPrimitiveLongTree) facade;
        }

        BoundedVersionedProviderBackedPrimitiveLongTree versioned() {
            return (BoundedVersionedProviderBackedPrimitiveLongTree) facade;
        }

        boolean touch(long id) {
            return switch (kind) {
                case OFFSET -> offset().touch(id);
                case CURSOR -> cursorTree().touch(id);
                case VERSIONED -> versioned().touch(id);
            };
        }

        long get(int lane, long id) {
            return switch (kind) {
                case OFFSET -> offset().getBits(lane, id);
                case CURSOR -> cursorTree().getBits(lane, id);
                case VERSIONED -> versioned().getBits(lane, id);
            };
        }

        void read(long id, long[] target) {
            switch (kind) {
                case OFFSET -> offset().readRow(id, target);
                case CURSOR -> cursorTree().readRow(id, target);
                case VERSIONED -> versioned().readRow(id, target);
            }
        }

        int page(long id) {
            return switch (kind) {
                case OFFSET -> offset().loadNextChildPage(id);
                case CURSOR -> cursorTree().loadNextChildPage(id);
                case VERSIONED -> versioned().loadNextChildPage(id);
            };
        }

        long ensure(long id, long minimum, int pages) {
            return switch (kind) {
                case OFFSET -> offset().ensureLoadedChildren(id, Math.toIntExact(minimum), pages);
                case CURSOR -> cursorTree().ensureLoadedChildren(id, minimum, pages);
                case VERSIONED -> versioned().ensureLoadedChildren(id, minimum, pages);
            };
        }

        int prefetch(long id, long first, int count) {
            return switch (kind) {
                case OFFSET -> offset().prefetchLoadedChildren(id, Math.toIntExact(first), count);
                case CURSOR -> cursorTree().prefetchLoadedChildren(id, first, count);
                case VERSIONED -> versioned().prefetchLoadedChildren(id, first, count);
            };
        }

        boolean materialized(long id) {
            return switch (kind) {
                case OFFSET -> offset().isMaterialized(id);
                case CURSOR -> cursorTree().isMaterialized(id);
                case VERSIONED -> versioned().isMaterialized(id);
            };
        }

        boolean resident(long id) {
            return switch (kind) {
                case OFFSET -> offset().isResident(id);
                case CURSOR -> cursorTree().isResident(id);
                case VERSIONED -> versioned().isResident(id);
            };
        }

        short flags(long id) {
            return switch (kind) {
                case OFFSET -> offset().flags(id);
                case CURSOR -> cursorTree().flags(id);
                case VERSIONED -> versioned().flags(id);
            };
        }

        long first(long id) {
            return switch (kind) {
                case OFFSET -> offset().firstLoadedChild(id);
                case CURSOR -> cursorTree().firstLoadedChild(id);
                case VERSIONED -> versioned().firstLoadedChild(id);
            };
        }

        long next(long id) {
            return switch (kind) {
                case OFFSET -> offset().nextLoadedSibling(id);
                case CURSOR -> cursorTree().nextLoadedSibling(id);
                case VERSIONED -> versioned().nextLoadedSibling(id);
            };
        }

        long parent(long id) {
            return switch (kind) {
                case OFFSET -> offset().parentOf(id);
                case CURSOR -> cursorTree().parentOf(id);
                case VERSIONED -> versioned().parentOf(id);
            };
        }

        long count(long id) {
            return switch (kind) {
                case OFFSET -> offset().loadedChildCount(id);
                case CURSOR -> cursorTree().loadedChildCount(id);
                case VERSIONED -> versioned().loadedChildCount(id);
            };
        }

        long cursor(long id) {
            return switch (kind) {
                case OFFSET -> offset().nextChildOffset(id);
                case CURSOR -> cursorTree().nextChildCursor(id);
                case VERSIONED -> versioned().nextChildCursor(id);
            };
        }

        boolean complete(long id) {
            return has(flags(id), LazyElementFlags.CHILDREN_COMPLETE);
        }

        long firstCursor() { return kind == Kind.OFFSET ? 1L : NEXT; }
        long secondCursor() { return kind == Kind.OFFSET ? 2L : LATER; }

        int residentCount() {
            return switch (kind) {
                case OFFSET -> offset().residentCount();
                case CURSOR -> cursorTree().residentCount();
                case VERSIONED -> versioned().residentCount();
            };
        }

        int maximumResident() {
            return switch (kind) {
                case OFFSET -> offset().maximumResident();
                case CURSOR -> cursorTree().maximumResident();
                case VERSIONED -> versioned().maximumResident();
            };
        }

        long materializedSize() {
            return switch (kind) {
                case OFFSET -> offset().materializedSize();
                case CURSOR -> cursorTree().materializedSize();
                case VERSIONED -> versioned().materializedSize();
            };
        }

        long capacity() {
            return switch (kind) {
                case OFFSET -> offset().logicalCapacity();
                case CURSOR -> cursorTree().logicalCapacity();
                case VERSIONED -> versioned().logicalCapacity();
            };
        }

        long bytes() {
            return switch (kind) {
                case OFFSET -> offset().allocatedPayloadBytes();
                case CURSOR -> cursorTree().allocatedPayloadBytes();
                case VERSIONED -> versioned().allocatedPayloadBytes();
            };
        }

        ProviderCollectionStats stats() {
            return switch (kind) {
                case OFFSET -> offset().stats();
                case CURSOR -> cursorTree().stats();
                case VERSIONED -> versioned().stats();
            };
        }

        void pin(long id, boolean value) {
            switch (kind) {
                case OFFSET -> offset().pin(id, value);
                case CURSOR -> cursorTree().pin(id, value);
                case VERSIONED -> versioned().pin(id, value);
            }
        }

        boolean invalidate(long id) {
            return switch (kind) {
                case OFFSET -> offset().invalidatePayload(id);
                case CURSOR -> cursorTree().invalidatePayload(id);
                case VERSIONED -> versioned().invalidatePayload(id);
            };
        }

        boolean evict(long id) {
            return switch (kind) {
                case OFFSET -> offset().evictPayload(id);
                case CURSOR -> cursorTree().evictPayload(id);
                case VERSIONED -> versioned().evictPayload(id);
            };
        }

        void application(long id, short bits) {
            // The tree facade has no application-flag setter. Seed its existing row owner only
            // during test setup, without adding a test-only production API or parallel state.
            Object owner = field(facade, "tree");
            if (kind == Kind.VERSIONED) owner = field(owner, "tree");
            ((ProviderBackedPrimitiveRows) field(owner, "nodes")).applicationFlags(id, bits);
        }
    }

    private static boolean referenceBit(Tree t, long id) {
        Object clock = field(t.facade, "residency");
        return ((LazyBitAddressSpace) field(clock, "referenced")).get(id);
    }

    private static Object field(Object owner, String name) {
        try {
            Field field = owner.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(owner);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("test metadata access " + name, failure);
        }
    }

    /** One deterministic dataset, presented through the existing three provider contracts. */
    private static final class Model implements
            PagedPrimitiveTreeProvider,
            CursorPagedPrimitiveTreeProvider,
            VersionedCursorPagedPrimitiveTreeProvider,
            PrimitiveTouchObserver {
        long parent = PARENT;
        long child = CHILD;
        long version = 7L;
        PagedPrimitiveTreeProvider.ChildHint hint = PagedPrimitiveTreeProvider.ChildHint.SOME;
        long rowLoads;
        long beforeTouches;
        long afterTouches;
        long hints;
        long pageLoads;
        long versionCalls;
        long rootPages;
        long rootHints;
        long touchFailures;
        final long[] cursors = new long[16];
        final long[] tokens = new long[16];
        final ProviderLoadException stale =
                new ProviderLoadException(PARENT, new IllegalStateException("stale child version"));
        boolean rejectStale;
        Phase phase = Phase.NONE;
        long failId = -1L;
        int failOnPageCall;
        Throwable failure;
        LongConsumer duringLoad;
        LongConsumer afterLoad;
        LongConsumer duringPage;

        void fail(Phase at, long id, Throwable problem) {
            phase = at;
            failId = id;
            failure = problem;
            failOnPageCall = 0;
        }

        void clearFailure() {
            phase = Phase.NONE;
            failure = null;
            failOnPageCall = 0;
        }

        private void maybeFail(Phase at, long id) {
            if (phase != at || id != failId) return;
            if (at == Phase.PAGE && failOnPageCall != 0 && pageLoads != failOnPageCall) return;
            if (failure instanceof Error error) throw error;
            throw (RuntimeException) failure;
        }

        @Override
        public void load(long rowId, long[] target) {
            rowLoads++;
            target[0] = rowId * 10L;
            if (duringLoad != null) duringLoad.accept(rowId);
            maybeFail(Phase.LOAD, rowId);
            if (target.length > 1) target[1] = rowId + 100L;
        }

        @Override
        public void afterLoad(long rowId, short flags) {
            if (afterLoad != null) afterLoad.accept(rowId);
            maybeFail(Phase.AFTER_LOAD, rowId);
        }

        @Override
        public void beforeTouch(long rowId, short flags) {
            beforeTouches++;
        }

        @Override
        public void afterTouch(long rowId, short flags, boolean materializedNow) {
            afterTouches++;
            maybeFail(Phase.AFTER_TOUCH, rowId);
        }

        @Override
        public void onTouchFailure(long rowId, short flags, Exception failure) {
            touchFailures++;
        }

        @Override
        public PagedPrimitiveTreeProvider.ChildHint childHint(long nodeId) {
            hints++;
            maybeFail(Phase.HINT, nodeId);
            return nodeId == parent ? hint : PagedPrimitiveTreeProvider.ChildHint.NONE;
        }

        @Override
        public PagedPrimitiveTreeProvider.ChildHint rootHint() {
            rootHints++;
            return PagedPrimitiveTreeProvider.ChildHint.SOME;
        }

        @Override
        public long loadRoots(int offset, int limit, long[] ids) {
            rootPages++;
            int count = Math.min(limit, 2 - offset);
            for (int i = 0; i < count; i++) ids[i] = parent + offset + i;
            return PagedPrimitiveTreeProvider.pageResult(
                    count, offset + count == 2 ? -1 : offset + count);
        }

        @Override
        public long loadChildren(long id, int offset, int limit, long[] ids) {
            long[] next = new long[1];
            int count = page(id, offset, offset, version, ids, next);
            return PagedPrimitiveTreeProvider.pageResult(
                    count, next[0] == CursorPagedPrimitiveTreeProvider.END_CURSOR ? -1 : offset + count);
        }

        @Override
        public int loadChildren(
                long id, long cursor, int limit, long[] ids, long[] next) {
            return page(id, cursor, ordinal(cursor), version, ids, next);
        }

        @Override
        public long childVersion(long id) {
            versionCalls++;
            maybeFail(Phase.VERSION, id);
            return version;
        }

        @Override
        public int loadChildren(
                long id, long token, long cursor, int limit, long[] ids, long[] next) {
            return page(id, cursor, ordinal(cursor), token, ids, next);
        }

        private int page(
                long id, long cursor, int ordinal, long token, long[] ids, long[] next) {
            cursors[Math.toIntExact(pageLoads)] = cursor;
            tokens[Math.toIntExact(pageLoads)] = token;
            pageLoads++;
            if (rejectStale && token != version) throw stale;
            int count = id == parent && ordinal < 3 ? 1 : 0;
            if (count != 0) ids[0] = child + ordinal;
            next[0] = count == 0 || ordinal == 2
                    ? CursorPagedPrimitiveTreeProvider.END_CURSOR
                    : ordinal == 0 ? NEXT : LATER;
            if (duringPage != null) duringPage.accept(id);
            maybeFail(Phase.PAGE, id);
            return count;
        }

        private static int ordinal(long cursor) {
            if (cursor == 0L) return 0;
            if (cursor == NEXT) return 1;
            if (cursor == LATER) return 2;
            throw new AssertionError("unexpected provider cursor " + cursor);
        }

        Events events() {
            return new Events(
                    rowLoads, beforeTouches, afterTouches, hints,
                    pageLoads, versionCalls, rootPages, rootHints);
        }
    }
}
