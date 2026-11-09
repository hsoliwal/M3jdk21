<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# M3JDK21 integrated Eclipse and MIndexAST continuation

**One product: M3JDK21.** This stacked continuation of
[hsoliwal/M3JDK21#1624](https://github.com/hsoliwal/M3jdk21/pull/1624)
preserves the current native Java/JNI/collections, M3FileTree DOOP,
MOP recipe-memory and original javac/M3AST branches. It combines
the separately reviewed
[M3JDK21 SWT original-owner OpenRewrite recipe #1628](https://github.com/hsoliwal/M3jdk21/pull/1628)
as a two-parent Git merge, without copying an additional SWT runtime
implementation into java.base. SWT owns the original
[direct product receiving PR #240](https://github.com/hsoliwal/eclipse.platform.swt/pull/240).

The direct Eclipse pattern is to **rewrite the original Eclipse module
internals** using M3 physical indexes, parent-owned array lanes,
offsets, bit masks, sparse resident facades and lazy traversal. Public
SWT/JDT/JFace/Resources/Equinox API, ABI, disposal, event order,
accessibility and native handle lifetimes remain authoritative.

## Scope: one additional Java21 AST behavioral-atom family

Original `jdk.m3.parser.M3ASTIndex` now exposes three additive,
source-owned API methods:

- `selectedNodes(kindOrNull,requiredFlags,forbiddenFlags)` — lazy
  `IntStream` over existing physical node IDs. The original kind
  spellings and bit lanes are used in place: no new parser, AST object
  model, secondary array/dictionary or retained String/Node copies.
- `forEachSelectedNode(kindOrNull,required,forbidden,IntConsumer)` —
  direct original-row callback without allocating an IntStream.
- `selectedSubtree(rootId,includeRoot,kindOrNull,required,forbidden)`
  — true depth-first traversal using existing `first/next/parent` int
  lanes and **one primitive cursor**, no stack, no node-list copy,
  no guessed numerically contiguous subtree.

A missing kind returns no selected IDs. Null kind selects all.
Conflicting required and forbidden flags are rejected before work.
The full int mask is compared without truncating its sign bit.
Source and semantic-binding identities are still validated by
their original compiler/parser owners: changing `.class` ABI,
classpath/module graph, processors or toolchain may require
re-attribution but does not justify parsing unchanged source again.

This is a **read-only API extension** on the original immutable MIndexAST
lineage, not a separate M3/JDK or Eclipse compatibility framework.
The original M3File bytes and node IDs remain unchanged; no JNI/native
source or symbol is modified.

## Qualification, MOP and donor ledger

Recipe dataset:
`m3/tooling/shared-frontend/recipe-custody/datasets/m3-ast-select-20261021`.
Three **serial, FILE-local** named source-sealed existing
`M3HashPinnedJavaSnapshotRecipe`/`M3HashPinnedTextSnapshotRecipe`
passes refine only:

1. `M3ASTIndex.java` original indexed selection atoms.
2. `M3ASTSelectionContract.java` additive Java21 differential.
3. `build.py` original parser test/linked-runtime registration.

The algorithm catalogue evaluates primitive mask filtering, stackless
DFS over parent/sibling arrays, direct `IntConsumer`, and
JDK `IntStream`. LeetCode 144 preorder-traversal, HackerRank and
GeeksforGeeks act **only as problem-category discovery donors**:
no third-party challenge code is imported. The original M3JDK21
Apache-2.0 source and Eclipse EPL/JDT source notices remain intact.

The test proves original parent/child DFS in intentionally interleaved
physical node ordering, masked selections, callback errors, source
identity and unchanged retained primitive index, 260 seeded random
trees, and a 50,000-child wide tree. It runs inside the **original**
parser build `CONTRACTS` sequence with Java21 strict source checks.
MOP improvements belong in canonical append-only `RECIPE_MEMORY.tsv`
only after actual recipe/guard evidence has passed. Unqualified
discoveries stay in `RECIPE_MEMORY_CANDIDATES.tsv`.

## Git source conservation across the combined superset

The previous five-donor Git verifier is retained *unchanged*. Instead
of loosening its original admission policy to accept unrelated future
edits, the CI reruns it at original commit
`d7b9e65241b717a387b25234df4cf21531f07d9f` in a detached
temporary Git worktree.

`verify_extension.py` independently ensures:
- original SWT recipe parent `285fdace40b124357d603214df4f1398e6c2b0de`
  is in the new Git ancestry and all twelve of its original source
  paths still have identical Git blob IDs and modes;
- exact M3AST `M3ASTIndex`, original `build.py` and new test
  preimage/postimage SHA-256 and Git source identities match
  their canonical Java/text OpenRewrite per-file recipes;
- only known evidence paths and these three new original parser files
  can change after the exact SWT merge parent;
- no JNI, Eclipse API or previous collection owner is replaced.

Reproduce in a complete Git clone:

```sh
python3 m3/qualification/eclipse-ast-extension-20261021/verify_extension.py
scratch="$(mktemp -d)"
git worktree add --detach "$scratch/prior" d7b9e65241b717a387b25234df4cf21531f07d9f
(cd "$scratch/prior" && python3 m3/qualification/unified-superset-20261021/verify_superset.py)
git worktree remove --force "$scratch/prior"

root="$PWD/m3/tooling/shared-frontend/recipe-custody"
mvn -B -ntp -f "$root/pom.xml" -Pcontract \
  -Dm3.dataset=m3-ast-select-20261021 \
  "-Drecipe.owner.root=$root/sdk-owner" verify
python3 "$root/receive.py" \
  --dataset m3-ast-select-20261021 \
  --recipe-owner "$root/sdk-owner" --target "$PWD" --check

for phase in lint compile test runtime; do
  python3 m3/tooling/module-packs/parser/build.py \
    --jdk-home "$JAVA_HOME" --out /tmp/m3-ast-selection --phase "$phase"
done
```

**DRAFT / HOLD**: this source custody is not runtime acceptance.
Real full M3JDK21 javac/JMOD, JDT, Eclipse/SWT/Tycho, Gradle/Maven
plugin reuse, HotSpot/JNI/C11, multi-platform UI, callback identity,
retained heap and source/class ABI-delta correctness remain separate
observed gates. A queued GitHub Actions workflow is not a passing one.
