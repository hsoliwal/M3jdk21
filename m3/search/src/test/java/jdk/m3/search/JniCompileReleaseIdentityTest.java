// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/JniCompileReleaseIdentityTest.java (blob 2b4422fba005) by M3_SEARCH_FORWARD v1.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jdk.m3.collections.M3DefaultProgressMonitor;
import jdk.m3.collections.M3ProgressMonitor;
import jdk.m3.collections.M3ProgressState;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Real-provider compatibility; adversarial JNI release-failure is in the separate native task. */
final class JniCompileReleaseIdentityTest {
    @Test
    void postCompilationCancellationRetainsExceptionAndASecondCompileSucceeds() {
        M3JniSearchEngine engine = nativeEngine();
        AtomicInteger checks = new AtomicInteger();
        CancellationException failure = new CancellationException("third native compile checkpoint");
        M3ProgressMonitor monitor = monitor(checks, () -> { throw failure; });
        assertSame(failure, assertThrows(CancellationException.class,
                () -> engine.compile(new byte[] {42}, monitor)));
        assertEquals(3, checks.get());
        try (M3CompiledSearch compiled = engine.compile(new byte[] {42})) {
            assertNotNull(compiled);
            assertEquals(1, compiled.indexOf(new byte[] {1, 42, 3}, 0));
        }
    }

    @Test
    void nativePatternFailureWithErrorKeepsThrowableIdentity() {
        M3JniSearchEngine engine = nativeEngine();
        AtomicInteger checks = new AtomicInteger();
        AssertionError failure = new AssertionError("third checkpoint error");
        assertSame(failure, assertThrows(AssertionError.class,
                () -> engine.compile(new byte[] {9}, monitor(checks, () -> { throw failure; }))));
        assertEquals(3, checks.get());
        try (M3CompiledSearch compiled = engine.compile(new byte[] {9})) {
            assertEquals(0, compiled.indexOf(new byte[] {9}, 0));
        }
    }

    private static M3JniSearchEngine nativeEngine() {
        String configured = System.getProperty("m3.native.library.path");
        assertNotNull(configured, "real compiled native library is required");
        Path library = Path.of(configured).toAbsolutePath().normalize();
        assertTrue(Files.isRegularFile(library));
        System.setProperty("m3.native.enabled", "true");
        System.setProperty("m3.native.library.path", library.toString());
        M3JniSearchEngine engine = new M3JniSearchEngine();
        assertTrue(engine.available(), "do not replace actual JNI with a Java fallback");
        return engine;
    }

    private static M3ProgressMonitor monitor(AtomicInteger checks, Runnable onThirdPoll) {
        return new M3ProgressMonitor() {
            @Override public void checkCanceled() {
                if (checks.incrementAndGet() == 3) onThirdPoll.run();
            }
            @Override public void beginTask(String name, long total) {}
            @Override public void done() {}
            @Override public void worked(long units) {}
            @Override public void setProgress(long worked, long total) {}
            @Override public void setTaskName(String name) {}
            @Override public void subTask(String name) {}
            @Override public boolean isCanceled() { return false; }
            @Override public void setCanceled(boolean canceled) {}
            @Override public M3ProgressState state() {
                return new M3DefaultProgressMonitor().state();
            }
        };
    }
}
