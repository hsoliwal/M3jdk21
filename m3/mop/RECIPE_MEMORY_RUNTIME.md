<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# M3JDK21 Machine Oriented Programming — executable recipe memory

**M3JDK21 is the integrated SDK/JDK product, not an M3 + JDK runtime adapter.**
This implements the memory-admission side of the original
[Hitesh Soliwal / Synexia MOP invariant](../SELF_IMPROVING_RECIPES.md):
**Atomize → Patternize → Annotate → Document**, and when a coding agent
corrects a reusable transformation recipe it preserves a proven lesson for
every subsequent agent.

The original M3JDK21 Maven/OpenRewrite 8.17.1 recipe and filesystem/compiler
owners remain the executors and semantic authorities. This module does
**not** introduce another parser, source index, database, JNI symbol,
filesystem, native memory manager or separate coding agent. It is a
first-party, standalone Java 21 source-custody gate.

## Append-only protocol

`com.m3.mop.M3RecipeMemory` implements the exact already-merged
`RECIPE_MEMORY.tsv` schema:

```tsv
utc	recipe	trigger	observed	correction	corrected_by	guard	lesson
```

An agent **reads this memory before editing a recipe**. On correction,
the agent creates a guarded, source-sealed patch and obtains independently
executed compile, regression-test and fixed-point evidence. The memory
writer admits the correction only when the exact patch SHA-256, guard
SHA-256, recipe identity and required three PASS lines match the supplied
proof receipt. It refuses malformed/multiline TSV cells, non-monotonic
timestamps, duplicates, unsafe/symlinked workspace paths and source drift.

The correction marker `SELF@relative/patch#sha256` avoids the circular
problem of embedding a Git commit SHA inside the same commit whose
checksum changes. It is **not** a Git SHA by itself. The Git review/CI
gate must independently check that the patch, guard and appended row
are in the same commit and that the actual commands produced the proof.

Writes take an intra-process synchronized lock and a cross-process
`FileChannel` lock, then use an atomically renamed UTF-8 TSV file.
Previous rows are kept byte-for-byte and validated again after the
append. File locking, atomic replacement and symlink race guarantees
remain provider-dependent; no multi-repository filesystem transaction
or guarantee against an adversary forging an unattested PASS receipt is
claimed.

**Crucial:** A text receipt with PASS lines does not prove those commands
actually ran. The recipe runner/CI must produce that receipt only from
successful, source-pinned executed gates. Corrections that are discovered
but unqualified belong in a separate CANDIDATE ledger and must not be
promoted to this proven-improvement memory.

## Commands

To run all strict Java 21 tests without installing a new framework:

```sh
bash m3/mop/verify.sh
```

The command compiles exactly the real M3JDK21 source and independent
test with `javac --release 21 -proc:none -Xlint:all -Werror`, then
runs the test in both default VM and `java -Xint` modes.

For a compiled classpath containing `M3RecipeMemory`:

```sh
java -cp <classes> com.m3.mop.M3RecipeMemory show   "$PWD" recipes/your-recipe/RECIPE_MEMORY.tsv

java -cp <classes> com.m3.mop.M3RecipeMemory append   "$PWD" recipes/your-recipe/RECIPE_MEMORY.tsv   2026-10-20T12:00:00Z com.synexia.rewrite.YourRecipe   real-gate 'ACTUAL_FAILED_GATE_LINE'   "SELF@patches/your.patch#<verified-sha256>" coding-agent   guards/YourRecipeRegression.java   'Reuse the fixed source/callback boundary'   "$PWD/proof/actual-passed-receipt.tsv"

java -cp <classes> com.m3.mop.M3RecipeMemory verify-append   /tmp/recipe-memory-before.tsv recipes/your-recipe/RECIPE_MEMORY.tsv
```

The receipt is **exactly**:

```tsv
M3_RECIPE_PROOF_V1	com.synexia.rewrite.YourRecipe	<patch-sha256>	<guard-sha256>
compile	PASS
test	PASS
fixed_point	PASS
```

The local self-test deliberately fabricates fixture receipts to test
validation and refusal logic. Such synthetic receipts are **not
production qualification evidence**.

## Executed local qualification and remaining gates

This candidate was compiled on Java 21.0.12.1 with all lint warnings
fatal, and the test completed **27 checks in the normal JVM and
27 checks with `-Xint`**. Cases cover multiple serial corrections,
unchanged memory prefix, duplicate, timestamp and schema refusal,
tampered patches, modified regression guards, failed fixed points,
symlinks/path traversal and concurrent same-recipe admission.

All two Java sources and the `verify.sh` runner are source-sealed
with exact preimage=ABSENT SHA-256 postimages in
`m3/tooling/shared-frontend/recipe-custody/datasets/m3-mop-recipe-memory-20261020`.
The canonical Maven/OpenRewrite SDK uses the unchanged original
`M3HashPinnedJavaSnapshotRecipe` and
`M3HashPinnedTextSnapshotRecipe`. Synexia is donor/canonical
recipe-workspace input, not a runtime dependency.

**DRAFT/HOLD** until the hosted OpenRewrite recipe runs,
same-commit Git admission checks, Maven/Gradle coding-agent
integration and broader product gates are observed. A self-improving
MOP programme cannot be declared completed from this admission kernel
alone. Existing M3File indexed streams, selective M3AST and JNI owners
remain independent review branches.
