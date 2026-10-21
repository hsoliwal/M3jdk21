<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 five-provider source successor (36 → 76)

This is the existing integrated SDK's exact installed-source census, not another M3+JDK layer. The original five-file historical OpenRewrite receipt remains unchanged. The preceding 36-file census is preserved verbatim as `predecessor-source-census-20261017.tsv`, SHA-256 `5ebaf6ef12f2f954029f588e38e36c844cf97b550626a4f16e35c9219d725620`. The current 76-file receipt is SHA-256 `eb77198310b1b2fe66fb800a5a38870bf4bd913ba9a6e1c0b4a70f90a1c412f6` and pins both SHA-256 and Git blob identities.

At product commit `7f657d6ca05760af7bee7d4d2d9e14ae91ddd942`, 40 Java source/tests were added after the last pinned census and seven previously admitted files changed. All 36 predecessor identities remain retained. The verifier requires the exact seven replacement paths, checks their previous output SHA as the successor preimage, and refuses missing/additional tracked Java, symlinks, unapproved file changes and drift. The original five-file historical contract is checked against the predecessor rather than silently rewritten. Separate `source-provenance-20261021.tsv` identifies exact source changes and commit roots.

The seven changed files implement and/or test shared M3 counted ownership, extended five-provider index/array/mask paths, Guava random-access list contracts and Spring casefold binding expansion; their latest source commits were inspected. These source receipts do **not** certify the five provider's full Java/JNI runtime compatibility or performance.

Existing GitHub Actions run `39608772037` failed on the stale tracked-file census before compilation. The successor contains no production Java or C changes; it is a fail-closed gate repair. Independent Python successor-lineage oracle passed eight hostile cases, but target exact-head Maven/OpenRewrite, Python verifier, JUnit, JNI and installed SDK gates remain **PENDING**.

## Java 21 syntax repair — verified CI failure

Exact run [39623768546](https://github.com/hsoliwal/M3jdk21/actions/runs/39623768546) passed source admissions, six recipe proofs, original five-file source replay and original M3 core compile. Maven then rejected `M3FiveLibraryArrayificationTest.java:56`: Java 21 does not permit compound `var` declarations. The corrected original test keeps left then right evaluation order with two legal `var` declarations. Production Java, public APIs and JNI symbols are unchanged.

Original test SHA-256 `43d670c2278b4db3cb39e8c6d63eff56827c7ef8359e7c85aa8f72b892bba51d` (Git blob `42d78b4fae7525467f0a5d4f34baf45b511e3b55`), corrected SHA-256 `6d31a53eb94af7c3cfe08ce126ce1be1f8d7da1b09150bd72a12c602d3ab3565` (Git blob `e220622405432368d2c46b32f9f1c197913e7c0a`). The old 76-row census SHA-256 `aa4c050c26a33e6a0d539e8c8bb9d24139681c746a8633ded8eb5f8502e435fe` is retained in the canonical recipe history; corrected 76-row census SHA-256 `eb77198310b1b2fe66fb800a5a38870bf4bd913ba9a6e1c0b4a70f90a1c412f6`. Its 36-row predecessor and original five-file recipe remain unmodified. A tiny isolated `javac --release 21 -Xlint:all -Werror` test passed. Exact target Maven/OpenRewrite JUnit and native tests remain pending.
