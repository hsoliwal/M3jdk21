# M3JDK21 backport programme status

Status: read-only deterministic fan-in over existing backport authorities.

## Purpose

The JEP catalogue decides compatibility disposition. The recipe DAG decides the small atom sequence.
The execution-wave compiler decides which atoms can run now without widening scope.

`backport_programme_status.py` joins those three authorities into one row per active,
non-superseded JEP. It does not edit OpenJDK source and does not promote a backport.

## Programme states

- `AUTHOR_RECIPE`: candidate has no checked-in recipe packet yet.
- `FILE_READY`: every currently emitted atom is READY under the no-extra-approval scheduler.
- `SCOPE_HOLD`: MODULE/MULTI_MODULE work or a dependent atom requires explicit scope proof.
- `EVIDENCE_HOLD`: DAG/recipe/compatibility evidence is not executable yet.
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
