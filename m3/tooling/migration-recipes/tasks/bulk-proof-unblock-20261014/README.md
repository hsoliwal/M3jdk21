# M3JDK bulk proof gate receipt

Canonical source: hsoliwal/com.synexia PR #11064, portable recipe
`com.synexia.rewrite.M3Jdk21BulkProofUnblockRecipe`.

Mirror: `com.m3.rewrite.backport.M3BulkProofUnblockRecipe` using the existing
`M3Jdk21HashPinnedSnapshotRecipe`. All three current-tree target preimages are exact,
source-reviewed and fail closed on drift. Receiver does not introduce a new rewrite engine.

Executed earlier failure run: `38497544839`, jobs `115636665551` and
`115636665718`. Issues: malformed test literals; three non-ASCII em dashes
inside comment-only FLLDCIM annotations in Swing classes. Build source compiler uses
`-encoding ascii` in `make/common/JavaCompilation.gmk`.

The Java target files retain their original GPL/OpenJDK headers; their Java executable
statements are unchanged. There is no new JDK API, hotpath or JNI mutation.

Acceptance gates:
- exact recipe source/target readback and second-pass fixed point;
- module-level migration recipe test compiler and JUnit;
- JDK release image and relevant jtreg;
- independent memory/performance proof if any performance claim is later made.

This packet does not grant promotion authority.
