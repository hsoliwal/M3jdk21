<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 CPU sliding-window tool admission

Implemented source candidate in M3JDK21 as an optional Java 21 capability
module, not an alteration of java.base or a promoted JDK/String ABI.

**Canonical Maven/OpenRewrite source recipe:** hsoliwal/com.synexia,
`M3CpuByteWindowRecipe`, at commit
`13253a623536376b9d290057caccd3026a7675e9`.
Two source files correspond to the exact pinned source postimages under
Synexia's `hash-pinned-java/m3-cpu-byte-window-20261018/` resources.
The target repository owns the received Java source and executable gates,
and this metadata is a receipt, not a second recipe authority.

Category: stream / sliding-window processors (CPUTOOLS(6).txt).
Algorithm: incremental unsigned-byte frequency counts for each fixed-width
source window; 256 primitive int counters; no Entry nodes, heap-sized
per-window list, text conversion, parallel string store or native ABI.
Offsets are source-relative, not M3String UTF-16 positions.
On overlapping windows the input delta is updated; on disjoint windows
the old counts drain and new counts fill. Modal ties choose smallest
unsigned byte. The output budget is validated before callback emission.
No call to external GPL/BSD Unix executables or code.
Existing SourceArtifact.Span owns bytes. M3String's OS-backed interner
remains the text boundary, not part of this byte algorithm.

Verification: `java --source 21`/Maven module verify executes the
standalone differential test. A fixed random seed covers source sizes
0..129, widths 1..17, steps 1/2/3/8/19/137, inclusive
unsigned-byte histogram modes, empty ranges and rejection before
callbacks. Native/JNI parity, whole-repository build/jtreg and
memory/performance benchmarking are separately pending. Reuse earlier
M3 sources; no licensed donor source copied.
