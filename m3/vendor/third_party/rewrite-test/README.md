# rewrite-test 8.17.1 source reference

This directory compiles the unchanged rewrite-test 8.17.1 sources from the published sources jar.
Donor-superset lane: build kind `commands` (LOMBOK_AT_SOURCE_LEVEL), status `PASS`.


```text
mvn -f m3/vendor/third_party/rewrite-test/pom.xml clean install
python m3/vendor/third_party/rewrite-test/verify_source.py m3/vendor/third_party/rewrite-test/target/rewrite-test-8.17.1-m3-source-1.jar
```

No speed, allocation or behaviour change is claimed. The published sources jar carries no test suite; the
compile from source and the consumers' own tests are the proof lane. Upstream tests remain a follow-up with
their own recipe. See `PROVENANCE.json` for the file-level pins and the runner receipt.
