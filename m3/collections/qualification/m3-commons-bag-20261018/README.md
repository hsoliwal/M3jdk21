<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# Canonical M3 Commons Bag implementation recipe — 18 October 2026

This is the **Synexia convergence authority**; the public SDK runtime belongs exclusively to `hsoliwal/M3jdk21` and does not require Synexia artifacts at runtime. It reuses *unaltered* original Synexia `M3GpuToolJoinRecipe` and `M3GpuToolJoinManifest` (OpenRewrite ScanningRecipe), with a new named resource root and SHA-256 sealed ABSENT-before Java/test templates. There is no manual edit to generated production files or a parallel rewrite framework.

## Actual Java provider API

`com.m3.collections.interop.M3ApacheBag<E>` implements the **real** Apache Commons Collections 4.5.0 `org.apache.commons.collections4.Bag<E>`, using the existing target `M3CountedBag<E>` (GC-traced key references in M3 index arrays, primitive occurrence counts). It is not a Guava Multiset, Eclipse MutableBag, or JDK Collection-compatible mutation policy; the Apache Bag contract intentionally violates certain JDK Collection semantics. In particular: `add(E)` reports new distinctness; `remove(Object)` removes all occurrences; `containsAll/removeAll/retainAll` are cardinality-aware; `uniqueSet` is a live read-only view. Apache upstream handles neither source nor native payload; optional M3/JNI state selection is delegated to the existing `M3IndexMaskCompactor`.

Existing JDK, Guava, Eclipse Collections and Spring M3 owners remain unchanged. An actual five-library provider JUnit test exercises them together, intentionally preserving their distinct public APIs and count/size semantics.

## Proof and scope

- First-party Java source and oracle templates have SHA256 seals `5ff51272c4fd48ef11a09b9138ac2329392bb2a70e2a4fe43fab2889909d224f` and `737dba455e469117b94cd82a48458bcbe2e4c1fd038b0beb578dcc00ccc1982a`.
- The unchanged class-backed `M3GpuToolJoinRecipe` enforces exact path/source preimages and refuses occupied/changed targets. The new Maven/JUnit test tests named discovery, source postimages, fixed point and occupied/drift refusal.
- **Local executed**: Java 21 `javac --release 21 -Xlint:all,-unchecked -Werror` using explicitly isolated API and count-owner *test stubs* (not provider integration); standalone randomized M3ApacheBag test **PASS 56,583 checks** against independent JDK count model. These figures do not claim complete target compilation.
- **Pending**: actual OpenRewrite/Maven JUnit recipe execution, M3 receiver owning-core/interop compile with Apache 4.5.0 and all real library jars, 6,000-step HashBag differential, C17 checked JNI, full JDK/JTreg/JCK, benchmark evidence.
- Preserve `develop` append-only; do not merge without independent CI and cross-provider acceptance. Existing mutable live JDK/Guava/Eclipse/Spring APIs must not be replaced with immutable snapshots.

Upstream Apache Bag/HashBag are **references** only under Apache-2.0, not copied implementation source; the JDK, Guava, Eclipse, Spring, and Apache API names remain upstream identities. LeetCode/HackerRank/GFG are reference-only problem categories. All new original code is first-party Apache-2.0.
