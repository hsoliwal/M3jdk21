<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 PMEM two-distinct-byte-gram conjunction (recipe-first)

Canonical private Synexia recipe, stacked on #15431 at `87acd7a0fb85fe47ad082de97270747b52603024`. Two full before/after Java 21 sources (one original private search owner, one JUnit suite), Maven/OpenRewrite hash-pinned recipe and donor category review. Original `PmemCorpusSearch`, KMP C17/JNI and source image owners remain unchanged.

The candidate intersects the two globally rarest **distinct** 2-byte grams required by a query. This is a necessary source-file prefilter, not a replacement search engine or license to change source code. The incumbent native/JNI or Java FILE KMP still validates every candidate's first exact position. No added Java/JNI public API or native symbol.

Verify on a full checkout:

```bash
bash synexia-openrewrite-recipes/recipe-crates/m3-pmem-gram-conjunction-20261105/verify-java21.sh
mvn -B -ntp -f synexia-openrewrite-recipes/recipe-crates/m3-pmem-gram-conjunction-20261105/pom.xml verify
M3_USE_PMDK=off mvn -B -ntp -f m3-pmem-sdk/pom.xml verify
mvn -B -ntp -f m3-pmem-sdk/campaign/pom.xml -Dtest=PmemCorpusGramIndexJUnitTest -Dpmem.gram.native=true test
```

Keep draft/HOLD until actual SDK Recipe.run/fixed-point, JNI/JMOD/JUnit and provenance receipts pass. No protected develop rebase or public fork recipient recipe copy.
