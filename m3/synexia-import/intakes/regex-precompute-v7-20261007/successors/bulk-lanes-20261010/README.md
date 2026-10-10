<!-- SPDX-License-Identifier: Apache-2.0 -->
# Primitive-lane decoder candidate

Copyright 2026 Hitesh Soliwal and contributors.

This successor applies the Synexia recipe `com.synexia.m3.RegexV7BulkLanes20261010` at `8918f154e38da11a788e4b108048bcb47da44ee0`
to exact receiver `57a2bdafd0acd0779663e438f82f8a295f4c2170`. Five existing primitive decode
loops use JDK typed-buffer bulk reads for at least 64 elements, after the original
overflow and remaining-byte validation. Scalar short lanes, public descriptors,
wire versions 1–7, buffer ownership and both preceding copy reductions survive.
No native ABI, public JDK name or canonical M3 payload owner changes.

The generated receiver codec is byte-identical to the donor candidate. Source
proof passes four SDK tests, 33 runtime JUnit tests per variant, 427,008 JDK
comparisons per variant, 4,096 new valid lane/transport cases, 96 invalid-count
cases and two required JNI tests under `-Xcheck:jni`. Both protected mutation
controls fail as intended. Reusable implementation, recipes and fixtures remain
canonically in Synexia; this receiver retains exact source and thin receipts.

Five alternating cost trials show mixed whole-image CPU results and up to about
0.7 KB additional Java allocation for the large fixture. No general speedup or
retained-memory improvement is established. See `cost-summary.json` for medians
and ranges and the canonical packet for raw cold/warm observations.

**Draft / NOT_ADMITTED.** These are donor results, not qualification of this
target's different full Program owner or JDK image. ContextOS closure, serial
catalogue/reactor review, annotation processing, bootstrap cold/runtime split,
matched JDK/jtreg, other platforms/GPU and retained/native/process-memory gates
remain open. Existing target CI additionally reports unclassified recipe owners,
missing SLF4J and stale/missing backport inputs; no gate is weakened. Prior sealed
intakes remain immutable. String remains the active phase.
