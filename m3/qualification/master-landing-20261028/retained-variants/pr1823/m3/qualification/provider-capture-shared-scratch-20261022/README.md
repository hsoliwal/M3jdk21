# M3JDK21 M3FS/M3File use-case-qualified provider/capture/scratch superset

The product is **one integrated Java21/HotSpot/JNI SDK**. This is a stacked
receiving change above M3JDK21 PR #1814, retaining its native m3db SQLite
VFS and NIO ZIP/JRT providers, its full MFileContent defaults and its
original one-read M3File.captureBytes(). No new source tree, parser,
collection storage owner, SQLite driver or JNI layer is created.

The original donor M3JDK21 PR #1817 adds a reusable 1..1048576-byte
M3File.observe scratch overload and moves M3HostFileInventory to one
64KiB scratch for its entire serial Stream<M3File> pass. Both existing PRs
share the same old M3File.java preimage. A whole-file replacement would
lose either captureBytes or the scratch overload. The canonical source
is therefore #1814's M3File plus ONLY the original #1817 borrowed-scratch
behavioral atom. The existing M3HostFileInventory and its original
M3FileSharedScratchContract test are taken unchanged from #1817.

Use cases reviewed:
- IDE/JDT/compiler complete source bytes, timestamp/key SHA proof:
  M3File.captureBytes, no second fingerprint read.
- Million-file offline Maven/Gradle project snapshot and SQLite generation:
  one 64KiB buffer per serial scan, not one 8KiB allocation per file.
- Optional parser/AST/hash downstream fanout: consume original source
  bytes synchronously, preserve no-callback-on-error and progress contracts.
- ZIP/JRT and other NIO filesystem providers: preserve original provider
  identity, lifetimes, path and archive traversal semantics from #1814.
- m3db native SQLite project namespace: preserve original M3FS WAL/JNI owner,
  not another JDBC production engine.
- Dynamic Object[]/primitive-array physical offsets, masks, sorted/filtered
  collections and native JNI: preserved, untouched by this pass.

Canonical Synexia recipe: #13006, reusing M3HashPinnedJavaSnapshotRecipe,
three exact before/after SHA256 source seals, two-cycle fixed-point and
stale preimage refusal test *definitions*, category/donor comparison and
append-only correction memory.

Proof distinction: Exact original source preimage and canonical product
postimage equality **PASSED** for three received Java/test targets.
Original PR #1814 capture test and #1817 scratch test receipts are
**historical donor evidence**; combined Maven/OpenRewrite, actual
M3File/M3FS original JUnit/native JNI, full JDK21/HotSpot/JMOD/jtreg,
Windows/macOS/Android providers and CPU/heap/native performance are
**NOT RUN** on this combined source. Do not claim a measured speedup,
safe automatic build skip or completed wider SDK superset.

Keep this a **reviewable draft** until full owning contracts are executed.
