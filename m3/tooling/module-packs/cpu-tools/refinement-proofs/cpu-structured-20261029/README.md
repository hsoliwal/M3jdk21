# M3JDK21 structured CPU atoms — 2026-10-29

These first-party Java 21 primitive-array algorithms extend the existing
`com.m3.tools.cpu` JMOD. Synexia owns the canonical source-sealed recipes:
`m3-cpu-structured-atoms-20261029` in `hsoliwal/com.synexia`.
No recipe source is copied into M3JDK21 and no second public M3 framework
or String representation is added.

The four classes implement exact, scoped atoms:
- `M3CnfSat`: Boolean CNF only, with unit propagation, deterministic bounded
  DPLL and detached verified assignment.
- `M3ArrayGraph`: canonical CSR SCC, reachability bit masks and max-flow.
- `M3StreamMath`: moving mean/EWMA/histogram/entropy/Gini/JS divergence.
- `M3EvidenceMath`: RRF ranking, Pareto frontier and quorum bit masks.

Java sources and fixture tests use signed-byte assignments, CSR primitive index
arrays, bit masks and primitive contiguous rank/score lanes. They do not retain
Map.Entry graphs, donor runtime APIs, native memory handles or String interners.

Independent OpenJDK 21 compiler and seeded oracle tests passed with
`-Xlint:all -Werror` and `-Xcheck:jni`, including interpreter executions.
The four original CPU fixtures remain unmodified; build.py is source-sealed to
include the two new independent oracle programs.

**NOT RUN / NOT QUALIFIED:** real Maven/OpenRewrite SDK receiving/fixed-point,
product full JMOD image and jtreg, whole CPU reactor, Windows/macOS native,
benchmark or full 91-family implementation. Do not mark all 91 families done;
use the exact Synexia attachment index to review remaining APIs.
