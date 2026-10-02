# Exploratory performance receipt — not an acceptance benchmark

Java 21.0.11, Linux x86_64, three fresh JVM forks, G1, `-Xms128m -Xmx128m`.
The source is `bench/AllocationProbe.java`; raw results are in `receipts/probe-*.csv`.
Each operation has three 20,000-operation warmups then 20,000 measured operations.
Results escape through a volatile object sink. Allocation is the current-thread
HotSpot ThreadMXBean counter; it is not retained-heap measurement or process RSS.
Inputs are two already-admitted 4,096-UTF-16-unit values, each containing non-Latin-1
units. This favors payload-sharing joins; it does not represent all text workloads.

| Operation | Observed ns/op across forks | Allocated bytes/op across forks |
|---|---:|---:|
| Stock String concatenation, 8,192 units | 1,067–2,019 | 16,424 |
| M3 retained two-segment concatenation | 312–725 | 240–432 |
| Stock toCharArray | 1,113–1,939 | 16,400 |
| M3 toCharArray | 1,101–2,223 | 16,400 |
| Stock one-unit substring | 31–34 | 48 |
| M3 one-unit slice | 19–33 | 48 |

The retained join allocates less in this workload; it does not allocate zero.
Materialization has identical payload-sized allocation and mixed timing, including
regression in two of the three paired forks. No general speedup is asserted.
The tiny M3 slice retains both original owners and the entire joined directory:
16,384 bytes of original char payload here, excluding all object/metadata overhead.
The stock substring does not retain those original arrays. This is a real retention
tradeoff, not a benefit inferred from low allocation.

The first target used in this probe differs from the final production file only
in its complexity Javadoc; there was no executable-code change. The raw probe is
not full candidate performance acceptance. Cold admission, repeated append at many
segments, collision-heavy distinct owners, cache pressure, live/weak retention,
GC/mapped/native memory, amortization and latency distributions remain unmeasured.
