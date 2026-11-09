# Existing NLP and search ownership at the receiving source pin

Read-only inventory of `hsoliwal/M3jdk21@2779aba3f98f31c324be43f731be7914e1a05935`.
This audit made no product changes. `RESULT.json` binds the inspected source
hashes and the two executed existing Python fixture gates.

## The existing lanes have different contracts

| Lane and source authority | Existing capability | Qualification still required for this task |
| --- | --- | --- |
| Parent `m3/tooling/module-packs/nlp-search/build.py`, `README.md` | The independent vendored Lucene 9.10.0 / OpenNLP 2.4.0 source lane, M3 source integration and its own build/probes. | This recovery task did not rebuild or run that image. Its checked-in files are not fresh runtime evidence. |
| Parent `current_forks.py`, `CURRENT_FORKS.md`, `CURRENT_FORK_REFS.json` | A separate sealed Java 21 staging lane for current fork sources. The contract preserves 29 Lucene production module identities, aggregates the 923-source OpenNLP CPU closure, and keeps optional morphology/spellcheck/UIMA modules separate. It uses existing M3 text/File/AST owners and the `jdk.m3.nlp.search` source adapter. | Fresh exact donor checkout, canonical staging recipe, complete dependencies and source compilation, fixed point/refusals, JMOD/image build and all required runtime probes. Java 21 foreign-memory preview and the pinned expression compiler backport are explicit requirements. This audit did not execute them. |
| Nested `current-forks/build.py`, `current-forks/README.md`, `current-forks/SOURCE_REF.json` | Accepts sealed **original** Gradle/Maven outputs. Current Lucene source/classfiles in this lane require Java 25. It keeps the independent compile → test → runtime receipt sequence and supports genuine named-module dependency closure. | Real JDK 25 plus JDK 21 for its release fixtures; original fork builds and source/test receipts; genuine external archives, license/NOTICE custody, descriptor recipe, complete module/runtime gates. A Java 21 source-level adapter does not make Java 25 dependency classes executable on Java 21. |
| `frontend/.../M3SourceIndex.java`, `m3-bridge/README.md` | Existing consumers of the real javac M3AST PARSED event, Lucene IndexWriter and OpenNLP tokenizer owners. Event-local projection avoids keeping compiler trees/tasks and avoids indexing each top-level type repeatedly. | A compatible supplied M3 compiler implementation, fresh callback/abort/lifetime probes and persisted-index runtime checks. Stock Java 21 plus an interface stub would not establish this behavior. |

The Java 21 staging companion implementation pins recorded in the parent receiver
are Lucene `873b6167603c69101d626ddffd8d733340300fea` and OpenNLP
`8b647d1d389736952e43fb46865c874747f7acde`. Their original input commits are
`d1b7235f1ba1c1eea5b04a5b7e6554595a90719e` and
`368337311ad94b665fd178f8d3ced005a73b2370`. The nested original-output lane requires
its own clean successor commits and exact archive locks; those inputs must not be
silently substituted with the staging lane's implementation or receipts.

## What the source integration already does

`SourceDocument`, `SourceSpans` and `LuceneSourceFields` already retain stable
source owners, use primitive half-open UTF-16 range arrays, provide real simple
and model tokenizer/sentence APIs, preserve AST occurrence identities, and create
real Lucene position/offset/term-vector fields. Source-character digests, original
file-byte digests, AST fingerprints, Lucene positions and UTF-8 terms remain
separate domains. The caller owns the analyzer, writer and transaction policy.

The existing `M3SourceIndex` frontend updates by exact URI term, stores the source
UTF-16 digest and bounded package/top-level-type metadata, and exposes analyzed
text queries and case-sensitive OpenNLP token phrases. It receives a checkpoint
for progress/cancellation. It does not retain a compiler task or duplicate the
entire source in a stored text field.

The nested bridge delegates source bytes to `SourceArtifact.Span`, text to the
existing immutable `String`/`CharSequence` and fork text readers, and parsing to
the actual compiler M3AST. Its historical "no tracked M3File" statement describes
that lane's inventoried base and partial receiving status. It must not be used to
deny the separate parent's explicit restored M3File closure or to create a new
competing File/AST/String owner.

## Gaps that the inspected code does not close

- URI-keyed `updateDocument` is present. Cross-event unchanged-source suppression,
  resource-delta batching, deletion/move policy and a durable incremental index
  transaction are not established by these adapters. An integration should reuse
  the existing resource snapshot and source seals, with explicit invalidation
  when analyzer, classpath, processor or toolchain identity changes.
- The PARSED projection and name metadata are lexical/source facts. The inspected
  adapters do not provide exact declaration/reference/override binding search or
  establish JDT semantic-index replacement. A text digest alone is not a semantic
  cache identity.
- A tokenizer or Lucene analyzed field does not establish arbitrary raw-source
  regex equivalence. Exact regex matches require a separate final check against
  the original source and its coordinates if an indexed candidate filter is used.
- Models, native Faiss/ONNX/GPU backends, additional model discovery and platform
  dependencies require separate admitted inputs and executed gates. Python archive
  tests do not install or qualify any of them.
- SWT/Nebula workspace UI, Eclipse search-view integration and broader OSGi/PDE
  lifecycles are outside this inspected pack and are not qualified here.

## Fresh tests actually executed

| Existing gate | Observed result | Scope |
| --- | --- | --- |
| `tests/test_current_forks.py -v` | 5 tests passed | Notice ownership, equal shared modules, conflicting implementation/notice refusal, unsafe legal path refusal. Fixture ZIPs are not real compiled modules. |
| `current-forks/test_opennlp_pack.py -v` | 24 tests passed | Archive/resource/service/rights/path refusals and exact source checkout/provenance behavior. The tests explicitly use arbitrary fixture payloads and do not claim NLP runtime proof. |
| `current-forks/test_zlibsvm_pack.py` | Not run | Requires five exact genuine external archive/POM/license inputs, not supplied for this audit. |
| `current-forks/test_jmod_pack.py` | Not run | Requires real Java 21 and Java 25 toolchains and its ordered prepared fixtures; only the task's Java 21 toolchain was provided. |

All inspected source hashes were unchanged after the two unit gates. No full
fork build, current JMOD image, M3 compiler runtime, whole JDK or UI qualification
is inferred from these 29 passing fixture tests.
