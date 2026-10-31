# HdrHistogram 2.1.12 source reference

This directory compiles the unchanged HdrHistogram 2.1.12 sources from the published sources jar.
Donor-superset lane: build kind `java` (NOT_IN_LOCK), status `PASS`.


```text
mvn -f m3/vendor/third_party/HdrHistogram/pom.xml clean install
python m3/vendor/third_party/HdrHistogram/verify_source.py m3/vendor/third_party/HdrHistogram/target/HdrHistogram-2.1.12-m3-source-1.jar
```

No speed, allocation or behaviour change is claimed. The published sources jar carries no test suite; the
compile from source and the consumers' own tests are the proof lane. Upstream tests remain a follow-up with
their own recipe. See `PROVENANCE.json` for the file-level pins and the runner receipt.
