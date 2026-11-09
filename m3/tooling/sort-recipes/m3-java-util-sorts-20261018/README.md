<!-- SPDX-License-Identifier: Apache-2.0; Copyright 2026 Hitesh Soliwal and contributors -->
# M3 java.util sorting recipe — 2026-10-18

This is the canonical Synexia task crate for the additive M3JDK21 sorting candidate.
Its class-backed recipe composes the existing hash-pinned Java and text engines.
M3JDK21 owns the resulting public JDK APIs, implementation, build and runtime gates.
The current ordered receiving stage is not advanced by this crate or its tests.

## Exact owners and sources

| Input | Identity |
| --- | --- |
| Target repository | hsoliwal/M3jdk21 |
| Target source base | 2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49 |
| Canonical engine source | hsoliwal/com.synexia@140c263a90f9803b9c46103c63355a286e0515f0 |
| New named recipe | com.synexia.m3.JavaUtilSorts20261018 |
| New Java composition owner | com.synexia.rewrite.sorts.M3JavaUtilSortsRecipe |
| Bounded Maven coordinate | com.synexia.recipes:m3-java-util-sorts-20261018:1.0.0 |
| Authoring baseline | Java 21; Maven 3.9.11; OpenRewrite 8.90.4; JUnit 5.10.2 |

The engine-sources.tsv provenance table binds four byte-for-byte copies to their
original canonical paths, Git blobs, SHA-256 hashes and Apache-2.0 licence. They
are an independently qualified authoring closure, not new engine owners. The
original M3HashPinnedJavaSnapshotRecipe, M3HashPinnedTextSnapshotRecipe,
M3ScopeInference and M3EditScope remain authoritative. Their source is unchanged.

Extend the existing canonical ownership manifest with the exact row in
provenance/CANONICAL_RECIPE_HOME.append.tsv. The target may retain a pinned,
noncanonical proof-tooling mirror with the exact published Synexia commit and
content root. The JDK runtime does not depend on Synexia, Maven or OpenRewrite.

Policies consulted include AGENTS.md, the M3 Recipe Mastery skill,
M3_RECIPE_FIRST_MASTERY_INVARIANT.md, CANONICAL_RECIPE_HOME.tsv,
SYNEXIA-M3JDK21-CANONICAL-OWNERSHIP-1.md and M3JDK21_DONOR_BOUNDARY.md.
Target naming and policy stay in M3JDK21's existing porting invariant and naming
registry. This bounded crate does not claim repository-wide inventory closure.

## Sealed payload format

Both payload manifests contain lexically sorted, four-column TSV rows:

~~~text
target/path<TAB>ABSENT<TAB>postimage-SHA256<TAB>template-basename
~~~

All targets are additive. ABSENT is an explicit admission: a missing file may be
created; a present file must already have the exact sealed postimage. Foreign
occupants, source drift, duplicate paths and malformed templates refuse.

Java production and jtreg sources use payload/java/*.java.txt. Maven packages them
under /com/synexia/rewrite/hash-pinned-java/m3-java-util-sorts-20261018/.
Java payloads must become actual, byte-exact OpenRewrite Java compilation units.
Markdown, TSV, JSON, Python and CI YAML use payload/text and the equivalent
hash-pinned-text resource root. Code-looking documentation and strings remain
data. Both lanes use their original canonical manifest validators.

The composition admits this exact source set as a bounded package/API candidate.
Within each file the existing engine prepares and emits one reviewed postimage
serially. It does not change pre-existing JDK owners or advance other M3 families.
Algorithm contracts, complexity, progress, cancellation, stability and target
coverage are defined in the target payload and catalogue. Source identity alone
does not prove those behaviors.

## Maven qualification and application

Use a Java 21 JDK and run:

~~~sh
mvn -B -ntp verify
~~~

The local offline qualification used the pre-existing dependency cache:

~~~sh
mvn -B -ntp -o -Dmaven.repo.local=/tmp/m3-maven-repository verify
~~~

The Maven suite runs the real Recipe.run lifecycle. The thin filesystem receiver
writes only those actual OpenRewrite results. It validates both manifests and raw
UTF-8 input byte identities, prepares every output, verifies the exact output set
and a complete second-run source fixed point, then creates new files serially with
CREATE_NEW. It checks all observed input identities again before writing and reads
back every output.

Use an isolated checkout with no concurrent writers. No existing file is replaced
or deleted. An interrupted additive write can be replayed from any exact subset
of the sealed postimages.

Set JDK_CHECKOUT to the target checkout and EVIDENCE_DIR to a writable directory
**outside** that checkout:

~~~sh
mvn -B -ntp compile exec:java \
  -DtargetRoot="$JDK_CHECKOUT" -DapplyMode=dry-run \
  -DreceiptPath="$EVIDENCE_DIR/sorting-dry-run.tsv"

mvn -B -ntp compile exec:java \
  -DtargetRoot="$JDK_CHECKOUT" -DapplyMode=apply \
  -DreceiptPath="$EVIDENCE_DIR/sorting-apply.tsv"

mvn -B -ntp compile exec:java \
  -DtargetRoot="$JDK_CHECKOUT" -DapplyMode=check \
  -DreceiptPath="$EVIDENCE_DIR/sorting-check.tsv"
~~~

dry-run and apply require the exact original target HEAD. check is read-only with
respect to the target and works on the published PR HEAD: every sealed postimage
must already exist, and the recipe must make zero changes. Its receipt records
the checked Git commit, original target base, engine source base, both manifest
hashes, Java runtime, observed preimages and deterministic target path/hash root.
SOURCE_APPLICATION_ONLY is the scope of that receipt; it does not claim JDK runtime
qualification or advance the ordered receiving stage.

The buildRoot and payloadRoot properties may locate bounded local build inputs.
receiptPath locates the external application receipt. Defaults stay relative to
this self-contained crate.

## What the tests establish

The frozen fixtures exercise absent-only creation through actual Recipe.run,
all eight subsets of three additive sources in both input orders, repeated serial
output, one- and two-worker trials, and disjoint Java/text composition orders.
Every trial produces the same exact target root and a no-change second run.

A real Java 21 compiler validates a sealed before/after replacement and compares
the public method surface, arithmetic overflow, empty input and explicit null
exception contract. Compiled fixtures include records, sealed types, lambdas,
anonymous classes, switch expressions and try-with-resources. Code-looking
comments, regex strings and text blocks remain byte exact.

Permanent refusal resources cover template hash drift, malformed Java and
noncanonical manifest ordering. Tests also exercise a foreign existing owner,
non-Java source admission, duplicate targets, missing required preimages and
interleaved changes between scan and visit. Text fixtures exercise independent
hash refusal and opaque documentation data. These are bounded adversarial cases.

The actual packet tests activate the named recipe, create every sealed file,
validate exact bytes, prove fixed point and partial-subset completion, test
dry-run and source-drift refusal before writes, reject symlink parents, and verify
the copied engine Git blobs. A temporary Git commit proves that read-only check
accepts a published head while apply and dry-run retain the original base rule.

## TimSort reuse and licensing

The specialized TimSort comes from the target's existing OpenJDK source:

- Path: src/java.base/share/classes/java/util/TimSort.java.
- Git blob: 7cce1cc450cb2894506f6f56140f4bdc938b703b.
- SHA-256: f147bfc6470b811546384b17b7fd3ed278586db806f445d66151a34f617c0d19.

provenance/donors/TimSort.java.txt retains that exact source and original notices.
Adjacent OpenJDK licence files preserve its terms. The parameterized
provenance/specialize_timsort.py records the hash-guarded candidate derivation.
By default it only checks a reviewed .java.txt candidate; --write-new can create
a fresh candidate file, never a .java product source. The canonical class-backed
OpenRewrite recipe remains the production materialization owner.

~~~sh
python3 provenance/specialize_timsort.py \
  --donor provenance/donors/TimSort.java.txt \
  --candidate payload/java/M3TimSort.java.txt
~~~

The exact derivation receipt is provenance/TimSort-derivation.tsv. Target compilation
and differential sorting tests remain the behavioral authority.

The recipe, orchestration, fixtures and original documentation are Apache-2.0,
copyright 2026 Hitesh Soliwal and contributors. LICENSE contains that grant;
provenance/SYNEXIA-NOTICE.txt retains the original notice. Each runtime payload
keeps its own OpenJDK/first-party/third-party licence and copyright. An Apache-2.0
recipe does not relicense generated source or donors. Algorithm ideas are recorded
as provenance; no copyright is asserted over abstract sorting methods.

## Acceptance boundary

QUALIFICATION.md records executed local gates and exact source identities.
The full Synexia reactor, rebuilt M3JDK21 image, platform jtreg/native checks,
published GitHub CI and performance measurements need their own executed receipts.
They must not be inferred from this finite recipe suite, copied source packet,
draft PR or merge ancestry.
