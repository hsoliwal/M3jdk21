# jackson-databind 2.16.1 source reference

This directory compiles the unchanged jackson-databind 2.16.1 sources from the published sources jar.
Donor-superset lane: build kind `java`, status `PASS`.


```text
mvn -f m3/vendor/third_party/jackson-databind-2.16.1/pom.xml clean install
python m3/vendor/third_party/jackson-databind-2.16.1/verify_source.py m3/vendor/third_party/jackson-databind-2.16.1/target/jackson-databind-2.16.1-m3-source-1.jar
```

No speed, allocation or behaviour change is claimed. The published sources jar carries no test suite; the
compile from source and the consumers' own tests are the proof lane. Upstream tests remain a follow-up with
their own recipe. See `PROVENANCE.json` for the file-level pins and the runner receipt.
