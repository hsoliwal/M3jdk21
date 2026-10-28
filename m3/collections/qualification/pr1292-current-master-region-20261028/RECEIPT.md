<!-- SPDX-License-Identifier: Apache-2.0 -->
# PR #1292 current-M3JDK21 native mask-window qualification

**Target source HEAD:** `74893b6381b597cc241d9fa10f00e0b44e6c2487` (after exact AST core receiver merge #2071
and token regex receiver merge #2072). **Original chronological PR:**
[#1292](https://github.com/hsoliwal/M3jdk21/pull/1292.patch), whose older
historical JNI implementation is `a34a203dceb7425f25aa4cede7898f22829966e8`.

**Current master JNI source:** `m3_index_mask_window.c` Git blob
`bd300c90d54ede9b7f238efe8a18c918d2fcef9e`. It retains both original
JNI exports, but the state-pass gathers only the minimal enclosing admitted
row region instead of the historical suffix. Its behavior must not be
overwritten by #1292's old source.

This PR adds one *new* instrumented C17 test to the existing
`m3/collections/src/test/native` owner. Its exact Git blob is
`6711b9edeeba1ed1a4de3cfdc118ac939b92bd5d`.
No production Java or JNI source is modified.

## Actually executed gates (stock Linux Java 21 / gcc 14.2)

- `cc -std=c17 -Wall -Wextra -Werror -O2` against exact current JNI source and
  new instrumented JNI-table test: **PASS**.
- Executed: `M3 JNI CURRENT REGION PASS checks=390`. Cases include rejected
  inputs, sparse row transfers, strict minimal admitted span, early output
  limits, resumable bounded cursor, two-chunk traversal, exact output guards,
  aliases, byte-mask range and invalid output capacity.
- `-fsanitize=undefined -fno-sanitize-recover=all` build and actual execution:
  **PASS**, same 390 assertions.
- The unchanged, already-installed historical native fixture
  `m3_mask_window_regions.c` (Git blob `52d4e5b20fade6ab7aa3f63d5d92a6b210e437bd`)
  also compiled and passed its existing four named cases against the exact
  current native source.
- No recipe Java, donor source, runtime library or JNI symbol is introduced.

The scoped GitHub workflow executes the same strict/sanitized C17 contract on
the exact expected source/fixture Git blobs. The hosting job's *own* result is
distinct from the above actually run local assertions.

**HOLD / incompleteness:** this is an instrumented fake JNI function table,
not an embedded/modified JVM `-Xcheck:jni` integration, and does not prove
full JDK/JNI/GC/ABI across platforms. The original #1292 Java projection and
historical native region test differ from current master; those source
successions must be reviewed under canonical [Synexia #12289](
https://github.com/hsoliwal/com.synexia/pull/12289) before closing the
historical PR. The test-only safe slice may merge additively without
pretending to settle its other conflicts.
