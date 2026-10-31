# Sorted SDK recipe scope repair

The canonical `M3SdkSortedContractRecipe` and its M3JDK21 proof mirror could not compile: `selfTest()` declared `file` inside `try`, then referred to it from `finally`. Strict Java 21 compilation of the exact original source reports one `compiler.err.cant.resolve.location` for variable `file` at line 224.

The correction moves only that declaration immediately before `try`. The public class and `main(String[]) throws Exception`, frozen `ATOMS`, source transformations, preimage/postimage Git blobs, refusal rules, and file publication code remain byte-for-byte unchanged. No collection runtime owner or callback contract changes.

## Custody and replay

| Input | Exact pin |
| --- | --- |
| Canonical repository | `hsoliwal/com.synexia@47981356df68c2e12c658d2d6fe3969b695aa2fe` |
| M3JDK21 receiver baseline | `hsoliwal/M3jdk21@a3e88976ee8664d1ef5d0cade96aec1206b2133d` |
| Original Java Git blob | `0ea9e8bca2a3522d037e675a1ae298f35e59a440` |
| Original Java SHA-256 | `4871d83f3c0da5d1bbf0f5157c1a73d5335a8508027d2a69b5e5096ce62673ff` |
| Corrected Java SHA-256 | `fa29e6edd44510564f00e5759f31ab97d595823857a8841f30ae2a1c58bed8b2` |
| Existing Java recipe owner | `M3HashPinnedJavaSnapshotRecipe`, Git blob `3bdeeaea9a212fe0c7d1f544510f87c4cd49eb40` |

The existing `synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017` proof host owns two separate serial datasets: `sorted-recipe-canonical` and `sorted-recipe-receiver`. Each binds one complete Java source, its original preimage, exact postimage, and unchanged canonical engine sources. Both execute the existing class-backed recipe through Maven/OpenRewrite 8.17.1. Candidate source is retained from actual SDK Results.

The original `canonical-wave2-source-custody-20261018.tsv` and `RECEIVER_V3.md` records remain historical evidence; their old source hash is not relabeled as the new source.

## Qualification completed

Each dataset passed all five existing SDK tests: exact replay and fixed point, input-order stability, mixed preimages/postimages, drift/missing/duplicate/wrong-representation refusal, post-scan refusal, and retention of qualified SDK Results. The existing input-sealing suite passed eight tests. The existing receiver-integrity suite passed nine cases per dataset, including no partial write, tampered inputs/Results, failed SDK tests, symlink targets, and preservation of unrelated bytes and metadata.

`verify_sorted_scope.py` additionally passed eleven compiler/runtime commands against the actual SDK-generated canonical source. It retains the exact original compiler failure and a reduced counterexample with the same compiler-stage diagnostic; compiles the corrected source with Java 21 and fatal warnings; checks its public descriptors; executes the unmodified selftest twice and checks temporary-root cleanup; replays the two real, historical V2 atoms to their exact V3 Git blobs; verifies repeated apply/check preserves bytes, modes, and modification times; and refuses a late drift without partially changing the earlier atom.

There is no original binary ABI comparison: the original source does not compile. Public declarations are unchanged in the exact source correction, and generated class descriptors were checked explicitly.

The first offline `mvn -Pcontract verify` attempt stopped before compilation because `exec-maven-plugin:3.5.0` was absent from the reused dependency cache. Its log is retained. Actual `mvn test` SDK executions passed, and both unchanged Python contract programs were executed directly. This is not a claim that the combined Maven profile passed or that the full M3JDK21 build passed.

## Reproduce

From the existing proof-host directory, use Java 21 and run each dataset serially:

```sh
mvn -B -ntp -Dm3.dataset=sorted-recipe-canonical test
python3 contract/test_prepare.py
python3 contract/verify_receiver.py --crate . --dataset sorted-recipe-canonical --recipe-owner ../.. --output target/sorted-recipe-canonical/receiver-proof
mvn -B -ntp -Dm3.dataset=sorted-recipe-receiver test
python3 contract/verify_receiver.py --crate . --dataset sorted-recipe-receiver --recipe-owner ../.. --output target/sorted-recipe-receiver/receiver-proof
```

For the scope regression, provide the exact original Java preimage under the canonical dataset's `pre` resources, the actual SDK-generated canonical source under `target/sorted-recipe-canonical/candidate`, the historical V2 templates from `m3-sdk-families-20261017/recipe/src/main/resources/templates`, an explicit Java 21 home, and a new output directory to `verify_sorted_scope.py --before ... --source ... --v2-templates ... --java-home ... --output ...`. Its output directory contains the minimized source, complete command logs, and `RECEIPT.json`.

The owning legacy recipe's existing Maven compiler/selftest gate remains in place and benefits directly from the source correction. No new permanent workflow is required for this one-file compiler defect.
