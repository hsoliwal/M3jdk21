# A3 complete-denominator absorption planning

Status: tool-plane contract layered on A3 (Atomize -> Patternize -> Absorb).

## Goal

A3 must make every released post-JDK21 enhancement visible to absorption planning without changing
the locked Java 21 product contract.

The planning denominator is the union of five evidence families:

1. released JEP catalogue rows;
2. individually inspected JBS seed rows;
3. every commit in the pinned JDK 22..27 GA intervals;
4. community/optional capability candidates;
5. reference-only challenge/search taxonomy rows.

No family replaces another. A seed row may carry deeper human review than an upstream commit row;
a JEP row carries the release/API identity; the complete upstream inventory closes the non-JEP
denominator; challenge catalogues contribute problem taxonomy and edge-case/benchmark ideas only.

## Complete released-change denominator

The authoritative full upstream inventory is produced by the existing:

`m3/backports/inventory.py`

against a complete `openjdk/jdk` checkout containing the pinned GA tags.

Expected released commit counts remain:

| release | commits |
| --- | ---: |
| 22 | 2,384 |
| 23 | 2,355 |
| 24 | 2,562 |
| 25 | 2,678 |
| 26 | 2,611 |
| 27 | 2,358 |
| total | 14,948 |

A3 must consume that exact generated TSV when it is supplied. It must not substitute
`UPSTREAM_CHANGE_SEEDS.tsv` for the complete denominator.

## Plan identities

A3 plan kinds are deliberately distinct:

- `JEP` — released JEP identity/disposition;
- `JBS` — manually inspected seed packet;
- `UPSTREAM` — one exact released upstream commit;
- `CAP` — optional/community capability;
- `ALGO` — reference-only challenge/search taxonomy.

Every row preserves its source disposition. A3 adds only a derived absorption lane.

For `UPSTREAM`, the exact commit SHA is the row identity and donor coordinate.

## Upstream lane mapping

The existing inventory classifications remain evidence, not compatibility conclusions.

A3 maps them conservatively:

- `candidate` -> `DIRECT`;
- `candidate-adapted` -> `ADAPT`;
- `admitted` -> `ADMIT`;
- `review-hotspot*` -> `SYSTEM`;
- `hold-*` -> `HOLD`;
- `reject-*` -> `BLOCK`;
- everything else -> `REVIEW`.

No path heuristic auto-admits a JDK change.

## Challenge/search review

LeetCode, HackerRank and GeeksforGeeks are **problem taxonomy/reference evidence**, not code donors.

The retained catalogue records search/problem families and a fixed serial review order:

```text
LeetCode -> HackerRank -> GeeksforGeeks
```

For each category, reviewers may derive:

- canonical problem shape;
- asymptotic constraints;
- adversarial/edge-case families;
- benchmark distributions;
- candidate algorithm names.

Challenge statement/editorial/solution bodies are never copied into M3JDK21.

Implementation mechanics must come from one of:

1. existing JDK/M3 owners;
2. independently written JDK-owned atoms;
3. license-pinned GitHub/OpenJDK donors whose provenance permits the intended reuse.

The catalogue therefore carries `source_copy_authority=false` for challenge platforms.

## GitHub donor examples for search mechanics

The current search-oriented donor lane may use:

- `openjdk/jdk` — primary JDK implementation/contract donor;
- `apache/lucene` — Apache-2.0 search/index/fuzzy automata mechanics;
- `google/re2j` — permissive regex automata reference where relevant.

A donor reference does not prove semantic equivalence. JDK contract tests remain the oracle.

## A3 CLI

The existing plan command remains backward-compatible:

```text
A3 plan --root <jdk> --out m3/build/a3/plan.tsv
```

A complete-denominator run supplies the generated upstream TSV:

```text
A3 plan \
  --root <jdk> \
  --upstream-inventory <path-to-UPSTREAM_CHANGES.tsv> \
  --out m3/build/a3/plan.tsv
```

When supplied, all upstream rows are appended deterministically to the JEP/JBS/community/algorithm
plan. Missing or malformed supplied inventory fails closed.

## Atomization/patternization boundary

A3 continues to apply OpenRewrite only to explicitly selected Java files under `src/` or `test/`.

The complete denominator changes planning coverage, not write authority:

```text
complete denominator
  -> classify row
  -> discover touched files/dependencies
  -> FILE-local Java candidates
  -> atomize
  -> patternize / IOP
  -> documentation
  -> fixed point
  -> source-sealed recipe crate
  -> wider-scope DAG when required
  -> configure / make
  -> jtreg
  -> runtime / JNI / HotSpot parity as applicable
  -> benchmark only when a performance claim is made
  -> serial promotion
```

Native C/C++/assembly files are inventoried and routed to native/source-sealed tooling; OpenRewrite
is not treated as a parser for those languages.

## Completion rule

A3 planning is complete only when:

- the released JEP denominator is the current release-authority denominator;
- all 14,948 pinned released commits are present in the supplied upstream plan;
- seed/community/challenge rows are retained;
- original source dispositions are not erased;
- every row receives exactly one derived A3 lane;
- no challenge-reference row grants source-copy or promotion authority.

This is preparation evidence only. It is not a claim that all rows are compatible or absorbed.
