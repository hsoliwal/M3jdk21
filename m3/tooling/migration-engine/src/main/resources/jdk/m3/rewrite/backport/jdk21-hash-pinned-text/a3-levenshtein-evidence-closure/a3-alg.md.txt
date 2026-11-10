# A3Alg — algorithm catalogue bridge

Status: M3JDK21 tool-plane evidence and planning contract.

## Purpose

A3 already inventories the JDK source/test corpus, plans every current JEP/JBS/community row, and
applies the retained FILE-local OpenRewrite convergence DAG. The missing bridge is algorithm evidence:
problem catalogues and permissive GitHub donors must be reviewed as reusable algorithm shapes before
any implementation atom is proposed.

A3Alg is the short JDK-adjacent name for that bridge.

A3Alg is tooling. It is not a JDK runtime API.

## Evidence roles

Challenge platforms are reference-only:

- LeetCode: problem taxonomy, constraints, edge cases and expected complexity;
- HackerRank: problem taxonomy, constraints, edge cases and expected complexity;
- GeeksforGeeks: technique descriptions and comparison evidence.

Challenge solution/editorial source is never copied into OpenJDK.

GitHub evidence is separate and license-bound:

- existing OpenJDK code is JDK-owned lineage;
- Apache-2.0 or otherwise explicitly compatible donors may be adapted only after the catalogue
  records the exact repository/license/reuse policy;
- unknown or incompatible licensing is reference-only and grants no source-copy authority.

## Catalogue

The canonical input is m3/backports/ALGORITHM_CATALOGUE.tsv.

One row is one candidate algorithm atom, not one problem solution.

Columns:

    atom_id
    category
    technique
    disposition
    scope
    target_owner
    leetcode_ref
    hackerrank_ref
    geeksforgeeks_ref
    github_ref
    github_license
    reuse_policy
    next_proof

admitted, candidate and candidate-adapted rows require all three challenge-platform references.
Rows that still lack cross-platform evidence remain hold-* or review-*.

## Planning

A3Alg validates the catalogue and writes a normalized review image under:

    m3/build/a3/algorithms.tsv

A3Plan consumes the same validated rows as ALG work alongside JEP/JBS/CAP rows while preserving
the original disposition.

No algorithm evidence row is mutation authority.

## Fast-search examples

The first catalogue seed covers the families already discussed in M3 work:

- exact binary search;
- exponential/galloping range discovery;
- rotated binary-search shape;
- prefix/Z preprocessing;
- KMP/failure-function string matching;
- bounded/fuzzy edit-distance automata;
- Fibonacci search.

Existing JDK owners are preferred. A new atom is admitted only when the repository has a real
contract/owner gap and the independent implementation can be proved against the JDK oracle.

### Fuzzy-search evidence closure

`ALG-LEV` now has the complete tri-platform reference denominator for the unit-cost edit-distance
family: LeetCode Edit Distance, HackerRank Basic Spell Checker, and GeeksforGeeks Edit Distance.
Apache Lucene remains Apache-2.0 GitHub reference evidence for Levenshtein-automaton mechanics.

That closes the evidence gap only. The row remains `REFERENCE_ONLY` and moves to
`hold-contract`: A3 must inventory a real JDK fuzzy/string owner and exact public/internal contract
before proposing any implementation or JNI/native acceleration.

## Recipe-first rule

The Java control-plane change is delivered through one source-sealed OpenRewrite ScanningRecipe.
It owns the exact A3 Java preimages/postimages and additive A3Alg sources/tests.

The recipe must:

1. scan first and fail on preimage drift;
2. generate only reviewed absent targets;
3. Java-roundtrip every reviewed postimage;
4. reach a second-pass fixed point;
5. grant no product-source, donor-copy or promotion authority.

This follows the OpenRewrite scanning-recipe model: inventory is collected before generation/edit
and recipe tests prove intended changes without unnecessary changes.

## Product boundary

A3Alg does not replace:

    configure -> make -> jtreg -> runtime/benchmark

It does not add public APIs, modify HotSpot/JNI code, or claim an algorithm is faster.

A selected algorithm atom still follows:

    evidence
      -> JDK owner/contract inventory
      -> FILE recipe candidate
      -> differential proof
      -> wider-scope admission when required
      -> native/JNI parity when required
      -> OpenJDK build + jtreg
      -> benchmark only when a performance claim is made
      -> serial promotion
