# Sorted-descending allocation observation

**The qualified receiver allocated 131,048 bytes per call, compared with 262,064
bytes for the exact baseline, in the existing proof's specified workload.** The
observed reduction is 131,016 bytes per call, or approximately **49.99%**, for
converting a 32,750-entry ascending projection slice to stable descending order.

This is a narrow observation of JVM-reported allocated bytes. It does not measure
elapsed time, CPU efficiency, JNI performance, retained heap, garbage collection,
or other collection operations.

## Reuse decision and actual inputs

[BEFORE_CODE.json](BEFORE_CODE.json) records the owner comparison, FOSS reuse
decision, hypothesis, and matched protocol before measurement. The measurement
reuses the existing Apache-2.0 `M3OrderWindowProof` allocation mode, unchanged, from
the qualified `m3jdk21-wrappers-r2-20261018` recipe dataset. There is no new Java
benchmark implementation or product-source edit.

The exact proof source SHA-256 is:

```text
633ad8b6e70b211efc1917daea6277b38632359f9442120396fbeb187e663cf0
```

The baseline is the complete collections module at M3JDK21 commit
`2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`. Its unchanged module build passed all
232 tests. The receiver contains the qualified r2 wrapper changes and the final
qualified long-path bridge; its complete module build passed all 255 tests.
Those runtime gates are retained as independent evidence, not inferred from the
allocation samples.

The proof was compiled with strict Java 21 checks against each actual complete
134-file main-source closure. Both compilations produced identical proof
bytecode. The same generated proof class files were then used for every baseline
and receiver JVM. Full source and class inventories were sealed and rechecked
after measurement. No external or project JAR was on the runtime classpath;
the actual module class directories and the JDK module image were hashed instead.

## Fixed workload and configuration

The original proof creates 32,768 long values in equal-value groups of eight,
builds an ascending projection, and slices from offset 5 with count 32,750. Each
fresh JVM performs 100 warmup calls and then three samples of 100
`sortedDescending()` calls. The existing volatile escape field retains each
returned projection long enough to prevent elimination of the call result.
Setup, warmup, and sample printing occur outside the measured loops.

All six measurement JVMs used the same Java 21 JDK and configuration:

```text
-Xms256m -Xmx256m -XX:ActiveProcessorCount=2
Linux CPU affinity: 0,1
```

The run order was baseline 1, receiver 1, baseline 2, receiver 2, baseline 3,
receiver 3. Maven and native compilation workloads were paused during the six
runs. The allocation mode does not invoke the JNI paths. The metric is the
current thread's `com.sun.management.ThreadMXBean.getThreadAllocatedBytes`
difference around the original 100-call loop.

## All raw samples

Each table cell is allocated bytes for **100 calls**. All raw standard output and
error files are retained in `evidence/runs/`.

| Lane | Fresh JVM | Sample 0 | Sample 1 | Sample 2 | Median bytes per call |
| --- | ---: | ---: | ---: | ---: | ---: |
| Baseline | 1 | 26,206,400 | 26,206,400 | 26,206,400 | 262,064 |
| Receiver | 1 | 13,104,800 | 13,104,800 | 13,104,800 | 131,048 |
| Baseline | 2 | 26,206,400 | 26,206,400 | 26,206,400 | 262,064 |
| Receiver | 2 | 13,104,800 | 13,104,800 | 13,104,800 | 131,048 |
| Baseline | 3 | 26,206,400 | 26,206,400 | 26,206,400 | 262,064 |
| Receiver | 3 | 13,104,800 | 13,104,800 | 13,104,800 | 131,048 |

All nine observations within each lane were equal; all three per-JVM medians
therefore agree. The three samples in a JVM share VM state. They are not three
independent fresh JVMs, and this packet makes no confidence-interval claim.

The baseline's opposite-order sort allocates an output index copy and a merge
scratch vector. The receiver uses its existing order information to reverse the
order of equal-value groups into a single index vector while preserving encounter
order within each group. That source comparison explains the removed temporary
array; the allocation counter quantifies the effect for this workload. Existing
contract/runtime proofs, rather than these allocation samples, establish order
and tie correctness.

## Reproduction and custody

[observe.py](observe.py) only orchestrates the existing proof and records receipts.
It requires complete, already-built actual module closures; it does not build an
alternate implementation or replace dependencies with fixtures. Use a new output
directory and the source/build roots described in [PROTOCOL.md](PROTOCOL.md):

```sh
python3 synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017/tasks/m3-order-window-allocation-20261018/observe.py \
  --baseline-root /path/to/exact-baseline \
  --baseline-build-root /path/to/baseline-build \
  --receiver-root /path/to/qualified-receiver \
  --receiver-build-root /path/to/receiver-build \
  --java-home /path/to/jdk-21 \
  --output /tmp/m3-order-window-allocation-new
```

Each root contains the `m3/collections` module. The checked-in protocol fixes CPUs
0 and 1, so those CPUs must be available for an exact replay. The runtime JDK and
source/class bytes are recorded to make environment differences visible.

The results, raw samples, commands, proof-source snapshot, generated class hashes,
toolchain hashes, cleared optional JVM environment settings, source inventories,
and unchanged input rechecks are under `evidence/`. [BINDINGS.json](BINDINGS.json)
connects them to the baseline build, final receiver qualification, and retained
actual SDK output seals. The original SDK and runtime gates remain independent.

[ALGORITHM_CATALOGUE.tsv](ALGORITHM_CATALOGUE.tsv) preserves the premeasurement
decision and appends the measured allocation result for reuse in later passes.
