<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# M3JDK21 original MIndexAST: filtered and subtree node-ID views

**M3JDK21 is one integrated Java 21 SDK/JDK**, not "M3 + JDK", and
this change is an original product-owner refinement rather than another
parser or AST model.

It extends the existing `jdk.m3.parser.M3ASTIndex` (whose source
originates in MIndexAST) by using its canonical primitive lanes:
`int[]` kinds, parents, first child, next sibling, span offsets,
state flags, and the already-interned per-source kind vocabulary.

## Concrete Java 21 operations

- `selectedNodes(kindOrNull, requiredMask, forbiddenMask)` is a **lazy
  primitive IntStream** over original physical occurrence IDs. No
  `Node` objects, `List<Entry>`, secondary ID array or new Java
  compiler session is constructed. A null kind means all; a missing
  spelling means empty. Flags are compared as full 32-bit values,
  and contradictory masks fail before iteration.
- `forEachSelectedNode(..., IntConsumer)` iterates directly over those
  same immutable lanes, without even a Stream/Spliterator object. Its
  callback, exception and partial-work behavior belong to the caller.
- `selectedSubtree(rootRow,includeRoot,kindOrNull,required,forbidden)`
  follows the **original** first-child/next-sibling/parent lanes in
  depth-first source order, retaining one primitive cursor and no
  DFS stack. It never interprets a numeric row range as a subtree:
  valid input builders may append children of earlier parents after
  nodes in unrelated branches. Traversal stops precisely at its root
  rather than entering the root's next sibling chain. Sequential
  traversal is intentionally not split for parallel execution.
- The original `M3File` source instance, source-generation SHA-256,
  UTF-16 geometry, node IDs and all public original methods remain
  unchanged. No JNI pointer, M3String store or new m3 AST persistence
  schema is introduced.

### Complexity and lean memory

For N original nodes, global filters are O(N), O(1) extra retained
state; direct callbacks avoid a Stream allocation. A selected subtree
takes O(its visited nodes + parent/sibling link traversals), and
O(1) scratch regardless of depth/width. The original primitive lanes
are shared. The `IntStream` and its single cursor object are
request-local and reclaimed after use; no persistent selection cache
or secondary hash map is introduced. Case-specific signatures preserve
the same Java 21 `IntStream` semantics as JDK primitive streams.

Other consumers—Maven, Gradle, OpenRewrite, JDT, Jackson, lint/code
clustering—may query these detached node facts only **after** their
original compiler/parser owner delivers a validated source generation.
This API is *not evidence that independently launched JVMs share live
javac/JDT objects*, or that unchanged `.class` files automatically
prove semantic environment stability.

## Reusable serial Maven/OpenRewrite recipes

Dataset:
`m3/tooling/shared-frontend/recipe-custody/datasets/m3-ast-select-20261021`.

Three source-hash-sealed FILE-local passes, all using **unchanged**
M3JDK21 canonical OpenRewrite recipes, in order:

1. Original `M3ASTIndex.java` additive lean selection behavioral atoms.
2. New `M3ASTSelectionContract.java` with independent seeded oracle.
3. Original parser `build.py` adds that contract to the existing
   lint, compile, test, linked-runtime sequence.

All postimages are exact source blobs; original source preimages are
retained; promotion must refuse drift and reach a fixed point. If
a generated file is wrong, correct the recipe and replay rather
than patch its output by hand. Record MOP recipe-correction memory
only after a real executed guard passes, with exact patch and
receipt identity.

### Reproduction

```sh
root="$PWD/m3/tooling/shared-frontend/recipe-custody"
mvn -B -ntp -f "$root/pom.xml" -Pcontract \
  -Dm3.dataset=m3-ast-select-20261021 \
  "-Drecipe.owner.root=$root/sdk-owner" verify
python3 "$root/receive.py" --dataset m3-ast-select-20261021 \
  --recipe-owner "$root/sdk-owner" --target "$PWD" --check

for phase in lint compile test runtime; do
  python3 m3/tooling/module-packs/parser/build.py \
    --jdk-home "$JAVA_HOME" --out /tmp/m3-selected-ast --phase "$phase"
done
```

The test exercises deliberately interleaved descendant IDs, nested
subtree bounds, exact call ordering, conflicting flags, missing kinds,
callback failure, 260 randomized trees, and a 50,000-child wide tree.
The standalone Java21 test is registered in the original parser
module's real `CONTRACTS` list.

## Current qualification boundaries

**Draft** until hosted Maven/OpenRewrite actual LST replay, exact
source generation, linked JDK21 parser/JMOD runtime, JDT and Maven/
Gradle plugin consumers, JNI ABI compatibility and heap/performance
probes pass. Tests or workflow descriptions alone are not a green
qualification receipt.

The immediately preceding Git merge preserves
`hsoliwal/M3JDK21#1628` original SWT recipe and its separate
`hsoliwal/eclipse.platform.swt#240` in-place Eclipse source receiver.
No SWT class is duplicated in `java.base`; Eclipse continues to own
the original SWT API and native resource lifecycle.
