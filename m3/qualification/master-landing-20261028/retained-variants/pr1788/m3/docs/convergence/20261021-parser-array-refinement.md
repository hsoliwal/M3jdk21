<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Parser, dynamic-array and mask refinement receipt

M3JDK21 is one integrated SDK. This change refines existing Java and JNI owners
through the existing Synexia Maven/OpenRewrite crate. It introduces no Synexia
runtime service or dependency and does not promote an incomplete compiler fact
into authority to skip compilation, reuse outputs or delete files.

## Canonical source custody

The source starting point is `33d2e6ae8048c436bd55b951d9f7dbfab2f3efcb`.
Publication preserves the later master tree at
`7f657d6ca05760af7bee7d4d2d9e14ae91ddd942`; every affected existing preimage was
checked against that tree, and every new path was checked absent.

Canonical recipe owner: `hsoliwal/com.synexia`,
`synexia-openrewrite-recipes/recipe-crates/m3-ast-build-reverse-20261017`, named
recipe `com.synexia.m3jdk.AstBuildReverse20261017`. The four effective engine
sources are unchanged from `408373f4101fdca2fb3625bec8af1234e5fd3e26`.
Each dataset seals its exact engine inputs, receiving preimage, actual SDK Result,
postimage and file mode. New files have explicit absent-before admission.

The canonical handoff is on the review branch
[`m3/parser-array-convergence-20261021`](https://github.com/hsoliwal/com.synexia/tree/m3/parser-array-convergence-20261021/synexia-openrewrite-recipes/recipe-crates/m3-ast-build-reverse-20261017).
Its `evidence/runtime/convergence-20261021/publication-work/PRODUCT_FRONTIER.json` records exact
before/after Git blobs and SHA-256 identities. `REPLAY.md`, `RESULTS.md`, the
catalogue and original gate receipts document execution and its limits. Review
against those content identities; a branch name alone is not source admission.

## Serial per-file atoms

All names below end in `-20261021`. They are independently admitted datasets,
with required same-owner order preserved:

| Datasets | Existing owner and behavior |
| --- | --- |
| `lex-budget`, `lex-token`, `lex-profile`, `lex-reuse` | Reuse private immutable lexical primitive lanes only after ordinary source snapshot/admission; identical String generation, profile and full limits are required. Return a fresh public result and replay scanner work into the original invocation budget. Failed scans are not cached. |
| `lex-enum` after `lex-token` | Reuse a private lazy enum universe for ordinary ordinal lookup; retain enum singleton identity and existing validation/evaluation order. |
| `lex-lookup-probe` | Add a separately executed public token-class allocation probe. It grants no whole-parser speed claim. |
| `views-identity`, `views-state`, `views-bulk` | Represent identity ordinals implicitly, retain detached rank/mask directories, and walk selection words in bounded batches. Preserve explicit JNI materialization when native execution is required. The new rank helper is package-private. |
| `views-sort`, `views-projection` | Bulk-copy only unopposed primitive merge tails; comparator order, stable physical occurrences, ties and failures stay unchanged. |
| `build-impact-scratch-v2` | Allocate BFS queue/visited scratch only after the existing verified-propagation branch is selected. Conservative planning, validation precedence and partial-fact refusal remain unchanged. |
| `path-output-batch` | Buffer at most 128 result indices on the native stack before an output-region write. Input access order, two-pass counting, drift refusal, cleanup, ABI and scalar fallback remain unchanged. |
| `parser-fixture`, `parser-build` | Repair qualification of existing internal-package tests. Compile them into a sealed test-only module patch; keep them out of the product module, JAR and JMOD. Retain all discovered sources, strict lint, original contracts and ordered phases. |

The wrapper behavior repairs have their own receipt and independent PR. They
are not required by these source refinements. Public/protected production
descriptors and native exports are unchanged in the qualified scope.

## Executed qualification

Every received dataset passed the actual unchanged Maven/OpenRewrite SDK:
five Java SDK tests, eight preparation/sealing tests, nine receiver cases, then
receiving check, apply and immediate zero-change fixed point. Target checks
used the received files rather than a candidate-template fallback.

Parser comparisons passed the original 316-check framework contract,
3,613 public differential observations and 39 private reuse checks, with
retention checked separately. The public differential digest is
`899332129c04e127abab5ede11562b780eb21cbd6a195055809c41f8c76de295`.
Strict Java 21 compilation and protected-API comparison passed.

The corrected full parser sequence passed lint, compilation and all host
consumer contracts under the same input seal
`aeccab98888451c996ce05ce960a5b7e8c0d417ba7bb44db1614929a06390959`.
Five real-module build-driver regressions passed. The original split-package
compilation failure remains in the evidence; the new sequence does not rewrite
its status. Linked-image runtime was not run locally because temporary space
was insufficient; the existing parser PR workflow remains its next gate.

Collection qualification includes 2,000 rank/bulk scenarios, 1,800 identity
cases, strict API checks, original generic/primitive sort contracts and actual
JNI paths. The final composed host run passed all 15 focused JUnit tests,
180 rank/native paths and 11,721 live-view checks. This used published JUnit
5.10.2 in a bounded host harness, not the custom product Maven dependency gate.

Native original/candidate comparison used GCC 13.3, strict C17 compilation,
fatal UBSan, actual javac-generated JNI declarations, export comparison and a
real JVM with `-Xcheck:jni`. Both states passed 624 instrumented geometries,
16 injected failures, two count-drift refusals, the original 2,324-check Java
contract and the new 146,501-check Java JNI contract. Their ordered input trace
is identical: `69899107718db95f`.

Build-impact verification passed all four original contracts, 27 real javac
permutation runs and 4,096 additional graph cases (36,897 checks). The public
digest is `ace6bfd333cbd1ebcf5ed4bfb1ecbf1580784531e79b6c388b6f62eede185de1`.

## Measured resource effects and limits

| Workload | Original | Received candidate |
| --- | ---: | ---: |
| Repeated admitted lexical generation, 34,816 UTF-16 units / 6,144 tokens | 247,488 allocated bytes/op | 128 bytes/op warm; 247,552 bytes/op cold |
| Public token-class lookup, 6,291,456 lookups in each fresh fork | 402,653,496 total allocated bytes | 312 total bytes; no per-lookup allocation observed |
| Build no-change or conservative branch, 512 sources / 513 slots / 511 edges | Branch-dependent | 2,608 fewer bytes/op; verified BFS unchanged |
| Eligible native cached-lane output containing `k` indices | `k` output region calls | `ceil(k / 128)` calls, plus 512 stack bytes |

Allocation observations used three fresh JVM forks with retained shapes,
checksums, warmup/repetition counts and raw samples. These are resource results
for the recorded workload, not throughput promises. Cold lexical cost increased
by 64 bytes for the memo; the enum atom did not improve that cold measurement.

Full source-built M3JDK21/HotSpot, full jtreg and JIT/GC/CDS, installed compiler
service discovery, complete dependency/output ownership, real SQLite/JNI
FactsWriter execution, other providers and platforms remain separate gates.
No Gradle-equivalent edited-tree cache, remote Maven cache, arbitrary zero-copy
source slice or complete semantic skip authority is introduced by this receipt.

## Rights and correction memory

Existing Apache source notices and donor modification notices remain intact.
GitHub donors are revision-pinned in the canonical catalogue. LeetCode,
HackerRank and GeeksforGeeks supply problem categories only; no challenge
solution or editorial body was copied. Rejected candidate behavior, the failed
first impact-test layout and every corrected qualification epoch remain in the
canonical packet. The existing Synexia catalogue/invariant is extended; no
second transformation engine or runtime owner is created.
