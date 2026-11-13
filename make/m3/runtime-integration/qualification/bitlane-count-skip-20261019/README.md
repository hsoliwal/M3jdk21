<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 java.base bitmap: source-first receiving gate

M3JDK21 is the integrated runtime, not M3 plus an added JDK layer. The runtime owner is the existing `src/java.base/share/classes/jdk/internal/mindex/M3BitLane28.java`. This directory contains build-time proof/custody only: no new runtime module, collection hierarchy, service, interner, JNI ABI or second payload owner.

## Review state

The initial commit deliberately leaves **all production runtime files unchanged**. The candidate is an exact resource for the canonical Synexia recipe. Receiving requires actual Maven/OpenRewrite generation, refusal/fixed-point tests and generated Java/JNI verification. Only their exact output may enter the runtime in a later source commit; a tested template is not generator evidence.

Canonical owner: https://github.com/hsoliwal/com.synexia/pull/12454 at `c4a25a6f3d93c9cd25883cd075f8c0f5c115f48c`. The mirrored engines, fixtures, POM and proof script retain exact source Git blob identities. This directory maps to `synexia-openrewrite-recipes` in that revision. It is not a second recipe owner; improvements return to Synexia. No private-repository credentials or Synexia runtime dependency are required by this public proof.

Target base `55e19380cc26ebd74de37c0e5bd95a5cbbe07d12` retains the tested preimage blob `5942b1f46eedf83ad8c4ef8edee91f75e7424e7a` from baseline `a3e88976ee8664d1ef5d0cade96aec1206b2133d`. Candidate blob `731a7b63a8fc0c5f234e68f459b3b67080cc6101`, SHA256 `e87553ae33e4d05f07c3b57e081a88d076e871499fe98a0f69df8ddb9d0d2fb2`.

## Refinement and existing work

Forward/backward navigation scans allocated-but-cleared bitmap pages despite prepared exact populations. Reuse incumbent `regionCounts` / `pageCounts` to skip zero populations; stop an empty lane after original bounds checks. Cold navigation does not prepare metadata. Sparse array payloads, offsets, bit operations, allocation counters, writes, native cardinality and public descriptors remain unchanged. This is a scan-cost refinement, not a wrong-answer claim.

Standalone Fenwick navigation (M3JDK21 #1122 / Synexia #12104) is a compared donor, not copied storage. Its larger metadata/mutation migration remains separate. Existing filtered/sorted work in M3JDK21 #1303 and feature-branch runtime CollectionViews #1038 is not recreated. SWT/Nebula viewport/event/resource contracts remain with their widget owners; no unrelated migration is included.

## Reproduce

Run `mvn -B -ntp -f m3/runtime-integration/qualification/bitlane-count-skip-20261019/recipes/m3jdk21-bitlane-runtime-count-skip-20261019/pom.xml clean test`, then the adjacent `verify-generated.sh`.

Seven authored SDK tests emit actual `Result.getAfter()` bytes into a fresh build directory. The script consumes that result, compiles the owner into bootstrap-loaded `java.base`, and executes the unchanged real C17 JNI peer. The final workflow receiving check intentionally refuses while production still contains the baseline.

## Actual local evidence and pending gates

Debian OpenJDK 21.0.12.1/GCC 14.2/Linux x86_64 source-fixture runs: strict compilation passed; both versions gave 334,723 Java checks and 371,921 checks with native loaded (37,198 native checks), matching digests/API/JNI headers and missing-native refusal. C1-only Java/JNI and interpreter-only Java also matched. Two compiled mutants were rejected by the unchanged oracle: inverted page counts and eager metadata preparation. These are not local Maven/OpenRewrite execution or a complete installed M3JDK build.

Full JDK/libjava build, jtreg/JCK, installed-image linkage, Windows/macOS, SWT/Nebula rendering/events/accessibility/resources, memory-pressure and benchmark admission remain pending. The finite sparse-corridor timing diagnostic is not JMH or universal performance evidence. History/PR audit coverage is bounded, not complete. Global family/release gates remain unchanged.

## Rights and invariant

Copyright 2026 Hitesh Soliwal and contributors for new first-party recipe/proof expression, Apache-2.0. Preserve original input headers, parent M3 LICENSE/NOTICE and OpenJDK/third-party rights. This packet does not settle src/** intake reviews or relicense code. No challenge/editorial source was copied. Synexia #12454's CATEGORY_REVIEW.tsv records GitHub, LeetCode 191, HackerRank Java BitSet and GeeksforGeeks popcount comparisons with reference-only dispositions.

The normative recipe-first/category-comparison/per-file policy remains `synexia-m3-recipe/docs/M3_RECIPE_FIRST_MASTERY_INVARIANT.md`. Copying source, merging PRs or passing donor tests does not advance compatibility claims.
