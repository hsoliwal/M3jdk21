<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 collection flow receiver

Copyright 2026 Hitesh Soliwal and contributors.

Recovered from the interrupted collection-flow receiver and requalified on 17 October
2026. Canonical reusable source/recipes remain Synexia convergence material. This task
is a target-specific source transport and proof mirror, not a Synexia runtime service.
The original source manifest/candidate identities are retained. Receiving base is now
05e72c4bbfb1069d1306941ffe8471d373fb3813 (the advanced purpose-wrapper branch).
Its existing M3CollectionViews -> java.util.CollectionViews facade, filtered lists/maps,
other collection owners and JDK APIs are not replaced. DONOR_MAP's older target_base
records the initial candidate, not the current branch. All four selected KEEP dependency
blobs remain identical in the receiving image.

## Scope

19 Java runtime sources and two C17 JNI sources, with four executable target proofs.
Client-key grouping/partitioning, bounded key-serial parallel execution, metadata-only
source access, primitive rolling sums/extrema/scans/pairs/zip, intermediate temporary
accumulation, explicit terminals, virtual index views and stable prepared sorting.
No new M3Views filtering hierarchy: use the existing target CollectionViews owner.
Prepared sorting is explicitly borrowed/revision-bound, not an independent payload
snapshot or automatically resorting owner. Key membership and source stability remain
client contracts. Standard Stream/Collector semantics are unchanged.

Recipe: com.synexia.m3.CollectionFlowJdk21Receiver, thin configuration of unchanged
M3GpuToolJoinRecipe/Manifest. Exact-text source transport is separately checked by the
real Java21 parser; it is not described as an attributed AST refactoring. Templates,
preimages, source hashes, actual SDK tests, native tests and generated output belong
in the same review. Repair the recipe/template, rerun, then materialize serially.

## Executed in this recovery

Offline Maven3.9.6 verify with captured OpenRewrite8.89.0/Jupiter5.10.1,
OpenJDK21.0.12.1 and GCC14.2 on Linux. Five actual SDK methods passed (zero failure,
error or skip); 64 bounded partial states; eight independent four-worker trials;
missing/drift/occupied/duplicate/cancellation refusal; Java21 parse/print roundtrip.
25 actual outputs, four KEEP inputs, exact canonical replay and zero-change fixed point.
Named-module javac and external named-module client passed. Both JNI units compiled
with C17 warnings-as-errors/fatal UBSan and checked JNI. Exact five-symbol header/export
parity. Retained v2: 47 Java groups/1400 cases and 50 JNI groups/1421 cases.
v3: 14 Java groups/131108 assertions and 16 JNI groups/133553 assertions.
Prepared sorting: 345868 assertions per backend. Target composition: 17 per backend.
Counts repeat/overlap, not additive independent coverage. The captured SDK/provider
binaries are not distributed; source/default target dependency policy is unchanged.

```sh
mvn -o -B -ntp -Dmaven.repo.local=/path/to/approved/repository \
  -Dreceiver.output=/absolute/NEW-proof-directory -f pom.xml verify
python3 tools/catalogue_search.py receiver
```

Maven writes only a private generated receiver. Before actual product installation,
require SDK success, whole-manifest admissibility, exact postimage parity and fixed
point. Host provides exclusive worktree custody; no transactional filesystem claim.

Catalogue: 43 records; 37 historical evaluations plus six original receiver records.
Competitive-programming pages are problem references, not copied solutions. Provenance
and scoped MIGRATION.tsv supplement, not replace, the target global naming authority.

## Acceptance boundary

This is selected-source module/Java/JNI qualification, not a full module/reactor or
built-JDK/jtreg/JCK/HotSpot result. The current wrapper facade's custom java.util API
is retained but not qualified by the isolated receiver compile. Keep draft pending
broader integration, global migration/admission, serialization/concurrency, other native
platforms and performance review. Arrays/masks refinements are a separately sealed
successor; this baseline still uses a HashMap for group positions.

Shared image inode and equal virtual addresses remain distinct proofs; this task does
not alter the fixed-address mapping experiment. No global phase is advanced, no master
or donor branch rewritten, no public JDK interface removed.
