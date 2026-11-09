# annotations 24.1.0 source reference

This directory compiles the unchanged annotations 24.1.0 sources from the published sources jar.
Donor-superset lane: build kind `commands` (MODULE_DESCRIPTOR_OR_MULTI_RELEASE), status `PASS`.
Excluded from compilation (retained under upstream-excluded/): META-INF/versions/9/module-info.java

```text
mvn -f m3/vendor/third_party/annotations/pom.xml clean install
python m3/vendor/third_party/annotations/verify_source.py m3/vendor/third_party/annotations/target/annotations-24.1.0-m3-source-1.jar
```

No speed, allocation or behaviour change is claimed. The published sources jar carries no test suite; the
compile from source and the consumers' own tests are the proof lane. Upstream tests remain a follow-up with
their own recipe. See `PROVENANCE.json` for the file-level pins and the runner receipt.
