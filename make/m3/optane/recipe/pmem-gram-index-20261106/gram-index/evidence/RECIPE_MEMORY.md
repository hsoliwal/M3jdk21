# Improvement memory: no duplicate native KMP or file entry objects

The previous `PmemCorpusSelectedSearch` already isolates a sparse reviewed source subset, but each selected source still calls the underlying JNI/FILE KMP. Add a **source-sealed necessary-condition** 2-gram index rather than inventing a second substring matcher. Select rarest gram by immutable document frequency, skip only files that provably lack the pair, and retain exact original first-hit contract, selection identity, call custody and output order. A query under two bytes bypasses prefilter.

Reject LeetCode/HR/GFG donor code as replacements until exact source, behavior, API and license gates succeed. Study the problem taxonomy only. Existing first-party JNI is the only native execution owner, and original Maven/OpenRewrite recipe code remains private to Synexia.

A complete 1 GB mapped corpus can occupy virtual address space without 1 GB Java heap, but source verification still allocates one original byte[] per read; index budget must not be confused with OS RSS. For a 50 KB file, at most 65,536 distinct two-byte grams can occur. Verify true 20k×50KB peak resident memory separately; smaller synthetic tests cannot establish production 1GB throughput.
