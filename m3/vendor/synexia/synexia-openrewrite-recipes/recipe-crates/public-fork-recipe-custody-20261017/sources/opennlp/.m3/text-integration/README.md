# OpenNLP M3 text receiver

Draft candidate: source base `4713d82577dd3e9e48cd9855ccd891c2739324a4`.
Canonical recipe/test owner: [Synexia PR 11438](https://github.com/hsoliwal/com.synexia/pull/11438). Runtime postimage SHA-256: `81893c7ab931646e364390006b5a57733ec8e0e9386c6ecaa4145cee1c086a6b`.

## Implemented

The built-in feature list starts at capacity18. Fixed category spellings reuse literal backing with `new String(known)`, preserving fresh String wrapper identities; custom/null keys retain concatenation. Unicode classification, whitespace delegation, feature order/content, abbreviation behavior and public/protected hooks stay unchanged. No new text pool or model cache is introduced.

## Recipe and source custody

`source-packet/` is a **generated immutable source handoff**, not a separately maintained downstream recipe. Every source is pinned by origin commit/path/SHA-256 in source-packet.tsv and checked before compilation or execution. Canonical source stays in Synexia. The receiving source-copy exception is explicit for review; fix canonical source/templates, regenerate and rerun rather than editing these snapshots or generated runtime output. Preimage/postimage fixtures and proof receipts remain alongside the changes.

The original anonymous-download receiver failed in GitHub Actions run38593868305 because com.synexia is private. It was replaced using the canonical `public-handoff/manifest.tsv` recipe and HandoffQualification test at Synexia commit `b392d2e2ce03997f01231cb7208a887c73d77731`. No credentials or access-policy changes are needed. This supersedes earlier download/offline-checkout instructions and the initial receipt's delivery mode; production source hashes are unchanged.

```sh
mvn -B -ntp -f .m3/text-integration/pom.xml verify
# Exact admitted preimage only, exclusive checkout:
mvn -B -ntp -f .m3/text-integration/pom.xml -Dm3.mode=apply verify
# Search the frozen task catalogue:
grep -i 'prefix' .m3/text-integration/source-packet/catalogue.jsonl
```

## Evidence and remaining gates

Both original and revised receivers passed locally: **1,052,956 checks**, covering source/template drift, fixed point, compiler-extracted context methods and actual classification branches over7keys x2injected whitespace outcomes xallUTF-16units. Fresh wrapper identity is tested. Context hooks use a documented test double; this is not a full model/tokenizer suite.

Maven is absent locally; direct scripts were used. CI now requests the same gate through Maven, and its actual results must be inspected. Full product reactor/lint/JUnit/model tests, patched OpenRewrite/canonical admission, M3JDK runtime and representative performance qualification remain separate. No end-to-end speedup is asserted.

Not implemented: token-local hierarchical feature preparation, frozen-model feature IDs, shared sentence ownership, interner-policy changes, JNI model evaluation or whole-repository coverage. Donor/category decisions are recorded in the15-entry catalogue. No editorial/submission code or GPL OpenJDK source was copied.
