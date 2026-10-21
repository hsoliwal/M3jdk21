# M3JDK21 M3File exact shadow/progress receiving
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

This is a **stacked, additive draft** over original integrated
[hsoliwal/M3jdk21#1624](https://github.com/hsoliwal/M3jdk21/pull/1624).
M3JDK21 is the actual product and owner, not an M3+JDK bridge.
The original M3FileTree, ZIP/JAR scanners, ownership/physical-row index,
state masks, file source interfaces and JNI exports remain untouched.

Canonical source/recipe:
[hsoliwal/com.synexia#12676](https://github.com/hsoliwal/com.synexia/pull/12676),
commit `0c8cf65608e3050c9957deae6cdaf60dab417221`,
`com.synexia.m3.M3FileShadowExactProgress20261021`.
Four source postimages match the target GitHub readback byte for byte.

## Two serial, replaceable behavioral atoms

- `M3FileShadowInputStream.read`: cap requested provider input by the
  remaining byte allowance **before** read. When full, one byte decides whether
  EOF was reached or the source exceeded the limit. Refuse further reads after
  a confirmed excess. A nonconforming provider's zero-byte bulk read falls
  back to one-byte progress. This prevents an oversized provider block from
  being consumed without corresponding shadow and progress publication.
- `M3FileSubProgress.report`: preserve `floor(work * share / total)` exactly
  with 64-bit unsigned division and a bounded unsigned 128/64 long algorithm.
  No `double` rounding and no runtime `BigInteger` materialization.

These behavioral atoms do not add or change an exported method/field, change
the archive/container graph or introduce a Synexia runtime dependency.

## Source proof and limits

| Gate | Current status |
| --- | --- |
| Existing product files exact-matched recipe preimages before transformation | PASS |
| Four Java source postimages compared to canonical recipe templates | PASS |
| Source update order | two serial file commits, then two serial test-file commits |
| Representative Java 21 standalone compilation | PASS, `-Xlint:all,-serial -Werror` |
| Representative Java 21 oracle | PASS, 20,014 checks |
| Actual Maven/OpenRewrite `Recipe.run`, fixed point and source-drift refusals | NOT RUN |
| Full original `m3/file` JUnit / downstream ZIP/JAR / input-output contracts | NOT RUN |
| Source-built HotSpot/JNI/JMOD, jtreg, provider portability and resource matrix | NOT RUN |

The standalone Java oracle was executed with minimal API-compatible stubs.
It is not evidence of the full product build; new JUnit must run under the
actual product module with its original stream classes.

## Superset conflict boundaries

1. [#1608](https://github.com/hsoliwal/M3jdk21/pull/1608) adds provider-aware
   file snapshots/attribute deltas using a different detached `M3File`
   implementation. Do **not** wholesale copy its `M3File.java` onto this
   branch: adapt observations into the original tree-owned filesystem and
   archive sources after provider and identity testing.
2. [#1624](https://github.com/hsoliwal/M3jdk21/pull/1624) owns the original
   M3File dynamic-array/offset/flag tree, shadows, ZIP/JAR and Java/JNI
   collection superset. This refinement is stacked on that authority.
3. [#1643](https://github.com/hsoliwal/M3jdk21/pull/1643) is a separate
   filtered/sorted wrapper behavioral-atom candidate. Reconcile by exact
   contract and recipe, not by copying a whole wrapper family.
4. The already merged M3AST parser receiving work in `master` must be
   preserved when eventually reconciling this stacked file branch.

Preserve filtered/sorted backed-view semantics, observer callback order,
null/equality/comparator identity, concurrent modification and JNI ABI;
a primitive index/mask optimization is not permission to change those contracts.

Algorithm category decisions, rejected alternatives, FOSS pointers and the
canonical append-only recipe memory live in Synexia PR #12676.
Global String -> arrays -> collections -> AST stage gates remain unchanged.
