# JavaEWAH 1.1.13 source reference

This directory compiles the unchanged JavaEWAH 1.1.13 sources from the published sources jar.
Donor-superset lane: build kind `java` (NOT_IN_LOCK), status `PASS`.


```text
mvn -f m3/vendor/third_party/JavaEWAH/pom.xml clean install
python m3/vendor/third_party/JavaEWAH/verify_source.py m3/vendor/third_party/JavaEWAH/target/JavaEWAH-1.1.13-m3-source-1.jar
```

No speed, allocation or behaviour change is claimed. The published sources jar carries no test suite; the
compile from source and the consumers' own tests are the proof lane. Upstream tests remain a follow-up with
their own recipe. See `PROVENANCE.json` for the file-level pins and the runner receipt.
