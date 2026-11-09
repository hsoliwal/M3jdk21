<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# cooccurrence-ppmi-20261106: relocate the co-occurrence PPMI top-k into `jdk.m3.nlp.M3CooccurrencePpmi` (Java leaf; recipe RELOCATE_COOCCURRENCE_PPMI v3)

Plan row `C:/m3-superset/conv/plans/lexicon-index.tsv` rank 6 `cooccurrence-ppmi-topk`, decision ABSORB: master had no
co-occurrence / PPMI owner (`git grep -i -E 'CooccurrencePpmi|Ppmi'` over `m3/` outside `m3/vendor` matched only
`m3/nlp/README.md` and `m3/nlp/pom.xml`; no CONVERGE ledger row names it). This is the **Java leaf**; the optional JNI
selector is a later leaf (below).

## What it is

`M3CooccurrencePpmi.build(documents, window[, limits])` counts directed symmetric-window co-occurrences over caller
tokens (no tokenization, normalization or sentence inference; pairs never cross documents; duplicate positions count
separately) under `Limits` (documents, tokens, vocabulary, distinct pairs, events, window <= 32, token chars) and
returns an immutable `Snapshot`: vocabulary in `String` order, `events`, `window`, `distinctPairs`, `centerCount`,
`contextCount`, `pairCount`, `ppmi` (log form, 0 for missing or non-positive, never NaN), `pairCounts()` (lexical
`PairCount` order) and `topPositiveContexts(center, limit)`: `ContextScore` records with positive PPMI ordered by score
descending, then context ascending, selected by a private primitive heap whose scratch is bounded by the positive
scores (Java path). The packed pair counter is frozen into sorted `long[]` keys and counts.

## Donor and relocation (recipe, never by hand)

| Item | Value |
|---|---|
| donor | `hsoliwal/com.synexia` develop `ca1aafc4d94f9dd1ee288226dcca070cc60991c9` (the `REPO` row) |
| owner | the only copy on develop: recipe template `synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/nlp-ppmi-topk-20261031/CooccurrencePpmi.java.template` blob `6a5cc3ec96a3` (400 lines, sha256 `aa5ba6c8ec67a2e3` = ppmi-topk `manifest.tsv` post-sha256; last written by `ac74366b16e` 2026-10-31); no template markers, used as it is |
| oracles | `recipes/nlp-ppmi-topk-20261031/{PpmiRankingOracle, PpmiSparseOracle, PpmiPositiveOracle}.java` (default package -> `jdk.m3.nlp`), renamed `M3CooccurrencePpmi*Oracle` |
| recipe | `relocate.py` v3 (same code as the backoff leaf's, PR #2544); `LITERAL` rows `synexia.ppmi.nativeTopK -> m3.ppmi.nativeTopK`, `synexia_ppmi_topk -> m3_ppmi_topk` |
| provenance | `PROVENANCE.tsv`: `body_match_renamed=TRUE` for all 4 files |
| receiver | `m3/nlp` `jdk.m3.nlp.M3CooccurrencePpmi` with `Snapshot`/`PairCount`/`ContextScore`/`Limits`. **Rejected:** the classical crate's `TARGET_MAP.tsv` receiver `jdk.m3.tools.text.ppmi` in `m3/tooling/module-packs/cpu-tools` (SKIP row) |

### Native selector: deferred (SKIP rows, nothing copied)

The owner keeps the donor's opt-in shape: `NativeSelection` reads `m3.ppmi.nativeTopK` once (default false) and only
then calls `System.loadLibrary("m3_ppmi_topk")`; `private static native int[] selectPositiveNative(double[], int)` stays
declared. **Every gate of this leaf runs the Java path** (property unset, no library); requesting the native path
without a library fails visibly (`UnsatisfiedLinkError: no m3_ppmi_topk`). The follow-up JNI leaf ports:

| Donor | Target |
|---|---|
| `ppmi_topk_jni.c.template` blob `d49941ca02c4` (`Java_com_synexia_m3_text_ppmi_CooccurrencePpmi_selectPositiveNative`) | `m3/nlp/src/main/native/m3_ppmi_topk_jni.c`, symbol `Java_jdk_m3_nlp_M3CooccurrencePpmi_selectPositiveNative` (JNI + INCLUDE rows of a bitmatrix-v3-style recipe, native build registration) |
| `NativeAllocationProbe.java` `a31ead31e7d4`, `native_alloc_probe.c` `2d7431b76eee` | allocator-injection lanes (donor: 1,021 checks / 200 injected failures per lane) |
| `PpmiRankingOracle --native-boundary` (relocated, not run here) | donor figure 10,000 arrays / 20,011 checks under `-Xcheck:jni` |

Other SKIP rows: the predecessor templates under `before/` (three of them are red witnesses in `differential.sh`), the
experiment `module-info`, `ManifestAtomProbe` and the donor engine `M3GpuToolJoinManifest` (run donor-side only), the
classical crate's `TARGET_MAP.tsv` (rejected receiver), its old template copy and `M3ClassicalNlpOracle`.

## Build and gates (2026-11-06, see GATES.tsv)

| Gate | Result |
|---|---|
| 1 `mvn package`, `-Xlint:all -Werror` | BUILD SUCCESS, 0 warnings, 10 tests (4 + 6), 0 failures |
| 2 oracle figures (per-mode JVMs, donor = relocated) | `RANK_ORACLE_PASS corpora=2500 checks=173501 digest=526d1967...`; `SPARSE_ORACLE_PASS corpora=1500 checks=399004 digest=a0932e65...`; `POSITIVE_BEHAVIOR_PASS degree=3001 positive=1` (heap growth boundaries 0..129, MAX) |
| 2 allocation | ranking 712 B/call (donor 712; preimage red 268,792), sparse 200 B/call (topk-v1 red 90,208), positive 376 B/call (donor 36,200 -> 376; sparse-v1 red 36,200) |
| 2 donor-side manifest atom | `MANIFEST_ATOM_PASS checks=17 refusals=8` (donor figure) |
| 3 differential | 2,000 seeded corpora (seed `0x5eed202611060006`): 1,781 snapshots, 126,056 queries, 70,894 scores; donor = relocated = both `-Xint`, sha256 `d4c83421b5bd175b92402af4b2635211554edd5f173190ccf9d47ae368a8c9d3` |
| 4 `javap -p` | 9 classes, 94 members (1 native) identical under the map |
| 5 untouched classes | 10/10 master class files byte-identical |
| 6 naming | 0 moves, 0 rewrites, 0 refusals; FIXED_POINT 0/0 |
| 7 scope | `m3/nlp/**` only; `m3/pom.xml` untouched |

`bash verify.sh <donor clone> <scratch>` runs them (the module's `verify.sh` calls it). Hard limits are recorded, not
fixed: 16 `for` statements, 2 methods over 15 lines (`topPositiveContexts` 47, `build` 53).

## Files

| File | Purpose |
|---|---|
| `src/main/java/jdk/m3/nlp/M3CooccurrencePpmi.java` | the relocated owner |
| `src/test/java/jdk/m3/nlp/M3CooccurrencePpmi{Ranking,Sparse,Positive}Oracle.java` (+ `*Test` wrappers) | relocated oracles, surefire through three JUnit wrappers |
| `src/test/java/jdk/m3/nlp/M3CooccurrencePpmiDifferentialDump.java` | reflective dump driver of gate 3 (not a test) |
| `relocate.py`, `RELOCATION_MAP.tsv`, `PROVENANCE.tsv` | the recipe, its map and its pins |
| `differential.sh`, `verify.sh`, `GATES.tsv`, `RECIPE_MEMORY.tsv` | gates and their record |
