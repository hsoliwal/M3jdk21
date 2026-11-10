/* ******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.paradigm.context;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Host-only bounded process adapter. No shell, no evaluation of source/email/catalog commands.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/tooling/ToolProcess.java}
 * blob {@code 46b459bbe0a7bdb5747d88b9ce36ff885da506a4} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class ToolProcess {
    private ToolProcess() { }
    public enum Status { EXITED, UNAVAILABLE, TIMEOUT, HALTED, OUTPUT_LIMIT, IO_FAILURE }
    public record Result(Status status, int exitCode, long elapsedNanos, long logBytes, String logHash) {
        public boolean succeeded() { return status == Status.EXITED && exitCode == 0; }
    }
    public static Result run(List<String> argv, Path directory, Path log, Duration timeout,
                             int maximumLogBytes, GrailControl control) throws IOException {
        List<String> command = List.copyOf(argv);
        if (command.isEmpty() || command.stream().anyMatch(s -> s.indexOf('\u0000') >= 0)
                || timeout.isNegative() || timeout.isZero() || timeout.compareTo(Duration.ofHours(24)) > 0
                || maximumLogBytes < 1 || maximumLogBytes > 64 * 1024 * 1024) {
            throw new IllegalArgumentException("Invalid local process limits");
        }
        control.charge(1);
        Path working = directory.toRealPath();
        long started = System.nanoTime();
        // Reserve the log before starting the child; a bad output destination cannot launch work.
        OutputStream destination = Files.newOutputStream(log, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        Process process;
        try { process = new ProcessBuilder(command).directory(working.toFile()).redirectErrorStream(true).start(); }
        catch (IOException unavailable) {
            destination.close();
            return new Result(Status.UNAVAILABLE, -1, System.nanoTime() - started, 0, ToolFiles.hash(new byte[0]));
        }
        AtomicBoolean overflow = new AtomicBoolean();
        AtomicReference<IOException> failure = new AtomicReference<>();
        Thread pump = Thread.ofVirtual().unstarted(() -> copy(process.getInputStream(), destination, maximumLogBytes, overflow, failure));
        Status status = Status.EXITED;
        boolean interrupted = false;
        int exit = -1;
        try {
            pump.start(); process.getOutputStream().close();
            while (process.isAlive()) {
                control.checkpoint();
                if (overflow.get()) { status = Status.OUTPUT_LIMIT; break; }
                if (failure.get() != null) { status = Status.IO_FAILURE; break; }
                if (System.nanoTime() - started >= timeout.toNanos()) { status = Status.TIMEOUT; break; }
                process.waitFor(25, TimeUnit.MILLISECONDS);
            }
            control.checkpoint();
        } catch (GrailControl.Halted halted) {
            status = Status.HALTED;
            // Preserve an existing interrupt but temporarily clear it for bounded child cleanup.
            interrupted = Thread.interrupted();
        } catch (InterruptedException halted) {
            interrupted = true; status = Status.HALTED;
        } catch (IOException io) {
            status = Status.IO_FAILURE;
        } finally {
            if (process.isAlive()) terminate(process);
            try {
                if (process.waitFor(2, TimeUnit.SECONDS)) exit = process.exitValue();
                else status = Status.IO_FAILURE;
                if (!pump.join(Duration.ofSeconds(2))) {
                    process.getInputStream().close(); pump.interrupt();
                    if (!pump.join(Duration.ofSeconds(2))) status = Status.IO_FAILURE;
                }
            } catch (InterruptedException halt) {
                interrupted = true; status = Status.HALTED;
            } catch (IOException io) {
                status = Status.IO_FAILURE;
            } finally {
                try { process.getInputStream().close(); } catch (IOException io) { status = Status.IO_FAILURE; }
                // Restore after the final bounded log read, which uses an interruptible channel.
            }
        }
        if (overflow.get() && status == Status.EXITED) status = Status.OUTPUT_LIMIT;
        if (failure.get() != null && status == Status.EXITED) status = Status.IO_FAILURE;
        try {
            byte[] bytes = ToolFiles.read(log, maximumLogBytes);
            return new Result(status, exit, System.nanoTime() - started, bytes.length, ToolFiles.hash(bytes));
        } finally {
            if (interrupted) Thread.currentThread().interrupt();
        }
    }
    private static void copy(InputStream input, OutputStream destination, int maximum,
                             AtomicBoolean overflow, AtomicReference<IOException> failure) {
        try (input; destination) {
            byte[] buffer = new byte[8192];
            int remaining = maximum, count;
            while ((count = input.read(buffer)) >= 0) {
                int accepted = Math.min(remaining, count);
                destination.write(buffer, 0, accepted); remaining -= accepted;
                if (count > accepted) { overflow.set(true); break; }
                if (count > 0 && remaining == 0) {
                    if (input.read() != -1) overflow.set(true);
                    break;
                }
            }
        } catch (IOException io) { failure.set(io); }
    }
    private static void terminate(Process process) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }
}
