// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.function.LongConsumer;

/** JDK callbacks for the existing cancellation/progress points in artifact custody. */
public final class M3FsProgress {
  private static final M3FsProgress NOOP = new M3FsProgress(() -> false, amount -> {});
  private final BooleanSupplier cancelled;
  private final LongConsumer progress;
  public M3FsProgress(BooleanSupplier cancelled, LongConsumer progress) {
    this.cancelled = Objects.requireNonNull(cancelled, "cancelled");
    this.progress = Objects.requireNonNull(progress, "progress");
  }
  public static M3FsProgress noop() { return NOOP; }
  public static M3FsProgress of(BooleanSupplier cancelled, LongConsumer progress) {
    return new M3FsProgress(cancelled, progress);
  }
  public void checkCanceled() { if (cancelled.getAsBoolean()) throw new CancellationException(); }
  public void worked(long amount) { progress.accept(amount); }
}
