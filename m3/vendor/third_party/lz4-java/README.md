# lz4-java 1.8.0 source reference

This directory compiles the unchanged lz4-java 1.8.0 sources from the published sources jar.
Donor-superset lane: build kind `commands` (LEGACY_SOURCE_LEVEL), status `PASS`.


```text
mvn -f m3/vendor/third_party/lz4-java/pom.xml clean install
python m3/vendor/third_party/lz4-java/verify_source.py m3/vendor/third_party/lz4-java/target/lz4-java-1.8.0-m3-source-1.jar
```

No speed, allocation or behaviour change is claimed. The published sources jar carries no test suite; the
compile from source and the consumers' own tests are the proof lane. Upstream tests remain a follow-up with
their own recipe. See `PROVENANCE.json` for the file-level pins and the runner receipt.
