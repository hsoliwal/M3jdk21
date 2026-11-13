# M3JDK21 lexicon plus native-backed LFU source superset (2026-11-05)

**Nondefault draft integration candidate; not promoted to master.**

Exact original first parent: 306b54ad6c70a7835190bd04427b416a01f08929 (M3JDK21 #2474, complete stacked direct-image lexicon source history).
Exact original second parent: a12cfe5aab802e23abf0c17506db17c7cd1a1684 (M3JDK21 #2468, current-master LFU identity-slot improvement).

## Source and contract

- Verified receiver original `M3LFUCache.java` Git blob f2d1a17877561a1143965e195d1710068f23c57e matches the donor's reviewed original master preimage. The exact donor postimage is baa982dd4ea6706b89d85741f29602f4acf3df1a and is installed at the same active original source path without reformatting.
- Donor #2468 changes only this package-private Java LFU cache implementation, adding one private identity-slot field and invalidations. No public/protected Java method or class descriptors, JNI C exports, JDK `Object`, lexicon image source, native library binaries or unrelated file changes.
- #2474 owns the independent lexicon `SharedLexiconImage` direct-view atom and its source-sealed recipe/test evidence. #2468 owns LFU Java/native parity; the private canonical Maven/OpenRewrite crate, donor review and 16 JUnit proof methods are in Synexia #16460, not copied into the public JDK fork.
- The Git commit has both exact original source histories as parents. This receiving tree modifies only the original LFU Java file and adds two qualification records. All other first-parent source modes and Git blobs are unchanged.

## Qualification limits

- Original #2468 source-bound Windows Oracle JDK21/GCC native proof: 17 matching JNI exports, five Java-only and 31 native JUnit cases, 16 focused LFU JUnit methods and 120,000 seeded differential operations. These are original donor-scoped receipts, NOT new evidence against this combined branch.
- Donor performance tradeoffs: at capacity 1024, repeated-key median 490.165 -> 5.68 ns/query while alternating-end 321.0825 -> 365.815 ns/query; capacity-1 repeated key regressed. No blanket speedup claim.
- Source-refinement category is identity-array cached lookup; LC/HR/GFG references remain taxonomy only, not source-copy authority.
- Exact-head CI on donor previously failed four-library receiver admission because its historical 84 source census differed from current master 86; other original source contract jobs had failures. The combined lexicon chain has its own unresolved workflow source-binding and NLP lock checks. None are waived.
- NO current-head combined JDK 21 source/JMOD/image, Maven/OpenRewrite fixed-point, JUnit/jtreg, loaded JNI, ABI, GC/JIT, Linux+Windows/macOS or performance acceptance asserted. Leave #2474 DRAFT/HOLD until those gates pass.

## Hard invariant

Full Java source remains a one-way JDK21-compatible result. Historical Git ancestry and tests are preserved; no force-push/rebase/squash, no change to master and no silent overwrite of a conflicting source owner.
