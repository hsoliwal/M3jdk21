# Rank select and original Maven/JNI continuation

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0.

This continues #1566 using the existing integrated M3JDK21 owners. The existing rank decoder recognizes a contiguous low-bit selection word and returns the word offset plus rank directly. Other words retain the original bit-clearing decoder; the adaptive sparse/dense window traversal remains unchanged. There are no new storage owners, runtime public classes, native methods or JNI implementations.

Canonical [Synexia #12563](https://github.com/hsoliwal/com.synexia/pull/12563), commit `9c8c0c58c7bdd2c8a0ca47c7ff9fa92bf3a0e7b1`, owns both reusable recipes, templates, tests, catalogue and the complete evidence packet at `synexia-openrewrite-recipes/tasks/m3-index-rank-prefix-select-20261020/qualification-20261020`. The existing Java engine generated the owner and two proof sources. The existing text engine generated the repaired first-party JUnit launcher verifier, resolving repository-relative paths and using the actual coordinate/source-jar manifest schema. Existing upstream source and legal bytes are unchanged. Neither canonical engine is duplicated into the product.

Final verification: **469 original-POM Maven tests**, no failures/errors/skips; **16 real SDK recipe tests**; **12,595,606 scalar-oracle assertions**; ten native libraries built with checked JNI and fatal UBSan; 180 rank JNI parity paths; 36,476 interpreter rank checks over 900 random paths; two unchanged public surfaces and all 12 unchanged generated JNI headers. Five actual verifier acceptance/refusal cases preserve source, inventory and packaged-license checks. `receipt.json` binds outputs and retained local evidence; the canonical final receipt contains the full build-log inventory.

All ten pre-existing source-owned dependency POMs now pass clean install in dependency order, including their configured Kotlin and module-descriptor work. Independent review found that plain install could retain stale classes. The defect was reproduced without installing the contaminated jar. The repaired driver seeds a disposable compiled class before each clean build and verifies its absence from classes and every built jar. All ten removals passed. Donor POMs and upstream bodies remain unchanged; their published source jars do not contain upstream test suites.

This closes the previous packet's original collection Maven dependency HOLD for this source scope. It does not erase historical results or promote the String-first qualification phase. The full source-built installed JDK/HotSpot, jtreg/JCK, canonical reactor/OpenRewrite source closure, other platforms, remaining tool families and retained/native memory remain open. Hosted CI is separate and was not claimed green.

`Long.expand` was evaluated and rejected as a general decoder after slower forced-software execution. The selected arithmetic path removes 2,016 bit-clearing iterations for all ranks in a full word in the disposable work probe. Paired timings include slower cases; no universal throughput improvement is claimed. Full catalogue decisions, candidate pins, review and failures are retained canonically. The inherited LeetCode/HackerRank/GeeksforGeeks references are category references, not copied solutions.

From that exact canonical checkout, with Java 21 in `JAVA_HOME`, Linux GCC, Python and Maven:

```sh
mvn -f synexia-openrewrite-recipes/tasks/m3-index-rank-prefix-select-20261020/qualification-20261020/pom.xml -Dm3.prefixApplyRoot="$M3_TARGET" -Dm3.provenanceApplyRoot="$M3_TARGET" test
python3 synexia-openrewrite-recipes/tasks/m3-index-rank-prefix-select-20261020/qualification-20261020/verify_maven.py "$M3_TARGET" /tmp/m3-prefix-proof-new --maven /absolute/path/to/mvn --offline
```

Populate the Maven plugin cache before offline reproduction. Optional `--settings` accepts an existing settings file. Use an absent output directory and exclusive checkout access for serial materialization. `INPUTS.tsv` seals 1,443 source/build/provenance inputs. The separately sealed schema recovery recipe is only for the exact intermediate verifier; do not sequence it after the direct final recipe.
