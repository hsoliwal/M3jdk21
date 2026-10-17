# Reused workspace output observation

During a second isolated fixture attempt, a path deleted and recreated for that attempt contained the prior generation's jmod: the jmod retained its `2026-10-17T12:11:02Z` timestamp while its newly prepared input descriptor had `2026-10-17T12:13:53Z`. A separately described result still had the earlier duplicate target attribute. A collaborator independently observed an older class reappearing in a replaced product proof directory.

These facts establish stale output at the reused paths; they do not establish a product source defect or a specific filesystem implementation cause. The final proof builds in a never-reused directory under `/tmp` and records its exact artifact hashes. All final Java 21 module assembly, stock-entry comparisons, linked-image checks, and unmodified upstream tests passed in that fresh generation.
