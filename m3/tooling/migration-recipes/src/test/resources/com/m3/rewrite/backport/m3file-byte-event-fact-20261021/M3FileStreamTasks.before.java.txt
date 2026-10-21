// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Progress-aware one-pass M3File admission.
 *
 * <p>This class never reopens or rereads payload bytes. It composes the existing
 * {@link M3FileStreamIndexer} event stream with a weighted child progress monitor.</p>
 */
public final class M3FileStreamTasks {
  private M3FileStreamTasks() {
    throw new AssertionError("No instances");
  }

  public static M3File plainText(
      Path path,
      M3FileResolver resolver,
      int languageId,
      M3ProgressMonitor monitor,
      long parentWork,
      M3FileStreamEvents.Sink events)
      throws IOException {
    return run(
        Objects.requireNonNull(monitor, "monitor"),
        parentWork,
        sink -> M3FileStreamIndexer.plainText(path, resolver, languageId, sink),
        events);
  }

  public static M3File codeLike(
      Path path,
      M3FileResolver resolver,
      int languageId,
      M3ProgressMonitor monitor,
      long parentWork,
      M3FileStreamEvents.Sink events)
      throws IOException {
    return run(
        Objects.requireNonNull(monitor, "monitor"),
        parentWork,
        sink -> M3FileStreamIndexer.codeLike(path, resolver, languageId, sink),
        events);
  }

  static M3File planned(
      M3FileUniversePlan.Entry entry,
      M3FileResolver resolver,
      int languageId,
      boolean codeLike,
      M3ProgressMonitor monitor,
      long parentWork,
      M3FileStreamEvents.Sink events)
      throws IOException {
    return run(
        Objects.requireNonNull(monitor, "monitor"),
        parentWork,
        sink -> M3FileStreamIndexer.planned(entry, resolver, languageId, codeLike, sink),
        events);
  }

  private static M3File run(
      M3ProgressMonitor parent,
      long parentWork,
      IndexedOperation operation,
      M3FileStreamEvents.Sink downstream)
      throws IOException {
    if (parentWork < 0L) throw new IllegalArgumentException("parentWork must be non-negative");
    M3ProgressMonitor child = parent.split(parentWork);
    MonitoredSink monitored =
        new MonitoredSink(child, Objects.requireNonNull(downstream, "events"));
    try {
      return operation.run(monitored);
    } finally {
      monitored.done();
    }
  }

  @FunctionalInterface
  private interface IndexedOperation {
    M3File run(M3FileStreamEvents.Sink sink) throws IOException;
  }

  private static final class MonitoredSink implements M3FileStreamEvents.Sink {
    private final M3ProgressMonitor monitor;
    private final M3FileStreamEvents.Sink downstream;
    private boolean begun;
    private boolean done;
    private long bytes;

    private MonitoredSink(M3ProgressMonitor monitor, M3FileStreamEvents.Sink downstream) {
      this.monitor = monitor;
      this.downstream = downstream;
    }

    @Override
    public synchronized void accept(M3FileStreamEvents.Event event) throws IOException {
      Objects.requireNonNull(event, "event");
      monitor.checkCanceled();
      if (event instanceof M3FileStreamEvents.Started started) {
        if (begun) throw new IOException("duplicate M3File Started event");
        begun = true;
        monitor.beginTask(
            "Index " + started.attributes().normalizedPath(),
            started.attributes().size());
      } else if (event instanceof M3FileStreamEvents.Bytes chunk) {
        if (!begun || done) throw new IOException("M3File byte event outside active traversal");
        long length = chunk.payload().length;
        bytes = Math.addExact(bytes, length);
        monitor.worked(length);
      } else if (event instanceof M3FileStreamEvents.Finished finished) {
        if (!begun || done) throw new IOException("invalid M3File Finished event");
        if (finished.byteCount() != bytes) {
          throw new IOException(
              "M3File progress byte count mismatch: events="
                  + bytes
                  + " finished="
                  + finished.byteCount());
        }
      }
      downstream.accept(event);
      if (event instanceof M3FileStreamEvents.Finished) done();
    }

    private synchronized void done() {
      if (done) return;
      done = true;
      monitor.done();
    }
  }
}
