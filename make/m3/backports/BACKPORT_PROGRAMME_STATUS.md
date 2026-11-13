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
