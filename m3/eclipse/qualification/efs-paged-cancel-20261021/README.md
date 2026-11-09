<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
-->
# M3JDK21 Eclipse EFS native-page cancellation — 2026-10-21

## Integrated SDK ownership
This modifies the **existing M3JDK21 Eclipse EFS provider itself** under `m3/eclipse/filesystem`, backed by the existing JNI/SQLite `M3FS` owner. It does not introduce M3+JDK, a Java compatibility adapter, another filesystem engine, or new native symbols. Filtered/sorted `CollectionViews` and masked `M3IndexPathRank` work remains in their original owners (including current separate PR #1764).

**Canonical source-sealed Maven/OpenRewrite recipe:** [hsoliwal/com.synexia#12875](https://github.com/hsoliwal/com.synexia/pull/12875), revision `50f33a537f54391e4cf5e6a889554b7d1b0147c6`, recipe `com.synexia.m3.EclipseEfsPagedCancel20261021`.
**Exact product input:** M3JDK21 master `7f657d6ca05760af7bee7d4d2d9e14ae91ddd942`, source Git blob `1f378dfd8a0cf9ea94c935797613726850e0dce3`.

## In-place changes
- One `canceled(monitor)` checkpoint before each 512-child `M3FS.DirectoryPage` in the existing depth-first recursive delete loop; a cancellation observed on the second page now aborts before any child unlink.
- Pass the borrowed progress monitor to the existing `childStores` during recursive-copy enumeration instead of unconditionally `null`.
- Preserve every existing public EFS method/signature, native pointer/generation ownership, tree order, root/no-op semantics, failure aggregation, existing dynamic string/boolean stack arrays, and JNI symbol set.
- New `M3EfsPagedCancellationTest` populates 520 real native-backed EFS files. On the fourth cancellation poll, it requires `OperationCanceledException` and all children remaining. This is a source-owned JUnit test in the existing Maven module, not a stand-alone simulation.

## Qualification
- Java 21 standalone ordinal/paging-order witness: PASS (synthetic 512-row/520-file counterexample).
- Source/template byte identity and Git tree readback: verified at PR creation.
- Canonical Maven/OpenRewrite recipe JUnit, drift-refusal/fixed-point: AUTHORED, NOT EXECUTED (Maven unavailable).
- Target `m3/eclipse/filesystem` JUnit with **real M3FS JNI**, Eclipse/Tycho/OSGi and full JDK/reactor: **NOT EXECUTED**; fail closed, PR remains DRAFT.
- No performance claim, JNI expansion or complete Eclipse Platform acceptance is made.

## Wider Eclipse Platform
All-source 12-Gitlink/17-reactor coverage owner: [Synexia#12853](https://github.com/hsoliwal/com.synexia/pull/12853). 109 Eclipse-derived capability-family catalogue and original-index viewport atom: [M3JDK21#1744](https://github.com/hsoliwal/M3jdk21/pull/1744). The matrix below records areas that still require their own per-file recipe and acceptance; these two PRs are not whole-platform proof.

## Existing targeted tests
`mvn -f m3/eclipse/filesystem/pom.xml verify`, with the repository-approved M3FS JNI library loaded, must run **all** unit and integration tests without skips. Verify the original `M3EclipseFileSystemTest` and `M3EfsContractRefinementTest`, then require the new regression and real SWT/OSGi EFS consumer gates. Do not substitute a native-absent simulation for this verification.
