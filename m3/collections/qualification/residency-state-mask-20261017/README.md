<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 parent-owned 64-bit residency-state indexed selection

**Product owner:** hsoliwal/M3jdk21. **Algorithm and Maven/OpenRewrite recipe source:** hsoliwal/com.synexia.
This is an additive primitive-array adapter, not another collection storage owner.
The existing M3ObjectResidency owns Object[] children, long[] parentBits, byte[]
lifecycle and pin/borrow metadata. None of those owners or java.base/HotSpot/JNI
interfaces is modified.

M3ResidencyStateMaskIndex scans a caller-owned int[] of physical slot coordinates
through a signed logical offset/count/stride path. It combines required/forbidden
64-bit parent-state masks with a six-state lifecycle bitset and retains duplicate
row IDs and encounter order. No child is constructed, escaped or retained by
selection. The arena lock protects parent reads; callers must keep the coordinate
array stable, including against unrelated writers.

- select: detached exact-size int[k], Java default
- count / first: no allocated result array
- selectInto: caller-owned int[] segment, O(1) extra storage, preflight capacity
  validation, explicit input/output alias refusal and unchanged destination
  on invalid path or insufficient capacity
- JNI_REQUIRED: explicitly opt-in C17 Region reads against ephemeral int/long/
  byte primitive snapshots (O(n) extra memory); no native Java roots held

**Receipt sequence:** Synexia first commits the canonical named OpenRewrite
`com.synexia.m3.M3Jdk21ResidencyStateMask20261017` recipe, test and SHA-256
before/after manifest, then this receiver mirrors exact templates and commits
only the generated product source/test/C files as a second commit.
No hand patch of generated receiver files is allowed.

**Verification:** `sh m3/collections/qualification/residency-state-mask-20261017/verify.sh`
checks exact Java/C17 source identities, compiles against the real
M3ObjectResidency (no test double), generates JNI headers, compiles the C
library, and compares Java and JNI execution under Serial, Parallel, G1,
ZGC, Shenandoah and G1 interpreter. Also run existing collection Maven/JUnit
tests and the canonical Synexia OpenRewrite SDK test in its owning reactor.

**Locally executed limited proof:** Java 21.0.12.1, C17 GCC; 9,627 Java
and 11,226 native test-double API checks, identical seeded fingerprint
12197969927824375845 across five collectors and interpreter. These do NOT
establish actual M3JDK21 product build success or a native speedup.

**Promotion holds:** actual hosted canonical Maven/OpenRewrite Recipe.run,
actual product receiver Java/JNI, source-installed inventory and #1002's
source manifest successor, native Windows/ARM, debug JDK jtreg/JCK,
GC/compiler ownership proofs and benchmarking. Keep review PR draft.
