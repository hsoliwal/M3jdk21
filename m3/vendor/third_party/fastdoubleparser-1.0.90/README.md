# fastdoubleparser 1.0.90 source reference

This directory compiles the unchanged fastdoubleparser 1.0.90 sources from the published sources jar.
Donor-superset lane: build kind `commands` (MODULE_DESCRIPTOR_OR_MULTI_RELEASE), status `PASS`.
Excluded from compilation (retained under upstream-excluded/): ch.randelshofer.fastdoubleparser/module-info.java

```text
mvn -f m3/vendor/third_party/fastdoubleparser-1.0.90/pom.xml clean install
python m3/vendor/third_party/fastdoubleparser-1.0.90/verify_source.py m3/vendor/third_party/fastdoubleparser-1.0.90/target/fastdoubleparser-1.0.90-m3-source-1.jar
```

No speed, allocation or behaviour change is claimed. The published sources jar carries no test suite; the
compile from source and the consumers' own tests are the proof lane. Upstream tests remain a follow-up with
their own recipe. See `PROVENANCE.json` for the file-level pins and the runner receipt.
