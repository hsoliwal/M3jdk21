<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# M3 filtered/sorted collection bulk admission — current-base receiving slice

**Canonical:** Synexia [#12139](https://github.com/hsoliwal/com.synexia/pull/12139),
recipe `com.synexia.m3.SdkFilterWindow20261017`, source head
`e4c6c523bbe03decbc4b8411a3fed729824ef647`.
**Receiving base:** M3JDK21 `master@e913ab89056657eca4009678cfe9a657c020e79b`.

## Serial source inventory and proof

| Field | Value |
| --- | --- |
| FILE | `m3/collections/src/main/java/jdk/m3/collections/M3MutableFilteredViews.java` |
| Original blob | `a9f0767e5d2903699295d2e76e337310916ff820` |
| Generated Synexia blob | `b95717dcb3c9ae2936ff0defc450c6bc4d27b304` |
| Original SHA-256 | `0ce075c9897724dc63c65b805480eaa918aaade46aec0188cfaf9aac3aa2863f` |
| Post SHA-256 | `ba88b504861bb43346a2a4b961c7fa0d01d815abebf0b9080c475653b2048c21` |
| Algorithm/test donor | Synexia first-party Apache-2.0; original JDK TreeSet/TreeMap used as oracle |
| Code ownership | Synexia reusable algorithm/recipe, M3JDK21 Java21 product receiver |

Follow **inventory → dated additive convergence → catalogue → Inline → Atomize →
Patternize → Comment → compile/test gates → PR**. The target-local Maven replay
checks Synexia's exact source snapshot, postimage and fixed point; it is not a
second algorithm/recipe authority. To change the transformed source, update the
canonical Synexia recipe and forward-port its new exact postimage.

## Corrected behavior

`SetView.addAll` consumes a potentially lazy source once, admitting each
reference before mutation; transient `ArrayList<E>` stores only call-local
values. `MapView.putAll` captures key/value lanes once, avoiding second
reads of reusable Entry objects and preserving comparator representatives,
nulls, encounter order and live backed range behavior. No retained collection
Entry/Node/Cell structure. No new M3 payload, JNI symbol, or `java.util` override.

Compilation/behavior gate uses the exact first-party `M3BulkCaptureContract`:
the single-use/lazy source red case and 1500 seeded TreeSet/TreeMap oracle
trials. Both source/predicate failures must occur before writes; JDK range
write failures intentionally remain partially applied (no false transaction).

## Maven recipe and JNI boundary

`mvn -B -ntp -f m3/collections/qualification/sdk-bulk-capture-20261017/recipe/pom.xml verify`
runs source postimage checking, a repeat fixed-point test and missing/drift
refusal. `--apply` on the recipe class is permitted only from the pinned
preimage; never hand-edit the generated owner. Workflow also performs strict
Java21 oracle compilation and the existing M3 collections Maven gate.

**Why only one runtime file:** Synexia #12139 also proposes bounded-work
`M3IndexMaskWindow` Java and C JNI. Its pinned Java/C preimages do **not**
match master after the optimized JNI region-read PR #1131. Replacing those
whole files would regress previously reviewed improvements. Those changes
must be reconciled in a successor canonical recipe, not transplanted here.

**HOLD:** target full Maven/CI, assembled JDK image/jtreg/JCK, full JNI
matrix, OS parity, lifecycle/load benchmarks and all 107 SDK API contracts
are not proved merely by this leaf. Collections remain below active String
and arrays phase admission.
