// SPDX-License-Identifier: Apache-2.0
package jdk.m3.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Exact argv OS pipelines; OS pipes handle inter-stage flow, hot arrays spill automatically. */
public final class NativeProcessPipeline {
  public NativeProcessPipeline() { }
  private static final int MAX_STAGES = 32;
  private static final Duration MAX_DURATION = Duration.ofDays(1);

  /**
   * Input stream ownership stays with the caller. If a caller-provided InputStream has a
   * noninterruptible read, cancellation cannot guarantee termination of that input worker.
   * No command is implicitly authorized: executable paths and argv must be host-approved.
   */
  public NativePipelineResult run(List<NativeCommand> commands, InputStream input,
      Duration timeout) throws IOException, InterruptedException, TimeoutException {
    Objects.requireNonNull(input, "input");
    Objects.requireNonNull(timeout, "timeout");
    if (commands == null || commands.isEmpty() || commands.size() > MAX_STAGES
        || timeout.isNegative() || timeout.isZero() || timeout.compareTo(MAX_DURATION) > 0) {
      throw new IllegalArgumentException("invalid pipeline or timeout");
    }
    List<ProcessBuilder> builders = new ArrayList<>(commands.size());
    for (NativeCommand command : commands) {
      Objects.requireNonNull(command, "stage");
      if (!Files.isRegularFile(command.executable())
          || !Files.isExecutable(command.executable())) {
        throw new IOException("executable unavailable: " + command.executable());
      }
      builders.add(new ProcessBuilder(command.argv()).redirectErrorStream(false));
    }
    int streamCount = commands.size() + 1;
    SpoolingCapture stdout = new SpoolingCapture(() -> HeapCaptureBudget.perStream(streamCount));
    List<SpoolingCapture> errors = new ArrayList<>(commands.size());
    for (int i = 0; i < commands.size(); i++) {
      errors.add(new SpoolingCapture(() -> HeapCaptureBudget.perStream(streamCount)));
    }
    List<Process> processes = List.of();
    ExecutorService workers = null;
    boolean published = false;
    long deadline = System.nanoTime() + timeout.toNanos();
    try {
      processes = ProcessBuilder.startPipeline(builders);
      workers = Executors.newVirtualThreadPerTaskExecutor();
      List<Future<?>> jobs = new ArrayList<>(commands.size() + 2);
      Process first = processes.getFirst();
      jobs.add(workers.submit(() -> {
        try (OutputStream target = first.getOutputStream()) { input.transferTo(target); }
        return null;
      }));
      Process last = processes.getLast();
      jobs.add(workers.submit(() -> {
        try (InputStream source = last.getInputStream()) { source.transferTo(stdout); }
        return null;
      }));
      for (int i = 0; i < processes.size(); i++) {
        final InputStream source = processes.get(i).getErrorStream();
        final SpoolingCapture target = errors.get(i);
        jobs.add(workers.submit(() -> {
          try (source) { source.transferTo(target); }
          return null;
        }));
      }
      List<Integer> exitCodes = new ArrayList<>(processes.size());
      for (Process process : processes) {
        long remaining = remaining(deadline);
        if (!process.waitFor(remaining, TimeUnit.NANOSECONDS)) {
          throw new TimeoutException("pipeline process timed out");
        }
        exitCodes.add(process.exitValue());
      }
      for (Future<?> job : jobs) {
        try { job.get(remaining(deadline), TimeUnit.NANOSECONDS); }
        catch (ExecutionException e) {
          Throwable cause = e.getCause();
          if (cause instanceof IOException io) throw io;
          throw new IOException("pipeline worker failed", cause);
        }
      }
      stdout.finish();
      for (SpoolingCapture error : errors) error.finish();
      published = true;
      return new NativePipelineResult(exitCodes, stdout, errors);
    } finally {
      if (workers != null) {
        if (published) workers.shutdown(); else workers.shutdownNow();
      }
      if (!published) {
        for (Process process : processes) process.destroyForcibly();
        for (SpoolingCapture error : errors) {
          try { error.close(); } catch (IOException ignored) { /* suppress cleanup failure */ }
        }
        try { stdout.close(); } catch (IOException ignored) { /* suppress cleanup failure */ }
      }
    }
  }

  private static long remaining(long deadline) throws TimeoutException {
    long available = deadline - System.nanoTime();
    if (available <= 0) throw new TimeoutException("pipeline deadline exceeded");
    return available;
  }
}
