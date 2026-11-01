<!-- SPDX-License-Identifier: Apache-2.0 -->
# Synexia callback proof intake: pending SDK application

Status: DOCUMENTED_NOT_INSTALLED. This target packet contains only the receiving
binding and fixture evidence. It does not contain the export manifest, Java
kernel, JNI library, copied recipe implementation or a JDK runtime change.

Canonical recipe: `com.synexia.m3.M3Jdk21CallbackBootstrap`, owned by Synexia
PR #9526 at `7c4a949507f55718245188e60d7940dbe772e80c`, directory:
`synexia-openrewrite-recipes/tools/patternizer/callback-parity/receivers/m3jdk21/`.
The source receiver tree is `cd66894c1616149d8539d39106a10c523f1d7aeb`.

The concrete receiver is the existing `m3/synexia-import` module:
`com.m3.synexia.importer.SynexiaImportManifest` and `SynexiaImporter`.
The authored bootstrap creates `m3/synexia-import/intakes/callback-parity/export.tsv`
for that importer. Its five-file intake is confined to
`m3/vendor/synexia/callback-parity/` as a pinned authoring/proof image.
The original Synexia replacement recipe and its preimage seal are unchanged.

This is an explicit DO_NOT_PORT_TO_JDK_RUNTIME disposition for this intake.
It is not a mapping into java.base, HotSpot, String or a JDK runtime dependency.
The intended proof image retains the original Java/JNI names solely to run the
existing donor oracle and two proof drivers. Any later JDK consumer requires a
separate target-owned adaptation and canonical naming-map review.

The target importer sources at `7fd4c0bdec006691ce0e8f2408c8963f191cd2e5` were
compiled unchanged and exercised in an isolated receiving fixture. All five
files matched, 24 refusal cases passed, and importer replay changed zero files.
Java/JNI callback and lifecycle proofs passed on the files actually imported.
This was a system-Java-21 fixture exercise, not an M3JDK21 image build or run.

Read the source-owned RUNBOOK.md for reproduction and the exact SDK sequence.
Eight bootstrap SDK tests are authored but remain unexecuted: Maven could not
launch in the recorded environment. Do not hand-create export.tsv or copy the
native template to bypass those tests. Genuine successful SDK Results, receiving
source checks and target-native/parent gates are still required before import.
Existing m3/LICENSE, m3/NOTICE, runtime owners and default-branch history remain
unchanged. Reusable recipe improvements continue to converge into Synexia.
