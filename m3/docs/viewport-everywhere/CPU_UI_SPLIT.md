<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 viewport: CPU workers and GUI event thread

Implementation leaf: `m3/viewport-core/src/main/java/jdk/m3/viewport` (2026-11-18).

## Thread domains

```text
UI input thread
  => capture detached request (model version + viewport geometry)
  => M3ViewportScheduler.request(request)
       | CPU executor (bounded: one outstanding drain per scheduler)
       | calculate indexes/row orders/masks/sparse offsets/overscan/diff
       | poll cancellation token when visiting large data
       V
     immutable M3ViewportSnapshot
       | latest generation; coalesce UI posts
       V
  toolkit UI executor
       | SWT Synchronizer.MODEL / Swing EventQueue.invokeLater
       V
  validate widget lifetime + model version; install; paint/accessibility
```

The toolkit-neutral core has no SWT, Swing, AWT, Nebula, NetBeans or other
UI runtime import. The owning widget retains its normal thread contract.

## New API

* `M3ViewportScheduler<R,S>`: dependency-injected worker `Executor`,
  `UiExecutor`, CPU `Computer` with cooperative cancellation, and UI
  `Receiver`. One queued worker drain, one queued UI publication; newest
  request/result generation wins; `close()` invalidates all pending work.
  CPU exceptions are delivered to the receiver on the GUI thread.
* `M3ViewportSnapshot`: immutable resident-window primitive lanes:
  `long[] logicalIds`, `int[] topPixels`, `int[] heights`,
  `long[] stateMasks`, model version, first logical row, visible subrange.
  Inputs are copied once at the CPU/UI boundary; callers can copy a bounded
  slice to reusable scratch without exposing internal arrays.

The scheduler **does not** create threads, take ownership of an injected
executor, or touch toolkit objects. The worker input must itself be an
immutable detached view of model data. A GUI operation must not be passed
as a CPU computation function.

## Per-toolkit publication

* SWT/Nebula: capture a per-widget stable key and the active
  `Synchronizer` on the SWT thread; queue the UI runnable with
  `synchronizer.asyncExec(Synchronizer.MODEL, key, runnable)`.
  Existing `FrameListener` callbacks apply frame-level native redraw;
  do not invoke `syncExec` from worker compute callbacks.
* Swing/SwingX/NetBeans: `uiRunnable -> EventQueue.invokeLater(uiRunnable)`.
  The receiver must run on the EDT.
* Native layered display: CPU prepares immutable scenes; SWT/Skija/native
  graphics, device resources and accessibility are managed by the toolkit
  presentation owner only.

No SWT dependency has been added to M3JDK. The adapter integration is
documented in `hsoliwal/eclipse.platform.swt/docs/m3/VIEWPORT_CPU_GUI_SPLIT.md`.

## Guarantees and limits

* Latest request wins even if an older CPU task finishes after a new scroll.
  Cancellation is cooperative: noncooperative compute work can still consume
  CPU, but its result is never installed.
* Queue depth stays bounded per scheduler (one queued CPU drain and one UI
  publication), not per number of scroll events.
* Model version is a **second** validity check beyond scheduler generation.
  A model mutation with the same viewport geometry still must issue a new
  request. A UI receiver checks the model version and widget lifetime.
* CPU tasks operate only on detached snapshots. SWT model/GC/control
  traversal, native measurement, paint and widget callbacks remain on UI.
* The packed resident snapshot does not allocate an object per logical row.
  It does allocate/copied resident lane arrays on creation; pools and
  zero-copy ownership transfers require separately qualified lifecycle rules.

## Verification

`M3ViewportSchedulerTest` exercises 100,000 scroll requests, queued and
in-flight invalidation, closed scheduler and compute failures.

`M3ViewportSnapshotTest` covers defensive lane copies, long logical
indices, a bounded caller copy and invalid lane geometry.

Java 21 standalone production-class compilation: verified locally.
Maven/JUnit CI, SWT native UI integration and jtreg/Workbench gates:
**not yet run / not claimed**. These changes are review-only until those
gates pass and each widget is integrated using detached CPU-safe inputs.
