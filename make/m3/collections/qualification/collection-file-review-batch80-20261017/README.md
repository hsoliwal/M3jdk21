# Collection file review batch 80 qualification

This unit receives six exact TSV/Markdown files through the named recipe `com.synexia.m3.CollectionFileReviewBatch8020261017`. The existing first-party text snapshot engine admits only absent targets or their source-sealed postimages. The custody verifier requires sorted, unique manifest owners and exact runtime/template bytes, and refuses symlink or junction aliases.

The local Java 21/OpenRewrite 8.17.1/JUnit 5.10.2 run passed both recipe tests: 64 installation subsets in two input orders (128 states), six postimage hashes, unrelated-file preservation, fresh marker-empty parsing and recipe reactivation with zero changes, and 12 drift/duplicate refusals. Received-table checks bind 80 unique source inventory rows, 24 categories, 23 candidate comparisons, 22 input-provenance rows and the declared review/license boundaries. Hosted CI is pending.

These 80 rows describe the compile-bound source inventory. They represent neither 80 new implementations nor the complete tree at latest master. Per-type interface and behavior evidence stays `REVIEW_REQUIRED`; the catalogue admits no behavioral replacement or performance winner. Shared compilation and exact byte custody do not establish those admissions.

The existing `AlgorithmCatalogue`, `PinnedAlgorithmDonorCatalog`, `ProblemCategoryCrosswalk`, `SerialProblemReview` and `M3SerialFileAtomConvergenceRecipe` remain the reusable retrieval/review surfaces described in [the catalogue README](../../algorithm-catalog/README-collection-file-review-20261017.md). This publication adds static links and an executed receiving recipe. See the recipe's [reuse decisions](../../recipe/collection-file-review-batch80-20261017/FOSS_REUSE_DECISION.tsv) and [source attribution](../../recipe/collection-file-review-batch80-20261017/THIRD_PARTY_NOTICES.md).

Run the narrow checks from the repository root:

```sh
python3 -I -S -B m3/collections/qualification/collection-file-review-batch80-20261017/verify.py
mvn -B -ntp -f m3/collections/recipe/collection-file-review-batch80-20261017/pom.xml test
```
