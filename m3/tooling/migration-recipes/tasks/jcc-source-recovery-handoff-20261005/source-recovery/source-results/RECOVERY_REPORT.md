# Source recovery: implementation reproduced, upstream qualification blocked

The recovery implementation is materialized and its final mechanical replay passes. The required upstream regression gate remains blocked: all 67 selected tests ran, 66 passed, and one errored because the static-analysis recipe `AtomicPrimitiveEqualsUsesGet` is unavailable on the admitted runtime classpath. The independent native lane passes 19 tests. These outcomes remain separate; source export, destination acceptance and whole-repository qualification are not admitted.

This is the entry point for the completed recovery epoch. It supersedes the prospective counts and execution scope in the original `RECOVERY_SCOPE.md` and `NATIVE_LANE.md`; those documents remain byte-for-byte historical planning records. The additional contract-framing repair, independent native lane and final mechanical-only replay each have their own frozen scope and evidence. The machine-readable [reviewed result](evidence/current-be92/RECOVERY_RESULT.json) and [E4 input](evidence/current-be92/E4_EXECUTION_INPUT.json) retain the precise outcome and acceptance flags.

## Revision and restoration boundary

The input is `be92c62ece9023b5c33676716a1076d00e26120a`, tree `3c4f32b66633a251ba2c117090830252a7e2da03`. The task deliberately stays on that revision. Later `develop` changes are outside this proof.

The merged source had omitted 1,039 artifacts from the delivered packet: 997 implementation/proof additions and 42 earlier scope documents. All 1,039 are restored with their exact previously delivered bytes and modes. Their original source commit is `d1cf2d81ee74a4a837f78e0cd2e4ccae2b3d4906`. Existing owner changes were reviewed semantically against the merged source, rather than replaced wholesale with earlier templates. The newer Mastery delegation, classloader, contract-surface ownership, source-path policy, parent POM, current parent tests and other retained upstream changes remain in place. [Restoration admission](evidence/current-be92/SAFE_ADDITIONS_STAGED.json) and [current projection](evidence/current-be92/CURRENT_PROJECTION.json) bind these separate roles.

The current proof projection contains 527 input files. Its main compiler list contains 446 first-party source owners: the earlier 443 plus `M3MasteryClassLoader`, `M3MasteryContractSurface` and `PSource`. The current parent context contains 109 paths: 88 first-party sources, 19 RE2/J vendor sources and both declared test classes. These are bounded task inventories, not whole-repository denominators. The 19 designated receiving sources are a smaller mapping subset and are separately [hash-bound](evidence/current-be92/DESIGNATED_SOURCE_OUTPUTS.json).

## Four current source changes

All changes to existing production owners came from actual `M3HashPinnedJavaSnapshotRecipe` Results and writes. The final replay applies these from exact merged preimages, along with two import-only copied-fixture changes.

| Owner | Change and preserved boundary |
| --- | --- |
| `M3HashPinnedJavaSnapshotRecipe` | Adds a private execution-context hook for an explicit ordered parser classpath. A supplied list is type-checked, copied immutably and shared by dependency preflight and target parsing. Explicit empty remains empty. An absent key preserves the merged owner's runtime/default behavior. Existing source/dependency admission, budget, callback, UTF-8/NUL, hash, path, type, metadata and print-fidelity checks remain. |
| `CompetitiveProblemReview` | Changes only the two `Review` comparators to use canonical enum and plan-shape order. Newer constructor aliases and validation behavior remain. The corrected ordering intentionally changes affected digest roots. |
| `ChallengeDonorPassLedger` | Removes the unused `EnumSet` import. The merged helper qualification, validation and donor authority boundaries remain. |
| `M3MasteryContractSurface` | Stops recursively hex-encoding an already encoded child value at two wrapping sites. Length-delimited ASCII child spans preserve boundaries, while the raw UTF-16 leaf/name encoding continues to distinguish lone surrogates and other code units. Composite annotation/array tags become `v3`; the resulting composite fingerprints intentionally change. Outer V2 and scalar/null encodings, sorting, array order, reflection, exception, cancellation locations and the depth boundary remain. |

The fourth change addresses an executed failure. The unchanged current Mastery test accepted a single-child annotation chain at depth 32 and refused depth 33. The original helper repeatedly expanded an encoded child by a factor of four, exhausting the fork before a completed suite report was available. Increasing the heap would not make that representation fit the accepted boundary. The new framing removes that amplification. Its size claim is relative to the observed value tree; it does not introduce a universal allocation bound for branching compressed annotation defaults. The unchanged 15-test Mastery suite and a separately frozen numerical growth control now pass. [Framing review](evidence/current-be92/contract-framing-review/REVIEW.md) and [actual unit review](evidence/current-be92/execution-static-review/current-units-epoch02-review/REVIEW.md) record that scope.

The copied observation campaign changes only one wildcard import into seven explicit imports. The copied Documentation fixture changes only four wildcard imports into explicit imports and the reviewed separator. Both canonical upstream fixture bodies remain untouched, and their original lint failures remain preserved. These copied targets are outside the 527-source/context denominator.

## Actual results

The table reports execution invocations per lane. It does not add overlapping Java/native methods into a count of unique tests. T03 does not rerun the behavioral suites; it checks the applicability of their exact retained source, profile, resource, tool and artifact evidence.

| Lane | Actual outcome | Scope |
| --- | --- | --- |
| Current baseline compilation | PASS | Exact 446-source current baseline, pinned JDK 21 and strict first-party compiler flags. |
| Repaired candidate compilation | PASS | All 446 sources; final replay reproduces the complete 1,446 classfile set and bytes. |
| Compiled API comparison | PASS | Three changed public owner families: 31 owned classfiles, 24 outward types and 277 public/protected members. The fourth helper has no outward type; its package signatures remain equal and one private helper is added. |
| Parser T02 baseline | 43 executed: 30 passed, 13 failed | Frozen controls reproduce the intended missing-hook behavior. No unrelated baseline error or skip. |
| Parser T02 candidate | 43 passed | Exact same test and 148 raw resource files; includes malformed-byte refusals and replacement metadata controls. |
| Repaired current units | 119 passed | Numerical framing growth 1; unchanged current Mastery 15; core 91; complete current parent 12. |
| Current observation owner | 8 passed | All three current authored classes, with the separately derived import-only campaign source. |
| Standalone observation campaign | 19 cases passed | Fresh standalone and JUnit campaign roots agree at `ef73c54a20835369ae541b95bdb7b56ba42c3a4366607b00157a09f7572cc2e7`. The historical root is not an oracle. |
| Whole retained upstream packet | **67 executed: 66 passed, one error** | No assertion failures or skips. The original whole packet and assertions remain; the unavailable static-analysis recipe blocks qualification. |
| Independent current native lane | 19 passed | Donor 7 and both current parent classes 12; strict fresh GCC/UBSan library, required library mode and exact Java/native receipt parity. |
| Final T03 mechanical replay | **70 stages passed** | Ten actual materializer test invocations; six initial Results/writes, fresh strict compilation, complete output equality and five independent disk replays with zero Results/writes. |

The authoritative outcomes are in [T03 RESULT](evidence/current-be92/final-t03/RESULT.json), [current units](evidence/current-be92/current-units-epoch02/RESULT.json), [observation](evidence/current-be92/observation-regression-epoch02/RESULT.json), [upstream](evidence/current-be92/upstream-regression-epoch02/RESULT.json) and [native](evidence/current-be92/native-lane-epoch02/RESULT.json). The upstream result's original generic unrun list incorrectly mentioned the upstream suite itself; the immutable [classification correction](evidence/current-be92/UPSTREAM_CLASSIFICATION_EPOCH_02.json) records that all 67 executed and the gate failed. No failed report was rewritten to appear successful.

## What the final replay establishes

The replay starts in a new isolated projection. Its immutable local seed contains 1,939 files, including all 527 merged context inputs and the six original target preimages. That local Git snapshot is an evidence snapshot, not remote ancestry.

The five recipe crates apply in a fixed order: parser hook; contract helper; attributed convergence helpers using an isolated symbol provider; copied observation imports; copied Documentation imports. They produce exactly six Results and six writes. Provider classes stay off every materializer runner classpath. A fresh strict compile then processes all 446 first-party sources. Its 1,446 classfiles and all 34 main resource outputs equal the retained fourth-owner output bytes.

The driver subsequently snapshots the newly written files and invokes all five crates again in fresh application build directories. Each invocation reads the actual written repository and produces zero Results and zero writes. This independently executed disk replay is distinct from each materializer's embedded reparse-and-second-application assertion. All 1,939 seed paths are compared: exactly the six admitted targets change; 1,933 remain identical. Within the 527 current context inputs, 523 remain identical and four production files change. [Independent execution review](evidence/current-be92/execution-static-review/t03-execution-review/REVIEW.md) binds those facts.

## Evidence applicability and its limits

The final manifest checks 2,325 files and 15 retained runtime directories, complete membership of 432 cached JARs and 766 POMs, ordered runtime profiles, exact tool identities, the Maven distribution, raw parser inputs, native artifacts and the recorded XML outcomes. The earlier cache snapshot and later captures agree under the reviewed offline/no-install workflow. The Maven boot/lib bytes match the retained checksum-verified 3.9.12 distribution. These are observational and archive-custody checks; they are not a claim of JVM-loaded-byte attestation or full hermeticity.

All 34 resource sources and outputs are explicitly bound in T03. In the earlier executions, 26 resource sources were directly frozen and eight were established by later source/output/prior-copy equality. Those eight are not retrospectively described as direct execution-time source seals. The pinned earlier parser provider is also preserved as its own source epoch; the final replay builds a fresh provider for the four-owner candidate. [Applicability review](evidence/current-be92/execution-static-review/current-owner-upstream-review/t03-applicability-review/REVIEW.md) and [provider execution review](evidence/current-be92/execution-static-review/current-owner-upstream-review/t03-provider-execution-review/REVIEW.md) retain these qualifications.

Composition executes the actual canonical Atomize/Patternize composites with the frozen explicit problem term and budgets. Four two-source fixtures produce 16 unique schedules and 32 executions including replay. Exact whole-body admission, no-host and private-native controls remain. Full first/replay receipt bytes agree. The structured receipts represent source text by hashes; they do not persist every full intermediate source map, and AP/PA final comparison is by recorded roots. This does not qualify every default option or provider combination.

The native lane uses the current header and C sources, a fresh GCC 13 library with strict warnings and UBSan, JDK 21 and `-Xcheck:jni`. RXL's 72-case receipt and six RXM compiler rows match the Java-only receipts byte for byte. Java regex automaton transitions remain Java. Required library availability does not demonstrate a separate JNI regex implementation, and API-level checks are not a count of native entries. This is a local selected-path result, not all platforms or all native header functions. [Native review](evidence/current-be92/execution-static-review/native-lane-epoch02-review/REVIEW.md) records those boundaries.

## Remaining blocker and preserved history

The current admitted runtime lacks the required static-analysis recipe. The bounded dependency review also leaves the exact Kotlin 1.13.0, rewrite-analysis 2.5.0, rewrite-templating 1.6.3 and rewrite-bom 8.21.0 closure unadmitted. No substitute versions, thin pack, class omissions or weakened test oracle were introduced. The original six-pass upstream expectation also requires a separate current-policy review once the full runtime is admitted; this packet does not change that assertion to a larger number merely to make the test pass. [Current runtime admission](evidence/current-be92/current-runtime-admission/CURRENT_RUNTIME_ADMISSION.md) and [availability review](evidence/current-be92/runtime-availability-epoch02/AVAILABILITY.md) describe the bounded search and remaining custody/closure requirements.

The original Mastery OOM trial remains a failure with an unknown completed Mastery invocation count. The original campaign and Documentation lint failures remain separate failed epochs. Historical 0b/d1 results, source templates, observation verifier pins and scope documents remain historical. No original verifier was repinned to accept the new helper. Selected publication omissions are redundant acquisition/index/draft copies or binary build artifacts; their hash/reason records preserve the availability boundary without pretending their bodies are included.

## Reproduction

The installed [T03 scope](T03_REPLAY_SCOPE.md), [driver](verify.sh), [provider](provider-v2.py) and [applicability program](evidence-applicability.py) define the finite mechanical replay. It requires a fresh exact-preimage projection, the admitted proof overlay, original copied fixture bytes, the pinned tools/cache and the retained evidence directories. The adoption script refuses mixed or already-written target states. The completed candidate is therefore not a valid initial replay seed.

The recorded invocation was:

```bash
JAVA_HOME=/workspace/scratch/877d9513c002/toolchains/jdk-21.0.2 \
MVN=/workspace/scratch/877d9513c002/toolchains/apache-maven-3.9.12/bin/mvn \
JCC_MAVEN_REPO=/workspace/scratch/1c68df1bae79/javac-convergence-20261005/m2 \
JCC_CHECKSTYLE_JAR=/workspace/scratch/5809f5dce3dd/provider-superset-20261005/toolchain/checkstyle-12.3.1-all.jar \
JCC_GATE_EVIDENCE_ROOT=/workspace/scratch/1c68df1bae79/javac-convergence-20261005/work/source-merge-review \
JCC_GCC=/usr/bin/x86_64-linux-gnu-gcc-13 \
bash /workspace/scratch/1c68df1bae79/javac-convergence-20261005/recovery-final-replay-be92/synexia-openrewrite-recipes/recipes/atom-pattern-mastery-20261005/jcc/recovery-be92/verify.sh \
  -Djcc.apply=true
```

The driver starts the reviewed tool subprocesses in a clean environment. The exact [launch receipt](evidence/current-be92/final-t03/LAUNCH.json), [seed](evidence/current-be92/final-t03/SEED.json), stage commands and logs are preserved. A bare source checkout does not include cached dependencies, retained class directories, native libraries or other binary evidence; their identities and construction commands are included, while the binaries themselves are excluded from this source publication. Rebuilding the incomplete upstream dependency closure remains outstanding.

Source export, receiving project materialization/qualification, the full module/reactor and product-image acceptance remain separate obligations. The E4 input deliberately leaves the future published output revision unbound and both export and destination acceptance false; publication tooling may fill only the actual verified commit/tree and immutable proof references.
