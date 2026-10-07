# Recover qualified Synexia v7 AST / regex / precompute intake

Status: additive no-loss recovery on current M3JDK21 master.

## Original reviewed packet

Source PR: M3JDK21 #332
Head: `2b357de895d2db9fd436315a55d3a4850ad272a4`

The PR was closed without merge after its qualified source-custody work had been completed. Current
master contains none of its 174 added intake/vendor/evidence files.

This recovery replays the exact reviewed Git blobs from #332. It does not regenerate or
memory-reimplement AST/precompute source.

## Recovered payload

- 22 admitted regex/precompute source payloads;
- 42 AST payloads (37 production sources and 5 original/oracle sources);
- exact source-custody, dependency-frontier, license and proof receipts;
- exact importer tests/evidence and fixed-point/refusal receipts;
- 174 added files total.

The three importer owners modified by #332 are also restored to their exact reviewed postimages.
Their delta against the current pre-fix master is limited to:

- qualification of the static `SynexiaImportPlan.root(...)` helper call;
- two corrected test fixtures;
- one missing-source test setup line.

No importer public API, assertion policy or failure-order contract changes.

## Ownership and license

Canonical generic source remains in `hsoliwal/com.synexia`.

Qualified first-party source retains:
- Apache-2.0;
- Copyright 2026 Hitesh Soliwal and contributors;
- original package/path identity under `m3/vendor/synexia`.

OpenJDK and third-party source are not relicensed.

## Authority

Original full-family review/provenance:
- Synexia PR #9648
- head `b24262a92b741c78c44ca61afc8dac7ee1376d8a`

Current-tree authority recovery:
- Synexia PR #9677
- exact reviewed authority/catalogue blobs only.

## Limits

This recovery restores qualified source custody and its proof packet.

It does not claim:
- whole-repository completion;
- source-complete consumer wiring;
- java.base/String/JNI product integration;
- full dependency closure;
- native/platform/performance acceptance.

Those remain separate target promotion gates.
