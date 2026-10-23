<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Bounded viewport model deltas in the M3JDK21 java.base owner

This is **M3JDK21**, not an M3-on-JDK adapter. The existing
`java.util.ChangeJournal` stores positional change events as six aligned
primitive/reference arrays, without retained per-event objects. GPU, SWT, Nebula,
GEF, RWT, Swing and themes are consumers of the CPU projection; no renderer
gets a second authoritative collection or native resource owner.

## Added behavioral atom

`snapshot(long afterVersion, int maxChanges)` returns at most `maxChanges`
consecutive retained events. Its returned `Batch.cursor()` is the last copied
version, so consumers can advance in small, bounded frames even when much more
history is available. Requests with zero budget check cursor validity and return
an empty batch without advancing. The original `snapshot(long)` calls the
bounded path with the journal capacity; its behavior is unchanged.

A logical journal window intersects at most **two physical intervals** in the
circular arrays. Six `System.arraycopy` calls per contiguous interval replace
per-event modulo and field copies. Bounds remain controlled by the journal
capacity, and callbacks continue to run after detaching the batch, outside the
owner monitor. The owner publishes complete `version` after all lanes.

## Inventory / donor decision

- **Java SE contract**: no changes to existing public methods, exception text,
  mutation order or batch replay semantics. The overload is additive and marked
  as an M3JDK21 non-standard API, not a Java SE promise.
- **Category**: ring-buffer rank/index and bounded viewport batch selection.
- **Challenge references, ideas only**: LeetCode 189 (rotation), HackerRank
  Dynamic Array, GeeksforGeeks circular-queue ring geometry. No solution copied.
- **Donor selection**: own `java.util.ArrayDeque` circular-array precedent and
  `System.arraycopy`; no third-party code, runtime dependency or JNI entrypoint.
- **Reuse**: existing `CollectionViews` owns filtered/sorted live wrappers;
  existing `jdk.internal.m3.M3IndexMaskWindow` + `libjava` owns bounded
  Java/JNI primitive-mask selection. This PR does not duplicate either.
- **UI/language/theme**: filtered/sorted order and logical model remain on CPU.
  A renderer may invalidate presentation on a theme change, while text metrics,
  directionality, wrapping and locale-sensitive ordering must invalidate their
  affected CPU geometry/order projection. This separate work is not asserted
  complete by the journal atom.

## Executed proof scope

On an OpenJDK 21 runtime, the exact candidate source blob was compiled with
`javac --patch-module java.base=... -Xlint:all -Werror`. The byte-identical
`ChangeJournalWindowTest.java` was then compiled and executed against the
patched java.base; it prints `PASS ChangeJournalWindowTest`. It runs 9,000
deterministic differential iterations across six journal capacities and checks
wrap, replay, rejected limits, stale cursors and callback mutation.

Exact candidate Git blob: `256830b7df09df1b7c6c3fa64bf34113044e8159`.
Exact added test blob: `f82dc70c1fa856ba60f59ba084d7e7eb9b484101`.

Synexia's reusable source-sealed OpenRewrite recipe and category catalogue:
`com.synexia.rewrite.M3Jdk21ChangeJournalWindow20261021` on
`hsoliwal/com.synexia`, branch
`m3/journal-viewport-source-seal-20261021-gpt6`.

## Pending gates

The full OpenJDK configure/make + jtreg, Maven/OpenRewrite JUnit fixture execution,
native JNI/GPU/SWT/RWT matrix, and comparative performance measurements have
**not** been run here. This independent PR must not be merged automatically.
