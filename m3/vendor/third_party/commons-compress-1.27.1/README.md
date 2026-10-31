# commons-compress 1.27.1 source reference

This directory compiles the unchanged commons-compress 1.27.1 sources from the published sources jar.
Donor-superset lane: build kind `commands` (EXTRA_BINARY_CLASSPATH), status `PASS`.


```text
mvn -f m3/vendor/third_party/commons-compress-1.27.1/pom.xml clean install
python m3/vendor/third_party/commons-compress-1.27.1/verify_source.py m3/vendor/third_party/commons-compress-1.27.1/target/commons-compress-1.27.1-m3-source-1.jar
```

No speed, allocation or behaviour change is claimed. The published sources jar carries no test suite; the
compile from source and the consumers' own tests are the proof lane. Upstream tests remain a follow-up with
their own recipe. See `PROVENANCE.json` for the file-level pins and the runner receipt.
