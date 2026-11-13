<!-- SPDX-License-Identifier: Apache-2.0 -->
# Merge-loss decisions R1-R3: audit cases 02, 03, 11 (m3/collections) - SUPERSEDED, nothing restored

Source: merge-loss audit S:/m3-scratch/eb-conv/mergeloss (LEAVES.tsv rows 2, 3, 11; cases/02.md, 03.md, 11.md).
Each flagged "lost" hunk is a competing implementation of a class that master already owned and later evolved, or
infrastructure for a contract master deliberately rejected. `DECISION.tsv` cites the commits.

| case | dropping merge | origin | owner on master | decision |
|---|---|---|---|---|
| 02 | 58deceae31b8 (#1335) | c4b5e7098ef | M3LFUCache 06e3c61cc16 + 3a3b1a55928, 53f1763c80b; entry retention removed by 58f4f5bab6a | SUPERSEDED |
| 03 | 43f11adb8897 (#1330) | 2005b567fee | M3ArrayDeque/M3PriorityQueue 36960a7b632 + adab56cdc85; fill-in ported the missing atoms | SUPERSEDED |
| 11 | 9bec8eed78e5 (#1332) | 1935ee82791 | M3LinkedHashMap; projection generations rejected by 58f4f5bab6a | SUPERSEDED |

Master evidence (m3/collections at bb377f2f1a3, real pom, -Werror, surefire; GATES.tsv): M3QueueContractTest (the case-03
origin's own contract test) 4/4, M3ArrayExpansionTest (runs M3ArrayDequeGrowthContract, the test the audit flagged)
3/3, M3PriorityQueueHeapifyTest 4/4. M3EntryFacadeCacheTest and M3LinkedHashMapCapacityTest are JNI-gated
(jni-test-gating-20261106) and not part of the default run.

The competing variants stay retained byte for byte under
`make/m3/collections/qualification/master-landing-20261028/retained-variants/pr1330|pr1332|pr1335/`.
