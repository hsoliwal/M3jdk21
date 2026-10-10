# rewrite-java-21 8.17.1 source reference

This directory retains the unchanged rewrite-java-21 8.17.1 published sources byte-for-byte without a build (status SOURCE_ONLY: LOMBOK_AT_SOURCE_LEVEL).
Donor-superset lane: build kind `none` (LOMBOK_AT_SOURCE_LEVEL), status `SOURCE_ONLY`.


```text
python -I .m3/task-crates/dependency-source-inline-20261010/verify_crate.py

```

No speed, allocation or behaviour change is claimed. The published sources jar carries no test suite; the
compile from source and the consumers' own tests are the proof lane. Upstream tests remain a follow-up with
their own recipe. See `PROVENANCE.json` for the file-level pins and the runner receipt.
