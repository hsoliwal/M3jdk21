// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/SearchEngineMonitorHandoffTest.java (blob 3e40411b7ad5) by M3_SEARCH_FORWARD v1.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jdk.m3.collections.M3DefaultProgressMonitor;
import jdk.m3.collections.M3ProgressMonitor;
import jdk.m3.collections.M3ProgressState;
import java.util.concurrent.CancellationException;
import java.util.function.IntConsumer;
import org.junit.jupiter.api.Test;

/** Failure-atomic cancellation of a newly created but unpublished search lease. */
final class SearchEngineMonitorHandoffTest {
    private static final byte[] NEEDLE = {42};

    @Test
    void postCompilationCancellationClosesUnpublishedPatternExactlyOnce() {
        OwnedPattern pattern = new OwnedPattern();
        Provider owner = new Provider(pattern);
        CancellationException canceled = new CancellationException("after compile");
        Monitor monitor = new Monitor(poll -> {
            if (poll == 2) throw canceled;
        });
        assertSame(canceled, assertThrows(CancellationException.class,
                () -> owner.compile(NEEDLE, monitor)));
        assertEquals(2, monitor.polls);
        assertEquals(1, owner.compiles);
        assertEquals(1, pattern.closes);
    }

    @Test
    void firstMonitorFailureDoesNotCallCompilerOrReleaseAnything() {
        OwnedPattern pattern = new OwnedPattern();
        Provider owner = new Provider(pattern);
        CancellationException canceled = new CancellationException("before compile");
        Monitor monitor = new Monitor(poll -> { throw canceled; });
        assertSame(canceled, assertThrows(CancellationException.class,
                () -> owner.compile(NEEDLE, monitor)));
        assertEquals(1, monitor.polls);
        assertEquals(0, owner.compiles);
        assertEquals(0, pattern.closes);
    }

    @Test
    void successfulTransferReturnsSamePatternWithoutClosingIt() {
        OwnedPattern pattern = new OwnedPattern();
        Provider owner = new Provider(pattern);
        Monitor monitor = new Monitor(ignored -> {});
        CompiledSearch returned = owner.compile(NEEDLE, monitor);
        assertSame(pattern, returned);
        assertEquals(2, monitor.polls);
        assertEquals(1, owner.compiles);
        assertEquals(0, pattern.closes);
        returned.close();
        assertEquals(1, pattern.closes);
    }

    @Test
    void cleanupFailureIsSuppressedOntoOriginalCancellationByIdentity() {
        OwnedPattern pattern = new OwnedPattern();
        IllegalStateException cleanup = new IllegalStateException("close failed");
        pattern.closeFailure = cleanup;
        Provider owner = new Provider(pattern);
        CancellationException canceled = new CancellationException("original cancel");
        Monitor monitor = new Monitor(poll -> {
            if (poll == 2) throw canceled;
        });
        Throwable observed = assertThrows(CancellationException.class,
                () -> owner.compile(NEEDLE, monitor));
        assertSame(canceled, observed);
        assertEquals(1, pattern.closes);
        assertEquals(1, observed.getSuppressed().length);
        assertSame(cleanup, observed.getSuppressed()[0]);
    }

    @Test
    void cleanupThrowingSameFailureInstanceDoesNotSelfSuppress() {
        OwnedPattern pattern = new OwnedPattern();
        IllegalStateException exception = new IllegalStateException("same object");
        pattern.closeFailure = exception;
        Provider owner = new Provider(pattern);
        Monitor monitor = new Monitor(poll -> {
            if (poll == 2) throw exception;
        });
        assertSame(exception, assertThrows(IllegalStateException.class,
                () -> owner.compile(NEEDLE, monitor)));
        assertEquals(0, exception.getSuppressed().length);
        assertEquals(1, pattern.closes);
    }

    @Test
    void nullProviderResultRetainsOriginalCompatibilityInBothPaths() {
        Provider owner = new Provider(null);
        Monitor good = new Monitor(ignored -> {});
        assertNull(owner.compile(NEEDLE, good));
        assertEquals(2, good.polls);
        CancellationException canceled = new CancellationException("null result cancelled");
        Monitor failure = new Monitor(poll -> {
            if (poll == 2) throw canceled;
        });
        assertSame(canceled, assertThrows(CancellationException.class,
                () -> owner.compile(NEEDLE, failure)));
        assertEquals(2, owner.compiles);
    }

    @Test
    void originalFactoryFailureIsNotReplacedOrRetried() {
        IllegalArgumentException failure = new IllegalArgumentException("bad needle");
        Provider owner = new Provider(null);
        owner.factoryFailure = failure;
        Monitor monitor = new Monitor(ignored -> {});
        assertSame(failure, assertThrows(IllegalArgumentException.class,
                () -> owner.compile(NEEDLE, monitor)));
        assertEquals(1, monitor.polls);
        assertEquals(1, owner.compiles);
    }

    @Test
    void nullMonitorUsesExistingNoopCompatibilityWithoutEarlyClose() {
        OwnedPattern pattern = new OwnedPattern();
        Provider owner = new Provider(pattern);
        assertSame(pattern, owner.compile(NEEDLE, null));
        assertEquals(1, owner.compiles);
        assertEquals(0, pattern.closes);
    }

    private static final class Provider implements SearchEngine {
        private final CompiledSearch supplied;
        private RuntimeException factoryFailure;
        private int compiles;

        private Provider(CompiledSearch supplied) { this.supplied = supplied; }
        @Override public String name() { return "lease-test-provider"; }
        @Override public int priority() { return 0; }
        @Override public boolean available() { return true; }
        @Override public CompiledSearch compile(byte[] needle) {
            compiles++;
            if (factoryFailure != null) throw factoryFailure;
            return supplied;
        }
    }

    private static final class OwnedPattern implements CompiledSearch {
        private int closes;
        private RuntimeException closeFailure;
        @Override public int indexOf(byte[] values, int fromIndex) { return -1; }
        @Override public int needleLength() { return 1; }
        @Override public String strategy() { return "test"; }
        @Override public void close() {
            closes++;
            if (closeFailure != null) throw closeFailure;
        }
    }

    private static final class Monitor implements M3ProgressMonitor {
        private final IntConsumer script;
        private int polls;
        private Monitor(IntConsumer script) { this.script = script; }
        @Override public void checkCanceled() { script.accept(++polls); }
        @Override public void beginTask(String name, long total) {}
        @Override public void done() {}
        @Override public void worked(long amount) {}
        @Override public void setProgress(long done, long total) {}
        @Override public void setTaskName(String name) {}
        @Override public void subTask(String name) {}
        @Override public boolean isCanceled() { return false; }
        @Override public void setCanceled(boolean canceled) {}
        @Override public M3ProgressState state() { return new M3DefaultProgressMonitor().state(); }
    }
}
