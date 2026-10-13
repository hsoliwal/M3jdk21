# M3 parallel fabric — JDK bulk-executor provider

This optional adapter connects **existing** JDK-internal `BulkScheduler` to **existing**
`com.m3.parallel` (reference CPU + qualified accelerators). It is not another scheduler.

One reviewed operation, `m3.parallel.int-add.v1`, processes a bounded independent integer lane.
Inputs are snapshotted; results are fresh; the execution uses `Verified.run`, which compares all
bound buffers against the existing pure-Java reference before returning a receipt. Unsupported
accelerators **fail closed**, with no silent CPU fallback.

It does not accept arbitrary lambda/Runnable execution and adds no `m3.parallel` dependency to
`java.base`. The bridge is an optional separate module.

## Focused proof

`bash m3/ports/parallel-bulk/verification/verify.sh` stages the five current JDK-internal bulk
sources, compiles the real `m3.parallel` Java sources using `javac --release 21 -Xlint:all,-module
-Werror`, compiles this adapter and its standalone harness, and runs its seeded differential
suite in JIT and `-Xint` modes. The Maven/JUnit lane is separate.

The proof covers integer overflow, edge sizes, input mutation isolation, exact input and output
schema admission, wrong kernel/geometry refusal and concurrent independent requests.

## Provenance and bounds

- Canonical recipe: Synexia OpenRewrite hash-pinned port packet.
- JDK bulk owner: `jdk.internal.vm.parallel` (M3JDK21), GPL/OpenJDK target.
- Parallel owner: `m3.parallel` (M3JDK21, Apache-2.0 independent module).
- Original TornadoVM backend: `hsoliwal/synexia-tornadovm`, maintained separately.
- Candidate selection never grants mutation or M3 canonical promotion.

The reference implementation proves correctness, not a throughput improvement. Real accelerated
runs remain qualified-device work and require full reference parity and setup-inclusive benchmarks.
