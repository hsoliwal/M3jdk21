# zero-allocation-hashing 0.16 source reference

This directory compiles the unchanged zero-allocation-hashing 0.16 sources from the published sources jar.
Donor-superset lane: build kind `commands` (LEGACY_SOURCE_LEVEL), status `PASS`.
Excluded from compilation (retained under upstream-excluded/): java/lang/Math.java

```text
mvn -f m3/vendor/third_party/zero-allocation-hashing/pom.xml clean install
python m3/vendor/third_party/zero-allocation-hashing/verify_source.py m3/vendor/third_party/zero-allocation-hashing/target/zero-allocation-hashing-0.16-m3-source-1.jar
```

No speed, allocation or behaviour change is claimed. The published sources jar carries no test suite; the
compile from source and the consumers' own tests are the proof lane. Upstream tests remain a follow-up with
their own recipe. See `PROVENANCE.json` for the file-level pins and the runner receipt.
