# M3 recipe correction memory: GramIndex third necessary condition

The original two-distinct-gram prefilter already had a contract-preserving
first-hit search; DO NOT replace or duplicate its JNI KMP.
A corpus with 9000 files containing ab+bc but not cd, 9000 containing cd
alone and 2000 containing exact abcd gives equal document frequency for
ab/bc/cd. The original filter must examine 11000 candidate files but only
2000 contain exact abcd. A third distinct pair membership check removes
9000 false positives without losing any match or changing API/JNI symbols.

One exact Java file-local atom and one JUnit regression are owned by
M3HashPinnedJavaSnapshotRecipe PRE -> POST. Preserve problem-category decisions
and no external challenge/solution source copying. Fixed point and source
drift refusal must execute under Maven before promotion; native FILE/POSIX
JMOD, true CPU/RSS and 1 GiB physical corpus remain independent HOLD gates.
