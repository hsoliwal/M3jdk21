<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Array expansion and JNI receiving provenance

Date: 29 October 2026. This is passive product provenance. Synexia exclusively owns the recipes, templates, catalogue, correction memory, compiler/JUnit fixtures and qualification machinery.

## Immutable canonical inputs

| Packet | Published Synexia revision | Review |
|---|---|---|
| Array expansion and staged collection lanes | `f0d22d34223865e04e1cdaae8126a58be42b0f70` | [Synexia #13979](https://github.com/hsoliwal/com.synexia/pull/13979) |
| Rank JNI acquisition correction and current-master integration | `22487c6050ad3b8e3823b4da021ff821b37c49c2` | [Synexia #13986](https://github.com/hsoliwal/com.synexia/pull/13986) |

Canonical task paths are `synexia-openrewrite-recipes/crates/m3-array-expansion-20261029` and `synexia-openrewrite-recipes/crates/m3-jni-rank-acquisition-20261029`. The named recipes are `com.synexia.m3.ArrayExpansion20261029`, `com.synexia.m3.ArrayLaneExpansion20261029`, `com.synexia.m3.ArrayMaskNativeExpansion20261029` and `com.synexia.m3.JniRankAcquisition20261029`. They reuse the existing canonical Java/text snapshot engines unchanged.

The original retrievable product input is `3569c790d5cc3f9bc871ab0ecd025dab0fd5cafc`. The final public receiving base is `669a949d192345acefcfc5e9bb0b3724174d270c`; all eighteen owned preimages/additive paths are unchanged between those revisions. The public baseline's eight newer collection sources were preserved and included in the final integration proof. Local checkpoints `8fd7a716ba948f4b822e356b0350b068fcdb4b8e`, `e9221fe8b3aad9659923b54a8fe0a867894dc072` and `091142d9065ca3714d8075776f1a73c9f5b4b551` identify actual local recipe receiving; they are not represented as published product commits.

The earlier `RECEIPT.md` in this directory records the historical first pass and its temporary GitHub quota block. Published canonical revisions above supersede that historical publication status. Its source bytes and historical observations are retained.

## Executed output identities

Nine Java owners, two C owners, six target test sources and the historical passive receipt were received through the canonical recipes. Every hash below was checked against the actual local received file before authoring this final passive-receipt recipe input.

| Product path | SHA-256 |
|---|---|
| `m3/collections/qualification/array-growth-20261029/RECEIPT.md` | `5f1cdbd9830abe1a0103ded48a456d7e9f2a01adcbcc2eb6223cade6ce9983df` |
| `m3/collections/src/main/java/com/m3/collections/M3ArrayDeque.java` | `c67e58ade928f25f5add7b374701fd33417890a59b324bbc2df220de5c23312b` |
| `m3/collections/src/main/java/com/m3/collections/M3ColumnarLanes.java` | `f20ebe2b3f6877a830f4cb8670be646dd38ea496d7e1d3223340e0ec1e6fc266` |
| `m3/collections/src/main/java/com/m3/collections/M3IntIntHashMap.java` | `63e82b9468fbeebba2293e926f018309b2e87dbf412d1d2260672934ac2142f5` |
| `m3/collections/src/main/java/com/m3/collections/M3IntLongHashMap.java` | `3f8bdfee2a06ef4ef2a4a779dab9e67ac633c6acdf1b2c1314c9e760c2cd0e65` |
| `m3/collections/src/main/java/com/m3/collections/M3LongLongHashMap.java` | `2f228e876a5b57700ecb513bc19775735774858017b6c15fcad7b5ea44469e27` |
| `m3/collections/src/main/java/com/m3/collections/M3LongLongTreeMap.java` | `eacd37b550d085e7ced690f7715b643572ca35781b330e93eb728f3d78b6a0a5` |
| `m3/collections/src/main/java/com/m3/collections/M3LongPatriciaTrie.java` | `6820e55c8f144952924caced009346ca24c0a21001716285b2514ac0e11cd64a` |
| `m3/collections/src/main/java/com/m3/collections/M3LongTreeSet.java` | `de261efba2f10d1b9ac35d641a0810b281d5d1bc43d5b0242366840421097bf9` |
| `m3/collections/src/main/java/com/m3/collections/M3ObjectIdentityMap.java` | `21316d56804687d9a2af137db0b68ad8110fdb294dc505b4b5fd735238218224` |
| `m3/collections/src/main/native/collections/m3_index_mask_compactor.c` | `382e93d8cc033b07da14537d2bf6c0d70405a8ce6a5eb318afc3005368bbf062` |
| `m3/collections/src/main/native/collections/m3_long_rank_batch_jni.c` | `1b7559066296c96b0177cd770ca34d5c66b15ac715b66c0f0fcc2d54a3800de5` |
| `m3/collections/src/test/java/com/m3/collections/M3ArrayDequeGrowthContract.java` | `ceb6e333dfdf640a12560b3c00e3bdc200a5d89a52b8d6594b39edd137d5e56e` |
| `m3/collections/src/test/java/com/m3/collections/M3ArrayExpansionTest.java` | `76d8968b8e53b8d00f82dc2d205f1f304e220b7c4c83b08cabedb1b25dcac1e1` |
| `m3/collections/src/test/java/com/m3/collections/M3ColumnarGrowthContract.java` | `ebf942a8bacd4426e00973fc0a7c4912db858bec480c230e5f8c0de91d2d194a` |
| `m3/collections/src/test/java/com/m3/collections/M3StagedLaneBehaviorContract.java` | `4e3682064d0ba71a52cd6cd189ee6b095d78591f98b7b4811b7aad329a96dbad` |
| `m3/collections/src/test/java/com/m3/collections/M3StagedLaneBehaviorTest.java` | `fa015c38e7cd23b009dcf4d8eafc29488af33cd08d81e5789a00ceef8ae4f14a` |
| `m3/collections/src/test/java/com/m3/collections/M3StagedLaneOrderDifferential.java` | `b5a44c618accdae7e156c0af4f254bf8782a3d3be31244db76ac0f14c84d101d` |

## Final formatting derivation

After the immutable behavioral packets were published, a complete baseline-to-candidate patch check found eight trailing spaces on blank line 136 of the emitted `M3StagedLaneBehaviorContract.java`. The existing typed Java snapshot engine, with resource key `m3-array-growth-oracle-whitespace-20261029`, removes only those eight spaces. It preserves every token and line number. Original source SHA-256 `d02f977c4e4ed95e67d876049142f0feee76ac4100f4ee0f15c218dee9fe24eb` becomes `4e3682064d0ba71a52cd6cd189ee6b095d78591f98b7b4811b7aad329a96dbad`.

Strict Java 21 compilation with debug information produces four byte-identical class files from the original and cleaned sources. The 701-test comparison below used the original frozen oracle source; this separately retained compiler identity establishes the final source's identical compiled test implementation. The independent original oracle, all runtime source images and their recorded proof results remain unchanged. Formatting resources, compilation/classpath seals, actual typed-recipe receiving, schedules and JUnit evidence are retained additively under the rank crate's `closeout/` in PR #13986. Their exact content identities do not claim that these later formatting files already existed in the earlier immutable behavioral commits.

The initial attempt to use the text engine for that Java target was correctly refused. The refusal and unchanged product preimage are recorded, and the text engine's Java-path prohibition remains unchanged. The correction follows the existing typed Java owner. The final passive receipt then uses its appropriate exact-text owner.

## Executed gates and limits

The main combined proof passed fourteen tests. Its metadata registration passed three tests and the complete canonical provenance checker. The rank named recipe passed its focused test, including actual receiving, six ownership refusals and a zero-change fixed point. Rank qualification retains seventy recording-JNI cases per image and eight real checked-JNI processes (baseline/candidate, full/legacy ABI, JIT/interpreter) with 225 fixtures per process. The candidate preserves a pending exception or installs OutOfMemoryError at six previously silent acquisition-failure exits, after the original cleanup. Normal-path results and JNI exports remain unchanged in those contracts.

The independent seven-owner oracle executed 5,237,092 model checks and 769,926 order/capacity observations per run. Fault injection qualified thirty-six failure-atomic growth/trim points plus three committed-deletion compaction points. Main native proof includes strict C17, generated JNI headers/export parity, recording-JNI fault cases, ASan/UBSan and real checked-JNI runs. Leak detection was unavailable in the ptrace environment; no leak-qualification result is claimed.

The final native-enabled collections comparison uses the unchanged original POM and identical dependency JAR hashes:

| Exact module image | Tests | Pass | Failure | Error | Skip |
|---|---:|---:|---:|---:|---:|
| Public base 669a949d | 691 | 681 | 8 | 0 | 2 |
| Public base plus the eighteen received paths | 701 | 691 | 8 | 0 | 2 |

All 691 shared outcomes match. All ten added tests pass; no tests are removed. Both Maven runs exit 1: the same eight existing assertions still fail and two existing tests remain skipped. One raw failure-message difference consists only of process-specific LiveEntry identity suffixes; both messages and their separate classification are retained. The public images differ in exactly the eighteen listed paths, with 773/780 source, POM and resource seals unchanged before and after execution. This additional passive receipt does not change the tested runtime or test-source bytes.

The original behavioral evidence, exact commands, donor/category decisions and retained counterexamples are in the immutable Synexia packets above. The later formatting and passive-receipt derivations are retained separately in the same rank PR with exact source hashes and compiler identity proof. Diagnostic deque timings and sparse JNI copy counts are scoped observations. No universal fastest implementation, all-collection optimality, hosted-CI success, all optional native-provider acceptance, source-built modified JDK, jtreg result, additional-platform acceptance or release/phase promotion is inferred.

This file is itself emitted by the existing canonical text recipe with resource key `m3-array-expansion-final-receipt-clean-20261029`, using the reused isolated Maven metadata host. Its source template, execution log, schedules and fixed-point proof remain in Synexia. It contains no executable recipe machinery.
