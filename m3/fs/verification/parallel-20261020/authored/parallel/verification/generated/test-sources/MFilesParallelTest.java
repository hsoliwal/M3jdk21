/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.m3.fs.vfs.FileName;
import com.m3.fs.vfs.FileObject;
import com.m3.fs.vfs.FileSystem;
import com.m3.fs.vfs.FileSystemException;
import com.m3.fs.vfs.FileSystemOptions;
import com.m3.fs.vfs.provider.AbstractFileName;
import com.m3.fs.vfs.provider.local.DefaultLocalFileProvider;
import com.m3.fs.vfs.provider.local.LocalFile;
import com.m3.fs.vfs.provider.local.LocalFileName;
import com.m3.fs.vfs.provider.local.LocalFileSystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/** Real filesystem/provider tests. Reflection makes the absent additive API a baseline assertion. */
@Timeout(20)
class MFilesParallelTest {
    @TempDir Path temp;

    @FunctionalInterface private interface Task { Object apply(MFile file) throws IOException; }

    // Eager construction would invoke the root iterable or provider factory before demand.
    @Test void constructionAndUnusedCloseHaveNoWorkEffects() throws Exception {
        AtomicInteger iterations = new AtomicInteger();
        Iterable<MFiles.Plan> roots = () -> { iterations.incrementAndGet(); return List.<MFiles.Plan>of().iterator(); };
        try (ExecutorService workers = pool(2); Stream<?> results = parallel(roots, workers, 2, MFile::name)) {
            assertFalse(results.isParallel());
            assertEquals(0, iterations.get());
        }
        assertEquals(0, iterations.get());
    }

    // Invalid arguments must fail before iteration, executor submission or provider resolution.
    @Test void validatesArgumentsWithoutStartingWork() throws Exception {
        AtomicInteger iterations = new AtomicInteger();
        Iterable<MFiles.Plan> roots = () -> { iterations.incrementAndGet(); throw new AssertionError("iterated"); };
        try (ExecutorService workers = pool(1)) {
            assertThrows(NullPointerException.class, () -> parallel(null, workers, 1, MFile::name));
            assertThrows(NullPointerException.class, () -> parallel(roots, null, 1, MFile::name));
            assertThrows(IllegalArgumentException.class, () -> parallel(roots, workers, 0, MFile::name));
            assertThrows(NullPointerException.class, () -> parallel(roots, workers, 1, null));
            assertEquals(0, iterations.get());
        }
    }

    // A null result is a valid mapped value; a queue sentinel must not discard it.
    @Test void emptyRootsAndNullResultsAreSupported() throws Exception {
        Path file = file("one.txt");
        try (ExecutorService workers = pool(2)) {
            try (Stream<?> empty = parallel(List.of(), workers, 2, MFile::name)) {
                assertEquals(List.of(), empty.toList());
            }
            try (Stream<?> values = convenience(MFiles.from(file), workers, 2, f -> null)) {
                assertEquals(Arrays.asList((Object) null), values.toList());
            }
            assertEquals(7, workers.submit(() -> 7).get());
        }
    }

    // Omitting the parent path/depth when forking changes filtering and relative names.
    @Test void localSubtreesPreserveSelectionNamesDepthAndPruning() throws Exception {
        Path root = tree();
        MFiles.Plan plan = MFiles.from(root).extensions("txt").filter(MFile::isFile)
                .descendInto(f -> !f.name().equals("skip")).maxDepth(3).breadthFirst();
        Set<String> expected;
        try (Stream<MFile> sequential = plan.stream()) {
            expected = Set.copyOf(sequential.map(MFile::relativeName).toList());
        }
        try (ExecutorService workers = pool(8); Stream<?> values = convenience(plan, workers, 3, MFile::relativeName)) {
            List<?> actual = values.toList();
            assertEquals(expected.size(), actual.size());
            assertEquals(expected, Set.copyOf(actual));
        }
    }

    // Serializing an entire local root fails the three simultaneous callbacks witness.
    @Test void oneLocalDirectoryUsesSeveralBoundedWorkers() throws Exception {
        Path root = Files.createDirectory(temp.resolve("many"));
        for (int i = 0; i < 12; i++) Files.writeString(root.resolve(i + ".txt"), "data");
        CountDownLatch entered = new CountDownLatch(3), release = new CountDownLatch(1);
        AtomicInteger active = new AtomicInteger(), maximum = new AtomicInteger();
        try (ExecutorService workers = pool(8); ExecutorService consumer = pool(1);
                Stream<?> values = convenience(MFiles.from(root), workers, 3, f -> {
                    int now = active.incrementAndGet(); maximum.accumulateAndGet(now, Math::max);
                    entered.countDown();
                    try { await(release); return f.relativeName(); }
                    finally { active.decrementAndGet(); }
                })) {
            Future<List<?>> done = consumer.submit(values::toList);
            try { assertTrue(entered.await(5, TimeUnit.SECONDS)); assertEquals(3, maximum.get()); }
            finally { release.countDown(); }
            assertEquals(12, done.get(5, TimeUnit.SECONDS).size());
            assertEquals(3, maximum.get());
            System.out.println("M3FILES_PARALLEL_RESOURCE {\"case\":\"local_files\",\"executor_threads\":8,\"limit\":3,\"entries\":12,\"peak_active_callbacks\":" + maximum.get() + "}");
        } finally { release.countDown(); }
    }

    // Awaiting child futures from the parent would deadlock a one-thread borrowed executor.
    @Test void singleThreadExecutorDoesNotWaitForNestedChildren() throws Exception {
        try (ExecutorService workers = pool(1); Stream<?> values = convenience(MFiles.from(tree()), workers, 4, MFile::relativeName)) {
            assertEquals(8, values.toList().size());
        }
    }

    // Waiting in submission order hides a completed second root behind the first root.
    @Test void resultsUseCompletionOrder() throws Exception {
        Path slow = file("slow.txt"), fast = file("fast.txt");
        CountDownLatch slowEntered = new CountDownLatch(1), release = new CountDownLatch(1);
        try (ExecutorService workers = pool(2); Stream<?> values = parallel(
                List.of(MFiles.from(slow), MFiles.from(fast)), workers, 2, f -> {
                    if (f.name().equals("slow.txt")) { slowEntered.countDown(); await(release); }
                    else await(slowEntered);
                    return f.name();
                })) {
            Iterator<?> it = values.iterator();
            try { assertEquals("fast.txt", it.next()); }
            finally { release.countDown(); }
            assertEquals("slow.txt", it.next());
            assertFalse(it.hasNext());
        } finally { release.countDown(); }
    }

    // Root admission may not materialize an unbounded iterable while workers are occupied.
    @Test void rootsAreAdmittedOnlyUpToTheGlobalBound() throws Exception {
        Path file = file("root.txt");
        AtomicInteger admitted = new AtomicInteger();
        Iterable<MFiles.Plan> roots = () -> new Iterator<>() {
            @Override public boolean hasNext() { return admitted.get() < 100; }
            @Override public MFiles.Plan next() { admitted.incrementAndGet(); return MFiles.from(file); }
        };
        CountDownLatch entered = new CountDownLatch(2), release = new CountDownLatch(1);
        try (ExecutorService workers = pool(4); ExecutorService consumer = pool(1);
                Stream<?> values = parallel(roots, workers, 2, f -> { entered.countDown(); await(release); return f.name(); })) {
            Future<?> first = consumer.submit(() -> values.iterator().next());
            try { assertTrue(entered.await(5, TimeUnit.SECONDS)); assertEquals(2, admitted.get()); }
            finally { release.countDown(); }
            first.get(5, TimeUnit.SECONDS);
            closeStream(values);
            assertEquals(2, admitted.get());
        } finally { release.countDown(); }
    }

    // A provider session and its objects must stay on the thread that initializes it.
    @Test void customProviderRegionHasOneOwnerAndIndependentRootsOverlap() throws Exception {
        Path a = Files.createDirectory(temp.resolve("a")), b = Files.createDirectory(temp.resolve("b"));
        Files.writeString(a.resolve("a.txt"), "a"); Files.writeString(b.resolve("b.txt"), "b");
        List<ObservedProvider> providers = new CopyOnWriteArrayList<>();
        Supplier<ObservedProvider> factory = () -> { var p = new ObservedProvider(); providers.add(p); return p; };
        CountDownLatch entered = new CountDownLatch(2), release = new CountDownLatch(1);
        try (ExecutorService workers = pool(4); ExecutorService consumer = pool(1); Stream<?> values = parallel(
                List.of(observed(a, factory), observed(b, factory)), workers, 4, f -> {
                    entered.countDown(); await(release);
                    try (InputStream input = f.openInputStream()) { return input.readAllBytes().length; }
                })) {
            Future<List<?>> done = consumer.submit(values::toList);
            try { assertTrue(entered.await(5, TimeUnit.SECONDS)); assertEquals(2, providers.size()); }
            finally { release.countDown(); }
            assertEquals(List.of(1, 1), done.get(5, TimeUnit.SECONDS));
            assertTrue(providers.stream().allMatch(p -> p.closed && p.opens == 1 && p.closes == 1));
        } finally { release.countDown(); }
    }

    // Archive entry callbacks must not independently reopen and rescan the same provider layer.
    @Test void automaticArchiveRegionRemainsOnOneWorker() throws Exception {
        Path archive = temp.resolve("pack.zip");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(archive))) {
            for (String name : List.of("a.txt", "dir/b.txt", "dir/c.txt")) {
                out.putNextEntry(new ZipEntry(name)); out.write(name.getBytes(StandardCharsets.UTF_8)); out.closeEntry();
            }
        }
        Set<Thread> archiveOwners = ConcurrentHashMap.newKeySet();
        MFiles.Plan plan = MFiles.from(temp).enterArchives(f -> f.name().endsWith(".zip"), 1)
                .filter(f -> f.isFile() && f.name().endsWith(".txt"));
        try (ExecutorService workers = pool(8); Stream<?> values = convenience(plan, workers, 4, f -> {
            archiveOwners.add(Thread.currentThread());
            try (InputStream in = f.openInputStream()) { return new String(in.readAllBytes(), StandardCharsets.UTF_8); }
        })) {
            assertEquals(Set.of("a.txt", "dir/b.txt", "dir/c.txt"), Set.copyOf(values.toList()));
            assertEquals(1, archiveOwners.size());
        }
    }

    // Finishing a Future by cancellation is not proof that its provider cleanup has finished.
    @Test void closeWaitsForActualWorkerExitAfterCancellation() throws Exception {
        CountDownLatch entered = new CountDownLatch(1), interrupted = new CountDownLatch(1), release = new CountDownLatch(1);
        AtomicBoolean exited = new AtomicBoolean();
        try (ExecutorService workers = pool(1); ExecutorService consumer = pool(1); ExecutorService closer = pool(1);
                Stream<?> values = convenience(MFiles.from(file("one.txt")), workers, 1, f -> {
                    entered.countDown();
                    try {
                        try { release.await(); } catch (InterruptedException expected) { interrupted.countDown(); }
                        boolean again = false;
                        while (release.getCount() != 0) {
                            try { release.await(); } catch (InterruptedException expected) { again = true; }
                        }
                        if (again) Thread.currentThread().interrupt();
                        return f.name();
                    } finally { exited.set(true); }
                })) {
            Future<?> consuming = consumer.submit(() -> values.iterator().hasNext());
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            Future<?> closing = closer.submit(values::close);
            try {
                assertTrue(interrupted.await(5, TimeUnit.SECONDS));
                assertFalse(closing.isDone()); assertFalse(exited.get());
            } finally { release.countDown(); }
            closing.get(5, TimeUnit.SECONDS);
            assertTrue(exited.get());
            try { consuming.get(5, TimeUnit.SECONDS); } catch (java.util.concurrent.ExecutionException expected) {
                assertTrue(expected.getCause() instanceof CancellationException || expected.getCause() instanceof IllegalStateException);
            }
        } finally { release.countDown(); }
    }

    // A task's checked failure and provider cleanup failure must both remain observable.
    @Test void taskFailurePreservesCheckedCauseAndSuppressedCleanup() throws Exception {
        IOException body = new IOException("controlled task failure");
        IllegalStateException cleanup = new IllegalStateException("controlled provider cleanup");
        ObservedProvider provider = new ObservedProvider(); provider.closeFailure = cleanup;
        try (ExecutorService workers = pool(1); Stream<?> values = convenience(observed(file("one.txt"), () -> provider), workers, 1,
                f -> { throw body; })) {
            UncheckedIOException failure = assertThrows(UncheckedIOException.class, values::toList);
            assertSame(body, failure.getCause()); assertTrue(containsSuppressed(failure, cleanup));
            assertTrue(provider.closed);
        }
    }

    // Terminal exhaustion must wait for session close and report its failure instead of success.
    @Test void failedFinalManagerClosePreventsSuccessfulExhaustion() throws Exception {
        ObservedProvider provider = new ObservedProvider();
        IllegalStateException failure = new IllegalStateException("manager close failed"); provider.closeFailure = failure;
        try (ExecutorService workers = pool(1); Stream<?> values = convenience(observed(file("one.txt"), () -> provider), workers, 1, MFile::name)) {
            assertSame(failure, assertThrows(IllegalStateException.class, values::toList));
            assertTrue(provider.closed);
        }
    }

    // Even an unclosed raw reader from a generic callback must close before its result is exposed.
    @Test void callbackContentCleanupCompletesBeforePublication() throws Exception {
        ObservedProvider provider = new ObservedProvider();
        AtomicReference<InputStream> leaked = new AtomicReference<>();
        try (ExecutorService workers = pool(1); Stream<?> values = convenience(observed(file("one.txt"), () -> provider), workers, 1, f -> {
            InputStream in = f.openInputStream(); leaked.set(in); assertTrue(in.read() >= 0); return f.name();
        })) {
            assertEquals("one.txt", values.iterator().next());
            assertEquals(1, provider.closes);
            assertThrows(IOException.class, () -> leaked.get().read());
        }
    }

    // Rejected work must never start provider I/O or shut down the borrowed executor.
    @Test void submissionRejectionIsPropagatedWithoutProviderWork() throws Exception {
        AtomicInteger factories = new AtomicInteger();
        try (ExecutorService workers = pool(1)) {
            workers.shutdown();
            try (Stream<?> values = convenience(observed(file("one.txt"), () -> { factories.incrementAndGet(); return new ObservedProvider(); }),
                    workers, 2, MFile::name)) {
                assertThrows(RejectedExecutionException.class, values::toList);
                assertEquals(0, factories.get());
            }
        }
    }

    // A caller-runs executor can block the only consumer before it can drain bounded results.
    @Test void inlineExecutorIsRefusedWithoutDeadlock() throws Exception {
        try (ExecutorService workers = new InlineExecutor(); Stream<?> values = convenience(MFiles.from(file("one.txt")), workers, 1, MFile::name)) {
            assertThrows(RejectedExecutionException.class, values::toList);
            assertFalse(workers.isShutdown());
        }
    }

    // Completion hashes/captures must come from one primary read followed by clean close.
    @Test void oneReadComposesProgressShadowAndCapture() throws Exception {
        Path root = Files.createDirectory(temp.resolve("capture"));
        for (int i = 0; i < 4; i++) Files.writeString(root.resolve(i + ".txt"), "word " + i);
        ObservedProvider provider = new ObservedProvider(); AtomicLong parentBytes = new AtomicLong();
        List<MFileCapture> captures = new CopyOnWriteArrayList<>();
        try (ExecutorService workers = pool(4); Stream<?> values = convenience(observed(root, () -> provider), workers, 4, f -> {
            ByteArrayOutputStream shadowBytes = new ByteArrayOutputStream();
            AtomicReference<MFileCapture> capture = new AtomicReference<>(); AtomicInteger completed = new AtomicInteger();
            MFileShadow shadow = new MFileShadow() {
                @Override public void onBytes(byte[] bytes, int offset, int length) { shadowBytes.write(bytes, offset, length); }
                @Override public void onComplete(MFileStreams.Completion evidence) {
                    assertEquals(MFileStreams.CompletionKind.INPUT_EOF, evidence.kind()); completed.incrementAndGet();
                }
            };
            M3FsProgress child = M3FsProgress.of(() -> Thread.currentThread().isInterrupted(), parentBytes::addAndGet);
            byte[] bytes;
            try (InputStream input = f.progressify(child).shadow(shadow).captureReads(capture::set).openInputStream()) {
                bytes = input.readAllBytes(); assertNull(capture.get()); assertEquals(0, completed.get());
            }
            assertArrayEquals(bytes, shadowBytes.toByteArray()); assertEquals(1, completed.get());
            assertNotNull(capture.get()); assertEquals(M3ContentId.digest(bytes), capture.get().manifest().raw().id());
            captures.add(capture.get()); return bytes.length;
        })) {
            assertEquals(List.of(6, 6, 6, 6), values.toList());
            assertEquals(4, provider.opens); assertEquals(4, provider.closes); assertEquals(24, parentBytes.get());
            assertEquals(4, captures.size()); assertTrue(provider.closed);
        }
    }

    // Reusing one live provider instance across sessions must fail before it gains a second owner.
    @Test void simultaneousProviderInstanceReuseIsRefused() throws Exception {
        ObservedProvider reused = new ObservedProvider(); CountDownLatch firstEntered = new CountDownLatch(1);
        Path first = file("a.txt"), second = file("b.txt"); AtomicInteger calls = new AtomicInteger();
        Supplier<ObservedProvider> factory = () -> {
            if (calls.incrementAndGet() > 1) {
                try { assertTrue(firstEntered.await(5, TimeUnit.SECONDS)); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new CancellationException(); }
            }
            return reused;
        };
        try (ExecutorService workers = pool(2); Stream<?> values = parallel(List.of(observed(first, factory), observed(second, factory)), workers, 2, f -> {
            firstEntered.countDown();
            try { new CountDownLatch(1).await(); }
            catch (InterruptedException expected) { Thread.currentThread().interrupt(); }
            return f.name();
        })) {
            assertThrows(UncheckedIOException.class, values::toList);
            assertEquals(1, reused.initializations); assertTrue(reused.closed);
        }
    }

    // Holding one delivered value permits only K further callbacks/results, not an unbounded queue.
    @Test void unconsumedResultsApplyGlobalBackpressure() throws Exception {
        Path root = Files.createDirectory(temp.resolve("credits"));
        for (int i = 0; i < 30; i++) Files.writeString(root.resolve(i + ".txt"), "x");
        CountDownLatch firstDelivered = new CountDownLatch(1), fourth = new CountDownLatch(1);
        CountDownLatch fifth = new CountDownLatch(1), releaseConsumer = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger(), delivered = new AtomicInteger();
        ObservedProvider provider = new ObservedProvider();
        try (ExecutorService workers = pool(4); ExecutorService consumer = pool(1);
                Stream<?> values = convenience(observed(root, () -> provider), workers, 3, f -> {
                    int index = calls.incrementAndGet();
                    if (index == 2) await(firstDelivered);
                    if (index == 4) fourth.countDown();
                    if (index == 5) fifth.countDown();
                    return f.name();
                })) {
            Future<?> all = consumer.submit(() -> values.forEach(value -> {
                if (delivered.incrementAndGet() == 1) {
                    firstDelivered.countDown(); awaitUninterruptibly(releaseConsumer);
                }
            }));
            try {
                assertTrue(fourth.await(5, TimeUnit.SECONDS));
                assertFalse(fifth.await(150, TimeUnit.MILLISECONDS));
                assertEquals(4, calls.get());
                System.out.println("M3FILES_PARALLEL_RESOURCE {\"case\":\"paused_consumer\",\"limit\":3,\"entries\":30,\"delivered\":1,\"callbacks_started\":" + calls.get() + ",\"observation_window_ms\":150}");
            } finally { releaseConsumer.countDown(); }
            all.get(5, TimeUnit.SECONDS); assertEquals(30, calls.get()); assertEquals(30, delivered.get());
        } finally { releaseConsumer.countDown(); }
    }

    // Both directory branches must make progress while their file callbacks are blocked.
    @Test void independentLocalDirectoryBranchesOverlap() throws Exception {
        Path root = Files.createDirectory(temp.resolve("branches"));
        for (String branch : List.of("left", "right")) {
            Files.createDirectory(root.resolve(branch)); Files.writeString(root.resolve(branch).resolve("one.txt"), branch);
        }
        CountDownLatch entered = new CountDownLatch(2), release = new CountDownLatch(1);
        Set<String> paths = ConcurrentHashMap.newKeySet();
        try (ExecutorService workers = pool(6); ExecutorService consumer = pool(1); Stream<?> values = convenience(
                MFiles.from(root).filter(MFile::isFile), workers, 3, f -> {
                    paths.add(f.relativeName()); entered.countDown(); await(release); return f.relativeName();
                })) {
            Future<List<?>> all = consumer.submit(values::toList);
            try { assertTrue(entered.await(5, TimeUnit.SECONDS)); assertEquals(Set.of("left/one.txt", "right/one.txt"), paths); }
            finally { release.countDown(); }
            assertEquals(2, all.get(5, TimeUnit.SECONDS).size());
            System.out.println("M3FILES_PARALLEL_RESOURCE {\"case\":\"directory_branches\",\"executor_threads\":6,\"limit\":3,\"overlapping_branches\":" + paths.size() + "}");
        } finally { release.countDown(); }
    }

    // Factory state can be non-thread-safe even though the returned providers are independent.
    @Test void providerFactoriesAreSerializedWithinTheOperation() throws Exception {
        CountDownLatch firstFactory = new CountDownLatch(1), release = new CountDownLatch(1);
        AtomicInteger inside = new AtomicInteger(), maximum = new AtomicInteger(), calls = new AtomicInteger();
        Supplier<ObservedProvider> factory = () -> {
            int active = inside.incrementAndGet(); maximum.accumulateAndGet(active, Math::max);
            try {
                if (calls.incrementAndGet() == 1) { firstFactory.countDown(); awaitUninterruptibly(release); }
                return new ObservedProvider();
            } finally { inside.decrementAndGet(); }
        };
        try (ExecutorService workers = pool(4); ExecutorService consumer = pool(1); Stream<?> values = parallel(
                List.of(observed(file("a.txt"), factory), observed(file("b.txt"), factory)), workers, 2, MFile::name)) {
            Future<List<?>> all = consumer.submit(values::toList);
            try {
                assertTrue(firstFactory.await(5, TimeUnit.SECONDS));
                assertEquals(1, calls.get());
            } finally { release.countDown(); }
            assertEquals(2, all.get(5, TimeUnit.SECONDS).size()); assertEquals(2, calls.get()); assertEquals(1, maximum.get());
        } finally { release.countDown(); }
    }

    // Failure must cancel other owners before waiting for the failing owner's provider close.
    @Test void callbackFailureCancelsOthersBeforeCleanupWait() throws Exception { failureCancelsBeforeCleanup(false); }
    @Test void filterFailureCancelsOthersBeforeCleanupWait() throws Exception { failureCancelsBeforeCleanup(true); }

    private void failureCancelsBeforeCleanup(boolean fromFilter) throws Exception {
        CountDownLatch slowEntered = new CountDownLatch(1), slowInterrupted = new CountDownLatch(1);
        CountDownLatch cleanupEntered = new CountDownLatch(1), releaseCleanup = new CountDownLatch(1);
        ObservedProvider bad = new ObservedProvider(), slow = new ObservedProvider();
        bad.beforeClose = () -> { cleanupEntered.countDown(); awaitUninterruptibly(releaseCleanup); };
        IllegalStateException original = new IllegalStateException("controlled body failure");
        MFiles.Plan badPlan = observed(file("bad.txt"), () -> bad);
        if (fromFilter) badPlan = badPlan.filter(f -> { awaitUninterruptibly(slowEntered); throw original; });
        try (ExecutorService workers = pool(2); ExecutorService consumer = pool(1); Stream<?> values = parallel(
                List.of(badPlan, observed(file("slow.txt"), () -> slow)), workers, 2, f -> {
                    if (f.name().equals("bad.txt")) { await(slowEntered); throw original; }
                    slowEntered.countDown();
                    try { new CountDownLatch(1).await(); }
                    catch (InterruptedException expected) { slowInterrupted.countDown(); Thread.currentThread().interrupt(); }
                    return f.name();
                })) {
            Future<?> all = consumer.submit(values::toList);
            try {
                assertTrue(cleanupEntered.await(5, TimeUnit.SECONDS));
                assertTrue(slowInterrupted.await(1, TimeUnit.SECONDS)); assertFalse(all.isDone());
            } finally { releaseCleanup.countDown(); }
            var thrown = assertThrows(java.util.concurrent.ExecutionException.class, () -> all.get(5, TimeUnit.SECONDS));
            assertSame(original, thrown.getCause()); assertTrue(bad.closed); assertTrue(slow.closed);
        } finally { releaseCleanup.countDown(); }
    }

    // A cancellation signal is expected, but a real close failure suppressed onto it is not disposable.
    @Test void cancellationRetainsProviderCleanupFailure() throws Exception {
        CountDownLatch entered = new CountDownLatch(1); ObservedProvider provider = new ObservedProvider();
        IllegalStateException cleanup = new IllegalStateException("cancel cleanup failed"); provider.closeFailure = cleanup;
        try (ExecutorService workers = pool(1); ExecutorService consumer = pool(1); Stream<?> values = convenience(
                observed(file("one.txt"), () -> provider), workers, 1, f -> {
                    entered.countDown(); await(new CountDownLatch(1)); return f.name();
                })) {
            Future<?> all = consumer.submit(values::toList); assertTrue(entered.await(5, TimeUnit.SECONDS));
            // The consuming thread may win delivery of the once-only failure; both paths must finish cleanup.
            Throwable closing = null;
            try { closeStream(values); } catch (RuntimeException failure) { closing = failure; }
            var consuming = assertThrows(java.util.concurrent.ExecutionException.class, () -> all.get(5, TimeUnit.SECONDS));
            assertTrue(closing == cleanup || consuming.getCause() == cleanup
                    || containsSuppressed(consuming.getCause(), cleanup));
            assertTrue(provider.closed);
        }
    }

    // hasNext(false) must not precede actual manager shutdown, even after all values were emitted.
    @Test void normalExhaustionWaitsForProviderCleanup() throws Exception {
        CountDownLatch cleanupEntered = new CountDownLatch(1), releaseCleanup = new CountDownLatch(1);
        ObservedProvider provider = new ObservedProvider();
        provider.beforeClose = () -> { cleanupEntered.countDown(); awaitUninterruptibly(releaseCleanup); };
        try (ExecutorService workers = pool(1); ExecutorService consumer = pool(1); Stream<?> values = convenience(
                observed(file("one.txt"), () -> provider), workers, 1, MFile::name)) {
            Future<List<?>> all = consumer.submit(values::toList);
            try { assertTrue(cleanupEntered.await(5, TimeUnit.SECONDS)); assertFalse(all.isDone()); }
            finally { releaseCleanup.countDown(); }
            assertEquals(List.of("one.txt"), all.get(5, TimeUnit.SECONDS)); assertTrue(provider.closed);
        } finally { releaseCleanup.countDown(); }
    }

    // A partially reserved root batch must not leave phantom workers or invoke providers on failure.
    @Test void rootIteratorAndNullPlanFailuresReleaseAdmissions() throws Exception {
        AtomicInteger factories = new AtomicInteger();
        MFiles.Plan first = observed(file("one.txt"), () -> { factories.incrementAndGet(); return new ObservedProvider(); });
        IllegalStateException iteratorFailure = new IllegalStateException("iterator failed");
        Iterable<MFiles.Plan> broken = () -> new Iterator<>() {
            private boolean emitted;
            @Override public boolean hasNext() { if (emitted) throw iteratorFailure; return true; }
            @Override public MFiles.Plan next() { emitted = true; return first; }
        };
        try (ExecutorService workers = pool(2)) {
            try (Stream<?> values = parallel(broken, workers, 2, MFile::name)) {
                assertSame(iteratorFailure, assertThrows(IllegalStateException.class, values::toList));
            }
            try (Stream<?> values = parallel(Arrays.asList(first, null), workers, 2, MFile::name)) {
                assertThrows(NullPointerException.class, values::toList);
            }
            assertEquals(0, factories.get()); assertFalse(workers.isShutdown());
        }
    }

    // A task error must not disappear inside FutureTask or be replaced by a generic wrapper.
    @Test void runtimeAndErrorIdentitySurviveWorkerBoundary() throws Exception {
        Path path = file("one.txt");
        for (Throwable original : List.of(new IllegalArgumentException("runtime"), new AssertionError("error"))) {
            try (ExecutorService workers = pool(1); Stream<?> values = convenience(MFiles.from(path), workers, 1, f -> {
                if (original instanceof RuntimeException runtime) throw runtime;
                throw (Error) original;
            })) {
                assertSame(original, assertThrows(Throwable.class, values::toList));
            }
        }
    }

    // Consumer interruption cancels work, waits for provider cleanup, and restores interrupt status.
    @Test void interruptedConsumerWaitsCleanupAndPreservesInterrupt() throws Exception {
        CountDownLatch entered = new CountDownLatch(1); ObservedProvider provider = new ObservedProvider();
        AtomicReference<Throwable> failure = new AtomicReference<>(); AtomicBoolean interrupted = new AtomicBoolean();
        try (ExecutorService workers = pool(1); Stream<?> values = convenience(observed(file("one.txt"), () -> provider), workers, 1, f -> {
            entered.countDown(); await(new CountDownLatch(1)); return f.name();
        })) {
            Thread consumer = new Thread(() -> {
                try { values.toList(); } catch (RuntimeException problem) { failure.set(problem); }
                finally { interrupted.set(Thread.currentThread().isInterrupted()); }
            });
            consumer.setDaemon(true); consumer.start(); assertTrue(entered.await(5, TimeUnit.SECONDS));
            consumer.interrupt(); consumer.join(5_000);
            assertFalse(consumer.isAlive()); assertInstanceOf(CancellationException.class, failure.get());
            assertTrue(interrupted.get()); assertTrue(provider.closed);
        }
    }

    // Waiting for oneself during close must fail explicitly rather than deadlock the whole operation.
    @Test void taskCannotCloseItsOwnResultStream() throws Exception {
        AtomicReference<Stream<?>> current = new AtomicReference<>();
        try (ExecutorService workers = pool(1); Stream<?> values = convenience(MFiles.from(file("one.txt")), workers, 1, f -> {
            closeStream(current.get()); return f.name();
        })) {
            current.set(values); assertThrows(IllegalStateException.class, values::toList);
        }
    }

    // Rejection after an earlier submission started must also wait for that owner's cleanup.
    @Test void laterSubmissionRejectionCancelsAndClosesEarlierWork() throws Exception {
        CountDownLatch entered = new CountDownLatch(1); ObservedProvider first = new ObservedProvider();
        RejectedExecutionException rejection = new RejectedExecutionException("controlled second submission rejection");
        try (ExecutorService workers = new RejectAfterOneExecutor(entered, rejection); Stream<?> values = parallel(
                List.of(observed(file("first.txt"), () -> first), MFiles.from(file("second.txt"))), workers, 2, f -> {
                    entered.countDown(); await(new CountDownLatch(1)); return f.name();
                })) {
            assertSame(rejection, assertThrows(RejectedExecutionException.class, values::toList));
            assertTrue(first.closed); assertFalse(workers.isShutdown());
        }
    }

    // A downstream action failure owns cancellation just as a provider/task failure does.
    @Test void downstreamFailureWaitsForOtherWorkerCleanup() throws Exception {
        CountDownLatch slowEntered = new CountDownLatch(1);
        ObservedProvider fast = new ObservedProvider(), slow = new ObservedProvider();
        IllegalStateException downstream = new IllegalStateException("controlled downstream failure");
        try (ExecutorService workers = pool(2); Stream<?> values = parallel(
                List.of(observed(file("fast.txt"), () -> fast), observed(file("slow.txt"), () -> slow)), workers, 2, f -> {
                    if (f.name().equals("fast.txt")) await(slowEntered);
                    else { slowEntered.countDown(); await(new CountDownLatch(1)); }
                    return f.name();
                })) {
            assertSame(downstream, assertThrows(IllegalStateException.class, () -> values.forEach(value -> { throw downstream; })));
            assertTrue(fast.closed); assertTrue(slow.closed);
        }
    }

    // Causal relation is not identity: a distinct cleanup error still carries evidence.
    @Test void relatedCleanupFailureIsNotDiscardedAsDuplicate() throws Exception {
        CountDownLatch slowEntered = new CountDownLatch(1);
        IllegalStateException original = new IllegalStateException("controlled task failure");
        IllegalStateException cleanup = new IllegalStateException("cleanup after that failure", original);
        ObservedProvider bad = new ObservedProvider(), slow = new ObservedProvider(); slow.closeFailure = cleanup;
        try (ExecutorService workers = pool(2); Stream<?> values = parallel(
                List.of(observed(file("bad.txt"), () -> bad), observed(file("slow.txt"), () -> slow)), workers, 2, f -> {
                    if (f.name().equals("bad.txt")) { await(slowEntered); throw original; }
                    slowEntered.countDown(); await(new CountDownLatch(1)); return f.name();
                })) {
            assertSame(original, assertThrows(IllegalStateException.class, values::toList));
            assertTrue(containsSuppressed(original, cleanup)); assertTrue(bad.closed); assertTrue(slow.closed);
        }
    }

    private Path file(String name) throws IOException { return Files.writeString(temp.resolve(name), "content"); }
    private Path tree() throws IOException {
        Path root = Files.createDirectory(temp.resolve("tree"));
        Files.createDirectories(root.resolve("keep/deep")); Files.createDirectories(root.resolve("skip/deep"));
        Files.writeString(root.resolve("top.txt"), "top"); Files.writeString(root.resolve("keep/a.txt"), "a");
        Files.writeString(root.resolve("keep/deep/b.txt"), "b"); Files.writeString(root.resolve("skip/deep/c.txt"), "c");
        return root;
    }
    private static ExecutorService pool(int threads) {
        return Executors.newFixedThreadPool(threads, runnable -> { Thread t = new Thread(runnable); t.setDaemon(true); return t; });
    }
    private static void closeStream(Stream<?> stream) { stream.close(); }
    private static void await(CountDownLatch latch) throws IOException {
        try { if (!latch.await(8, TimeUnit.SECONDS)) throw new IOException("fixture latch timed out"); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new CancellationException("fixture interrupted"); }
    }
    private static void awaitUninterruptibly(CountDownLatch latch) {
        boolean interrupted = false;
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
        try {
            while (latch.getCount() != 0) {
                long remaining = deadline - System.nanoTime();
                assertTrue(remaining > 0, "fixture latch timed out");
                try { assertTrue(latch.await(remaining, TimeUnit.NANOSECONDS), "fixture latch timed out"); }
                catch (InterruptedException expected) { interrupted = true; }
            }
        } finally { if (interrupted) Thread.currentThread().interrupt(); }
    }
    private static MFiles.Plan observed(Path path, Supplier<ObservedProvider> factory) {
        return MFiles.from("observed:" + path.toAbsolutePath()).withProvider("observed", factory);
    }
    private static Class<?> taskType() {
        try { return Class.forName("com.m3.fs.MFiles$FileTask"); }
        catch (ClassNotFoundException missing) { throw new AssertionError("Missing bounded MFiles.FileTask API", missing); }
    }
    private static Object callback(Task task) {
        if (task == null) return null;
        Class<?> type = taskType();
        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            if (method.getName().equals("apply")) return task.apply((MFile) args[0]);
            if (method.getName().equals("toString")) return "TestFileTask";
            if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
            if (method.getName().equals("equals")) return proxy == args[0];
            throw new AssertionError(method);
        });
    }
    private static Stream<?> parallel(Iterable<? extends MFiles.Plan> roots, ExecutorService workers, int limit, Task task) throws Exception {
        try { return (Stream<?>) MFiles.class.getMethod("mapParallel", Iterable.class, ExecutorService.class, int.class, taskType())
                .invoke(null, roots, workers, limit, callback(task)); }
        catch (InvocationTargetException wrapped) { return rethrow(wrapped.getCause()); }
    }
    private static Stream<?> convenience(MFiles.Plan plan, ExecutorService workers, int limit, Task task) throws Exception {
        try { return (Stream<?>) MFiles.Plan.class.getMethod("mapParallel", ExecutorService.class, int.class, taskType())
                .invoke(plan, workers, limit, callback(task)); }
        catch (InvocationTargetException wrapped) { return rethrow(wrapped.getCause()); }
    }
    private static Stream<?> rethrow(Throwable failure) throws Exception {
        if (failure instanceof Exception exception) throw exception;
        if (failure instanceof Error error) throw error;
        throw new AssertionError(failure);
    }
    private static boolean containsSuppressed(Throwable actual, Throwable expected) {
        for (Throwable p = actual; p != null; p = p.getCause()) {
            for (Throwable suppressed : p.getSuppressed()) if (suppressed == expected || containsSuppressed(suppressed, expected)) return true;
        }
        return false;
    }

    private static final class InlineExecutor extends AbstractExecutorService {
        private boolean shutdown;
        @Override public void execute(Runnable command) { if (shutdown) throw new RejectedExecutionException(); command.run(); }
        @Override public void shutdown() { shutdown = true; }
        @Override public List<Runnable> shutdownNow() { shutdown = true; return List.of(); }
        @Override public boolean isShutdown() { return shutdown; }
        @Override public boolean isTerminated() { return shutdown; }
        @Override public boolean awaitTermination(long time, TimeUnit unit) { return shutdown; }
    }

    private static final class RejectAfterOneExecutor extends AbstractExecutorService {
        private final ExecutorService delegate = pool(1);
        private final CountDownLatch firstEntered;
        private final RejectedExecutionException rejection;
        private int submissions;
        RejectAfterOneExecutor(CountDownLatch firstEntered, RejectedExecutionException rejection) {
            this.firstEntered = firstEntered; this.rejection = rejection;
        }
        @Override public void execute(Runnable command) {
            if (++submissions == 1) delegate.execute(command);
            else { awaitUninterruptibly(firstEntered); throw rejection; }
        }
        @Override public void shutdown() { delegate.shutdown(); }
        @Override public List<Runnable> shutdownNow() { return delegate.shutdownNow(); }
        @Override public boolean isShutdown() { return delegate.isShutdown(); }
        @Override public boolean isTerminated() { return delegate.isTerminated(); }
        @Override public boolean awaitTermination(long time, TimeUnit unit) throws InterruptedException {
            return delegate.awaitTermination(time, unit);
        }
    }

    /** Real local I/O with strict single-thread provider ownership assertions. */
    private static final class ObservedProvider extends DefaultLocalFileProvider {
        private Thread owner;
        private volatile boolean closed;
        private volatile int opens, closes, initializations;
        private RuntimeException closeFailure;
        private Runnable beforeClose;
        private void checkOwner() { assertSame(owner, Thread.currentThread(), "provider crossed owner threads"); }
        @Override public void init() throws FileSystemException { owner = Thread.currentThread(); initializations++; super.init(); }
        @Override protected FileSystem doCreateFileSystem(FileName name, FileSystemOptions options) {
            checkOwner(); LocalFileName local = (LocalFileName) name;
            return new LocalFileSystem(local, local.getRootFile(), options) {
                @Override protected FileObject createFile(AbstractFileName fileName) {
                    checkOwner();
                    return new LocalFile(this, local.getRootFile(), fileName) {
                        @Override protected String[] doListChildren() throws Exception {
                            checkOwner(); String[] names = super.doListChildren(); Arrays.sort(names); return names;
                        }
                        @Override protected InputStream doGetInputStream(int buffer) throws Exception {
                            checkOwner(); opens++;
                            return new FilterInputStream(super.doGetInputStream(buffer)) {
                                private boolean ended;
                                @Override public int read(byte[] bytes, int offset, int length) throws IOException {
                                    checkOwner(); return super.read(bytes, offset, length);
                                }
                                @Override public int read() throws IOException { checkOwner(); return super.read(); }
                                @Override public void close() throws IOException {
                                    checkOwner(); if (ended) return; ended = true; closes++; super.close();
                                }
                            };
                        }
                    };
                }
            };
        }
        @Override public void close() {
            checkOwner(); if (beforeClose != null) beforeClose.run();
            try { super.close(); } finally { closed = true; }
            if (closeFailure != null) throw closeFailure;
        }
    }
}
