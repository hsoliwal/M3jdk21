<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 UTF-16 identity comparison: exact Java/JNI result receiver

**Canonical recipe owner:** [hsoliwal/com.synexia PR #15080](https://github.com/hsoliwal/com.synexia/pull/15080), crate `synexia-openrewrite-recipes/crates/m3-utf16-identity-category-20261101`. This public product repository retains only materialized source/test postimages and source/acceptance receipts, not the mutation recipe.

Three original M3JDK21 file owners were source sealed against master `315cd18d0092f70c212223bb6af1f4366591ed1b` before publication. Check `SOURCE_PINS.tsv` for exact Git blobs and SHA-256 pre/postimages.

## What changes

`M3ArrayNative.compareUtf16` now recognizes `left == right && leftStart == rightStart` **after** both null/range checks and before calling optional JNI. The existing C JNI `nativeCompareUtf16` entry recognizes same-reference/same-window using `IsSameObject` after its own validation, before copying any GC-managed array region. This does not compare JNI opaque handle values, acquire heap pointers, change native symbols, change public descriptors, change allocation ownership or add object-per-entry structures. Differently offset overlapping views and independently allocated equal arrays remain on their existing scalar/native paths.

`M3ArrayNativeTest` adds the same-window, valid-empty-offset, offset-inequality, invalid-large-range and optional real-JNI-reflection cases alongside pre-existing differential tests. All existing C and Java source notices stay unchanged.

## Executed evidence / release gates

An isolated Java 21 plus C11/JNI probe using the same identity test shape compiled with `-Xlint:all -Werror` / `-Wall -Wextra -Werror` and passed **20,021** windows. This is not the product test suite. Full source/JNI module verification is owned by the **existing** `.github/workflows/m3-array-substrate.yml` (no new workflow): Java lane `mvn -B -ntp -f m3/arrays/pom.xml test` and JNI lane `mvn -B -ntp -f m3/arrays/pom.xml -Pnative-linux -DargLine=-Xcheck:jni test`.

This receiver stays in a **draft PR / HOLD** until actual Maven, source digest, Java/JNI, memory/GC, JDK integration and benchmark gates return passing evidence. Do not infer a speedup or phase advancement from the simple isolated proof. Respect the String -> arrays -> collections priority and existing JDK21 superset/API rules. LeetCode/HackerRank/GeeksforGeeks are algorithm problem-shape labels only; no challenge code was imported.

## Provenance

Source shape is first-party maintenance of existing Apache-2.0 M3 Java/JNI code. Original JDK/donor rights, upstream receipts, package names and binary contracts remain unchanged. No rebase, squash, or force push; no default branch write. Related category-first inventory, held work queue, tests and reusable OpenRewrite recipe are canonical in the private convergence workspace.

## Real GitHub Actions readback (2026-11-01)

Run: [M3 Array Substrate #41282563855](https://github.com/hsoliwal/M3jdk21/actions/runs/41282563855) at source head `6a49cfa96164ac692178144e18981f15d362b6f5`.

- `native`: **PASS**. CMake/CTest passed 4 native tests; Maven completed 58 JUnit tests (0 failures/errors, 9 skipped) with requested `-Xcheck:jni`. `M3ArrayNativeTest` reported 6 of 6 passing, including the new identical-window case.
- `java`: **PASS**. Maven reported 45 JUnit tests (0 failures/errors, 14 skipped), including all 6 `M3ArrayNativeTest` methods. Auxiliary SDK tests passed in the workflow.
- `receiving`: **FAIL**, due to `M3JDK21_NAME_RECEIVER_REFUSED: companion relation source hash drift: src/java.base/share/classes/java/util/Arrays.java (sha256)`. This PR did **not** modify that Java base file; treat this as a separate source-pin reconciliation task, not permission to update its hash or suppress its gate.

**Overall workflow remains FAILED.** No merge/release claims, and skipped tests do not count as passing. The local isolated proof remains a separate narrower source of evidence; A/A-calibrated performance and whole-JDK acceptance are still pending.
