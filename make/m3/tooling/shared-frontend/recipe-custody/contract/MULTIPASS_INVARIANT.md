<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 single-authority multi-pass source and JNI recipes

**M3JDK21 is one Java 21 SDK/runtime product, not M3 + a JDK layer.**
This is a build-time, test-time qualification protocol over the existing
M3JDK21 Java/JNI source owners. It never implements a second parser, source
cache, AST, rewrite engine, JNI ABI, String store, or collection hierarchy.

## Mechanical admission and serial per-file execution

The authoritative existing transformations are
`M3HashPinnedJavaSnapshotRecipe` (OpenRewrite 8.17.1 Java LST) and
`M3HashPinnedTextSnapshotRecipe` (native C, headers, Maven/other text).
An aggregate crate seals initial and terminal states. The six-column
`PASSES.tsv` identifies *one original path per pass*, its ordinal, exact
stage crate, resource name, behavioral atom and problem-category catalogue.

For every pass:

1. **Inventory** M3File/M3AST generation identity, source SHA-256 and
   effective compilation environment, retaining the actual compiler task
   and previously parsed Java LST where available.
2. **Atomize** one semantically meaningful original-owner method or JNI
   kernel. Do not subdivide until contracts become unreviewable.
3. **Discover/evaluate** incumbent, Synexia, JDK, Eclipse/Guava/
   Apache/Spring and LeetCode/HackerRank/GeeksforGeeks *patterns* by
   category. Evaluate complexity, memory, API/ABI, exact callbacks,
   Unicode, concurrency and donor licensing. No challenge code copied.
4. **Transform** serially through the original registered OpenRewrite
   recipe, never by editing a generated target after qualification.
5. **Prove** exact hash-pinned preimage, generated postimage,
   fixed point/refusal, JNI symbol parity where native source changes,
   actual bytecode/compiler/ABI tests, and original observable behavior.
6. **Promote** only after the applicable JDK21/HotSpot, jtreg/JUnit,
   Maven/Gradle/JDT/NetBeans and native platform gates are observed.

Scope is selected before compilation. Immutable .class inputs with valid
ABI/environment evidence are reused. The source generation can be parsed
once in its original javac/JDT/NetBeans owner; detached primitive indexed
node facts are retained only if their source and environment seals match.
The Maven/OpenRewrite SDK's candidate parse is *not* evidence of persistent
cross-JVM semantic reuse. It does not authorize reparsing a whole project.

## Two independently exercised proofs

**Real original OpenRewrite SDK.** Existing
`M3TaskRecipeTest.exactReplayFixedPointAndInputOrder` optionally reads the
dataset's `PASSES.tsv`, parses initial input once per Java language lane,
runs each canonical stage on the **same original SourceFile object set**,
checks exactly its nominated Java/text target changes, preserves sibling
file object identity and exact source content, and compares terminal
source bytes with the original aggregate receipt. The normal five existing
test methods and historical datasets' behavior are retained.

**Source-chain/refusal verifier.** `contract/verify_stage_chain.py` adds
no transformation logic. It proves each one-target per-file recipe
continues from the prior state, verifies aggregate seals, UTF-8 and
stage/source path safety, applies no source writes, and admits only valid
global-prefix checkpoints. The verifier supports a *later stage on the
same source file*, Java and JNI C text lanes, new files, unchanged no-op
passes, and deterministic zero-work final-state detection. It keeps a
single O(file count) transient source-generation vector, not one copy
per pass. Thirteen independent fixture tests exercise success, every
intermediate checkpoint, tampering, mixed-generation refusal, duplicate
stage ownership, absent-source mismatches, path kind and symlinks.

`-Pcontract verify` runs the original SDK/receiver, 13 new refusal
fixtures, and the source-chain verifier. `--optional` skips datasets
without a pass plan, preserving historical qualification contracts.
`--allow-checkpoint` is used for authoring; **promotion requires the
terminal state**. A metadata-model fixed point is not a substitute for
the real SDK's original LST fixed-point test.

## Source files and proof gates

Use the M3String dataset, which already owns three ordered one-file
pass stages, as the first concrete instance:

```sh
root="$PWD/m3/tooling/shared-frontend/recipe-custody"
mvn -B -ntp -f "$root/pom.xml" -Pcontract \
  -Dm3.dataset=m3-prefix-cursor-20261020 \
  "-Drecipe.owner.root=$root/sdk-owner" verify
python3 "$root/contract/verify_stage_chain.py" --root "$PWD" \
  --dataset m3-prefix-cursor-20261020
```

M3String itself remains exactly owner + packed coordinate. The original
balanced tuple DAG, weak canonical pool, scalar-run cursors and JNI
compatibility shadow owners remain unchanged. Existing M3Collections
parent-owned dynamic arrays, primitive offsets/indices, bit masks and
filtered/sorted backed views remain authoritative.

This is generalized **qualification capability**, not a claim that every
M3File/M3AST/JDT/NetBeans/Maven/Gradle/Spring/Apache/Guava source has
already been transformed or that a full M3JDK21 image/HotSpot/JNI suite
passed. Product promotion requires actual successful execution, not
only source custody or a queued hosted workflow.
