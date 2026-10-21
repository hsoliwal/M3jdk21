<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Collection recovery qualification

The canonical [recipe and proof packet](https://github.com/hsoliwal/com.synexia/tree/10f781ae3e525b7630ed0661b3bb99a41f092fb4/synexia-openrewrite-recipes/recipe-crates/m3jdk21-collection-recovery-20261020) owns the transformation, fixtures, catalogue, allocation probe and command evidence. The receiver keeps exact generated source seals and scoped results. Three collection main probes live in the normal src/test/java tree and the SortedSet regression remains a jtreg main test.

Five real Maven/OpenRewrite tests pass. Java/JNI projection and current filtered-wrapper regressions pass; the source-built SortedSet baseline loses split comparator metadata, while the candidate passes mixed, interpreter and C1 modes. Both production public APIs match. Windows C17/JNI tests run under -Xcheck:jni. The fixed allocation workload halves per-operation allocation; this is not a whole-workload performance claim.

Full source-built M3JDK21 image, complete jtreg, whole-module/Linux acceptance and phase advancement remain open. Existing historical source-seal workflows still require qualified successor handling; their failures are not waived by these scoped results. See QUALIFICATION.json and the canonical COMMANDS.json for exact scope.
