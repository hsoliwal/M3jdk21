<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# syntax-index-facts-20261106: hsoliwal/com.synexia M3AST crates applied to jdk.m3.ast.M3SyntaxIndex (recipe SYNTAX_INDEX_FACTS v1)

M3JDK convergence census leaf L004. Owner: `m3/ast/src/main/java/jdk/m3/ast/M3SyntaxIndex.java`. Each donor crate
is a hash-pinned one-file snapshot (manifest: target path, preimage sha256, postimage sha256) on the original
`com/m3/ast` path; the M3JDK owner is that preimage after the naming recipe (`package com.m3.ast` ->
`package jdk.m3.ast`, rule `PKG_COM_M3`, and nothing else), so every crate applies as `EXACT_TARGET`.
Donor: `hsoliwal/com.synexia` develop `ca1aafc4d94f9dd1ee288226dcca070cc60991c9`; every donor path, blob and sha256
is in [PROVENANCE.tsv](PROVENANCE.tsv).

| Crate | Verdict | Owner change | Proof (relocated) |
|---|---|---|---|
| `m3ast-facts-stable-scatter-20261030` | APPLIED (was UNAPPLIED, census 2 hunks / 16 lines) | `java.util.Comparator` import dropped; `FactBuilder.freeze` no longer sorts `Fact` objects by `(node, insertion)`: it counts per node, prefixes `begins[]`, scatters in append order (append order is `insertion` order) with `begins[]` as the cursor and restores the row starts; same five sidecar columns, O(F+N) instead of O(F log F + N); behaviour-preserving | `M3SyntaxIndexStableScatterContract` (+ `M3SyntaxIndexStableScatterTest`): the literal comparator-sort oracle vs the scatter over 20,004 maps; crate figure reproduced: `PASS M3_AST_FACT_STABLE_SCATTER maps=20004 fact_rows_large=300000 assertions=20405713` |
| `m3ast-kind-postings-20261030` (preimage = the postimage above) | APPLIED (was UNAPPLIED, census 4 hunks / 83 lines against master, 2 hunks against crate 1) | one `private volatile KindPostings kindPostings`; `nodesOfKind` keeps the null check first, then builds the lazy immutable CSR lanes once (double-checked, `synchronized (this)`) and answers by `Arrays.binarySearch` over the sorted kind keys with a fresh `copyOfRange` of the source-ordered node range; `private static final class KindPostings` (keys, begins, counts, nodes); first query O(N + K log K + H), later queries O(log K + H) instead of O(N + H) each; no public API change, same results | `M3SyntaxIndexKindPostingsContract` (+ `M3SyntaxIndexKindPostingsTest`): the original linear scan vs the postings over 9,002 maps, collision strings, caller-mutated results, null queries, a 40,000-node corpus, eight concurrent cold readers; crate figure reproduced: `PASS M3_AST_KIND_POSTINGS maps=9002 assertions=13622340 concurrency_threads=8 source_nodes_large=40000` |

No NEC row: both crates map onto the owner exactly. Not ported (recorded in the crates' `NEXT_ATOMS.tsv`, outside this
leaf): primitive `Fact` lanes in `FactBuilder.add`, an earliest-key lookup for `Facts.first`, a parser kind ordinal
in `M3SyntaxTree` (another file), batched JNI postings (no measured benefit).

## Recipe

```
python -I m3/ast/recipe/syntax-index-facts-20261106/apply.py --donor-git <hsoliwal/com.synexia clone> [--root <repo>] [--check]
```

Rule kinds (one block per crate, in manifest stacking order): `EDIT` (the crate's preimage and postimage read as
donor blobs and mapped through `make/m3/tooling/jdk-naming` (`jdk_naming.rewrite_text`, `target_path` for the manifest
path); the owner without its `//FLLDCIM:` lines must equal the mapped preimage, otherwise the recipe refuses; the
postimage is the mapped crate postimage plus one `//FLLDCIM: MRL` anchor per edited declaration naming the crate),
`FILE` (the crate proof relocated into `src/test/java/jdk/m3/ast`: module header, `package jdk.m3.ast`, the declared
type rename, one `//FLLDCIM: RELOCATE` anchor; `body_match` read back from disk), `WRAPPER` (the module's JUnit
entry point, as `M3SyntaxIndexTest` wraps `M3SyntaxIndexContract`). The rules of one file form a chain pinned by git
blob (the first at master `c4e268d495a0`, the base this leaf is built on, each next one the blob the previous rule produces); a file must equal one
image of its chain or the recipe refuses (exit 2) before writing anything. [EDITS.tsv](EDITS.tsv) (file, kind,
crate, before blob, before sha256, after sha256, donor blobs, names, match, hunks, rule) is a fixed point; a second
run reports `changed=0`. No donor path or package appears in the recipe (the naming recipe rewrites `.py`); donor
bytes are read by blob sha after `PROVENANCE.tsv` is checked against the donor tree.

## Oracles

- Crate proofs: the relocated contracts print the crate's recorded figures verbatim (JDK 21.0.9).
- [oracle/differential.sh](oracle/differential.sh) `<before-owner-blob> <out>`: the m3/ast main sources of the working
  tree compiled twice, once with `M3SyntaxIndex.java` replaced by a pinned preimage blob of EDITS.tsv; [oracle/SyntaxIndexDifferentialDump.java](oracle/SyntaxIndexDifferentialDump.java)
  compiled against each and run over the 70 Java sources of `m3/ast` and `m3/algorithms` (jdk.compiler parse ->
  `M3SyntaxTree`, 62,977 nodes, at `BASE_COMMIT`): root digests, `nodesOfKind` for every distinct kind and 7 absent probes (each
  checked against a brute-force scan of `tree.kind(i)`, repeated, and checked for caller-owned arrays), a seeded
  `FactBuilder` workload whose insertion order differs from node order (115,414 facts on 51,803 nodes, duplicate
  keys; every row per node and `first()`), refusals, eight cold concurrent readers. Every differing dump line is
  classified by record type; the differing class files are checked with `javap -c -p`. Both crates are
  behaviour-preserving, so the dumps must be identical: master vs crate 1, crate 1 vs crate 2 and master vs both
  give the same sha256 `ee85584793df...` (55,293 lines) with 0 brute-force mismatches. Mutants prove the dump has
  teeth: a reversed scatter changes 36,188 `F` lines; postings without the key sort, or answering with the shared
  node lane, change 2,426 and 2,788 `KIND` lines (4,986 and 5,688 brute-force mismatches).

## Gates

[GATES.tsv](GATES.tsv); [verify.sh](verify.sh) `<donor-git-dir> <scratch-dir>` runs them end to end. The module's
`mvn package` is red on master at `testCompile` (`M3SyntaxGrowthAgent` imports `jdk.internal.org.objectweb.asm`
without `--add-exports`; 9 errors, all in that file); the main build runs with `-Dmaven.test.skip=true` and the
tests through `javac` with the pom's flags plus that one export and the JUnit console. Lessons:
[RECIPE_MEMORY.tsv](RECIPE_MEMORY.tsv).
