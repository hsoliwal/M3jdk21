<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# Current master receiving and evidence

Canonical source and evidence are published in [PR #12528](https://github.com/hsoliwal/com.synexia/pull/12528) at commit
`74d5a8e17e130c65dc3b57fae8d23c7e243a2fdd`. It is an open draft PR; publication does not mean merge.
This receiving metadata is a recreated successor, using the published mappings,
source admissions, manifests and receipts. The seven former metadata templates
were not in the frozen publication map; exact recovery of those bytes is not claimed.

Receiving source remains M3JDK21 `9a164c31b38c7013c179a3ff38b0bb60c199c7fd`. The canonical collection recipe is
`com.synexia.rewrite.M3CurrentMasterConvergence20261019`, manifest
`ab3479808088106c5fd6d2be37381390158f5d36590bffe7eb09e04a7fd71ff6`: 60 targets, 39 KEEP and 21 changes including 12 additions.

## Selected owners

| Owner | Current operation |
| --- | --- |
| M3LongIndexProjection | Receive equal-key-run reversal into the existing immutable projection: `6be07c18dbbbe9ae259ff85821bb3f4ec666fddd820b0950f4115c4256f45deb` to `a29b0bc589565924611538c6fabe15a70d3629bc4eb3db0977d19f411ef4eb4e`. |
| M3WindowPlan | Already received; KEEP `bd192d5a81b3b331f8df75e09a4c3f0e5c462b5e39c3a72a7a020acb6e2e6b38`. Its historical overflow repair is not applied again. |
| M3RowTree | Explicit current inverse `d8398841841dbdff9bdc3f14e02ea6783286d4a77c29b5bcfbbfb1649086ebfe` to `3e9ff2db9ac2918dac993bb44493269ea4c4309d156724583de549a666ae19bb` in the current manifest. The historical KEEP alone does not authorize this inverse; retain the original positive and adverse resource controls. |

Current physical-state APIs and native parity oracles remain present. No test
exclusion or oracle weakening was used to admit the current native result.

## Current collection execution

| Receipt | Raw SHA-256 | Bounded result |
| --- | --- | --- |
| [Actual SDK](https://github.com/hsoliwal/com.synexia/blob/74d5a8e17e130c65dc3b57fae8d23c7e243a2fdd/synexia-openrewrite-recipes/recipes/m3-collection-refinement-20261019/viewport-convergence/evidence/current-master-sdk/receipt.json) | `2fa15d17940e8e0b8e6afa2b63bb344523704c3b251b8e02d835c86aed8d03b3` | OpenRewrite 8.17.1 PlainText ScanningRecipe, 3 tests, zero failures/errors/skips. JavaLST and whole reactor NOT_RUN. |
| [Complete module](https://github.com/hsoliwal/com.synexia/blob/74d5a8e17e130c65dc3b57fae8d23c7e243a2fdd/synexia-openrewrite-recipes/recipes/m3-collection-refinement-20261019/viewport-convergence/evidence/current-master-reconciliation/verification/receipt.json) | `99813f6c392f0586835b96a3b7ff4f266f7cce16dc999d4d6f82495aa3c24b11` | 657 files; 268 main and 264 test Java sources; Maven 477 tests, zero failures/errors and two native-only assumptions; console 475 successful / two aborted. |
| [Execution binding](https://github.com/hsoliwal/com.synexia/blob/74d5a8e17e130c65dc3b57fae8d23c7e243a2fdd/synexia-openrewrite-recipes/recipes/m3-collection-refinement-20261019/viewport-convergence/evidence/current-master-reconciliation/VERIFIER_EXECUTION.json) | `394ca5a405c1ded6721e8c7bc295a41f244e140767bb35bb73a7f0ab20306007` | Binds the complete module to current9a and manifest5bb24. |
| [All native](https://github.com/hsoliwal/com.synexia/blob/74d5a8e17e130c65dc3b57fae8d23c7e243a2fdd/synexia-openrewrite-recipes/recipes/m3-collection-refinement-20261019/viewport-convergence/evidence/current-native-convergence-attempt01/execution/RECEIPT.json) | `1273fdae7e8d571a4a517632bc59b5fb901900cdfb04f02d665c899dd926dae8` | 477 successful; zero failures/errors/skips/aborts. Eleven C units, ten libraries and 21 generated-header JNI exports, strict C17/fatal UBSan and Java21 checked JNI. |

The native run sets the required mask/all-native/rank-native/signal-native flags.
Individual tests may explicitly select Java. The M3MaskViewportNative host and
the instrumented `src/test/native/m3_gather_spans.c` harness are outside that
ten-library gate. Source, classes and generated headers stayed unchanged.
Qualification duration is not a performance benchmark.

## Current Swing source admission with earlier execution reuse

The existing binding manifest remains
`8c1d7492734d8151271ce11fdd23b3b77bdc68308b95c560c2b0792fd421a46b`;
the existing Swing UI manifest remains
`8700ab0a685f1d8ac2890874d13a440461d1a496e92907759cc718a6dc7b6d16`.
Their 19 entries retain seven byte-identical current preimages and twelve
exact-ref absent additions. The 73 tracked Swing module paths, modes and blobs
are identical across epochs, tree `d1e0a2113d425d30037f27af66cf11c36e5db200`.
The earlier executed binding-only candidate has 75 files: the baseline plus its
two proof additions. The combined receiving module has 80 tracked source files
after the five further UI `jdk-reproducers` additions. No fresh 80-file Maven
execution is claimed. Four unchanged UI fixture dependencies are in the
baseline module; separate `java.desktop` qualification remains separately recorded.

- [Exact source admission](https://github.com/hsoliwal/com.synexia/blob/74d5a8e17e130c65dc3b57fae8d23c7e243a2fdd/synexia-openrewrite-recipes/recipes/m3-collection-refinement-20261019/viewport-convergence/evidence/delivery-metadata/current-swing-9a164c31/ADMISSION.json), SHA-256 `af080f1d211aa7082f4efa6b3f7b138733e2f0bfae1064b0195d39199f025a93`.
- [Complete module identity](https://github.com/hsoliwal/com.synexia/blob/74d5a8e17e130c65dc3b57fae8d23c7e243a2fdd/synexia-openrewrite-recipes/recipes/m3-collection-refinement-20261019/viewport-convergence/evidence/delivery-metadata/current-swing-9a164c31/MODULE_ADMISSION.json), SHA-256 `e996fa1ab799a1017b4175d14f13d1ab6e8e4b41eaf3b05a836b017d221a4208`.

Earlier execution remains bound to `c2a8733f6eebaadcc206a61d08906bfd8b6d60dc`: binding 603 real EDT
checks, 20 unchanged desktop controls and 18 original-POM JUnit tests; Swing UI
28 JUnit tests, exact public/differential observations and preserved failing
witnesses. The unchanged Swing recipes retain the historical nine-test SDK
receipt. Source identity permits reuse of those bytes; no runtime or SDK rerun
is claimed by this metadata recovery.

## Naming, rights and receiving boundary

The guarded publication appends ten mappings to the existing 66 in
`m3/docs/name-mapping.json`. The prior raw SHA-256 is
`ac0b133afdfedcf68727332c91e82390a951f3a03350a6cffa9de0e6edcb77f0` and Git blob
`ce76431af299d15823c1cd30f9083ab1a9186f03`. Every original byte is preserved
around the single insertion; all 66 original mapping objects and every other
index value remain equal. `RECEIVING_PUBLICATION.json` records the exact seals.

The recreated `SOURCE_RIGHTS.tsv` maps 28 selected owners/proofs to the [published
1,128-destination rights crosswalk](https://github.com/hsoliwal/com.synexia/blob/74d5a8e17e130c65dc3b57fae8d23c7e243a2fdd/synexia-openrewrite-recipes/recipes/m3-collection-refinement-20261019/viewport-convergence/publication-metadata/current-master/SOURCE_RIGHTS.tsv) and two unchanged
inherited recipe owners to their exact published Apache-2.0 SPDX headers. Preserve
Apache-2.0 first-party and per-file GPLv2/Classpath Exception rights and notices;
recipe licensing does not relicense generated payloads.

Canonical source/evidence pins remain fixed above. Collections are received in
[PR #1522](https://github.com/hsoliwal/M3jdk21/pull/1522); Swing and this additive receiving metadata are
received in [PR #1525](https://github.com/hsoliwal/M3jdk21/pull/1525). Both are open drafts.
STRING stays QUALIFICATION_REQUIRED and later phases stay
WAITING_FOR_PREDECESSOR. Installed M3JDK21 image, jtreg/JCK, native desktop UI,
SWT/Skija host delivery, Android/iOS runtime/host/device and broad resource
acceptance remain separate gates. No recovered file or metadata state promotes them.
