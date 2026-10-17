<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# M3 primitive inverse map: source-sealed target receiver

## Canonical authority and exact input
Canonical reusable Java LST/OpenRewrite recipe:
https://github.com/hsoliwal/com.synexia/pull/12130
at commit `22a9f028071e83191809cf5d85728ef54eaae256`.
Name: `com.synexia.m3.Jdk21PrimitiveBiMap20261017`
(reuses `M3HashPinnedJavaSnapshotRecipe`).
This receiver is stacked on M3 SDK PR #1130, exact base
`14268ef0dc4d043d7cdbe96d4c25f7b88a4656f1`.
All three target source paths were ABSENT before this receiver.

| Output path | Canonical source SHA-256 |
|---|---|
| `src/main/java/com/m3/collections/M3LongBiMap.java` | `0627829aa19138f5fe3b7551bfb087e3ea9d90305b28deffa6128b56b3239970` |
| `src/test/java/com/m3/collections/M3LongBiMapSelfTest.java` | `125bfd1732ba960ff2ffe99b21f45298b877a18e259a588e1e8f3d72daed0bc2` |
| `src/test/java/com/m3/collections/M3LongBiMapTest.java` | `9c937b37e723510266976ef9d041db87b454c97b695f8c5985b9de5d0cc576eb` |

Git blob identity of all three source files is checked against the Synexia
resource templates. No previously present Java or native source changes.
No new JNI symbols or dependency on Synexia at M3 runtime.

## Deliberate bounded contract

`M3LongBiMap` composes two existing `M3LongLongHashMap`
index owners, each already storing parallel `long[]` key/value lanes and
`byte[]` states. The primitive bidirectional projection enforces a
bijection with a stable live inverse, `forcePut` eviction, mutation and
inverse-mutation parity, negative expected-size refusal, duplicate-value
refusal before mutation, and zero/negative/extreme long support.
No retained per-mapping Entry/Node. No guarantees of thread safety,
encounter order, serializable object form or Map/Guava/Eclipse/Commons
interface substitution. Two full indexes have nontrivial cost; no speedup
claim until matched CPU and retained heap evidence.

Guava `com.google.common.collect.BiMap`, Eclipse Collections
`org.eclipse.collections.api.bimap.BiMap`, and Apache Commons
`org.apache.commons.collections4.BidiMap` are **upstream contract
references**. Spring `org.springframework.util.MultiValueMap` is not a
bidirectional map. Upstream commits, source paths and license per owner
are recorded in the canonical Synexia recipe catalogue. No third-party
implementation source or challenge/editorial solutions were copied.

## Qualification

- PASS, local: `javac --release 21 -Xlint:all -Werror` with a **test-only
  stand-in** of existing `M3LongLongHashMap` and seeded 12,000-operation
  differential selftest. Not a real M3-owner proof.
- PENDING: exact-head workflow with the real M3 hash map owner and JUnit.
- PENDING: execution of canonical Maven/OpenRewrite fixed-point and refusal
  tests, exact source replay receipt, full module Maven/reactor, installed
  JDK and jtreg/JCK proof, provider JAR compatibility, memory/CPU perf,
  concurrency and serialization gates.
- Explicit HOLD: this is a draft, not a five-provider SDK superset and not
  authorization to replace `java.util` implementations. Do not advance
  String -> arrays -> collections -> AST/compiler promotion.

Use the existing runtime owner and verify source hashes in CI.
No rebase, force push or modification of product master/develop.
