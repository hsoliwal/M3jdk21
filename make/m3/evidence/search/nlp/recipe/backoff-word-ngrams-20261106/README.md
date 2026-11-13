<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# backoff-word-ngrams-20261106: relocate the backoff word n-gram model into `jdk.m3.nlp.M3BackoffWordNgrams` (recipe RELOCATE_BACKOFF_WORD_NGRAMS v3)

Plan row `C:/m3-superset/conv/plans/lexicon-index.tsv` rank 5 `backoff-word-ngrams`, decision ABSORB: master had no
word-level backoff n-gram owner (`git grep -i -E 'BackoffWordNgrams|wordngram'` over `m3/` outside `m3/vendor` matched
only `m3/nlp/README.md` and `m3/nlp/pom.xml`; no CONVERGE ledger row names it).

## What it is

`M3BackoffWordNgrams.train(documents, order[, limits])` counts exact word-level conditional n-grams (every observed
token contributes one transition for each history suffix length 0..order-1; no BOS/EOS, smoothing, tokenization or
cross-document transition) under `Limits` (documents, tokens, contexts, transitions, observations, order <= 8, token
chars). The immutable `Model` answers `exactCount(context, next)` without backoff and `rank(history, limit)`: the
longest observed history suffix wins, then shorter suffixes, then the corpus unigram row; `Prediction(token, count,
observed, suffixLength, probability)` records ordered by count descending, then `String.compareTo` ascending. Counts
are exact `long`s (ranking never compares doubles). Frozen rows are primitive lexical/ranked arrays in a reverse
array trie; training uses primitive AVL rows and an array-backed reverse-context trie.

## Donor and relocation (recipe, never by hand)

| Item | Value |
|---|---|
| donor | `hsoliwal/com.synexia` develop `ca1aafc4d94f9dd1ee288226dcca070cc60991c9` (the `REPO` row; the recipe refuses any other revision) |
| owner | the only copy on develop: recipe template `synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/nlp-wordrank-20261031/BackoffWordNgrams.java.template` blob `a995f3bfaab1` (559 lines, sha256 `3a0c497a7cb73131` = wordrank `manifest.tsv` post-sha256; last written by `b027a57284d` 2026-11-05). The manifest's target `synexia-m3-jmods/experiments/com.synexia.m3.text.wordngram` is absent on develop. The template carries no template marker; it is used as it is (transform: none beyond the recipe's header/package/import/TYPE/LITERAL/anchor edits) |
| oracles | `recipes/nlp-wordrank-20261031/{WordRankOracle, WordContextOracle, WordTrainingOracle, TrainingContextIndexOracle, TrainingGrowthOracle, TrainingContextGrowthOracle}.java` (default package -> `jdk.m3.nlp`) and the fault helpers `TrainingArrayFault`, `TrainingContextArrayFault`; renamed `M3BackoffWordNgrams*` |
| recipe | `relocate.py` v3 (v2 of the perplexity leaf, PR #2537, plus the default-package `PACKAGE -` row); `mutants.py` (fault variants and mutants of gate 2c) |
| provenance | `PROVENANCE.tsv`: `body_match_renamed=TRUE` for all 9 files |
| receiver | `m3/nlp` `jdk.m3.nlp.M3BackoffWordNgrams` with `Model`/`Limits`/`Prediction`. **Rejected:** the classical crate's `TARGET_MAP.tsv` receiver `jdk.m3.tools.text.wordngram` in `make/m3/tooling/module-packs/cpu-tools` (SKIP row) |

Held (`SKIP`, nothing copied, blob pinned): the manifest probe `WordRankManifestProbe` (donor recipe engine), the four
predecessor templates under `before/` (the preimage `8d3c6052a7f8` is still compiled by `differential.sh` for the red
allocation witness), the experiment `module-info` template, the classical crate's `TARGET_MAP.tsv`, its old
`wordngram/BackoffWordNgrams.java.template` and its joint smoke oracle `M3ClassicalNlpOracle`.

## Build and gates (2026-11-06, see GATES.tsv)

| Gate | Result |
|---|---|
| 1 `mvn package`, `-Xlint:all -Werror` | BUILD SUCCESS, 0 warnings, 18 tests (4 + 14), 0 failures |
| 2 oracle figures (per-mode JVMs, donor = relocated) | `WORD_RANK_PASS corpora=2000 checks=1071280 digest=2808f144...` (the donor's recorded figures); `LONG_COUNTS_PASS checks=8` (counts > 2^53); context 800 corpora / 1,027,707 checks; training behavior 95,298, layout 1,601,777, overflow 5; training context 93,890 + layout 18 |
| 2 allocation | 752 B/query at 6,000 candidates top-10 (donor: 872); red preimage 220,736 B/query (donor: 220,736) |
| 2 mutants / faults | double-compare, eager-publish, root-backoff, stale-child, unbalanced all FAIL with the donor's messages; both fault variants PASS, also `-Xint` |
| 3 differential | 2,000 seeded corpora (seed `0x5eed202611060005`): 1,784 models, 300,067 queries, 119,067 predictions; donor = relocated = both `-Xint`, sha256 `a3a74a64d2999e5f6e5c4ba0443622fa14959e3e19daf38473717cd342fd9272` |
| 4 `javap -p` | 9 classes, 125 members identical under the map |
| 5 untouched classes | 10/10 master class files byte-identical |
| 6 naming | 0 moves, 0 rewrites, 0 refusals; FIXED_POINT 0/0 |
| 7 scope | `m3/nlp/**` only; `make/m3/pom.xml` untouched |

`bash verify.sh <donor clone> <scratch>` runs them (the module's `verify.sh` calls it). Hard limits are recorded, not
fixed: 19 `for` statements, 8 methods over 15 lines (`train` 45).

## Open items

- The donor's own `verify-training.sh` / `verify-training-context.sh` mutation blocks predate `MutableContextIndex`
  (see RECIPE_MEMORY.tsv); `mutants.py` is the corrected, block-scoped form. The context-index AVL has no rotation
  mutant in the donor oracle set.
- Module/JMOD/jlink lanes of the donor scripts are not reproduced: `m3/nlp` is a plain module jar (`org.m3jdk.nlp`),
  checked by the first leaf's `jar --describe-module` gate.

## Files

| File | Purpose |
|---|---|
| `src/main/java/jdk/m3/nlp/M3BackoffWordNgrams.java` | the relocated owner |
| `src/test/java/jdk/m3/nlp/M3BackoffWordNgrams{Rank,Context,Training,TrainingContextIndex}Oracle.java` | relocated oracles (surefire through the four `*OracleTest` wrappers) |
| `src/test/java/jdk/m3/nlp/M3BackoffWordNgramsTraining{,Context}GrowthOracle.java`, `...ArrayFault.java` | fault oracles and helpers (gate 2c only) |
| `src/test/java/jdk/m3/nlp/M3BackoffWordNgramsDifferentialDump.java` | reflective dump driver of gate 3 (not a test) |
| `relocate.py`, `RELOCATION_MAP.tsv`, `PROVENANCE.tsv`, `mutants.py` | the recipe, its map, its pins, the mutants |
| `differential.sh`, `verify.sh`, `GATES.tsv`, `RECIPE_MEMORY.tsv` | gates and their record |
