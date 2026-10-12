# caffeine 3.1.8 source reference

This directory retains the unchanged caffeine 3.1.8 published sources byte-for-byte without a build (status API_SURFACE_DIFFERS_MISSING_CLASSES: API_SURFACE_DIFFERS_MISSING_CLASSES vs the published jar (missing classes 528, differing members 0/0; diff caffeine-3.1.8.base.tsv)).
Donor-superset lane: build kind `commands` (API_SURFACE_DIFFERS_MISSING_CLASSES vs the published jar (missing classes 528, differing members 0/0; diff caffeine-3.1.8.base.tsv)), status `API_SURFACE_DIFFERS_MISSING_CLASSES`.


```text
python -I .m3/task-crates/dependency-source-inline-20261010/verify_crate.py

```

No speed, allocation or behaviour change is claimed. The published sources jar carries no test suite; the
compile from source and the consumers' own tests are the proof lane. Upstream tests remain a follow-up with
their own recipe. See `PROVENANCE.json` for the file-level pins and the runner receipt.
