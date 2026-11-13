<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Cross-JDK optimization inventory and M3JDK21 receiver — 2026-10-17

**Scope:** major Java release lines through GA JDK 27 and representative HotSpot, OpenJ9,
GraalVM Native Image and vendor-specific JDK implementations. This is a **candidate inventory,
not a complete comparison of every commit, architecture, configuration, vendor distribution or
patch release**, nor a performance ranking of all JVMs. JDK 28 is early access, not a GA source
baseline as of this inventory. For every candidate, compare exact upstream commits with
our pinned OpenJDK21 base and current M3 fork files before making a code change.

## Baseline and evidence

- Product: [hsoliwal/M3jdk21](https://github.com/hsoliwal/M3jdk21), Java 21, upstream
  commit \`890adb6410dab4606a4f26a942aed02fb2f55387\`, current M3 master
  checked at \`970a34350ebdf6a3f1c5ff5acc8445d0f3515f55\`.
- Canonical transformation source: [Synexia PR #11966](https://github.com/hsoliwal/com.synexia/pull/11966),
  branch \`m3/jdk-codepoint-rank-cursor-20261017\`, exact snapshot Maven/OpenRewrite
  recipe \`com.synexia.m3.Jdk21CodePointRankCursor20261017\`.
- Receiving product branch: \`m3/jdk-codepoint-rank-cursor-20261017\`.
  \`M3StringCodePointPrecompute\` retains its original internal JDK owner, bounded
  weak-owner cache, Java public methods and GPLv2+Classpath source license.
- Source-only proof \`python3 m3/runtime-integration/verify-codepoint-rank-cursor-receiver.py\`.
  Source-extracted Java reference differential:
  \`python3 m3/runtime-integration/codepoint-rank/verify_source_oracle.py\`.
  This second test compiles the **exact algorithm body extracted from the fork source** against
  Java 21, not a separate hand-copied kernel. It is still **not** a compiled modified-JDK test.
- Native/source-built acceptance requires full M3JDK21 image, matching
  libjava/HotSpot, jtreg normal/-Xint/C1/C2, GC/CDS/JNI cases, regression
  suites and JMH cold/warm CPU + heap/RSS measurements before promotion.

## Comparing JVM implementations, not just build brands

| Family | Distinctive candidates | Portability to M3JDK21 | Decision |
| --- | --- | --- | --- |
| OpenJDK HotSpot 8/11/17 | Historical APIs, escape analysis and longstanding compatibility suites | Low-value bulk replacement; choose known fixes with bug, ancestry and tests | Baselines/contract reference |
| OpenJDK 21 (M3 base) | Java 21 ABI, javac, java.base, HotSpot, JNI, jtreg, G1/ZGC | **Direct base**; preserve behavior before optimization | Keep as compatibility authority |
| OpenJDK 22–24 | Intermediate VM/library and Project Leyden/AOT developments | Source ancestry must be reviewed chronologically | Per-commit review only |
| OpenJDK 25 | Compact Object Headers (JEP 519), AOT method profiling (JEP 515) | Cross-cutting object header, locking, barriers, tooling and CDS changes | Separate risky HotSpot backport |
| OpenJDK 26 | G1 synchronization reductions (JEP 522), GC-agnostic AOT object cache (JEP 516) | Collector/compiler/image changes; cannot paste into 21 | Separate GC/AOT qualification |
| OpenJDK 27 | HashMap bulk copy, Swing AttributedString, URL, file-open, crypto, C2 vectorization, compact headers default (JEP 534), G1 default (JEP 523) | Bounded Java-library changes could be individually backported; runtime features require complete matched tree | Ranked below |
| Eclipse OpenJ9 / IBM Semeru | Shared Classes Cache, AOT code reuse, JIT heuristics | Distinct VM, GC and object layout; source is not a HotSpot drop-in | Compare architecture and results |
| GraalVM (JVM and Native Image) | Graal compiler; Native Image closed-world static build, build-time init, optional PGO (not in Community Edition) | Native Image is not a transparent HotSpot java.base replacement | Workload comparison/reference |
| Azul Zulu / Prime | Zulu is HotSpot-derived; Prime has ReadyNow profiles, Falcon/Optimizer Hub | ReadyNow/Prime capabilities are product-specific; do not copy closed implementation | Design/reference and benchmark |
| Oracle JDK, Eclipse Temurin, Amazon Corretto, Microsoft Build, SAPMachine, Red Hat, BellSoft Liberica, Tencent Kona and other HotSpot distributions | Upstream HotSpot with vendor-specific patch sets, images, support and tuning | Compare exact source patches; distribution name alone does not imply new algorithms | Per-patch audit; distinct benchmark baselines |

**Sources:** [OpenJDK JDK 27 performance review](https://inside.java/2026/09/28/performance-update-jdk27/),
[OpenJDK JDK 27 GA notes](https://www.oracle.com/java/technologies/javase/27all-relnotes.html),
[JDK 25 changes](https://docs.oracle.com/en/java/javase/25/migrate/significant-changes-jdk-25.html),
[JDK 26 changes](https://docs.oracle.com/en/java/javase/26/migrate/significant-changes-jdk-26-release.html),
[OpenJ9 Shared Classes Cache](https://github.com/eclipse-openj9/openj9-docs/blob/master/docs/xshareclasses.md),
[GraalVM Native Image PGO availability](https://www.graalvm.org/latest/reference-manual/native-image/guides/optimize-native-executable-with-pgo/),
[Azul Prime ReadyNow/Optimizer Hub](https://docs.azul.com/optimizer-hub/).
Read exact release/PR source before adapting any code.

## Ordered candidate intake and disposition

1. **P0 implemented candidate: M3 String code-point rank/select.** Existing source
   \`M3StringCodePointPrecompute.Geometry.offsetByCodePoints\` repeatedly calls a
   \`codePointCount\` binary search from a second binary search, O(log N log P) worst
   case with N source UTF-16 length, P stored paired-low offsets. Replace the
   offset search with one order-statistics search over monotone
   \`pairedLowOffsets[j] - j\`, O(log P). Preserve exact mid-surrogate
   traversal rules and all exception paths. For building geometry, replace
   repeated \`source.charAt\` tuple descent with existing M3 \`CodeUnitCursor\`;
   no new owner, payload or public API. Requires actual M3 image and speed tests
   before a performance-win claim.
2. **P1 candidate: JDK-8371656 HashMap.putAll / constructor bulk fast path.**
   Reuse stored hashes and source-table traversal for exact known source map
   types; ensure Map self-copy, source-order, fail-fast details, identity and
   subclass behavior. Source: [OpenJDK #28243](https://github.com/openjdk/jdk/pull/28243).
3. **P1 candidate: JDK-8380794 AttributedString run attribute representation.**
   Potential Swing text measuring/rendering win; map-backed attributes, exact
   iterator/access semantics, mutation tests. Source:
   [OpenJDK #30402](https://github.com/openjdk/jdk/pull/30402).
4. **P1 candidate: JDK-8379557 URL formatting.** Avoid redundant interim
   String allocation; preserve every URLStreamHandler override and unusual
   authority/query/fragment combination. Source:
   [OpenJDK #30151](https://github.com/openjdk/jdk/pull/30151).
5. **P1 candidate: JDK-8373647 redundant Unix fstat in FileOutputStream /
   RandomAccessFile.** Check portability and errno/EISDIR expectations for
   Linux, BSD, macOS and Windows separately. Source:
   [OpenJDK #28823](https://github.com/openjdk/jdk/pull/28823).
6. **P2 candidate: JDK-8378698 Base64 encoding copy removal.**
   **Must** reconcile with M3 String's canonical owner/compatibility shadow:
   an internal trusted String constructor may assume an ordinary byte[] layout.
   Do not transplant without source/HotSpot/JNI parity. Source:
   [OpenJDK #29920](https://github.com/openjdk/jdk/pull/29920).
7. **P2/P3 candidates: C2 SuperWord subword casts (JDK-8342095),
   known-bits simplifications (JDK-8367341), compact headers (JEP 519/534),
   G1 synchronization (JEP 522), AOT profiles (JEP 515).**
   Full HotSpot cross-module dependencies, architecture support, correctness,
   JVMTI/JNI and GC constraints dominate implementation cost; isolate
   each backport before merging.
8. **Reference-only: OpenJ9, GraalVM Native Image, Azul Prime.**
   Borrow the **idea** (share immutable class data, train/cache warmup profiles,
   compaction strategies), not incompatible VM source or proprietary services.
   For new runtime design, profile against actual M3 workload before porting.

## Acceptance and comparison discipline

Categorize candidate work as CONTRACT, ALGORITHM, LIBRARY, HOTSPOT, GC,
JIT, JNI, IO, SECURITY or REFERENCE. Prioritize **correctness and backward
compatibility first**, then measured end-to-end latency, CPU time, allocations,
retained heap/native RSS, GC pauses, warmup/startup, cross-platform behavior,
and maintenance complexity. Different hardware and allocation profiles
produce different winners; “best JDK” is not a universal scalar.

Per file, follow **Inline (only when safe) → Atomize → Patternize → Comment**,
with Synexia canonical Maven/OpenRewrite input hashes, test and fixed-point
replay. Keep both contracts and donor/copy-rights ledger. Promote a target
only when \`javac\`, jtreg, runtime/JNI and performance evidence on a matched
image is attributable to the exact installed Git commit.

**Status: draft, narrow implemented source change + source-test candidates.
No full JDK 27 feature backport, no complete cross-JDK performance shootout,
no full jtreg pass and no performance superiority claim.**
