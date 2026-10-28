<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 original JNI LC719 rank selection — 2026-10-26

M3JDK21 is the JDK product, **not** M3 plus JDK or a runtime adapter. This pass edits the existing original `M3LongRankBatchNative.java`, `M3LongRankIndex.java`, and `m3_long_rank_batch_jni.c`, adds independent Java21/JUnit oracle tests and a focused source/JNI proof. No java.base replacement, jmod or HotSpot integration is claimed yet.

**Synexia convergence:** PR #13224 commit `e7d9e06d5b2c20ee3f805c1e7ed8033615801ab6`, two source-sealed recipes `Jdk21Lc719KthPairDistance20261026` (Java AST) and `Jdk21Lc719KthPairDistanceJni20261026` (original C text), algorithm catalogue and source templates. The product receives only their byte-exact postimage source and test/CI result, NOT an OpenRewrite recipe or Synexia runtime dependency.

**Category:** LeetCode #719 K-th smallest pair distance, binary search on result with an O(n) two-pointer count predicate, extended from LeetCode's constrained integer domain to signed `long` including duplicate occurrences and extreme full mathematical distances. This is first-party original code; no challenge solution was copied.

**Contract:** One-based rank `[1,n*(n-1)/2]`; `IndexOutOfBoundsException` for illegal ranks; `ArithmeticException` for a distance greater than Long.MAX_VALUE; null and caller-owned output offsets validated before mutation; no source/output alias. The original immutable sorted `long[]` owner is read-only. A Java O(n log(range)) reference remains available. The existing `libm3rankbatch` gets one additive JNI export `kthPairDistancesInto0` using two fixed 256-value caches and 128-query/output stack pages. No retained GC addresses, callbacks, boxed per-pair values, JNI owner swap or duplicate runtime layer. Native opt-in uses the existing property; native-required tests fail closed if the export is missing. Existing native symbols and old Java methods remain untouched.

**Exact product source blobs:** Java native owner `022cefecdee793a8e6ea91b4a4e9496480ca14ac`; public index `6ba52962e3fd3c1efb397550ffd72670e9c2a293`; native C owner `d52ddd76ccc2cb53c7175a3a62155d7e34eb7234`; standalone test `d36e442bee52c3f69f4df717d694b686e3f9acc1`; JUnit wrapper `df65643e3e909b7b66168cb30c7bd85f93244b10`; focused workflow `8c1106d4f4e30a65695d19480065060a44d1fc16`.

**Locally executed isolated prototype:** OpenJDK 21 strict javac; C17 strict `-Wall -Wextra -Werror -fsanitize=undefined`; 25,053 pure Java oracle checks, 36,542 JNI-required checks, 36,542 `-Xint -Xcheck:jni` checks passed. The method and C kernel blocks are byte-identical with the prototype snippets; actual expanded original owner source/JNI still requires CI to execute.

**Pending gates:** actual full original-source native workflow on this exact head; Synexia actual OpenRewrite Java/Text SDK fixed-point/missing/drift tests; complete Maven/JUnit/JNI/installed-JDK, jmods, jtreg/JCK, cross-platform and performance profiling. PR stays **DRAFT/HOLD**, no automatic merge or deployment.
