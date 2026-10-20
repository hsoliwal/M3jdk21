<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Keyed mapper failure progress

## Existing owner and failure

Reuse `M3KeyedParallelismPlan` from the exact existing Synexia snapshot at `fea5cef9f7a569e6ceb557f1965e234a3f075aba`, source blob `045e64a2dfc5c395bde5ec211196254ae7a680bb` (Apache-2.0). The receiver preimage is byte-identical. A failed worker publishes its original exception, then exits; the coordinator can remain blocked inserting into that worker's full queue before consuming the failure completion. The frozen latch-controlled baseline oracle timed out after 5.103 seconds, and the owning test JVM dump showed the same blocked admission.

## Selected behavioral atom

Change only the failed-worker branch. After publishing the original failure, drain cancelled work from the existing bounded queue until its existing end marker or shutdown interruption. Do not call the mapper for drained records. This unblocks the coordinator's pending insertion. Its unchanged global outstanding-work bound then forces consumption of the FIFO completion queue, reaching the already-published failure before discarded work could require a missing completion. The existing shutdown interrupts the drainer.

The coordinator and admission/accounting bytes are unchanged (SHA-256 `79b4f0077eb141508771086f34009b5ac2432721cc8bb3b00ae08a62d03e195c`). No field, queue, worker, timer, work-record type, allocation or normal-path operation is added. Public, protected and private descriptors are identical. Source/key demand, key affinity, result order, callback and original exception identity remain the contract. This is a failure-progress repair, with no general throughput claim.

## Alternatives and proof

| Candidate | Decision and reason |
| --- | --- |
| Publish failure and exit | Refused by the deterministic deadlock and owning JVM dump |
| Offer, then block for a completion | Not selected: it can delay source admission needed by a legitimate running mapper even after queue capacity becomes free |
| Timed offer and completion polling | Not selected: introduces a timer policy and changes coordinator admission demand |
| Failed worker drains cancelled work until existing shutdown | Selected: preserves coordinator behavior and bounded storage while exposing the original failure |

The corrected frozen oracle passes 44 assertions, including exception identity, progress and suppression of later callbacks on the failed lane. The unchanged object-stream harness passes 540 checks, and an independent review replay passes the same 44 assertions. Six success geometries exercise configured partition/window combinations; they do not directly instrument maximum outstanding work. Budget preservation rests on the unchanged coordinator and its bound, not an exhaustive measured-budget claim. Arbitrarily blocking user callbacks and universal Java Memory Model proof are outside this finite evidence.

`KEYED_ADMISSION_EVIDENCE.json` retains exact source, command and proof receipts. The earlier full isolated Java run passed 450 tests; the combined target adds three tests and is qualified separately by the owning Java/JNI gate. The canonical task retains the original thread dump and failed run.
