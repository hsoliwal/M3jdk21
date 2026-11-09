<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Three exact Java 21 collection-contract test corrections

The standalone M3JDK21 five-provider array/index/mask library successfully
compiled its production code. The strict Java 21 test compiler then reported
three independently attributable test-source problems:

- `M3ApacheCollectionsTest`: retain the Apache Commons Collections 4.5
  `Predicate` API but suppress its known deprecation **only on the field**.
- `M3FiveLibraryKeyRangesTest`: add the missing `java.util.Map` import.
- `M3FiveProviderFlagSelectionTest`: use the existing direct
  `M3SpringArrayMultiValueMap` Java owner rather than a nonexistent
  `M3FiveProviderDirectSdk` factory. Existing Spring list identity, alias,
  null and mutation contract assertions are preserved.

The canonical Synexia Java-LST recipe
`com.synexia.m3.M3InteropThreeJava21Diagnostics20261022` generates exactly
those three source atoms. All 13 original `@Test` methods and all assertions
are retained. No production Java/JNI source, array/index/mask ABI, or public
Java/Eclipse/vendor contract is changed.

**Two historical source classes are not interchangeable:**

1. `M3ApacheCollectionsTest` belongs to the original immutable 36-owner
   source census at Git commit `a720780b1a5a`. The original receipt and
   separate seven historical revisions remain untouched; its eighth approved
   source revision is recorded in `receipts/changed-historical-01.tsv`.
2. KeyRangesTest and FlagSelectionTest belong to the separately admitted
   40-file additive census. Their exact two source revisions are recorded in
   `receipts/changed-added-02.tsv`. The four original 10-file receipts and
   earlier single `var` syntax repair remain unchanged.

Each revision receipt binds exact full source path, original SHA-256/Git blob,
new SHA-256/Git blob, canonical Synexia recipe, preserved test-method count and
the `PENDING_JAVA21_VENDOR_CI` status. The successor verifier refuses
altered, symlinked, missing, unlisted or duplicate source revisions. The
historical 36 verification still executes **unchanged on the original pinned
checkout**. The final tracked source set remains exactly 76 files.

This stage verifies source identity and preserves original behavioral probes,
but it is **not** acceptance of the complete provider/JNI runtime. The
unchanged full Maven/JUnit, Guava, Apache, Eclipse Collections, Spring, Java21
module and any required JNI gates must pass against the exact target PR head.
