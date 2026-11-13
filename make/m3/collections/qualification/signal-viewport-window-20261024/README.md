<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->
# Integrated M3JDK21 signal viewport page receiving — 2026-10-24

**M3JDK21 is the modified product itself**, not a Synexia overlay. This receiver
extends only the original M3SignalBooleanQuery and delivers its additive original-source
JUnit oracle and CI workflow. All reusable Maven/OpenRewrite recipes, before/after
source snapshots, improvement memory and catalogue remain exclusively in Synexia.

* A new `Compiled.evaluateWindow(skip,limit,version,digest[,progress])` returns one
  compact `Window` with **exact total match count**, actual clamped start, and only
  requested physical row IDs. `limit=0` obtains the exact count with **zero result IDs**.
* Existing `evaluate()` and the original `and/or` API behavior are preserved; each
  still returns all matching physical rows. One query workspace remains bounded by
  the existing max-memory policy (64 MiB cap), now without a full result-array
  allocation for bounded pages. No retained per-row objects or copied value columns.
* A window is a snapshot of typed **stored-coordinate** matching, not a live SWT
  viewport or proof of domain equality. The caller must maintain consistent source
  inputs, propagate native/reflection/external changes through `invalidateAll`, and
  revalidate source generation before using results.
* Page offset and limit must be nonnegative; offsets beyond the end return an empty
  page. Stable order is physical row ID ascending, including missing, long beyond
  2^53, signed zero and NaN predicates through the original exact range index.
* Cancellation is delegated to the **existing** `M3ProgressMonitor`; failures must
  not publish partial results. The original progress callbacks are still invoked,
  and a mutation during completion is rejected by the original epoch owner.

## Explicit qualification

Local JDK 21 `javac --release 21 -proc:none -Xlint:all -Werror` against unchanged
API-compatible source doubles: PASS. Independent window tests: 32,077 assertions
normal and `-Xint`, including 360 seeded source datasets, 36 page requests per
sample, all principal row/word boundaries, and a 262,153-row count-only fixture.
Earlier unchanged original Boolean (61,543 assertions) and cancellation (19)
oracles passed with this candidate. Those local compatible-owner tests do **not**
constitute source-built modified JDK or owning Maven reactor acceptance.

Required: exact Synexia OpenRewrite SDK first application/fixed point/drift refusal,
original hosted source Java21 and owning Maven/JUnit, source-built JDK/jmods,
jtreg/JCK, JNI/HotSpot/GC/CDS, Windows/Linux and before/after CPU/heap/native
benchmarks. All remain **PENDING** until actually executed. Keep PR DRAFT/HOLD.
