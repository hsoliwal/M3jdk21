# M3JDK21 backport programme status

Status: read-only deterministic fan-in over existing backport authorities.

## Purpose

The JEP catalogue decides compatibility disposition. The recipe DAG decides the small atom sequence.
The execution-wave compiler decides which atoms can run now without widening scope.

`backport_programme_status.py` joins those three authorities into one row per active,
non-superseded JEP. It does not edit OpenJDK source and does not promote a backport.

## Programme states

- `AUTHOR_RECIPE`: candidate has no checked-in recipe packet yet.
- `FILE_READY`: every **exactly matched** atom has a READY wave, and every atom is FILE scope.
- `SCOPE_READY`: every exactly matched atom has a READY wave, but at least one atom has explicitly
  approved non-FILE scope. This is **not** Java-21 compatibility or runtime proof.
- `SCOPE_HOLD`: MODULE/MULTI_MODULE work or a dependent atom requires explicit scope proof.
- `EVIDENCE_HOLD`: DAG/recipe/compatibility evidence is not executable yet, or atom/wave
  evidence is truncated, duplicated, stale, cross-scope, unknown-state, or marked READY for
  a non-executable atom.
- `HOLD`: catalogue policy explicitly holds the JEP.
- `REJECT`: catalogue policy rejects the JEP for the Java-21 compatibility baseline.

Superseded preview/lineage rows are omitted from the active programme output and remain visible in
`JEP_CATALOGUE.tsv`.

## Authority

The status ledger grants no:

- source mutation authority;
- compatibility authority;
- scope-promotion authority;
- merge/promotion authority.

A row is a deterministic next-action receipt only. Compiler, jtreg/JUnit, runtime/JNI/VM proof and
fixed point remain required before promotion.

## Execution

```bash
python3 m3/backports/backport_programme_status.py \
  --root . \
  --tsv-out m3/backports/target/backport-programme-status.tsv \
  --json-out m3/backports/target/backport-programme-status.json
```

The output is suitable for review dashboards and for Camel/Airflow/Drools scheduling decisions
because it is derived from the already canonical recipe/wave graph rather than a second DAG.

## Strict DAG → wave identity gate (2026-10-15)

A programme row is never READY from a partial list or a count alone. The compiler now matches the
canonical atom IDs to scheduled wave IDs as exact sets and rejects missing, duplicate, stale, or
unexpected projections. Each wave must retain its originating scope; a READY wave cannot correspond
to a non-executable atom. Unknown wave states and mismatched counts remain
`EVIDENCE_HOLD / REPAIR_BACKPORT_DAG_WAVE_ALIGNMENT`, not `FILE_READY`.

Only an all-READY, all-FILE wave set becomes `FILE_READY`. An all-READY wave set with a non-FILE
atom becomes `SCOPE_READY`; this requires the scheduler's explicit scope approval but **does not**
prove the target JDK image, recipe JUnit, jtreg, JNI/runtime parity, or fixed point.

`mvn -B -ntp -f m3/backports/pom.xml verify` now emits the scoped programme evidence into
`target/backport-programme-status.tsv` and `target/backport-programme-status.json`.
Both outputs are read-only receipts. The older `program-status.json` aggregate remains intact.


## Current Nebula -> Synexia -> M3JDK21 admission (2026-10-15)

The backport programme itself remains active: catalogue classification, packet authoring, recipe
preparation, dependency closure and target-local proof work continue normally.

The **current Nebula-qualified transfer edge** is separately HOLD-bound by:

`NEBULA_PROOF_GATED_ADMISSION_20261015.tsv`

It pins:

- Nebula PR #212 / commit `9a77803ce174be534ba5f543a9f3b342858d2bbc`;
- Synexia custody PR #11632 / commit `cf21063f2620ca12202122643bf7d4120ed16194`;
- canonical owners `M3NebulaRecipeDag` and `M3NebulaViewportDistillationRecipe`.

Current states:

- Nebula: `HOLD_UNTIL_PROVEN` because hosted proof workflows have not executed green;
- Synexia: `DRAFT_UNMERGED`;
- M3JDK21 packet execution from this transfer: `false`;
- mutation authority: `false`;
- promotion authority: `false`.

This does **not** stop independent JEP/JDK backport inventory or recipe development already admitted
by the existing catalogue. It prevents only the new Nebula-qualified recipe handoff from silently
becoming executable before its two prerequisite proof stages are complete.

## 2026-10-16 pinned upstream Nebula proof gate

The backport programme now carries an additive V2 admission receipt for the real pinned
`EclipseNebula/nebula` proving checkout:

- upstream revision: `5d0bcbb9c6e3a118685a5ec91c6f5d8eb7833893`;
- Synexia proof PR: `hsoliwal/com.synexia#11749`;
- current Synexia proof commit: `b3b35e5fd706a6584df4f476b38e8255f8692ee3`;
- proof workflow: `M3 Nebula Recipe DAG Convergence`;
- physical private-atom extraction: `HOLD_TRUSTED_VERIFIER_REQUIRED`;
- backport packet execution: false;
- mutation authority: false;
- promotion authority: false.

The machine receipt is
`NEBULA_UPSTREAM_PROOF_ADMISSION_20261016.tsv` and is verified by
`nebula_upstream_proof_admission.py` during the Maven `validate` phase.

The older V1/historical Nebula receipts remain intact. V2 does not reinterpret them and does not
authorize any JDK source mutation. The programme remains HOLD until Synexia #11749 is green and its
post-Tycho `M3_NEBULA_UPSTREAM_HANDOFF_V1` artifact is available for exact receiver binding.

