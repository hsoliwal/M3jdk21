// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

/**
 * Continuous input observation using the original M3FileDeltaLedger state.
 * Events request reconciliation; they are never treated as complete change facts.
 * The caller owns compilation, dependency analysis, outputs and diagnostics.
 * A build is acknowledged only through {@link #commit}, after caller-owned work
 * succeeds and the input generation has been checked again.
 *
 * <p>This initial adapter deliberately reconciles the complete admitted input
 * inventory after an event, including OVERFLOW, and at a bounded periodic interval.
 * It never passes a partial watcher event set to the ledger's complete-set scan.
 * File contents are hashed even when size and timestamp match. There is no second
 * persistent fingerprint store, parser, dependency graph or compiler here.
 *
 * <p>All operations except close are confined to the opening thread. Inputs and
 * exclusions are repository-relative (absolute paths under the root are also
 * accepted). Directory inputs are recursive. Exclude generated outputs from the
 * input inventory; the compiler's output ownership contract validates them.
 */
public final class M3FileWatcher implements AutoCloseable {
    private final Path root;
    private final List<Path> inputs;
    private final List<Path> exclusions;
    private final Predicate<Path> exclusionRule;
    private final Path image;
    private final Path lockPath;
    private final boolean includeTimestampChanges;
    private final long reconcileNanos;
    private final Thread owner = Thread.currentThread();
    private final WatchService watcher;
    private final FileChannel lockChannel;
    private final FileLock lock;
    private M3FileDeltaLedger.Snapshot committed;
    private M3FileDeltaLedger.Snapshot offered;
    private long lastScan;
    private boolean scanRequested = true;
    private boolean forceOffer;
    private volatile boolean closed;

    private M3FileWatcher(Path directory, Collection<Path> inputPaths,
            Collection<Path> excludedPaths, Predicate<Path> exclusionRule,
            Path stateImage, Duration reconciliationInterval,
            boolean includeTimestampChanges) throws IOException {
        root = Objects.requireNonNull(directory, "directory").toAbsolutePath().normalize();
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(root))
            throw new IOException("M3 watcher root must be a real directory");
        inputs = normalized(inputPaths);
        exclusions = normalized(excludedPaths);
        this.exclusionRule = Objects.requireNonNull(exclusionRule, "exclusionRule");
        image = inside(Objects.requireNonNull(stateImage, "stateImage"));
        lockPath = image.resolveSibling(image.getFileName() + ".lock");
        if (image.equals(root) || lockPath.equals(root)) throw new IOException("invalid state image");
        reconcileNanos = duration(reconciliationInterval, false);
        this.includeTimestampChanges = includeTimestampChanges;
        checkAncestors(image);
        Files.createDirectories(image.getParent());
        checkAncestors(lockPath);
        FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE,
                StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
        FileLock acquired = null;
        WatchService opened = null;
        try {
            try { acquired = channel.tryLock(); }
            catch (OverlappingFileLockException busy) { throw new IOException("M3 build state already watched", busy); }
            if (acquired == null) throw new IOException("M3 build state already watched");
            boolean imagePresent = true;
            try { Files.readAttributes(image, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS); }
            catch (NoSuchFileException absent) { imagePresent = false; }
            // A failure after the first observation must not be recast as absence.
            if (imagePresent) committed = M3FileDeltaLedger.load(image, root);
            opened = root.getFileSystem().newWatchService();
            // A root registration also discovers previously absent input directories.
            root.register(opened, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        } catch (Throwable failure) {
            if (opened != null) try { opened.close(); } catch (IOException close) { failure.addSuppressed(close); }
            if (acquired != null) try { acquired.release(); } catch (IOException close) { failure.addSuppressed(close); }
            try { channel.close(); } catch (IOException close) { failure.addSuppressed(close); }
            throw failure;
        }
        watcher = opened;
        lockChannel = channel;
        lock = acquired;
    }

    /** Open a listener and reuse the sealed last-successful ledger image, if present. */
    public static M3FileWatcher open(Path root, Collection<Path> inputs,
            Collection<Path> exclusions, Path stateImage, Duration reconciliationInterval,
            boolean includeTimestampChanges) throws IOException {
        return new M3FileWatcher(root, inputs, exclusions, path -> false, stateImage,
                reconciliationInterval, includeTimestampChanges);
    }

    /**
     * Open with an additional caller-owned output exclusion rule. The rule receives
     * normalized absolute paths and runs only on the opening thread. Its policy
     * must remain stable during an offered build and its commit verification.
     * This supports build systems with nested output directories without placing
     * their naming conventions in the shared file-state owner.
     */
    public static M3FileWatcher open(Path root, Collection<Path> inputs,
            Collection<Path> exclusions, Predicate<Path> exclusionRule,
            Path stateImage, Duration reconciliationInterval,
            boolean includeTimestampChanges) throws IOException {
        return new M3FileWatcher(root, inputs, exclusions, exclusionRule, stateImage,
                reconciliationInterval, includeTimestampChanges);
    }

    /**
     * Return one pending input generation, or empty after maxWait. An outstanding
     * offer is not repeated until the admitted input generation changes or the
     * caller requests a build explicitly. Failed work never advances the successful
     * baseline. Restoring that baseline after a failed offer still requests work:
     * the unsuccessful build may have removed or damaged its previous outputs.
     * Multiple notifications are coalesced before the complete reconciliation.
     */
    public Optional<M3FileDeltaLedger.Snapshot> awaitChange(Duration maxWait)
            throws IOException, InterruptedException {
        checkOwner();
        long budget = duration(maxWait, true);
        long started = System.nanoTime();
        while (true) {
            if (scanRequested || System.nanoTime() - lastScan >= reconcileNanos) {
                // Leave this true when inventory or hashing throws: no event is acknowledged.
                scanRequested = true;
                drain();
                var observed = M3FileDeltaLedger.scan(root, committed, inventory(), includeTimestampChanges);
                lastScan = System.nanoTime();
                scanRequested = false;
                boolean newGeneration = offered == null
                        ? committed == null || changed(observed)
                        : !sameInputs(offered, observed);
                if (forceOffer || newGeneration) {
                    forceOffer = false;
                    offered = observed;
                    return Optional.of(observed);
                }
            }
            long remaining = budget - (System.nanoTime() - started);
            if (remaining <= 0) return Optional.empty();
            long untilReconcile = reconcileNanos - (System.nanoTime() - lastScan);
            WatchKey key = watcher.poll(Math.min(remaining, Math.max(1L, untilReconcile)), TimeUnit.NANOSECONDS);
            if (key != null) {
                consume(key);
                drain();
                scanRequested = true;
            }
        }
    }

    /** Request another observation even without an event, including an explicit full build. */
    public void requestBuild() {
        checkOwner();
        scanRequested = true;
        forceOffer = true;
    }

    /**
     * Acknowledge successful compilation/output work for the exact offered generation.
     * Return false, keeping the previous successful state, if inputs changed during
     * that work. A failed or cancelled build must not call this method.
     * Observations do not make mutable filesystem reads atomic; callers requiring
     * stronger admission must compile an immutable input generation.
     */
    public boolean commit(M3FileDeltaLedger.Snapshot built) throws IOException {
        checkOwner();
        if (built == null || built != offered) throw new IllegalArgumentException("not the offered M3 generation");
        scanRequested = true;
        var after = M3FileDeltaLedger.scan(root, built, inventory(), includeTimestampChanges);
        if (changed(after)) {
            // This observed invalidation requires another build even if inputs
            // revert before the next scan: current outputs are no longer trusted.
            forceOffer = true;
            return false;
        }
        M3FileDeltaLedger.save(image, after);
        committed = after;
        offered = null;
        lastScan = System.nanoTime();
        // Events occurring during/after verification remain queued and will be reconciled.
        return true;
    }

    private List<Path> inventory() throws IOException {
        var files = new ArrayList<Path>();
        for (Path input : inputs) {
            checkAncestors(input);
            // Register existing ancestors too, so a not-yet-created nested input
            // directory becomes visible without waiting for the periodic scan.
            for (Path parent = input.getParent(); parent != null && parent.startsWith(root);
                    parent = parent.getParent()) {
                if (Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS))
                    parent.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            }
            if (excluded(input)) continue;
            BasicFileAttributes attributes;
            try { attributes = Files.readAttributes(input, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS); }
            catch (NoSuchFileException absent) { continue; }
            if (attributes.isRegularFile()) {
                input.getParent().register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
                files.add(input);
            } else if (attributes.isDirectory()) {
                Files.walkFileTree(input, new SimpleFileVisitor<Path>() {
                    @Override public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attrs)
                            throws IOException {
                        if (excluded(path)) return FileVisitResult.SKIP_SUBTREE;
                        path.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
                        return FileVisitResult.CONTINUE;
                    }
                    @Override public FileVisitResult visitFile(Path path, BasicFileAttributes attrs)
                            throws IOException {
                        if (!excluded(path)) {
                            if (!attrs.isRegularFile()) throw new IOException("non-regular M3 watched input: " + path);
                            files.add(path);
                            if (files.size() > 1_000_000) throw new IOException("M3 watcher input budget exceeded");
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } else throw new IOException("non-regular M3 watched input: " + input);
        }
        return files;
    }

    private boolean excluded(Path path) {
        if (path.equals(image) || path.equals(lockPath)
                || (Objects.equals(path.getParent(), image.getParent())
                    && path.getFileName().toString().startsWith(".m3file-delta-"))) return true;
        for (Path exclusion : exclusions) if (path.startsWith(exclusion)) return true;
        return exclusionRule.test(path);
    }

    private void checkAncestors(Path path) throws IOException {
        Path cursor = root;
        for (Path component : root.relativize(path)) {
            cursor = cursor.resolve(component);
            if (Files.isSymbolicLink(cursor)) throw new IOException("symlink in M3 watched path");
        }
    }

    private List<Path> normalized(Collection<Path> paths) {
        Objects.requireNonNull(paths, "paths");
        var result = new ArrayList<Path>();
        for (Path path : paths) {
            Path normalized = inside(Objects.requireNonNull(path, "path"));
            if (!result.contains(normalized)) result.add(normalized);
        }
        return List.copyOf(result);
    }

    private Path inside(Path path) {
        Path normalized = (path.isAbsolute() ? path : root.resolve(path)).toAbsolutePath().normalize();
        if (!normalized.startsWith(root)) throw new IllegalArgumentException("M3 watcher path escapes root");
        return normalized;
    }

    private void drain() {
        for (WatchKey key; (key = watcher.poll()) != null;) consume(key);
    }

    private static void consume(WatchKey key) {
        // Creation, removal, modification, overflow and an invalid key all request
        // the same complete reconciliation; no event payload is trusted as state.
        key.pollEvents();
        key.reset();
    }

    private boolean sameInputs(M3FileDeltaLedger.Snapshot left, M3FileDeltaLedger.Snapshot right) {
        if (!left.root().equals(right.root())) return false;
        int liveLeft = 0;
        for (int slot = 0; slot < left.slots(); slot++) {
            if (!left.liveAt(slot)) continue;
            liveLeft++;
            int other = right.physicalSlot(Path.of(left.pathAt(slot)));
            if (other < 0 || !right.liveAt(other) || left.kindAt(slot) != right.kindAt(other)
                    || left.sizeAt(slot) != right.sizeAt(other)
                    || !Arrays.equals(left.digestAt(slot), right.digestAt(other))) return false;
            if (includeTimestampChanges && (left.hasLastModifiedAt(slot) != right.hasLastModifiedAt(other)
                    || (left.hasLastModifiedAt(slot)
                        && !left.lastModifiedAt(slot).equals(right.lastModifiedAt(other))))) return false;
        }
        int liveRight = 0;
        for (int slot = 0; slot < right.slots(); slot++) if (right.liveAt(slot)) liveRight++;
        // Delta flags and tombstone slot positions describe a baseline comparison,
        // not the current live input identity. Use the snapshot's existing path index.
        return liveLeft == liveRight;
    }

    private static boolean changed(M3FileDeltaLedger.Snapshot snapshot) {
        for (int slot = 0; slot < snapshot.slots(); slot++) if (snapshot.changedAt(slot)) return true;
        return false;
    }

    private static long duration(Duration duration, boolean allowZero) {
        Objects.requireNonNull(duration, "duration");
        if (duration.isNegative() || (!allowZero && duration.isZero())
                || duration.compareTo(Duration.ofDays(1)) > 0)
            throw new IllegalArgumentException("M3 watcher duration outside zero/positive through one day");
        return duration.toNanos();
    }

    private void checkOwner() {
        if (closed) throw new IllegalStateException("M3 watcher is closed");
        if (Thread.currentThread() != owner) throw new IllegalStateException("M3 watcher belongs to its opening thread");
    }

    @Override public synchronized void close() throws IOException {
        if (closed) return;
        closed = true;
        IOException failure = null;
        try { watcher.close(); } catch (IOException ex) { failure = ex; }
        try { lock.release(); } catch (IOException ex) { if (failure == null) failure = ex; else failure.addSuppressed(ex); }
        try { lockChannel.close(); } catch (IOException ex) { if (failure == null) failure = ex; else failure.addSuppressed(ex); }
        if (failure != null) throw failure;
    }
}
