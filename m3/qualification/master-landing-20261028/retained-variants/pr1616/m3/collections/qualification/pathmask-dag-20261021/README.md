<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 existing path-mask owner: generated Java/JNI refinement

M3JDK21 owns this implementation. This is not an external runtime layer, new
collection store, adapter framework or second recipe engine. The product runtime
has no new Synexia, OpenRewrite or catalogue-service dependency.

## Exact receipt and stack

Canonical recipe/evidence: [Synexia #12651](https://github.com/hsoliwal/com.synexia/pull/12651),
commit `34517b81f0b5a4cd19f2f56b299aeaa41fc78504`.
Recipe class: `com.synexia.rewrite.M3PathMaskDagRefinementRecipe`.
Packet path: `synexia-openrewrite-recipes/recipe-crates/pathmask-dag-20261021`.
Packet Git tree: `f92371cd8d2d912879309dc445017e24c681ce95`.
The canonical repository is private; this product verifier runs independently
and does not need its credentials or a private dependency at runtime.

Receiving parent: `d1b9aa9f35cf6f8cf7af1fb581471c2551bd7e97` from
[M3JDK21 #1616](https://github.com/hsoliwal/M3jdk21/pull/1616).
Both runtime input blobs were read back at that exact parent and matched the
original qualified baseline. The parent retains the filtered/sorted-view,
provider-invalidation and callback-lifecycle improvements. No earlier PR was
merged, rebased or overwritten; its full test counts are not claimed as this run.

| Existing owner | Input Git blob | Generated SHA-256 |
| --- | --- | --- |
| M3IndexPathMasks.java | 1e77b2027aa008aa8aebfeaf4b00a5bb89e5b7bf | 6703eef75122fd9c18dba196249f2b4420030ee712f73f6929a9a32ba07a3b5a |
| m3_index_path_masks.c | 086b49293257fd17cc7fbd1ebbb98460aa677e35 | 3b2aa04fa4d6b228482e661e26b0198145e4ec74d17afc1e563f9bf5391d4011 |

The exact-source recipe produced these files into separate absent outputs.
It refuses unknown targets, stale source and postimage drift; repeated output
replay is a detached fixed point. Product test source is the same independent
oracle as the canonical packet (Git blob `8b9ab6d2d5264d0e248da9afedf717c72121b7d2`).
No renamed public symbols or new JNI ABI require a name-mapping entry.

## Runtime changes and retained contracts

The Java owner avoids a redundant second scan for an empty selection. The same
native owner performs chunked signed-stride gathering, contiguous physical-slot
reads, pure state-mask stages and stable compaction. Scratch capacity 256 is a
buffer budget, not a limit on rows, stage count or behavioral decomposition.

Row encounter order, duplicates, signed IDs, full 32-bit masks, negative strides,
logical-to-physical offsets, detached results, resumable output windows and
input/output alias rejection remain intact. No object payload, per-entry node,
callback or retained heap address is introduced. The existing JNI symbol and
public JVM descriptors are unchanged. The default backend policy is unchanged.

The private native entry now also refuses a null later plane on an empty path
and a later invalid offset hidden by earlier stage rejection. Public Java
preflight already refused these inputs. Concurrent source mutation remains
unsupported; cardinality checks do not promise complete race detection.

## Reproduce the received-source proof

On a Linux JDK21/GCC host, from the repository root:

```sh
bash m3/collections/qualification/pathmask-dag-20261021/verify-received.sh /absolute/new-empty-output
```

The script verifies exact source hashes before compiling the actual product
Java owner and C17 library. It uses generated JNI headers, strict warnings,
fatal UBSan and -Xcheck:jni. It runs 1,620 scalar-oracle fixtures per public mode,
with 1,906,217 assertions, interpreter JNI, defensive-entry and missing-library
refusals. Public descriptors must match the original digest. Raw receiving
output is in `verification.txt`.

The canonical packet additionally retains original-red/candidate-green runs,
old/new Java-C cross-pairings, source-recipe replay, valid DAG root comparisons,
category evaluation and diagnostic resource measurements. Those overlap with
these fixtures; counts must not be summed into a unique-case total.

## Recipe-first invariant and combined coverage

Inventory -> dated additive convergence -> problem-category catalogue ->
Inline -> Atomize -> Patternize -> Comment -> compile/test/ABI gates -> PR.
Mutations remain serial within each file. Keep public contracts, ownership and
observable behavior fixed for admitted inputs; declare and test any malformed-
input tightening. Improve the recipe, regenerate outputs, retain exact failures
and selection rationale. Independent discovery may run in parallel, never bypass
verification. No arbitrary decomposition cap is introduced.

The existing coding-agent pack is Synexia #12579 at
`bb9d69729a5ecd673e8160b83961715d9ea3a7ae`: 101 aliases/27 categories for installed
recipes, file/folder scope, documentation, typed interfaces/annotations, bounded
regex and canonical DAG composition. This task reuses its ownership and records
coverage; it does not pretend to receive or qualify that entire conflicted draft
in the product. Its source conflicts, SDK tests and target-specific application
remain separate gates. Existing filtered/sorted wrappers stay with their owners;
a detached mask selection is not a substitute for live-view mutation semantics.

## Limits and licensing

Fresh proof is for the exact shared kernel, not the whole SDK or collection
family. Full Maven/OpenRewrite/reactor/lint/provenance, full collection/provider
matrix, source-built JDK/jtreg/HotSpot/GC, Windows JNI, independent peer review and
broad resource qualification are NOT_RUN. Maven was absent on the authoring host.
Keep this PR draft. No WSL dependency or active String phase promotion is added.

Existing first-party Apache-2.0 owners and notices are retained. New recipes,
proofs and documentation carry Hitesh Soliwal/Synexia copyright. External problem
platforms supplied category references only, not copied editorial code. No
OpenJDK source is relicensed.
