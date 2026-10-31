<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 integrated indexed-array two-mask window refinement

M3JDK21 is the **single Java21/JDK/HotSpot/JNI SDK**, not an M3 layer installed over another JDK. This task changes the original M3JDK21 collection component in place, retaining the parent-owned int[]/long[] row paths and byte[] state lanes. It does not introduce another collection hierarchy, retained object wrappers, alternate parser, or a Java-to-JNI adapter product.

## Problem category and algorithm

Before this change, M3IndexMaskCompactor already supported:
- Whole-state logical positions with required AND excluded unsigned-byte bits.
- Bounded int[]/long[] row/state windows with **required bits only**, with independent offsets.

The missing combination prevented existing filtered/sorted indexed owners from selecting a *bounded* row window that also excludes flag states without first preparing a separately filtered state buffer.

This proposal adds **overloads** of the two existing window methods with an excludedMask parameter. Existing method signatures, JNI symbols and observable required-only execution stay unchanged. Both new Java methods retain source encounter order, signed int/long IDs, duplicate occurrences, and independent row/state offsets. They allocate only a detached exact-sized primitive result (and a defensive full-width copy when every row survives); their source arrays remain owned by callers. Required and forbidden masks are checked against an **unsigned byte** exactly as in the pre-existing logical-position method.

JNI reuses the existing m3_index_mask_compactor.c Region API, 256-row stack chunking, input geometry validation, two-pass selection, and state-drift failure. Only three additive symbols are introduced: windowMasksAbiNative, selectWindowMasksNative, selectLongWindowMasksNative. An old native binary without the new ABI is explicitly refused when NATIVE_REQUIRED is selected; there is no hidden Java fallback. No JNI callbacks, persisted native pins, or per-row heap allocations are introduced.

## Canonical source transformation recipe

Reuse **the existing Synexia M3HashPinnedJavaSnapshotRecipe**, not a duplicate migration engine. The two named OpenRewrite recipe entries are:
- com.synexia.m3.RequiredExcludedOffsetWindows20261021 — original Java owner + original scalar oracle + JUnit adapter.
- com.synexia.m3.RequiredExcludedOffsetWindowNative20261021 — original C11 JNI owner, PlainText source.
  
The four checked-in template bodies have Git blobs matching their receiving files byte-for-byte. Their manifests pin exact preimage SHA-256 or ABSENT and exact postimage SHA-256. Any stale source must be held and the recipe corrected/replayed rather than overwriting the receiver.

## Verification instructions and boundaries

Run: `bash m3/collections/qualification/required-excluded-windows-20261021/verify.sh`.

This compiles the *original receiving* M3IndexMaskCompactor.java with `javac --release 21 -Xlint:all -Werror`, runs an independent seeded 4,096-case scalar oracle on Java, compiles the *original receiving* m3_index_mask_compactor.c as C11/JNI using `-Wall -Wextra -Werror -Wpedantic`, and runs the identical oracle with real JNI and `-Xcheck:jni`. The tests cover:
- offset independence, zero and contradictory masks, high-bit states 0x80, signed extremes and duplicates;
- defensive output isolation, stable index order, both int[]/long[] paths;
- invalid offsets, null inputs, unsigned-mask out-of-range exceptions;
- random scalar-vs-Java and scalar-vs-JNI agreement and unchanged parent arrays.

The JUnit adapter runs the scalar oracle in the existing m3/collections Maven module. This new workflow is not a replacement for full Maven/JDK/HotSpot/jtreg/JNI suite admission, sorted/filtered live-wrapper contract tests, and platform parity.

**Execution truth for initial review submission:** Exact GitHub source/postimage readback and source-locked recipe manifests COMPLETE. Full original Maven reactor, GitHub Actions, matched JDK image/jtreg, Windows JNI, public API dump, GC/concurrency resource proof, and performance benchmarks are **NOT YET VERIFIED** by this pass. No performance improvement is asserted without a matched baseline. Source PRs and owner branches elsewhere remain open. This branch is review only.

## Algorithm catalogue and license evaluation

See `m3/collections/algorithm-catalog/required-excluded-windows-20261021.tsv`. Original JDK/Synexia primitive compaction and C JNI Region APIs were selected. LeetCode 27 (stable-prefix inspiration only), GeeksforGeeks stable partition and HackerRank AND Product are documented as category references; their published challenge/editorial code was not copied. The GeeksforGeeks boxed stable sort is rejected because it violates primitive/no-boxing and O(n) target constraints. Existing copyright/SPDX headers and original exported signatures are preserved.

## Remaining convergence

The broader M3JDK21 SDK PR stack covers live filtered/sorted collection wrappers, dynamic parent-owned Object[] arrays, full M3String and M3AST, native memory and Maven bootstrap. This PR adds only the above specific owner capability and test evidence; it does **not** mark hundreds of other PRs as integrated.
