<!-- SPDX-License-Identifier: Apache-2.0 -->
# Collections optimization pass, 2026-10-20

Brief rule 23 / `C:/m3-superset/tools/OPTIMIZATION_PASS.md`: measure first, then one leaf at a time, each with a
before/after number recorded in `docs/m3/collections-superset/OPTIMIZATION_LEDGER.tsv`.

`OwnedVsDonorBench.java` measures every owned five-provider class against its donor type at 10^5 and 10^6:
build time (fill a fresh instance), query time (n lookups plus one traversal), each the median of 5 runs after
5 warm-ups; bytes allocated per element while building and per query (ThreadMXBean, minimum of 5); and heap
retained per element after GC (10 instances at 10^5, 2 at 10^6; enum-keyed owners per instance over 10,000
copies). Every (owner, size, implementation) runs in its own JVM with `-Xms3g -Xmx3g -XX:+UseSerialGC`, so the
donor's call-site profiles never affect the owned class. `run-bench.sh` is the driver (paths are this
workstation's: owned module sources, provider jars, output under `S:/m3-scratch/collections/bench`).

`baseline-20261020.tsv` is the measured starting point (JDK 21.0.9, Windows 11); `baseline-20261020-median3.tsv` repeats it
as the median of 3 JVMs. Where the owned class is
slower or larger than the donor, a leaf follows; a measured "no" is recorded too.

Leaves (`leaves/`) are paired A/B runs (`run-ab.sh`): the owned class built from the base revision and from the
leaf revision, alternating JVMs, 7 pairs per size. This workstation is shared with other tracks (CPU load at
100% of 32 threads during the runs), and outside load only ever adds time, so `agg.sh` reports the minimum
over JVMs of each JVM's median-of-5 time, and the median for allocation and retained heap; `*.raw.tsv` keeps
every run.
