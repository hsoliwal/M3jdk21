<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Current NLP receiver source closure

This canonical task fixes two exact receiving files so the existing Java 21
Lucene/OpenNLP companion build can verify the current M3 source tree. Its actual
OpenRewrite SDK, guarded materialization, and complete receiving source/JMOD/image
gate pass. The receiving image contains 62 pack modules plus 30 JDK modules.
`QUALIFICATION_SUMMARY.json` is the receipt index; each earlier failure remains
under its original immutable attempt directory.

## Exact changes

| Receiving owner | Change | Preserved contract |
| --- | --- | --- |
| `m3/core/src/com/m3/text/SharedLexiconPrecomputeCatalog.java` | Declare the existing implicit `public Builder()` explicitly. | All nine public class descriptors and the complete Builder executable disassembly are identical. The seven field initializers remain in the same constructor order. Debug line-table identity is not claimed. |
| `m3/tooling/module-packs/nlp-search/current-source-lock.json` | Update the GVAS and Builder-owner hashes; add explicit entries for the already received BigString and RopeTextIndex sources. | All other metadata, source roots, schema and guards remain unchanged. The closed manifest has 145 entries, including the exact 27-file text module closure. |

The initial source-lock refusal was correct: its stored GVAS hash was stale and
two received files were absent from the manifest. The successor follows exact
receiving commits and fresh owning gates. It does not accept arbitrary current
source. The unchanged verifier still refuses a changed owner, a missing owner,
and an unlisted additional Java source.

The original whole-module rope gate separately refused an exported-constructor
lint warning. The declaration above repairs that source condition while keeping
the original strict flags and whole-module script. A narrower diagnostic result
is retained separately and is not substituted for the original authority.

## Qualification

| Authority | Actual result |
| --- | --- |
| New canonical SDK | One aggregate JUnit test, zero failures/errors/skips; two phases and two exact outputs, attributed to the actual `SharedLexiconPrecomputeCatalog#builder` owner. All 21 source inputs and parser dependencies remain unchanged. |
| Isolated proof-checked copier | Two writes, zero replay writes; product drift, symbolic destination, emitted-output drift, and canonical-owner drift all refuse. |
| Actual receiving application | Two exact SDK-generated files applied; replay writes zero; complete source-seal readback passes. |
| Existing GVAS Java/JNI gate | Three cooperating JVMs, 17,530 checks per child, one verified mapping address/inode. Initial sandbox visibility failure retained. |
| Existing BigString owning workflow | 3,213 checks on the exact received source and tests. |
| Existing whole-module rope gate | 83,941 checks in each of four original JVM modes after the explicit constructor declaration. |
| Existing catalogue test | 50 checks before and 50 after; nine public descriptors and complete Builder disassembly match. |
| Actual complete receiving NLP/search gate | 62 pack modules; all three installed-image probes pass. All 62 module JAR hashes equal the isolated candidate, and 11,373 input readbacks are unchanged. |
| Separate correction-memory helper | 129 conformance checks, including 32 control-character cases, plus five linked evidence-integrity records. This is not an additional SDK or runtime suite. |

The 62 pack modules are 36 built from source and 26 from locked external
dependencies. They comprise 29 Lucene, four OpenNLP and three M3 source-built
modules. The original build reports 3,626 Lucene, 1,000 OpenNLP and 142 M3
production Java sources. External archives are cached, hash-locked inputs;
this proof does not call them source-built.

The receiving probes exercise 2,000 raw UTF-16 digest cases, 2,000 AST span
cases, 300 Unicode token cases, real token/sentence model training and
serialization/reload, immutable source offsets, probability parity, and disk
indexing/query/term-vector offsets. This binds the existing M3File/M3String/M3AST
sharing in the current companion lane. It does not establish JDT semantic-binding
search, every OpenNLP model, or the complete upstream test suites.

## Source custody and historical disagreement

The source basis is M3JDK21 `2779aba3f98f31c324be43f731be7914e1a05935`.
Actual application onto the receiving follow-up is recorded separately under
`proof/receiving-application-001/`; its source checks bind the same two before
and after images regardless of unrelated receiving changes.

The unchanged OpenNLP companion checkout is
`8b647d1d389736952e43fb46865c874747f7acde`; Lucene is
`873b6167603c69101d626ddffd8d733340300fea`. Their original source-lock donor
bases remain distinct. This is the Java 21 staged lane; original Java 25 fork
qualification remains a separate contract.

The immutable older BigString canonical head has templates that add public
streaming APIs and differ from its own manifest and the received files. Full
diffs and hashes are retained under `proof/owner-candidate-001/`. This task
qualifies the exact received contracts; it neither imports those extra methods
nor claims the conflicting historical recipe can regenerate the received files.
See [the exact owner comparison](proof/owner-candidate-001/OWNER_CONTRACTS.md)
and `LINEAGE_REVIEW.json` beside it.

## Replay

The existing four snapshot engines are reused unchanged at canonical source pin
`e02e7cb0f2e8e46db4c5b476d59aa26658fa4d07`. `SOURCE_SEALS.json` and
`phases.tsv` select one Java and one text phase. Original source preparation is
custody only; actual SDK output is the authority for receiving writes.

Use Java 21, Maven 3.9.9 and a one-path-per-line file naming actual compiled M3
parser dependencies. Exact commands, source inventory and dependency hashes are
retained in `proof/sdk-001-offline/` and `proof/isolated-sdk-custody-001/`.

```sh
python3 -B qualify.py --java-home "$JAVA_HOME" --maven "$MAVEN" \
  --maven-repo "$MAVEN_REPO" --parser-classpath "$ACTUAL_CLASSPATH_FILE" \
  --attempt sdk-local --timeout-seconds 300
python3 -B apply_receiver.py "$M3_RECEIVER"
python3 -B apply_receiver.py "$M3_RECEIVER" --apply
python3 -B verify_sources.py "$M3_RECEIVER"
```

The unchanged complete `current_forks.py` invocation, exact donor sources,
dependency directories and installed-image probes are retained in
`proof/receiver-runtime-001/combined-receiver-001-receipts/`. Baseline hydration
is separately labeled unchanged source custody, not a transformation.

## Boundaries

The original image warnings about optional `jdk.management` readability and
Lucene's foreign Linker native access are retained; launch flags are unchanged.
No optimization, object-size, benchmark or native-platform claim follows.
Original Java 25/upstream suites, optional Faiss/native providers, arbitrary
models, full String/HotSpot/JDK-family promotion, platforms and exact hosted
admission remain separate gates. The broader hosted migration-engine pin
failure is retained in the companion current-owner evidence stage.

No prior core, SWT or Maven4 packet is rewritten. The new task's memory is
closed to `RECORDED/PENDING`; [its retrieval entry](memory/LESSONS.md) links
actual failures and corrections without inheriting prior task events.
`provenance/SOURCE_AND_RIGHTS.json` retains source/license boundaries. This is
existing-owner maintenance and makes no patent status or priority claim.
