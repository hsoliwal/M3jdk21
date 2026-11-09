<!-- Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 -->
# M3 wrapper and path-mask convergence

The corrected collection and Java/JNI changes have passed actual source-sealed
receiving qualification at M3JDK21
`2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`. The
[catalogue acceptance record](catalogue/FINAL_RECEIVING_STATUS.json) binds **29
runtime/test source changes**, the separate three-file verification packet, and
the complete 337-input proof closure to actual SDK, receiver, module, API and JNI
receipts. The collections module passed **255 tests with no failures, errors or
skips**. All named runtime contracts and final source rechecks passed.

The original 28-file r2 dataset remains sealed against `07ba6e383343d32c83cfdaf641387bbe72bf325d`.
Those preimages still match the newer receiver. A separate one-file long-mask
bridge is sealed against the newer 2f revision. The initial rejected 26-file
dataset and its exception-swallowing fallback remain historical evidence and
are excluded from promotable inputs. The original 56-file catalogue freeze is
preserved byte-for-byte under `catalogue/history/candidate-56-20261018/`.

## Reused Synexia invariant and recipe

The user-required M3 invariant is the existing
[Synexia recipe-first invariant](../../../docs/M3-SCALE/invariants/SYNEXIA-LLM-RECIPE-FIRST-2.md):
maintain reusable, validated recipes; refine source files serially while
preserving their public contracts; compare algorithm choices through the
problem-category catalogue and donor evidence; retain failed candidates and
their regressions. Behavioral atoms may be replaced independently when the
composed contract remains established. The existing
[canonical ownership invariant](../../../docs/M3-SCALE/invariants/SYNEXIA-M3JDK21-CANONICAL-OWNERSHIP-1.md)
continues to govern the handoff and receiving phases.

The task uses the existing
[`m3-collections-contract-refinement-20261017`](../m3-collections-contract-refinement-20261017/README.md)
crate and recipe `com.synexia.m3jdk.CollectionsContractRefinement20261017`.
`M3HashPinnedJavaSnapshotRecipe` and `M3HashPinnedTextSnapshotRecipe`, the sealer,
and the receiver remain unchanged. No new recipe engine, parser, compiler,
catalogue search service or Synexia runtime dependency is introduced.

| Dataset | Targets | Qualification | Actual receiving |
| --- | --- | --- | --- |
| `m3jdk21-wrappers-r2-20261018` | 28 Java/JNI/test sources | 5 SDK tests; 8 sealing and 9 receiver cases | First 28; repeat 0 |
| `m3jdk21-long-bridge-20261018` | 1 existing Java facade | 5 SDK tests; 8 sealing and 9 receiver cases | First 1; repeat 0 |
| `m3jdk21-wrapper-proof2-20261018` | 3 verification text files | 5 SDK tests; 8 sealing and 9 receiver cases | First 3; repeat 0 |

From the canonical Synexia repository root, using the recorded Java 21/Maven
toolchain and available dependency mirror, the existing serial invocation is:

```sh
cd synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017
M3_TARGET=/absolute/m3-checkout
for dataset in m3jdk21-wrappers-r2-20261018 m3jdk21-long-bridge-20261018 m3jdk21-wrapper-proof2-20261018; do
  mvn -B -ntp -Dm3.dataset="$dataset" -Pcontract verify
  python3 receive.py --dataset "$dataset" --target "$M3_TARGET" --check
  python3 receive.py --dataset "$dataset" --target "$M3_TARGET"
done
```

The existing POM owns dependency versions. The recorded execution used its
offline cache with `-o` and `-Dmaven.repo.local`; other environments must provide
the corresponding approved dependencies. A target must match exact preimages
or admitted postimages. Drift requires a separately sealed successor. Actual SDK
Results authorize receiving; the candidate directory alone does not.

The final canonical commit is recorded externally in the receiving
`m3/collections/verification/wrapper-convergence-20261018/SOURCE_PIN.json`, so the
core packet never tries to contain its own eventual commit hash. Catalogue
metadata is a separate TEXT receiving step, owned by the same generic recipe.

## Review and measured result

Start with [catalogue/README.md](catalogue/README.md), the
[29-file receiving review](catalogue/FINAL_RECEIVING_FILE_REVIEW.tsv), and the
[execution index](catalogue/runtime-proof/README.md). The unchanged original
28-file seal and 25 candidate records remain accessible beside actual execution
evidence. R2 SDK input/result manifests, successful XML, Maven log and
refusal/fixed-point receipt are retained in the catalogue; readers do not need
an uncommitted build output directory to audit them.

The corrected navigation avoids comparator retry and swallowed opaque-owner
exceptions, proves stored-null presence at any comparator position, and retains
original visibility/predicate traversal. Read-only boundaries and deep private
filter chains are qualified. Int/long masks share one Java selection kernel and
one JNI region gather owner; the newer long facade keeps its own validation and
bounded-window contract. Existing public/protected APIs remain, with the four
classified additive members recorded explicitly.

For the exact 32,750-entry sorted-direction workload, the unchanged allocation
proof measured 262,064 before and 131,048 after invoking-thread allocated bytes
per call across three fresh JVMs per lane, with three 100-call samples each.
The 131,016-byte difference (49.9938946%) applies to that allocation workload.
There is no elapsed-time, CPU, JNI, retained-heap, GC or broad performance claim.

## Admission and runtime limits

Bounded navigation enhancements admit only reviewed exact JDK tree/skip-list
owners and their JDK-owned views, final M3 tree owners, and known private M3
wrapper chains. Opaque decorators and subclasses retain their original range
operations and exception demand. The local Linux JNI proof uses generated
headers, retained int ABI, additive long ABI, C17/UBSan and actual checked-JNI
execution; independent missing-library refusal also passes. This does not
establish another platform or a native speed advantage.

The **unchanged historical donation verifier remains unexecuted**, including its
exact JUnit 1.12.2 lane. Static historical admission refuses both the exact 2f
baseline (14 source drifts, 185 undeclared Java files) and the received tree
(15, 207), against 113 declared rows; the manifest seal separately passes. Its
guard and manifests have not been weakened or rewritten. Scoped acceptance does
not replace that authority or claim full JDK, jtreg/JCK, whole SDK, arbitrary
concurrency/serialization or broad AST/build completion. The separate status
matrices and receiving phase owners retain those decisions.
