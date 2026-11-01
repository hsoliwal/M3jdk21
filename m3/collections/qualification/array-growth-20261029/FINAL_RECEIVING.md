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
| `m3/collections/qualification/array-growth-20261029/RECEIPT.md` | `1dea3c58d781b817e931f83ae1f2fa5e1ed974fb342b609ea7faca5a7b0a8b4d` |
| `m3/collections/src/main/java/jdk/m3/collections/M3ArrayDeque.java` | `55be5f7d9747f88c751987e69f9c2587559d27f55ece13c0e8aef84506f04108` |
| `m3/collections/src/main/java/jdk/m3/collections/M3ColumnarLanes.java` | `fb44d6095f7107a6f8407d290b5fa5a2e91f68e96921e7d259167bbbbe0cc49f` |
| `m3/collections/src/main/java/jdk/m3/collections/M3IntIntHashMap.java` | `9fb476f33f85532d0e8809cc107dcf270d38ff9381fb155a4d1131f13b0bbf5e` |
| `m3/collections/src/main/java/jdk/m3/collections/M3IntLongHashMap.java` | `37310ceb33c4671b7f8b2620427d07fcd94b9d9fde9fb9f822eabd35c31079f8` |
| `m3/collections/src/main/java/jdk/m3/collections/M3LongLongHashMap.java` | `2f228e876a5b57700ecb513bc19775735774858017b6c15fcad7b5ea44469e27` |
| `m3/collections/src/main/java/jdk/m3/collections/M3LongLongTreeMap.java` | `fa5d80161a84fe8c1e244586a1e6003646413f4f98a9c8e1368699d530c9a965` |
| `m3/collections/src/main/java/jdk/m3/collections/M3LongPatriciaTrie.java` | `73ba60be2b315964613264ef900b89938055ec8bdbbaede7abf11459d6633ef6` |
| `m3/collections/src/main/java/jdk/m3/collections/M3LongTreeSet.java` | `846004dfb6e7640963960654ebf3d7602d3e154757659f166c97dfa7b5e3ed20` |
| `m3/collections/src/main/java/jdk/m3/collections/M3ObjectIdentityMap.java` | `c89c1fded953c52f6f7d417f83bccc6ac88d644be10281160190f2b6c293fc76` |
| `m3/collections/src/main/native/collections/m3_index_mask_compactor.c` | `dd0f0811904b0e53d12f4148b13d7abdc73b235f44f4956474c695a6c6e8f6e5` |
| `m3/collections/src/main/native/collections/m3_long_rank_batch_jni.c` | `b8502c2f15b752b539282b0bdefd3f38eb1dbfe022bcae796d99df1e55b3158f` |
| `m3/collections/src/test/java/jdk/m3/collections/M3ArrayDequeGrowthContract.java` | `576840364559d3c6e3a467b1e9f22b18cf2bb8c81e459039ac0ed7cb5663a312` |
| `m3/collections/src/test/java/jdk/m3/collections/M3ArrayExpansionTest.java` | `a127595d7eb71525ddd0d73ebe8c783cda60316bedecbdf9297e0cf9653f036c` |
| `m3/collections/src/test/java/jdk/m3/collections/M3ColumnarGrowthContract.java` | `ff82a42cd66551c00ff12fb7bbcdc0583f4914d686b571024cbf10c3f74fb74a` |
| `m3/collections/src/test/java/jdk/m3/collections/M3StagedLaneBehaviorContract.java` | `4335032879ee2b6c9345e779a2aefb2c3d3a9b57deff019f6cc289619a6a3bd2` |
| `m3/collections/src/test/java/jdk/m3/collections/M3StagedLaneBehaviorTest.java` | `bfad22b130466186139316b65be6346b6db92f976c4ae0e01459475cc130cec7` |
| `m3/collections/src/test/java/jdk/m3/collections/M3StagedLaneOrderDifferential.java` | `80dff35b1b92ea3060b846d1e113401f69ffd0de6c83b727b95bdd673cdcddc0` |

## Final formatting derivation

After the immutable behavioral packets were published, a complete baseline-to-candidate patch check found eight trailing spaces on blank line 136 of the emitted `M3StagedLaneBehaviorContract.java`. The existing typed Java snapshot engine, with resource key `m3-array-growth-oracle-whitespace-20261029`, removes only those eight spaces. It preserves every token and line number. Original source SHA-256 `d02f977c4e4ed95e67d876049142f0feee76ac4100f4ee0f15c218dee9fe24eb` becomes `4335032879ee2b6c9345e779a2aefb2c3d3a9b57deff019f6cc289619a6a3bd2`.

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
