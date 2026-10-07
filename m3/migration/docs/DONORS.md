# Prefix-facts donor and problem-category record

This is a scoped donor evaluation, not the requested exhaustive algorithm catalogue. No LeetCode, HackerRank or GeeksforGeeks solution code is copied into the public target.

| Evidence / problem | Use | License and adaptation | Correctness / tradeoff |
|---|---|---|---|
| Synexia MIndexPrefixZ, PR #7526 head 73978088621dc70f5021befafa443f7e622b517e | Executable source lineage | Apache-2.0 file notice retained; user-authorized adaptation only | Existing Z-box idea adapted to M3StringPiece; target callback and primitive metadata accounting differ |
| HackerRank String Similarity | Common-prefix/suffix similarity category | Problem statement inspected, not solution-code permission | Target preserves arbitrary UTF-16, not only the challenge alphabet; independent String oracle tests the sum |
| LeetCode 2223, Sum of Scores of Built Strings | Related catalogue title | Title identified; full statement/license not established in this session | No solution copied or performance result inferred |
| GeeksforGeeks Z algorithm | Technique comparison | Article inspected as explanation, not blanket code-copy authorization | Its usual Z[0]=0 convention differs from the source's self-suffix Z[0]=n. Do not copy separator-based flattening into arbitrary UTF-16 retained views |

Source URLs:
- https://github.com/hsoliwal/com.synexia/blob/73978088621dc70f5021befafa443f7e622b517e/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexPrefixZ.java
- https://www.hackerrank.com/challenges/string-similarity/problem
- https://leetcode.com/problems/sum-of-scores-of-built-strings/
- https://www.geeksforgeeks.org/dsa/z-algorithm-linear-time-pattern-searching-algorithm/

No target-only change is automatically reverse-ported. The inner-scan cancellation improvement should be proposed separately to Synexia with its own IProgressMonitor tests. Public visibility is not permission to copy donor code, and OpenJDK notices/licensing remain unchanged.
