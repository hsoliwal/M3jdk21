<!-- SPDX-License-Identifier: Apache-2.0 -->
# Primitive canonical identity and identical-address candidate

Copyright 2026 Hitesh Soliwal and contributors.

Status: opt-in, target-owned runtime/compiler candidate; NOT a qualified M3JDK release.
The canonical Synexia recipe is PR #11846, commit
`c90964099bdb0a752d29033d3c7723842ad6b2dc`, under
`synexia-m3-recipe/recipes/m3jdk-primitive-identity-20261016`.
It adapts the existing source-sealed M3LeaseWindowReplay, not a parallel general rewrite engine.
The eight recipe-generated target postimages are committed at
`362652bff10f83a28d9d3331e4c3588cd51bc791`; base master is
`855afe57a679aeb454052630de7beddcb25f5f8f`.
The recipe pins ABSENT preimages and exact SHA-256 postimages. No Synexia runtime service or
package dependency enters the JDK. Target files retain GPL-2.0-only WITH Classpath-exception-2.0;
Apache-2.0 authoring/proof tooling does not relicense them.

## Implemented boundary

- `M3PrimitiveIdentity`: immediate typed keys for all eight primitives; raw-bit, boxed-value,
  and Java numeric-promotion equality remain separate. No per-scalar native allocation.
- `M3PrimitiveImage`: optional bounded, immutable, sorted/deduplicated OS-mapped catalogue;
  namespace/generation handles and callback-scoped native addresses. It is NOT a live
  multi-writer interner. Inputs/files belong to a trusted immutable publication protocol.
- Linux `M3PrimitiveImage.c`: strict MAP_SHARED + MAP_FIXED_NOREPLACE, exact-result check,
  collision refusal, and cleanup. No destructive MAP_FIXED and no silent address fallback.
- Opt-in `M3PrimitiveIdentityLowering`: existing attributed javac trees/operators lower admitted
  same-type M3 comparisons. Standard javac is unchanged. Raw FP keys, unboxing, mixed keys,
  widening and instance-qualified static evaluation are not blindly rewritten.

The same virtual pointer is meaningful only among participants sharing the SAME immutable
image layout, namespace/generation, admitted base address and live mapping lifetime. Addresses
are not persistent identity; an address from a released mapping must never be retained/reused.
Two processes independently assigning values at the same pointer is not canonicalization.
No ordinary Java reference identity, String storage, boxing contract or mutable array is changed.
The image digest detects corruption; it does not authenticate hostile files or substitute for
exact typed value identity. Read-only mapping does not prevent external file truncation.

## Reproduce the focused proof

Requirements: Linux64, full Java 21, Bash, Python3, gcc, sha256sum; a new output directory.
With the exact Synexia recipe checkout/export available:

```sh
RECIPE=/absolute/com.synexia/synexia-m3-recipe/recipes/m3jdk-primitive-identity-20261016
bash m3/runtime-integration/primitive-identity/verify.sh "$RECIPE" /absolute/new-evidence-directory
```

The driver verifies executable recipe hashes, hydrates exact source candidates, runs replay
and refusal/fixed-point tests, stages ONLY the eight generated files, compiles patched java.base
and jdk.compiler, and builds the exact JNI functions with a stock-JDK test registration adapter.
It runs default/interpreter/C1/C2 contracts, separate-JVM fixed mappings, and normal/lowered
compiler fixtures. It does not compile unrelated modified JDK sources from the checkout.
The GitHub workflow additionally attempts the Maven recipe test gate and retains logs.

## Recorded local evidence

- 26 recipe self-test checks; eight-file replay reaches an unchanged second pass.
- 311,476 primitive/image/JNI assertions in EACH of default, interpreter, C1 and C2 modes.
- Three separately exec-started JVMs verify all eight typed record addresses and contents.
  Captured base: `0x200000000000`; int42 key address: `0x2000000000a8` in each process.
  This base is a local experiment, not a universal supported address ABI.
- 136 fixed-address parent checks; collision leaves the existing mapping intact; validation
  failure releases its reservation; active callbacks prevent close.
- 17 compiler expressions lowered; original/lowered fixture behavior agrees. Separate local
  bytecode checks confirm 14 admitted methods lose helper calls and six refusal cases retain them.

## Pending before product promotion

Maven itself was NOT run locally (no executable). The full M3JDK matched image, jtreg/JCK,
startup/GC/CDS/ASLR coexistence, alternate platforms, retained-heap/native-memory/CPU benchmarks,
live multi-writer admission/reclamation, universal process address reservation, and absolute
address-literal compilation are NOT qualified. A workflow definition is not a passing run.
No String-first receiving phase is advanced. `receiver.tsv` is an explicit pending disposition;
central naming-map integration and consolidation with existing canonical storage owners remain
review gates. Do not merge/promote as a blanket equality or storage replacement.
