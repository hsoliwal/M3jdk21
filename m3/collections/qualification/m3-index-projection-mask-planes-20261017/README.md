# M3 physical mask-plane projection receiver — 2026-10-17

**Parent:** M3JDK21 #1092 at `5abb627329d67072667237f62c7ef95511da927b`. **Canonical source/recipe:** [Synexia #12085](https://github.com/hsoliwal/com.synexia/pull/12085). **Package owner:** existing `com.m3.collections.M3LongIndexProjection`. No changes to public `java.util`, M3 String representation or JNI ABI.

New `filteredByStatePlanes(int[][], int[], int[], M3IndexPathMasks.Backend)` and `firstMatchingOrdinalByStatePlanes(int[][], int[], int[])` invoke the EXISTING `M3IndexPathMasks` primitive operation using the immutable index as both row and physical-offset input. Existing single-byte `filtered`, stable indirect long sorting, range slicing, direct address load kernel, bitmap precompute and live sorted/filtered wrappers are preserved verbatim.

A product slice has shared logical bounds `from` and `to`, so `M3IndexPathMasks` runs with `first=from, count=size(), stride=1`. First-match absolute index is converted into view-local ordinal. No `physicalIndices()` clone is created for the new precomputed mask path. Input planes remain caller-owned and must be stable throughout selection. Output snapshot owns its fresh primitive coordinate vector, unless all rows are retained, in which case the identical immutable view can be reused. Java is default; native-required executes the existing real C17 JNI lane or throws when unavailable.

**Source receipt** (SHA-256, as supplied by canonical Synexia recipe):
- M3LongIndexProjection.java before: `48f6ed4064dde5ce385c80bb4faf74eec7bccb7c6c438ad6b1ef09b65fbf10ac`
- M3LongIndexProjection.java after: `c6159ed8f93b8ed364cd67bd2aeb82e246a7eaf93506cf4b38ac5a0ec09ae2ec`
- M3LongIndexPathMaskProof.java after: `07dd1d5312e1a5827852ae402c8d2f1256e11ed4c8ad8ca3e4ca88389d753b7c`

The standalone proof runs sorted ascending/descending views, slices including empty ranges, multiple 32-bit mask stages, physical-ID versus logical-ordinal differential oracle, duplicate values, integer extremes, all-admitted identity sharing, invalid inputs, and optional checked-JNI parity.

**Gate distinction:** Java21 source-extracted module kernel and checked native C17 parity are narrower than full M3JDK21 build, all Maven/JUnit/reactor tests, JCK/JTReg, native memory/GC/JIT, and same-host A/B allocation and throughput performance. Do not merge until all required gates pass. No unmeasured performance gain is claimed.

Algorithm/category evidence and donor/license choices remain in Synexia's `CATEGORY_REVIEW.tsv`. Recipe-first chronology is immutable: inventory → dated convergence → catalogue → Inline → Atomize → Patternize → Comment → compile/test → reviewable PR.
