<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 installed collections source V3 and Maven artifact provenance

**Stack**: source `hsoliwal/M3jdk21@80332fcefd2b9c23501a86cb0ca01ee476460795` (draft PR #999), not
the `master` branch and not a replacement of the JDK21 public collection APIs.

## Actual independent failure evidence
[Existing M3 collection contracts run 38998743680](https://github.com/hsoliwal/M3jdk21/actions/runs/38998743680)
on #999 failed two execution jobs:
1. `verify_donation.py --cost-probe`: after verified JDK21 and 1.12.2 JUnit artifacts,
   PR #999 synthetic merge checkout installed manifest **actual**
   `6627c851e6a879e28c0434b41f988669c118649e04a4d89bcf30102aa9e767ed`
   versus **expected** `90e6e4adcc6a9224b6ca7f85b5d01b950ed8c368fcc660b04ce56fda59eae689`.
   The pinned head retains the expected historical manifest SHA; the old inventory still has fewer paths than the installed Java source set. Treat the synthetic merge-tree hash mismatch as a separate Git ref custody discrepancy. Do not change the
   expected SHA alone or weaken duplicate/path checks: generate a NEW complete
   path + SHA256 manifest for this exact source epoch, and update only the root
   JSON receipt pointer; retain all original/historical manifests and nested JSON.
2. Historical Maven lane recipe asks for non-public/nonexistent Central version
   `org.openrewrite:rewrite-java-21:8.17.1-m3-source-1`.
   The main module POM analogously points at `org.junit.jupiter:junit-jupiter:5.10.2-m3-source-1`.
   Source-build-specific coordinates are not generally available in Central.
   Choose existing known published `rewrite-java-21:8.17.1` and `junit-jupiter:5.10.2`
   for these two isolated **test-only build dependencies** (no production runtime edits).

## Reusable Maven/OpenRewrite recipe and SHA custody
The precommitted named recipe
`com.synexia.m3.RepairUnresolvableCollectionTestCoordinates20261017`
delegates to existing published `org.openrewrite.xml.ChangeTagValue` (Apache-2.0)
inside a real isolated Maven/JUnit 21 project. `M3CollectionDependencyRepair`
reuses its Java/XML visitor, sealed source/postimage SHA256 fixture inventory and
fixed-point logic. No separate Java source parser or generative LLM core.
Two exact original XML POMs and reviewed postimages reside in `resources/`,
with complete `pom-manifest.tsv`. `M3CollectionDependencyRepairTest` actually
executes OpenRewrite XML against the original POM strings, tests exact
postimages/fixed point/partial delivery/missing/drift/occupied/extra refusal.
Fix recipe and rerun, never patch installed emitted targets.

A second cohesive JDK21 recipe `M3InstalledSourceInventoryRecipe` reuses JDK
`Files.walk` + `MessageDigest(SHA-256)` to produce the **ordered** 136-row
full current ledger: every ordinary Java file under `src/main/java`,
`src/test/java` (84 package-level main + 1 module-info + 47 test = 132 Java), `LICENSE`, `NOTICE`,
and two known `verification/*.java` native probes. Refuses symlinks,
unexpected cardinality, path traversal, duplicate paths, unpinned source
commit, partial trees or SHA drift. This is a source-bound *inventory*
receipt, not migration or source rewrite. No source code has been changed.

`M3DonationPointerRecipe` accepts only the Git-blob-verified complete
original `synexia-donation.json` and its own exact generated fixed point,
updates only the top-level installed manifest path+SHA256, leaving all other
nested receipts and attribution byte-identical.

### Required sequence
1. **Source first**: frozen donor POM/JSON preimages, exact POM postimages,
   new receipt emitter and JUnit tests committed; no target files changed.
2. Run real Maven 21 recipe, assert XML SDK results and source/refusal tests;
   collect receipt candidate bytes from isolated `target/receipt-candidates`.
3. Commit that exact generated manifest and the reviewed JSON pointer
   candidate; install the two POM postimages *serially* only after JUnit green.
4. Rerun exact receipt/SDK tests to fixed point; then run actual module JUnit/
   JNI acceptance. Keep historical manifests intact. Any changed source member
   or hash must cause refusal and require a **new successor recipe**, not
   an automatic CI checksum update.

## Donor/category and licence boundaries
- OpenRewrite XML/JDK21: Maven test dependency correction, not application runtime.
- Source inventory is SHA256 exact, not SimHash or fuzzy heuristic.
- JDK`java.util`, Guava, Apache Commons, Eclipse Collections and Spring
  are downstream API/behavior comparison gates; this receipt does not prove
  these ecosystems are drop-in compatible.
- LeetCode/HackerRank/GeeksForGeeks are algorithm problem-category/test
  discovery sources, **no pasted challenge code**.
- Original first-party Apache-2.0 and JUnit/OpenRewrite upstream licenses
  remain theirs; no new donor code incorporated.

## Acceptance honesty
This repairs two high-impact verification blockers in an isolated
collection module. It cannot certify the entire M3JDK21 source-built JDK,
full 24-family/96-shape/five-library superset, all JNI platforms, GC/safepoints,
serialization, multi-thread linearizability, JMH superiority or all MIndex to
M3 renames. Keep the PR DRAFT until exact-head Maven SDK, source inventory,
module and native gates run green.
