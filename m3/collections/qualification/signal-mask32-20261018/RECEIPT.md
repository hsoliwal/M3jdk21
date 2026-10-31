<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# M3JDK21 source-sealed 32-bit signal ordering receipt — 2026-10-18

## Runtime owner and status

Product: `hsoliwal/M3jdk21`; target base commit `2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`.
Canonical convergence/reusable-recipe workspace: `hsoliwal/com.synexia`.
Source recipe PR: [Synexia #12350](https://github.com/hsoliwal/com.synexia/pull/12350).
Engine: `com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe`.
Target stage: opt-in collection proof, **NOT java.base promotion or String-phase advancement**.

This tranche adds full-width physical-slot state-mask filtering to the **existing**
`M3SignalOrder` and `M3SignalDimensions` owners. The existing byte-mask APIs
and all signatures/observable behavior remain unchanged. A newly generated
source-only differential test and JUnit entrypoint prove the gap. The native
backend remains the existing C11/JNI `m3signalorder` primitive stable radix
sorter. No Java object address, `Map.Entry` or managed object crosses JNI.

## Source-time and signal geometry

Source-time `@Sortable` generation (#1312) and progressive signal comparison evidence
remain separate owners. Signals may derive from domain properties, fields, methods,
class/package names, method signatures, calls, and dependency relationships.
A numeric signal is **not** identity, and the absolute numerical gap is not
automatically a semantic distance. Exact canonical object order needs a monotonicity
certificate or an existing comparator verifier. This tranche **does not** add
another signal producer or make source-time claims.

## Storage and algorithm selection

- Canonical data: original caller-owned double signal lanes and aligned full-width `int[]` slot-state masks.
- Selection: two sequential primitive scans (count then source-ordinal fill) using all 32 bits, including sign bit.
- Order: existing stable Java or JNI radix over transformed `long[]` keys and `int[]` row ordinals.
- Retention: immutable `int[]` row permutation only; primitive workspaces are short-lived, not per-entry objects.
- No additional runtime dependency, JNI symbol, collection hierarchy, cache, or swap plane.
- Challenge donor selection is documented in `CATEGORY_REVIEW.tsv`; external challenge implementation text is never copied.

## Checks

Run from the repository:

```sh
bash m3/collections/qualification/signal-mask32-20261018/verify.sh
```

The script compiles the actual owner sources using `javac --release 21 -Xlint:all -Werror`,
executes a separate JDK comparator differential oracle with random IEEE edge signals and
full-width mask matrices, then builds the existing JNI C source and reruns the same oracle
with native mode explicitly required on Linux. It rejects an unavailable native backend;
the default Java path never silently claims JNI success.

The canonical recipe additionally seals exact original target Java source hashes,
postimage hashes, ABSENT-before tests, fixed-point and drift refusal tests. Provenance
is recorded in the Synexia branch and these target mapping rows.

**PENDING until executed and reported:** Maven/OpenRewrite recipe JUnit, full M3 module
build, jtreg/HotSpot java.base acceptance, CPU/heap/native measurements, full JDK
regression and integration gates. No performance improvement is claimed from
algorithmic complexity alone. `master` is never rebased/force-pushed or replaced.
