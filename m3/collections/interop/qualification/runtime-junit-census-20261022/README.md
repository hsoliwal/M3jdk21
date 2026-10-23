<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# M3JDK21 five-library JUnit full-method acceptance

This product receiver verifies **44 existing Java test source files and exactly
158 original JUnit Jupiter `@Test` methods**, rather than the obsolete fixed
55-method check from the earlier five-library interop workflow. No source test
or assertion is discarded, reimplemented or skipped.

The four eleven-file source receipts under `receipts/` seal the Git object SHA
of each Java test owner and its ordered method identifiers. Zero-`@Test`
SelfTest/helper Java classes are also explicitly retained in the inventory.
The source verifier rejects changed original source, missing or unexpected
test classes, out-of-order/duplicate method IDs, missing SPDX ownership,
unadmitted parameterized/dynamic test forms, and any unreviewed JUnit method.

The runtime verifier parses **actual Maven Surefire `TEST-*.xml` output**,
requiring a unique executed testcase for every source method, no extra or
missing execution IDs, exactly matching counts, and zero failures, errors or
skips. Eight synthetic adversarial Python tests cover missing or changed
sources, forged method inventory, duplicate runs, unexpected/missing cases,
skipped/failing suites, and expected success.

## Source and ownership boundaries

- Product owner: `hsoliwal/M3jdk21` (the standalone M3JDK21 Java/JNI product,
  not a separate wrapper runtime). The 44 tests cover the existing JDK,
  Guava, Eclipse Collections, Apache Commons Collections and Spring interop.
- Algorithm/storage authority: existing dynamic arrays, `M3ArrayList`,
  `M3IndexMaskCompactor`, physical offsets, bit masks, stable filtered/sorted
  views and their original native/JNI ABI. This change edits **no** production
  Java, JNI, C or C++ and imports no algorithm-donor source.
- Recipe owner: exclusively `hsoliwal/com.synexia` using the original
  `M3HashPinnedTextSnapshotRecipe` and Maven/OpenRewrite recipe crate.
  Public M3JDK21 receives only generated verification source, test data,
  workflow changes and source/provenance receipts.
- Source preimage: product `master@b1cc8ab32eaef5b01582623b100da80946fd9346`.
  Every test source Git blob and the full exact workflow transition are
  pinned by the source-sealed Synexia template dataset.

## Qualification and next candidate promotion

Full source-census admission remains the historical **36 + 7 + 40 = 76**
owner verification (29 unchanged historical sources, seven pinned revisions,
40 added files). The original historical 36-file verifier, older five-atom
recipe, and single Java21 compound-`var` postimage remain untouched.

Run the unchanged product Maven `verify` before accepting any Surefire XML.
Require the source/Surefire method inventories and eight negative controls
to pass on the exact PR head. Existing public API/`javap`/serialization and
optional JNI mask-conformance gates remain separately authoritative.

The earlier exact 55 count was a source-incomplete 2026-10-15 snapshot.
An execution count of 158 is not a performance result or proof of full
Eclipse/SWT/JDT/EMF compatibility. If another behavioral test is added, first
refine the canonical Synexia recipe, regenerate the source ledger and execute
the entire contract; never weaken the verifier or add an arbitrary wildcard.
