# LatencyUtils 2.0.3 source reference

This directory compiles the unchanged LatencyUtils 2.0.3 sources from the published sources jar.
Donor-superset lane: build kind `java` (NOT_IN_LOCK), status `PASS`.


```text
mvn -f m3/vendor/third_party/LatencyUtils/pom.xml clean install
python m3/vendor/third_party/LatencyUtils/verify_source.py m3/vendor/third_party/LatencyUtils/target/LatencyUtils-2.0.3-m3-source-1.jar
```

No speed, allocation or behaviour change is claimed. The published sources jar carries no test suite; the
compile from source and the consumers' own tests are the proof lane. Upstream tests remain a follow-up with
their own recipe. See `PROVENANCE.json` for the file-level pins and the runner receipt.
