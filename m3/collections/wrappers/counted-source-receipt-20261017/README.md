<!-- SPDX-License-Identifier: Apache-2.0 -->
# Two exact M3JDK21 counted-source pin successors

**Canonical source recipe** in Synexia convergence workspace:
`com.synexia.m3.M3Jdk21CountedSourcePin20261017`.
**Product receiver**: M3JDK21 PR #1031, stacked after the existing
filtered Map/primitive AVL and exact JNI native-path source receipt.

## Source chronology, not last-writer-wins

The original `M3HashPinnedTextSnapshotRecipe` provider Git blob
`2db1257a40d21be06ba2a67da87aff0311183809` was previously hardcoded
in `.github/workflows/m3-count-totals.yml`. Because the source review
selected one exact additional first-party JNI C owner path, the reviewed
provider blob is now `8bd6f399b298f364a41d53391ba2e72da4c5c635`.

Its receiving change, and no other change to the counted recipe owner,
sets `m3/collections/counts/recipe` Git tree identity to
`5e6f70feac19cd0f3a9e763656248b22c4192d0b`
(from `45d5b77bc9b18e02922eb0e69ef246a8bdff05c4`).
`m3-counted-continuation.yml` pins this tree; it must be reconciled
to the new exact identity rather than failing before compilation.

## Single explicit text-recipe change

The existing class-backed Maven/OpenRewrite `M3HashPinnedTextSnapshotRecipe`
receives exactly two reviewed workflow YAML files in a sorted
path->SHA-256 manifest:

- `.github/workflows/m3-count-totals.yml`: one provider Git blob literal
- `.github/workflows/m3-counted-continuation.yml`: one recipe subtree literal

No other lines are modified; no `if:` guard, step, JNI command, compiler,
test or fixed-point check is skipped, relaxed or deleted. The test
`M3Jdk21CountedSourcePinRecipeTest` executes actual `Recipe.run`, proves
exact after-text, fixed point, partial delivery, missing/drift refusals
and verifies both before/after authority values.

## Receiver and qualifying tests

Mirrored resources and JUnit live in the existing isolated
`m3/collections/wrappers/workflow-pin-20261017/recipe` Maven module.
This avoids changing either `counts/recipe` or `wrappers/recipe`
during recipe development, so it does not recursively invalidate
the Git tree being pinned.

The receiver commits source recipe/template/tests FIRST, then only the
two generated Git workflow blobs SECOND. The corrected counted and
M3JDK workflows still must execute under the new commit. Failure of
the installed-source manifest gate, full JDK/jtreg/JCK or any Java/JNI
contract remains HOLD, not an excuse to bypass verification.

Problem-category and license decisions are in CATEGORY_DECISIONS.tsv.
No third-party challenge source is included.
