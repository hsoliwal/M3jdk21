# xz 1.10 source reference

This directory compiles the unchanged xz 1.10 sources from the published sources jar.
Donor-superset lane: build kind `commands` (MODULE_DESCRIPTOR_OR_MULTI_RELEASE), status `PASS`.
Excluded from compilation (retained under upstream-excluded/): META-INF/versions/9/module-info.java, META-INF/versions/9/org/tukaani/xz/check/CRC64.java, META-INF/versions/9/org/tukaani/xz/common/ByteArrayView.java, META-INF/versions/9/org/tukaani/xz/lz/BasicMatchLengthFinder.java, META-INF/versions/9/org/tukaani/xz/lz/MatchLength.java, META-INF/versions/9/org/tukaani/xz/lz/MatchLengthFinder.java, META-INF/versions/9/org/tukaani/xz/lz/UnalignedLongLEMatchLengthFinder.java

```text
mvn -f m3/vendor/third_party/xz/pom.xml clean install
python m3/vendor/third_party/xz/verify_source.py m3/vendor/third_party/xz/target/xz-1.10-m3-source-1.jar
```

No speed, allocation or behaviour change is claimed. The published sources jar carries no test suite; the
compile from source and the consumers' own tests are the proof lane. Upstream tests remain a follow-up with
their own recipe. See `PROVENANCE.json` for the file-level pins and the runner receipt.
