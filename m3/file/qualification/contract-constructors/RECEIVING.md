<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Current M3JDK21 original M3File: 16-contract constructor receiver

**Review status: DRAFT/HOLD — exact candidate source, runtime not qualified.**
M3JDK21 is the JDK, not M3 plus a JVM layer. The sole private source-changing
Maven/OpenRewrite recipe is
[Synexia #13230](https://github.com/hsoliwal/com.synexia/pull/13230),
`com.synexia.m3.M3Jdk21File16ContractConstructors20261026`,
at source head `66948cb69b4b27bc1a8645b5de22097a1401b41f`.

Current protected M3JDK21 master preimage: `175c7ed1056005e5a686e3189f601c8ed4f406bb`.
Original `m3/file/verify.py` SHA-1 Git blob:
`8c66ff655ef6a719e64da75c2ba1f6ada1f8ac2a`, with **16** independently
enumerated original M3File contracts. It is **unchanged**, along with all
M3File implementation, `module-info.java`, original Java APIs, JNI
entry points and resource-lock/stream behavior.

## Four exact source edits

The four original public test classes
`M3FileEditBatchContract`, `M3FileEditFramesContract`,
`M3FileObservationContract`, and `M3FileSourceContract`
each receive **one explicitly public no-argument constructor** immediately
after the original public class declaration.
Their prior implicit constructor was already `public <init>()V`.
No test `main`, assertions, provider state, return values or
exception identity changes.

Each preimage Git blob **exactly matches current protected master**.
Each postimage Git blob **exactly matches the canonical private Synexia
source template**. Source SHA-256, licence, provenance and review status
appear in the passive `SOURCE_RECEIPT.tsv`. No OpenRewrite executable
recipe, memory writer or duplicate JDK runtime owner is copied.

## Original product failure and isolated proof

Two original M3JDK21 GitHub Actions jobs on the previous exact preimage
received hosted runners and actually executed Java21 strict module builds:
[run #40262356507](https://github.com/hsoliwal/M3jdk21/actions/runs/40262356507)
and [#40262356515](https://github.com/hsoliwal/M3jdk21/actions/runs/40262356515).
They failed under unchanged `javac --release 21 -Xlint:all,-module -Werror`
because those four test classes relied on implicit public constructors.
The source donor separately showed the exact warning is eliminated by
explicit public `<init>()V` with unchanged public bytecode descriptor.

These are **candidate result files**, not a claim of successful current-head
product CI. After this PR commit, original M3JDK21 16-contract host and
`jlink` image workflows must actually run and pass. If another contract,
historical handoff, naming or JNI check fails, preserve its independent
red gate and inspect it; do not suppress lint, overwrite the 16-contract
verifier, or mark the entire branch admitted based on four Java files.

## Admission boundaries

- Canonical Synexia private Java LST OpenRewrite `Recipe.run`,
  4-atom fixed point and drift-refusal JUnit: **NOT EXECUTED** because private
  runner assignment is blocked; issue
  [#1205](https://github.com/hsoliwal/com.synexia/issues/1205).
- Original M3JDK21 current-head host plus linked-image 16-contract
  behavioral module test: **MUST RUN** after this candidate.
- Installed source-built entire M3JDK21/HotSpot/JTReg/JCK/JNI/GC/CDS,
  provider/cross-platform ABI and CPU/retained-heap/native-cost gates:
  **NOT QUALIFIED**.
- No develop rebase, protected-master modification, automatic merge, or
  changes to the historical 13-contract receiving branch #1887.
