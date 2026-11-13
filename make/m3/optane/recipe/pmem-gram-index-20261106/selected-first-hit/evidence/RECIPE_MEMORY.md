# M3 recipe-first correction memory — sparse first-hit search

- Inventory/source freeze: existing `PmemCorpusSearch.java` SHA-256 `3493a0f911023d8863d86cdf1b8f19c08d25d4aa5a76be8cfacb3bdc1bc64deb` and Git blob `31f6ab6e419033630c6b09a8700c6a064dea6339`. Public signature/behavior, native C17 JNI exports, region leases, and full-source search remain unchanged.
- Reuse existing source-index owner and KMP engine. Adding a public method to `PmemCorpusSearch` would violate the strict unchanged-interface policy, so the selected subset is a **new package-private source atom**, not a rewrite of the original.
- Reject noncanonical file-index selections before any native or cancellation callback. Defensively clone selection and needle. Bind the ordered selection to the snapshot generation in an explicit, domain-separated SHA-256 identity.
- `complete()` is selected-set completeness; a result-cap stop must not be misinterpreted as global or selected-set absence.
- Prefer primitive `int[]` and `long[]` result lanes; no per-hit Entry or Node objects.
- LeetCode #28 first occurrence is a semantics donor; HackerRank Find a String counts overlapping occurrences and is incompatible; GeeksforGeeks KMP is a category reference; the exact first-party PMEMSDK implementation is the selected source code owner.
- Make the **canonical Maven/OpenRewrite recipe** (two `ABSENT -> SHA-256` new Java/JUnit sources) the transformation authority. Exact source/image and fixed point are independent of compiled/JNI behavior; do not confuse tests on API-shaped stubs with native acceptance.
