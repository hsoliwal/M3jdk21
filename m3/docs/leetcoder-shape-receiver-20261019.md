<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# LeetCoder shape-to-recipe receiver — 2026-10-19

**State: REVIEWABLE CANDIDATE; JDK PRODUCT ADMISSION NOT COMPLETE.**

Canonical producer: [Synexia PR #12422](https://github.com/hsoliwal/com.synexia/pull/12422).
Canonical recipe: `com.synexia.rewrite.M3LeetCoderShapeDispatchRecipe`.
Exact source-sealed crate: `leetcoder-shape-dispatch-v1`.

This receiver lives in the **M3JDK21 Maven authoring/proof plane** only.
The OpenJDK runtime remains the product owner: no Synexia package is added to
`java.base`, HotSpot or JNI, and ordinary `String`, `Arrays`, `Collection`
and `Integer` public contracts are unchanged.

## Serial file-local protocol

1. Inventory the exact target method/file with A3, and consult
   `m3/backports/ALGORITHM_CATALOGUE.tsv`, `m3/tooling/recipe-catalogue.tsv`,
   and the Synexia challenge/problem catalogue.
2. `REVIEW` uses the sealed AST matcher and emits algorithm-candidate data
   without changing the Java source.
3. `APPLY_CANDIDATE` requires the complete exact source path and SHA-256 of
   the source *before* the edit, and emits at most one candidate/file/generation.
4. The existing `M3ReplaceIntBitCountLoopRecipe` provides the candidate edit.
   Do **not** copy LeetCode, HackerRank, or GeeksForGeeks solutions.
5. The A3 trusted host must certify the candidate with source diff, lint,
   Java 21 compiler, differential JUnit and observable contract checks;
   classloader, reflection, exception and resource effects must stay compatible.
   Then require a fixed point and (for actual JDK modifications) full
   configure/make/jtreg and target runtime parity.
6. Only after evidence can a recipe-generated target patch be reviewed
   and committed to `src/**`. Another atom starts from the new SHA preimage.

The Maven profile below is useful only for Maven-managed source sets it actually
sees; it does **not** by itself scan the whole OpenJDK `src/**` tree.
For product sources, use the existing A3 exact-file candidate route with the
Synexia mastered receipt, then OpenJDK build/jtreg gates.

## Maven profile

The profile is disabled by default. Supply the exact relative path seen by
the Maven module, and optionally select one read-only or candidate pass:

```bash
mvn -B -ntp -f m3/pom.xml -Pm3-leetcoder-shape \
  -Dm3.leetcoder.sourcePath=src/main/java/example/Bits.java \
  -Dm3.leetcoder.mode=REVIEW \
  rewrite:dryRun

mvn -B -ntp -f m3/pom.xml -Pm3-leetcoder-shape \
  -Dm3.leetcoder.sourcePath=src/main/java/example/Bits.java \
  -Dm3.leetcoder.mode=APPLY_CANDIDATE \
  -Dm3.leetcoder.expectedSourceSha256=<exact-64-hex-utf8-sha256> \
  rewrite:dryRun
```

The second command is a **dry-run candidate**, not a JDK product mutation.
It requires a local Maven-accessible build of the qualifying Synexia
`synexia-openrewrite-recipes:1.0.0-SNAPSHOT` artifact. Until that artifact,
JUnit proof, source-hash preimage and A3 mastery receipts are available,
the profile is not an executable qualification claim.

## First shape: BIT_OPERATIONS / population count

The recognizer accepts only the sealed, pure `int` Kernighan and admitted
unsigned-shift forms and delegates to `Integer.bitCount(int)`. Arbitrary
signed conditions (`x > 0`), side effects, comments in the rewritten body,
ambiguous overloads and unfenced targets are rejected.

LeetCode 191 and GeeksForGeeks count-set-bits discuss the technique;
HackerRank supplies bit-manipulation category context, not the same Java
contract. The existing Synexia MIT donor notice remains authoritative.
No challenge/editorial source is copied.

Complexity: `while(x != 0) { x &= x-1; count++; }` performs 0..32 iterations
for a Java int. `Integer.bitCount` is a JDK library operation with
implementation/JIT-dependent performance. **No speedup is asserted without
a benchmark.** No scalar JNI implementation is added because the boundary
may cost more than the operation; batch JNI needs separate size/ABI/GC/
lifetime and CPU-vs-native evidence.

Additional categories (KMP, Aho-Corasick, binary search, pairwise XOR,
sliding windows) stay catalogue-only until exact owner/contract review
and a dedicated structural rule produce verified candidates.

## Evidence and licensing

- Target-specific import: `EXTERNAL_RECIPE_TOOL`.
- Synexia Apache-2.0 code remains in its separate recipe JAR;
  OpenJDK sources remain under their existing per-file licenses.
- Compiler/JUnit/fixed-point proof is not replaced by catalogue matching,
  Source SHA-256 equality or a passing parser.
- Deterministic target integrity gate:
  `python3 m3/tooling/a3/verify_leetcoder_receiver.py`.
- Status: `PENDING_SYNEXIA_PR12422_CI, PENDING_A3_RECEIPT,`
  `PENDING_JDK_BUILD_JTREG,` and `PENDING_BENCHMARK`.
