# Indicative Guava owned queue timings

Five fresh JVMs per owner, three warmups and three measured batches per case. Entries are nanoseconds per stated cycle (offer workload: per offer), median of five JVM medians and their min/max range. Setup/SHA256 are outside timing; declared refill, clear, iterator and array allocation costs are inside timing.

| Capacity | Workload | Guava ns median [range] | Owned ns median [range] |
|---:|---|---:|---:|
| 0 | eviction-offer | 0.3 [0.3, 0.4] | 0.3 [0.3, 0.3] |
| 0 | mixed-offer-poll | 0.9 [0.8, 6.2] | 0.9 [0.8, 6.6] |
| 0 | interior-iterator-remove | 96.7 [24.6, 102.6] | 32.0 [19.6, 105.6] |
| 0 | removeAll-sparse | 154.4 [78.0, 165.4] | 138.1 [72.2, 148.3] |
| 0 | removeAll-dense | 154.6 [78.8, 165.1] | 139.4 [72.3, 149.7] |
| 0 | oversized-addAll | 163.9 [95.9, 177.1] | 125.4 [48.8, 134.3] |
| 0 | wrapped-typed-toArray | 112.7 [27.6, 120.7] | 106.0 [26.2, 109.6] |
| 17 | eviction-offer | 4.9 [3.5, 6.4] | 4.8 [4.2, 5.2] |
| 17 | mixed-offer-poll | 8.4 [7.3, 9.3] | 8.3 [7.9, 10.1] |
| 17 | interior-iterator-remove | 285.4 [268.2, 314.9] | 295.0 [281.4, 304.7] |
| 17 | removeAll-sparse | 543.5 [515.1, 577.3] | 508.1 [480.7, 568.4] |
| 17 | removeAll-dense | 614.4 [564.1, 644.2] | 569.9 [514.8, 722.7] |
| 17 | oversized-addAll | 332.3 [290.8, 348.4] | 273.6 [224.6, 344.9] |
| 17 | wrapped-typed-toArray | 90.0 [81.8, 216.4] | 172.6 [77.8, 400.4] |
| 1024 | eviction-offer | 2.8 [2.4, 5.0] | 4.5 [4.3, 4.6] |
| 1024 | mixed-offer-poll | 6.0 [4.0, 7.5] | 8.2 [6.3, 9.4] |
| 1024 | interior-iterator-remove | 7479.2 [4272.7, 8196.5] | 7842.5 [7602.1, 9390.5] |
| 1024 | removeAll-sparse | 10001.7 [8954.6, 10578.9] | 12336.0 [12126.3, 14562.8] |
| 1024 | removeAll-dense | 9981.8 [8468.9, 10717.4] | 11369.5 [10301.3, 12078.6] |
| 1024 | oversized-addAll | 3450.4 [3196.0, 3585.6] | 4182.4 [3770.0, 7350.0] |
| 1024 | wrapped-typed-toArray | 579.9 [558.9, 604.5] | 717.0 [614.3, 744.9] |

All 21 workload/capacity digests agreed across both owners, untimed passes, warmups and 630 measured batches. Both frozen owner class files/source and exact provider JAR were unchanged.

Capacity zero is an empty-cursor/array baseline. These local descriptive samples have no confidence interval or general speed guarantee; GC and uncontrolled ambient load remain factors. No whole-library, nativequeue, allocation-rate or new behavioral admission is made.

Full per-batch timing, return checksums, digests, JVM identity, source hashes and commands: RESULTS.json.
