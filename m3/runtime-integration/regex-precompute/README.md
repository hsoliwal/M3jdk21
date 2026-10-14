# M3String regex precompute receiver — HOLD

Canonical value mapping remains **Synexia MIndexString -> java.lang.M3String**. Public String and its existing Pattern fallback are unchanged. This receives only bounded compiled dot/star plans, observed-frontier effects over canonical tuple ranges, and the target-owned private JNI scalar kernel. It does NOT receive the whole Synexia regex/DB/compiler/image family.

Canonical recipe and all generated resources/tests: `hsoliwal/com.synexia`, `synexia-openrewrite-recipes/recipes/m3jdk21-regex-precompute-20261014`, class `com.synexia.rewrite.M3Jdk21RegexPrecomputeReceiverRecipe`. Java/text source-seal engines remain canonical in Synexia. Target preimage revision: `d5abed865bd47c4dd5ad78e95704348fe2095ed2`. Exact target files must equal the canonical manifests' SHA-256 postimages. Cross-repository PRs identify the published source revision; no Synexia JDK runtime dependency is introduced.

```sh
# First use the exact canonical recipe checkout identified in the receiving PR:
mvn -B -ntp -f /path/to/com.synexia/synexia-openrewrite-recipes/recipes/m3jdk21-regex-precompute-20261014/pom.xml verify
mvn -B -ntp -f m3/runtime-integration/regex-precompute/pom.xml -Dsynexia.checkout=/path/to/com.synexia verify
# After a real target JDK build/configuration:
make test TEST=jtreg:java/lang/String/M3StringRegexPrecomputeTest.java
```

The thin Maven receiver verifies materialized bytes and calls the canonical fixture proof. It does not clone/reimplement the recipe or constitute a product build. Four explicitly TEST_ONLY storage classes were used locally, alongside exact candidate Java/C, stock Java21 Pattern, and the frozen target DP. Java 2205708 / JNI 2280860 assertions passed; 75136 required native calls ran under UBSan and -Xcheck:jni. Counts overlap and are not unique-case totals. Interpreter repetition timed out without a completion receipt. Local Maven/SDK could not run. Full source-built JDK/HotSpot, actual jtreg, canonical native-pool GC/lifetime, platform/ABI, full-borrow receiver checks, and JMH/JFR/RSS are still required.

Small-input cold diagnostics regress while warm observed-frontier reuse improves. The 256-unit native threshold is a candidate, not a validated crossover. Keep the review branch and PR on HOLD; do not auto-promote or advance the String -> arrays -> collections -> compiler receiving phases. Invalid/unsupported regex still belongs to Pattern, so no general ReDoS-safety claim is made.

Fix canonical templates/recipe and rerun after any defect. Do not patch receiver output, rebase, squash or force-update default branches. Retain OpenJDK per-file target licenses; Apache recipe licensing does not relicense generated target code.
