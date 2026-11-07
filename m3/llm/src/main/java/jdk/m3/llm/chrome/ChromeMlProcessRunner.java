// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.chrome;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Shell-free, single-flight adapter for a caller-owned ChromeMLBareRunner executable.
 * A failed/uncertain invocation retires this instance; the host must explicitly create a new one.
 * Asset hashes are checked at construction, metadata before/after each call. The host must prevent
 * concurrent asset replacement; this check is not a read-only mount or an OS sandbox.
 */
public final class ChromeMlProcessRunner implements ChromeMlRunner, AutoCloseable {
    private static final long MAX_ASSET_BYTES = 16L * 1024 * 1024 * 1024;
    private final ChromeMlRuntimeLayout layout;
    private final ChromeMlRuntimeLayout.Identity identity;
    private final Duration timeout;
    private final int maxCaptureBytes;
    /** Compatibility constructor family retained from the native-source helper. */
    public enum Backend { GPU, CPU, APU }
    @FunctionalInterface interface Launcher { Process start(ProcessBuilder builder) throws IOException; }
    @FunctionalInterface interface ProcessStarter { Process start(List<String> command, Path directory) throws IOException; }
    private final ChromeMlBackend backend;
    private final ProcessStarter starter;
    private final Object lifecycle = new Object();
    private final AtomicBoolean cleanupUncertain = new AtomicBoolean();
    private final List<ChromeMlPathPolicy.FileState> assetStates;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicReference<Thread> owner = new AtomicReference<>();
    private final AtomicReference<Process> active = new AtomicReference<>();

    public ChromeMlProcessRunner(ChromeMlRuntimeLayout layout, ChromeMlRuntimeLayout.ExpectedHashes expected,
            Duration timeout, int maxCaptureBytes) throws IOException {
        this(layout, expected, timeout, maxCaptureBytes, ChromeMlBackend.GPU);
    }

    public ChromeMlProcessRunner(ChromeMlRuntimeLayout layout, ChromeMlRuntimeLayout.ExpectedHashes expected,
            Duration timeout, int maxCaptureBytes, Backend backend) throws IOException {
        this(layout, expected, timeout, maxCaptureBytes, ChromeMlBackend.valueOf(backend.name()));
    }

    ChromeMlProcessRunner(ChromeMlRuntimeLayout layout, ChromeMlRuntimeLayout.ExpectedHashes expected,
            Duration timeout, int maxCaptureBytes, Backend backend, Launcher launcher) throws IOException {
        this(layout, expected, timeout, maxCaptureBytes, ChromeMlBackend.valueOf(backend.name()),
                (command, directory) -> launcher.start(new ProcessBuilder(command).directory(directory.toFile())));
    }

    public ChromeMlProcessRunner(ChromeMlRuntimeLayout layout, ChromeMlRuntimeLayout.ExpectedHashes expected,
            Duration timeout, int maxCaptureBytes, ChromeMlBackend backend) throws IOException {
        this(layout, expected, timeout, maxCaptureBytes, backend,
                (command, directory) -> new ProcessBuilder(command).directory(directory.toFile()).start());
    }

    ChromeMlProcessRunner(ChromeMlRuntimeLayout layout, ChromeMlRuntimeLayout.ExpectedHashes expectedHashes,
            Duration timeout, int maxCaptureBytes, ChromeMlBackend backend, ProcessStarter starter) throws IOException {
        this.backend = Objects.requireNonNull(backend, "backend");
        this.starter = Objects.requireNonNull(starter, "starter");
        this.layout = Objects.requireNonNull(layout, "layout");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
        if (timeout.compareTo(Duration.ofMillis(1)) < 0 || timeout.compareTo(Duration.ofMinutes(30)) > 0) {
            throw new IllegalArgumentException("timeout");
        }
        if (maxCaptureBytes < 1024 || maxCaptureBytes > 64 * 1024 * 1024) {
            throw new IllegalArgumentException("maxCaptureBytes");
        }
        this.maxCaptureBytes = maxCaptureBytes;
        this.assetStates = inspectAssets();
        this.identity = layout.validate(Objects.requireNonNull(expectedHashes, "expectedHashes"));
        requireAssetsUnchanged();
    }

    public ChromeMlRuntimeLayout.Identity runtimeIdentity() { return identity; }

    public boolean isClosed() { return closed.get(); }
    public boolean isRetired() { return isClosed(); }
    public boolean isQuiescent() { return owner.get() == null && active.get() == null && !cleanupUncertain.get(); }
    public ChromeMlBackend requestedBackend() { return backend; }
    public Backend requestedLegacyBackend() { return Backend.valueOf(backend.name()); }

    @Override
    public ChromeMlRunnerResult generate(ChromeMlRunnerRequest request, TokenSink sink) throws Exception {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(sink, "sink");
        Thread caller = Thread.currentThread();
        if (!owner.compareAndSet(null, caller)) { throw new IOException("RUNNER_BUSY"); }
        Process process = null;
        Thread outThread = null;
        Thread errThread = null;
        boolean success = false;
        List<ProcessHandle> descendants = new ArrayList<>();
        long deadline = System.nanoTime() + timeout.toNanos();
        try {
            requireOpen();
            requireAssetsUnchanged();
            requireTime(deadline);
            List<String> command = argv(request);
            // Recheck the exact constructor-audited bytes, within the overall invocation budget.
            layout.validate(identity.expectedHashes(), Duration.ofNanos(
                    Math.min(remaining(deadline), Duration.ofMinutes(2).toNanos())));
            requireOpen();
            requireTime(deadline);
            process = Objects.requireNonNull(starter.start(command, layout.workingDirectory()), "process");
            active.set(process);
            // Covers a close racing with the OS spawn; never return a result after cancellation.
            requireOpen();
            process.getOutputStream().close();
            ChromeMlBoundedCapture stdout = new ChromeMlBoundedCapture(process.getInputStream(), maxCaptureBytes, sink);
            ChromeMlBoundedCapture stderr = new ChromeMlBoundedCapture(process.getErrorStream(), maxCaptureBytes, null);
            outThread = Thread.ofVirtual().name("chromeml-stdout").start(stdout);
            errThread = Thread.ofVirtual().name("chromeml-stderr").start(stderr);
            while (true) {
                requireOpen();
                requireTime(deadline);
                rememberDescendants(process, descendants);
                if (stdout.failure() != null || stderr.failure() != null) {
                    Throwable failure = stdout.failure() != null ? stdout.failure() : stderr.failure();
                    if (failure instanceof IOException io) { throw io; }
                    throw new IOException("RUNNER_CAPTURE_FAILED", failure);
                }
                if (!process.isAlive() && !outThread.isAlive() && !errThread.isAlive()) { break; }
                TimeUnit.NANOSECONDS.sleep(Math.min(TimeUnit.MILLISECONDS.toNanos(10), remaining(deadline)));
            }
            if (process.exitValue() != 0) { throw new IOException("RUNNER_EXIT_" + process.exitValue()); }
            String out = stdout.text();
            String err = stderr.text();
            ChromeMlRunnerOutputParser.Parsed parsed;
            try {
                parsed = ChromeMlRunnerOutputParser.parseCompleted(out);
            } catch (IllegalArgumentException failure) {
                throw new IOException("RUNNER_COMPLETION_REJECTED", failure);
            }
            requireAssetsUnchanged();
            requireOpen();
            requireTime(deadline);
            synchronized (lifecycle) {
                requireOpen();
                success = true;
                return new ChromeMlRunnerResult(parsed.text(), parsed.status(), out, err);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw exception;
        } finally {
            if (!success) { closed.set(true); }
            boolean interrupted = Thread.interrupted();
            try {
                destroy(process, descendants);
                if (outThread != null) { outThread.interrupt(); }
                if (errThread != null) { errThread.interrupt(); }
                long cleanupDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
                if (process != null) { process.waitFor(remaining(cleanupDeadline), TimeUnit.NANOSECONDS); }
                for (Thread capture : new Thread[] {outThread, errThread}) {
                    if (capture != null) { capture.join(Duration.ofNanos(remaining(cleanupDeadline))); }
                }
                if ((process != null && process.isAlive()) || descendants.stream().anyMatch(ProcessHandle::isAlive)
                        || (outThread != null && outThread.isAlive()) || (errThread != null && errThread.isAlive())) {
                    cleanupUncertain.set(true);
                }
            } catch (InterruptedException interruptedCleanup) {
                interrupted = true;
                cleanupUncertain.set(true);
            } finally {
                if (!cleanupUncertain.get()) { active.compareAndSet(process, null); }
                owner.compareAndSet(caller, null);
                if (interrupted) { Thread.currentThread().interrupt(); }
            }
            if (cleanupUncertain.get()) {
                closed.set(true);
                throw new IOException("RUNNER_CLEANUP_UNCERTAIN");
            }
        }
    }

    /** Cancel the active process and reject future calls. User callbacks must honor interruption. */
    @Override
    public void close() {
        synchronized (lifecycle) { closed.set(true); }
        destroy(active.get(), List.of());
        // The owner polls the cancellation flag. Do not interrupt a pooled caller thread that
        // could have finished this invocation and already begun unrelated work.
    }

    private void requireOpen() throws IOException, InterruptedException {
        if (Thread.currentThread().isInterrupted()) { throw new InterruptedException("RUNNER_INTERRUPTED"); }
        if (closed.get()) { throw new IOException("RUNNER_CLOSED"); }
    }

    private static long remaining(long deadline) { return Math.max(1L, deadline - System.nanoTime()); }

    private static void requireTime(long deadline) throws IOException {
        if (deadline - System.nanoTime() <= 0) { throw new IOException("RUNNER_TIMEOUT"); }
    }

    private List<ChromeMlPathPolicy.FileState> inspectAssets() throws IOException {
        ChromeMlPathPolicy.requireDirectoryNoLinks(layout.workingDirectory());
        List<ChromeMlPathPolicy.FileState> states = new ArrayList<>();
        for (Path path : List.of(layout.runner(), layout.chromeMlDll(), layout.dawnDll(), layout.weights())) {
            var state = ChromeMlPathPolicy.inspectRegular(path, MAX_ASSET_BYTES);
            if (state.size() == 0) { throw new IOException("RUNTIME_ASSET_EMPTY"); }
            states.add(state);
        }
        return List.copyOf(states);
    }

    private void requireAssetsUnchanged() throws IOException {
        var now = inspectAssets();
        for (int i = 0; i < assetStates.size(); i++) {
            ChromeMlPathPolicy.requireUnchanged(assetStates.get(i), now.get(i));
        }
    }

    private static void rememberDescendants(Process process, List<ProcessHandle> known) throws IOException {
        try (var children = process.descendants()) {
            for (ProcessHandle child : children.limit(129).toList()) {
                if (!known.contains(child)) {
                    if (known.size() == 128) { throw new IOException("RUNNER_DESCENDANT_LIMIT"); }
                    known.add(child);
                }
            }
        }
    }

    private static void destroy(Process process, List<ProcessHandle> known) {
        if (process == null) { return; }
        // Best effort for observed descendants; use Windows Job Objects for OS-enforced containment.
        try (var children = process.descendants()) {
            children.limit(129).filter(ProcessHandle::isAlive).forEach(ProcessHandle::destroyForcibly);
        }
        known.stream().filter(ProcessHandle::isAlive).forEach(ProcessHandle::destroyForcibly);
        if (process.isAlive()) { process.destroyForcibly(); }
    }

    List<String> argv(ChromeMlRunnerRequest request) {
        if (request.prompt().length() > 16_384 || request.imagePaths().size() > 8
                || request.maxOutput() > 8192 || (request.contextTokens() != null && request.contextTokens() > 32768)
                || (request.topK() != null && request.topK() > 1024)
                || (request.temperature() != null && (request.temperature() < 0 || request.temperature() > 2))) {
            throw new IllegalArgumentException("RUNNER_REQUEST_LIMIT");
        }
        var args = new ArrayList<String>(); args.add(layout.runner().toString());
        if (backend == ChromeMlBackend.GPU) { args.add("--gpu"); }
        if (backend == ChromeMlBackend.APU) { args.add("--apu"); }
        args.addAll(List.of("--chat-input", "--no-cache", "--fast", "--prompt", request.prompt(),
                "--dll", layout.chromeMlDll().toString(), "--dawn-dll", layout.dawnDll().toString(),
                "--weights", layout.weights().toString(), "--max-output", Integer.toString(request.maxOutput())));
        if (request.contextTokens() != null) { args.addAll(List.of("--context-tokens", request.contextTokens().toString())); }
        if (request.topK() != null) { args.addAll(List.of("--top-k", request.topK().toString())); }
        if (request.temperature() != null) { args.addAll(List.of("--temperature", request.temperature().toString())); }
        if (request.shortContext()) { args.add("--short"); }
        if (request.top1()) { args.add("--top1"); }
        if (request.noSafetyReserve()) { args.add("--no-safety-reserve"); }
        for (Path image : request.imagePaths()) { args.addAll(List.of("--image", image.toString())); }
        if (request.audioPath() != null) {
            if (request.audioTargetRate() > 192000) { throw new IllegalArgumentException("RUNNER_AUDIO_RATE_LIMIT"); }
            args.addAll(List.of("--audio", request.audioPath().toString(), "--audio-target-rate", Integer.toString(request.audioTargetRate())));
        }
        // Conservative upper bound for Windows' 32767 UTF-16 CreateProcess command line, including quoting.
        long units = args.stream().mapToLong(s -> 2L * s.length() + 3L).sum();
        if (units > 30000) { throw new IllegalArgumentException("RUNNER_COMMAND_LINE_LIMIT"); }
        return List.copyOf(args);
    }
}
