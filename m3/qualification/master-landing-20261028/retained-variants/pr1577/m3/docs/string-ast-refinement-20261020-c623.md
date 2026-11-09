# M3JDK21 String, AST and collection refinement — 2026-10-20

SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0

## Product and canonical source

M3JDK21 is the integrated product. The Synexia convergence workspace owns the reusable
recipes, original preimages, donor comparisons, rejected experiments, independent oracles
and qualification records. Receive product files through actual OpenRewrite Results from
the existing hash-pinned Java and text recipes. Do not hand-patch receiving postimages.

Canonical review: https://github.com/hsoliwal/com.synexia/pull/12576
Product review: https://github.com/hsoliwal/M3jdk21/pull/1577
Canonical crate: `synexia-openrewrite-recipes/recipes/m3-string-ast-refinement-20261020-c623`
Named recipe: `com.synexia.m3jdk.StringAstRefinement20261020C623`

## Included work

The additive combination retains both histories from source-span PR #1548
(`eef7653d42f7477c9b77fbd6fd94af495715ebf4`) and collection/JNI/AST-scope PR #1546
(`9a87e78465ea2ae4f5c8a74d429c5b68111608e1`).
Their changes since common ancestor `8a3540d43c25ce92dafe2dd6b3f3e61ee7783dda`
are disjoint: 35 paths and 125 paths, respectively. Canonical COMBINATION.tsv records
the exact incoming blobs and modes. This preserves predecessor work and evidence;
it does not claim a new review or runtime qualification of every inherited file.

This successor refines three existing production files:
- `M3Ast.preOrder` returns its fresh working array when it is exactly filled,
  retaining the trimming copy otherwise. Each invocation still returns a detached array;
  canonical subtrees retain every expanded occurrence in traversal order.
- `m3_index_mask_compactor.c` skips three local identity-compaction loops when
  accepted count equals the enclosing selected span. Strictly increasing accepted
  offsets then satisfy `offset[i] - first == i`. JNI calls, regions, two-pass state reads,
  validation, allocations, exception checks and nine exported signatures are unchanged.
- `java.lang.M3StringImmediate` replaces eight em dashes in FLLDCIM comments with
  ASCII hyphens after a real macOS JDK build failed source decoding. Javac syntax trees
  are identical. No expression, declaration or immediate-owner representation changes.

The inherited DAG selftest's two-edge assertion failed on its exact original source.
The fixture has one branch-to-leaf edge and two root-to-shared-branch edges at different
ordinals. The corrected test requires three edges and verifies endpoints and ordinals.
Production DAG semantics are preserved.

## String helps AST through its existing owner

The actual path is `M3ASTIngress(M3File) -> M3ASTIndex.source -> M3File.text -> String`.
String inputs preserve their original reference; other CharSequences snapshot once.
Existing M3String range projection can retain that source's physical owner and offset.
The new source-owner test covers original and nonzero-offset sources, composite-derived
slices, Latin-1, wide UTF-16, surrogate units, empty/full ranges and first kind references.
No second character store, synthetic owner registry or arbitrary predicate cache is added.

The numeric `M3AstSourceSpans` API does not hold a String or resolve an owner and explicitly
does not extend owner lifetime. Its current numeric guards also exclude some actual packed
representations. An owner-bearing sidecar construction contract remains a separate design
and qualification boundary; numeric IDs alone are not lifetime handles.

## Catalogue and selection

Search `m3/collections/algorithm-catalog/string-ast-refinement-20261020-c623.tsv`
by problem category, owner, decision, evidence or license. It extends the existing TSV
catalogue convention. Existing Java catalogue APIs do not automatically ingest this file.
The canonical CATALOGUE.tsv retains the same comparison records and actual proof paths.

Existing Synexia live filtered/sorted wrappers and primitive index projections are reused.
Liveness, optional mutations, callback evaluation, comparator ties, stored representatives,
iterator behavior and backing bounds remain owner-defined. A materialized projection
cannot silently substitute for a live view.

Two primitive exact-kind hash indexes passed correctness checks but failed cost admission.
The bounded linear `M3ASTIndex` remains byte-identical at SHA-256
`4563fcc9ea0a1572f9e0d8040f4980adf40feec4d85b3d0e30d14906b4672fc2`.
Both rejected sources and measurements remain canonical so future passes reuse the refusal.

GitHub Guava, OpenJFX and fastutil implementations are pinned contract/algorithm references.
LeetCode, HackerRank and GeeksforGeeks entries identify relevant problem categories and
contract gaps. No external implementation body was copied in this successor.
Retain donor licenses and notices before any later adaptation.

## Executed focused evidence

- Independent preorder oracle: 343453 checks, 605 roots and 21074 occurrences in each
  baseline/candidate normal/interpreted run. Public/protected javap descriptors match.
- Corrected universal AST/DAG test: 1566 checks; existing source spans: 24068 checks.
- At 8191 expanded occurrences, three allocation trials measured 65688 to 32904 bytes
  per call, saving 32784 bytes. This is a workload-specific diagnostic, not general speed.
- Exact-kind oracle: 78538 checks per execution, including 4096 equal-hash names,
  zero hashes, UTF-16 edge cases, first representative identity and refusal precedence.
- Actual strict C17 JNI: generated javac -h header, nine-export parity, four fresh JVMs
  (original/candidate, normal/interpreted), all with -Xcheck:jni. Three JUnit tests pass
  per JVM with no failures/skips/aborts, including 576 contiguous and 468 one-hole cases.
- Fake-JNI transfer/fault/drift evidence is separate. The retained component receipt
  includes observed stdout and declared compiler arguments; its earlier complete process
  log was not retained. The final real C17/JVM gate has its full raw execution output.
- Source-owner probe: stock Java 27533 checks; existing M3 image 34524 checks,
  1862 source ranges and 1745 physical owner projections, with both CompactStrings modes.
  That image is from a dirty `a9e6ed8a2680+` checkout, and M3StringPool differs.
  It is not a rebuild of this combined receiving head.
- ASCII comment repair: zero javac parse diagnostics and identical normalized syntax-tree
  SHA-256 `73155ef0576c4f6af129740205090abf954a9577ff8dd5139d42bec74c8a8f93`.

## Recipe and receiving qualification

The canonical crate uses the unchanged four recipe/scope owners at Synexia
`f04fbac91a7891e64b77c7b24cef1ab75ec88144` and the existing M3TaskRecipeTest.
Manifests pin full before/after bytes and parse contexts. Replay, fixed point, input
permutation, mixed pre/post inputs, missing/drifted/duplicate/wrong representations and
post-scan changes are admission gates, with unrelated decoys required unchanged.

The local filesystem was full. Anonymous memory descriptors carried bytes to actual
JavaCompiler, JUnit assertions, OpenRewrite Recipe.run and JVM/JNI; they did not replace
those implementations. Canonical evidence/sdk retains the actual Result exports.
The four memory-only recipe tests are distinct from the two filesystem tests.
The canonical focused Maven workflow runs all six original tests on an Actions runner.

Portable recipe gate:
```sh
mvn -B -ntp -f synexia-openrewrite-recipes/recipes/m3-string-ast-refinement-20261020-c623/pom.xml test
```

Full receiver admission uses the existing `receive.py` after the complete filesystem
harness succeeds; the memory receipt must not be disguised as full Surefire acceptance.
Current receiving-head images, macOS rebuild, jtreg, full Maven reactor and other platform
gates remain separate. Preserve draft status while those required gates are outstanding.
No global String phase or whole-family migration is promoted by these focused results.
