<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 state-mask histogram receiver (2026-10-17)

**Canonical Synexia authoring authority:** [PR #12177](https://github.com/hsoliwal/com.synexia/pull/12177)
at source commit `7f70c57ceed2ad24d1ca888db604be74be10e30d`.
Recipe `com.synexia.m3.StateMaskHistogram20261017` is a composition of the existing
Synexia Maven/OpenRewrite `M3HashPinnedJavaSnapshotRecipe` and
`M3HashPinnedTextSnapshotRecipe`. The four files below `templates/` are exact
canonical source mirrors for public reviewer custody, not competing recipe owners.

Production remains under independent Apache-2.0 `m3/collections`; Synexia
adds no dependency to JDK `java.base`, GC, HotSpot, JDK collection public types or JNI ABI.

The payload owner is unchanged: caller-owned dynamic arrays, `int[]` physical
indexes/strides, `byte[]` unsigned 8-bit object-state masks. This new *detached*
statistics owner retains two 256-word primitive planes. Repeat occurrences of a
physical slot count separately. Reuse the current filtered/sorted JDK and primitive
collection wrappers; no per-row object graph or `Map.Entry` retention is introduced.

Native is opt-in and explicitly refuses absence. Only primitive metadata crosses C17
in <=256-byte JNI regions, without reference pinning or callbacks. Indexed native
capture makes an explicitly transient aligned byte plane. No performance improvement
or end-to-end source/binary substitutability is asserted without benchmark/JDK gates.

Generated output SHA-256 pins: `manifest.tsv` and `native-manifest.tsv`.
Local reproduction: `bash m3/collections/recipe/state-mask-histogram-20261017/verify.sh`.
This proves a bounded Java21/C17/JNI contract, **not** full patched JDK,
OpenRewrite execution, JCK/jtreg, Maven module or multi-platform parity.
