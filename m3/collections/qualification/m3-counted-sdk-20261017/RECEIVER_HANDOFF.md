<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# M3 Counted SDK receiver handoff

Canonical Synexia recipe: https://github.com/hsoliwal/com.synexia/pull/12164
Exact source recipe commit: `891dbf8445120de94b2569d30446543803b9d1a4`
Target baseline: `hsoliwal/M3jdk21 master@e913ab89056657eca4009678cfe9a657c020e79b`

This target uses two ordered commits: (1) canonical recipe, README/category/rights receipts, unchanged source templates and workflow, (2) generated Java implementation and tests. All product postimages are the identical Git blobs as the Synexia template sources. Existing M3 maps, public JDK `java.util`, HotSpot and original JNI C are not edited. There is no Synexia runtime dependency.

Status: **DRAFT / HOLD**. Local Java 21 selected-source contract oracle passed 165,184 checks using clearly marked stand-ins for existing map/JNI owners. Actual provider dependency test and Maven are **NOT_RUN locally**. Head CI must compile actual real M3 map/JUnit/module and actual Guava/Commons library JARs, perform differential and checked original JNI. No JDK/jtreg installed-image or speedup claims. String → arrays → collections receiving order remains gated.
