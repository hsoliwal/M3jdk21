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
- New independent test Git blob: 39875249ce1b1adf5203bcf2074db45848bc5eeb.
- Both exact postimages are first held as canonical Synexia recipe templates;
  the product branch readback returned the exact same Git blobs.
- The product Java file retains its GPLv2+Classpath original header.
  Reusable Synexia recipe/fixture licensing is separately attributed.

## Actually executed Java 21 evidence

The exact original and candidate compactor sources each compiled in a
patched java.base Java 21 module with -source 21 -target 21 -Xlint:all -Werror.
Both separately executed M3CollectionMaskCompactorPrefixTest and passed
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
