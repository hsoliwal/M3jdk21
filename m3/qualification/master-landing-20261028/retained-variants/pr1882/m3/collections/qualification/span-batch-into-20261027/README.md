<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 original JNI span batch receiver, 2026-10-27

This is **M3JDK21** original product source, not an M3-on-JDK wrapper.
Canonical transformation, donor-selection catalogue and recipes stay solely
in [Synexia PR #13224](https://github.com/hsoliwal/com.synexia/pull/13224),
source-sealed commit `4538f6a507397545ee09a47a0c15cc6e66d46883`.
The product receives exactly the original M3SpanIndex.java/C JNI source
postimages and two generated Java21 contract/JUnit tests. The original
`batchNative` JNI export is preserved; an additive
`batchIntoNative` export implements caller-owned primitive output windows.

## Exact original product source custody
- Prior Java owner Git blob: `acb0a8aac6e04fa0eab5d52e434d11d82b9a696a`; post: `f8229851a1a36b72f807736b6df33ef5e8f4ea01`
- Prior JNI C source blob: `3166f819c15a88c70a13f4bc90f61f393f36e224`; post: `05d13d490d6bc039a3549420fc92b5d12fd38520`
- New Java21 standalone contract: `3797d53282f8127b11f824845773607728abcfa3`; new JUnit runner: `693cfced51b361861eedea201b022da52b07fd7a`
- Validation script exact Synexia blob: `7da9665a1ddfa042aac93ecff86d85d5a5881fb9`
- Focused GitHub Actions workflow blob: `ebf7f6391366c30c7315245f1de6b866bb3d2fb9`

## Semantics
`M3SpanIndex.Snapshot.earliestCoveringInto(int[] rows,int queryFrom,int length,int[] output,int outputFrom,Backend)`
retains the existing earliest-starting interval semantics, signed row boundary
behavior, masks and immutable snapshot identity. All Java windows/nulls are
prevalidated; aliased row/output arrays are refused before writes. JAVA uses
the original scalar prefix-max binary search; NATIVE_REQUIRED fails closed on
missing JNI. Native code uses fixed 256-query pages and bounded critical
sections with no Java callbacks/persistent native pointers. Legacy batch ABI
remains available; it internally reuses the new C17 query pager.

## Evidence and admission
Isolated **exact original source** Java 21 strict compilation, C17 Werror
and undefined-behavior sanitizer compile, both JNI symbols, and differential
Java/native/-Xint/-Xcheck:jni tests executed successfully:
native 161,307 assertions in both normal and interpreter modes; Java-only
fallback 160,947. No external LeetCode solution was copied. The category
catalogue compares LeetCode #35/#56/#1851; #1851 is rejected as a different
smallest-containing-interval objective.

Run the original source gate:
`bash m3/collections/qualification/span-batch-into-20261027/verify.sh .`.
The dedicated workflow rechecks the exact PR head and retains its receipts.
**HOLD** pending actual head CI, full Maven/JUnit/JNI reactor, source-built
JDK/jmods, jtreg/JCK, cross-platform/ARM and measured throughput. This
separately built module is not proof that the installed JDK java.util owners
have yet been natively replaced.
