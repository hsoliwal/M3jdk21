# guava 33.0.0-jre source reference

This directory compiles the unchanged guava 33.0.0-jre sources from the published sources jar.
Donor-superset lane: build kind `commands` (LEGACY_SOURCE_LEVEL), status `PASS`.


```text
mvn -f m3/vendor/third_party/guava/pom.xml clean install
python m3/vendor/third_party/guava/verify_source.py m3/vendor/third_party/guava/target/guava-33.0.0-jre-m3-source-1.jar
```

No speed, allocation or behaviour change is claimed. The published sources jar carries no test suite; the
compile from source and the consumers' own tests are the proof lane. Upstream tests remain a follow-up with
their own recipe. See `PROVENANCE.json` for the file-level pins and the runner receipt.
