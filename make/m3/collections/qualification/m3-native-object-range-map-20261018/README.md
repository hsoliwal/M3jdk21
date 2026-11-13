<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Native generic object-range collection: M3JDK21 receiving proof

Canonical Synexia source-first recipe:
[hsoliwal/com.synexia#12254](https://github.com/hsoliwal/com.synexia/pull/12254),
exact generated-source authority at
`114ffba6809ed7790328ec2c4ca7218dc390342c`.
This recipient uses byte-identical noncanonical proof resources under
`recipe-mirror/` and no Synexia runtime dependency.

**Actual implementation:** `M3ObjectRangeMap<K,V>` with retained
`Object[]` start/end/value lanes, aligned full32 `int[]` state mask
lane, binary search, checked dynamic capacity, in-place aligned
compaction/shift of range spans, null values/keys when comparator permits,
clear and GC-reference release, and no per-span Java Node/Entry objects.

`IndexView` is a versioned primitive physical-ordinal permutation
with half-open metadata slice, reverse, stable value sorting, and
once-per-span filtered selection; its full32 state selection uses the
existing M3IndexPathMasks C17/JNI backend *only* on int[] row offsets and
state planes. No Java reference, predicate or comparator is passed to C.
JNI remains opt-in and never silently falls back.

The source is first-party M3, not a thin adapter to Guava/Eclipse/
Spring/Apache providers, and is not `M3LongRangeMap` relabeled.
This native collection intentionally does not claim any vendor-specific
concrete class identity or exhaustive provider API parity.

## Executed and pending tests

**Local executed**: strict JDK21 javac with test-only shapes of
M3SlotArrays/M3IndexPathMasks, 706,260 deterministic checks normal,
706,260 checks interpreted JVM. 12,000 randomized generic range writes,
removals, null payloads, comparator-direction handling, in-place
splits, immutable index windows and 32-bit state tests.
Real M3 dependency compilation and JNI were NOT part of that run.

**Receiving workflow must execute**: Python byte/hash source check,
real target `javac --release 21 -Werror` dependency closure, 12k
normal and interpreted randomized operations, actual GCC/C17+UBSan
`m3_index_path_masks.c` compilation, 600 sampled actual native-vs-Java
reference-valued index selection cases using `-Xcheck:jni`, real owning
Maven/JUnit, and the Synexia Maven/OpenRewrite fixed-point/refusal
gate. Workflow creation does not prove a PASS.

Full JDK image, jtreg/HotSpot/GC, cross-platform JNI, CPU/retained
heap benchmarks, user-defined concurrency, full all-five-library
source-compatible class and method coverage remain pending.

This is one real reference-bearing native M3 family. Do not claim
the entire collections universe delivered. The active String→arrays→
collections→AST/compiler receiving sequence and all additive history,
existing owner classes and first-party Apache-2.0 rights are retained.
