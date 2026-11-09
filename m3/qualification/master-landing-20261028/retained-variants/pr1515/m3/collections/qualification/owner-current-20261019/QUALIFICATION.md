# Current collection owner qualification

## Scope and receiving baseline

This seven-target pass is based on `hsoliwal/M3jdk21` commit `9a164c31b38c7013c179a3ff38b0bb60c199c7fd`.
It changes three existing production owners and adds four existing donor contract/JUnit sources. Production postimages are generated only by the canonical Synexia named OpenRewrite recipe. The reviewed template files are not product output.

The canonical recipe `com.synexia.m3.CollectionsOwnerCurrent20261019` passed 14 actual Maven/OpenRewrite qualification methods, with zero failures, errors or skips. Actual output is `/workspace/scratch/7ea994fb52d9/proofs/sdk-collections-current-01/sdk-output`; its `SDK_EXECUTION.tsv` seals each emitted Java LST source.

Exact inputs and postimages are recorded in `SOURCE_SEALS.json`. No product source was manually edited by the collection workstream.

| Existing owner | Bounded change | Retained behavior |
| --- | --- | --- |
| `M3FilteredLists.Cursor` | Reversibly read the original retained backing cursor immediately before indexed remove/set/add writes. | Fail-fast generation, callback and acceptance order, original exception identity, hidden/null/duplicate occurrences, indexed backing writes including CopyOnWriteArrayList. |
| `M3LongIndexProjection.sorted` | Reverse known opposite-order groups into one final primitive index array. | Stable physical-index order within equal-value groups, shared payload owner, slices and filtered offsets, same-order fast path and general encounter-order merge sort. |
| `M3FilteredViews` private sorted/navigable owners | Restore strict sequenced endpoint guards and the existing guarded JDK reversed traversal after upstream removed both duplicate override copies. | Current reverse split comparator metadata, late binding, predicate demand, stored representatives, interior-null navigation, bounded ranges, original JDK reverse traversal and covariant navigable reversal. |

Four added sources are the donor `M3FilteredListMutationProof`, its JUnit wrapper, `M3IndexOrderWindowContract`, and its JUnit wrapper. The unchanged current strict sequenced, merged feature and recursive split tests qualify the third repair.

## Current upstream reconciliation

The earlier 11-target candidates are historical and must not be applied to current master. Current upstream already provides fresh caller-owned Entry projections and invokes the compatibility hook once before each projection. Its stronger callback/exception-identity, null, stored-key identity and owner-graph tests remain byte-for-byte intact. Both the Integer-key reserved-LIVE fixture and unsigned high-bit mask oracle were already corrected upstream and are likewise untouched. The updated pom resource metadata is preserved.

`PROVENANCE.json` records exact hashes of these five untouched current files. This pass does not change `M3SlotMap`, `M3EntryFacadeCacheTest`, either corrected fixture, the generic LFU utility, native algorithms or JNI symbols.

## Demonstrated counterexamples and preserved contracts

The raw current source compiles all 268 production Java files with Java 21 `-Xlint:all,-module -Werror`. Its existing `M3StrictFilteredSequencedContract` then fails: hidden sorted `removeFirst` returns `NoSuchElementException` through the Java 21 default instead of the published unconditional `UnsupportedOperationException`. Restoring the four overrides passes the same unmodified 126-check proof. The unchanged newly merged cross-feature proof passes 2,517 checks and recursive split proof passes 6,324 checks on the focused preview.

The original filtered-list mutation bug remains independently reproducible against actual JDK ArrayList cursors. After selecting 2 from backing `[2,4]` and then inserting 6 externally at the beginning, the original filtered cursor performs all three incorrect writes. The JDK cursor throws `ConcurrentModificationException` and preserves `[6,2,4]`. The corrected cursor matches that outcome for remove, set and add. Its correction retains indexed backing writes; substituting delegate-iterator writes would incorrectly remove supported CopyOnWriteArrayList behavior.

The differential control for the full standalone runner contains only the strict sequenced repair. It retains the original filtered-list and long-index production owners so their observations can be compared independently. Current original main compilation and strict failure are separately retained; a control is never presented as an unchanged raw baseline.

## Real SDK and runtime evidence

All three final current gates passed using exact SDK-emitted output.

| Gate | Actual result | Evidence |
| --- | --- | --- |
| Owning Maven module | 268 main and 258 test sources;139 suites, 467 tests; 0 failures/errors, 2 conditional skips; 154.941 seconds within a 600-second deadline. | `proof/emitted-owning-module-1/JUNIT_SUMMARY.json`, `MAVEN.log`, complete source/pom receipt. |
| Strict Java/JNI runner | 88 commands passed; 21 surrounding contract outputs identical to the sequenced-reconciled control. | `proof/emitted-verification-1/RESULT.json` and `COMMANDS.json`. |
| Filtered mutation proof | 775,092 checks in both tiered and interpreted execution; all three current baseline incorrect writes reproduced, all three emitted outcomes match original JDK cursors. | Mutation proof and independent witness logs in the strict runner. |
| Stable index/window differential | Java 2,990,400 and required-native 3,267,362 checks on both control and output; 65,536 mask pairs and 67,914 windows. Missing-native refusal 13 checks per owner. | Order/window logs;shared observation digest `e15c399b7be3d8c0fbd38bc6e2cd4c63b93bb0b95d1272bce9acdcc7bec5445b`. |
| Surrounding required JNI contracts | Mask bridge 16,241;range 638,432 with 2,048 seeded cases;required/excluded 512;compactor 26,152;multivalue 440,027;projection 20,706;filtered-offset 28,615. | `candidate-native-*.log`;current multivalue reserved-LIVE assertion remains unchanged. |
| Public and native compatibility | Seven owner public/protected descriptor sets and all 12 generated JNI headers identical to control. Three exact original C owners unchanged. | API logs, generated headers, `NATIVE_INPUTS.json`. |
| Integrated M3JDK21 image | Nine commands passed using the emitted external `com.m3.collections` named module, exact test patch and real required JNI libraries. Includes the same 3,267,362-check native order/window digest, 638,432 range checks, 440,027 multivalue checks and 775,092 mutation checks. | `proof/integrated-java-native-1/RESULT.json`, module-resolution log and exact image/module/native custody. |

The two Maven skips are existing explicit native-lane assumptions in `M3FilteredSplitsMaskSpansTest`: `allNativeBackendsAreRequiredByTheFullLane` and `maskSpanCheckedJniOracle`. The five-library all-native/mask-span optional lanes were not enabled by this Maven invocation. The selected JNI components listed above were separately required and executed successfully; the skip count is not reported as zero.

The standalone vendor is Temurin 21.0.12.1+1-LTS. Native selectors were rebuilt from the unchanged C owners with GCC 13.3, C17, `-Wall -Wextra -Werror -pedantic`, UBSan and nonrecovering undefined-behavior checks. Runtime commands use `-Xcheck:jni`. The second runtime is the actual integrated fastdebug M3JDK21 image, whose `bin/java`, `lib/modules`, `lib/server/libjvm.so` and release metadata are hashed in its receipt. This is real native execution and explicit module resolution, not a fallback-only assertion.

## Bounded resource observation

The existing donor resource probe measured three samples of 100 known-direction reversals over 32,750 selected rows on each owner. These are finite JVM observations. The logical output indices remain identical; the new known-order path creates one final primitive index array without the generic sort working/scratch pair.

| Median per 100 reversals | Original order algorithm in current sequenced control | Emitted order algorithm |
| --- | ---: | ---: |
| Allocated bytes | 26,207,984 | 13,106,384 |
| Thread CPU nanoseconds | 116,681,056 | 11,776,636 |
| Process CPU nanoseconds | 120,000,000 | 10,000,000 |
| Wall nanoseconds | 122,083,252 | 16,297,190 |
| Heap delta after requested GC, bytes | 64 | 32 |
| Whole-VM RSS, KiB | 58,832 | 59,372 |
| Whole-VM high-water RSS, KiB | 61,372 | 59,372 |
| Direct/mapped buffer bytes | 8,192 /0 | 8,192 /0 |

Allocated bytes fall by approximately 50% for this bounded workload. CPU and wall observations also improve in these samples, but sample variation/JIT effects and whole-VM measurements do not establish a universal speedup or RSS reduction. In particular, median RSS is slightly higher in the emitted process. Requested-GC deltas are observations, not a retained-heap guarantee. Raw samples, environment, exact probe source and medians are retained in `proof/emitted-verification-1/RESOURCE_SUMMARY.json` and the two resource logs. Native algorithms/transfer behavior are unchanged; this resource sample measures the Java sort owner.

## Donor custody

The filtered-list mutation atom and exact proof/test come from M3 PR1268 at `624e2e67060a804fab97394815b04c5bf9b6d43b` and canonical Synexia PR12255 at `804f98e37704d817542bbe980db6bff1b74b1032`. The donor's older complete source was not substituted for the current owner; newer current read-only fixes remain.

The stable opposite-order group reversal is the exact M3 PR1451 source at `63bc1db1c6ef1c701aa5e25629288c0c4139a64d`, also held in current wrapper-convergence recipe custody. Its canonical Synexia donor is PR12461 at `db329cda4434f40c282285c5879984b515660403`. The exhaustive stable index/window contract and resource probe are reused from the existing current custody.

The sequenced repair reuses the original guarded `readOnlySortedSet(SortedSet.super.reversed())`, endpoint refusal and covariant navigable atoms. It preserves the current new `M3SlotSpliterators.sorted(source)` adapter and introduces no wrapper framework.

## Limits and historical evidence

Finite collection tests do not establish global String/array/collection/AST/compiler admission, exhaustive JDK/vendor behavior, JCK/JTReg completion or universal resource improvements. The prebuilt integrated M3JDK21 image is used with the exact emitted external `com.m3.collections` module and test patch; this does not claim collections was linked into that prebuilt image.

Prior SDK8/9/11 outputs and old-base controls remain under `candidates/collections` and `proofs/sdk-collections-*`. Historical SDK11 passed its 82-command strict Java/JNI run. Its old-base full Maven run stopped progressing inside the then-existing object-stream keyed-plan path and was deliberately terminated after 41 suites/127 tests with zero reported failures or errors; it is explicitly incomplete. Current source contains subsequent keyed-plan liveness work and the new current full gate is bounded. No historical whole-module PASS is claimed.
