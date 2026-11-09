<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# M3JDK21 source-facts/collection refinement

M3JDK21 is the integrated product. This pass refines its existing immutable source,
compiler-fact, filtered primitive collection and JNI array owners. Synexia is the
canonical convergence workspace for their reusable recipes, algorithm comparisons
and validation evidence.

The exact source and recipe checkpoint is
[com.synexia commit A](https://github.com/hsoliwal/com.synexia/commit/a0d04d5d33b91e58c8cc6a3a2629d8798b4adac4),
reviewed in [canonical PR #12566](https://github.com/hsoliwal/com.synexia/pull/12566).
The [source receipt](source-receipt.json) binds all 18 preceding received paths to
that immutable checkpoint, their exact pre/post SHA256 and Git blob identities,
and the actual SDK/runtime evidence. This README and receipt are subsequently
emitted by the same existing text recipe as dataset `m3-facts-custody-20261020`.
The record contains no future-commit or self-referential seal.

## What the existing owners now do

| Owner | Refined behavior | Contract retained |
| --- | --- | --- |
| `M3File` | Compute one character seal lazily for an immutable source and safely share it across calls and threads. | Exact UTF-16 identity remains distinct from original byte identity; existing public methods and source snapshot ownership remain intact. |
| `M3FileSourceFacts`, `M3JavacASTIndex` | Feed digest bytes in bounded blocks and reuse immutable file facts across declarations and compatible tasks. | Original javac trees, compilation context and synchronous callbacks remain authoritative. Arbitrary resolvers keep event-local evaluation, and changed dependencies invalidate reuse. |
| `M3PrimitiveFilteredSequences` | Flatten known nested wrapper predicate chains into arrays at depth three or greater, preserving a single underlying primitive cursor. | Lazy forward/reverse iteration, repeated `hasNext`, direct `next`, exhaustion, predicate order, exceptions, aliases and mutation owners remain covered. Depth-one/two retain the measured existing paths. |
| Existing native array concatenation | Restore the approved donor's second-pass null refusal and release the unpublished result reference. | Existing JNI signatures, normal output, exception mapping and pending-throwable ownership remain unchanged. |

The immutable `M3File` can consume the exact UTF-16 result of the existing JNI
array owner. The delivered native snapshot proof checks that composition against
actual Java classes and the required native library. Parse-once reuse stays with
the existing compiler source and context owners; no independent parser or AST
store is introduced.

## Composition with existing reviewed work

The receiving parent is `8a3540d43c25ce92dafe2dd6b3f3e61ee7783dda` from
[target PR #1539](https://github.com/hsoliwal/M3JDK21/pull/1539), stacked on
[target PR #1538](https://github.com/hsoliwal/M3JDK21/pull/1538). Their reusable
canonical work remains [com.synexia PR #12543](https://github.com/hsoliwal/com.synexia/pull/12543)
at `236cf96fcc288d3aa9c2f3223f6388831685bac8`.
The 51 inherited paths include sorted/filtered wrapper, array/mask, parser,
build-model and filesystem refinements. This pass reuses them. Its new runtime
paths do not overlap those inherited paths; two metadata catalogue files append
to the composed parent. Exact source preimages for this pass remain identical to
master `d6d76b563050fc13c4fa79bc494edf29f1614b37`; final metadata uses the composed
parent's own preimages.

## Executed qualification

| Gate | Result |
| --- | --- |
| Each sealed source/metadata dataset | Five actual OpenRewrite SDK tests, eight input-sealing tests, nine receiver integrity cases; real SDK outputs received and fixed point zero. |
| Primitive cursor differential | 40,764 paired shallow observations, equal callback trace, 164,104 existing checks on each version; 39 deep checks pass where the original overflows at 20,000 nested filters. |
| Composed owning collections | 469 Java tests with three expected native-only skips; 469 required-JNI tests with zero skips; no failures or errors. Ten source dependencies, ten native libraries, four C fixtures and 22 explicit native contracts. |
| Actual received target source facts | 1,381 assertions; file/concurrent/frozen-view/repeated-task/arbitrary-resolver digest counts 1/1/1/0/3; eight block updates. |
| Exact current canonical counterpart | 1,529 received assertions against 1,526 original assertions; all 84 unchanged dependency sources are exact current canonical blobs. Seven public class descriptors match separately in each namespace. |
| Actual received Java/JNI snapshots | 51 snapshots, 1,531 assertions with `-Xcheck:jni`. |
| Received native exceptional paths | C17 ASan/UBSan fixture passes 21 cases and 4,966 checks; original second-pass bare-null witness fails as expected. |
| Required-native array owner | 13 tests, zero failures/errors/skips. |

The immutable
[canonical evidence overview](https://github.com/hsoliwal/com.synexia/blob/a0d04d5d33b91e58c8cc6a3a2629d8798b4adac4/synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017/evidence/refinement-20261020/README.md)
links the raw logs, source closures, API descriptors, SDK patches and rejected
candidates. `source-receipt.json` supplies exact hashes for the key proof records.
Finite allocation/CPU observations informed the selection: the depth-two array
plan was rejected for extra allocation, the selected depth-eight traversal has a
CPU regression in its sample, and the digest buffer increases first-use scratch
allocation while eliminating repeat immutable-file digest work. These results
support the documented selections without asserting a universal speedup.

## Reproduce the source and native proofs

Use an actual Java 21 installation. The native scripts currently target Linux
and require a C compiler with the sanitizers used by the scripts. From the
M3JDK21 repository root:

```sh
export JAVA_HOME=/path/to/jdk-21
export M3_PROOF_JAVA_HOME="$JAVA_HOME"
bash m3/ports/compiletime/qualification/source-facts-reuse-20261020/run-proof.sh
bash m3/ports/compiletime/qualification/source-facts-reuse-20261020/verify-native.sh
bash m3/ports/compiletime/qualification/source-facts-reuse-20261020/verify-jni-failures.sh
```

The existing compiletime workflow has an independent job for these received
source and native proofs. The full owning-module commands and raw execution
records remain in the canonical evidence; the three scripts above are focused
replay commands, not a replacement for the owning collections gate.

For recipe replay, check out canonical commit A and enter its existing crate:
`synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017`.
With Java 21, Maven 3.9.9 and a prepared dependency cache, run each dataset from the
source receipt serially:

```sh
mvn -o -B -ntp -Dmaven.repo.local=/path/to/prepared-cache \
  -Dm3.dataset=m3-source-facts-20261020 -Pcontract verify
python3 receive.py --dataset m3-source-facts-20261020 \
  --target /path/to/M3JDK21 \
  --recipe-owner /path/to/com.synexia/synexia-openrewrite-recipes --check
```

Remove `--check` only when receiving the qualified output; repeat it afterwards
to observe zero changes. The existing receiver checks the actual successful SDK
report and exact input/result bindings before per-file writes. The custody-only
dataset is published after checkpoint A on canonical PR #12566; A intentionally
contains the source recipes and proofs, not this later receipt dataset.

## Reusable catalogue and invariant

Search the existing recipe and algorithm catalogues before each per-file pass:

```sh
rg 'm3-primitive-filter|m3-source-facts|m3-jni-concat' m3/tooling/recipe-catalogue.tsv
rg 'primitive|utf16|digest|cursor|concat' m3/tooling/shared-frontend/algorithm-catalogue.tsv
```

The existing target porting invariant and canonical recipe-first invariant require
reusable validated transformations, serial file refinement, unchanged public
interfaces/contracts/observable behavior, and independently replaceable semantic
atoms whose composition preserves the contract. There is no arbitrary internal
atom limit. Retain candidate correctness, compatibility, complexity, resource
observations, donor licenses, failures and selection rationale so later passes
reuse the accumulated comparison.

The catalogue records GitHub, LeetCode, HackerRank and GeeksforGeeks approaches.
The pinned MIT `javadev` nested-iterator candidate is a lazy stack; a real Java 21
witness rejects its repeated-`hasNext`, direct-`next` and exhaustion behavior for
this cursor contract. No challenge implementation body is copied. Native refusal
restores the existing approved Apache-2.0 Synexia donor atom recorded in the source
receipt. First-party changes retain the existing source license notices.

These are scoped source, API, Java/JNI and owning-module proofs. The receiving
phase remains String. A completed source-built integrated image, applicable full
tier/platform gates and the PR's required hosted checks remain separately assessed;
local proof receipts do not assert those wider gates passed.
