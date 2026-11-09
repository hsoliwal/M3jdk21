# Source-generation revalidation (Synexia private recipe)

This proposal is stacked on the original preflight #16571 and uses the **incumbent** `M3HashPinnedJavaSnapshotRecipe`.

Local JDK21 structural and deterministic proof, then mandatory real Maven + JNI gates in a complete checkout:

```sh
mvn -B -ntp -f synexia-openrewrite-recipes/recipe-crates/m3-pmem-gram-source-revalidate-20261105/pom.xml verify
M3_USE_PMDK=off bash m3-pmem-sdk/scripts/build-and-test.sh
mvn -B -ntp -f m3-pmem-sdk/campaign/pom.xml -Dtest=PmemGramCampaignAdmissionJUnitTest -Dpmem.gram.campaign.native=true test
mvn -B -ntp -f m3-pmem-sdk/campaign/pom.xml -Dtest=PmemGramCampaignAdmissionJUnitTest -Dpmem.gram.campaign.scale=true test
```

Do not treat a queued or skipped job as a pass; do not publish recipe code into public M3JDK21/Eclipse target forks. Donor references are category-only, not source-copy authority.
