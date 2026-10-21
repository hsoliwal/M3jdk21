<!-- SPDX-License-Identifier: Apache-2.0 -->
# Current M3 five-provider interop source admission — 36 owners

M3JDK21 product receiver stacked on target #1210 at exact commit 25d0ceae8645d141b976b4722049d2cca70dc9b6; Synexia owns the reusable Maven/OpenRewrite source-sealed template recipe. The historical five-file recipe and original manifest (Git blob bdc0cab41cd4033421388fbd44f37203d765f57e) are IMMUTABLE and all four historical non-factory owners are unchanged.

The original current-census script tracked just 13 Java source/test paths while the newer independently admitted JDK, Guava, Eclipse Collections, Apache Commons Collections and Spring SDK owner tree contains exactly 34: 18 production files and 16 tests. The 2026-10-17 run 39045561291 failed its exact-head source-census gate because 21 existing files were unadmitted. This successor documents and seals each current tracked path to BOTH its SHA-256 bytes and Git blob identity, extending the exact ALLOWED_PATHS set to 34. Keep the existing five historical predecessor rules, symlink/drift rejection, suspicious new file refusal, and synthetic unit tests, extending the cardinality fixture to 34.

No collection production Java, JVM, JNI ABI, source-migration naming, vendor dependency or public signature changes. This is an admission/receipt repair that permits the separately authored real provider API Maven/JUnit and JNI gates to execute. If any of the 34 sources changes in a newer branch, reject and create the next source-sealed recipe; never rewrite the census by hand. No upstream donor sources copied. Apache, Guava, Spring, JDK and Eclipse licenses remain with the retained original owners.

Original sources are reference values from the pinned M3JDK21 tree, not 'all five libraries implemented'. Real Maven/OpenRewrite JUnit scheduler and package/ABI/interop/HotSpot/multi-platform gates remain to be actually observed. The historic receipt SHA-256 is retained as exact predecessor, not reused as the current receipt.

## 2026-10-21 sealed successor for 76 owners

The original 36-row `current-source-census.tsv` was retained verbatim as
`previous-source-census-20261021.tsv`, with SHA-256 bound in `verify.py`.
At original M3JDK21 source head `712fda47e962631eddebe5e214b820796c755016`, exactly **40 new tracked files**
were separately reviewed and admitted and **7 existing files changed**.
The seven source predecessors and successors are recorded in
`source-successor-ledger-20261021.tsv` using before/after SHA-256 and Git blobs.
The immutable historical five-source recipe remains untouched; the changed
`M3LibraryCollectionsTest.java` historical successor is explicitly pinned to
its old and new hashes instead of disabling the historical protection.
Current verifier rejects incomplete tracked membership, predecessor/ledger drift,
new unexpected source files, mismatched Git blobs, SHA-256 drift, and any
unapproved change to the remaining historical four owners. There are 32
production and 44 test first-party interop files in this exact receipt.

This is an inventory/provenance correction, **not** proof all five donor-provider
contracts work or that 76 sources compiled successfully. The actual original
Java/JNI and full reactor gates must run after source admission; failed or
unstarted gates remain open. Canonical Synexia recipe dataset:
`m3-interop-census-20261021`; no manual edits to production source files.

## Second original-source census epoch — 76 → 76

The previous complete 76-row `current-source-census.tsv` is retained byte-for-byte
as `previous-source-census-76-20261021.tsv`. An immutable one-row
`source-epoch-76-delta-20261021.tsv` records the Java21 test correction in
`M3FiveLibraryArrayificationTest.java`, including old/new SHA256 and Git blobs.
Exactly one test path changed: 76 source identities remain tracked, with zero
addition/removal. The 36-file previous receipt, its seven-source successor ledger
and the historical five-file recipe remain untouched and independently verified.

The current receipt and both predecessor-to-successor epochs are admitted by the
existing fail-closed verifier. Any further source change requires another exact
serial recipe and immutable receipt, not a weakened or dynamically regenerated
file list. The actual prior PR-head four-library run caught the stale test blob
before compilation; this successor is designed to restore the integrity gate,
not to claim JDK/JNI/provider tests pass. Canonical Synexia dataset: `m3-interop-epoch2-20261021`.
