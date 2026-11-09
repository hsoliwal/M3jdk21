# Synexia M3 reviewed gram campaign — canonical private recipe

Parent: `hsoliwal/com.synexia` PR #16531 at SHA
`75aaac3c08eb3f80061c1823fd8360ca8a23393a`.

Exactly two original absent-before Java source outputs, pinned by full SHA-256
in `src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-pmem-gram-campaign-20261105/manifest.tsv`.
No original Java/JNI owners change. Recipe is committed first, generated output
second. Never rebase develop or copy recipes into public product forks.

Local restricted proof:
`bash synexia-openrewrite-recipes/recipe-crates/m3-pmem-gram-campaign-20261105/verify-java21.sh`

Real qualification (must run on complete source checkout, not a stub):

```sh
mvn -B -ntp -f synexia-openrewrite-recipes/recipe-crates/m3-pmem-gram-campaign-20261105/pom.xml verify
M3_USE_PMDK=off mvn -B -ntp -f m3-pmem-sdk/pom.xml verify
mvn -B -ntp -f m3-pmem-sdk/campaign/pom.xml -Dtest=PmemGramCampaignAdmissionJUnitTest -Dpmem.gram.campaign.native=true test
mvn -B -ntp -f m3-pmem-sdk/campaign/pom.xml -Dtest=PmemGramCampaignAdmissionJUnitTest -Dpmem.gram.campaign.scale=true test
```

All proof limits are explicit in `CONTRACT.md`. Keep draft HOLD until native,
Maven/OpenRewrite and canonical provenance/custody actually pass.
