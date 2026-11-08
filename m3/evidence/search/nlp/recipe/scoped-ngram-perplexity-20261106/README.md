<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# scoped-ngram-perplexity-20261106: relocate the scoped n-gram perplexity scorer into `jdk.m3.nlp.M3NgramPerplexity` (recipe RELOCATE_NGRAM_PERPLEXITY v2)

Plan row `C:/m3-superset/conv/plans/lexicon-index.tsv` rank 3 `scoped-ngram-perplexity`, decision ABSORB: master had no
perplexity owner (`git grep -i -E 'NgramPerplexity|perplex'` over `m3/` outside `m3/vendor` matched only
`m3/nlp/README.md` and `m3/nlp/pom.xml`, which name this leaf as the module's next atom; no CONVERGE ledger row names it).

## What it is

`M3NgramPerplexity.of(snapshot, alpha, bigramWeight)` binds one frozen `M3ScopedNgrams.Snapshot` (the module's first
atom, PR #2506) to an add-alpha unigram/bigram model with linear interpolation: one shared out-of-vocabulary category,
`unigramProbability`, `conditionalProbability` (bigram/unigram interpolation, uniform bigram mass for unseen contexts),
`logLikelihood(sentence)` (first token by the unigram prior, then sentence-local adjacent pairs, Kahan-compensated) and
`perplexity(sentences)` (empty sentences neutral, an evaluation set without tokens refused). It never learns or
modifies the snapshot; the only derived resident state is the sorted primitive unigram vocabulary. `of` refuses a
non-positive or non-finite `alpha`, a `bigramWeight` outside [0, 1], a snapshot without unigram observations, a
unigram total different from `contextTotal()`, a bigram of a missing unigram and an outgoing bigram marginal that
differs from `contextTotal(word)` or exceeds `count(word)`.

## Donor and relocation (recipe, never by hand)

| Item | Value |
|---|---|
| donor | `hsoliwal/com.synexia` develop `ca1aafc4d94f9dd1ee288226dcca070cc60991c9` (the `REPO` row; the recipe refuses any other revision) |
| owner | `synexia-indexstring/.../MIndexNgramPerplexity.java` blob `a283c73104c3` (209 lines, sha256 `5eb5db3a8f485f37`), written by `6cf3e2bf8bf` 2026-10-31 and restored on develop by the retained-variant merge `153faac6e88`; byte-identical to the sealed postimage `00-MIndexNgramPerplexity.java.txt` of crate `m3-ngram-perplexity-20261031` (manifest post-sha256 `5eb5db3a8f485f37`) |
| test | the sealed partner `hash-pinned-java/m3-ngram-perplexity-20261031/01-MIndexNgramPerplexityTest.java.txt` blob `9526d238f3e7` (manifest post-sha256 `55ab7fd63d35e441`, 7 `@Test`) |
| producer | `MIndexScopedNgrams` -> `M3ScopedNgrams` by a `TYPE` row without a `FILE` row (the module owns it; donor blob `66a18d7d622a`, unchanged at the pin). No `MEMBER` row: `Scope`, `Snapshot`, `Tuple`, the constructor, `observe`, `add`, `freeze`, `entries`, `count`, `contextTotal`, `scope` and `ids` have the same names and signatures |
| recipe | `relocate.py` v2: `python -I relocate.py --donor-git <clone> [--check]` (derived from `m3/nlp/recipe/relocate.py` v1 and `m3/arrays/recipe/bitmatrix-20261106/relocate.py` v3: `REPO` pin, `LITERAL`/`SKIP`/`NEC` rows, `body_match_renamed`) |
| provenance | `PROVENANCE.tsv`: `body_match=FALSE body_match_renamed=TRUE` for both files (only the header, the package line, the anchor and the three declared type names differ) |

### Held (nothing copied, blob pinned in the recipe log)

| Row | Donor | Why |
|---|---|---|
| `SKIP` | develop test `MIndexNgramPerplexityTest.java` blob `44175f074d95` (8 `@Test`) | adds `scorerRetainsOnlyVocabularyCardinality`, written for the residency-refined owner `f5393e070948` (`2f5d834dfb0`); against the restored owner `a283c73104c3` it fails (measured: 8 found, 7 successful, 1 failed) |
| `SKIP` | crate `M3NgramPerplexityRecipeTest.java` blob `f40e3688e3db` | OpenRewrite-SDK proof of the donor's materialization, needs `org.openrewrite`; this recipe replaces it |
| `NEC` | `cognix-nlp/.../M3StatisticalNgram.java` blob `8785e20cda55` (last changed `1b371920542`) | a second perplexity owner in the donor, area OUT: String tokens with BOS/EOS/UNK sentinels over the cognix `LanguageNgramModel`; not ported, equivalence not decided |

## Build and gates (2026-11-06, see GATES.tsv)

| Gate | Result |
|---|---|
| 0 master baseline | BUILD SUCCESS, 4 tests, 10 class files pinned |
| 1 `mvn package`, `-Xlint:all -Werror` | BUILD SUCCESS, 0 warnings, 11 tests, 0 failures (+7) |
| 2 oracle figures | relocated test 7/7; donor sealed test 7/7 on the donor classes; donor develop test 7/8 (SKIP row) |
| 3 differential | 2,000 sentence sets from seed `0x5eed202611060003`: 2,908 scorers, 9,268 refusals, 111,096 queries; donor, relocated, donor `-Xint`, relocated `-Xint` dumps identical, 19,712 lines, sha256 `d06b49516454e7f80f9f5f3b23cd3fa1c8e77e67cede50694078c28f5033c71a` |
| 4 `javap -p` parity | identical under the map, 19 member lines |
| 5 untouched classes | 10/10 master class files byte-identical |
| 6 naming | `jdk_naming.py apply --only m3/nlp/`: 0 moves, 0 rewrites, 0 refusals; FIXED_POINT 0/0; recipe `--check` changed=0 afterwards |
| 7 scope | `m3/nlp/**` only; `m3/pom.xml` untouched |

`bash verify.sh <donor clone> <scratch>` runs them (the module's `verify.sh` calls it after the first leaf's gates).
Hard limits are recorded, not fixed: the relocated owner keeps the donor's 6 `for` statements and 4 methods over 15
lines (`of` 61 lines).

## Open items

- Residency refinement: owner `f5393e070948` (`2f5d834dfb0`, vocabulary cardinality instead of the `long[]`
  vocabulary) with the develop test `44175f074d95` is a follow-up leaf (a new owner body; Chrome single-file
  generation applies if the body is regenerated rather than relocated).
- `M3StatisticalNgram` (cognix, `NEC`): decide equivalence or keep it out of M3JDK.

## Files

| File | Purpose |
|---|---|
| `src/main/java/jdk/m3/nlp/M3NgramPerplexity.java` | the relocated owner |
| `src/test/java/jdk/m3/nlp/M3NgramPerplexityTest.java` | the relocated sealed JUnit 5 test (7 methods) |
| `src/test/java/jdk/m3/nlp/M3NgramPerplexityDifferentialDump.java` | reflective dump driver of gate 3 (not a test) |
| `relocate.py`, `RELOCATION_MAP.tsv`, `PROVENANCE.tsv` | the recipe, its map and its pins |
| `differential.sh`, `verify.sh`, `GATES.tsv`, `RECIPE_MEMORY.tsv` | gates and their record |
