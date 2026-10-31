# Preliminary paired String run comparison observations

These are actual-owner elapsed-time observations in the prior built M3 JVM, with
source patches explicitly pinned to receiving commit
`18213fe70c66567fdfc6ba6f669909e7e9251663`. Both arms compile `String.java` and all
17 `M3String*.java` owners from that pin; only `M3String.java` and
`M3StringPrecomputeBridge.java` differ in the candidate. The exact hash manifest is
`observation-sources.json`. The prior native/VM image is shared between the arms and
is not a fresh build of this receiver. This is preliminary evidence only.

Three paired runs use order baseline/candidate, candidate/baseline,
baseline/candidate, each in a fresh JVM. A concurrent six-job HotSpot build competed
for CPU, so these results are tagged noisy and are not release performance claims.
The driver uses the existing actual String owner construction and reflection pattern;
it introduces no benchmark library, retained production instrumentation or product API.
Neither CPU time, allocations, memory retention, GC work nor native resource usage was
measured. All reported times are System.nanoTime elapsed intervals.

## Corpus and measurement controls

Each size has shared equal ranges, a shared prefix with only the last unit different,
and equal text backed by independent scalar owners. Each pair has distinct surrounding
tuple owners/range coordinates, verified with actual CodeUnitCursor metadata. At 256
units the shared cases have 256/255 exactly shared units; at 4096 they have 4096/4095;
independent cases have zero. All six corpus metadata records match exactly across all
six JVMs, including independent spelling hash and initially unprepared range facts.
No String hashCode call prepares a shortcut for the late-mismatch equality case.
The final compatibility-array identity checks also pass.

Cold means the first measured call of an operation on that fixture after construction
and metadata inspection. It is not process-startup cold, empty-pool cold or a cleared
CPU-cache experiment. Warm means a measured batch following 32 untimed calls of the
same operation; batches have 512 calls at 256 units and 128 calls at 4096 units. Every
cold result and batch checksum is checked against the independent text oracle.

Tables show the median of three fork measurements in microseconds per operation.
Each warm fork measurement is its batch elapsed time divided by its iteration count.
Raw totals and every fork's cold sample are preserved in `observation-measurements.json`
and the six original logs. Counts and shared-unit proofs are not treated as speedups.

## Warm elapsed observations

| Units | Corpus | Operation | Baseline median us/op | Candidate median us/op | Baseline min-max | Candidate min-max |
| --- | --- | --- | ---: | ---: | ---: | ---: |
| 256 | shared_equal | equals | 146.426 | 13.661 | 133.026-164.555 | 13.556-14.528 |
| 256 | shared_equal | compareTo | 149.953 | 14.666 | 132.175-160.617 | 14.311-17.711 |
| 256 | shared_equal | startsWith | 140.638 | 14.066 | 129.594-207.839 | 12.647-24.200 |
| 256 | shared_late_mismatch | equals | 136.209 | 17.762 | 132.396-143.635 | 15.030-21.694 |
| 256 | shared_late_mismatch | compareTo | 133.432 | 15.511 | 130.976-136.345 | 14.748-16.641 |
| 256 | shared_late_mismatch | startsWith | 145.228 | 15.672 | 142.703-155.921 | 15.232-17.689 |
| 256 | independent_equal | equals | 143.062 | 95.643 | 130.491-143.816 | 93.242-99.007 |
| 256 | independent_equal | compareTo | 132.024 | 98.892 | 128.980-133.423 | 90.876-103.344 |
| 256 | independent_equal | startsWith | 134.138 | 87.715 | 132.952-166.489 | 87.084-103.638 |
| 4096 | shared_equal | equals | 2088.282 | 28.101 | 2047.162-2179.755 | 27.053-28.750 |
| 4096 | shared_equal | compareTo | 1914.005 | 29.758 | 1791.267-2139.779 | 28.242-44.534 |
| 4096 | shared_equal | startsWith | 2028.419 | 29.076 | 1923.707-2147.103 | 27.214-30.907 |
| 4096 | shared_late_mismatch | equals | 1935.966 | 32.994 | 1930.727-2028.434 | 32.264-33.933 |
| 4096 | shared_late_mismatch | compareTo | 1981.282 | 32.265 | 1826.716-2426.920 | 30.860-33.242 |
| 4096 | shared_late_mismatch | startsWith | 1914.790 | 32.754 | 1907.958-2969.749 | 31.554-33.036 |
| 4096 | independent_equal | equals | 2218.093 | 1253.374 | 1977.996-2519.651 | 1187.556-1304.347 |
| 4096 | independent_equal | compareTo | 1911.075 | 1247.687 | 1842.678-1912.440 | 1188.135-1284.842 |
| 4096 | independent_equal | startsWith | 1937.390 | 1268.087 | 1916.997-2008.872 | 1206.637-1279.185 |

## First fixture-call observations

| Units | Corpus | Operation | Baseline median us | Candidate median us |
| --- | --- | --- | ---: | ---: |
| 256 | shared_equal | equals | 186.071 | 40.381 |
| 256 | shared_equal | compareTo | 159.872 | 34.752 |
| 256 | shared_equal | startsWith | 179.481 | 33.401 |
| 256 | shared_late_mismatch | equals | 142.345 | 24.728 |
| 256 | shared_late_mismatch | compareTo | 141.894 | 21.813 |
| 256 | shared_late_mismatch | startsWith | 146.762 | 21.923 |
| 256 | independent_equal | equals | 185.751 | 108.053 |
| 256 | independent_equal | compareTo | 137.467 | 102.485 |
| 256 | independent_equal | startsWith | 145.630 | 99.310 |
| 4096 | shared_equal | equals | 2037.705 | 59.190 |
| 4096 | shared_equal | compareTo | 2301.875 | 34.202 |
| 4096 | shared_equal | startsWith | 2062.894 | 32.850 |
| 4096 | shared_late_mismatch | equals | 2191.908 | 39.099 |
| 4096 | shared_late_mismatch | compareTo | 1993.079 | 37.166 |
| 4096 | shared_late_mismatch | startsWith | 2185.208 | 37.636 |
| 4096 | independent_equal | equals | 2026.459 | 1471.451 |
| 4096 | independent_equal | compareTo | 1971.136 | 1270.848 |
| 4096 | independent_equal | startsWith | 2031.737 | 1308.134 |

## Evidence limits and rejected setup

The observed fixture timings support further controlled installed-image measurement;
they do not establish workload-wide speedups. This corpus exercises long tuple paths,
not short/scalar-only inputs, flat-side comparisons, general user key distributions or
compiled M3 mode. Retain the source build, correctness and recipe gates independently.

An earlier setup copied the live receiver while root was recipe-emitting it. Its hash
audit found identical candidate owner sources in both arms, so every timing from that
setup was rejected and retained under `rejected-observation-live-worktree`. The corrected
script uses pinned git objects for every common/baseline source and checks the baseline
M3String preimage hash before compilation. No data from the rejected setup appears in
these tables. No frozen candidate source was changed during this measurement.
