<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Complete collection source acquisition

The existing two-owner refinement does not complete all collections. A read-only, source-sealed workflow is being added to acquire the complete tracked collection sources/tests/native code/catalogues and the JDK java.util sources for the next canonical recipe pass.

Exact inputs: observed master 2779aba3f98f31c324be43f731be7914e1a05935 and reviewed candidate a3f2d208700da920f4cfc09aca4668a70b8eaac2. The workflow does not merge them, rewrite source, upload secrets, change public contracts, or promote the global receiving phase. Source capture and source counts are not optimization or qualification.

Canonical contract: com.synexia commit ed1d76aaeb3b3060a7080d4d8184a0bbd006aa31, synexia-openrewrite-recipes/recipes/keyed-api-20261015/collections-source-capture-20261021/CONTRACT.md. The class-backed binding uses the unchanged M3GpuToolJoinRecipe/M3GpuToolJoinManifest source-seal engine. Local strict Java21 compilation and three actual Maven/OpenRewrite scheduler tests passed for exact output, fixed point, foreign-target refusal and unrelated-input preservation. Workflow SHA-256: ab240121f12cfb120f13b405bddd4d49e1e315d54fe1f739f36f836e7f15706c.

The source artifacts must match their Git/SHA-256 ledgers before use. Retain the recipe and raw results in Synexia. Hosted capture has not yet executed at this documentation commit. Whole-module, original provider, JNI, installed-JDK and all-family transformation gates remain separate and pending. No default branch write or merge is performed here.
