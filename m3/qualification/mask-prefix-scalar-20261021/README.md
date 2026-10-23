<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 integrated collection array/offset/mask refinement

**Product:** hsoliwal/M3jdk21, Java21 java.base itself, not M3 plus a JDK.
**Stable capability:** m3.java-base.mask-prefix-scalar-20261021, recorded in
m3/docs/name-mapping.json migration.records. No JDK API or class rename.
**Original implementation owner:** jdk.internal.mindex.M3CollectionMaskCompactor.
**Original caller:** java.util.ArrayList removal-mask path.
**Canonical source recipe:** com.synexia.m3.M3Jdk21MaskPrefixScalar20261021
in hsoliwal/com.synexia, branch m3/array-mask-prefix-recipe-20261021.

## Actual code change

This replaces a single, private scalar compaction atom in the existing Java
implementation. Bit positions and write indexes remain relative to the caller's
physical Object[] window [from,to). The original owner already counts deletion
runs over primitive long[] masks and uses System.arraycopy on long retained
runs. That original bulk path is unchanged.

The scalar fallback now finds its first deleted bit with the existing next()
method. Before that first deletion, all references occupy correct physical
positions and need no self-assignments or associated GC-barrier traffic. When
there are no deletions, it returns to without touching the array. Otherwise
it compacts only the suffix into the same dynamic array. No Entry/Node object,
additional Object[] owner, JNI symbol, raw heap pointer or runtime adapter is
created. The original ArrayList API, modCount, exception/visibility contracts
and caller-owned tail clearing are not changed.

## Catalogue-driven comparison

The existing searchable category ledger in
m3/collections/algorithm-catalog/java-base-filtered-sorted-receiver-20261020.tsv
is extended for MASK_IN_PLACE_SCALAR_PREFIX. LeetCode 27 gives the stable
remove-element problem shape; HackerRank Dynamic Array gives offset/index
scenarios; GeeksforGeeks provides alternative in-place deletion/bitset taxonomy.
Existing Synexia and M3JDK21 array-mask owners were inspected first. Only
the already-owned M3 word-level next/removalRuns method was reused. No challenge
submission/editorial or unreviewed external GitHub code was copied.

## Source/readback

- Before original java.base Git blob: 51ea3a72bb191afc250a58be964363bd5680467c.
- Direct product postimage Git blob: f85e7359010bd25e0a909a145e283007cff25d7c.
- New independent test Git blob: d720912f0f57bcde9fc07a05f31869a19f3452eb.
- Both exact postimages are first held as canonical Synexia recipe templates;
  the product branch readback returned the exact same Git blobs.
- The product Java file retains its GPLv2+Classpath original header.
  Reusable Synexia recipe/fixture licensing is separately attributed.

## Actually executed Java 21 evidence

The exact original and candidate compactor sources each compiled in a
patched java.base Java 21 module with -source 21 -target 21 -Xlint:all -Werror.
Both separately executed M3CollectionMaskCompactorPrefixTest in mixed mode, and the candidate also passed under -Xint, with
**2,371,785 assertions per run**. Coverage includes offsets 0/1/13, null and
reference identity, lengths 0 to 4095 around 64-bit word and 512-element
dispatch boundaries, nine mask scenarios, 2,000 seeded random masks, poisoned
out-of-range mask bits, original exception classes and unchanged tail slots.
No full JDK image, JNI native library or quantitative performance test was
executed by this isolated local proof.

## Target and remaining verification

Target CI invokes m3/qualification/mask-prefix-scalar-20261021/verify.sh.
It checks exact Git-source pins, compiles both the original java.base
M3CollectionMaskCompactor and java.util.CollectionViews under Java 21
patch-module rules, runs the new mask regression and reuses the existing
BootstrapViewsTest and CollectionViewsContract, including filtered/sorted
live/descending/range and deep predicate behavior. CI must show all PASS
records; queued/zero-job/failed runs are not a pass.

Canonical recipe execution remains pending:

./mvnw -B -ntp -f synexia-openrewrite-recipes/pom-m3jdk21-mask-prefix-scalar-20261021.xml test

Full product gates **NOT_RUN**: complete OpenJDK configure/make source-built
M3JDK21 image; jtreg/JIT (-Xint, C1/C2), GC/card-barrier/intern/HotSpot;
native JNI mask parity where integrated; all five collection providers;
CPU (cold/warm), allocation and retained heap, native/process memory and
platform performance regression. Correctness of this small atom does not
promote the full arrays or collections receiving phases.

Do not merge related stacked PR bodies by copying whole files: qualify the
existing filtered/sorted wrapper improvements separately against the current
owner in #1734 and associated Synexia recipes. Keep one serial file writer,
review SHA-256 identities and improve the canonical recipe upon counterexamples.

## Original JDK caller / HotSpot qualification extension — 2026-10-22

Direct caller inventory confirmed both original `java.util.ArrayList.removeIf` and
`java.util.Vector.bulkRemove` find the first rejected index and set
`deathRow[0] = 1L` before invoking `M3CollectionMaskCompactor` for
windows of at least 512 elements. Therefore the candidate's leading-prefix
skip has **no demonstrated hot-path speedup** for these callers; the precomputed
first-bit fast path may add work. Retain it only as a contract-qualified
general compactor candidate pending measurements.

The exact new test `test/jdk/java/util/ArrayList/M3ArrayMaskCallersContract.java`
was first saved under canonical Synexia recipe #12851 (Git blob
`81fcd00c088dd9ae8ed203b71a59a1dae6fdee7e`).
It preserves source identity and independently asserts original collection
behavior: selection and tail clearing, physical element reference identity,
nulls, duplicates, every predicate call, pre-existing iterator modCount/CME,
no-op mutation neutrality, rollback on predicate exceptions, and reentrant
structural mutation refusal. Stock OpenJDK 21 passed **365,398 assertions**
in normal and interpreted mode locally.

The product-scoped `verify.sh` now source-pins the real, unchanged
`ArrayList.java` and `Vector.java`, compiles them directly in
`java.base` with the candidate compactor and `CollectionViews`,
checks both compiled call sites via bytecode, then executes original-JDK
baseline and patched-JDK implementations under mixed and interpreted
modes. Exact script/CI output remains pending qualification.

This is still a focused java.base **patch-module** proof, not a
`make images`/jtreg/JCK installed modified OpenJDK build or a JIT/GC
performance benchmark. New tests may fail and must be fixed in the
canonical recipe first; no equivalence or speedup is inferred from queued CI.


## Reused M3JDK21 original qualified Java/JNI tests — 2026-10-22

The receiver's proof script now also reuses the unchanged original
`test/jdk/java/util/ArrayList/M3MaskCompactionDifferential.java`
(Git blob `c505b5604e978f215e4aeb93eadfd9f933ffc743`) and
`M3JdkRemoveIfMaskContracts.java`
(Git blob `2dc2fb6494f008fb7166dabfb97000d282a5f9af`).
Those cover the established 25,000-case sparse/dense/full-deletion
mask differential, nested `ArrayList.subList`, `Vector`, fail-fast,
exception and callback behavior. Actual patched `ArrayList.class` and
`Vector.class` public declarations are compared to original Java 21
with `javap -public` on exact classfile paths. These source tests are
**reused**, not reimplemented.

The focused GitHub workflow additionally calls the existing
`m3/collections/qualification/required-excluded-windows-20261021/verify.sh`
to compile the original C11 `m3_index_mask_compactor.c` and
execute Java vs real JNI with `-Xcheck:jni`. That is a companion
M3JDK21 state-mask JNI proof, **not** a claim that this Object[] compactor
calls native C or creates a new JNI ABI.

The patched Java 21 `java.base` owner tests are configured under
normal, interpreter, Serial GC, Parallel GC and G1 GC execution.
Host JDK 21 passed **365,398** direct caller checks in baseline
normal, interpreted, Serial, Parallel and G1 modes locally.
The new patched modes, original donor suites, public ABI gate, and
native C11 gate are committed but **PENDING actual PR CI results**.

Full `make images` of M3JDK21 with a matching Boot JDK, all jtreg/JCK,
platform-specific JNI, JVMTI, CDS, ZGC, Shenandoah and HotSpot/JIT
phase-lifetime oracles remain separate `NOT_RUN` gates. No formal
performance admission or universal equivalence is asserted.


## Full modified OpenJDK image and real jtreg gate

The focused PR workflow now also has a separate heavy
`source-built-jdk-jtreg` job. It reuses the repository's original
`.github/actions/get-bootjdk` and `get-jtreg` actions, runs
`bash configure`, `make images`, and then the six existing/new original
JDK jtreg contract classes in the **built M3JDK21 image**:

- `M3ArrayMaskCallersContract`, `M3MaskCompactionDifferential`,
  `M3JdkRemoveIfMaskContracts`, `M3CollectionMaskCompactorPrefixTest`;
- `BootstrapViewsTest` and `CollectionViewsContract`.

This is source-image qualification, not merely `--patch-module`.
It is deliberately fail-closed: a missing image, failed configure/build,
or failing jtreg is a **HOLD**, never treated as a success. The job is
committed and queued, **NOT YET EXECUTED/VERIFIED**. Native state-mask
Java/C11 parity remains an independently required job. Full JCK, JVM
compiler tier matrix, JVMTI, CDS/shared lexicon, ZGC/Shenandoah,
cross-platform architecture and measured allocation/CPU performance
are still outside the bounded PR proof.
