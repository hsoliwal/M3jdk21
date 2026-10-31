<!-- Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Generation metadata resource qualification

A fresh recovery run compared the untouched pinned donor and the receiver at
**50,000 deterministic namespace rows**, in separate Temurin `21.0.12.1+1-LTS`
JVMs. Each process retained the same prepared public input map, performed three
warmup constructions, and measured three constructions. Both source closures
compiled with `-proc:none -Xlint:all -Werror`; both JVMs exited successfully.

All six measured roots encoded identical **3,451,572-byte** canonical values:
`9ba5076f50d1643150a4ab24c9fea08d10cecb448850edce4facafb4383a694f` (SHA-256).
The input fingerprints also matched.

| Observation | Donor | Receiver |
|---|---:|---:|
| Median current-thread construction CPU | 178.364 ms | 85.615 ms |
| CPU range | 137.126–367.775 ms | 66.877–88.679 ms |
| Median construction allocation | 51,653,824 bytes | 48,653,664 bytes |
| Median incremental post-GC owner heap | 9,395,520 bytes | 9,195,432 bytes |
| Incremental heap range | 8,216,448–9,395,832 bytes | 7,350,808–9,195,744 bytes |
| Sampled process RSS range | 127,272–127,996 KiB | 123,492–124,136 KiB |
| wait4 maximum process RSS | 127,184 KiB | 123,416 KiB |
| Whole-process user / system CPU | 5.148863 / 0.275578 s | 4.497453 / 0.248019 s |
| Exit NMT total committed | 106,382,228 bytes | 106,071,089 bytes |
| Exit NMT total reserved | 1,983,398,804 bytes | 1,981,936,689 bytes |

Current-thread CPU/allocation use `ThreadMXBean` and include the identical
reflective constructor invocation. Heap values sum heap-pool collection-usage
snapshots after three requested full collections; live heap counters remain in
the raw samples. Linux `/proc/self/status` and child-specific `wait4` supply the
process observations. Their reported peak values differ slightly in this
environment; neither observation is substituted for the other. NMT accounts for
whole-JVM virtual memory and does not measure resident native M3FS allocations.

The donor can share prepared input ID objects while the receiver copies their
bytes into columns. Path normalization and canonical serialization are included;
this does not isolate one array change. API enumeration, native filesystem I/O,
JDBC and Eclipse workloads are outside this probe. Baseline heap variation,
shared-host scheduling and JIT/GC activity limit interpretation. Three warmups
do not establish a steady-state benchmark. **No timing or memory assertion, broad
performance guarantee, or isolated native-memory improvement is claimed.**

`resource-measurements.json` is the unchanged final raw receipt, SHA-256
`10b4aecd04223611055512e974db789f2e8591eea4da122e84c5614f58103a0b`.
It records every sample, exact donor/receiver source hashes, generated class
hashes, executable hashes, commands, toolchain release, JVM flags and NMT totals.
The canonical source-only probe is under `verification/metadata-resource-probe`.
The runner verifies donor blobs before compiling; the donor revision is
`hsoliwal/com.synexia@289e32f130c9c8e4a1cf0edc1066327d699393bf`.
