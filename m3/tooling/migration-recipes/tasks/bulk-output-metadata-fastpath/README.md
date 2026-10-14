# Bulk output metadata fast path — 2026-10-14

Source of transformation authority: `hsoliwal/com.synexia#11088`;
`M3BulkOutputMetadataFastPathRecipe` with exact preimage/postimage SHA-256.

Existing JDK family: `jdk.internal.vm.parallel` (mapping already recorded by
M3JDK21 #465). Product owner remains M3JDK21. TornadoVM remains an optional
provider and is never linked into `java.base`.

## Hot-path correction

Previously the scheduler evaluated `execution.outputs().keySet()`.
`BulkExecution.outputs()` deliberately clones every primitive result array.
This caused the scheduler to duplicate all output payloads just to check their
names. It also allocated `Set.copyOf(task.outputs())` despite `BulkTask`
already validating names for uniqueness.

Now `BulkExecution.outputNames()` returns the key-set view of its immutable
snapshot, and the scheduler compares size and membership against the already
unique output list. The extra metadata-check payload copy is eliminated.
Defensive copying remains on construction and public data access.

## Proof obligations

- Exact source-sealed recipe replay, fixed point, drift refusal
- Java21 isolated patched `java.base` compile and old/new jtreg tests
- Full M3JDK21 image build and jtreg
- CPU behavior and output-array ownership unchanged
- Setup-inclusive allocation/cold-warm wall-time evidence before asserting a speedup
- GPU/Tornado hardware parity remains a separate provider-specific gate

Only the algorithmic allocation elimination is established by source mechanics.
No numerical throughput improvement or full JDK acceptance is claimed until
executed evidence exists.
