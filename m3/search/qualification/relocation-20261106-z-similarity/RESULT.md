# m3/search leaf 6: M3ZSimilarity (Z-score sum), Java + JNI (2026-11-06)

**Donor.** com.synexia develop, m3-fast-search commit 32a73ab98a2e (2026-11-05, "add tested UTF-16 Z-similarity Java/JNI outputs"):
- M3ZSimilarity.java;
- jni_z_similarity.c;
- 4 tests.

**Recipe.** M3_SEARCH_RELOCATE v2 with rules_v6.tsv. That is rules_v2 plus three rows:
- `StringsZAlgorithm.zArray(input)` becomes `M3StringAlgorithms.zFunction(input, jdk.m3.algorithm.M3ProgressMonitor.none())`. This keeps one Z-function owner (jdk.m3.algorithm). Both arrays have z[0] = 0 and the same z[1..], and the sum starts at n, so it does not depend on any z[0] convention.
- The import of com.synexia.algorithms.string.StringsZAlgorithm becomes jdk.m3.algorithm.M3StringAlgorithms.
- DONOR_TEST_DEFECT: M3ZSimilarityPeriodTwoNativeTest's offsets list names index 2 for length 2. `units[2]` throws AIOOBE before any native call, in the donor too. The relocated test skips offsets >= length.

jdk_naming then rewrites the 5 provenance lines and reaches its fixed point.

**Module changes.**
- m3/search/pom.xml: the new dependency com.m3:m3-algorithms. algorithms is a default reactor module and depends only on m3-collections.
- src/main/native/CMakeLists.txt: m3search is built from jni_search.c and jni_z_similarity.c, as the donor's synexia_m3 target is.

## Gates

Stock JDK 21.0.9 and MinGW gcc, on master c6ce2ad332e6 (leaf6_gate.sh + search_gate.sh).

| gate | result |
| --- | --- |
| m3/search main javac -Xlint:all,-module -Werror | 34 files -> 35, 0 warnings |
| test tree javac | 0 errors |
| gcc -shared -std=c11 -Wall -Wextra -Werror jni_search.c + jni_z_similarity.c | 0 diagnostics |
| M3ZSimilarityTest | 3/3 |
| M3ZSimilarity{,PeriodTwo,Uniform}NativeTest (-Dm3.native.enabled, -Dm3.native.library.path) | 1/1, 2/2, 2/2 |
| ZDiff: brute force = donor sumJava = relocated sumJava = relocated native | PASS, 40,000 cases (seeds 20261106 and 7), lengths 0..600 over periodic, uniform and random text with surrogates |
| default m3/search suite (Native tests excluded, as surefire does) | live 67/0, leaf 70/0 |
