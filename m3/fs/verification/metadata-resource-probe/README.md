<!-- Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Generation metadata resource probe

This finite probe constructs the untouched, pinned Synexia `M3GenerationRoot` and
the M3JDK21 receiver in separate Java 21 processes. It retains an identical
deterministic prepared namespace of 50,000 paths and public content IDs in each
process, performs three warmup constructions, and records three measured
constructions. The probe verifies that the canonical bytes and root IDs match.

For each construction it records current-thread CPU time and allocated bytes
through `com.sun.management.ThreadMXBean`, wall time, post-GC heap usage relative
to the retained input, and Linux `/proc/self/status` resident-memory counters.
Heap usage sums heap-pool collection-usage snapshots after three requested full
collections; separate live `Runtime` heap counters are also retained in each sample.
Linux `wait4` records child-specific peak RSS and whole-process CPU use. The JVM emits
an exit Native Memory Tracking summary. No timing or memory assertion is made.

Run with an exact checkout of donor commit
`289e32f130c9c8e4a1cf0edc1066327d699393bf`; the runner verifies both donor source
blobs before compiling. The receiver is compiled from its actual staged source,
and all source files, generated class files, Java tools, commands and JVM flags
are hashed or recorded in `resource-measurements.json`.

```sh
python3 run_probe.py \
  --jdk /path/to/jdk21 \
  --donor /path/to/com.synexia/synexia-indexstring \
  --receiver /path/to/M3jdk21/m3/fs/src/main/java/com/m3/fs \
  --out /path/to/qualification/metadata-resource-probe
```

`--receiver` may be repeated for split staging directories. `--rows` accepts at
most 100,000 rows; the published receipt must state the actual value.

The retained-heap delta measures the incremental owner against a live public
input map. The donor may share input ID objects while the receiver copies IDs
into packed bytes. This is deliberately included in the workload. The input
map itself, API enumeration, native filesystem operations, JDBC and Eclipse are
outside the measured construction interval. Reflection overhead is included
equally in both modes. Current-thread counters omit compiler and GC threads;
whole-process counters include them. RSS is complete process residency, while
NMT committed or reserved memory is virtual-memory accounting. Neither isolates
M3FS native allocations. Shared-host scheduling, garbage collection and JIT
compilation can change individual samples. These measurements qualify the stated
workload and do not establish a general performance guarantee.
