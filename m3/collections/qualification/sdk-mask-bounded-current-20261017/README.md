<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Synexia canonical current-base bounded-work JNI recipe

This successor to [Synexia #12139](https://github.com/hsoliwal/com.synexia/pull/12139)
is a **new current-source-qualified Maven task leaf**, not a second OpenRewrite
scheduler. It preserves all later M3JDK21 #1131 native fast-path optimizations.
Source authority stays here; runtime owner remains M3JDK21.

Target pinned: `hsoliwal/M3jdk21@e913ab89056657eca4009678cfe9a657c020e79b`.
See `SOURCE_RECEIPT.tsv` for both exact source and generated SHA-256s,
and `FILE_PASSES.tsv` for serial Inline → Atomize → Patternize → Comment gates.

- Existing `M3IndexMaskWindow.scan` and `scanNative` public ABI preserved.
- Add `scanBounded(...maxExamined,...limit,...backend)` and
  `scanBoundedNative`, keeping source and result limits independent.
- The Java default path keeps the existing constant-mask early exits,
  `System.arraycopy`, and shared typed primitive scan.
- JNI keeps the original fixed 128-cell Region arrays and first-match scan
  ahead of row reads. The original symbol now calls that same bounded
  internal kernel with the complete source range.
- No new object-per-entry state, native heap, copied source window,
  JNI Java-object reference, predicate/comparator callback, or runtime
  Synexia dependency.

`mvn -B -ntp -f synexia-openrewrite-recipes/recipes/sdk-mask-bounded-current-20261017/pom.xml verify` must pass and print
`M3_MASK_BOUNDED_RECIPE_PASS`. Then target must run strict real-source Java21,
5000-case first-party `M3BudgetedMaskWindowContract`, checked JNI with
C17/UBSan and the old shared-library refusal path. No corpus is bundled.
LeetCode/HackerRank/GFG are only algorithm category tags; provider SDK
interfaces are different contracts.

**HOLD:** these postimages are not proof of green target CI, full JDK image,
jtreg/JCK, Linux/non-Linux native parity, matched CPU/heap microbenchmarks,
or phase promotion before String→arrays gates.
