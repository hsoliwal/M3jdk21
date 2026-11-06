<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# m3/nlp: statistical NLP atoms

Module `org.m3jdk.nlp` (package `jdk.m3.nlp`), Maven coordinates `com.m3:m3-nlp:0.1.0-SNAPSHOT`, Java 21, no
dependency outside `java.base`. This module is the home of the statistical NLP atoms of M3JDK; its first atom is the
scoped n-gram top-k. The pom and the layout are plain on purpose: the next atoms (below) are added as further classes
of `jdk.m3.nlp`, each relocated by the same recipe shape with its own oracle.

## What it is

`M3ScopedNgrams` keeps exact, bounded word n-gram counts over `long` token IDs (any admitted ID, including signed
packed IDs) for one `Scope` (source, revision, language, generation, token kind): instances never silently combine
corpora or ID generations, and every `observe` / `add` must repeat the scope. `observe(scope, tokens, multiplicity)`
counts every n-gram of one sentence or document up to `maxOrder` (no n-gram crosses a call); `add(scope, gram, count)`
imports a counted tuple; both are atomic on a rejected batch (distinct-tuple, batch-token and overflow budgets).
`freeze()` returns an immutable `Snapshot` with `count(gram)`, `contextTotal(prefix)`, `distinctTuples()`, a
deterministic `entries()` export and `top(prefix, k)`: the k most frequent continuations of a prefix as `Candidate`
records (next ID, count, exact `count / contextTotal` maximum-likelihood probability, no smoothing), ordered by count
descending then signed next ID ascending, through a primitive worst-first heap (two `long[]` lanes, O(k) scratch,
`Candidate` records only for the results). Budgets: `maxOrder` 1..64, prefix length < 64, k in 1..4096.

## Donor and relocation (recipe, never by hand)

| Item | Value |
|---|---|
| plan row | `C:/m3-superset/conv/plans/lexicon-index.tsv` rank 1 `scoped-ngram-topk`, decision ABSORB (master had no owner; `jdk.m3.tools.text.ngram` is char-gram only and is untouched) |
| donor | `hsoliwal/com.synexia` develop `64652588f0d4c23b1f6e0411090b2a796325ae0e` |
| donor files | `synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexScopedNgrams.java` (blob `66a18d7d622a`, 181 lines, sha256 `72a1bf5720106961...`, the sealed postimage of crate `synexia-openrewrite-recipes/crates/m3-ngram-topk-admission-20261104`); the crate's `proof/M3NgramTopKOracle.java` (blob `de4e7fabe240`); `synexia-indexstring/src/test/java/com/synexia/indexstring/MIndexScopedNgramsTopKAdmissionTest.java` (blob `dc7ed3650647`) |
| donor commits | `2b086b597ac` 2026-11-04 freeze contract + baseline; `873c7ba0826` 2026-11-04 primitive heap; `75e0a673230` 2026-11-04 JUnit fixtures; `b35e7a69a01` 2026-11-04 recipe seal + oracles |
| recipe | `recipe/relocate.py` v1 (`RELOCATE_SCOPED_NGRAMS`): `python -I recipe/relocate.py --donor-git <clone> --donor-rev <rev> [--check]` |
| map | `RELOCATION_MAP.tsv`: 3 `FILE` rows, `PACKAGE com.synexia.indexstring -> jdk.m3.nlp`, 3 `TYPE` rows (`MIndexScopedNgrams -> M3ScopedNgrams`, `M3NgramTopKOracle -> M3ScopedNgramsOracle`, `MIndexScopedNgramsTopKAdmissionTest -> M3ScopedNgramsTopKAdmissionTest`), `EXTERNAL org.junit.jupiter.api` |
| provenance | `PROVENANCE.tsv`: donor commit, blob sha, donor sha256, target sha256 and `body_match` per file |
| naming | `python -I m3/tooling/jdk-naming/jdk_naming.py apply --root . --only m3/nlp/`: 0 moves, 0 rewrites, 0 refusals |

The recipe rewrites only the license header (M3JDK style), the `package` line, mapped `import` lines and the declared
type names, and inserts one `//FLLDCIM: RELOCATE` anchor before the top-level type naming the donor blob URL at the
donor commit. Everything else after the package line is kept byte for byte: `body_match=TYPES` for all three files
means the bytes equal the donor's after the three declared type renames (method bodies unchanged), so the algorithm
is the donor's. The recipe is idempotent (second run `changed=0`) and refuses an unmapped package or import instead
of guessing the closure.

## Build

| Step | Command |
|---|---|
| compile, test, jar | `JAVA_HOME=<JDK 21> mvn -q -B -ntp -f m3/nlp/pom.xml package` (`-Xlint:all -Werror`, `proc none`; main on the module path, the test tree on the classpath; surefire runs `M3ScopedNgramsOracleTest` (wrapper of the oracle main) and `M3ScopedNgramsTopKAdmissionTest`) |
| from the control reactor | `mvn -f m3/pom.xml -pl nlp package` (`nlp` is a default module of `m3/pom.xml`) |
| oracle directly | `java -cp target/classes;target/test-classes jdk.m3.nlp.M3ScopedNgramsOracle verify` (`:` instead of `;` outside Windows); prints `M3_TOPK_ORACLE_PASS corpora=384 checks=183900 digest=82f7578e19205ff9f9f798c917f1a7fbc816f26bdd767cad3263daf1f454bc60` and an allocation probe line |
| differential | `bash recipe/differential.sh <donor clone> <donor rev> <scratch dir>` after `package`: compiles the donor class and the donor oracle from the donor tree, runs the donor oracle, replays the oracle's 384 corpora and 10,000 further scoped queries through both classes with `M3ScopedNgramsDifferentialDump` and compares the dumps byte for byte |
| all gates | `bash verify.sh <donor clone> <donor rev> <scratch dir>` |

Consumers depend on the module with

```xml
<dependency>
  <groupId>com.m3</groupId>
  <artifactId>m3-nlp</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

and `requires org.m3jdk.nlp;` in their module descriptor.

## Gates (2026-11-06, see GATES.tsv)

| Gate | Result |
|---|---|
| 1 `mvn package`, JDK 21.0.9, `-Xlint:all -Werror` | BUILD SUCCESS |
| 2 oracle + admission test in the test phase | 4 JUnit tests, 0 failures; `M3_TOPK_ORACLE_PASS corpora=384 checks=183900 digest=82f7578e19205ff9f9f798c917f1a7fbc816f26bdd767cad3263daf1f454bc60` (the crate's recorded digest); allocation probe 888 B/call at 30,000 pairs, k=10 |
| 3 differential donor vs relocated | identical dumps over 60,000 queries (the oracle's 384 corpora + 40 corpora / 10,000 random scoped queries; 2,624 refusals; 64,829 lines, 7,809,200 bytes), sha256 `d41c2b8d625cb09e789daa1daf4079119063518c274c15f33c6a3d03b27ad160` on both sides; the donor oracle on the donor class prints the same `82f7578e...` digest |
| 4 `jar --describe-module` | `org.m3jdk.nlp` exports `jdk.m3.nlp`, requires `java.base` only |
| 5 `jdk_naming.py apply --only m3/nlp/` | 0 moves, 0 rewrites, 0 refusals |
| 6 scope | only `m3/nlp/**` and one `<module>nlp</module>` line of `m3/pom.xml` |

Lessons of the run are in `RECIPE_MEMORY.tsv`.

## What fills the module next (plan `lexicon-index.tsv`)

| Rank | Leaf | Donor | Target |
|---|---|---|---|
| 3 | scoped-ngram-perplexity | `synexia-indexstring/.../MIndexNgramPerplexity.java` (209 lines, 6 JUnit tests, crate `m3-ngram-perplexity-20261031`) | `jdk.m3.nlp.M3NgramPerplexity` over `M3ScopedNgrams.Snapshot` |
| 5 | backoff-word-ngrams | recipe template `nlp-wordrank-20261031/BackoffWordNgrams.java.template` (559 lines, 4 oracles, 1,071,280 checks) | `jdk.m3.nlp.M3BackoffWordNgrams` |
| 6 | cooccurrence-ppmi-topk | recipe template `nlp-ppmi-topk-20261031/CooccurrencePpmi.java.template` (400 lines) + optional JNI selector | `jdk.m3.nlp.M3CooccurrencePpmi` (+ `src/main/native`) |

Each lands as its own relocation leaf with its own `FILE`/`TYPE` rows in `RELOCATION_MAP.tsv`, its own oracle in the
test tree and its own `GATES.tsv` rows.

## Files

| File | Purpose |
|---|---|
| `src/main/java/module-info.java`, `src/main/java/jdk/m3/nlp/M3ScopedNgrams.java` | the module and the relocated atom (Scope, Tuple, Candidate, Snapshot) |
| `src/test/java/jdk/m3/nlp/M3ScopedNgramsOracle.java` | the relocated independent oracle (384 seeded corpora, 183,900 checks, digest, parallel read-only proof, allocation probe) |
| `src/test/java/jdk/m3/nlp/M3ScopedNgramsOracleTest.java` | JUnit 5 wrapper of the oracle main |
| `src/test/java/jdk/m3/nlp/M3ScopedNgramsTopKAdmissionTest.java` | the relocated JUnit 5 admission test (3 methods) |
| `src/test/java/jdk/m3/nlp/M3ScopedNgramsDifferentialDump.java` | reflective dump driver of the differential gate (not a test) |
| `recipe/relocate.py`, `RELOCATION_MAP.tsv`, `PROVENANCE.tsv` | the relocation recipe, its mapping table and its pins |
| `recipe/differential.sh`, `verify.sh`, `GATES.tsv`, `RECIPE_MEMORY.tsv` | gates and their record |

## Relocation leaves after the first

Each further atom of the plan lands as its own leaf directory `recipe/<leaf>-YYYYMMDD/`, as the leaves of `m3/arrays`
do: `relocate.py` (recipe v2, pinned to its donor commit by the `REPO` row of the leaf's `RELOCATION_MAP.tsv`; the
first leaf's `recipe/relocate.py` v1, `RELOCATION_MAP.tsv` and `PROVENANCE.tsv` stay as they are), `RELOCATION_MAP.tsv`
(`REPO`, `FILE`, `PACKAGE`, `TYPE`, `EXTERNAL`, `LITERAL`, `SKIP` and `NEC` rows), `PROVENANCE.tsv` (with
`body_match_renamed`), `differential.sh`, `verify.sh`, `GATES.tsv`, `RECIPE_MEMORY.tsv` and a `README.md` naming the
owner, the donor, the gates and the open items. The module's `verify.sh` runs every leaf's `verify.sh` after its own
gates, on the same package. A leaf touches the module's own files only through this section and that loop, which are
the same bytes in every leaf, so leaves branched from the same master merge in any order.
