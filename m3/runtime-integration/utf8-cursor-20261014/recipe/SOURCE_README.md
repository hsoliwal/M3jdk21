<!-- SPDX-License-Identifier: Apache-2.0 -->
# Exact UTF8 receiving SDK qualification

DRAFT. This Maven qualification module reuses the existing UTF8 cursor recipe,
unchanged five-test SDK class b735eb59bf2b7f6167674d2bf8a345679e55d49f and exact
42399d41-to35ed9985 fixtures. It changes no Java/JNI runtime source and does not
add a phase-chain transition. Source11448 and target898 are already merged;
their UTF8 qualification HOLD remains intact.

The provider is the existing public M3JDK vendored Java-snapshot owner
2530c533003a8a17d0e782728d98a4dfebe13651, with exact public M3EditScope27436081
and M3ScopeInferencef6fd8d4a support. These three files are frozen unchanged.
The canonical current39c3a6c9 provider is different. This module qualifies
the exact receiving2530 owner only; it cannot admit canonical39c, matched-JDK
execution or phase advancement. Do not substitute the public counts d58 provider:
it lacks APIs and the strict Java-LST refusal expected by the unchanged test.

The existing public wrappers Maven POM is adapted to this finite source closure.
Dependencies remain public OpenRewrite8.17.1 with Java21 parser/runtime and
JUnit5.10.2. The canonical project source-fork dependency is not substituted in
its original module. Official v8.17.1 JavaParser3f8dc7b0 exposes the dependsOn and
classpath overloads used here; DelegatingExecutionContext9eaa566c exists there.
Actual Java compilation and SDK execution remain NOT_RUN at authoring.

Run from an admitted environment:
python3 -I -S -B sdk/verify.py
mvn -B -ntp -f sdk/pom.xml test
python3 -I -S -B sdk/verify.py --reports

The five original tests cover exact preimage/target scope; real scheduler
application and fixed point; missing/duplicate/drift refusal; rejection of
identical PlainText; and unrelated Java source preservation. They must execute
unchanged. The receipt checker requires exact testcase names/class, five tests,
zero failures/errors/skips and sealed provider/test/template bytes. A missing,
skipped, renamed or partial test suite is a refusal. YAML descriptor activation
is recorded separately as NOT_RUN: these existing tests instantiate the Java
provider directly.

A related receiving mirror may live at
m3/runtime-integration/utf8-cursor-20261014/recipe/canonical in M3jdk21. It must
equal this sdk Git subtree exactly, carry a source-commit/tree binding outside
the core, and validate existing public provider blobs, live35ed target and
immutable UTF8 HOLD before and after Maven. No private engine, unrelated source,
credentials or datasets are required. The only canonical-source test transferred
is the directly relevant existing UTF8 SDK test; GPL fixtures are the already
public JDK source. No new execution host, quota purchase or restriction workaround
is introduced. Extend the destination's existing qualification job only.

Keep source and receiving PRs draft until exact-candidate tests and required
review execute. If the module fails, repair the qualification recipe and rerun;
never weaken the original five tests or switch to PlainText to obtain a pass.
A receiving2530 SDK pass leaves canonical39c, full reactor, real M3 owners,
matched JDK/jtreg, JNI lifetime/GC/CDS/JIT, retention and performance unqualified.
Whole MIndex/MatIndex/SubMIndex migration and continual optimization remain ACTIVE.

## Candidate records

FOSS_REUSE_DECISION.tsv and CATEGORY_REVIEW.tsv record donor choice and rejection.
sdk/pins.json seals the finite core. Its verify.py requires actual Surefire
evidence; a parser/model/hash check is not SDK execution. Source-advisory review
and the CPU report-checker test, if present, have independent limited scopes.
Original name mappings, source chronology, historical receipts and UTF8 HOLD
are unchanged. The receiving mirror is a qualification tool, not a competing
recipe owner or a runtime promotion.

Maven validate also runs26 bounded filesystem input-closure mutants: nested or
foreign Java sources, shadowing test resources, optional unsealed parser context,
each omitted required pin, altered critical identities and a rewritten test/pin
pair. These new whole-checker cases are NOT_RUN at authoring. Both actual input
validation and report custody use single-read bytes; five SDK tests stay exact.
