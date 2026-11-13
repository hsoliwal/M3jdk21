<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Filtered collection merge reconciliation, 2026-10-19

## Reviewed scope

Repository `hsoliwal/M3jdk21`, exact source revision
`677437be295b23f7ccf7409be287008944d0e8b0`. This packet contains reviewed
recipe payloads. The review did not modify the product checkout. Synexia remains
the canonical Maven/OpenRewrite recipe owner; the parent task must execute its
qualified recipe before materializing these product bytes.

`M3FilteredViews.java` Git blob is
`34fca25ba45467567213ed7a170bf4b27d6427e3`; its source SHA-256 is
`eb3feb81c6534e617727cc2993ae01dea5675a40a18414b872dc7212edbf7f88`.
All target/source payload hashes are in `CANDIDATE_MANIFEST.json`.

## Actual merged-source blocker

Java 21 compilation of all 268 owning-module production Java source files fails
with four duplicate-method errors in one owner:

- `SortedSetView.reversed()` appears at lines 314 and 333.
- `SortedSetView.removeFirst()` appears at lines 320 and 340.
- `SortedSetView.removeLast()` appears at lines 321 and 341.
- `NavigableSetView.reversed()` appears at lines 413 and 419.

`baseline-compile.json` records the exact compiler command, exit and 268 source
hashes; `baseline-compile.log` retains diagnostics. No conclusion about current
behavior can be inferred from this non-compilable source. Both historical valid
bodies and their executed tests were inspected to choose the surviving atom.

## Historical contract selection

The valid strict-sequenced body at `f14837505f58aa1af0b82b5802747628a9026890`
reverses the physical sorted source before wrapping its predicate. The valid
navigation-reconciliation body at
`d11f07d78d1705d325272e2dfd5b87e72ab100ab` guards the JDK21 default reversed
view over the existing filtered owner. Both pass the existing 126-check strict
sequenced oracle and the 904-check read-only endpoint oracle.

Their ordinary-read callback tapes differ. The earlier strict-only tape is
`5590f3792e00a48dfcd832aa9076a8b02c859e9c74a95a756272dd7e492d59bd`;
the navigation reconciliation tape is
`0a279ab0c033308d4ec3777b2d1328f4ff9dd4909e6a79e447418ad800f283ac`.
Selecting a body by line order or deleting an entire historical feature would
silently choose different callback demand.

The candidate retains the navigation-reconciliation guarded default reverse
body, preserving its established ordinary-read tape exactly. Identical
read-only endpoint and covariant navigable reverse declarations are collapsed.
The existing comparator-preserving `M3SlotSpliterators.sorted(this)` call from
PR #1303 remains. PR #1314's middle-null navigation, comparator-representative
entry membership and explicit continuation-array behavior remain intact.

## Cross-feature split correction

Compiling only the duplicate removal exposes an additional composition gap:
`ReadOnlySortedSet.spliterator()` delegates to the JDK21 reverse owner's
spliterator, so reversed wrappers or their sorted subranges do not consistently
retain sorted characteristics and comparator metadata on split children.

The candidate reuses `M3SlotSpliterators.sorted(source)` inside the existing
guard. This binds the same source iterator and size lazily, supplies the actual
owner comparator lazily, and preserves it recursively on child splits. There is
no new retained field, structural element object, payload snapshot, mask owner,
JNI symbol or algorithm implementation. This is a disclosed metadata contract
repair; it is not labeled equivalent to the previously missing metadata.
Ordinary value/callback behavior is checked independently.

The de-duplicated-only reference compiles and fails the new contract with
`AssertionError: sorted properties on every split`; its raw log is retained
under `deduplicated-reference/`. The qualified candidate passes.

## Executed evidence

| Gate | Result |
| --- | --- |
| Exact current owning Java sources, strict Java21 compiler | 268 sources, four duplicate errors |
| Candidate owning Java sources, same compiler flags | 268 sources, zero warnings/errors |
| New merged-sequenced contract | 2,517 checks passed |
| Existing strict-sequenced contract | 126 checks passed |
| Existing recursive filtered split contract | 6,324 checks passed |
| Existing navigation reconciliation contract | 154,239 checks passed: navigation 141,840; entries 11,447; read-only 904; deep wrappers 48 |
| Existing ordinary-read callback tape | Exact match with `d11f07d...` navigation-reconciliation body |
| Declared API, private field and method descriptors | All 11 `M3FilteredViews*` class shapes match the compilable de-duplicated reference |
| Generated JNI headers | All 12 headers from the complete reference/candidate module are byte-identical |

The new contract composes Java21 `SequencedSet` refusal, empty/hidden views,
whole/range/double reversal, live owner mutations before split binding,
natural/custom/null-friendly comparator order, actual stored representative
identity, recursively split stream sorting and comparator-admitted middle nulls.
The JUnit wrapper makes this same independent contract discoverable in the
owning module.

Raw `api-comparison.diff` against the historical `d11f07d...` body contains the
already-admitted explicit `SortedSetView.spliterator()` override from PR #1303.
It is not a new callable method: the same method was inherited from the public
SortedSet contract. `candidate-api-parity.json` compares against the current
compilable composition and confirms all declared descriptors and fields match.

## Remaining gates and limits

This packet records local source/compiler and focused actual-owner evidence.
The parent task owns canonical recipe execution, fixed point/refusal,
receiving publication, owning Maven/JUnit/JNI gate and hosted exact-head
workflow evidence. The wrapper atom adds no native implementation and the
header comparison alone does not establish native runtime qualification.

No universal performance improvement is claimed. There is no retained payload
or metadata-field change. The raw compatibility tape proves the named finite
callback corpus, not all possible hostile or stateful predicate behavior.
Source-built OpenJDK replacement, global receiving-phase promotion and complete
five-provider library acceptance remain outside these operation-specific
results. NetBeans collection donors require their own category/contract fit;
this repair reuses already-admitted M3 owners.
