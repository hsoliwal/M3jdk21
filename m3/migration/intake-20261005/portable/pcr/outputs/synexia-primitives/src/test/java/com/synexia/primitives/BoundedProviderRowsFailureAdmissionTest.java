// SPDX-License-Identifier: Apache-2.0
package com.synexia.primitives;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class BoundedProviderRowsFailureAdmissionTest {
    @Test
    void everyDemandAccountsForPublishedRowsWhenAfterTouchFails() {
        for (Demand demand : Demand.values()) {
            for (FailureKind kind : FailureKind.values()) {
                AfterTouchProvider provider = new AfterTouchProvider(kind);
                BoundedProviderBackedPrimitiveRows rows = bounded(provider);
                rows.applicationFlags(1L, application(1L));
                assertTrue(rows.touch(1L));
                provider.failAfterTouch = true;

                for (long rowId = 2L; rowId <= 4L; rowId++) {
                    long id = rowId;
                    rows.applicationFlags(id, application(id));
                    Throwable failure = observeFailure(provider,
                            () -> invoke(demand, rows, id), id);
                    assertEquals(0, failure.getSuppressed().length);
                    assertTrue(rows.isMaterialized(id), label(demand, kind));
                    assertTrue(rows.isResident(id), label(demand, kind));
                    assertFalse(rows.isMaterialized(id - 1L), label(demand, kind));
                    assertFalse(rows.isResident(id - 1L), label(demand, kind));
                    assertEquals(application(id), rows.applicationFlags(id));
                    assertEquals(application(id - 1L), rows.applicationFlags(id - 1L));
                    assertLedger(rows, 1);

                    long failedDemands = id - 1L;
                    ProviderCollectionStats stats = rows.stats();
                    assertEquals(id, stats.touches());
                    assertEquals(0L, stats.cacheHits());
                    assertEquals(1L, stats.loads(),
                            "failed demands retain the historical successful-demand load count");
                    assertEquals(kind == FailureKind.EXCEPTION ? failedDemands : 0L,
                            stats.loadFailures());
                    assertEquals(demand == Demand.RETRY ? failedDemands : 0L, stats.retries());
                    assertEquals(failedDemands, stats.policyEvictions());
                    assertEquals(0L, stats.explicitEvictions());
                    assertEquals(0L, stats.invalidations());
                    assertEquals(0L, stats.admissionFailures());
                    assertEquals(id, provider.loads, "each delegated demand loads once");
                }

                provider.failAfterTouch = false;
                assertEquals(40L, rows.getBits(0, 4L));
                assertEquals(4L, provider.loads, "reconciled payload is a warm hit");
                assertEquals(1L, rows.stats().cacheHits());
                assertLedger(rows, 1);
            }
        }
    }

    @Test
    void allPinnedRefusalPreservesPrimaryAndRollsBackPrePinnedMiss() {
        for (Demand demand : Demand.values()) {
            for (FailureKind kind : FailureKind.values()) {
                AfterTouchProvider provider = new AfterTouchProvider(kind);
                BoundedProviderBackedPrimitiveRows rows = bounded(provider);
                assertTrue(rows.touch(1L));
                rows.pin(1L, true);
                rows.pin(2L, true);
                rows.applicationFlags(2L, LazyElementFlags.APPLICATION_3);
                provider.failAfterTouch = true;

                Throwable primary = observeFailure(provider,
                        () -> invoke(demand, rows, 2L), 2L);
                assertEquals(1, primary.getSuppressed().length);
                Throwable admission = primary.getSuppressed()[0];
                assertInstanceOf(IllegalStateException.class, admission);
                assertEquals("PROVIDER_RESIDENCY_ALL_PINNED", admission.getMessage());
                assertTrue(rows.isResident(1L));
                assertTrue(rows.isMaterialized(1L));
                assertFalse(rows.isResident(2L));
                assertFalse(rows.isMaterialized(2L));
                assertTrue(LazyElementFlags.has(rows.flags(2L), LazyElementFlags.PINNED));
                assertTrue(LazyElementFlags.has(rows.flags(2L), LazyElementFlags.INVALIDATED));
                assertEquals(LazyElementFlags.APPLICATION_3, rows.applicationFlags(2L));
                assertLedger(rows, 1);

                ProviderCollectionStats stats = rows.stats();
                assertEquals(2L, stats.touches());
                assertEquals(1L, stats.loads());
                assertEquals(kind == FailureKind.EXCEPTION ? 1L : 0L, stats.loadFailures());
                assertEquals(demand == Demand.RETRY ? 1L : 0L, stats.retries());
                assertEquals(0L, stats.policyEvictions());
                assertEquals(1L, stats.admissionFailures());
                assertEquals(0L, stats.invalidations(),
                        "internal admission rollback is not explicit user invalidation");
                assertEquals(2L, provider.loads);

                provider.failAfterTouch = false;
                rows.pin(1L, false);
                assertTrue(rows.touch(2L), "the rejected row may be demanded again");
                assertTrue(rows.isResident(2L));
                assertFalse(rows.isResident(1L));
                assertFalse(rows.evict(2L), "new row's original pin remains authoritative");
                assertEquals(3L, provider.loads);
                assertEquals(1L, rows.stats().admissionFailures());
                assertEquals(1L, rows.stats().policyEvictions());
                assertLedger(rows, 1);
            }
        }
    }

    @Test
    void normalAdmissionFailureIsNotReconciledOrCountedTwice() {
        AfterTouchProvider provider = new AfterTouchProvider(FailureKind.EXCEPTION);
        BoundedProviderBackedPrimitiveRows rows = bounded(provider);
        assertTrue(rows.touch(1L));
        rows.pin(1L, true);
        rows.pin(2L, true);
        rows.applicationFlags(2L, LazyElementFlags.APPLICATION_0);

        IllegalStateException failure = assertThrows(
                IllegalStateException.class, () -> rows.touch(2L));
        assertEquals("PROVIDER_RESIDENCY_ALL_PINNED", failure.getMessage());
        assertEquals(0, failure.getSuppressed().length);
        assertFalse(rows.isMaterialized(2L));
        assertFalse(rows.isResident(2L));
        assertTrue(LazyElementFlags.has(rows.flags(2L), LazyElementFlags.PINNED));
        assertEquals(LazyElementFlags.APPLICATION_0, rows.applicationFlags(2L));
        assertTrue(rows.isResident(1L));
        assertEquals(2L, provider.loads);
        assertEquals(2L, rows.stats().loads(),
                "normal successful delegation retains the existing load counter policy");
        assertEquals(0L, rows.stats().loadFailures());
        assertEquals(1L, rows.stats().admissionFailures());
        assertLedger(rows, 1);
    }

    @Test
    void lateColumnAndTargetValidationFailuresAlsoEnterResidency() {
        AfterTouchProvider provider = new AfterTouchProvider(FailureKind.EXCEPTION);
        BoundedProviderBackedPrimitiveRows rows = bounded(provider);
        assertTrue(rows.touch(1L));
        long id = 2L;
        for (BadArgument argument : BadArgument.values()) {
            long rowId = id++;
            RuntimeException failure = assertThrows(
                    argument.failureType, () -> invokeBad(argument, rows, rowId));
            assertEquals(0, failure.getSuppressed().length);
            assertTrue(rows.isMaterialized(rowId), argument.name());
            assertTrue(rows.isResident(rowId), argument.name());
            assertFalse(rows.isResident(rowId - 1L), argument.name());
            assertLedger(rows, 1);
        }
        assertEquals(5L, provider.loads);
        assertEquals(5L, rows.stats().touches());
        assertEquals(1L, rows.stats().loads());
        assertEquals(0L, rows.stats().loadFailures());
        assertEquals(4L, rows.stats().policyEvictions());
        assertEquals(50L, rows.getBits(0, 5L));
        assertEquals(5L, provider.loads);
    }

    @Test
    void failureBeforePublicationLeavesResidentAndRetryAccountingIntact() {
        AfterTouchProvider provider = new AfterTouchProvider(FailureKind.EXCEPTION);
        BoundedProviderBackedPrimitiveRows rows = bounded(provider);
        assertTrue(rows.touch(1L));
        provider.failLoadId = 2L;

        ProviderLoadException failedLoad = assertThrows(
                ProviderLoadException.class, () -> rows.touch(2L));
        assertSame(provider.loadFailure, failedLoad.getCause());
        assertTrue(rows.isResident(1L));
        assertFalse(rows.isMaterialized(2L));
        assertTrue(LazyElementFlags.has(rows.flags(2L), LazyElementFlags.FAILED));
        assertEquals(1L, rows.stats().loadFailures());
        assertEquals(0L, rows.stats().policyEvictions());
        assertLedger(rows, 1);

        provider.failLoadId = -1L;
        provider.failAfterTouch = true;
        ProviderLoadException retryFailure = assertThrows(
                ProviderLoadException.class, () -> rows.retry(2L));
        assertSame(provider.notificationFailure, retryFailure.getCause());
        assertTrue(rows.isResident(2L));
        assertTrue(rows.isMaterialized(2L));
        assertFalse(rows.isResident(1L));
        assertEquals(3L, provider.loads);
        assertEquals(1L, rows.stats().loads());
        assertEquals(2L, rows.stats().loadFailures());
        assertEquals(1L, rows.stats().retries());
        assertEquals(1L, rows.stats().policyEvictions());
        assertLedger(rows, 1);
    }

    @Test
    void cachedProviderReadDoesNotAdmitTheInFlightRowTwice() {
        final class Provider implements PrimitiveRowProvider, PrimitiveTouchObserver {
            BoundedProviderBackedPrimitiveRows rows;
            int beforeTouches;
            int afterTouches;
            int loads;
            boolean inspect;

            @Override
            public void beforeTouch(long id, short flags) {
                beforeTouches++;
            }

            @Override
            public void afterTouch(long id, short flags, boolean loaded) {
                afterTouches++;
            }

            @Override
            public void load(long id, long[] target) {
                loads++;
                target[0] = id * 10L;
                if (inspect) assertEquals(10L, rows.getBits(0, 1L));
            }

            @Override
            public void afterLoad(long id, short flags) {
                if (inspect) assertEquals(20L, rows.getBits(0, id));
            }
        }

        Provider provider = new Provider();
        BoundedProviderBackedPrimitiveRows rows = new BoundedProviderBackedPrimitiveRows(
                2, 2, 6, 1, provider, PrimitiveKind.LONG);
        provider.rows = rows;
        assertTrue(rows.touch(1L));
        provider.inspect = true;
        assertTrue(rows.touch(2L));
        assertEquals(2, provider.loads);
        assertEquals(2, provider.beforeTouches);
        assertEquals(2, provider.afterTouches);
        assertEquals(1L, rows.stats().policyEvictions());
        assertTrue(rows.isResident(2L));
        assertFalse(rows.isResident(1L));
        assertLedger(rows, 1);
    }

    private static BoundedProviderBackedPrimitiveRows bounded(PrimitiveRowProvider provider) {
        return new BoundedProviderBackedPrimitiveRows(
                2, 2, 6, 1, provider, PrimitiveKind.LONG, PrimitiveKind.LONG);
    }

    private static void assertLedger(BoundedProviderBackedPrimitiveRows rows, int count) {
        assertEquals(count, rows.residentCount());
        assertEquals((long) count, rows.materializedSize());
        assertTrue(rows.residentCount() <= rows.maxResidentRows());
    }

    private static short application(long id) {
        return (id & 1L) == 0L
                ? LazyElementFlags.APPLICATION_3
                : LazyElementFlags.APPLICATION_0;
    }

    private static String label(Demand demand, FailureKind kind) {
        return demand + "/" + kind;
    }

    private static Throwable observeFailure(
            AfterTouchProvider provider,
            org.junit.jupiter.api.function.Executable operation,
            long rowId) {
        if (provider.notificationFailure instanceof Error error) {
            Error actual = assertThrows(Error.class, operation);
            assertSame(error, actual);
            return actual;
        }
        ProviderLoadException actual = assertThrows(ProviderLoadException.class, operation);
        assertEquals(rowId, actual.logicalId());
        assertSame(provider.notificationFailure, actual.getCause());
        return actual;
    }

    private static void invoke(
            Demand demand, BoundedProviderBackedPrimitiveRows rows, long rowId) {
        switch (demand) {
            case TOUCH -> rows.touch(rowId);
            case GET -> rows.getBits(0, rowId);
            case READ -> rows.readRow(rowId, new long[2]);
            case SET -> rows.setBits(0, rowId, 999L);
            case RETRY -> rows.retry(rowId);
        }
    }

    private static void invokeBad(
            BadArgument argument, BoundedProviderBackedPrimitiveRows rows, long rowId) {
        switch (argument) {
            case GET_LANE -> rows.getBits(-1, rowId);
            case SET_LANE -> rows.setBits(-1, rowId, 999L);
            case READ_NULL -> rows.readRow(rowId, null);
            case READ_SHORT -> rows.readRow(rowId, new long[1]);
        }
    }

    private enum Demand { TOUCH, GET, READ, SET, RETRY }
    private enum FailureKind { EXCEPTION, ERROR }

    private enum BadArgument {
        GET_LANE(IndexOutOfBoundsException.class),
        SET_LANE(IndexOutOfBoundsException.class),
        READ_NULL(NullPointerException.class),
        READ_SHORT(IllegalArgumentException.class);

        private final Class<? extends RuntimeException> failureType;

        BadArgument(Class<? extends RuntimeException> failureType) {
            this.failureType = failureType;
        }
    }

    private static final class AfterTouchProvider
            implements PrimitiveRowProvider, PrimitiveTouchObserver {
        private final Throwable notificationFailure;
        private final IOException loadFailure = new IOException("load failed before publication");
        private boolean failAfterTouch;
        private long failLoadId = -1L;
        private long loads;

        private AfterTouchProvider(FailureKind kind) {
            notificationFailure = kind == FailureKind.EXCEPTION
                    ? new IOException("after-touch notification failed")
                    : new AssertionError("after-touch notification error");
        }

        @Override
        public void load(long rowId, long[] target) throws Exception {
            loads++;
            target[0] = rowId * 10L;
            target[1] = rowId + 100L;
            if (rowId == failLoadId) throw loadFailure;
        }

        @Override
        public void afterTouch(long rowId, short flags, boolean materializedNow) throws Exception {
            if (!failAfterTouch || rowId < 2L) return;
            if (notificationFailure instanceof Error error) throw error;
            throw (Exception) notificationFailure;
        }
    }
}
