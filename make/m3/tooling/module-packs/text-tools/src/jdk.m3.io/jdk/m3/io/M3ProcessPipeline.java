// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.io;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;

/**
 * Explicit external-command boundary; the existing jdk.m3.text.M3UnixRuntime remains the in-process owner.
 * Uses the JDK's native process/pipeline implementation, not a shell parser or Java text relay.
 * No implicit decoding, String interning, global stream replacement or output accumulation occurs.
 * Native argv encoding and explicitly selected programs/scripts retain JDK/OS semantics.
 * Commands are trusted host code, not a sandbox. Timeout cannot preempt native process launch.
 */
public final class M3ProcessPipeline {
    public static final int MAX_STAGES = 64;
    private static final long POLL_NANOS = TimeUnit.MILLISECONDS.toNanos(20);
    private static final long REAP_NANOS = TimeUnit.SECONDS.toNanos(1);

    private M3ProcessPipeline() { }

    /**
     * An immutable argv pipeline. Endpoints must be inherited, files, or output DISCARD:
     * PIPE endpoints are rejected because this synchronous API cannot expose/drain them.
     * Each stage inherits stderr; stderr can never fill an undrained Java-side pipe.
     * Redirect.to has the JDK's explicit create/truncate semantics; APPEND is also supported.
     */
    public record Request(List<List<String>> commands, Path directory,
                          ProcessBuilder.Redirect input, ProcessBuilder.Redirect output,
                          Duration timeout) {
        public Request {
            Objects.requireNonNull(commands, "commands");
            Objects.requireNonNull(input, "input");
            Objects.requireNonNull(output, "output");
            Objects.requireNonNull(timeout, "timeout");
            if (commands.isEmpty() || commands.size() > MAX_STAGES) {
                throw new IllegalArgumentException("pipeline must contain 1.." + MAX_STAGES + " stages");
            }
            List<List<String>> frozen = new ArrayList<>(commands.size());
            for (List<String> stage : commands) {
                List<String> argv = List.copyOf(Objects.requireNonNull(stage, "stage"));
                if (argv.isEmpty() || argv.getFirst().isEmpty()) {
                    throw new IllegalArgumentException("empty command");
                }
                for (String argument : argv) {
                    if (argument.indexOf('\0') >= 0) {
                        throw new IllegalArgumentException("NUL in command argument");
                    }
                }
                frozen.add(argv);
            }
            commands = List.copyOf(frozen);
            if (input.type() != ProcessBuilder.Redirect.Type.READ
                    && input.type() != ProcessBuilder.Redirect.Type.INHERIT) {
                throw new IllegalArgumentException("input must be a file or INHERIT");
            }
            if (output.type() != ProcessBuilder.Redirect.Type.WRITE
                    && output.type() != ProcessBuilder.Redirect.Type.APPEND
                    && output.type() != ProcessBuilder.Redirect.Type.INHERIT) {
                throw new IllegalArgumentException("output must be a file, DISCARD, APPEND or INHERIT");
            }
            // An explicit bound also avoids nanoTime subtraction overflow after arbitrary durations.
            if (timeout.isZero() || timeout.isNegative() || timeout.compareTo(Duration.ofDays(365)) > 0) {
                throw new IllegalArgumentException("timeout must be positive and at most 365 days");
            }
        }

        public static Request inherited(List<List<String>> commands, Duration timeout) {
            return new Request(commands, null, ProcessBuilder.Redirect.INHERIT,
                    ProcessBuilder.Redirect.INHERIT, timeout);
        }
    }

    /** Exit statuses stay in stage order. A nonzero command status is not a launch failure. */
    public record Result(List<Integer> exitCodes) {
        public Result {
            exitCodes = List.copyOf(Objects.requireNonNull(exitCodes, "exitCodes"));
            if (exitCodes.isEmpty()) throw new IllegalArgumentException("empty exit status list");
        }
        public boolean succeeded() { return exitCodes.stream().allMatch(code -> code == 0); }
        public int lastExitCode() { return exitCodes.getLast(); }
        /** Rightmost nonzero status, or zero, without discarding the individual stage results. */
        public int pipefailExitCode() {
            for (int i = exitCodes.size() - 1; i >= 0; i--) {
                if (exitCodes.get(i) != 0) return exitCodes.get(i);
            }
            return 0;
        }
    }

    public static Result run(Request request) throws IOException, InterruptedException, TimeoutException {
        return run(request, null);
    }

    /**
     * The optional monitor is an adapter for the host's existing ProgressMonitor.isCanceled.
     * It is polled only on this caller thread, before launch and while awaiting process completion.
     * Cancellation, interruption, timeout or callback failure terminate the directly launched stages.
     * Descendants escaping those processes are not supervised; use an OS job/cgroup for containment.
     */
    public static Result run(Request request, BooleanSupplier canceled)
            throws IOException, InterruptedException, TimeoutException {
        Objects.requireNonNull(request, "request");
        checkCanceled(canceled);
        List<ProcessBuilder> builders = new ArrayList<>(request.commands().size());
        for (List<String> command : request.commands()) {
            ProcessBuilder builder = new ProcessBuilder(command)
                    .redirectError(ProcessBuilder.Redirect.INHERIT);
            if (request.directory() != null) builder.directory(request.directory().toFile());
            builders.add(builder);
        }
        builders.getFirst().redirectInput(request.input());
        builders.getLast().redirectOutput(request.output());
        long started = System.nanoTime();
        // The JDK cleans up already-started stages if native launch of a later stage fails.
        List<Process> processes = ProcessBuilder.startPipeline(builders);
        boolean completed = false;
        try {
            long budget = request.timeout().toNanos();
            for (;;) {
                checkCanceled(canceled);
                Process live = null;
                for (Process process : processes) {
                    if (process.isAlive()) { live = process; break; }
                }
                long remaining = budget - (System.nanoTime() - started);
                if (remaining <= 0) throw new TimeoutException("native pipeline deadline exceeded");
                if (live == null) {
                    List<Integer> statuses = processes.stream().map(Process::exitValue).toList();
                    completed = true;
                    return new Result(statuses);
                }
                live.waitFor(Math.min(remaining, POLL_NANOS), TimeUnit.NANOSECONDS);
            }
        } finally {
            if (!completed) terminate(processes);
            for (Process process : processes) {
                closeQuietly(process.getOutputStream());
                closeQuietly(process.getInputStream());
                closeQuietly(process.getErrorStream());
            }
        }
    }

    private static void checkCanceled(BooleanSupplier canceled) throws InterruptedException {
        if (Thread.interrupted()) throw new InterruptedException("pipeline interrupted");
        if (canceled != null && canceled.getAsBoolean()) {
            throw new CancellationException("pipeline canceled");
        }
    }

    private static void terminate(List<Process> processes) {
        for (Process process : processes) {
            if (process.isAlive()) process.destroyForcibly();
        }
        boolean interrupted = Thread.interrupted();
        long started = System.nanoTime();
        try {
            for (Process process : processes) {
                while (process.isAlive()) {
                    long remaining = REAP_NANOS - (System.nanoTime() - started);
                    if (remaining <= 0) return; // OS termination is not an instantaneous guarantee.
                    try { process.waitFor(remaining, TimeUnit.NANOSECONDS); }
                    catch (InterruptedException e) { interrupted = true; }
                }
            }
        } finally {
            if (interrupted) Thread.currentThread().interrupt();
        }
    }

    private static void closeQuietly(java.io.Closeable resource) {
        try { resource.close(); }
        catch (IOException ignored) { /* Redirected/null streams do not carry retained output. */ }
    }
}
