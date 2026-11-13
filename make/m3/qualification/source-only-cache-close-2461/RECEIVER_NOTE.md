# Source-only M3JDK21 V10 + payload-cache close union

**DRAFT/HOLD.** This is original Git ancestry/source preservation, not installed product activation.

First original parent: 9fc254894dfe386e9e7202ae2655beae89be653b (PR #2404).
Second original parent: 2e5af99316bc3472ae573d78d9b2518d86d20d61 (PR #2461).

Current active `m3/core/src/jdk/m3/text/SharedLexiconCatalog.java` Git blob remains 14fb1241d2ad1aad1a861a65f5b356d23fb8804a.
Qualified donor `SharedLexiconCatalog.java` Git blob 2a6daa583453e83842b75be310a8e204f0004201 is retained as an **inert `.java.txt`** under this qualification folder, with exactly one cache-release statement after `closed=true` under the same lifecycle write lock.
The original canonical private Synexia Maven/OpenRewrite and 14-JUnit source authority is #16377; its recipe source is not copied to this public product.

The donor is 4 commits ahead and zero behind this receiver, but has additional workflow/tool changes, precompute payload and V11 migration metadata. Those are reachable through the genuine Git second parent, but are **not installed** into this receiving tree. See ORIGINAL_HEAD_DELTA.tsv.

The reviewed pre/post source blobs matched exactly at admission. The first-parent Git tree receives only the 3 inert qualification assets: held source, original-head delta and this receipt; no active Java/native/workflow changes, rename or deletions.

Original donor qualification (14 Java JUnit and focused SDK) is not a combined JDK image/JNI/GC/JIT/Windows/ABI proof. Original donor hosted CI is queued; before activating the change, update current source-bound recipe pins and rerun all relevant product gates. Do not merge to protected master while HOLD.
