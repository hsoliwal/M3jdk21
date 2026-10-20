<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 original file impact: bounded receiving proof
Status: **PARTIAL / DRAFT / REVIEW HOLD**. This is a product-target module source candidate and not installed full M3JDK21/HotSpot ABI closure. This PR is stacked on [M3JDK21 #1568](https://github.com/hsoliwal/M3JDK21/pull/1568) and uses the canonical [Synexia #12582](https://github.com/hsoliwal/com.synexia/pull/12582).

## What changed
Two additive files only under the existing SDK-owned `com.m3.file` module: `M3FileBuildImpact` (derived reverse impact projection) and `M3FileBuildImpactContract` (independent oracle, original file snapshots). The algorithm accepts exact `M3FileDeltaLedger.Snapshot` physical slots and **caller-supplied authoritative compiler ABI flags and dependency edges**. It forms one `int[]` CSR edge-offset lane, one `int[]` edge destination lane, a bounded `int[]` queue, `byte[]` visited states and `long[]` impact masks. No per-row Object/Node/Cell/Entry, no parser, no duplicate M3File payload, no new JNI symbol, no changes to public Java SE or five provider APIs.

### Why this is M3JDK21 and not a wrapper
The changed class belongs to the current `com.m3.file` module of **hsoliwal/M3JDK21**, uses its existing `M3FileDeltaLedger.Snapshot` identity, and requires no `com.synexia` runtime. Synexia owns the Maven/OpenRewrite recipe that generates it. Maven, Gradle, javac and M3AST do not become replacement parsers or model stores.

### Correctness boundary
- `completeGraph=false` **always marks all nodes conservative**.
- A changed POM/DSL/unknown build input invalidates all declared rows until finer compiler/model impact proof is available.
- Raw source/class byte changes without compiler `ABI_PROVEN_SAME` propagate to reverse dependents.
- Only javac/M3AST may supply `ABI_PROVEN_SAME`, and only when the compiled ABI analysis includes constants, processors, sources and exact classpath/modulepath/toolchain generation. Unknown or contradictory masks are refused.
- A direct changed row remains affected even if its ABI is unchanged; its dependents may be candidates only with the full proof envelope.
- The Plan is **not** an authorization to skip any arbitrary Maven Mojo or Gradle task; plugin side effects, parent-model logic, repository updates, generated sources and task outputs remain framework-owned.
- Cycles and duplicate arcs terminate because a physical slot can enter the queue once. Selection is in stable physical index order.

## Validation actually executed (2026-10-20)
Local OpenJDK 21.0.12.1; `javac --release 21 -proc:none -Xlint:all,-module -Werror` with actual original `M3File`, `M3FileDeltaLedger` (from #1568), module descriptor, the new source and both tests. `java --module-path ... -m com.m3.file/com.m3.file.M3FileDeltaLedgerContract`: **M3_FILE_DELTA_PASS checks=30**. `java --module-path ... -m com.m3.file/com.m3.file.M3FileBuildImpactContract`: **M3_FILE_IMPACT_PASS checks=10567** (1,000 randomized graph closures against independent repeated-closure implementation). Java/JNI resource/speed measurements and whole build are **NOT EXECUTED**.

Local host had no Maven binary/Rewrite dependency cache; canonical Maven/OpenRewrite application, exact postimage fixed point, refusal and hosted workflows are therefore **PENDING**. Required before promotion: recipe generation checked against original pins, full M3File module Maven build, source-built installed M3JDK21, affected jtreg/JCK, JDK/GC/JNI module packaging, actual Maven 4 and Gradle fork cache/task tests, Java/native memory and throughput. Never upgrade pending to PASS by copying source or citing earlier PRs.

## Coexisting owners and source constraints
Preserve [scoped javac #1559](https://github.com/hsoliwal/M3JDK21/pull/1559), [Maven fork #5](https://github.com/hsoliwal/maven/pull/5), [full file port #1316](https://github.com/hsoliwal/M3JDK21/pull/1316), and filtered/sorted/JNI [#1554](https://github.com/hsoliwal/M3JDK21/pull/1554), [#1549](https://github.com/hsoliwal/M3JDK21/pull/1549), [#1551](https://github.com/hsoliwal/M3JDK21/pull/1551). The first two are not yet native ABI build planners; this narrow addition deliberately does not replace their underlying owners. Public JDK/Guava/Eclipse Collections/Apache Commons Collections/Spring Framework contracts stay distinct and unmodified.

All new implementation and proof source is Synexia first-party Apache-2.0, copyright 2026 Hitesh Soliwal and contributors; original OpenJDK/vendor rights preserved. Educational graph examples from LeetCode/HackerRank/GeeksforGeeks are classification references only; no challenge source copied.
