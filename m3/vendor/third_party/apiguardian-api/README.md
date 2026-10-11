# apiguardian-api 1.1.2 source reference

This directory compiles the unchanged apiguardian-api 1.1.2 sources from the published sources jar.
Donor-superset lane: build kind `commands` (NOT_IN_LOCK), status `PASS`.
Excluded from compilation (retained under upstream-excluded/): module-info.java

```text
mvn -f m3/vendor/third_party/apiguardian-api/pom.xml clean install
python m3/vendor/third_party/apiguardian-api/verify_source.py m3/vendor/third_party/apiguardian-api/target/apiguardian-api-1.1.2-m3-source-1.jar
```

No speed, allocation or behaviour change is claimed. The published sources jar carries no test suite; the
compile from source and the consumers' own tests are the proof lane. Upstream tests remain a follow-up with
their own recipe. See `PROVENANCE.json` for the file-level pins and the runner receipt.
